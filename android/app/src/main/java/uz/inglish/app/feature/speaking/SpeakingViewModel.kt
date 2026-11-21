package uz.inglish.app.feature.speaking

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
import uz.inglish.app.core.network.SpeakingApi
import uz.inglish.app.core.network.dto.SpeakingPromptDto
import uz.inglish.app.core.network.dto.SpeakingSubmissionDto
import uz.inglish.app.core.network.dto.SpeakingSubmissionSummaryDto
import uz.inglish.app.core.network.userMessage
import java.util.UUID

data class SpeakingUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val prompts: List<SpeakingPromptDto> = emptyList(),
    val partFilter: String? = null,
    val loadingPrompt: Boolean = false,
    val selectedPrompt: SpeakingPromptDto? = null,
    val transcript: String = "",
    val submitting: Boolean = false,
    val submission: SpeakingSubmissionDto? = null,
    val pendingSubmissionId: String? = null,
    val history: List<SpeakingSubmissionSummaryDto> = emptyList(),
)

class SpeakingViewModel(private val speakingApi: SpeakingApi) : ViewModel() {
    private val _state = MutableStateFlow(SpeakingUiState())
    val state: StateFlow<SpeakingUiState> = _state.asStateFlow()

    /** Stable across retries so a lost response replays the first score. */
    private var submitIdempotencyKey: String? = null

    init {
        load()
    }

    fun load() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val prompts = speakingApi.prompts(_state.value.partFilter)
                val history = runCatching { speakingApi.submissions() }.getOrDefault(emptyList())
                _state.update { it.copy(loading = false, prompts = prompts, history = history) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }

    fun setPartFilter(part: String?) {
        _state.update { it.copy(partFilter = part) }
        load()
    }

    fun openPrompt(slug: String) {
        if (_state.value.loadingPrompt) return
        submitIdempotencyKey = null
        _state.update {
            it.copy(loadingPrompt = true, error = null, transcript = "", submission = null, pendingSubmissionId = null)
        }
        viewModelScope.launch {
            try {
                val prompt = speakingApi.prompt(slug)
                _state.update { it.copy(loadingPrompt = false, selectedPrompt = prompt) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(loadingPrompt = false, error = error.userMessage()) }
            }
        }
    }

    fun closePrompt() {
        submitIdempotencyKey = null
        _state.update {
            it.copy(selectedPrompt = null, submission = null, transcript = "", pendingSubmissionId = null)
        }
    }

    fun closeSubmission() {
        _state.update { it.copy(submission = null) }
    }

    fun updateTranscript(text: String) {
        if (_state.value.transcript == text) return
        submitIdempotencyKey = null
        _state.update { it.copy(transcript = text, pendingSubmissionId = null) }
    }

    fun submit() {
        val current = _state.value
        val prompt = current.selectedPrompt ?: return
        if (current.submitting) return
        if (countWords(current.transcript) < 10) {
            _state.update { it.copy(error = "Javobni biroz kengaytiring (kamida 10 so'z).") }
            return
        }

        val idempotencyKey = submitIdempotencyKey ?: UUID.randomUUID().toString().also {
            submitIdempotencyKey = it
        }
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            try {
                val submissionId = current.pendingSubmissionId ?: run {
                    val created = speakingApi.createSubmission(prompt.slug, current.transcript)
                    _state.update { it.copy(pendingSubmissionId = created.id) }
                    created.id
                }
                val scored = speakingApi.submit(submissionId, idempotencyKey)
                submitIdempotencyKey = null
                val history = runCatching { speakingApi.submissions() }.getOrDefault(current.history)
                _state.update {
                    it.copy(
                        submitting = false,
                        submission = scored,
                        pendingSubmissionId = null,
                        history = history,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // Reuse the created draft on retry instead of duplicating it.
                _state.update { it.copy(submitting = false, error = error.userMessage()) }
            }
        }
    }

    fun openSubmission(id: String) {
        submitIdempotencyKey = null
        _state.update { it.copy(loadingPrompt = true, error = null) }
        viewModelScope.launch {
            try {
                val submission = speakingApi.submission(id)
                _state.update {
                    it.copy(
                        loadingPrompt = false,
                        selectedPrompt = null,
                        submission = submission,
                        transcript = submission.transcript,
                        pendingSubmissionId = null,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(loadingPrompt = false, error = error.userMessage()) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { SpeakingViewModel(container.speakingApi) }
        }
    }
}
