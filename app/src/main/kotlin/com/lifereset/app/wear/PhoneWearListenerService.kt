package com.lifereset.app.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.lifereset.app.LifeResetApplication
import com.lifereset.core.protocol.ProtocolJson
import com.lifereset.core.protocol.wear.WearCompleteRequest
import com.lifereset.core.protocol.wear.WearPaths
import kotlinx.coroutines.launch

/**
 * 워치 -> 폰 메시지 수신. 앱이 꺼져 있어도 시스템이 이 서비스를 깨운다.
 * 완료 처리는 폰 화면에서 누른 것과 똑같이 GameRepository 로 들어간다.
 */
class PhoneWearListenerService : WearableListenerService() {

    override fun onMessageReceived(event: MessageEvent) {
        val app = application as LifeResetApplication
        val container = app.dataContainer
        when (event.path) {
            WearPaths.COMPLETE_PATH -> {
                val request = runCatching {
                    ProtocolJson.decodeFromString<WearCompleteRequest>(event.data.decodeToString())
                }.getOrNull() ?: return
                // 상태가 다시 계산되면 WearStatePublisher 가 알아서 워치로 보낸다
                container.appScope.launch {
                    container.gameRepository.complete(request.questId, request.amount)
                }
            }
            WearPaths.REFRESH_PATH -> container.appScope.launch { app.wearPublisher.publish() }
        }
    }
}
