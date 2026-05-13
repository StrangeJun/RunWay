package com.runway.run.dto;

import com.runway.run.domain.RunningPoint;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class RunPointResponse {

    private Integer sequence;
    private Double latitude;
    private Double longitude;
    private Double altitudeMeters;
    private Double speedMps;
    private Instant recordedAt;

    public static RunPointResponse from(RunningPoint point) {
        // JTS Coordinate: x = longitude, y = latitude
        return RunPointResponse.builder()
                .sequence(point.getSequence())
                .latitude(point.getLocation().getY())
                .longitude(point.getLocation().getX())
                .altitudeMeters(point.getAltitudeMeters())
                .speedMps(point.getSpeedMps())
                .recordedAt(point.getRecordedAt())
                .build();
    }
}
