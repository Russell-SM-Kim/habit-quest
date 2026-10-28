package com.lifereset.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lifereset.core.designsystem.theme.LrTheme
import com.lifereset.core.designsystem.theme.SkyTextShadow

/**
 * 화면 맨 위, 배경 하늘 위에 바로 놓이는 큰 흰 글씨.
 * 예: "Day 23" + "/ 66", 아래에 작은 설명, 그 아래 흰 진행 막대.
 */
@Composable
fun HeroHeader(
    title: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    caption: String? = null,
    progress: Float? = null,
    end: (@Composable () -> Unit)? = null,
) {
    val colors = LrTheme.colors
    Column(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                title,
                style = LrTheme.type.hero.copy(shadow = SkyTextShadow),
                color = colors.onSky,
                modifier = Modifier.semantics { heading() },
            )
            if (suffix != null) {
                Text(
                    " $suffix",
                    style = LrTheme.type.number.copy(shadow = SkyTextShadow),
                    color = colors.onSky.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) { end?.invoke() }
        }
        if (caption != null) {
            Text(caption, style = LrTheme.type.caption.copy(shadow = SkyTextShadow), color = colors.onSky)
        }
        if (progress != null) SkyProgress(progress)
    }
}

/** 하늘 위 작은 흰 글씨 (패널 사이 소제목 등). */
@Composable
fun SkyLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = LrTheme.type.bodyStrong.copy(shadow = SkyTextShadow),
        color = LrTheme.colors.onSky,
        modifier = modifier,
    )
}
