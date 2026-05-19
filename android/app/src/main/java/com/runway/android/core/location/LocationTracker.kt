package com.runway.android.core.location

import kotlinx.coroutines.flow.Flow

interface LocationTracker {
    // Returns a cold Flow that emits GPS locations while collected.
    // Caller must ensure ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION is granted before collecting.
    fun locationFlow(): Flow<RunwayLocation>
}
