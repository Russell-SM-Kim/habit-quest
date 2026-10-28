package com.lifereset.core.designsystem.fx

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.lifereset.core.designsystem.theme.LrTheme
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** 연출 뒤에 깔리는 어두운 막. 탭하면 onTap (연출 건너뛰기/닫기). */
@Composable
internal fun FxScrim(
    onTap: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) { alpha.animateTo(1f, tween(200)) }
    Box(
        Modifier
            .fillMaxSize()
            .background(LrTheme.colors.scrim.copy(alpha = LrTheme.colors.scrim.alpha * alpha.value))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
            ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** 중심에서 퍼져나가는 고리 두 개. */
@Composable
internal fun RingBurst(color: Color, modifier: Modifier = Modifier, delayMillis: Int = 0) {
    val first = remember { Animatable(0f) }
    val second = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { first.animateTo(1f, tween(900, delayMillis, LinearOutSlowInEasing)) }
        launch { second.animateTo(1f, tween(900, delayMillis + 220, LinearOutSlowInEasing)) }
    }
    Canvas(modifier.fillMaxSize()) {
        val maxRadius = min(size.width, size.height) * 0.6f
        listOf(first.value, second.value).forEach { t ->
            if (t in 0.001f..0.999f) {
                drawCircle(
                    color = color.copy(alpha = 1f - t),
                    radius = maxRadius * t,
                    style = Stroke(width = (6f * (1f - t) + 1f).dp.toPx()),
                )
            }
        }
    }
}

/** 사방으로 흩어지는 작은 마름모 조각들. 랜덤 없이 각도를 고정해 매번 같게 그린다. */
@Composable
internal fun ShardBurst(color: Color, modifier: Modifier = Modifier, count: Int = 24, delayMillis: Int = 0) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1100, delayMillis, FastOutSlowInEasing)) }
    Canvas(modifier.fillMaxSize()) {
        val t = progress.value
        if (t <= 0f || t >= 1f) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val reach = min(size.width, size.height) * 0.48f
        repeat(count) { i ->
            val angle = Math.toRadians(i * 360.0 / count + (i % 3) * 7.0)
            val distance = reach * t * (0.6f + (i % 4) * 0.12f)
            val position = Offset(
                center.x + (cos(angle) * distance).toFloat(),
                center.y + (sin(angle) * distance).toFloat(),
            )
            val s = (5f + (i % 3) * 2f).dp.toPx() * (1f - t * 0.6f)
            rotate(degrees = 45f + i * 15f, pivot = position) {
                val diamond = Path().apply {
                    moveTo(position.x, position.y - s)
                    lineTo(position.x + s * 0.6f, position.y)
                    lineTo(position.x, position.y + s)
                    lineTo(position.x - s * 0.6f, position.y)
                    close()
                }
                drawPath(diamond, color.copy(alpha = 1f - t))
            }
        }
    }
}
