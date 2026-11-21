package uz.inglish.app.feature.battle

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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import uz.inglish.app.core.AppContainer
import uz.inglish.app.core.network.ApiException
import uz.inglish.app.core.network.BattleApi
import uz.inglish.app.core.network.BattlePackApi
import uz.inglish.app.core.network.BattleRealtime
import uz.inglish.app.core.network.dto.BattleAnswerResultDto
import uz.inglish.app.core.network.dto.BattleRoomDto
import uz.inglish.app.core.network.userMessage

enum class BattleStage { HOME, LOBBY, GAME, RESULTS }

data class BattleUiState(
    val loading: Boolean = false,
    val answering: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val room: BattleRoomDto? = null,
    val participantId: String? = null,
    val selectedIndex: Int? = null,
    val feedback: BattleAnswerResultDto? = null,
    val answeredRoundId: String? = null,
    val serverOffsetMs: Long = 0L,
    val connected: Boolean = false,
    val codeInput: String = "",
    val nickname: String = "",
    val questionCount: Int = 10,
    val timeLimitMs: Int = 20_000,
) {
    val stage: BattleStage
        get() = when {
            room == null -> BattleStage.HOME
            room.status == "LOBBY" -> BattleStage.LOBBY
            room.status == "FINISHED" -> BattleStage.RESULTS
            else -> BattleStage.GAME
        }
}

