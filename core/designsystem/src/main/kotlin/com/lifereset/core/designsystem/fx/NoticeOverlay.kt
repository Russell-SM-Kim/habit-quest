package com.lifereset.core.designsystem.fx

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifereset.core.designsystem.component.ConfirmCancelRow
import com.lifereset.core.designsystem.component.DialogWindow
import com.lifereset.core.designsystem.component.SystemWindowAppear
import com.lifereset.core.designsystem.theme.LrTheme

/**
 * 일반 시스템 알림 창 (완벽한 하루, 보스 처치, 전직, 시즌 클리어).
 * 본문은 한 글자씩 타이핑되듯 나타난다.
 */
@Composable
internal fun NoticeOverlay(
    header: String,
    headline: String,
    lines: List<String>,
    onDone: () -> Unit,
    burst: Boolean = false,
) {
    val colors = LrTheme.colors
    val typed = remember { Animatable(0f) }
    val body = lines.joinToString("\n")
    LaunchedEffect(header, headline) {
        typed.animateTo(body.length.toFloat(), tween(durationMillis = (body.length * 18).coerceIn(200, 1200), delayMillis = 350))
    }
    FxScrim(onTap = onDone) {
        if (burst) {
            RingBurst(color = Color.White)
            ShardBurst(color = colors.accent)
        }
        SystemWindowAppear(Modifier.padding(24.dp)) {
            DialogWindow(
                title = header,
                modifier = Modifier.widthIn(max = 330.dp),
                footer = { ConfirmCancelRow(onConfirm = onDone, onCancel = onDone, cancelLabel = "닫기") },
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        headline,
                        style = LrTheme.type.number.copy(fontSize = 26.sp),
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                    if (body.isNotEmpty()) {
                        Text(
                            body.take(typed.value.toInt()),
                            style = LrTheme.type.body,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
