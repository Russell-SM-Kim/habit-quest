package com.lifereset.feature.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifereset.core.data.auth.AccountManager
import com.lifereset.core.data.repository.SettingsRepository
import com.lifereset.core.designsystem.component.IconDisc
import com.lifereset.core.designsystem.component.LrField
import com.lifereset.core.designsystem.component.LrPanel
import com.lifereset.core.designsystem.component.LrTextField
import com.lifereset.core.designsystem.component.LrWideButton
import com.lifereset.core.designsystem.icon.LrIcons
import com.lifereset.core.designsystem.theme.LrTheme
import com.lifereset.core.designsystem.theme.SkyTextShadow

@Composable
fun WelcomeRoute(
    settings: SettingsRepository,
    accounts: AccountManager,
    viewModel: WelcomeViewModel = viewModel(factory = WelcomeViewModel.factory(settings, accounts)),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    WelcomeScreen(
        ui = ui,
        onSignIn = { viewModel.signIn(context) },
        onSkip = viewModel::continueWithoutAccount,
        onServerUrlChange = viewModel::editServerUrl,
        onToggleServerField = viewModel::toggleServerField,
    )
}

/**
 * 처음 켰을 때 한 번만 나오는 화면.
 *  - Google 계정으로 시작하기 → 로그인 + 서버 기록과 합치기 → 바로 오늘 화면
 *  - 로그인 없이 시작하기 → 이 폰에만 저장 (설정에서 언제든 로그인 가능)
 */
@Composable
fun WelcomeScreen(
    ui: WelcomeUiState,
    onSignIn: () -> Unit,
    onSkip: () -> Unit,
    onServerUrlChange: (String) -> Unit,
    onToggleServerField: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LrTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        Text("Habit Quest", style = LrTheme.type.hero.copy(shadow = SkyTextShadow), color = colors.onSky)
        Text(
            "66일 시즌 · 끝없는 레벨업",
            style = LrTheme.type.bodyStrong.copy(shadow = SkyTextShadow),
            color = colors.onSky,
            modifier = Modifier.padding(top = 6.dp),
        )
        Spacer(Modifier.height(32.dp))

        LrPanel(
            title = "WELCOME",
            modifier = Modifier.widthIn(max = 440.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Feature(LrIcons.Check, "퀘스트는 한 번만 만들면 매일 자동으로", "할 일은 탭 한 번")
                Feature(LrIcons.Bolt, "한 만큼 캐릭터가 자란다", "레벨업 때 능력치가 활동 비율대로 오름")
                Feature(LrIcons.Sync, "Google 계정으로 어디서나", "폰을 바꿔도 기록이 이어지고, 알아서 동기화")

                Spacer(Modifier.height(6.dp))

                if (ui.showServerField) {
                    LrField("서버 주소", hint = "앱을 받은 곳에서 알려준 https:// 주소") {
                        LrTextField(ui.serverUrl, onServerUrlChange, placeholder = "https://")
                    }
                }

                LrWideButton(
                    text = if (ui.busy) "로그인 중…" else "Google 계정으로 시작하기",
                    onClick = onSignIn,
                    icon = LrIcons.User,
                    filled = true,
                )
                if (ui.busy) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(color = colors.iconDisc, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    }
                }
                ui.message?.let {
                    Text(it, style = LrTheme.type.caption, color = colors.danger)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        SkyLink("로그인 없이 시작하기 (이 폰에만 저장)", onSkip)
        if (!ui.showServerField) {
            SkyLink("서버 주소 바꾸기", onToggleServerField, Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun Feature(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        IconDisc(icon = icon)
        Column {
            Text(title, style = LrTheme.type.rowTitle, color = LrTheme.colors.textPrimary)
            Text(subtitle, style = LrTheme.type.caption, color = LrTheme.colors.textSecondary)
        }
    }
}

/** 하늘 위 흰 글씨 링크. */
@Composable
private fun SkyLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text,
        style = LrTheme.type.body.copy(shadow = SkyTextShadow),
        color = LrTheme.colors.onSky,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}
