package com.lifereset.app.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.lifereset.core.designsystem.icon.LrIcons

/** 아래 메뉴에 나오는 4개 화면. 순서를 바꾸면 메뉴 순서가 바뀐다. */
enum class TopLevel(val route: String, val label: String, val icon: ImageVector) {
    TODAY("today", "오늘", LrIcons.Home),
    STATUS("status", "상태", LrIcons.User),
    RECORD("record", "기록", LrIcons.Calendar),
    SETTINGS("settings", "설정", LrIcons.Settings),
}

/** 메뉴 밖 화면 경로. */
object Routes {
    const val QUESTS = "quests"
    const val QUEST_EDIT = "quests/edit?id={id}"
    const val ARG_ID = "id"

    fun questEdit(id: String? = null): String = if (id == null) "quests/edit" else "quests/edit?id=$id"
}
