package com.runway.android.ui.running.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.result.NetworkResult
import com.runway.android.data.running.model.RunDetailResponse
import com.runway.android.domain.running.RunningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RunDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val runningRepository: RunningRepository,
) : ViewModel() {

    val runId: String = checkNotNull(savedStateHandle["runId"])

    var detail by mutableStateOf<RunDetailResponse?>(null)
        private set
    var isLoading by mutableStateOf(true)
        private set

    init {
        loadDetail()
    }

    private fun loadDetail() {
        viewModelScope.launch {
            val result = runningRepository.getRunDetail(runId)
            if (result is NetworkResult.Success) {
                detail = result.data
            }
            isLoading = false
        }
    }
}
