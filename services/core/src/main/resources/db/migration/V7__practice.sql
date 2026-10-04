-- Module practice (data-model §practice, #87): giao bài, bài làm theo bước, kết quả chấm. Các bất biến, giữ ở CSDL vì
-- hai tab của cùng học sinh có thể nộp cùng lúc:
--   1. Giao bài và mở bài làm chỉ cho học sinh của lớp (ghi danh vai STUDENT), chỉ với bài DA_PHAT_HANH của chính lớp đó.
--   2. Mỗi (học sinh, lớp, bài, phiên bản nội dung) có tối đa một bài làm DANG_LAM: nội dung bài đổi thì bài làm dở cũ
--      (viết cho đề cũ) thôi được chấm và học sinh mở bài làm mới. Bài làm đã nộp thì bước, bảng, sự kiện nhập và kết
--      quả chấm của nó không đổi nữa.
--   3. Kết quả chấm chỉ thêm. Chấm lại đúng một yêu cầu (băm của payload /v1/grade) không ghi lần hai: hai tab nộp cùng
--      bước ghi một lần. Riêng KHONG_CHAM_DUOC (dịch vụ toán lỗi, không bao giờ là đạt: FR-009) không chặn lần chấm lại;
--      nhưng đã có phán quyết thì không ghi thêm KHONG_CHAM_DUOC cho yêu cầu đó.
--   4. Bài làm của phiên bản nội dung cũ (bài đã đổi đề) thôi được chấm, ghi bước hay nộp.
-- Bài làm lưu nội dung mới nhất của từng bước, đủ để dựng lại đúng payload /v1/grade của v0 (thứ tự dòng, nhãn dòng, thứ
-- tự ô bảng), để kết quả chấm so được với v0 (SC-006).

CREATE TABLE assignments (
    id          uuid         PRIMARY KEY,
    class_id    uuid         NOT NULL REFERENCES classes (id) ON DELETE CASCADE,
    problem_id  uuid         NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    student_id  uuid         NOT NULL,
    -- v0 chỉ có «assigned»; hủy giao giữ dòng để còn lịch sử.
    status      varchar(8)   NOT NULL CHECK (status IN ('DA_GIAO', 'DA_HUY')),
    set_name    varchar(120),
    due_at      timestamptz,
    assigned_by uuid         REFERENCES users (id) ON DELETE SET NULL,
    assigned_at timestamptz  NOT NULL,
    UNIQUE (class_id, problem_id, student_id),
    FOREIGN KEY (class_id, student_id) REFERENCES enrollments (class_id, user_id) ON DELETE CASCADE
);
CREATE INDEX assignments_cua_hoc_sinh ON assignments (student_id, class_id);

CREATE TABLE submissions (
    id              uuid         PRIMARY KEY,
    class_id        uuid         NOT NULL,
    student_id      uuid         NOT NULL,
    problem_id      uuid         NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    -- Phiên bản nội dung của bài lúc mở bài làm (problems.content_version, V5): nội dung đổi thì bài làm cũ không chấm tiếp.
    content_version integer      NOT NULL CHECK (content_version > 0),
    status          varchar(8)   NOT NULL CHECK (status IN ('DANG_LAM', 'DA_NOP')),
    guess_suspected boolean      NOT NULL DEFAULT false,
    guess_reason    varchar(300),
    -- Kết quả lúc nộp; KHONG_CHAM_DUOC = dịch vụ toán lỗi, không bao giờ là đạt.
    result          varchar(16)  CHECK (result IN ('DAT', 'SAI', 'KHONG_KIEM_DUOC', 'KHONG_CHAM_DUOC')),
    started_at      timestamptz  NOT NULL,
    submitted_at    timestamptz,
    FOREIGN KEY (class_id, student_id) REFERENCES enrollments (class_id, user_id) ON DELETE CASCADE,
    CHECK ((status = 'DA_NOP') = (submitted_at IS NOT NULL)),
    CHECK ((status = 'DA_NOP') = (result IS NOT NULL)),
    CHECK (guess_suspected OR guess_reason IS NULL)
);
CREATE UNIQUE INDEX submissions_mot_bai_dang_lam ON submissions (student_id, class_id, problem_id, content_version)
    WHERE status = 'DANG_LAM';
CREATE INDEX submissions_cua_hoc_sinh ON submissions (student_id, class_id, problem_id, started_at);

-- Dòng của một bước kiểu DONG. line_kind là nhãn «loai» của v0 (NGHIEM, KHONG_XD, DONG_BIEN…), không có thì null.
CREATE TABLE submission_steps (
    submission_id uuid         NOT NULL REFERENCES submissions (id) ON DELETE CASCADE,
    step_code     varchar(32)  NOT NULL REFERENCES step_templates (step_code),
    line_no       smallint     NOT NULL CHECK (line_no >= 0),
    latex         text         NOT NULL CHECK (length(latex) <= 2000),
    line_kind     varchar(16)  CHECK (line_kind ~ '^[A-Z][A-Z0-9_]{0,15}$'),
    PRIMARY KEY (submission_id, step_code, line_no)
);

-- Bảng của một bước kiểu BANG (bảng xét dấu); ô: row_code (X, DAU_YPHAY, BIEN_THIEN), k 0-based (docs/chi-so-o-bang.md),
-- ordinal giữ thứ tự ô như học sinh gửi.
CREATE TABLE submission_tables (
    submission_id uuid         NOT NULL REFERENCES submissions (id) ON DELETE CASCADE,
    step_code     varchar(32)  NOT NULL REFERENCES step_templates (step_code),
    table_kind    varchar(16)  NOT NULL CHECK (table_kind ~ '^[A-Z][A-Z0-9_]{0,15}$'),
    PRIMARY KEY (submission_id, step_code)
);
CREATE TABLE submission_table_cells (
    submission_id uuid         NOT NULL,
    step_code     varchar(32)  NOT NULL,
    ordinal       smallint     NOT NULL CHECK (ordinal >= 0),
    row_code      varchar(16)  NOT NULL CHECK (row_code ~ '^[A-Z][A-Z0-9_]{0,15}$'),
    k             smallint     CHECK (k >= 0),
    value         varchar(200) NOT NULL,
    PRIMARY KEY (submission_id, step_code, ordinal),
    FOREIGN KEY (submission_id, step_code) REFERENCES submission_tables (submission_id, step_code) ON DELETE CASCADE
);

-- Sự kiện sửa ô, cho nghi đoán mò (v0: nghiDoanMo). Chỉ thêm.
CREATE TABLE input_events (
    id            uuid         PRIMARY KEY,
    submission_id uuid         NOT NULL REFERENCES submissions (id) ON DELETE CASCADE,
    step_code     varchar(32)  NOT NULL REFERENCES step_templates (step_code),
    cell_row      varchar(16),
    cell_k        smallint     CHECK (cell_k >= 0),
    old_value     varchar(200),
    new_value     varchar(200) NOT NULL,
    at            timestamptz  NOT NULL,
    CHECK ((cell_row IS NULL) = (cell_k IS NULL))
);
CREATE INDEX input_events_cua_bai_lam ON input_events (submission_id, at);

-- Một lần chấm: step_code là bước nộp tới (nop_toi). request_hash là SHA-256 (hex) của payload /v1/grade đã chuẩn hóa.
-- Các cột theo phản hồi của v0: ket_qua, loai_ket_qua, buoc_sai, ma_loi, do_tin_cay, per_buoc, thong_bao, cac_van_de,
-- toan_dung, chua_xong, phien_ban_chuan_hoa, chuan_hoa.
CREATE TABLE grading_results (
    id                 uuid         PRIMARY KEY,
    submission_id      uuid         NOT NULL REFERENCES submissions (id) ON DELETE CASCADE,
    step_code          varchar(32)  NOT NULL REFERENCES step_templates (step_code),
    request_hash       char(64)     NOT NULL CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    result             varchar(16)  NOT NULL CHECK (result IN ('DAT', 'SAI', 'KHONG_KIEM_DUOC', 'KHONG_CHAM_DUOC')),
    result_type        varchar(64),
    wrong_steps        jsonb,
    error_code         varchar(32),
    confidence         real         CHECK (confidence BETWEEN 0 AND 1),
    per_step           jsonb        NOT NULL DEFAULT '{}',
    message            text,
    issues             jsonb,
    math_ok            boolean,
    unfinished         boolean      NOT NULL DEFAULT false,
    normalizer_version varchar(32),
    normalization      jsonb,
    graded_at          timestamptz  NOT NULL,
    -- Dịch vụ toán lỗi: không có phán quyết nào đi kèm.
    CHECK (result <> 'KHONG_CHAM_DUOC' OR (wrong_steps IS NULL AND error_code IS NULL AND confidence IS NULL AND math_ok IS NULL))
);
CREATE UNIQUE INDEX grading_results_mot_lan_cham ON grading_results (submission_id, request_hash) WHERE result <> 'KHONG_CHAM_DUOC';
CREATE INDEX grading_results_cua_bai_lam ON grading_results (submission_id, graded_at);

-- 1. Học sinh của lớp, bài đã phát hành cho lớp: kiểm lúc giao bài và lúc mở bài làm (khóa dòng phát hành FOR SHARE để
-- lần rút phát hành đồng thời chờ). Sau đó bài có thể bị rút: bài làm cũ còn đó, việc chặn mở tiếp là của tầng ứng dụng.
CREATE FUNCTION practice_hoc_sinh_va_bai_phat_hanh() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    -- Hủy giao không cần bài còn phát hành. IF lồng: NEW.status của submissions là trạng thái khác.
    IF TG_TABLE_NAME = 'assignments' THEN
        IF NEW.status = 'DA_HUY' THEN
            RETURN NEW;
        END IF;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM enrollments WHERE class_id = NEW.class_id AND user_id = NEW.student_id AND role_in_class = 'STUDENT') THEN
        RAISE EXCEPTION 'Người dùng % không là học sinh của lớp %', NEW.student_id, NEW.class_id USING ERRCODE = 'check_violation';
    END IF;
    PERFORM 1 FROM problem_releases WHERE class_id = NEW.class_id AND problem_id = NEW.problem_id AND status = 'DA_PHAT_HANH' FOR SHARE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Bài % chưa phát hành cho lớp %', NEW.problem_id, NEW.class_id USING ERRCODE = 'check_violation';
    END IF;
    -- IF lồng: PL/pgSQL không ngắt mạch AND, đọc NEW.content_version trên assignments là lỗi «không có trường».
    IF TG_TABLE_NAME = 'submissions' THEN
        IF NEW.content_version IS DISTINCT FROM (SELECT content_version FROM problems WHERE id = NEW.problem_id) THEN
            RAISE EXCEPTION 'Bài làm mở với phiên bản nội dung % không phải phiên bản hiện tại của bài %', NEW.content_version,
                NEW.problem_id USING ERRCODE = 'check_violation';
        END IF;
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER assignments_hoc_sinh_va_bai_phat_hanh BEFORE INSERT OR UPDATE OF class_id, problem_id, student_id, status ON assignments
    FOR EACH ROW EXECUTE FUNCTION practice_hoc_sinh_va_bai_phat_hanh();
