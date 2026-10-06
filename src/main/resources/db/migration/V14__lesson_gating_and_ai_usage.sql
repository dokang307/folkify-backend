-- Gating bài học theo gói + bộ đếm lượt chấm điểm AI theo tháng

ALTER TABLE lessons ADD COLUMN required_plan    VARCHAR(10) NOT NULL DEFAULT 'FREE';
ALTER TABLE lessons ADD COLUMN youtube_video_id VARCHAR(20);
ALTER TABLE lessons ADD COLUMN channel_name     VARCHAR(255);
ALTER TABLE lessons ADD COLUMN source_url       TEXT;
ALTER TABLE lessons ADD CONSTRAINT chk_lessons_required_plan CHECK (required_plan IN ('FREE', 'BASIC', 'PRO'));

-- Dữ liệu cũ: 2 bài đầu mỗi nhạc cụ FREE, Advanced → PRO, còn lại BASIC
UPDATE lessons SET required_plan = CASE
    WHEN order_index < 2 THEN 'FREE'
    WHEN lower(level) LIKE 'advanced%' THEN 'PRO'
    ELSE 'BASIC'
END;

CREATE TABLE IF NOT EXISTS user_ai_usage (
    user_id    UUID       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    period     VARCHAR(7) NOT NULL,
    used_count INTEGER    NOT NULL DEFAULT 0,
    updated_at TIMESTAMP  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, period)
);
