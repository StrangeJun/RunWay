package com.runway.android.core.posture

/** Normalized (0–1) landmark point extracted from one video frame. */
data class SkeletonPoint(val x: Float, val y: Float, val v: Float)  // v = visibility

/** Landmark snapshot for one frame, used to drive the skeleton replay overlay. */
data class PostureVideoFrame(
    val t: Long,                     // timestamp from video start (ms)
    val pts: List<SkeletonPoint>,    // 13 key landmarks in KEY_LANDMARK_INDICES order
)

/**
 * MediaPipe landmark indices we store for the skeleton overlay.
 * Order: nose, lShoulder, rShoulder, lElbow, rElbow, lWrist, rWrist,
 *        lHip, rHip, lKnee, rKnee, lAnkle, rAnkle
 */
val KEY_LANDMARK_INDICES = intArrayOf(0, 11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28)

// Local indices (position in pts list) for skeleton edge drawing
private const val I_NOSE = 0
private const val I_L_SHOULDER = 1; private const val I_R_SHOULDER = 2
private const val I_L_ELBOW = 3;    private const val I_R_ELBOW = 4
private const val I_L_WRIST = 5;    private const val I_R_WRIST = 6
private const val I_L_HIP = 7;      private const val I_R_HIP = 8
private const val I_L_KNEE = 9;     private const val I_R_KNEE = 10
private const val I_L_ANKLE = 11;   private const val I_R_ANKLE = 12

/** Skeleton edge pairs (local indices into PostureVideoFrame.pts). */
val SKELETON_EDGES: List<Pair<Int, Int>> = listOf(
    I_L_SHOULDER to I_R_SHOULDER,
    I_L_SHOULDER to I_L_ELBOW,  I_L_ELBOW to I_L_WRIST,
    I_R_SHOULDER to I_R_ELBOW,  I_R_ELBOW to I_R_WRIST,
    I_L_SHOULDER to I_L_HIP,    I_R_SHOULDER to I_R_HIP,
    I_L_HIP to I_R_HIP,
    I_L_HIP to I_L_KNEE,        I_L_KNEE to I_L_ANKLE,
    I_R_HIP to I_R_KNEE,        I_R_KNEE to I_R_ANKLE,
)

val SKELETON_JOINT_INDICES: List<Int> = (0..12).toList()
