package com.runway.run.repository;

import com.runway.run.domain.RunningPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RunningPointRepository extends JpaRepository<RunningPoint, UUID> {

    List<RunningPoint> findByRunningRecordIdOrderBySequenceAsc(UUID runningRecordId);

    void deleteByRunningRecordId(UUID runningRecordId);

    @Modifying
    @Query(value = """
            INSERT INTO running_points
                (id, running_record_id, sequence, location, altitude_meters, speed_mps, recorded_at)
            VALUES
                (gen_random_uuid(), :runningRecordId, :sequence,
                 ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                 :altitudeMeters, :speedMps, :recordedAt)
            ON CONFLICT (running_record_id, sequence) DO NOTHING
            """, nativeQuery = true)
    void insertIgnoreConflict(
            @Param("runningRecordId") UUID runningRecordId,
            @Param("sequence") int sequence,
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("altitudeMeters") Double altitudeMeters,
            @Param("speedMps") Double speedMps,
            @Param("recordedAt") Instant recordedAt
    );
}
