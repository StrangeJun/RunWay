package com.runway.wear.data

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

class PhoneStateListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != WatchPaths.STATE) return
        PhoneRunStateRepository.update(messageEvent.data)
    }
}