CREATE TRIGGER submissions_hoc_sinh_va_bai_phat_hanh BEFORE INSERT ON submissions
    FOR EACH ROW EXECUTE FUNCTION practice_hoc_sinh_va_bai_phat_hanh();

-- 2. Bài làm: lớp, học sinh, bài, phiên bản nội dung, lúc mở không đổi; chỉ đi DANG_LAM → DA_NOP, một lần; cờ nghi đoán
-- mò chỉ bật.
CREATE FUNCTION submissions_chi_nop_mot_lan() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.class_id, NEW.student_id, NEW.problem_id, NEW.content_version, NEW.started_at)
            IS DISTINCT FROM (OLD.class_id, OLD.student_id, OLD.problem_id, OLD.content_version, OLD.started_at) THEN
        RAISE EXCEPTION 'Bài làm % không đổi lớp, học sinh, bài, phiên bản hay lúc mở', OLD.id USING ERRCODE = 'check_violation';
    END IF;
    IF OLD.status = 'DA_NOP' THEN
        RAISE EXCEPTION 'Bài làm % đã nộp, không sửa được', OLD.id USING ERRCODE = 'check_violation';
    END IF;
    -- Cờ nghi đoán mò chỉ bật, lý do đầu giữ nguyên.
    IF (OLD.guess_suspected AND NOT NEW.guess_suspected)
            OR (OLD.guess_reason IS NOT NULL AND NEW.guess_reason IS DISTINCT FROM OLD.guess_reason) THEN
        RAISE EXCEPTION 'Bài làm % đã bị nghi đoán mò: cờ và lý do không đổi được', OLD.id USING ERRCODE = 'check_violation';
    END IF;
    -- Bài làm của phiên bản nội dung cũ (bài đã đổi đề) thôi được nộp hay đánh dấu. Khóa dòng bài FOR SHARE: lần đổi nội
    -- dung đồng thời chờ tới khi giao dịch này xong.
    PERFORM 1 FROM problems WHERE id = OLD.problem_id AND content_version = OLD.content_version FOR SHARE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Bài làm % là của phiên bản nội dung cũ: không ghi được', OLD.id USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER submissions_chi_nop_mot_lan BEFORE UPDATE ON submissions
    FOR EACH ROW EXECUTE FUNCTION submissions_chi_nop_mot_lan();

