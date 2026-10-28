package com.lifereset.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifereset.core.designsystem.icon.LrIcons
import com.lifereset.core.designsystem.theme.LrTheme
import com.lifereset.core.designsystem.theme.SkyTextShadow

/**
 * 퀘스트 한 줄 (연회색 메뉴 줄 + 진회색 원형 아이콘).
 * highlighted = 다음에 할 퀘스트 (금색 줄, 한 화면에 하나만).
 *
 * 완료되는 순간: ① 아이콘이 튕기며 체크로 바뀌고 ② 줄이 한 번 번쩍이고 ③ 취소선이 왼쪽에서 그어진다.
 * 길게 누르면 onLongClick (기록 추가·취소).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuestRow(
    title: String,
    subtitle: String,
    trailing: String,
    done: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    code: String = "",
    highlighted: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val colors = LrTheme.colors
    val gold = highlighted && !done
    val check by animateFloatAsState(
        targetValue = if (done) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "check",
    )
    val strike by animateFloatAsState(
        targetValue = if (done) 1f else 0f,
        animationSpec = tween(durationMillis = 320, delayMillis = if (done) 120 else 0),
        label = "strike",
    )
    val flash = remember { Animatable(0f) }
    LaunchedEffect(done) {
        if (done) {
            flash.snapTo(1f)
            flash.animateTo(0f, tween(durationMillis = 650))
        }
    }

    val textColor = when {
        gold -> colors.onAccent
        done -> colors.textTertiary
        else -> colors.textPrimary
    }
    val titleStyle = if (gold) LrTheme.type.rowTitle.copy(shadow = SkyTextShadow) else LrTheme.type.rowTitle

    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = LrTheme.Dimens.rowHeight)
            .background(if (gold) colors.accent else colors.row)
            .border(1.dp, if (gold) colors.accent else colors.rowBorder)
            .drawBehind {
                if (flash.value > 0f) drawRect(colors.accent.copy(alpha = 0.55f * flash.value))
            }
            .combinedClickable(
                enabled = enabled,
                role = Role.Checkbox,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .semantics { stateDescription = if (done) "완료" else "미완료" }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ① 원형 아이콘: 할 일 = 능력치 코드, 완료 = 체크
        Box(
            Modifier
                .size(LrTheme.Dimens.iconDisc)
                .scale(if (done) 0.85f + 0.15f * check else 1f)
                .border(2.dp, Color(0xE6FFFFFF), CircleShape)
                .padding(1.dp)
                .clip(CircleShape)
                .background(if (gold) Color.White else colors.iconDisc),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                Icon(
                    LrIcons.Check,
                    contentDescription = null,
                    tint = colors.onIconDisc,
                    modifier = Modifier.size(16.dp).scale(check.coerceIn(0f, 1.2f)),
                )
            } else {
                Text(code, style = LrTheme.type.code.copy(fontSize = LrTheme.type.caption.fontSize * 0.92f), color = if (gold) colors.iconDisc else colors.onIconDisc)
            }
        }

        // ③ 제목 + 취소선
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = titleStyle,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.drawWithContent {
                    drawContent()
                    if (strike > 0f) {
                        val y = size.height / 2f
                        drawLine(colors.textTertiary, Offset(0f, y), Offset(size.width * strike, y), 1.5.dp.toPx())
                    }
                },
            )
            if (subtitle.isNotEmpty()) {
                Text(subtitle, style = LrTheme.type.caption.copy(fontSize = LrTheme.type.caption.fontSize * 0.92f), color = if (gold) colors.onAccent else colors.textTertiary, maxLines = 2)
            }
        }

        Text(
            text = if (done) "Clear" else trailing,
            style = LrTheme.type.numberSmall,
            color = textColor,
        )
    }
}
