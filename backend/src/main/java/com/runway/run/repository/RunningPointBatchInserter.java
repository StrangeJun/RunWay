package com.runway.run.repository;

import com.runway.run.dto.SavePointsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RunningPointBatchInserter {

    private static final String SQL = """
            INSERT INTO running_points
                (id, running_record_id, sequence, location, altitude_meters, speed_mps, recorded_at)
            VALUES
                (gen_random_uuid(), ?, ?,
                 ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography,
                 ?, ?, ?)
            ON CONFLICT (running_record_id, sequence) DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;

    public void batchInsert(UUID runId, List<SavePointsRequest.PointData> points) {
        jdbcTemplate.batchUpdate(SQL, points, points.size(), (ps, p) -> {
            ps.setObject(1, runId);
            ps.setInt(2, p.getSequence());
            ps.setDouble(3, p.getLongitude());   // ST_MakePoint(lon, lat) order
            ps.setDouble(4, p.getLatitude());
            ps.setObject(5, p.getAltitudeMeters());
            ps.setObject(6, p.getSpeedMps());
            ps.setTimestamp(7, p.getRecordedAt() != null
                    ? Timestamp.from(p.getRecordedAt()) : null);
        });
    }
}
