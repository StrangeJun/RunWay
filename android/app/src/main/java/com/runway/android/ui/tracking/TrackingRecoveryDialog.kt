package com.runway.android.ui.tracking

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TrackingRecoveryDialog(viewModel: TrackingRecoveryViewModel) {
    val snap = viewModel.snapshot ?: return

    AlertDialog(
        onDismissRequest = {},
        title = { Text("이전 런 세션 발견") },
        text = {
            Column {
                Text("앱이 종료되었지만 완료되지 않은 런 세션이 있습니다.")
                Spacer(Modifier.height(8.dp))
                Text("거리: %.2f km".format(snap.distanceMeters / 1000.0))
                Text("시간: %02d:%02d".format(snap.elapsedSeconds / 60, snap.elapsedSeconds % 60))
                if (viewModel.isRecovering) {
                    Spacer(Modifier.height(8.dp))
                    CircularProgressIndicator()
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = viewModel::finish,
                enabled = !viewModel.isRecovering,
            ) {
                Text("완료 처리")
            }
        },
        dismissButton = {
            TextButton(
                onClick = viewModel::abandon,
                enabled = !viewModel.isRecovering,
            ) {
                Text("포기")
            }
        },
    )
}
