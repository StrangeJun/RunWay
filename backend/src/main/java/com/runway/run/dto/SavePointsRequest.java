package com.runway.run.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
public class SavePointsRequest {

    @NotEmpty(message = "포인트 목록은 비어 있을 수 없습니다.")
    @Size(max = 500, message = "포인트는 한 번에 최대 500개까지 전송할 수 있습니다.")
    @Valid
    private List<PointData> points;

    @Getter
    public static class PointData {

        @NotNull(message = "sequence는 필수입니다.")
        private Integer sequence;

        @NotNull(message = "latitude는 필수입니다.")
        @DecimalMin(value = "-90.0", message = "latitude는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90.0", message = "latitude는 90 이하여야 합니다.")
        private Double latitude;

        @NotNull(message = "longitude는 필수입니다.")
        @DecimalMin(value = "-180.0", message = "longitude는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180.0", message = "longitude는 180 이하여야 합니다.")
        private Double longitude;

        private Double altitudeMeters;

        private Double speedMps;

        @NotNull(message = "recordedAt은 필수입니다.")
        private Instant recordedAt;
    }
}
