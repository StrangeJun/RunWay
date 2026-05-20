package com.runway.android.data.running.model

data class PersonalRecordsResponse(
    val longestRun: PersonalRecordItem?,
    val fastestPace: PersonalRecordItem?,
    val mostCalories: PersonalRecordItem?,
    val best5k: PersonalRecordItem?,
    val best10k: PersonalRecordItem?,
    val totalCompletedRuns: Long,
    val totalDistanceMeters: Double,
)
