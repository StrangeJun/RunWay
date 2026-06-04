package com.runway.android.core.posture.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posture_analyses")
data class PostureAnalysisEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val overallScore: Int,
    val grade: String,
    val overallFeedback: String,
    val kneeScore: Int,
    val kneeMeasuredAngle: Float,
    val kneeFeedback: String,
    val kneeTip: String,
    val trunkScore: Int,
    val trunkMeasuredAngle: Float,
    val trunkFeedback: String,
    val trunkTip: String,
    val elbowScore: Int,
    val elbowMeasuredAngle: Float,
    val elbowFeedback: String,
    val elbowTip: String,
    val hipScore: Int,
    val hipMeasuredAngle: Float,
    val hipFeedback: String,
    val hipTip: String,
    val overstrideScore: Int,
    val overstrideRatio: Float,
    val overstrideFeedback: String,
    val overstrideTip: String,
)
