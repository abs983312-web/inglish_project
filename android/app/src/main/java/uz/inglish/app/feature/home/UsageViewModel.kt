package uz.inglish.app.feature.home

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
import uz.inglish.app.core.network.BillingApi
import uz.inglish.app.core.network.dto.UsageDto
import uz.inglish.app.core.network.userMessage

data class UsageUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val usage: UsageDto? = null,
)

class UsageViewModel(private val billingApi: BillingApi) : ViewModel() {
    private val _state = MutableStateFlow(UsageUiState())
    val state: StateFlow<UsageUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val usage = billingApi.usage()
                _state.update { it.copy(loading = false, usage = usage) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { UsageViewModel(container.billingApi) }
        }
    }
}
