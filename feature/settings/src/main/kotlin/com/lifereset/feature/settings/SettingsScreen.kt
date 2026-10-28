package com.lifereset.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifereset.core.data.repository.SettingsRepository
import com.lifereset.core.data.sync.SyncManager
import com.lifereset.core.designsystem.component.HeroHeader
import com.lifereset.core.designsystem.component.LrField
import com.lifereset.core.designsystem.component.LrPanel
import com.lifereset.core.designsystem.component.LrTextField
import com.lifereset.core.designsystem.component.LrWideButton
import com.lifereset.core.designsystem.component.SwitchRow
import com.lifereset.core.designsystem.component.SystemDialog
import com.lifereset.core.designsystem.icon.LrIcons
import com.lifereset.core.designsystem.theme.LrTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsRoute(
    settings: SettingsRepository,
    sync: SyncManager,
    onOpenQuests: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(settings, sync)),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val status by viewModel.syncStatus.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val gameStart by viewModel.gameStart.collectAsStateWithLifecycle()
    val backgroundUri by viewModel.backgroundUri.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    var confirmRestore by remember { mutableStateOf(false) }
    val colors = LrTheme.colors
    val context = LocalContext.current

    // 사진 고르기. 앱을 다시 켜도 읽을 수 있도록 영구 읽기 권한을 받아 둔다.
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            viewModel.setBackground(uri.toString())
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(LrTheme.Dimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeroHeader(title = "Settings", caption = "게임 시작일 ${gameStart ?: "-"} (시즌 1 Day 1)")

        LrPanel(title = "PLAYER", contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LrField("이름") {
                    LrTextField(form.name, { v -> viewModel.edit { it.copy(name = v) } })
                }
                LrWideButton("이름 저장", onClick = { viewModel.saveName() }, icon = LrIcons.Check)
            }
        }

        LrWideButton("퀘스트 관리", onClick = onOpenQuests, icon = LrIcons.List)

        LrPanel(title = "DISPLAY · SOUND", contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LrWideButton("배경 이미지 고르기", onClick = { pickImage.launch(arrayOf("image/*")) }, icon = LrIcons.Edit)
                if (backgroundUri != null) {
                    LrWideButton("기본 하늘 배경으로", onClick = { viewModel.setBackground(null) })
                }
                SwitchRow(
                    title = "효과음",
                    subtitle = "퀘스트 클리어·레벨업 소리 (무음 모드에선 안 남)",
                    checked = soundEnabled,
                    onCheckedChange = viewModel::setSound,
                )
            }
        }

        LrPanel(title = "SERVER", contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LrField("서버 주소", hint = "Tailscale 주소. 예: https://내PC이름.tailnet이름.ts.net") {
                    LrTextField(form.serverUrl, { v -> viewModel.edit { it.copy(serverUrl = v) } }, placeholder = "https://")
                }
                LrField("API 키", hint = "PC 의 start-server.bat 에 넣은 키와 같아야 합니다") {
                    LrTextField(form.apiKey, { v -> viewModel.edit { it.copy(apiKey = v) } })
                }
                LrWideButton("저장하고 연결 확인", onClick = { viewModel.saveServer() }, filled = true, icon = LrIcons.Confirm)
                LrWideButton("지금 동기화", onClick = { viewModel.syncNow() }, icon = LrIcons.Sync)
                LrWideButton("서버에서 복원 (새 폰·재설치)", onClick = { confirmRestore = true })

                val last = status.lastSyncAt?.let {
                    SimpleDateFormat("M월 d일 HH:mm", Locale.KOREA).format(Date(it))
                } ?: "없음"
                Text(
                    "마지막 동기화: $last · ${status.message}",
                    style = LrTheme.type.caption,
                    color = if (status.ok) colors.textSecondary else colors.danger,
                )
                if (busy) Text("처리 중…", style = LrTheme.type.caption, color = colors.textSecondary)
                message?.let { Text(it, style = LrTheme.type.bodyStrong, color = colors.textPrimary) }
            }
        }
    }

    if (confirmRestore) {
        SystemDialog(
            title = "RESTORE",
            onDismiss = { confirmRestore = false },
            onConfirm = {
                confirmRestore = false
                viewModel.restore()
            },
            confirmLabel = "복원",
        ) {
            Text(
                "서버에 있는 기록으로 이 폰의 기록을 덮어씁니다. 서버에 없는 이 폰만의 기록은 다음 동기화 때 올라갑니다.",
                style = LrTheme.type.body,
                color = colors.textPrimary,
            )
        }
    }
}
