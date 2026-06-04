package com.runway.android.ui.posture

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runway.android.core.posture.PostureEvaluator
import com.runway.android.core.posture.PosturePoseAnalyzer
import com.runway.android.core.posture.PostureResult
import com.runway.android.core.posture.local.PostureAnalysisDao
import com.runway.android.core.posture.local.PostureAnalysisEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface PostureAnalysisState {
    data object Idle : PostureAnalysisState
    data object Analyzing : PostureAnalysisState
    data class Success(val result: PostureResult, val id: String) : PostureAnalysisState
    data class Error(val message: String) : PostureAnalysisState
}

@HiltViewModel
class PostureAnalysisViewModel @Inject constructor(
    private val poseAnalyzer: PosturePoseAnalyzer,
    private val evaluator: PostureEvaluator,
    private val dao: PostureAnalysisDao,
) : ViewModel() {

    private val _analysisState = MutableStateFlow<PostureAnalysisState>(PostureAnalysisState.Idle)
    val analysisState: StateFlow<PostureAnalysisState> = _analysisState.asStateFlow()

    val analysisHistory = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun analyze(videoUri: Uri) {
        if (_analysisState.value is PostureAnalysisState.Analyzing) return
        viewModelScope.launch {
            _analysisState.value = PostureAnalysisState.Analyzing
            runCatching {
                val frames = poseAnalyzer.extractAngles(videoUri)
                if (frames.isEmpty()) {
                    _analysisState.value = PostureAnalysisState.Error(
                        "영상에서 자세를 감지하지 못했습니다. 전신이 잘 보이도록 다시 촬영해주세요."
                    )
                    return@launch
                }
                val result = evaluator.evaluate(frames)
                val id = UUID.randomUUID().toString()
                dao.insert(result.toEntity(id))
                _analysisState.value = PostureAnalysisState.Success(result, id)
            }.onFailure { e ->
                _analysisState.value = PostureAnalysisState.Error(
                    "분석 중 오류가 발생했습니다: ${e.message}"
                )
            }.also {
                // Delete temp cache file after analysis (privacy).
                // Only delete file:// URIs — content:// URIs are managed by the OS.
                runCatching {
                    if (videoUri.scheme == "file") {
                        videoUri.path?.let { path -> java.io.File(path).delete() }
                    }
                }
            }
        }
    }

    fun resetState() {
        _analysisState.value = PostureAnalysisState.Idle
    }

    suspend fun loadById(id: String): PostureAnalysisEntity? = dao.findById(id)

    private fun PostureResult.toEntity(id: String) = PostureAnalysisEntity(
        id = id,
        createdAt = System.currentTimeMillis(),
        overallScore = overallScore,
        grade = grade,
        overallFeedback = overallFeedback,
        kneeScore = knee.score,
        kneeMeasuredAngle = knee.measuredValue,
        kneeFeedback = knee.feedback,
        kneeTip = knee.tip,
        trunkScore = trunk.score,
        trunkMeasuredAngle = trunk.measuredValue,
        trunkFeedback = trunk.feedback,
        trunkTip = trunk.tip,
        elbowScore = elbow.score,
        elbowMeasuredAngle = elbow.measuredValue,
        elbowFeedback = elbow.feedback,
        elbowTip = elbow.tip,
        hipScore = hip.score,
        hipMeasuredAngle = hip.measuredValue,
        hipFeedback = hip.feedback,
        hipTip = hip.tip,
        overstrideScore = overstride.score,
        overstrideRatio = overstride.measuredValue,
        overstrideFeedback = overstride.feedback,
        overstrideTip = overstride.tip,
    )
}
