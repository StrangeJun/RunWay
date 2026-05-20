package com.runway.android.ui.share

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.result.NetworkResult
import com.runway.android.core.share.ImageCaptureUtil
import com.runway.android.core.share.ImageShareUtil
import com.runway.android.core.share.ShareTemplate
import com.runway.android.data.running.model.RunDetailResponse
import com.runway.android.domain.running.RunningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RunShareImageViewModel @Inject constructor(
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
    var selectedTemplate by mutableStateOf(ShareTemplate.DARK_SPORT)
        private set
    var isSharing by mutableStateOf(false)
        private set
    var shareError by mutableStateOf<String?>(null)
        private set

    private val _shareEvent = MutableSharedFlow<Uri>()
    val shareEvent = _shareEvent.asSharedFlow()

    init {
        loadDetail()
    }

    fun onTemplateChange(template: ShareTemplate) {
        selectedTemplate = template
    }

    fun captureAndShare(context: Context) {
        if (isSharing) return
        val d = detail ?: return
        isSharing = true
        shareError = null

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val bitmap = ImageCaptureUtil.draw(d, selectedTemplate)
                ImageShareUtil.saveToCache(context, bitmap)
                    ?: error("이미지를 캐시에 저장하지 못했습니다.")
            }.onSuccess { uri ->
                _shareEvent.emit(uri)
            }.onFailure {
                withContext(Dispatchers.Main) {
                    shareError = "이미지를 생성하지 못했습니다."
                }
            }
            withContext(Dispatchers.Main) { isSharing = false }
        }
    }

    fun clearShareError() {
        shareError = null
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
