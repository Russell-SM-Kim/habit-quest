package com.lifereset.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifereset.core.designsystem.icon.LrIcons
import com.lifereset.core.designsystem.theme.LrTheme
import com.lifereset.core.designsystem.theme.SkyTextShadow

/**
 * 원형 메뉴 버튼: 흰 테두리 링 + 연회색 원 + 회색 아이콘.
 * selected = true 면 금색으로 채워지고 은은하게 빛난다.
 * label 은 배경 위에 놓이므로 흰 글자 + 그림자.
 */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    size: Dp = LrTheme.Dimens.circleButton,
    label: String? = null,
) {
    val colors = LrTheme.colors
    val ring = size + 10.dp
    Column(
        modifier = modifier.clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(ring)
                .then(if (selected) Modifier.shadow(10.dp, CircleShape, spotColor = colors.accent, ambientColor = colors.accent) else Modifier)
                .border(2.dp, if (selected) colors.accent else colors.circleRing, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(if (selected) colors.accent else colors.circle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = if (label == null) contentDescription else null,
                    tint = if (selected) colors.onAccent else colors.onCircle,
                    modifier = Modifier.size(size * 0.48f),
                )
            }
        }
        if (label != null) {
            Text(
                text = label,
                style = LrTheme.type.caption.copy(
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    shadow = SkyTextShadow,
                ),
                color = colors.onSky,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
    }
}

data class NavEntry(val label: String, val icon: ImageVector)

/** 접었을 때: 화면 아래 원형 메뉴. 배경이 비쳐 보이도록 따로 바탕을 깔지 않는다. */
@Composable
fun LrBottomBar(
    entries: List<NavEntry>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(top = 6.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        entries.forEachIndexed { index, entry ->
            CircleIconButton(
                icon = entry.icon,
                contentDescription = entry.label,
                onClick = { onSelect(index) },
                selected = index == selectedIndex,
                label = entry.label,
            )
        }
    }
}

/** 펼쳤을 때: 화면 왼쪽 세로 원형 메뉴. */
@Composable
fun LrNavRail(
    entries: List<NavEntry>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxHeight()
            .width(104.dp)
            .statusBarsPadding()
            .padding(top = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        entries.forEachIndexed { index, entry ->
            CircleIconButton(
                icon = entry.icon,
                contentDescription = entry.label,
                onClick = { onSelect(index) },
                selected = index == selectedIndex,
                size = 56.dp,
                label = entry.label,
            )
        }
    }
}

/** 가로로 긴 메뉴형 버튼: 연회색 줄 + 진회색 원형 아이콘. */
@Composable
fun LrWideButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    filled: Boolean = false,
) {
    val colors = LrTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .background(if (filled) colors.iconDisc else colors.row)
            .border(1.dp, colors.rowBorder)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            IconDisc(icon = icon, inverted = filled)
            Spacer(Modifier.width(12.dp))
        } else {
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text,
            style = LrTheme.type.rowTitle,
            color = if (filled) colors.onIconDisc else colors.textPrimary,
        )
    }
}

/** 진회색 원 안의 흰 아이콘 (메뉴 줄 앞머리). inverted = 흰 원 + 회색 아이콘. */
@Composable
fun IconDisc(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    inverted: Boolean = false,
    size: Dp = LrTheme.Dimens.iconDisc,
) {
    val colors = LrTheme.colors
    Box(
        modifier
            .size(size)
            .border(2.dp, Color(0xE6FFFFFF), CircleShape)
            .padding(1.dp)
            .clip(CircleShape)
            .background(if (inverted) Color.White else colors.iconDisc),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (inverted) colors.iconDisc else colors.onIconDisc, modifier = Modifier.size(size * 0.5f))
    }
}

/** 다이얼로그 하단: 파란 ○(확인) / 빨간 ✕(취소). onConfirm 이 null 이면 ✕ 만. */
@Composable
fun ConfirmCancelRow(
    onConfirm: (() -> Unit)?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    confirmLabel: String = "확인",
    cancelLabel: String = "취소",
) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(72.dp, Alignment.CenterHorizontally),
    ) {
        if (onConfirm != null) {
            DialogCircle(color = LrTheme.colors.confirm, icon = LrIcons.Confirm, label = confirmLabel, onClick = onConfirm)
        }
        DialogCircle(color = LrTheme.colors.cancel, icon = LrIcons.Close, label = cancelLabel, onClick = onCancel)
    }
}

@Composable
private fun DialogCircle(color: Color, icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(58.dp)
            .shadow(6.dp, CircleShape, spotColor = color, ambientColor = color)
            .clip(CircleShape)
            .background(Color.White)
            .border(3.dp, color, CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}
