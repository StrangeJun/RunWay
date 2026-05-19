package com.runway.android.core.tracking

import com.runway.android.core.tracking.local.PendingRunPointDao
import com.runway.android.core.tracking.local.PendingRunPointEntity
import com.runway.android.data.running.model.RunPointRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingPointQueue @Inject constructor(
    private val dao: PendingRunPointDao,
) {
    suspend fun add(runningRecordId: String, points: List<RunPointRequest>) {
        if (points.isEmpty()) return
        dao.insertAll(points.map { p ->
            PendingRunPointEntity(
                runningRecordId = runningRecordId,
                sequence = p.sequence,
                latitude = p.latitude,
                longitude = p.longitude,
                altitudeMeters = p.altitudeMeters,
                speedMps = p.speedMps,
                recordedAt = p.recordedAt,
            )
        })
    }

    suspend fun dequeue(runningRecordId: String, batchSize: Int = 50): List<PendingRunPointEntity> =
        dao.dequeue(runningRecordId, batchSize)

    suspend fun deleteByIds(ids: List<Long>) {
        if (ids.isEmpty()) return
        dao.deleteByIds(ids)
    }

    suspend fun deleteByRunningRecordId(runningRecordId: String) =
        dao.deleteByRunningRecordId(runningRecordId)
}

fun PendingRunPointEntity.toRunPointRequest() = RunPointRequest(
    sequence = sequence,
    latitude = latitude,
    longitude = longitude,
    altitudeMeters = altitudeMeters,
    speedMps = speedMps,
    recordedAt = recordedAt,
)
