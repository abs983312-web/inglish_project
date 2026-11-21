package uz.inglish.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import uz.inglish.app.core.AppContainer
import uz.inglish.app.core.network.dto.UserDto
import uz.inglish.app.feature.content.ContentScreen
import uz.inglish.app.feature.content.ContentViewModel
import uz.inglish.app.feature.battle.BattleScreen
import uz.inglish.app.feature.battle.BattlePacksScreen
import uz.inglish.app.feature.battle.BattlePacksViewModel
import uz.inglish.app.feature.battle.BattleViewModel
import uz.inglish.app.feature.billing.PaywallScreen
import uz.inglish.app.feature.billing.PaywallViewModel
import uz.inglish.app.feature.placement.PlacementScreen
import uz.inglish.app.feature.placement.PlacementViewModel
import uz.inglish.app.feature.home.HomeScreen
import uz.inglish.app.feature.home.ProgressViewModel
import uz.inglish.app.feature.home.UsageViewModel
import uz.inglish.app.feature.speaking.SpeakingScreen
import uz.inglish.app.feature.speaking.SpeakingViewModel
import uz.inglish.app.feature.vocabulary.VocabularyScreen
import uz.inglish.app.feature.vocabulary.VocabularyViewModel
import uz.inglish.app.feature.writing.WritingScreen
import uz.inglish.app.feature.writing.WritingViewModel
import uz.inglish.app.ui.components.InglishSand

private enum class MainTab(val label: String, val icon: ImageVector) {
    HOME("Asosiy", Icons.Filled.Home),
    LESSONS("Darslar", Icons.Filled.Star),
    WRITING("Writing", Icons.Filled.Edit),
    SPEAKING("Speaking", Icons.Filled.Person),
    VOCABULARY("Lug'at", Icons.Filled.Favorite),
    BATTLE("Battle", Icons.Filled.PlayArrow),
}

