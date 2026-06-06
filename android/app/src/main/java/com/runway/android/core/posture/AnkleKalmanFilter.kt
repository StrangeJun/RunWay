package com.runway.android.core.posture

import kotlin.math.abs

/**
 * 2D Kalman filter for ankle position and velocity tracking.
 *
 * Ported from GaitKeeper (MIT) — chrismarth/GaitKeeper — lib/kalman-filter.ts
 * Adapted from pixel coordinates to MediaPipe normalized coordinates (0–1).
 *
 * State vector: [x, y, vx, vy]
 *
 * Usage:
 *   val filter = AnkleKalmanFilter(initialX, initialY)
 *   filter.update(ankle.x, ankle.y, timestampMs)
 *   if (filter.isLanding()) { /* heel strike detected */ }
 */
class AnkleKalmanFilter(
    initialX: Float,
    initialY: Float,
    private val processNoise: Float = 0.0005f,      // Q — normalized coords are 0-1 scale
    private val measurementNoise: Float = 0.02f,    // R
) {
    // State: [x, y, vx, vy]
    private var stateX = initialX
    private var stateY = initialY
    private var vx = 0f
    private var vy = 0f

    // Covariance (diagonal only, same simplification as GaitKeeper)
    private var px = 0.1f
    private var py = 0.1f
    private var pvx = 0.1f
    private var pvy = 0.1f

    private var lastTimestampMs: Long = -1L

    fun update(measX: Float, measY: Float, timestampMs: Long) {
        val dt = if (lastTimestampMs < 0L) {
            0f
        } else {
            ((timestampMs - lastTimestampMs) / 1000f).coerceIn(0f, 0.1f)
        }
        lastTimestampMs = timestampMs

        // ── Predict ──────────────────────────────────────────────────────────
        stateX += vx * dt
        stateY += vy * dt
        px += processNoise * dt
        py += processNoise * dt
        pvx += processNoise * dt * 0.1f
        pvy += processNoise * dt * 0.1f

        // ── Update ───────────────────────────────────────────────────────────
        val kx = px / (px + measurementNoise)
        val ky = py / (py + measurementNoise)
        val innovX = measX - stateX
        val innovY = measY - stateY
        stateX += kx * innovX
        stateY += ky * innovY

        if (dt > 0.001f) {
            val alpha = 0.3f   // velocity blending (mirrors GaitKeeper)
            vx = (1f - alpha) * vx + alpha * (innovX / dt)
            vy = (1f - alpha) * vy + alpha * (innovY / dt)
        }

        px *= (1f - kx)
        py *= (1f - ky)
    }

    val x get() = stateX
    val y get() = stateY
    val velocityX get() = vx
    val velocityY get() = vy

    /**
     * Heel strike / landing detection.
     * In normalized coordinates: ground ≈ y=0.85, threshold in normalized units.
     * vy > 0 means moving downward (Y increases downward in image space).
     */
    fun isLanding(
        groundY: Float = 0.85f,
        posThreshold: Float = 0.05f,
        minVy: Float = 0.05f,      // normalized units/sec moving down
        maxVy: Float = 5.0f,
    ): Boolean {
        val nearGround = abs(stateY - groundY) < posThreshold
        val movingDown = vy in minVy..maxVy
        return nearGround && movingDown
    }

    /**
     * Toe-off detection: ankle moving upward or already above ground.
     */
    fun isToeOff(
        groundY: Float = 0.85f,
        posThreshold: Float = 0.04f,
        minUpVy: Float = 0.05f,    // vy < -minUpVy means moving up
    ): Boolean {
        val movingUp = vy < -minUpVy
        val aboveGround = stateY < groundY - posThreshold
        return movingUp || aboveGround
    }

    fun reset(x: Float, y: Float) {
        stateX = x; stateY = y
        vx = 0f; vy = 0f
        px = 0.1f; py = 0.1f; pvx = 0.1f; pvy = 0.1f
        lastTimestampMs = -1L
    }
}
