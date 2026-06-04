package com.runway.android.core.posture.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PostureAnalysisDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PostureAnalysisEntity)

    @Query("SELECT * FROM posture_analyses ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PostureAnalysisEntity>>

    @Query("SELECT * FROM posture_analyses WHERE id = :id")
    suspend fun findById(id: String): PostureAnalysisEntity?

    @Query("DELETE FROM posture_analyses WHERE id = :id")
    suspend fun deleteById(id: String)
}
