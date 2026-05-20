package com.runway.android.core.tracking

enum class PauseReason {
    NONE,    // running normally
    MANUAL,  // user pressed pause
    AUTO,    // auto-paused due to low speed
}
