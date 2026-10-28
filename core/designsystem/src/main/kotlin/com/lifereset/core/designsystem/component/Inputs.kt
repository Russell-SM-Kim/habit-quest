package com.lifereset.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifereset.core.designsystem.theme.LrTheme

/*
 * 폼 컴포넌트. 선택된 값은 진회색으로 채운다 (금색은 화면의 '현재 위치'에만 쓰기 때문).
 */

/** 라벨 + 내용 묶음. 폼의 한 칸. */
@Composable
fun LrField(
    label: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = LrTheme.type.bodyStrong.copy(fontSize = LrTheme.type.caption.fontSize), color = LrTheme.colors.textSecondary)
        content()
        if (hint != null) Text(hint, style = LrTheme.type.caption, color = LrTheme.colors.textSecondary)
    }
}

@Composable
fun LrTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    number: Boolean = false,
    singleLine: Boolean = true,
) {
    val colors = LrTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        placeholder = { Text(placeholder, color = colors.textTertiary) },
        shape = RectangleShape,
        textStyle = if (number) LrTheme.type.numberSmall.copy(fontSize = LrTheme.type.rowTitle.fontSize) else LrTheme.type.body,
        keyboardOptions = KeyboardOptions(keyboardType = if (number) KeyboardType.Decimal else KeyboardType.Text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.iconDisc,
            unfocusedBorderColor = colors.rowBorder,
            focusedContainerColor = colors.row,
            unfocusedContainerColor = colors.row,
            cursorColor = colors.textPrimary,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
        ),
    )
}

/** 붙어 있는 선택 버튼 줄 (단위, 난이도). */
@Composable
fun <T> SegmentedRow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    subLabel: ((T) -> String)? = null,
) {
    val colors = LrTheme.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            Column(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .background(if (isSelected) colors.iconDisc else colors.row)
                    .border(1.dp, colors.rowBorder)
                    .clickable(role = Role.Tab) { onSelect(option) }
                    .semantics { this.selected = isSelected }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    label(option),
                    style = LrTheme.type.body.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                    color = if (isSelected) colors.onIconDisc else colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
                if (subLabel != null) {
                    Text(
                        subLabel(option),
                        style = LrTheme.type.code,
                        color = if (isSelected) colors.onIconDisc else colors.textTertiary,
                    )
                }
            }
        }
    }
}

/** 켜고 끄는 칩 (능력치 선택 등). */
@Composable
fun ToggleChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LrTheme.colors
    Box(
        modifier
            .heightIn(min = 44.dp)
            .background(if (selected) colors.iconDisc else colors.row)
            .border(1.dp, colors.rowBorder)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = LrTheme.type.body, color = if (selected) colors.onIconDisc else colors.textPrimary)
    }
}

/** 요일 같은 원형 토글. */
@Composable
fun CircleToggle(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LrTheme.colors
    Box(
        modifier
            .size(44.dp)
            .border(2.dp, Color(0xE6FFFFFF), CircleShape)
            .padding(1.dp)
            .clip(CircleShape)
            .background(if (selected) colors.iconDisc else colors.row)
            .clickable(role = Role.Checkbox, onClick = onClick)
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = LrTheme.type.bodyStrong, color = if (selected) colors.onIconDisc else colors.textSecondary)
    }
}

/** 켜기/끄기 스위치 줄 (설정). */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val colors = LrTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .background(colors.row)
            .border(1.dp, colors.rowBorder)
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .semantics { this.selected = checked }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = LrTheme.type.rowTitle, color = colors.textPrimary)
            if (subtitle != null) Text(subtitle, style = LrTheme.type.caption, color = colors.textSecondary)
        }
        Box(
            Modifier
                .size(width = 48.dp, height = 28.dp)
                .clip(CircleShape)
                .background(if (checked) colors.iconDisc else colors.track)
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(Modifier.size(22.dp).clip(CircleShape).background(Color.White))
        }
    }
}
