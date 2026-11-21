package uz.inglish.app.feature.vocabulary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.inglish.app.core.AppContainer
import uz.inglish.app.core.network.VocabularyApi
import uz.inglish.app.core.network.dto.VocabularyItemDto
import uz.inglish.app.core.network.dto.VocabularySetDto
import uz.inglish.app.core.network.dto.VocabularyStatsDto
import uz.inglish.app.core.network.userMessage

data class VocabularyUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val stats: VocabularyStatsDto? = null,
    val due: List<VocabularyItemDto> = emptyList(),
    val index: Int = 0,
    val revealed: Boolean = false,
    val reviewing: Boolean = false,
    val sessionDone: Boolean = false,
    val reviewedCount: Int = 0,
    val tab: Int = 0,
    val list: List<VocabularyItemDto> = emptyList(),
    val listLoading: Boolean = false,
    val listQuery: String = "",
    val sets: List<VocabularySetDto> = emptyList(),
    val selectedSet: String? = null,
    val listTotal: Int = 0,
    val listHasMore: Boolean = false,
) {
    val current: VocabularyItemDto? get() = due.getOrNull(index)
}

class VocabularyViewModel(private val vocabularyApi: VocabularyApi) : ViewModel() {
    private val _state = MutableStateFlow(VocabularyUiState())
    val state: StateFlow<VocabularyUiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var listJob: Job? = null

    init {
        load()
        loadSets()
    }

    fun load() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val stats = vocabularyApi.stats()
                val due = vocabularyApi.due(limit = 20)
                _state.update {
                    it.copy(
                        loading = false,
                        stats = stats,
                        due = due,
                        index = 0,
                        revealed = false,
                        reviewedCount = 0,
                        sessionDone = due.isEmpty(),
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }

    fun setTab(tab: Int) {
        _state.update { it.copy(tab = tab) }
        if (tab == 1) {
            if (_state.value.sets.isEmpty()) loadSets()
            if (_state.value.list.isEmpty() && !_state.value.listLoading) {
                loadList(reset = true)
            }
        }
    }

    fun selectSet(tag: String?) {
        if (_state.value.selectedSet == tag) return
        _state.update { it.copy(selectedSet = tag) }
        loadList(reset = true)
    }

    fun loadMore() {
        val current = _state.value
        if (current.listLoading || !current.listHasMore) return
        loadList(reset = false)
    }

    fun reveal() {
        _state.update { it.copy(revealed = true) }
    }

    fun rate(rating: Int) {
        val current = _state.value
        val item = current.current ?: return
        if (current.reviewing) return

        _state.update { it.copy(reviewing = true, error = null) }
        viewModelScope.launch {
            try {
                vocabularyApi.review(item.id, rating)
                val nextIndex = current.index + 1
                val done = nextIndex >= current.due.size
                val stats = runCatching { vocabularyApi.stats() }.getOrNull()
                _state.update {
                    it.copy(
                        reviewing = false,
                        index = nextIndex,
                        revealed = false,
                        sessionDone = done,
                        reviewedCount = it.reviewedCount + 1,
                        stats = stats ?: it.stats,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(reviewing = false, error = error.userMessage()) }
            }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(listQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            loadList(reset = true, query = query)
        }
    }

    private fun loadSets() {
        viewModelScope.launch {
            try {
                val sets = vocabularyApi.sets()
                _state.update { it.copy(sets = sets) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(error = error.userMessage()) }
            }
        }
    }

    private fun loadList(reset: Boolean, query: String = _state.value.listQuery) {
        val tag = _state.value.selectedSet
        val offset = if (reset) 0 else _state.value.list.size
        listJob?.cancel()
        _state.update { it.copy(listLoading = true, error = null) }
        listJob = viewModelScope.launch {
            try {
                val response = vocabularyApi.list(
                    query = query,
                    tag = tag,
                    limit = LIST_PAGE_SIZE,
                    offset = offset,
                )
                _state.update {
                    val merged = if (reset) {
                        response.items
                    } else {
                        (it.list + response.items).distinctBy { item -> item.id }
                    }
                    it.copy(
                        listLoading = false,
                        list = merged,
                        listTotal = response.total,
                        listHasMore = merged.size < response.total,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(listLoading = false, error = error.userMessage()) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    companion object {
        private const val LIST_PAGE_SIZE = 50

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { VocabularyViewModel(container.vocabularyApi) }
        }
    }
}
