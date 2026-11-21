package uz.inglish.app.feature.content

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
import kotlinx.serialization.json.JsonElement
import uz.inglish.app.core.AppContainer
import uz.inglish.app.core.network.ContentApi
import uz.inglish.app.core.network.dto.ContentUnitDetailDto
import uz.inglish.app.core.network.dto.ContentUnitDto
import uz.inglish.app.core.network.dto.ExerciseAnswerResultDto
import uz.inglish.app.core.network.userMessage

data class ContentUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val units: List<ContentUnitDto> = emptyList(),
    val loadingUnit: Boolean = false,
    val selectedUnit: ContentUnitDetailDto? = null,
    val checkingId: String? = null,
    val answers: Map<String, ExerciseAnswerResultDto> = emptyMap(),
)

class ContentViewModel(private val contentApi: ContentApi) : ViewModel() {
    private val _state = MutableStateFlow(ContentUiState())
    val state: StateFlow<ContentUiState> = _state.asStateFlow()

    init {
        loadUnits()
    }

    fun loadUnits() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val units = contentApi.units()
                _state.update { it.copy(loading = false, units = units) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }

    fun openUnit(slug: String) {
        if (_state.value.loadingUnit) return
        _state.update { it.copy(loadingUnit = true, error = null) }
        viewModelScope.launch {
            try {
                val unit = contentApi.unit(slug)
                _state.update {
                    it.copy(
                        loadingUnit = false,
                        selectedUnit = unit,
                        answers = emptyMap(),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(loadingUnit = false, error = error.userMessage()) }
            }
        }
    }

    fun closeUnit() {
        _state.update { it.copy(selectedUnit = null, answers = emptyMap()) }
    }

    fun checkAnswer(exerciseId: String, answer: JsonElement) {
        if (_state.value.checkingId != null) return
        _state.update { it.copy(checkingId = exerciseId, error = null) }
        viewModelScope.launch {
            try {
                val result = contentApi.checkAnswer(exerciseId, answer)
                _state.update {
                    it.copy(
                        checkingId = null,
                        answers = it.answers + (exerciseId to result),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update {
                    it.copy(checkingId = null, error = error.userMessage())
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { ContentViewModel(container.contentApi) }
        }
    }
}
