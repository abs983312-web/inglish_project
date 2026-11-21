package uz.inglish.app.feature.writing

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
import uz.inglish.app.core.network.WritingApi
import uz.inglish.app.core.network.dto.WritingSubmissionDto
import uz.inglish.app.core.network.dto.WritingSubmissionSummaryDto
import uz.inglish.app.core.network.dto.WritingTaskDetailDto
import uz.inglish.app.core.network.dto.WritingTaskDto
import uz.inglish.app.core.network.userMessage
import java.util.UUID

data class WritingUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val tasks: List<WritingTaskDto> = emptyList(),
    val loadingTask: Boolean = false,
    val selectedTask: WritingTaskDetailDto? = null,
    val draft: String = "",
    val submitting: Boolean = false,
    val submission: WritingSubmissionDto? = null,
    val pendingSubmissionId: String? = null,
    val history: List<WritingSubmissionSummaryDto> = emptyList(),
)

class WritingViewModel(private val writingApi: WritingApi) : ViewModel() {
    private val _state = MutableStateFlow(WritingUiState())
    val state: StateFlow<WritingUiState> = _state.asStateFlow()

    /**
     * Stable across retries so a lost response replays the first scoring result
     * instead of scoring the same draft twice.
     */
    private var submitIdempotencyKey: String? = null

    init {
        load()
    }

    fun load() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val tasks = writingApi.tasks()
                val history = runCatching { writingApi.submissions() }.getOrDefault(emptyList())
                _state.update { it.copy(loading = false, tasks = tasks, history = history) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }

    fun openTask(slug: String) {
        if (_state.value.loadingTask) return
        submitIdempotencyKey = null
        _state.update {
            it.copy(loadingTask = true, error = null, draft = "", submission = null, pendingSubmissionId = null)
        }
        viewModelScope.launch {
            try {
                val task = writingApi.task(slug)
                _state.update { it.copy(loadingTask = false, selectedTask = task) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(loadingTask = false, error = error.userMessage()) }
            }
        }
    }

    fun closeTask() {
        submitIdempotencyKey = null
        _state.update {
            it.copy(selectedTask = null, submission = null, draft = "", pendingSubmissionId = null)
        }
    }

    fun closeSubmission() {
        _state.update { it.copy(submission = null) }
    }

    fun updateDraft(text: String) {
        if (_state.value.draft == text) return
        submitIdempotencyKey = null
        _state.update { it.copy(draft = text, pendingSubmissionId = null) }
    }

    fun submit() {
        val current = _state.value
        val task = current.selectedTask ?: return
        if (current.submitting) return
        if (countWords(current.draft) < 20) {
            _state.update { it.copy(error = "Kamida 20 so'z yozing.") }
            return
        }

        val idempotencyKey = submitIdempotencyKey ?: UUID.randomUUID().toString().also {
            submitIdempotencyKey = it
        }
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            try {
                val submissionId = current.pendingSubmissionId ?: run {
                    val created = writingApi.createSubmission(task.slug, current.draft)
                    _state.update { it.copy(pendingSubmissionId = created.id) }
                    created.id
                }
                val scored = writingApi.submit(submissionId, idempotencyKey)
                submitIdempotencyKey = null
                val history = runCatching { writingApi.submissions() }.getOrDefault(current.history)
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
                // Keep pendingSubmissionId so a retry reuses the same draft
                // instead of creating a duplicate submission.
                _state.update { it.copy(submitting = false, error = error.userMessage()) }
            }
        }
    }

    fun openSubmission(id: String) {
        submitIdempotencyKey = null
        _state.update { it.copy(loadingTask = true, error = null) }
        viewModelScope.launch {
            try {
                val submission = writingApi.submission(id)
                _state.update {
                    it.copy(
                        loadingTask = false,
                        selectedTask = null,
                        submission = submission,
                        draft = submission.text,
                        pendingSubmissionId = null,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(loadingTask = false, error = error.userMessage()) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { WritingViewModel(container.writingApi) }
        }
    }
}
