package uz.inglish.app.feature.battle

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
import uz.inglish.app.core.network.BattlePackApi
import uz.inglish.app.core.network.dto.BattlePackDto
import uz.inglish.app.core.network.dto.BattlePackQuestionInput
import uz.inglish.app.core.network.dto.BattleRoomResponse
import uz.inglish.app.core.network.dto.CreateBattlePackRequest
import uz.inglish.app.core.network.userMessage

enum class PacksScreen { LIST, CREATE, DETAIL }

data class PackQuestionDraft(
    val prompt: String = "",
    val type: String = "MCQ",
    val options: List<String> = listOf("", "", "", ""),
    val correctIndex: Int = 0,
    val explanation: String = "",
) {
    val isTrueFalse: Boolean get() = type == "TRUE_FALSE"
}

data class BattlePacksUiState(
    val screen: PacksScreen = PacksScreen.LIST,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val deleting: Boolean = false,
    val playing: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val scope: String = "popular",
    val search: String = "",
    val items: List<BattlePackDto> = emptyList(),
    val total: Int = 0,
    val selected: BattlePackDto? = null,
    val played: BattleRoomResponse? = null,
    // Create / edit form
    val editingId: String? = null,
    val title: String = "",
    val description: String = "",
    val videoUrl: String = "",
    val tags: String = "",
    val questions: List<PackQuestionDraft> = listOf(PackQuestionDraft()),
    val activeQuestion: Int = 0,
) {
    val activeDraft: PackQuestionDraft?
        get() = questions.getOrNull(activeQuestion)
}

