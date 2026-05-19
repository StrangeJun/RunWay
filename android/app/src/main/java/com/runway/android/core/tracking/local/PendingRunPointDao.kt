package com.runway.android.core.tracking.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingRunPointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<PendingRunPointEntity>)

    @Query("SELECT * FROM pending_run_points WHERE runningRecordId = :runningRecordId ORDER BY sequence ASC LIMIT :limit")
    suspend fun dequeue(runningRecordId: String, limit: Int): List<PendingRunPointEntity>

    @Query("DELETE FROM pending_run_points WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM pending_run_points WHERE runningRecordId = :runningRecordId")
    suspend fun deleteByRunningRecordId(runningRecordId: String)
}