class BattleViewModel(
    private val battleApi: BattleApi,
    private val battlePackApi: BattlePackApi,
    private val realtime: BattleRealtime,
) : ViewModel() {
    private val _state = MutableStateFlow(BattleUiState())
    val state: StateFlow<BattleUiState> = _state.asStateFlow()

    /** Bumped on create/join/leave so late responses cannot revive an old game. */
    private var gameGeneration = 0
    private var realtimeJob: Job? = null
    private var realtimeRoomId: String? = null

    fun updateCode(value: String) {
        _state.update { it.copy(codeInput = value.filter(Char::isDigit).take(6)) }
    }

    fun updateNickname(value: String) {
        _state.update { it.copy(nickname = value.take(20)) }
    }

    fun setQuestionCount(count: Int) {
        if (count !in listOf(10, 15, 20)) return
        _state.update { it.copy(questionCount = count) }
    }

    fun setTimeLimit(ms: Int) {
        if (ms !in listOf(10_000, 20_000, 30_000)) return
        _state.update { it.copy(timeLimitMs = ms) }
    }

    fun createBot() = createRoom("BOT")

    fun createFriend() = createRoom("FRIEND")

    fun createClassroom() = createRoom("CLASSROOM")

    private fun createRoom(mode: String) {
        if (_state.value.loading) return
        val generation = ++gameGeneration
        val settings = _state.value
        disconnectRealtime()
        _state.update {
            it.copy(
                loading = true,
                error = null,
                notice = null,
                room = null,
                participantId = null,
                selectedIndex = null,
                feedback = null,
                answeredRoundId = null,
            )
        }

        viewModelScope.launch {
            try {
                val response = battleApi.createRoom(
                    mode = mode,
                    questionCount = settings.questionCount,
                    timeLimitMs = settings.timeLimitMs,
                    autoBot = mode == "FRIEND",
                )
                if (generation != gameGeneration) return@launch
                applyRoom(response.room, response.participantId)
                _state.update { it.copy(loading = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (generation != gameGeneration) return@launch
                _state.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }

    fun joinByCode(code: String? = null) {
        val normalized = (code ?: _state.value.codeInput)
            .filter(Char::isDigit)
            .take(6)

        if (normalized.length != 6) {
            _state.update { it.copy(error = "6 xonali xona kodini kiriting.") }
            return
        }

        val generation = ++gameGeneration
        val oldRoom = _state.value.room
        disconnectRealtime()
        _state.update {
            it.copy(
                loading = true,
                error = null,
                notice = null,
                room = null,
                participantId = null,
                selectedIndex = null,
                feedback = null,
                answeredRoundId = null,
            )
        }

        if (oldRoom != null && oldRoom.status != "FINISHED") {
            viewModelScope.launch { runCatching { battleApi.leave(oldRoom.id) } }
        }

        viewModelScope.launch {
            try {
                val response = battleApi.join(normalized, _state.value.nickname)
                if (generation != gameGeneration) return@launch
                applyRoom(response.room, response.participantId)
                _state.update { it.copy(loading = false, codeInput = "") }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (generation != gameGeneration) return@launch
                _state.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }

    fun setReady(ready: Boolean) {
        val current = _state.value
        val room = current.room ?: return
        if (current.loading || room.status != "LOBBY") return
        val generation = gameGeneration
        _state.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            try {
                val updated = battleApi.setReady(room.id, ready)
                if (generation != gameGeneration) return@launch
                applyRoom(updated, current.participantId)
                _state.update { it.copy(loading = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }

    fun start() {
        val current = _state.value
        val room = current.room ?: return
        if (current.loading || room.status != "LOBBY") return
        val generation = gameGeneration
        _state.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            try {
                val updated = battleApi.start(room.id)
                if (generation != gameGeneration) return@launch
                applyRoom(updated, current.participantId)
                _state.update { it.copy(loading = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }

    fun addBot() {
        val current = _state.value
        val room = current.room ?: return
        if (current.loading || room.status != "LOBBY") return
        val generation = gameGeneration
        _state.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            try {
                val updated = battleApi.addBot(room.id)
                if (generation != gameGeneration) return@launch
                applyRoom(updated, current.participantId)
                _state.update { it.copy(loading = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }

    fun selectOption(index: Int) {
        val current = _state.value
        val room = current.room ?: return
        if (
            room.phase != "QUESTION" ||
            current.answering ||
            current.answeredRoundId == room.currentQuestion?.id
        ) {
            return
        }
        _state.update { it.copy(selectedIndex = index) }
    }

    fun submit() = submitInternal(_state.value.selectedIndex, force = false)

    /** Timer expired: submit the selected option if there is one, otherwise -1. */
    fun timeout() = submitInternal(null, force = true)

    private fun submitInternal(index: Int?, force: Boolean) {
        val current = _state.value
        val room = current.room ?: return
        val question = room.currentQuestion ?: return
        if (room.phase != "QUESTION" || current.answering) return
        if (current.answeredRoundId == question.id) return
        if (!force && index == null) return

        val generation = gameGeneration
        val answerIndex = index ?: -1
        _state.update {
            it.copy(
                answering = true,
                answeredRoundId = question.id,
                selectedIndex = index ?: it.selectedIndex,
                error = null,
            )
        }

        viewModelScope.launch {
            try {
                val response = battleApi.answer(room.id, question.id, answerIndex)
                if (generation != gameGeneration) return@launch
                applyRoom(response.room, response.participantId ?: current.participantId)
                _state.update { it.copy(answering = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (generation != gameGeneration) return@launch
                val roundClosed = error is ApiException && error.code in setOf(
                    "BATTLE_ROUND_CLOSED",
                    "BATTLE_ROUND_MISMATCH",
                    "BATTLE_ALREADY_ANSWERED",
                )
                _state.update {
                    it.copy(
                        answering = false,
                        answeredRoundId = if (roundClosed) question.id else null,
                        notice = if (roundClosed) {
                            "Vaqt tugadi. Keyingi savol kutilmoqda."
                        } else {
                            it.notice
                        },
                    )
                }

                if (roundClosed) {
                    runCatching { battleApi.room(room.id) }.onSuccess { response ->
                        applyRoom(
                            response.room,
                            response.participantId ?: current.participantId,
                        )
                    }
                } else {
                    _state.update { it.copy(error = error.userMessage()) }
                }
            }
        }
    }

    fun playAgain() {
        val room = _state.value.room ?: return
        val isHost = _state.value.participantId == room.hostParticipantId

        // A pack room is replayed from the same pack, not from random questions.
        val pack = room.pack
        if (pack != null && isHost) {
            val generation = ++gameGeneration
            disconnectRealtime()
            _state.update {
                it.copy(
                    loading = true,
                    error = null,
                    notice = null,
                    room = null,
                    participantId = null,
                    selectedIndex = null,
                    feedback = null,
                    answeredRoundId = null,
                )
            }
            viewModelScope.launch {
                try {
                    val response = battlePackApi.play(pack.id, room.mode, false)
                    if (generation != gameGeneration) return@launch
                    applyRoom(response.room, response.participantId)
                    _state.update { it.copy(loading = false) }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    if (generation != gameGeneration) return@launch
                    _state.update { it.copy(loading = false, error = error.userMessage()) }
                }
            }
            return
        }

        if (room.mode == "BOT" || isHost) {
            _state.update {
                it.copy(
                    questionCount = room.questionCount,
                    timeLimitMs = room.timeLimitMs,
                )
            }
            createRoom(room.mode)
        } else {
            leave()
            _state.update {
                it.copy(notice = "Yangi xona kodi bilan qo‘shiling yoki o‘zingiz xona yarating.")
            }
        }
    }

    fun leave() {
        val room = _state.value.room
        val settings = _state.value
        gameGeneration += 1
        disconnectRealtime()
        _state.value = BattleUiState(
            questionCount = settings.questionCount,
            timeLimitMs = settings.timeLimitMs,
            nickname = settings.nickname,
        )

        if (room != null && room.status != "FINISHED") {
            viewModelScope.launch { runCatching { battleApi.leave(room.id) } }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /** Called by the packs screen after a pack room has been created. */
    fun adoptRoom(room: BattleRoomDto, participantId: String?) {
        gameGeneration += 1
        disconnectRealtime()
        _state.update {
            it.copy(
                room = null,
                participantId = null,
                selectedIndex = null,
                feedback = null,
                answeredRoundId = null,
                error = null,
                notice = null,
                loading = false,
            )
        }
        applyRoom(room, participantId)
    }

    fun setNickname(nickname: String) {
        val current = _state.value
        val room = current.room ?: return
        val trimmed = nickname.trim()
        if (trimmed.length < 2 || trimmed.length > 20) {
            _state.update { it.copy(error = "Taxallus 2–20 belgidan iborat bo‘lishi kerak.") }
            return
        }
        if (current.loading) return
        _state.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            try {
                val updated = battlePackApi.setNickname(room.id, trimmed)
                applyRoom(updated, current.participantId)
                _state.update { it.copy(loading = false) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }

    fun clearNotice() {
        _state.update { it.copy(notice = null) }
    }

    private fun applyRoom(
        room: BattleRoomDto,
        participantId: String?,
        fromSocket: Boolean = false,
    ) {
        val previous = _state.value

        // A REST response can arrive after a newer WebSocket snapshot. The
        // server timestamp makes stale snapshots harmless without a sequence
        // column in the database.
        if (
            previous.room != null &&
            previous.room.id == room.id &&
            previous.room.serverNow > 0 &&
            room.serverNow > 0 &&
            room.serverNow < previous.room.serverNow
        ) {
            return
        }

        val pid = participantId ?: previous.participantId
        val questionChanged = previous.room?.currentQuestion?.id != room.currentQuestion?.id
        val myAnswer = room.reveal?.answers?.firstOrNull { it.participantId == pid }

        val derivedFeedback = if (room.reveal != null && myAnswer != null) {
            BattleAnswerResultDto(
                roundId = room.reveal.roundId,
                isCorrect = myAnswer.isCorrect,
                correctIndex = room.reveal.correctIndex,
                explanation = room.reveal.explanation,
                scoreGained = myAnswer.scoreGained,
                responseMs = myAnswer.responseMs,
                answerIndex = myAnswer.answerIndex,
            )
        } else {
            null
        }

        val selected = when {
            myAnswer != null && myAnswer.answerIndex >= 0 -> myAnswer.answerIndex
            questionChanged -> null
            else -> previous.selectedIndex
        }

        val answeredRoundId = when {
            questionChanged -> null
            myAnswer != null -> room.reveal?.roundId
            else -> previous.answeredRoundId
        }

        val offset = if (room.serverNow > 0) {
            room.serverNow - System.currentTimeMillis()
        } else {
            previous.serverOffsetMs
        }

        _state.update {
            it.copy(
                room = room,
                participantId = pid,
                selectedIndex = selected,
                feedback = derivedFeedback,
                answeredRoundId = answeredRoundId,
                serverOffsetMs = offset,
                connected = if (fromSocket) true else it.connected,
                loading = false,
                answering = if (questionChanged) false else it.answering,
                error = null,
                notice = if (questionChanged) null else it.notice,
            )
        }

        when (room.status) {
            "FINISHED" -> disconnectRealtime()
            "LOBBY", "ACTIVE" -> connectRealtime(room.id)
        }
    }

    private fun connectRealtime(roomId: String) {
        if (realtimeRoomId == roomId && realtimeJob?.isActive == true) return
        disconnectRealtime()
        realtimeRoomId = roomId
        realtimeJob = viewModelScope.launch {
            var backoffMs = 1_000L
            while (isActive) {
                try {
                    realtime.updates(roomId).collect { room ->
                        backoffMs = 1_000L
                        applyRoom(room, _state.value.participantId, fromSocket = true)
                    }
                    _state.update { it.copy(connected = false) }
                    // A clean server close should not spin this loop.
                    delay(backoffMs)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    _state.update { it.copy(connected = false) }

                    if (error is ApiException && error.code == "ROOM_CLOSED") {
                        _state.update {
                            it.copy(
                                error = "Xona yopildi.",
                                room = null,
                                participantId = null,
                                connected = false,
                            )
                        }
                        realtimeRoomId = null
                        break
                    }

                    // The REST snapshot doubles as a token refresh: if the
                    // access token expired, authorizedRequest rotates it before
                    // the next WebSocket attempt.
                    val refresh = runCatching { battleApi.room(roomId) }
                    refresh.onSuccess { response ->
                        applyRoom(
                            response.room,
                            response.participantId ?: _state.value.participantId,
                        )
                    }
                    val failure = refresh.exceptionOrNull()
                    if (
                        failure is ApiException &&
                        (failure.status == 403 || failure.status == 404 || failure.status == 410)
                    ) {
                        _state.update {
                            it.copy(
                                error = failure.userMessage(),
                                room = null,
                                participantId = null,
                                connected = false,
                            )
                        }
                        realtimeRoomId = null
                        break
                    }

                    delay(backoffMs)
                    backoffMs = (backoffMs * 2).coerceAtMost(8_000L)
                }
            }
        }
    }

    private fun disconnectRealtime() {
        realtimeJob?.cancel()
        realtimeJob = null
        realtimeRoomId = null
        _state.update { it.copy(connected = false) }
    }

    override fun onCleared() {
        disconnectRealtime()
        super.onCleared()
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                BattleViewModel(
                    battleApi = container.battleApi,
                    battlePackApi = container.battlePackApi,
                    realtime = container.battleRealtime,
                )
            }
        }
    }
}