class BattlePacksViewModel(
    private val packApi: BattlePackApi,
) : ViewModel() {
    private val _state = MutableStateFlow(BattlePacksUiState())
    val state: StateFlow<BattlePacksUiState> = _state.asStateFlow()

    private var listJob: Int = 0

    fun load(scope: String = _state.value.scope) {
        val generation = ++listJob
        _state.update { it.copy(loading = true, error = null, scope = scope) }

        viewModelScope.launch {
            try {
                val page = packApi.list(scope = scope, search = _state.value.search)
                if (generation != listJob) return@launch
                _state.update {
                    it.copy(
                        loading = false,
                        items = page.items,
                        total = page.total,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (generation != listJob) return@launch
                _state.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }

    fun setScope(scope: String) {
        if (scope !in setOf("popular", "new", "mine")) return
        if (_state.value.scope == scope) return
        load(scope)
    }

    fun setSearch(value: String) {
        _state.update { it.copy(search = value.take(60)) }
    }

    fun applySearch() = load(_state.value.scope)

    fun openCreate() {
        _state.update {
            it.copy(
                screen = PacksScreen.CREATE,
                editingId = null,
                title = "",
                description = "",
                videoUrl = "",
                tags = "",
                questions = listOf(PackQuestionDraft()),
                activeQuestion = 0,
                error = null,
                notice = null,
            )
        }
    }

    fun openDetail(pack: BattlePackDto) {
        _state.update { it.copy(screen = PacksScreen.DETAIL, selected = pack, error = null) }
        viewModelScope.launch {
            try {
                val full = packApi.get(pack.id)
                _state.update { it.copy(selected = full) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // The summary is already useful; keep it if the detail call fails.
            }
        }
    }

    fun backToList() {
        _state.update { it.copy(screen = PacksScreen.LIST, selected = null, error = null) }
        load(_state.value.scope)
    }

    fun setTitle(value: String) = _state.update { it.copy(title = value.take(120)) }
    fun setDescription(value: String) = _state.update { it.copy(description = value.take(1000)) }
    fun setVideoUrl(value: String) = _state.update { it.copy(videoUrl = value.take(500)) }
    fun setTags(value: String) = _state.update { it.copy(tags = value.take(200)) }

    fun selectQuestion(index: Int) {
        if (index !in _state.value.questions.indices) return
        _state.update { it.copy(activeQuestion = index) }
    }

    fun addQuestion() {
        _state.update {
            val next = it.questions + PackQuestionDraft()
            it.copy(questions = next, activeQuestion = next.lastIndex, error = null)
        }
    }

    fun removeQuestion(index: Int) {
        _state.update {
            if (it.questions.size <= 1) return@update it.copy(
                error = "Kamida 1 ta savol kerak.",
            )
            val next = it.questions.filterIndexed { position, _ -> position != index }
            it.copy(
                questions = next,
                activeQuestion = it.activeQuestion.coerceAtMost(next.lastIndex),
            )
        }
    }

    fun setQuestionType(type: String) {
        updateActive { draft ->
            if (type == "TRUE_FALSE") {
                draft.copy(type = type, options = listOf("True", "False"), correctIndex = 0)
            } else {
                draft.copy(type = type, options = listOf("", "", "", ""), correctIndex = 0)
            }
        }
    }

    fun setQuestionPrompt(value: String) = updateActive { it.copy(prompt = value.take(400)) }

    fun setOption(index: Int, value: String) = updateActive { draft ->
        if (draft.isTrueFalse || index !in draft.options.indices) return@updateActive draft
        draft.copy(
            options = draft.options.mapIndexed { position, option ->
                if (position == index) value.take(120) else option
            },
        )
    }

    fun setCorrect(index: Int) = updateActive { draft ->
        if (index in draft.options.indices) draft.copy(correctIndex = index) else draft
    }

    fun setExplanation(value: String) = updateActive { it.copy(explanation = value.take(600)) }

    private fun updateActive(transform: (PackQuestionDraft) -> PackQuestionDraft) {
        _state.update { current ->
            val draft = current.questions.getOrNull(current.activeQuestion) ?: return@update current
            current.copy(
                questions = current.questions.mapIndexed { index, item ->
                    if (index == current.activeQuestion) transform(draft) else item
                },
                error = null,
            )
        }
    }

    fun save() {
        val current = _state.value
        if (current.saving) return

        val title = current.title.trim()
        if (title.length < 3) {
            _state.update { it.copy(error = "Sarlavha kamida 3 belgidan iborat bo‘lishi kerak.") }
            return
        }
        if (current.videoUrl.isBlank()) {
            _state.update { it.copy(error = "YouTube havolasini kiriting.") }
            return
        }

        val questions = mutableListOf<BattlePackQuestionInput>()
        for ((index, draft) in current.questions.withIndex()) {
            val prompt = draft.prompt.trim()
            if (prompt.length < 3) {
                _state.update {
                    it.copy(
                        error = "${index + 1}-savol matnini kiriting.",
                        activeQuestion = index,
                    )
                }
                return
            }

            val options = if (draft.isTrueFalse) {
                listOf("True", "False")
            } else {
                draft.options.map { it.trim() }.filter { it.isNotEmpty() }
            }
            if (options.size < 2) {
                _state.update {
                    it.copy(
                        error = "${index + 1}-savolda kamida 2 variant kiriting.",
                        activeQuestion = index,
                    )
                }
                return
            }
            if (draft.correctIndex !in options.indices) {
                _state.update {
                    it.copy(
                        error = "${index + 1}-savolda to‘g‘ri javobni belgilang.",
                        activeQuestion = index,
                    )
                }
                return
            }

            questions += BattlePackQuestionInput(
                type = if (draft.isTrueFalse) "TRUE_FALSE" else "MCQ",
                prompt = prompt,
                options = options,
                correctIndex = draft.correctIndex,
                explanation = draft.explanation.trim().takeIf { it.isNotEmpty() },
            )
        }

        val request = CreateBattlePackRequest(
            title = title,
            description = current.description.trim().takeIf { it.isNotEmpty() },
            videoUrl = current.videoUrl.trim(),
            tags = current.tags
                .split(',')
                .map { it.trim() }
                .filter { it.length >= 2 }
                .distinctBy { it.lowercase() }
                .take(5),
            questions = questions,
        )

        val editingId = current.editingId
        _state.update { it.copy(saving = true, error = null) }

        viewModelScope.launch {
            try {
                val saved = if (editingId == null) {
                    packApi.create(request)
                } else {
                    packApi.update(editingId, request)
                }
                _state.update {
                    it.copy(
                        saving = false,
                        screen = PacksScreen.DETAIL,
                        selected = saved,
                        editingId = null,
                        notice = if (editingId == null) {
                            "Pack yaratildi! Kod bilan do‘stlaringizni taklif qiling."
                        } else {
                            "Pack yangilandi."
                        },
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(saving = false, error = error.userMessage()) }
            }
        }
    }

    fun startEdit() {
        val pack = _state.value.selected ?: return
        _state.update {
            it.copy(
                screen = PacksScreen.CREATE,
                editingId = pack.id,
                title = pack.title,
                description = pack.description.orEmpty(),
                videoUrl = pack.videoUrl,
                tags = pack.tags.joinToString(", "),
                questions = pack.questions.map { question ->
                    PackQuestionDraft(
                        prompt = question.prompt,
                        type = question.type,
                        options = if (question.type == "TRUE_FALSE") {
                            listOf("True", "False")
                        } else {
                            question.options + List(4 - question.options.size.coerceAtMost(4)) { "" }
                        },
                        correctIndex = question.correctIndex ?: 0,
                        explanation = question.explanation.orEmpty(),
                    )
                }.ifEmpty { listOf(PackQuestionDraft()) },
                activeQuestion = 0,
                error = null,
            )
        }
    }

    fun play(pack: BattlePackDto, mode: String = "FRIEND") {
        if (_state.value.playing) return
        _state.update { it.copy(playing = true, error = null) }
        viewModelScope.launch {
            try {
                val response = packApi.play(pack.id, mode = mode, shuffle = false)
                _state.update { it.copy(playing = false, played = response) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(playing = false, error = error.userMessage()) }
            }
        }
    }

    fun consumePlayed() {
        _state.update { it.copy(played = null, playing = false) }
    }

    fun delete(pack: BattlePackDto) {
        if (_state.value.deleting) return
        _state.update { it.copy(deleting = true, error = null) }
        viewModelScope.launch {
            try {
                packApi.delete(pack.id)
                _state.update {
                    it.copy(
                        deleting = false,
                        screen = PacksScreen.LIST,
                        selected = null,
                        notice = "Pack o‘chirildi.",
                        items = it.items.filterNot { item -> item.id == pack.id },
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(deleting = false, error = error.userMessage()) }
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(error = null, notice = null) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                BattlePacksViewModel(packApi = container.battlePackApi)
            }
        }
    }
}
