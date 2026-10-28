package com.lifereset.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

/**
 * 워치 전용 색. 워치는 화면이 검은 게 배터리에 좋아서 폰과 달리 어두운 바탕을 쓰고,
 * 칩(줄)은 폰의 연회색 메뉴 줄, 강조는 폰과 같은 금색만 쓴다.
 */
object WearColors {
    val row = Color(0xFF2E3136)
    val rowDone = Color(0xFF1E2023)
    val disc = Color(0xFFE4E5E7)
    val onDisc = Color(0xFF3E4146)
    val text = Color(0xFFF2F3F5)
    val textDim = Color(0xFFA6AAB0)
    val accent = Color(0xFFF0B400)
    val exp = Color(0xFF8CC63F)
}

@Composable
fun WearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = Colors(
            primary = WearColors.accent,
            onPrimary = Color.White,
            secondary = WearColors.exp,
            surface = WearColors.row,
            onSurface = WearColors.text,
            onSurfaceVariant = WearColors.textDim,
        ),
        content = content,
    )
}
