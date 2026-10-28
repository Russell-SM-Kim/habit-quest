package com.lifereset.app.wear

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.lifereset.core.data.DataContainer
import com.lifereset.core.protocol.ProtocolJson
import com.lifereset.core.protocol.wear.WearPaths
import com.lifereset.core.protocol.wear.toWearSnapshot
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString

/**
 * 게임 상태가 바뀔 때마다 워치로 요약본(WearSnapshot)을 보낸다.
 * DataItem 으로 보내므로 워치가 꺼져 있어도 다음에 켜질 때 최신 값을 받는다.
 */
class WearStatePublisher(
    context: Context,
    private val container: DataContainer,
) {
    private val dataClient = Wearable.getDataClient(context.applicationContext)

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    fun start() {
        container.appScope.launch {
            container.gameRepository.state
                .filterNotNull()
                .debounce(300) // 연달아 바뀌면 마지막 것만
                .collect { publish() }
        }
    }

    /** 워치가 새로고침을 요청했을 때도 부른다. */
    suspend fun publish() {
        val state = container.gameRepository.state.value ?: return
        val snapshot = state.toWearSnapshot(container.time.nowMillis())
        val request = PutDataMapRequest.create(WearPaths.STATE_PATH).apply {
            dataMap.putString(WearPaths.STATE_KEY, ProtocolJson.encodeToString(snapshot))
        }.asPutDataRequest().setUrgent()
        runCatching { dataClient.putDataItem(request).await() }
            .onFailure { Log.w(TAG, "워치로 상태를 보내지 못했습니다 (워치 미연결이면 정상)", it) }
    }

    private companion object {
        const val TAG = "WearStatePublisher"
    }
}
