package uz.inglish.app.feature.placement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.inglish.app.core.AppContainer
import uz.inglish.app.core.countWords
import uz.inglish.app.core.network.PlacementApi
import uz.inglish.app.core.network.dto.PlacementAnswerRequest
import uz.inglish.app.core.network.dto.PlacementSessionDto
import uz.inglish.app.core.network.userMessage
import java.util.UUID

data class PlacementUiState(
    val creating: Boolean = false,
    val submitting: Boolean = false,
    val loadingLatest: Boolean = false,
    val error: String? = null,
    val session: PlacementSessionDto? = null,
    val latest: PlacementSessionDto? = null,
    val answers: Map<String, Int> = emptyMap(),
    val writingText: String = "",
    val speakingTranscript: String = "",
) {
    val answeredCount: Int
        get() = answers.size

    val totalQuestions: Int
        get() = session?.questions?.size ?: 0

    val allAnswered: Boolean
        get() = session != null && answers.size >= (session.questions.size)

    val canSubmit: Boolean
        get() = allAnswered &&
            countWords(writingText) >= (session?.writing?.minWords ?: 60) &&
            countWords(speakingTranscript) >= 20

    val finished: Boolean
        get() = session?.status == "FINISHED"
}

class PlacementViewModel(private val placementApi: PlacementApi) : ViewModel() {
    private val _state = MutableStateFlow(PlacementUiState(loadingLatest = true))
    val state: StateFlow<PlacementUiState> = _state.asStateFlow()

    private var generation = 0

    /** Stable across retries so a lost response replays the first result. */
    private var submitIdempotencyKey: String? = null

    init {
        loadLatest()
    }

    fun loadLatest() {
        val gen = generation
        _state.update { it.copy(loadingLatest = true) }
        viewModelScope.launch {
            try {
                val latest = placementApi.latest()
                if (gen != generation) return@launch
                _state.update { it.copy(loadingLatest = false, latest = latest) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (gen != generation) return@launch
                _state.update { it.copy(loadingLatest = false, error = error.userMessage()) }
            }
        }
    }

    fun start() {
        if (_state.value.creating || _state.value.submitting) return
        val gen = ++generation
        submitIdempotencyKey = UUID.randomUUID().toString()
        _state.update {
            it.copy(creating = true, error = null, session = null, answers = emptyMap(), writingText = "", speakingTranscript = "")
        }

        viewModelScope.launch {
            try {
                val session = placementApi.create()
                if (gen != generation) return@launch
                _state.update {
                    it.copy(creating = false, session = session, answers = emptyMap(), writingText = session.writing?.text.orEmpty(), speakingTranscript = session.speaking?.transcript.orEmpty())
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (gen != generation) return@launch
                _state.update { it.copy(creating = false, error = error.userMessage()) }
            }
        }
    }

    fun select(questionId: String, answerIndex: Int) {
        if (_state.value.finished || _state.value.submitting) return
        _state.update { it.copy(answers = it.answers + (questionId to answerIndex)) }
    }

    fun updateWriting(text: String) {
        if (_state.value.finished || _state.value.submitting) return
        _state.update { it.copy(writingText = text) }
    }

    fun updateSpeakingTranscript(text: String) {
        if (_state.value.finished || _state.value.submitting) return
        _state.update { it.copy(speakingTranscript = text) }
    }

    fun submit() {
        val current = _state.value
        val session = current.session ?: return
        if (current.submitting || current.finished || !current.canSubmit) return

        val questions = session.questions
        val payload = questions.mapNotNull { question ->
            val answerIndex = current.answers[question.id] ?: return@mapNotNull null
            PlacementAnswerRequest(questionId = question.id, answerIndex = answerIndex)
        }
        if (payload.size != questions.size) return

        val gen = generation
        val idempotencyKey = submitIdempotencyKey ?: UUID.randomUUID().toString().also {
            submitIdempotencyKey = it
        }
        _state.update { it.copy(submitting = true, error = null) }

        viewModelScope.launch {
            try {
                val updated = placementApi.submit(
                    session.id,
                    payload,
                    current.writingText.trim(),
                    current.speakingTranscript.trim(),
                    idempotencyKey,
                )
                if (gen != generation) return@launch
                submitIdempotencyKey = null
                _state.update {
                    it.copy(submitting = false, session = updated, latest = updated)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (gen != generation) return@launch
                _state.update { it.copy(submitting = false, error = error.userMessage()) }
            }
        }
    }

    fun reset() {
        generation += 1
        submitIdempotencyKey = null
        _state.value = PlacementUiState()
        loadLatest()
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { PlacementViewModel(container.placementApi) }
        }
    }
}
