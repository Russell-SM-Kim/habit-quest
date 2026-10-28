package com.lifereset.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lifereset.core.designsystem.theme.LrTheme

/**
 * 알림 창: 흰 창 + 가운데 제목 + 회색 띠 본문 + 파란 ○ / 빨간 ✕ 원형 버튼.
 * onConfirm 을 null 로 주면 ✕ 만 보인다.
 */
@Composable
fun SystemDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (() -> Unit)?,
    confirmLabel: String = "확인",
    dismissLabel: String = "취소",
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        SystemWindowAppear {
            DialogWindow(
                title = title,
                footer = {
                    ConfirmCancelRow(
                        onConfirm = onConfirm,
                        onCancel = onDismiss,
                        confirmLabel = confirmLabel,
                        cancelLabel = dismissLabel,
                    )
                },
            ) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), content = content)
            }
        }
    }
}

/** 다이얼로그와 연출 창이 공유하는 흰 창 틀. body 는 회색 띠 안에 들어간다. */
@Composable
fun DialogWindow(
    title: String,
    modifier: Modifier = Modifier,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    body: @Composable ColumnScope.() -> Unit,
) {
    val colors = LrTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .shadow(16.dp, spotColor = Color(0x661E283C))
            .background(Color(0xF7F8F8F9))
            .border(1.dp, Color.White),
    ) {
        Text(
            title,
            style = LrTheme.type.panelTitle.copy(fontSize = LrTheme.type.panelTitle.fontSize * 1.1f),
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 14.dp).semantics { heading() },
        )
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFFC9CCD1))
                .border(1.dp, Color(0xFFB4B8BE)),
            content = body,
        )
        footer?.invoke(this)
    }
}
