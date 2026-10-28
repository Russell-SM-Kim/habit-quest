package com.lifereset.wear.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.lifereset.core.protocol.wear.WearQuest
import com.lifereset.core.protocol.wear.WearSnapshot
import kotlinx.coroutines.delay

/**
 * 워치 화면 하나:
 *  ┌ 시계
 *  │ Lv 12 · C랭크 · 🔥5
 *  │ EXP 막대 (둥근 테두리)
 *  │ Day 23 / 66
 *  │ [퀘스트 칩들] ← 탭하면 폰으로 완료 요청
 *  └ [새로고침]
 */
@Composable
fun WearApp(viewModel: WearViewModel) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    var levelUp by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is WearEvent.LevelUp -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    levelUp = event.level
                }
                WearEvent.Cleared -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    WearTheme {
        val listState = rememberScalingLazyListState()
        Scaffold(
            timeText = { TimeText() },
            positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
        ) {
            val snapshot = ui.snapshot
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                if (snapshot != null) {
                    CircularProgressIndicator(
                        progress = snapshot.xpInLevel.toFloat() / snapshot.xpToNext.coerceAtLeast(1),
                        modifier = Modifier.fillMaxSize().padding(2.dp),
                        indicatorColor = WearColors.exp,
                        trackColor = WearColors.row,
                        strokeWidth = 4.dp,
                    )
                }
                ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    item { Header(snapshot) }
                    if (ui.phoneMissing) {
                        item {
                            Text(
                                "폰과 연결되지 않았어요",
                                color = WearColors.textDim,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (snapshot != null) {
                        items(snapshot.quests, key = { it.slotKey }) { quest ->
                            QuestChip(
                                quest = quest,
                                waiting = quest.questId in ui.pending,
                                onClick = { viewModel.complete(quest.questId) },
                            )
                        }
                        if (snapshot.quests.isEmpty()) {
                            item { Text("오늘은 휴식일", color = WearColors.textDim, fontSize = 13.sp) }
                        }
                    }
                    item {
                        Chip(
                            label = { Text("새로고침") },
                            onClick = viewModel::refresh,
                            colors = ChipDefaults.secondaryChipColors(),
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                LevelUpFlash(level = levelUp, onDone = { levelUp = null })
            }
        }
    }
}

@Composable
private fun Header(snapshot: WearSnapshot?) {
    Column(
        Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (snapshot == null) {
            Text("폰에서 앱을 한 번 열어주세요", color = WearColors.textDim, fontSize = 12.sp, textAlign = TextAlign.Center)
            return@Column
        }
        Text("LV ${snapshot.level}", color = WearColors.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "${snapshot.playerClass} · ${snapshot.rank} · 🔥${snapshot.streak}",
            color = WearColors.textDim,
            fontSize = 11.sp,
        )
        Text(
            "Day ${snapshot.seasonDay} / ${snapshot.seasonLength} · ${snapshot.quests.count { it.done }}/${snapshot.quests.size}",
            color = WearColors.textDim,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun QuestChip(quest: WearQuest, waiting: Boolean, onClick: () -> Unit) {
    val done = quest.done || waiting
    Chip(
        onClick = onClick,
        enabled = !done,
        modifier = Modifier.fillMaxWidth(),
        colors = ChipDefaults.chipColors(
            backgroundColor = WearColors.row,
            contentColor = WearColors.text,
            secondaryContentColor = WearColors.textDim,
        ),
        icon = {
            Box(
                Modifier.size(24.dp).clip(CircleShape).background(if (done) WearColors.accent else WearColors.disc),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (done) "✓" else "", color = if (done) Color.White else WearColors.onDisc, fontSize = 13.sp)
            }
        },
        label = { Text(quest.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = if (quest.detail.isNotEmpty()) {
            { Text(quest.detail, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        } else {
            null
        },
    )
}

/** 레벨업 순간 화면 가운데 금색 원이 1.8초 동안 떴다 사라진다. */
@Composable
private fun LevelUpFlash(level: Int?, onDone: () -> Unit) {
    LaunchedEffect(level) {
        if (level != null) {
            delay(1_800)
            onDone()
        }
    }
    AnimatedVisibility(
        visible = level != null,
        enter = fadeIn() + scaleIn(initialScale = 0.6f),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(Modifier.fillMaxSize().background(Color(0xCC000000)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("LEVEL UP", color = WearColors.accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.height(4.dp))
                Row { Text("LV ${level ?: ""}", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
