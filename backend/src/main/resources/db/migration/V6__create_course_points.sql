CREATE TABLE course_points
(
    id              UUID NOT NULL DEFAULT gen_random_uuid(),
    course_id       UUID NOT NULL,
    -- running_points를 다운샘플링하여 생성한 코스 경로 웨이포인트 순서
    sequence        INT  NOT NULL,
    location        geography(Point, 4326) NOT NULL,
    altitude_meters FLOAT,

    CONSTRAINT pk_course_points PRIMARY KEY (id),
    CONSTRAINT fk_course_points_course
        FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE,
    -- 중복 sequence 방지 + B-tree 인덱스 자동 생성 (경로 순서 조회에 사용)
    CONSTRAINT uq_course_points_course_seq UNIQUE (course_id, sequence)
);
-- uq_course_points_course_seq UNIQUE 제약이 (course_id, sequence) B-tree 인덱스를 자동 생성한다
-- 별도 CREATE INDEX 불필요
