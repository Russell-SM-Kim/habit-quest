package com.lifereset.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifereset.core.designsystem.theme.LrTheme

/** 왼쪽 위만 사선으로 잘린 창 모양. 컷 크기는 LrTheme.Dimens.panelCut. */
@Composable
fun panelShape(): Shape = CutCornerShape(topStart = LrTheme.Dimens.panelCut)

/**
 * 흰 반투명 유리창. 앱의 거의 모든 덩어리가 이 창 하나로 만들어진다.
 * 제목이 있으면 가운데 회색 제목 + 아래 얇은 선이 붙는다.
 */
@Composable
fun LrPanel(
    modifier: Modifier = Modifier,
    title: String? = null,
    headerEnd: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LrTheme.colors
    val shape = panelShape()
    Column(
        modifier
            .shadow(elevation = 8.dp, shape = shape, spotColor = Color(0x551E283C), ambientColor = Color(0x221E283C))
            .clip(shape)
            .background(colors.glass)
            .border(1.dp, colors.glassBorder, shape),
    ) {
        if (title != null) {
            Box(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                Text(
                    text = title,
                    style = LrTheme.type.panelTitle,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).semantics { heading() },
                )
                if (headerEnd != null) {
                    Box(Modifier.align(Alignment.CenterEnd).padding(bottom = 8.dp)) { headerEnd() }
                }
            }
            Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.5.dp).background(colors.rule))
        }
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/** 창 아래쪽 회색 띠 (요약 한 줄, 다이얼로그 본문). */
@Composable
fun PanelBand(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(LrTheme.colors.band)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        content = content,
    )
}

/** 연회색 작은 박스 (요약 숫자 카드 등). */
@Composable
fun LrCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .background(LrTheme.colors.row)
            .border(1.dp, LrTheme.colors.rowBorder)
            .padding(contentPadding),
        content = content,
    )
}

/**
 * 시스템 창이 열리는 연출: 가로선 한 줄이 펼쳐진 뒤 위아래로 열린다.
 * 다이얼로그와 레벨업 창이 모두 이걸로 감싼다.
 */
@Composable
fun SystemWindowAppear(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scaleX = remember { Animatable(0f) }
    val scaleY = remember { Animatable(0.03f) }
    LaunchedEffect(Unit) {
        scaleX.animateTo(1f, tween(durationMillis = 180, easing = FastOutSlowInEasing))
        scaleY.animateTo(1f, tween(durationMillis = 220, easing = FastOutSlowInEasing))
    }
    Box(
        modifier.graphicsLayer {
            this.scaleX = scaleX.value
            this.scaleY = scaleY.value
            alpha = (scaleX.value * 1.5f).coerceAtMost(1f)
        },
    ) { content() }
}
