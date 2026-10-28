package com.lifereset.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lifereset.app.navigation.Routes
import com.lifereset.app.navigation.TopLevel
import com.lifereset.core.data.DataContainer
import com.lifereset.core.designsystem.component.LrBackground
import com.lifereset.core.designsystem.component.LrBottomBar
import com.lifereset.core.designsystem.component.LrNavRail
import com.lifereset.core.designsystem.component.NavEntry
import com.lifereset.core.designsystem.fx.FxHost
import com.lifereset.core.designsystem.sound.FxSounds
import com.lifereset.core.designsystem.theme.LrTheme
import com.lifereset.feature.quests.QuestEditRoute
import com.lifereset.feature.quests.QuestListRoute
import com.lifereset.feature.record.RecordRoute
import com.lifereset.feature.settings.SettingsRoute
import com.lifereset.feature.status.StatusRoute
import com.lifereset.feature.today.TodayRoute

/**
 * 앱의 뼈대.
 *
 *  [배경(하늘 또는 내 사진)]
 *    └ 접힘(폭 < 600dp): 화면 + 아래 원형 메뉴
 *    └ 펼침(폭 ≥ 600dp, 폴드 안쪽 화면): 왼쪽 세로 메뉴 + 화면 두 개 나란히 (WidePanes)
 *  [맨 위: 연출 레이어 FxHost]
 *
 * 화면(feature)끼리는 서로를 모르고, 화면 이동은 전부 여기서 연결한다.
 */
@Composable
fun LifeResetApp(container: DataContainer, sounds: FxSounds? = null) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val selectedTab = TopLevel.entries.indexOfFirst { it.route == currentRoute }
    val backgroundUri by container.settingsRepository.backgroundUri.collectAsStateWithLifecycle(initialValue = null)
    val navEntries = TopLevel.entries.map { NavEntry(it.label, it.icon) }
    val onSelectTab: (Int) -> Unit = { index -> nav.navigateToTab(TopLevel.entries[index]) }

    LrBackground(imageUri = backgroundUri) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= LrTheme.Dimens.wideBreakpoint

            if (wide) {
                Row(Modifier.fillMaxSize().navigationBarsPadding()) {
                    LrNavRail(
                        entries = navEntries,
                        selectedIndex = selectedTab.coerceAtLeast(0),
                        onSelect = onSelectTab,
                    )
                    AppNavHost(container, nav, wide = true, modifier = Modifier.weight(1f))
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    AppNavHost(container, nav, wide = false, modifier = Modifier.weight(1f))
                    if (selectedTab >= 0) {
                        LrBottomBar(entries = navEntries, selectedIndex = selectedTab, onSelect = onSelectTab)
                    }
                }
            }
        }

        // 맨 위 레이어: 퀘스트 클리어, 레벨업 등 연출 + 효과음
        FxHost(fx = container.gameRepository.fx, sounds = sounds)
    }
}

/**
 * 화면 경로 표. wide = true 면 탭 화면마다 오른쪽에 짝 화면을 하나 더 붙인다.
 *   오늘 | 상태,  상태 | 기록,  기록 | 상태,  설정 | 퀘스트 목록
 */
@Composable
private fun AppNavHost(
    container: DataContainer,
    nav: NavHostController,
    wide: Boolean,
    modifier: Modifier = Modifier,
) {
    val today: @Composable () -> Unit = {
        TodayRoute(
            game = container.gameRepository,
            settings = container.settingsRepository,
            onAddQuest = { nav.navigate(Routes.questEdit()) },
        )
    }
    val status: @Composable () -> Unit = { StatusRoute(container.gameRepository, container.settingsRepository) }
    val record: @Composable () -> Unit = { RecordRoute(container.gameRepository) }
    val settings: @Composable () -> Unit = {
        SettingsRoute(
            settings = container.settingsRepository,
            sync = container.syncManager,
            onOpenQuests = { nav.navigate(Routes.QUESTS) },
        )
    }
    val questList: @Composable () -> Unit = {
        QuestListRoute(
            quests = container.questRepository,
            onEdit = { id -> nav.navigate(Routes.questEdit(id)) },
            onAdd = { nav.navigate(Routes.questEdit()) },
        )
    }

    NavHost(navController = nav, startDestination = TopLevel.TODAY.route, modifier = modifier) {
        composable(TopLevel.TODAY.route) { Panes(wide, today, status) }
        composable(TopLevel.STATUS.route) { Panes(wide, status, record) }
        composable(TopLevel.RECORD.route) { Panes(wide, record, status) }
        composable(TopLevel.SETTINGS.route) { Panes(wide, settings, questList) }
        composable(Routes.QUESTS) { questList() }
        composable(
            route = Routes.QUEST_EDIT,
            arguments = listOf(
                navArgument(Routes.ARG_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            QuestEditRoute(
                quests = container.questRepository,
                time = container.time,
                questId = entry.arguments?.getString(Routes.ARG_ID),
                onFinished = { nav.popBackStack() },
            )
        }
    }
}

/** 좁으면 main 하나, 넓으면 main | side 반반. */
@Composable
private fun Panes(
    wide: Boolean,
    main: @Composable () -> Unit,
    side: @Composable () -> Unit,
) {
    if (!wide) {
        main()
        return
    }
    Row(Modifier.fillMaxSize().padding(end = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f)) { main() }
        Box(Modifier.weight(1f)) { side() }
    }
}

private fun NavHostController.navigateToTab(tab: TopLevel) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
