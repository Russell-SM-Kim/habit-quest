package com.lifereset.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * 선 아이콘 모음 (24×24, 굵기 2). 색은 Icon(tint = …) 로 칠한다.
 * 아이콘을 바꾸려면 path 문자열만 교체하거나, res/drawable 의 벡터로 바꿔서 painterResource 로 쓰면 된다.
 */
object LrIcons {
    val Home = icon("home", "M3 10 12 3l9 7v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z", "M9 22V12h6v10")
    val User = icon("user", circle(12f, 8f, 4f), "M20 21a8 8 0 0 0-16 0")
    val Calendar = icon("calendar", "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z", "M16 2v4", "M8 2v4", "M3 10h18")
    val Settings = icon("settings", "M20 7h-9", "M14 17H5", circle(17f, 17f, 3f), circle(7f, 7f, 3f))
    val Plus = icon("plus", "M5 12h14", "M12 5v14")
    val Check = icon("check", "M20 6 9 17l-5-5")
    val Close = icon("close", "M6 6l12 12", "M18 6 6 18")
    val Confirm = icon("confirm", circle(12f, 12f, 7f))
    val Back = icon("back", "m15 18-6-6 6-6")
    val Flame = icon(
        "flame",
        "M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z",
    )
    val Shield = icon("shield", "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z")
    val Trophy = icon(
        "trophy",
        "M6 9H4.5a2.5 2.5 0 0 1 0-5H6", "M18 9h1.5a2.5 2.5 0 0 0 0-5H18", "M4 22h16",
        "M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22",
        "M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22",
        "M18 2H6v7a6 6 0 0 0 12 0V2Z",
    )
    val Bolt = icon("bolt", "M13 2 3 14h9l-1 8 10-12h-9l1-8z")
    val Sync = icon("sync", "M21 12a9 9 0 0 1-15.5 6.2L3 16", "M3 12a9 9 0 0 1 15.5-6.2L21 8", "M21 3v5h-5", "M3 21v-5h5")
    val Edit = icon("edit", "M12 20h9", "M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z")
    val List = icon("list", "M8 6h13", "M8 12h13", "M8 18h13", "M3 6h.01", "M3 12h.01", "M3 18h.01")
    val Sword = icon("sword", "M14.5 17.5 3 6V3h3l11.5 11.5", "M13 19l6-6", "M16 16l4 4", "M19 21l2-2")

    private fun circle(cx: Float, cy: Float, r: Float): String =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

    private fun icon(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            paths.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
}
