-- Lớp học (#81, specs/001-lat-cat-doc/data-model.md §classroom). Migration chỉ thêm, không sửa sau khi merge.
-- Quyền theo lớp (FR-032, F-08 của v0) kiểm ở tầng ứng dụng qua port ClassMembership (ADR 006), chưa dùng RLS.
CREATE TABLE classes (
    id          uuid        PRIMARY KEY,
    name        varchar(60) NOT NULL,
    grade       smallint    NOT NULL CHECK (grade BETWEEN 1 AND 12),
    school_year varchar(9)  NOT NULL CHECK (school_year ~ '^[0-9]{4}-[0-9]{4}$'),
    created_at  timestamptz NOT NULL,
    UNIQUE (name, school_year)
);

-- Một người một vai trong một lớp. Giáo viên dạy được nhiều lớp; học sinh chỉ thuộc một lớp (P2: mỗi học sinh một
-- lớp, mọi dữ liệu học của học sinh tính theo lớp đó). Mở rộng nhiều lớp thì bỏ chỉ mục duy nhất bằng migration mới.
CREATE TABLE enrollments (
    class_id      uuid        NOT NULL REFERENCES classes (id) ON DELETE CASCADE,
    user_id       uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_in_class varchar(10) NOT NULL CHECK (role_in_class IN ('TEACHER', 'STUDENT')),
    enrolled_at   timestamptz NOT NULL,
    PRIMARY KEY (class_id, user_id)
);

CREATE INDEX enrollments_user_id_idx ON enrollments (user_id);
CREATE UNIQUE INDEX enrollments_one_class_per_student_idx ON enrollments (user_id) WHERE role_in_class = 'STUDENT';

-- Cài lớp. Nhà AI chỉ là mã nhà; khóa do máy chủ quản (FR-019), không có cột khóa. Mặc định: không mở lời giải sau khi
-- nộp (FR-006), nhà offline, không cho máy cục bộ (core chạy trong container không tới được loopback, research R3).
CREATE TABLE class_settings (
    class_id                     uuid        PRIMARY KEY REFERENCES classes (id) ON DELETE CASCADE,
    reveal_solution_after_submit boolean     NOT NULL DEFAULT false,
    ai_provider                  varchar(32) NOT NULL DEFAULT 'offline' CHECK (ai_provider ~ '^[a-z][a-z0-9_-]{0,31}$'),
    ai_allow_local               boolean     NOT NULL DEFAULT false,
    updated_at                   timestamptz NOT NULL,
    updated_by                   uuid        REFERENCES users (id) ON DELETE SET NULL
);

-- Cảnh báo cho giáo viên của lớp: kẹt (mastery ghi, KET) và «gửi thầy cô» (tutor ghi, NHO_GV), qua port
-- CanhBaoGiaoVien. Bài, bước, kỹ năng tham chiếu bằng mã, không khóa ngoại sang bảng nội dung (module khác).
CREATE TABLE escalations (
    id           uuid         PRIMARY KEY,
    class_id     uuid         NOT NULL REFERENCES classes (id) ON DELETE CASCADE,
    student_id   uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    skill_code   varchar(64)  NOT NULL,
    problem_code varchar(64),
    step_code    varchar(64),
    kind         varchar(10)  NOT NULL CHECK (kind IN ('KET', 'NHO_GV')),
    reason       varchar(500) NOT NULL,
    created_at   timestamptz  NOT NULL,
    handled_at   timestamptz,
    handled_by   uuid         REFERENCES users (id) ON DELETE SET NULL,
    CHECK (handled_by IS NULL OR handled_at IS NOT NULL)
);

CREATE INDEX escalations_class_open_idx ON escalations (class_id, created_at) WHERE handled_at IS NULL;
CREATE INDEX escalations_student_idx ON escalations (student_id);
-- Không ghi trùng khi chưa xử lý: một cảnh báo mở cho mỗi (lớp, học sinh, loại, kỹ năng, bài), như v0 kiểm trước khi ghi.
CREATE UNIQUE INDEX escalations_one_open_idx
    ON escalations (class_id, student_id, kind, skill_code, coalesce(problem_code, ''))
    WHERE handled_at IS NULL;
