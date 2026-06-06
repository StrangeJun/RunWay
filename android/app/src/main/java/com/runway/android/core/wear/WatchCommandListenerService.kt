package com.runway.android.core.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WatchCommandListenerService : WearableListenerService() {
    companion object {
        const val COMMAND_PATH = "/runway/watch/command"
    }

    @Inject lateinit var coordinator: WearRunSessionCoordinator

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != COMMAND_PATH) return
        WatchCommandParser.parse(messageEvent.data)?.let(coordinator::handle)
    }
}
