package com.runway.wear.data

import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

data class PhoneRunState(
    val status: String,
    val error: String? = null,
)

object PhoneRunStateRepository {
    private val _state = MutableStateFlow<PhoneRunState?>(null)
    val state = _state.asStateFlow()

    fun update(payload: ByteArray) {
        val parsed = runCatching {
            val json = JSONObject(payload.decodeToString())
            PhoneRunState(
                status = json.getString("status"),
                error = json.optString("error").takeUnless { it.isBlank() || it == "null" },
            )
        }.getOrNull() ?: return
        _state.value = parsed
    }
}

object PhoneAuthStateRepository {
    data class Snapshot(val isLoggedIn: Boolean, val receivedAtNanos: Long)

    private val _state = MutableStateFlow<Snapshot?>(null)
    val state = _state.asStateFlow()

    fun update(payload: ByteArray) {
        val loggedIn = runCatching {
            JSONObject(payload.decodeToString()).getBoolean("isLoggedIn")
        }.getOrNull() ?: return
        _state.value = Snapshot(loggedIn, System.nanoTime())
    }
}

object WatchRunSyncRepository {
    private val _lastSyncedId = MutableStateFlow<String?>(null)
    val lastSyncedId = _lastSyncedId.asStateFlow()

    fun acknowledge(payload: ByteArray): String? {
        val localId = runCatching {
            JSONObject(payload.decodeToString()).getString("localId")
        }.getOrNull() ?: return null
        _lastSyncedId.value = localId
        return localId
    }
}

class PhoneStateListenerService : WearableListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WatchPaths.STATE -> PhoneRunStateRepository.update(messageEvent.data)
            WatchPaths.AUTH_STATE -> PhoneAuthStateRepository.update(messageEvent.data)
            WatchPaths.RUN_UPLOAD_ACK -> {
                WatchRunSyncRepository.acknowledge(messageEvent.data)?.let { localId ->
                    PendingWatchRunStore(this).remove(localId)
                }
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents
            .filter { it.type == DataEvent.TYPE_CHANGED }
            .filter { it.dataItem.uri.path == WatchPaths.COURSE_CATALOG }
            .forEach { event ->
                val asset = DataMapItem.fromDataItem(event.dataItem).dataMap.getAsset("courses")
                    ?: return@forEach
                serviceScope.launch {
                    val descriptor = runCatching {
                        Wearable.getDataClient(this@PhoneStateListenerService)
                            .getFdForAsset(asset)
                            .await()
                    }.getOrNull() ?: return@launch
                    val courses = descriptor.inputStream.use { input ->
                        val array = runCatching { JSONArray(input.readBytes().decodeToString()) }
                            .getOrNull() ?: return@launch
                        buildList {
                            for (index in 0 until array.length()) {
                                runCatching { OfflineCourse.fromJson(array.getJSONObject(index)) }
                                    .getOrNull()
                                    ?.let(::add)
                            }
                        }
                    }
                    OfflineCourseStore(this@PhoneStateListenerService).replace(courses)
                }
            }
    }

    override fun onPeerConnected(peer: Node) {
        serviceScope.launch {
            WatchDataLayerClient(this@PhoneStateListenerService)
                .syncPendingRuns(PendingWatchRunStore(this@PhoneStateListenerService))
        }
    }
}
