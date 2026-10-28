package com.lifereset.feature.today.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import com.lifereset.core.designsystem.component.HeroHeader
import com.lifereset.core.designsystem.component.LrPanel
import com.lifereset.core.designsystem.component.SkewGauge
import com.lifereset.core.designsystem.icon.LrIcons
import com.lifereset.core.designsystem.theme.LrTheme
import com.lifereset.core.designsystem.theme.SkyTextShadow
import com.lifereset.core.game.model.PlayerState

/**
 * 화면 맨 위.
 * ① 하늘 위 큰 글씨: Day 23 / 66 + 시즌 진행 막대
 * ② 유리창: 레벨 원, 이름, 클래스·랭크, EXP 게이지
 */
@Composable
internal fun PlayerHeader(player: PlayerState, name: String, syncLabel: String? = null) {
    val colors = LrTheme.colors
    val season = player.season
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HeroHeader(
            title = "Day ${season.day}",
            suffix = "/ ${season.seasonLength}",
            caption = listOfNotNull("Season ${season.season} · ${season.daysLeft}일 남음", syncLabel).joinToString(" · "),
            progress = season.progress,
            end = { StreakBadge(player.streak, player.shields) },
        )
        LrPanel(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LevelBadge(player.level)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Text(name, style = LrTheme.type.rowTitle, color = colors.textPrimary)
                        Text(
                            "${player.playerClass.label} · ${player.rankLabel} 랭크",
                            style = LrTheme.type.caption,
                            color = colors.textSecondary,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("EXP", style = LrTheme.type.code.copy(fontSize = 11.sp), color = colors.textSecondary)
                        SkewGauge(progress = player.xpProgress, modifier = Modifier.weight(1f))
                        Text("${player.xpInLevel}/${player.xpToNext}", style = LrTheme.type.numberSmall, color = colors.textPrimary)
                    }
                }
            }
        }
    }
}

/** 하늘 위 연속 기록 표시: 불꽃 + n일, 보호권 개수. */
@Composable
private fun StreakBadge(streak: Int, shields: Int) {
    val colors = LrTheme.colors
    Row(
        Modifier.padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(LrIcons.Flame, contentDescription = null, tint = colors.onSky, modifier = Modifier.size(18.dp))
        Text("${streak}일", style = LrTheme.type.numberSmall.copy(fontSize = 16.sp, shadow = SkyTextShadow), color = colors.onSky)
        if (shields > 0) {
            Icon(LrIcons.Shield, contentDescription = "보호권", tint = colors.onSky, modifier = Modifier.padding(start = 6.dp).size(15.dp))
            Text("$shields", style = LrTheme.type.numberSmall.copy(shadow = SkyTextShadow), color = colors.onSky)
        }
    }
}

/** 진회색 원 + 흰 링 안의 레벨 숫자. */
@Composable
private fun LevelBadge(level: Int) {
    val colors = LrTheme.colors
    Box(
        Modifier
            .size(50.dp)
            .border(2.dp, Color(0xE6FFFFFF), CircleShape)
            .padding(2.dp)
            .clip(CircleShape)
            .background(colors.iconDisc),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("LV", style = LrTheme.type.code.copy(fontSize = 9.sp), color = colors.onIconDisc)
            Text("$level", style = LrTheme.type.number.copy(fontSize = 18.sp, lineHeight = 18.sp), color = colors.onIconDisc)
        }
    }
}
