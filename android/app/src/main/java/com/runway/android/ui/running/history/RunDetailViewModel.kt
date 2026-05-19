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
    var hasError by mutableStateOf(false)
        private set

    init {
        loadDetail()
    }

    fun retry() {
        isLoading = true
        hasError = false
        detail = null
        loadDetail()
    }

    private fun loadDetail() {
        viewModelScope.launch {
            when (val result = runningRepository.getRunDetail(runId)) {
                is NetworkResult.Success -> detail = result.data
                else -> hasError = true
            }
            isLoading = false
        }
    }
}
