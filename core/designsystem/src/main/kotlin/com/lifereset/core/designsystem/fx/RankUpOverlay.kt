package com.lifereset.core.designsystem.fx

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifereset.core.designsystem.component.ConfirmCancelRow
import com.lifereset.core.designsystem.component.DialogWindow
import com.lifereset.core.designsystem.component.SystemWindowAppear
import com.lifereset.core.designsystem.theme.LrTheme
import com.lifereset.core.game.engine.Fx
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 랭크업 연출: 기존 랭크 글자가 떨리다 부서지듯 사라지고, 새 랭크 글자가 빛과 함께 조립된다.
 */
@Composable
internal fun RankUpOverlay(fx: Fx.RankUp, onDone: () -> Unit) {
    val colors = LrTheme.colors
    val shake = remember { Animatable(0f) }
    val oldOut = remember { Animatable(0f) }
    val newIn = remember { Animatable(0f) }
    var showWindow by remember { mutableStateOf(false) }

    LaunchedEffect(fx) {
        shake.animateTo(1f, tween(450))
        oldOut.animateTo(1f, tween(300, easing = FastOutSlowInEasing))
        newIn.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        delay(150)
        showWindow = true
    }

    FxScrim(onTap = onDone) {
        if (newIn.value > 0f) {
            RingBurst(color = colors.accent)
            ShardBurst(color = Color.White, count = 32)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.height(150.dp), contentAlignment = Alignment.Center) {
                val jitter = if (oldOut.value == 0f) (sin(shake.value * 60f) * 8f * shake.value).roundToInt() else 0
                Text(
                    text = fx.from.name,
                    style = LrTheme.type.number.copy(fontSize = 110.sp),
                    color = Color.White,
                    modifier = Modifier
                        .offset { IntOffset(jitter, 0) }
                        .graphicsLayer {
                            alpha = 1f - oldOut.value
                            scaleX = 1f + oldOut.value * 0.8f
                            scaleY = 1f + oldOut.value * 0.8f
                        },
                )
                Text(
                    text = fx.to.name,
                    style = LrTheme.type.number.copy(fontSize = 120.sp),
                    color = Color.White,
                    modifier = Modifier.graphicsLayer {
                        alpha = newIn.value.coerceIn(0f, 1f)
                        val scale = 2.4f - 1.4f * newIn.value
                        scaleX = scale
                        scaleY = scale
                    },
                )
            }
            if (showWindow) {
                SystemWindowAppear(Modifier.padding(horizontal = 24.dp)) {
                    DialogWindow(
                        title = "RANK UP",
                        modifier = Modifier.widthIn(max = 320.dp),
                        footer = { ConfirmCancelRow(onConfirm = onDone, onCancel = onDone, cancelLabel = "닫기") },
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("${fx.label} 랭크", style = LrTheme.type.number.copy(fontSize = 26.sp), color = colors.textPrimary)
                            Text(
                                "칭호 '${fx.to.title}' 획득 · 레벨업 보너스 ${fx.to.bonusPoints}점",
                                style = LrTheme.type.caption,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 6.dp, start = 16.dp, end = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
