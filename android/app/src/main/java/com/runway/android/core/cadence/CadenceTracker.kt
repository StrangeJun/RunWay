package com.runway.android.core.cadence

import kotlinx.coroutines.flow.Flow

interface CadenceTracker {
    fun cadenceSpmFlow(): Flow<Int?>
}
