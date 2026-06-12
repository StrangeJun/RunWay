package com.runway.wear

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.ambient.AmbientLifecycleObserver
import com.runway.wear.ui.RunWayWearApp

class MainActivity : ComponentActivity() {
    private var launchToken by mutableIntStateOf(0)
    private var isAmbient by mutableStateOf(false)

    private val ambientObserver = AmbientLifecycleObserver(
        this,
        object : AmbientLifecycleObserver.AmbientLifecycleCallback {
            override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
                isAmbient = true
            }
            override fun onExitAmbient() {
                isAmbient = false
            }
            override fun onUpdateAmbient() {}
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(ambientObserver)
        // 앱이 포그라운드에 있는 동안 항상 화면 유지
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (savedInstanceState == null) launchToken++
        setContent {
            val watchViewModel: WatchViewModel = viewModel()
            val lifecycleState = LocalLifecycleOwner.current.lifecycle.currentStateAsState()
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { }
            LaunchedEffect(Unit) {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACTIVITY_RECOGNITION,
                        Manifest.permission.BODY_SENSORS,
                    ),
                )
            }
            LaunchedEffect(lifecycleState.value) {
                if (lifecycleState.value == Lifecycle.State.RESUMED) {
                    watchViewModel.refreshConnection()
                }
            }
            RunWayWearApp(watchViewModel, launchToken, isAmbient)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        launchToken++
    }


}
