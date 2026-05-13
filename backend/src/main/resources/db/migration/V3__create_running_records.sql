CREATE TABLE running_records
(
    id                      UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id                 UUID        NOT NULL,
    -- PostgreSQL native enum 대신 VARCHAR(30) + CHECK 제약 사용
    -- 이유: Spring Boot JPA @Enumerated(EnumType.STRING)과 충돌 없이 매핑 가능
    status                  VARCHAR(30) NOT NULL DEFAULT 'in_progress',
    started_at              TIMESTAMPTZ NOT NULL,
    ended_at                TIMESTAMPTZ,
    distance_meters         FLOAT       NOT NULL DEFAULT 0,
    duration_seconds        INT         NOT NULL DEFAULT 0,
    avg_pace_seconds_per_km INT,
    avg_heart_rate_bpm      INT,
    calories_burned         INT,
    -- 런 완료 시 running_points를 ST_MakeLine()으로 집계하여 저장
    path                    geography(LineString, 4326),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_running_records PRIMARY KEY (id),
    CONSTRAINT chk_running_record_status
        CHECK (status IN ('in_progress', 'paused', 'completed', 'abandoned')),
    CONSTRAINT fk_running_records_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- 유저별 최신 런 조회 (내 기록 목록 API)
CREATE INDEX idx_running_records_user_started ON running_records (user_id, started_at DESC);
-- 진행 중인 런 복구 조회 (앱 재시작 시 in_progress 런 복구)
CREATE INDEX idx_running_records_status       ON running_records (status);
-- Phase 2 경로 분석 대비 (ST_DWithin, ST_Intersects 등 공간 쿼리)
CREATE INDEX idx_running_records_path         ON running_records USING GIST (path);
