package com.lifereset.core.designsystem.fx

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifereset.core.designsystem.component.LrPanel
import com.lifereset.core.designsystem.theme.LrTheme
import com.lifereset.core.game.engine.Fx
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 퀘스트 클리어 알림. 화면 위에서 내려와 EXP 숫자가 올라가고 1.4초 뒤 사라진다.
 * 화면을 막지 않아서 연달아 체크해도 방해되지 않는다.
 */
@Composable
internal fun QuestClearToast(fx: Fx.QuestClear, onDone: () -> Unit) {
    val enter = remember { Animatable(0f) }
    val count = remember { Animatable(0f) }
    LaunchedEffect(fx) {
        launch { enter.animateTo(1f, tween(240, easing = FastOutSlowInEasing)) }
        count.animateTo(fx.xp.toFloat(), tween(550, delayMillis = 120))
        delay(750)
        enter.animateTo(0f, tween(200))
        onDone()
    }
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 16.dp), contentAlignment = Alignment.TopCenter) {
        LrPanel(
            modifier = Modifier
                .widthIn(min = 240.dp)
                .graphicsLayer {
                    alpha = enter.value
                    translationY = (1f - enter.value) * -40.dp.toPx()
                }
                .semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("QUEST CLEAR", style = LrTheme.type.code, color = LrTheme.colors.textSecondary)
                Text(
                    "+${count.value.toInt()} EXP",
                    style = LrTheme.type.number.copy(fontSize = 20.sp),
                    color = LrTheme.colors.textPrimary,
                )
                if (fx.stats.isNotEmpty()) {
                    Text(fx.stats.joinToString(" ") { "${it.code}↑" }, style = LrTheme.type.code, color = LrTheme.colors.textSecondary)
                }
            }
        }
    }
}
