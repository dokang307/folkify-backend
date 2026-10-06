-- AI chấm phần trình diễn theo TÁC PHẨM (bảng songs) thay cho "bài tập" (practice_exercises).
-- Mỗi tác phẩm có 1 audio mẫu; người học chơi một đoạn hoặc cả bài, AI tự dò vị trí đoạn đó.

ALTER TABLE songs ADD COLUMN required_plan              VARCHAR(10) NOT NULL DEFAULT 'BASIC';
ALTER TABLE songs ADD COLUMN reference_audio_url        TEXT;
ALTER TABLE songs ADD COLUMN reference_contour          JSONB;
ALTER TABLE songs ADD COLUMN reference_duration_seconds DOUBLE PRECISION;
ALTER TABLE songs ADD COLUMN source_url                 TEXT;
ALTER TABLE songs ADD COLUMN attribution                TEXT;
ALTER TABLE songs ADD CONSTRAINT chk_songs_required_plan CHECK (required_plan IN ('FREE', 'BASIC', 'PRO'));

-- Lượt chấm cũ (bài tập / chơi tự do) không ánh xạ được sang tác phẩm → xoá (tính năng chưa phát hành)
DELETE FROM performance_attempts;
ALTER TABLE performance_attempts ADD COLUMN song_id UUID NOT NULL REFERENCES songs(id) ON DELETE CASCADE;
ALTER TABLE performance_attempts DROP COLUMN exercise_id;
ALTER TABLE performance_attempts DROP COLUMN mode;
ALTER TABLE performance_attempts ALTER COLUMN rhythm_score SET NOT NULL;

DROP TABLE IF EXISTS practice_exercises;

CREATE INDEX IF NOT EXISTS idx_performance_attempts_song ON performance_attempts(song_id);
