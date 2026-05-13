package com.runway.run.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "running_points")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RunningPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "running_record_id", nullable = false, columnDefinition = "uuid")
    private UUID runningRecordId;

    @Column(nullable = false)
    private Integer sequence;

    // JTS Point → Hibernate Spatial 이 geography(Point,4326) 로 변환
    @Column(columnDefinition = "geography(Point,4326)", nullable = false)
    private Point location;

    @Column(name = "altitude_meters")
    private Double altitudeMeters;

    @Column(name = "speed_mps")
    private Double speedMps;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Builder
    private RunningPoint(UUID runningRecordId, Integer sequence, Point location,
                         Double altitudeMeters, Double speedMps, Instant recordedAt) {
        this.runningRecordId = runningRecordId;
        this.sequence = sequence;
        this.location = location;
        this.altitudeMeters = altitudeMeters;
        this.speedMps = speedMps;
        this.recordedAt = recordedAt;
    }
}
