-- Câu hỏi ôn tập lý thuyết theo bài học, chấm điểm phía server

CREATE TABLE IF NOT EXISTS quiz_questions (
    id          UUID        PRIMARY KEY,
    lesson_id   UUID        NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    question    TEXT        NOT NULL,
    type        VARCHAR(10) NOT NULL DEFAULT 'SINGLE' CHECK (type IN ('SINGLE', 'MULTI')),
    explanation TEXT,
    order_index INTEGER     NOT NULL DEFAULT 0,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS quiz_options (
    id          UUID      PRIMARY KEY,
    question_id UUID      NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    text        TEXT      NOT NULL,
    is_correct  BOOLEAN   NOT NULL DEFAULT FALSE,
    order_index INTEGER   NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP
);

CREATE TABLE IF NOT EXISTS quiz_attempts (
    id            UUID      PRIMARY KEY,
    user_id       UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id     UUID      NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    score_percent INTEGER   NOT NULL,
    correct_count INTEGER   NOT NULL,
    total_count   INTEGER   NOT NULL,
    answers       JSONB     NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_quiz_questions_lesson      ON quiz_questions(lesson_id, order_index);
CREATE INDEX IF NOT EXISTS idx_quiz_options_question      ON quiz_options(question_id, order_index);
CREATE INDEX IF NOT EXISTS idx_quiz_attempts_user_lesson  ON quiz_attempts(user_id, lesson_id, created_at DESC);
