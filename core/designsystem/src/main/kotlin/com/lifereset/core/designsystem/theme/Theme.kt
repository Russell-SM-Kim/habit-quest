package com.lifereset.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val LocalLrColors = staticCompositionLocalOf { GlassColors }
private val LocalLrTypography = staticCompositionLocalOf { DefaultTypography }

/** 화면 어디서나 LrTheme.colors.accent 처럼 꺼내 쓴다. */
object LrTheme {
    val colors: LrColors
        @Composable @ReadOnlyComposable get() = LocalLrColors.current
    val type: LrTypography
        @Composable @ReadOnlyComposable get() = LocalLrTypography.current

    /** 치수. 모서리 컷 크기, 줄 높이 등을 여기서 한 번에 조절한다. */
    object Dimens {
        /** 창 왼쪽 위 사선 컷 */
        val panelCut: Dp = 14.dp
        val screenPadding: Dp = 16.dp
        val rowHeight: Dp = 50.dp
        val rowGap: Dp = 4.dp
        val iconDisc: Dp = 32.dp
        val circleButton: Dp = 52.dp
        val circleRing: Dp = 62.dp
        /** 이 너비 이상이면 폴드 펼친 화면 배치 (왼쪽 메뉴 + 두 칸) */
        val wideBreakpoint: Dp = 600.dp
    }
}

@Composable
fun LifeResetTheme(
    colors: LrColors = GlassColors,
    typography: LrTypography = DefaultTypography,
    content: @Composable () -> Unit,
) {
    // Material 컴포넌트(텍스트필드 등)도 같은 색을 쓰도록 매핑
    val material = lightColorScheme(
        primary = colors.iconDisc,
        onPrimary = colors.onIconDisc,
        secondary = colors.accent,
        background = colors.sky,
        onBackground = colors.onSky,
        surface = colors.glass,
        onSurface = colors.textPrimary,
        onSurfaceVariant = colors.textSecondary,
        outline = colors.rule,
        error = colors.danger,
    )
    CompositionLocalProvider(
        LocalLrColors provides colors,
        LocalLrTypography provides typography,
    ) {
        MaterialTheme(colorScheme = material, content = content)
    }
}
