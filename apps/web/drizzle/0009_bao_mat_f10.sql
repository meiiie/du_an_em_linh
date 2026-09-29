-- F-10: giới hạn tần suất (khoá đăng nhập, hạn mức gia sư, nộp bước) lưu trong DB để sống qua khởi động lại
CREATE TABLE IF NOT EXISTS rate_limit_events (
  id bigserial PRIMARY KEY,
  khoa text NOT NULL,
  tao_luc timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS rate_limit_events_khoa_tao_luc ON rate_limit_events (khoa, tao_luc);