@Composable
fun MainScreen(
    container: AppContainer,
    user: UserDto?,
    profileError: String?,
    verifyLoading: Boolean,
    verifyNotice: String?,
    verifyError: String?,
    onUpdateProfile: (displayName: String, cefrLevel: String?, targetBand: Double?, examDate: String?) -> Unit,
    onClearProfileError: () -> Unit,
    onDeleteAccount: () -> Unit,
    onRequestEmailVerification: () -> Unit,
    onConfirmEmailVerification: (String) -> Unit,
    onClearVerifyMessages: () -> Unit,
    onLogout: () -> Unit,
    initialBattleCode: String? = null,
    onBattleCodeConsumed: () -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var showPlacement by rememberSaveable { mutableStateOf(false) }
    var showPaywall by rememberSaveable { mutableStateOf(false) }
    var showPacks by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showPlacement || showPaywall) {
        showPlacement = false
        showPaywall = false
    }

    BackHandler(enabled = !showPlacement && !showPaywall && !showPacks && selectedTab != MainTab.HOME.ordinal) {
        selectedTab = MainTab.HOME.ordinal
    }

    SystemBarIcons(darkStatusIcons = true, darkNavigationIcons = true)

    val usageViewModel: UsageViewModel = viewModel(
        key = "usage-${user?.id}",
        factory = UsageViewModel.factory(container),
    )
    val usageState by usageViewModel.state.collectAsStateWithLifecycle()

    val progressViewModel: ProgressViewModel = viewModel(
        key = "progress-${user?.id}",
        factory = ProgressViewModel.factory(container),
    )
    val progressState by progressViewModel.state.collectAsStateWithLifecycle()

    val battleViewModel: BattleViewModel = viewModel(
        key = "battle-${user?.id}",
        factory = BattleViewModel.factory(container),
    )
    val battleState by battleViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(selectedTab) {
        if (selectedTab == MainTab.HOME.ordinal) {
            usageViewModel.load()
            progressViewModel.load()
        }
    }

    LaunchedEffect(initialBattleCode) {
        if (!initialBattleCode.isNullOrBlank()) {
            selectedTab = MainTab.BATTLE.ordinal
        }
    }

    if (showPlacement) {
        val vm: PlacementViewModel = viewModel(
            key = "placement-${user?.id}",
            factory = PlacementViewModel.factory(container),
        )
        val state by vm.state.collectAsStateWithLifecycle()
        PlacementScreen(
            state = state,
            onStart = vm::start,
            onSelect = vm::select,
            onUpdateWriting = vm::updateWriting,
            onUpdateSpeakingTranscript = vm::updateSpeakingTranscript,
            onSubmit = vm::submit,
            onClose = { showPlacement = false },
            onClearError = vm::clearError,
        )
        return
    }

    if (showPaywall) {
        val vm: PaywallViewModel = viewModel(
            key = "paywall-${user?.id}",
            factory = PaywallViewModel.factory(container),
        )
        val state by vm.state.collectAsStateWithLifecycle()
        PaywallScreen(
            state = state,
            onPurchase = vm::purchase,
            onRestore = vm::restore,
            onClose = {
                showPaywall = false
                usageViewModel.load()
            },
            onClearMessages = vm::clearMessages,
        )
        return
    }

    if (showPacks) {
        val packsVm: BattlePacksViewModel = viewModel(
            key = "battle-packs-${user?.id}",
            factory = BattlePacksViewModel.factory(container),
        )
        val packsState by packsVm.state.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) { packsVm.load() }
        LaunchedEffect(packsState.played) {
            val played = packsState.played ?: return@LaunchedEffect
            battleViewModel.adoptRoom(played.room, played.participantId)
            packsVm.consumePlayed()
            showPacks = false
            selectedTab = MainTab.BATTLE.ordinal
        }

        BattlePacksScreen(
            state = packsState,
            vm = packsVm,
            onBack = { showPacks = false },
        )
        return
    }

    Scaffold(
        containerColor = InglishSand,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                MainTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 10.sp) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (MainTab.entries.getOrElse(selectedTab) { MainTab.HOME }) {
                MainTab.HOME -> HomeScreen(
                    user = user,
                    usageState = usageState,
                    progressState = progressState,
                    profileError = profileError,
                    verifyLoading = verifyLoading,
                    verifyNotice = verifyNotice,
                    verifyError = verifyError,
                    onRefreshUsage = usageViewModel::load,
                    onRefreshProgress = progressViewModel::load,
                    onOpenLessons = { selectedTab = MainTab.LESSONS.ordinal },
                    onOpenWriting = { selectedTab = MainTab.WRITING.ordinal },
                    onOpenSpeaking = { selectedTab = MainTab.SPEAKING.ordinal },
                    onOpenVocabulary = { selectedTab = MainTab.VOCABULARY.ordinal },
                    onOpenBattle = { selectedTab = MainTab.BATTLE.ordinal },
                    onOpenPlacement = { showPlacement = true },
                    onOpenPaywall = { showPaywall = true },
                    onUpdateProfile = onUpdateProfile,
                    onClearProfileError = onClearProfileError,
                    onDeleteAccount = onDeleteAccount,
                    onRequestEmailVerification = onRequestEmailVerification,
                    onConfirmEmailVerification = onConfirmEmailVerification,
                    onClearVerifyMessages = onClearVerifyMessages,
                    onLogout = onLogout,
                )

                MainTab.LESSONS -> {
                    val vm: ContentViewModel = viewModel(
                        key = "content-${user?.id}",
                        factory = ContentViewModel.factory(container),
                    )
                    val state by vm.state.collectAsStateWithLifecycle()
                    ContentScreen(
                        state = state,
                        onRetry = vm::loadUnits,
                        onOpenUnit = vm::openUnit,
                        onCloseUnit = vm::closeUnit,
                        onCheckAnswer = vm::checkAnswer,
                        onClearError = vm::clearError,
                    )
                }

                MainTab.WRITING -> {
                    val vm: WritingViewModel = viewModel(
                        key = "writing-${user?.id}",
                        factory = WritingViewModel.factory(container),
                    )
                    val state by vm.state.collectAsStateWithLifecycle()
                    WritingScreen(
                        state = state,
                        onRetry = vm::load,
                        onOpenTask = vm::openTask,
                        onCloseTask = vm::closeTask,
                        onUpdateDraft = vm::updateDraft,
                        onSubmit = vm::submit,
                        onOpenSubmission = vm::openSubmission,
                        onCloseSubmission = vm::closeSubmission,
                        onClearError = vm::clearError,
                    )
                }

                MainTab.SPEAKING -> {
                    val vm: SpeakingViewModel = viewModel(
                        key = "speaking-${user?.id}",
                        factory = SpeakingViewModel.factory(container),
                    )
                    val state by vm.state.collectAsStateWithLifecycle()
                    SpeakingScreen(
                        state = state,
                        onRetry = vm::load,
                        onSetPart = vm::setPartFilter,
                        onOpenPrompt = vm::openPrompt,
                        onClosePrompt = vm::closePrompt,
                        onUpdateTranscript = vm::updateTranscript,
                        onSubmit = vm::submit,
                        onOpenSubmission = vm::openSubmission,
                        onCloseSubmission = vm::closeSubmission,
                        onClearError = vm::clearError,
                    )
                }

                MainTab.VOCABULARY -> {
                    val vm: VocabularyViewModel = viewModel(
                        key = "vocabulary-${user?.id}",
                        factory = VocabularyViewModel.factory(container),
                    )
                    val state by vm.state.collectAsStateWithLifecycle()
                    VocabularyScreen(
                        state = state,
                        onRetry = vm::load,
                        onSetTab = vm::setTab,
                        onReveal = vm::reveal,
                        onRate = vm::rate,
                        onQueryChange = vm::onQueryChange,
                        onSelectSet = vm::selectSet,
                        onLoadMore = vm::loadMore,
                        onClearError = vm::clearError,
                    )
                }

                MainTab.BATTLE -> {
                    LaunchedEffect(initialBattleCode) {
                        val code = initialBattleCode ?: return@LaunchedEffect
                        battleViewModel.joinByCode(code)
                        onBattleCodeConsumed()
                    }

                    BattleScreen(
                        state = battleState,
                        onCreateBot = battleViewModel::createBot,
                        onCreateFriend = battleViewModel::createFriend,
                        onCreateClassroom = battleViewModel::createClassroom,
                        onOpenPacks = { showPacks = true },
                        onJoinCode = { battleViewModel.joinByCode() },
                        onCodeChange = battleViewModel::updateCode,
                        onNicknameChange = battleViewModel::updateNickname,
                        onSetNickname = battleViewModel::setNickname,
                        onSetQuestionCount = battleViewModel::setQuestionCount,
                        onSetTimeLimit = battleViewModel::setTimeLimit,
                        onReady = battleViewModel::setReady,
                        onStart = battleViewModel::start,
                        onAddBot = battleViewModel::addBot,
                        onSelectOption = battleViewModel::selectOption,
                        onSubmit = battleViewModel::submit,
                        onTimeout = battleViewModel::timeout,
                        onPlayAgain = battleViewModel::playAgain,
                        onLeave = battleViewModel::leave,
                        onClearError = battleViewModel::clearError,
                    )
                }
            }
        }
    }
}
