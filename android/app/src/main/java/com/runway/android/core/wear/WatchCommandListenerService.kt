package com.runway.android.core.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.runway.android.core.datastore.TokenDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import javax.inject.Inject

@AndroidEntryPoint
class WatchCommandListenerService : WearableListenerService() {
    companion object {
        const val COMMAND_PATH = "/runway/watch/command"
        const val AUTH_REQUEST_PATH = "/runway/watch/auth/request"
        const val AUTH_STATE_PATH = "/runway/watch/auth/state"
        const val RUN_UPLOAD_PATH = "/runway/watch/run-upload"
        const val RUN_UPLOAD_ACK_PATH = "/runway/watch/run-upload-ack"
    }

    @Inject lateinit var coordinator: WearRunSessionCoordinator
    @Inject lateinit var tokenDataStore: TokenDataStore
    @Inject lateinit var uploadCoordinator: WatchRunUploadCoordinator
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            COMMAND_PATH ->
                WatchCommandParser.parse(messageEvent.data)?.let(coordinator::handle)
            AUTH_REQUEST_PATH -> sendAuthState(messageEvent.sourceNodeId)
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents
            .filter { it.type == DataEvent.TYPE_CHANGED }
            .filter { it.dataItem.uri.path?.startsWith("$RUN_UPLOAD_PATH/") == true }
            .forEach { event ->
                val uri = event.dataItem.uri
                val asset = DataMapItem.fromDataItem(event.dataItem).dataMap.getAsset("run")
                    ?: return@forEach
                serviceScope.launch {
                    val descriptor = runCatching {
                        Wearable.getDataClient(this@WatchCommandListenerService)
                            .getFdForAsset(asset)
                            .await()
                    }.getOrNull() ?: return@launch
                    descriptor.inputStream.use { input ->
                        val payload = runCatching {
                            JSONObject(input.readBytes().decodeToString())
                        }.getOrNull() ?: return@launch
                        uploadCoordinator.upload(payload, uri)
                    }
                }
            }
    }

    private fun sendAuthState(nodeId: String) {
        serviceScope.launch {
            val payload = JSONObject()
                .put("isLoggedIn", tokenDataStore.accessTokenFlow.first() != null)
                .toString()
                .encodeToByteArray()
            runCatching {
                Wearable.getMessageClient(this@WatchCommandListenerService)
                    .sendMessage(nodeId, AUTH_STATE_PATH, payload)
                    .await()
            }
        }
    }
}
