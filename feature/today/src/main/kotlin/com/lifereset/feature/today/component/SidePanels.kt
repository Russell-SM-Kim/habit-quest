package com.lifereset.feature.today.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifereset.core.designsystem.component.LrPanel
import com.lifereset.core.designsystem.component.LrWideButton
import com.lifereset.core.designsystem.component.SkewGauge
import com.lifereset.core.designsystem.theme.LrTheme
import com.lifereset.core.game.model.BossState
import com.lifereset.core.game.rules.RecoveryQuest

/** 주간 보스 HP. 운동 퀘스트 1회 = 데미지 1. */
@Composable
internal fun BossPanel(boss: BossState) {
    val colors = LrTheme.colors
    LrPanel(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.width(112.dp)) {
                Text("WEEKLY BOSS", style = LrTheme.type.code.copy(fontSize = LrTheme.type.caption.fontSize), color = colors.danger)
                Text(boss.name, style = LrTheme.type.bodyStrong, color = colors.textPrimary)
            }
            SkewGauge(
                progress = boss.hpLeft.toFloat() / boss.maxHp,
                color = colors.danger,
                height = 8.dp,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (boss.defeated) "처치" else "HP ${boss.hpLeft}/${boss.maxHp}",
                style = LrTheme.type.numberSmall,
                color = colors.textPrimary,
            )
        }
    }
}

/** 어제 연속 기록이 끊겼을 때만 나오는 회복 퀘스트. */
@Composable
internal fun RecoveryPanel(onComplete: () -> Unit) {
    val colors = LrTheme.colors
    val quest = RecoveryQuest.definition(java.time.LocalDate.now())
    LrPanel(title = "RECOVERY", contentPadding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("어제 연속 기록이 끊겼습니다. 오늘 안에 완료하면 되살아납니다.", style = LrTheme.type.body, color = colors.textPrimary)
            Text(quest.note, style = LrTheme.type.caption, color = colors.textSecondary)
            LrWideButton(text = "회복 퀘스트 완료", onClick = onComplete, filled = true)
        }
    }
}
