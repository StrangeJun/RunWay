package com.runway.wear

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runway.wear.WatchViewModel
import com.runway.wear.ui.PathFinderWearApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    watchViewModel.refreshAuthState()
                }
            }
            PathFinderWearApp(watchViewModel)
        }
    }
}
