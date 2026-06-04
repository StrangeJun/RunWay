ALTER TABLE courses
    ADD COLUMN difficulty        VARCHAR(20),
    ADD COLUMN slope_level       VARCHAR(20),
    ADD COLUMN risk_level        VARCHAR(20),
    ADD COLUMN surface_type      VARCHAR(20),
    ADD COLUMN recommended_time  VARCHAR(20),
    ADD COLUMN warnings          TEXT;

ALTER TABLE courses
    ADD CONSTRAINT chk_course_difficulty
        CHECK (difficulty IS NULL OR difficulty IN ('easy', 'normal', 'hard')),
    ADD CONSTRAINT chk_course_slope_level
        CHECK (slope_level IS NULL OR slope_level IN ('flat', 'moderate', 'steep')),
    ADD CONSTRAINT chk_course_risk_level
        CHECK (risk_level IS NULL OR risk_level IN ('low', 'medium', 'high')),
    ADD CONSTRAINT chk_course_surface_type
        CHECK (surface_type IS NULL OR surface_type IN ('road', 'park', 'trail', 'mixed')),
    ADD CONSTRAINT chk_course_recommended_time
        CHECK (recommended_time IS NULL OR recommended_time IN ('morning', 'day', 'night', 'any'));