-- 2. Phần con của bài làm (bước, bảng, ô, sự kiện, kết quả chấm) chỉ ghi khi bài làm còn DANG_LAM và đúng phiên bản nội
-- dung hiện tại của bài. Khóa dòng bài làm và dòng bài: lần nộp bài và lần đổi nội dung đồng thời chờ, nên không có phần
-- con nào vào sau lúc nộp hay cho đề đã đổi. Kết quả chấm khóa dòng bài làm FOR NO KEY UPDATE (các lần ghi kết quả của
-- cùng bài làm nối tiếp nhau, cho luật 3); phần con khác FOR SHARE. Nơi ghi phải khóa dòng bài làm từ đầu bằng mức đó
-- hay mạnh hơn (adapter: FOR NO KEY UPDATE), để không có hai giao dịch cùng nâng khóa rồi chờ nhau.
CREATE FUNCTION practice_bai_lam_dang_mo() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    bai_lam uuid := CASE WHEN TG_OP = 'DELETE' THEN OLD.submission_id ELSE NEW.submission_id END;
BEGIN
    -- Xóa theo dây chuyền khi xóa cả bài làm (hay lớp, học sinh, bài) thì cho qua.
    IF TG_OP = 'DELETE' AND NOT EXISTS (SELECT 1 FROM submissions WHERE id = bai_lam) THEN
        RETURN OLD;
    END IF;
    IF TG_TABLE_NAME = 'grading_results' THEN
        PERFORM 1 FROM submissions s JOIN problems p ON p.id = s.problem_id
            WHERE s.id = bai_lam AND s.status = 'DANG_LAM' AND p.content_version = s.content_version
            FOR NO KEY UPDATE OF s FOR SHARE OF p;
    ELSE
        PERFORM 1 FROM submissions s JOIN problems p ON p.id = s.problem_id
            WHERE s.id = bai_lam AND s.status = 'DANG_LAM' AND p.content_version = s.content_version FOR SHARE OF s, p;
    END IF;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Bài làm % đã nộp, không có, hay là của phiên bản nội dung cũ: không ghi thêm được', bai_lam
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END $$;
CREATE TRIGGER submission_steps_bai_lam_dang_mo BEFORE INSERT OR UPDATE OR DELETE ON submission_steps
    FOR EACH ROW EXECUTE FUNCTION practice_bai_lam_dang_mo();
