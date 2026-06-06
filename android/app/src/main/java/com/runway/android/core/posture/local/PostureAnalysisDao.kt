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

    @Query("SELECT * FROM posture_analyses WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    fun observeAll(ownerId: String): Flow<List<PostureAnalysisEntity>>

    @Query("SELECT * FROM posture_analyses WHERE id = :id AND ownerId = :ownerId")
    suspend fun findById(id: String, ownerId: String): PostureAnalysisEntity?

    @Query("SELECT COUNT(*) FROM posture_analyses WHERE ownerId = :ownerId")
    suspend fun count(ownerId: String): Int

    @Query("DELETE FROM posture_analyses WHERE id = :id AND ownerId = :ownerId")
    suspend fun deleteById(id: String, ownerId: String)

    @Query("UPDATE posture_analyses SET ownerId = :ownerId WHERE ownerId = ''")
    suspend fun assignUnowned(ownerId: String)
}
