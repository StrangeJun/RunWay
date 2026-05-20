CREATE TABLE course_reports (
    id               UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    course_id        UUID         NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    reporter_id      UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason           VARCHAR(30)  NOT NULL CHECK (reason IN ('inappropriate_content', 'wrong_location', 'spam', 'other')),
    description      TEXT,
    status           VARCHAR(30)  NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'reviewed', 'dismissed')),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_course_reports_course_id ON course_reports (course_id);
CREATE INDEX idx_course_reports_reporter_id ON course_reports (reporter_id);
