-- Bài tập luyện có bản mẫu + kết quả chấm điểm AI.
-- File ghi âm của học viên KHÔNG được lưu; chỉ lưu điểm, metrics và nhận xét.

CREATE TABLE IF NOT EXISTS practice_exercises (
    id                  UUID         PRIMARY KEY,
    instrument_id       UUID         NOT NULL REFERENCES instruments(id) ON DELETE CASCADE,
    lesson_id           UUID         REFERENCES lessons(id) ON DELETE SET NULL,
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    required_plan       VARCHAR(10)  NOT NULL DEFAULT 'BASIC' CHECK (required_plan IN ('FREE', 'BASIC', 'PRO')),
    reference_audio_url TEXT,
    reference_contour   JSONB,
    source_url          TEXT,
    attribution         TEXT,
    status              VARCHAR(10)  NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'READY')),
    order_index         INTEGER      NOT NULL DEFAULT 0,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,
    CONSTRAINT chk_ready_has_contour CHECK (status = 'DRAFT' OR reference_contour IS NOT NULL)
);

CREATE TABLE IF NOT EXISTS performance_attempts (
    id              UUID        PRIMARY KEY,
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exercise_id     UUID        REFERENCES practice_exercises(id) ON DELETE SET NULL,
    instrument_id   UUID        NOT NULL REFERENCES instruments(id) ON DELETE CASCADE,
    mode            VARCHAR(10) NOT NULL CHECK (mode IN ('REFERENCE', 'FREE')),
    overall         INTEGER     NOT NULL,
    pitch_score     INTEGER     NOT NULL,
    rhythm_score    INTEGER,
    stability_score INTEGER     NOT NULL,
    metrics         JSONB       NOT NULL,
    feedback        JSONB       NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_practice_exercises_instrument ON practice_exercises(instrument_id, order_index);
CREATE INDEX IF NOT EXISTS idx_performance_attempts_user     ON performance_attempts(user_id, created_at DESC);
