-- Giới hạn đăng nhập sai (F-10, #69): mỗi lần sai một dòng. key_hash là SHA-256 (hex) của email đã chuẩn hóa + IP,
-- nên bảng không giữ email hay IP. Bản ghi quá một ngày được dọn mỗi đêm.
CREATE TABLE login_failures (
    id         bigint      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    key_hash   varchar(64) NOT NULL,
    created_at timestamptz NOT NULL
);

CREATE INDEX login_failures_key_created_idx ON login_failures (key_hash, created_at);
CREATE INDEX login_failures_created_idx ON login_failures (created_at);
