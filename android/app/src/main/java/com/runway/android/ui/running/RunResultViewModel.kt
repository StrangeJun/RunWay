package com.runway.android.ui.running

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class RunResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val runId: String? = savedStateHandle.get<String>("runId")?.takeIf { it != "none" }

    private val elapsedSeconds: Int = savedStateHandle.get<Int>("elapsedSeconds") ?: 0
    private val distanceKm: Float = savedStateHandle.get<Float>("distanceKm") ?: 0f

    val timerText: String = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)

    val distanceText: String = "%.2f".format(distanceKm)

    val paceText: String = if (distanceKm > 0.001f) {
        val secsPerKm = (elapsedSeconds / distanceKm).toInt()
        "%d'%02d\"/km".format(secsPerKm / 60, secsPerKm % 60)
    } else {
        "--'--\"/km"
    }

    val caloriesText: String = "${(distanceKm * 72).toInt()} kcal"

    val stepsText: String = String.format(Locale.US, "%,d", (distanceKm * 1320).toInt())
}
