-- Mức hiểu (T049, specs/001-lat-cat-doc/data-model.md §mastery, research R7): BKT của v0 (apps/web/lib/learning.ts) theo
-- (học sinh, kỹ năng), cập nhật một lần mỗi bài làm đã nộp. Bảng ghi đè của giáo viên (FR-034, FR-035) có ở đây, chưa có API
-- (T060b). Dữ liệu học sinh: kiểm quyền ở tầng ứng dụng như ADR 006 (học sinh chỉ đọc của mình, giáo viên qua lớp mình dạy).
-- Kỹ năng là mã chụp lại như submissions.skill_code (V8), không khóa ngoại: xóa hay đổi danh mục không chặn nộp bài.

-- Tham số BKT có phiên bản. Dòng 'bkt' chép nguyên văn data/v0/bkt.json (giá trị seed.ts của v0 ghi vào mastery_config).
-- nguong_doan_mo_so_lan_doi_o giữ cho đủ bản v0; practice đọc ngưỡng đó từ app.practice.nguong-doan-mo.
CREATE TABLE mastery_config (
    key     varchar(32) PRIMARY KEY,
    value   jsonb       NOT NULL,
    version integer     NOT NULL CHECK (version > 0)
);

INSERT INTO mastery_config (key, value, version) VALUES ('bkt', '{
    "p_t": 0.12,
    "p_g": 0.2,
    "p_s": 0.1,
    "nguong_tin_cay_ma_loi": 0.65,
    "so_luot_ket": 3,
    "nguong_doan_mo_so_lan_doi_o": 4,
    "nguong_muc": {"THONG_HIEU": 0.4, "VAN_DUNG": 0.62, "VAN_DUNG_CAO": 0.82}
}', 1);

-- mastery là real như cột của v0: v0 đọc lại số float4 qua chữ số ngắn nhất (postgres.js), core đọc như vậy, nên lượt sau
-- tính trên đúng số v0 tính. bloom_level suy từ level4 theo thang của lab Sư phạm (skill math-pedagogy): Vận dụng cao gộp
-- ba mức Bloom, lưu mức thấp nhất là Phân tích. completed_at là lúc (gần nhất) lên Vận dụng cao, có khi và chỉ khi đang ở đó.
CREATE TABLE mastery_states (
    student_id       uuid          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    skill_code       varchar(32)   NOT NULL,
    mastery          real          NOT NULL CHECK (mastery BETWEEN 0 AND 1),
    level4           varchar(16)   NOT NULL CHECK (level4 IN ('NHAN_BIET', 'THONG_HIEU', 'VAN_DUNG', 'VAN_DUNG_CAO')),
    bloom_level      varchar(16)   NOT NULL GENERATED ALWAYS AS (CASE level4
        WHEN 'NHAN_BIET' THEN 'NHO' WHEN 'THONG_HIEU' THEN 'HIEU' WHEN 'VAN_DUNG' THEN 'VAN_DUNG' ELSE 'PHAN_TICH' END) STORED,
    attempts         integer       NOT NULL CHECK (attempts >= 0),
    stuck_counter    integer       NOT NULL CHECK (stuck_counter >= 0),
    -- Mã lỗi gặp gần đây, không trùng, giữ thứ tự gặp lần đầu, tối đa 8 (lastErrorCodes của v0).
    last_error_codes varchar(32)[] NOT NULL DEFAULT '{}' CHECK (cardinality(last_error_codes) <= 8),
    completed_at     timestamptz,
    PRIMARY KEY (student_id, skill_code),
    CHECK ((level4 = 'VAN_DUNG_CAO') = (completed_at IS NOT NULL))
);

-- Mỗi bài làm đã nộp được tính nhiều nhất một lần (UNIQUE submission_id): nộp lại phát lại sự kiện, không tính lần hai. Bài
-- không được tính (KHONG_KIEM_DUOC, lỗi trình bày dấu U) không có sự kiện. Không khóa ngoại sang submissions: mức hiểu là của
-- học sinh, xóa lớp không xóa điều em đã học; xóa tài khoản xóa cả hai qua student_id. Nghi đoán mò thì không đổi mức hiểu.
CREATE TABLE mastery_events (
    id              uuid        PRIMARY KEY,
    student_id      uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    skill_code      varchar(32) NOT NULL,
    submission_id   uuid        NOT NULL UNIQUE,
    delta           real        NOT NULL,
    rule_applied    varchar(20) NOT NULL CHECK (rule_applied IN ('THEO_KY_NANG_BAI', 'THEO_MA_LOI', 'THEO_BUOC', 'NGHI_DOAN_MO')),
    wrong_step      varchar(32),
    error_code      varchar(32),
    confidence      real        CHECK (confidence BETWEEN 0 AND 1),
    guess_suspected boolean     NOT NULL,
    level4_before   varchar(16) NOT NULL CHECK (level4_before IN ('NHAN_BIET', 'THONG_HIEU', 'VAN_DUNG', 'VAN_DUNG_CAO')),
    level4_after    varchar(16) NOT NULL CHECK (level4_after IN ('NHAN_BIET', 'THONG_HIEU', 'VAN_DUNG', 'VAN_DUNG_CAO')),
    config_version  integer     NOT NULL,
    created_at      timestamptz NOT NULL,
    CHECK (guess_suspected = (rule_applied = 'NGHI_DOAN_MO')),
    CHECK (NOT guess_suspected OR (delta = 0 AND level4_before = level4_after))
);

CREATE INDEX mastery_events_cua_hoc_sinh ON mastery_events (student_id, skill_code, created_at);

-- Ghi đè mức của giáo viên (FR-034): bản đang hiệu lực là bản chưa gỡ mới nhất; máy vẫn tính mức của nó song song.
CREATE TABLE mastery_overrides (
    id          uuid         PRIMARY KEY,
    student_id  uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    skill_code  varchar(32)  NOT NULL,
    level4      varchar(16)  NOT NULL CHECK (level4 IN ('NHAN_BIET', 'THONG_HIEU', 'VAN_DUNG', 'VAN_DUNG_CAO')),
    reason      varchar(500) NOT NULL CHECK (btrim(reason) <> ''),
    teacher_id  uuid         REFERENCES users (id) ON DELETE SET NULL,
    created_at  timestamptz  NOT NULL,
    removed_at  timestamptz,
    removed_by  uuid         REFERENCES users (id) ON DELETE SET NULL,
    CHECK (removed_by IS NULL OR removed_at IS NOT NULL)
);

CREATE INDEX mastery_overrides_cua_hoc_sinh ON mastery_overrides (student_id, skill_code, created_at) WHERE removed_at IS NULL;

-- Bài kế giáo viên chọn tay (FR-035): hiện trước đề xuất của máy, hết hiệu lực khi học sinh mở bài.
CREATE TABLE next_problem_overrides (
    id          uuid         PRIMARY KEY,
    student_id  uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    problem_id  uuid         NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    reason      varchar(500) NOT NULL CHECK (btrim(reason) <> ''),
    teacher_id  uuid         REFERENCES users (id) ON DELETE SET NULL,
    created_at  timestamptz  NOT NULL,
    consumed_at timestamptz
);

CREATE INDEX next_problem_overrides_dang_cho ON next_problem_overrides (student_id, created_at) WHERE consumed_at IS NULL;
