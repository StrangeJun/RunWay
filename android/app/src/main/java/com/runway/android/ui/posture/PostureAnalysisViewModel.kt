package com.runway.android.ui.posture

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.runway.android.core.posture.PostureEvaluator
import com.runway.android.core.posture.PosturePoseAnalyzer
import com.runway.android.core.posture.PostureResult
import com.runway.android.core.posture.PostureVideoFrame
import com.runway.android.core.posture.local.PostureAnalysisDao
import com.runway.android.core.posture.local.PostureAnalysisEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
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
    @ApplicationContext private val context: Context,
    private val poseAnalyzer: PosturePoseAnalyzer,
    private val evaluator: PostureEvaluator,
    private val dao: PostureAnalysisDao,
) : ViewModel() {

    private val gson = Gson()
    private val videoFramesType = object : TypeToken<List<PostureVideoFrame>>() {}.type

    private val _analysisState = MutableStateFlow<PostureAnalysisState>(PostureAnalysisState.Idle)
    val analysisState: StateFlow<PostureAnalysisState> = _analysisState.asStateFlow()

    val analysisHistory = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun analyze(videoUri: Uri) {
        if (_analysisState.value is PostureAnalysisState.Analyzing) return
        viewModelScope.launch {
            _analysisState.value = PostureAnalysisState.Analyzing
            val id = UUID.randomUUID().toString()
            var savedVideoPath: String? = null

            runCatching {
                val output = poseAnalyzer.extractAnalysis(videoUri, id)
                // Capture early so onFailure can always clean up the copied video,
                // even if analysis finds no poses. Using return@launch here would
                // bypass onFailure/also and leak the file.
                savedVideoPath = output.videoPath

                check(output.frames.isNotEmpty()) {
                    "영상에서 자세를 감지하지 못했습니다. 전신이 잘 보이도록 다시 촬영해주세요."
                }

                val result = evaluator.evaluate(output.frames)
                val framesJson = gson.toJson(output.videoFrames)
                dao.insert(result.toEntity(id, output.videoPath, framesJson, output.videoWidth, output.videoHeight))
                _analysisState.value = PostureAnalysisState.Success(result, id)
            }.onFailure { e ->
                savedVideoPath?.let { File(it).delete() }
                _analysisState.value = PostureAnalysisState.Error(
                    e.message ?: "분석 중 오류가 발생했습니다."
                )
            }.also {
                // Always delete the original cache file (privacy).
                // The video was already copied to internal storage by the analyzer.
                runCatching {
                    if (videoUri.scheme == "file") {
                        videoUri.path?.let { path -> File(path).delete() }
                    }
                }
            }
        }
    }

    fun resetState() {
        _analysisState.value = PostureAnalysisState.Idle
    }

    suspend fun loadById(id: String): PostureAnalysisEntity? = dao.findById(id)

    fun parseVideoFrames(json: String?): List<PostureVideoFrame> {
        if (json.isNullOrEmpty()) return emptyList()
        return runCatching { gson.fromJson<List<PostureVideoFrame>>(json, videoFramesType) }
            .getOrDefault(emptyList())
    }

    fun deleteAnalysis(id: String) {
        viewModelScope.launch {
            val entity = dao.findById(id)
            entity?.videoPath?.let { path -> File(path).delete() }
            dao.deleteById(id)
        }
    }

    private fun PostureResult.toEntity(
        id: String,
        videoPath: String?,
        videoFramesJson: String?,
        videoWidth: Int,
        videoHeight: Int,
    ) = PostureAnalysisEntity(
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
        videoPath = videoPath,
        videoFramesJson = videoFramesJson,
        videoWidth = videoWidth,
        videoHeight = videoHeight,
    )
}
