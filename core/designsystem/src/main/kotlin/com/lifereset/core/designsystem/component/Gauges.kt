package com.lifereset.core.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lifereset.core.designsystem.theme.LrTheme

/**
 * 기울어진 게이지 (EXP, 보스 HP). 값이 바뀌면 0.7초 동안 차오른다.
 * @param onDark 어두운 띠 위에 놓일 때 (보스 HP) 트랙을 진하게
 */
@Composable
fun SkewGauge(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = LrTheme.colors.exp,
    height: Dp = 10.dp,
    onDark: Boolean = false,
) {
    val colors = LrTheme.colors
    val track = if (onDark) Color(0x99282C32) else colors.track
    val border = if (onDark) Color(0xCCFFFFFF) else colors.trackBorder
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "gauge",
    )
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(animated, 0f..1f) },
    ) {
        val skew = size.height * 0.47f
        fun parallelogram(right: Float) = Path().apply {
            moveTo(skew, 0f)
            lineTo(right, 0f)
            lineTo(right - skew, size.height)
            lineTo(0f, size.height)
            close()
        }
        val outline = parallelogram(size.width)
        drawPath(outline, track)
        if (animated > 0f) {
            val right = skew + (size.width - skew) * animated
            drawPath(parallelogram(right.coerceAtLeast(skew * 2)), color)
        }
        drawPath(outline, border, style = Stroke(width = 1.dp.toPx()))
    }
}

/** 배경 위에 바로 놓는 흰 반투명 진행 막대 (시즌 진행). */
@Composable
fun SkyProgress(progress: Float, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "sky")
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(Color(0x59FFFFFF))
            .then(Modifier),
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(animated).background(Color(0xF2FFFFFF)))
    }
}

/** 얇은 가로 막대 (능력치, 분배 비율). */
@Composable
fun ThinBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = LrTheme.colors.iconDisc,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(600), label = "bar")
    Box(modifier.height(6.dp).background(LrTheme.colors.track)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(animated).background(color))
    }
}
