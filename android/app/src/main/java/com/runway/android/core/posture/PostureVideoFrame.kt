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
const val SKEL_NOSE = 0
const val SKEL_L_SHOULDER = 1; const val SKEL_R_SHOULDER = 2
const val SKEL_L_ELBOW = 3;    const val SKEL_R_ELBOW = 4
const val SKEL_L_WRIST = 5;    const val SKEL_R_WRIST = 6
const val SKEL_L_HIP = 7;      const val SKEL_R_HIP = 8
const val SKEL_L_KNEE = 9;     const val SKEL_R_KNEE = 10
const val SKEL_L_ANKLE = 11;   const val SKEL_R_ANKLE = 12

// One-sided limb edges (arm chain + torso + leg chain)
val SKELETON_EDGES_LEFT: List<Pair<Int, Int>> = listOf(
    SKEL_L_SHOULDER to SKEL_L_ELBOW, SKEL_L_ELBOW to SKEL_L_WRIST,
    SKEL_L_SHOULDER to SKEL_L_HIP,
    SKEL_L_HIP to SKEL_L_KNEE,       SKEL_L_KNEE to SKEL_L_ANKLE,
)
val SKELETON_EDGES_RIGHT: List<Pair<Int, Int>> = listOf(
    SKEL_R_SHOULDER to SKEL_R_ELBOW, SKEL_R_ELBOW to SKEL_R_WRIST,
    SKEL_R_SHOULDER to SKEL_R_HIP,
    SKEL_R_HIP to SKEL_R_KNEE,       SKEL_R_KNEE to SKEL_R_ANKLE,
)
// Bilateral connectors (drawn only when both endpoints are visible enough)
val SKELETON_EDGES_BILATERAL: List<Pair<Int, Int>> = listOf(
    SKEL_L_SHOULDER to SKEL_R_SHOULDER,
    SKEL_L_HIP to SKEL_R_HIP,
)

/** All edges combined — kept for backward compatibility / tests. */
val SKELETON_EDGES: List<Pair<Int, Int>> =
    SKELETON_EDGES_BILATERAL + SKELETON_EDGES_LEFT + SKELETON_EDGES_RIGHT

val SKELETON_JOINT_INDICES: List<Int> = (0..12).toList()
