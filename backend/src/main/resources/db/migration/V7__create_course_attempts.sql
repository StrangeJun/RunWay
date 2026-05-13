CREATE TABLE course_attempts
(
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    course_id           UUID        NOT NULL,
    user_id             UUID        NOT NULL,
    -- 코스 시도 중 생성된 러닝 기록. SET NULL: 런 삭제 시 시도 기록은 유지됨
    running_record_id   UUID,
    status              VARCHAR(30) NOT NULL DEFAULT 'in_progress',
    -- Phase 1: completed 시 자동으로 verified 설정 (서비스 레이어 처리)
    -- Phase 2: GPS 경로 매칭 검증 로직에서 pending → verified/rejected 전환
    verification_status VARCHAR(30) NOT NULL DEFAULT 'pending',
    started_at          TIMESTAMPTZ NOT NULL,
    completed_at        TIMESTAMPTZ,
    -- 완주 소요 시간 (미완주 시 NULL). 리더보드 집계 기준 컬럼
    duration_seconds    INT,
    distance_meters     FLOAT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_course_attempts PRIMARY KEY (id),
    CONSTRAINT chk_course_attempt_status
        CHECK (status IN ('in_progress', 'completed', 'abandoned')),
    CONSTRAINT chk_attempt_verification_status
        CHECK (verification_status IN ('pending', 'verified', 'rejected')),
    CONSTRAINT fk_course_attempts_course
        FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
    CONSTRAINT fk_course_attempts_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_course_attempts_run
        FOREIGN KEY (running_record_id) REFERENCES running_records (id) ON DELETE SET NULL
);

-- 리더보드 조회 핵심 partial index
-- WHERE 절로 completed + verified 기록만 인덱싱하여 크기를 최소화
-- SELECT MIN(duration_seconds) GROUP BY user_id ORDER BY best_time 쿼리에 사용
CREATE INDEX idx_course_attempts_leaderboard
    ON course_attempts (course_id, duration_seconds)
    WHERE status = 'completed' AND verification_status = 'verified';

-- 코스별 시도 목록 조회
CREATE INDEX idx_course_attempts_course_id  ON course_attempts (course_id);
-- 유저별 시도 이력 조회
CREATE INDEX idx_course_attempts_user_id    ON course_attempts (user_id);
-- 유저의 특정 코스 시도 이력 조회
CREATE INDEX idx_course_attempts_course_user ON course_attempts (course_id, user_id);