CREATE TRIGGER submission_tables_bai_lam_dang_mo BEFORE INSERT OR UPDATE OR DELETE ON submission_tables
    FOR EACH ROW EXECUTE FUNCTION practice_bai_lam_dang_mo();
CREATE TRIGGER submission_table_cells_bai_lam_dang_mo BEFORE INSERT OR UPDATE OR DELETE ON submission_table_cells
    FOR EACH ROW EXECUTE FUNCTION practice_bai_lam_dang_mo();
CREATE TRIGGER input_events_bai_lam_dang_mo BEFORE INSERT ON input_events
    FOR EACH ROW EXECUTE FUNCTION practice_bai_lam_dang_mo();
CREATE TRIGGER grading_results_bai_lam_dang_mo BEFORE INSERT ON grading_results
    FOR EACH ROW EXECUTE FUNCTION practice_bai_lam_dang_mo();

-- 3. Yêu cầu chấm đã có phán quyết thì không ghi thêm lần «không chấm được» cho nó: lịch sử của một yêu cầu không bao giờ
-- có lỗi sau phán quyết. Chạy sau trigger trên (tên xếp sau), khi dòng bài làm đã bị khóa FOR NO KEY UPDATE.
CREATE FUNCTION grading_results_giu_phan_quyet() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.result = 'KHONG_CHAM_DUOC' AND EXISTS (SELECT 1 FROM grading_results
            WHERE submission_id = NEW.submission_id AND request_hash = NEW.request_hash AND result <> 'KHONG_CHAM_DUOC') THEN
        RAISE EXCEPTION 'Yêu cầu chấm % của bài làm % đã có phán quyết', NEW.request_hash, NEW.submission_id
            USING ERRCODE = 'unique_violation';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER grading_results_giu_phan_quyet BEFORE INSERT ON grading_results
    FOR EACH ROW EXECUTE FUNCTION grading_results_giu_phan_quyet();

-- 3. Sự kiện nhập và kết quả chấm chỉ thêm: không sửa, không xóa riêng (xóa cả bài làm thì theo dây chuyền).
CREATE FUNCTION practice_chi_them() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION '% chỉ thêm, không sửa được', TG_TABLE_NAME USING ERRCODE = 'check_violation';
    END IF;
    IF EXISTS (SELECT 1 FROM submissions WHERE id = OLD.submission_id) THEN
        RAISE EXCEPTION '% của bài làm % không xóa riêng được', TG_TABLE_NAME, OLD.submission_id USING ERRCODE = 'check_violation';
    END IF;
    RETURN OLD;
END $$;
CREATE TRIGGER input_events_chi_them BEFORE UPDATE OR DELETE ON input_events
    FOR EACH ROW EXECUTE FUNCTION practice_chi_them();
CREATE TRIGGER grading_results_chi_them BEFORE UPDATE OR DELETE ON grading_results
    FOR EACH ROW EXECUTE FUNCTION practice_chi_them();
