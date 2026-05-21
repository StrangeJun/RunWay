CREATE TABLE course_favorites
(
    user_id    UUID        NOT NULL,
    course_id  UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_course_favorites PRIMARY KEY (user_id, course_id),
    CONSTRAINT fk_course_favorites_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_course_favorites_course
        FOREIGN KEY (course_id) REFERENCES courses (id) ON DELETE CASCADE
);

CREATE INDEX idx_course_favorites_user_id ON course_favorites (user_id, created_at DESC);
