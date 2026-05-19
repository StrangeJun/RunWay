package com.runway.android.core.tracking

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * GPS 추적이 진행되는 동안 ForegroundService를 유지한다.
 * - 실제 GPS 수집 및 상태 관리는 RunTrackingManager(appScope)에서 처리한다.
 * - 이 서비스는 알림 표시와 프로세스 유지만 담당한다.
 *
 * NOTE: 에뮬레이터에서는 Extended Controls → Location에서 mock location을 전송해야
 *       GPS 신호가 수신된다. 물리 기기에서는 실제 GPS가 사용된다.
 */
@AndroidEntryPoint
class RunTrackingService : Service() {

    @Inject lateinit var manager: RunTrackingManager
    @Inject lateinit var notification: RunTrackingNotification

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var notificationUpdateJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            RunTrackingAction.ACTION_START -> handleStart(intent)
            RunTrackingAction.ACTION_STOP  -> handleStop()
        }
        return START_NOT_STICKY
    }

    private fun handleStart(intent: Intent) {
        val mode = intent.getStringExtra(RunTrackingAction.EXTRA_MODE)
            ?.let { runCatching { RunTrackingMode.valueOf(it) }.getOrNull() }
            ?: RunTrackingMode.FREE_RUN

        val initialNotification = notification.build(RunTrackingState(mode = mode))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                RunTrackingNotification.NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } else {
            startForeground(RunTrackingNotification.NOTIFICATION_ID, initialNotification)
        }

        manager.start(mode)
        startNotificationUpdates()
    }

    private fun handleStop() {
        notificationUpdateJob?.cancel()
        // manager.stop() is called by the ViewModel before sending ACTION_STOP,
        // but we call it here too as a safety net.
        manager.stop()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startNotificationUpdates() {
        notificationUpdateJob?.cancel()
        notificationUpdateJob = serviceScope.launch {
            while (true) {
                delay(5_000L)
                val state = manager.state.value
                if (state.isTracking) {
                    notification.update(state)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
