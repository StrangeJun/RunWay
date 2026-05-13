CREATE TABLE running_points
(
    id                UUID        NOT NULL DEFAULT gen_random_uuid(),
    running_record_id UUID        NOT NULL,
    -- 클라이언트가 GPS 샘플링 순서대로 부여하는 0-based 정수
    sequence          INT         NOT NULL,
    location          geography(Point, 4326) NOT NULL,
    altitude_meters   FLOAT,
    speed_mps         FLOAT,
    recorded_at       TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_running_points PRIMARY KEY (id),
    CONSTRAINT fk_running_points_record
        FOREIGN KEY (running_record_id) REFERENCES running_records (id) ON DELETE CASCADE,
    -- 중복 sequence 방지 + B-tree 인덱스 자동 생성 (경로 순서 조회에 사용)
    CONSTRAINT uq_running_points_record_seq UNIQUE (running_record_id, sequence)
);
-- uq_running_points_record_seq UNIQUE 제약이 (running_record_id, sequence) B-tree 인덱스를 자동 생성한다
-- 별도 CREATE INDEX 불필요
