CREATE TABLE courses
(
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    creator_id       UUID         NOT NULL,
    -- 원본 러닝 기록. SET NULL: 런 삭제 시 코스는 유지됨
    source_record_id UUID,
    -- VARCHAR(100): 충분한 이름 길이 허용 (VARCHAR(10) 아님에 주의)
    name             VARCHAR(100) NOT NULL,
    description      TEXT,
    status           VARCHAR(30)  NOT NULL DEFAULT 'draft',
    distance_meters  FLOAT        NOT NULL,
    is_loop          BOOLEAN      NOT NULL DEFAULT FALSE,
    -- 인근 코스 탐색 (ST_DWithin) 의 기준점
    start_location   geography(Point, 4326) NOT NULL,
    end_location     geography(Point, 4326) NOT NULL,
    -- course_points를 ST_MakeLine()으로 집계하여 저장
    path             geography(LineString, 4326),
    -- course_attempts에서 집계 시 매번 COUNT하지 않도록 denormalized 카운터 유지
    attempt_count    INT          NOT NULL DEFAULT 0,
    completion_count INT          NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMPTZ,

    CONSTRAINT pk_courses PRIMARY KEY (id),
    CONSTRAINT chk_course_status
        CHECK (status IN ('draft', 'published', 'archived')),
    CONSTRAINT fk_courses_creator
        FOREIGN KEY (creator_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_courses_source_record
        FOREIGN KEY (source_record_id) REFERENCES running_records (id) ON DELETE SET NULL
);

-- 인근 코스 탐색 핵심 인덱스: ST_DWithin(start_location, ...) 쿼리에 사용
-- JPA @Index 어노테이션은 USING GIST를 지원하지 않으므로 반드시 Flyway SQL에서 생성
CREATE INDEX idx_courses_start_location ON courses USING GIST (start_location);

-- 내가 만든 코스 목록 조회
CREATE INDEX idx_courses_creator    ON courses (creator_id);
-- published 필터링 (인근 코스 탐색 시 WHERE status = 'published')
CREATE INDEX idx_courses_status     ON courses (status);
-- soft delete 필터링 (WHERE deleted_at IS NULL)
CREATE INDEX idx_courses_deleted_at ON courses (deleted_at);
