package com.lifereset.wear.data

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.lifereset.core.protocol.ProtocolJson
import com.lifereset.core.protocol.wear.WearCompleteRequest
import com.lifereset.core.protocol.wear.WearPaths
import com.lifereset.core.protocol.wear.WearSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * 폰과의 연결 한 곳에 모음.
 * - 받기: 폰이 올려둔 DataItem(/life-reset/state) 을 읽고, 바뀔 때마다 [snapshot] 갱신
 * - 보내기: 완료 요청 / 새로고침 요청을 연결된 폰(노드)에 메시지로 보냄
 */
class PhoneLink(context: Context) {
    private val dataClient = Wearable.getDataClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    private val _snapshot = MutableStateFlow<WearSnapshot?>(null)
    val snapshot: StateFlow<WearSnapshot?> = _snapshot.asStateFlow()

    private val listener = DataClient.OnDataChangedListener { events: DataEventBuffer ->
        events.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == WearPaths.STATE_PATH) {
                decode(event.dataItem)?.let { _snapshot.value = it }
            }
        }
    }

    fun start() {
        dataClient.addListener(listener)
    }

    fun stop() {
        dataClient.removeListener(listener)
    }

    /** 워치 앱을 켰을 때: 마지막으로 받아둔 상태부터 읽는다. */
    suspend fun loadLatest() {
        val buffer = dataClient.getDataItems(Uri.parse("wear://*${WearPaths.STATE_PATH}")).await()
        try {
            buffer.mapNotNull(::decode).maxByOrNull { it.generatedAt }?.let { _snapshot.value = it }
        } finally {
            buffer.release()
        }
    }

    /** @return 연결된 폰이 하나라도 있어서 보냈으면 true */
    suspend fun sendComplete(questId: String): Boolean =
        send(WearPaths.COMPLETE_PATH, ProtocolJson.encodeToString(WearCompleteRequest.serializer(), WearCompleteRequest(questId)))

    suspend fun requestRefresh(): Boolean = send(WearPaths.REFRESH_PATH, "")

    private suspend fun send(path: String, body: String): Boolean {
        val nodes = nodeClient.connectedNodes.await()
        nodes.forEach { node -> messageClient.sendMessage(node.id, path, body.encodeToByteArray()).await() }
        return nodes.isNotEmpty()
    }

    private fun decode(item: DataItem): WearSnapshot? = runCatching {
        val json = DataMapItem.fromDataItem(item).dataMap.getString(WearPaths.STATE_KEY) ?: return null
        ProtocolJson.decodeFromString(WearSnapshot.serializer(), json)
    }.getOrNull()
}
