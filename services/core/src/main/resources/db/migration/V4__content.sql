-- Nội dung chủ đề (#85, specs/001-lat-cat-doc/data-model.md §content). Migration chỉ thêm, không sửa sau khi merge.
-- Hai phần:
--   * nội dung chung của chủ đề: chủ đề, kỹ năng, mã lỗi, khung bước, bài, lời giải, thang gợi ý. Importer nạp từ
--     data/supham và data/v0 (research R6), giáo viên sửa sau (US3);
--   * phần theo lớp: tài liệu, bảng công thức, lượt kiểm, trạng thái phát hành, duyệt. Tầng 2 và 3 của cổng dùng tài liệu
--     và bảng của lớp, nên kết quả kiểm và phát hành tính theo lớp (FR-004).
-- Giá trị trạng thái giữ mã của v0 để đối chiếu: DAT, SAI, KHONG_KIEM_DUOC, GV_DUYET; NHAP, DA_PHAT_HANH, BI_CHAN,
-- CHO_GIAO_VIEN_DUYET. Không có dữ liệu học sinh trong các bảng này.

-- ---- Nội dung chung -----------------------------------------------------------------------------------------------

-- Mã chủ đề giữ như v0 (DH12), để đối chiếu với data/v0 và tệp vàng của v0.
CREATE TABLE topics (
    code  varchar(32)  PRIMARY KEY,
    name  varchar(300) NOT NULL,
    grade smallint     NOT NULL CHECK (grade BETWEEN 1 AND 12)
);

-- Danh mục kỹ năng của lab (data/supham/danh-muc-ky-nang-DH.json), gồm cả kỹ năng tiên quyết ngoài chủ đề.
CREATE TABLE skills (
    code        varchar(32)  PRIMARY KEY,
    topic_code  varchar(32)  NOT NULL REFERENCES topics (code),
    name        varchar(300) NOT NULL,
    -- Yêu cầu cần đạt trích nguyên văn CT GDPT 2018 kèm trang (FR-003), như cột description của v0.
    description text,
    grade       smallint     CHECK (grade BETWEEN 1 AND 12),
    is_core     boolean      NOT NULL
);

CREATE TABLE skill_prerequisites (
    skill_code        varchar(32) NOT NULL REFERENCES skills (code) ON DELETE CASCADE,
    prerequisite_code varchar(32) NOT NULL REFERENCES skills (code) ON DELETE CASCADE,
    -- Mức tối thiểu của kỹ năng tiên quyết trước khi mở kỹ năng này (`muc_toi_thieu` của danh mục, mã 4 mức).
    min_level         varchar(3)  CHECK (min_level IN ('NB', 'TH', 'VD', 'VDC')),
    PRIMARY KEY (skill_code, prerequisite_code),
    CHECK (skill_code <> prerequisite_code)
);

-- Khung bước của dạng tự luận (data/v0/khung-buoc.json): B.DH.TXD … B.DH.KETLUAN.
CREATE TABLE step_templates (
    step_code   varchar(32)  PRIMARY KEY,
    topic_code  varchar(32)  NOT NULL REFERENCES topics (code),
    ordinal     smallint     NOT NULL CHECK (ordinal > 0),
    input_kind  varchar(8)   NOT NULL CHECK (input_kind IN ('DONG', 'BANG')),
    skill_code  varchar(32)  REFERENCES skills (code),
    description varchar(300) NOT NULL,
    UNIQUE (topic_code, ordinal)
);

-- Mã lỗi của lab (data/supham/ma-loi-DH.csv). step_code không có khóa ngoại: CSV có bước của dạng khác ngoài khung 5
-- bước (B.DH.DOCBANG, B.DH.THAMSO, B.DH.DAOHAM2…), như v0.
CREATE TABLE error_types (
    code         varchar(32)   PRIMARY KEY,
    skill_code   varchar(32)   REFERENCES skills (code),
    step_code    varchar(32),
    name         text          NOT NULL,
    fix_hint     text,
    -- Loại kết quả liên quan (`loai_ket_qua_lien_quan` của CSV, tách theo «|»).
    result_types varchar(32)[] NOT NULL DEFAULT '{}'
);

-- Bài: nội dung chung của chủ đề. Trạng thái phát hành nằm ở problem_releases theo lớp.
CREATE TABLE problems (
    id                uuid          PRIMARY KEY,
    code              varchar(64)   NOT NULL UNIQUE,
    skill_code        varchar(32)   NOT NULL REFERENCES skills (code),
    extra_skill_codes varchar(32)[] NOT NULL DEFAULT '{}',
    level4            varchar(16)   NOT NULL CHECK (level4 IN ('NHAN_BIET', 'THONG_HIEU', 'VAN_DUNG', 'VAN_DUNG_CAO')),
    -- 3 mức CV 7991 chỉ để hiển thị (ADR 004); v0 suy từ 4 mức khi lab không ghi.
    level3            varchar(16)   CHECK (level3 IN ('BIET', 'HIEU', 'VAN_DUNG')),
    bloom_level       varchar(16)   CHECK (bloom_level IN ('NHO', 'HIEU', 'VAN_DUNG', 'PHAN_TICH', 'DANH_GIA', 'SANG_TAO')),
    difficulty        real          CHECK (difficulty BETWEEN 0 AND 1),
    statement_text    text          NOT NULL,
    statement_latex   text          NOT NULL,
    function_sympy    text,
    -- TU_LUAN_5_BUOC: khung 5 bước; dạng khác (TN_DUNG_SAI, TRA_LOI_NGAN…) chưa có khung làm trên app (SP-06 của v0).
    answer_form       varchar(32)   NOT NULL DEFAULT 'TU_LUAN_5_BUOC' CHECK (answer_form ~ '^[A-Z][A-Z0-9_]{0,31}$'),
    -- Bài khung ngắn bắt đầu giữa khung (bai-khung-ngan.seed-v01.json).
    start_step        varchar(32)   REFERENCES step_templates (step_code),
    -- Nguồn của bài như v0: SUPHAM, SUPHAM_KHUNG_NGAN, MAY_GIAI, THAM_SO_HOA, VI_DU_CONG.
    origin            varchar(32)   NOT NULL CHECK (origin ~ '^[A-Z][A-Z0-9_]{0,31}$'),
    -- SHA-256 (hex) của đề, lời giải, thang gợi ý; đổi nội dung thì lượt kiểm cũ không còn dùng được.
    content_hash      varchar(64)   NOT NULL CHECK (content_hash ~ '^[0-9a-f]{64}$'),
    created_by        uuid          REFERENCES users (id) ON DELETE SET NULL,
    created_at        timestamptz   NOT NULL,
    updated_at        timestamptz   NOT NULL
);

CREATE INDEX problems_skill_code_idx ON problems (skill_code);

-- Lời giải mẫu và dữ kiện bảo vệ: không bao giờ vào DTO của học sinh khi đang làm, không vào prompt (FR-006, ADR 003).
-- Tách bảng để truy vấn đề bài cho học sinh không chạm tới lời giải.
CREATE TABLE solutions (
    problem_id      uuid  PRIMARY KEY REFERENCES problems (id) ON DELETE CASCADE,
    worked_solution jsonb,
    protected_facts jsonb NOT NULL DEFAULT '[]',
    final_answer    text
);

-- Thang gợi ý của bài theo bước. step_code không có khóa ngoại: dạng khác khung 5 bước có bước riêng, như v0.
CREATE TABLE hint_levels (
    problem_id uuid        NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    step_code  varchar(32) NOT NULL,
    level      smallint    NOT NULL CHECK (level BETWEEN 1 AND 3),
    text       text        NOT NULL,
    PRIMARY KEY (problem_id, step_code, level)
);

-- ---- Theo lớp -----------------------------------------------------------------------------------------------------

-- Tài liệu của lớp, căn cứ tầng 2. license_status = chua_ro thì không làm căn cứ, không được trích dẫn (R9).
CREATE TABLE documents (
    id             uuid         PRIMARY KEY,
    class_id       uuid         NOT NULL REFERENCES classes (id) ON DELETE CASCADE,
    -- Mã ổn định của tài liệu nhập (v0-don-dieu, sp-tai-lieu-0001…): importer nhận lại khi nạp lần hai. Tài liệu giáo
    -- viên tải lên để trống.
    code           varchar(64),
    title          varchar(300) NOT NULL,
    kind           varchar(16)  NOT NULL CHECK (kind IN ('tu_soan', 'de_mau', 'tham_khao')),
    source         varchar(300),
    -- Mã quyền dùng như v0 và services/math (app/verify.py, QUYEN_HOP_LE); dịch vụ toán quyết mã nào làm căn cứ.
    license_status varchar(32)  NOT NULL CHECK (license_status ~ '^[a-z][a-z0-9_]{0,31}$'),
    file_ref       varchar(500),
    text_content   text         NOT NULL,
    version        integer      NOT NULL DEFAULT 1 CHECK (version > 0),
    uploaded_by    uuid         REFERENCES users (id) ON DELETE SET NULL,
    created_at     timestamptz  NOT NULL,
    UNIQUE (class_id, code)
);

-- Đoạn của tài liệu có vị trí, cho tầng 2 và trích dẫn [n]. text_folded: chữ thường, bỏ dấu, để tìm cụm từ như v0
-- (apps/web/lib/kien-thuc.ts), không vector (R9).
CREATE TABLE document_passages (
    id          uuid    PRIMARY KEY,
    document_id uuid    NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    page        integer CHECK (page > 0),
    char_start  integer NOT NULL CHECK (char_start >= 0),
    char_end    integer NOT NULL,
    text        text    NOT NULL,
    text_folded text    NOT NULL,
    CHECK (char_end > char_start),
    UNIQUE (document_id, char_start)
);

-- Bảng công thức của lớp, có phiên bản. Mỗi lần khóa là một phiên bản; bảng đang dùng là bảng KHOA mới nhất của lớp.
-- Sửa bảng đã khóa = tạo bảng nháp phiên bản mới; mỗi lớp nhiều nhất một bảng nháp. Bảng KHOA không đổi được nữa
-- (trigger cuối tệp), nên lưu một bảng khóa theo thứ tự: ghi bảng NHAP và các dòng, rồi đổi sang KHOA.
CREATE TABLE formula_sheets (
    id          uuid         PRIMARY KEY,
    class_id    uuid         NOT NULL REFERENCES classes (id) ON DELETE CASCADE,
    version     integer      NOT NULL CHECK (version > 0),
    status      varchar(4)   NOT NULL CHECK (status IN ('NHAP', 'KHOA')),
    note        varchar(500),
    -- SHA-256 (hex) của các dòng lúc khóa; lượt kiểm ghi bảng đã dùng.
    fingerprint varchar(64)  CHECK (fingerprint ~ '^[0-9a-f]{64}$'),
    locked_at   timestamptz,
    -- Trống khi importer khóa bảng của v0.
    locked_by   uuid         REFERENCES users (id) ON DELETE SET NULL,
    created_at  timestamptz  NOT NULL,
    UNIQUE (class_id, version),
    UNIQUE (id, class_id),
    CHECK ((status = 'KHOA') = (locked_at IS NOT NULL AND fingerprint IS NOT NULL)),
    CHECK (locked_by IS NULL OR status = 'KHOA')
);

CREATE UNIQUE INDEX formula_sheets_one_draft_idx ON formula_sheets (class_id) WHERE status = 'NHAP';

-- Dòng của bảng công thức và kết quả kiểm lúc khóa (job kiem-dong-cong-thuc, ADR 013). Khóa được bảng chỉ khi mọi dòng
-- có tier1_status = DAT và tier2_status = DAT có trích dẫn; SAI hay KHONG_KIEM_DUOC chặn khóa. Căn cứ đầy đủ của từng
-- tầng (can_cu, phản ví dụ, mọi trích dẫn) ở tier1_detail, tier2_detail; trích dẫn chính ở citation_passage_id.
CREATE TABLE formulas (
    id                  uuid         PRIMARY KEY,
    formula_sheet_id    uuid         NOT NULL REFERENCES formula_sheets (id) ON DELETE CASCADE,
    ordinal             smallint     NOT NULL CHECK (ordinal > 0),
    -- Mã dòng ổn định trong bảng (d-1… của v0): job trả kết quả theo mã.
    code                varchar(32)  NOT NULL,
    skill_code          varchar(32)  REFERENCES skills (code),
    title               varchar(200) NOT NULL,
    latex               text         NOT NULL,
    statement           text         NOT NULL,
    kind                varchar(16)  CHECK (kind IN ('DANG_THUC', 'DINH_LI', 'KHONG_BIET')),
    tier1_status        varchar(16)  CHECK (tier1_status IN ('DAT', 'SAI', 'KHONG_KIEM_DUOC')),
    tier2_status        varchar(16)  CHECK (tier2_status IN ('DAT', 'SAI', 'KHONG_KIEM_DUOC')),
    tier1_detail        jsonb,
    tier2_detail        jsonb,
    citation_passage_id uuid         REFERENCES document_passages (id),
    UNIQUE (formula_sheet_id, ordinal),
    UNIQUE (formula_sheet_id, code),
    CHECK (tier2_status IS DISTINCT FROM 'DAT' OR citation_passage_id IS NOT NULL),
    CHECK ((tier1_status IS NULL AND tier2_status IS NULL) OR kind IS NOT NULL)
);

-- Đoạn trích thêm của tầng 2 (trich_dan_them): dòng định lí cần một đoạn cho mỗi mệnh đề (contracts/math-v1.md). Khóa
-- ngoại giữ tài liệu không bị xóa khi còn là căn cứ của một dòng, như trích dẫn chính. Đoạn phải thuộc tài liệu cùng lớp
-- với bảng (trigger cuối tệp).
CREATE TABLE formula_citations (
    formula_id uuid NOT NULL REFERENCES formulas (id) ON DELETE CASCADE,
    passage_id uuid NOT NULL REFERENCES document_passages (id),
    PRIMARY KEY (formula_id, passage_id)
);

CREATE INDEX formula_citations_passage_idx ON formula_citations (passage_id);

-- Câu gợi ý đã điền tham số của đề, qua cổng với bảng công thức của lớp. Lúc chạy chỉ dùng câu DAT với phiên bản bảng
-- hiện tại (ADR 013 mục 6). result_kind: loại kết quả, hoặc «chung».
CREATE TABLE hint_gate_results (
    id               uuid        PRIMARY KEY,
    problem_id       uuid        NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    step_code        varchar(32) NOT NULL,
    result_kind      varchar(32) NOT NULL,
    level            smallint    NOT NULL CHECK (level BETWEEN 1 AND 3),
    text             text        NOT NULL,
    formula_sheet_id uuid        NOT NULL REFERENCES formula_sheets (id) ON DELETE CASCADE,
    status           varchar(16) NOT NULL CHECK (status IN ('DAT', 'SAI', 'KHONG_KIEM_DUOC')),
    checked_at       timestamptz NOT NULL,
    UNIQUE (problem_id, step_code, result_kind, level, formula_sheet_id)
);

-- Một lượt kiểm 3 tầng, gắn lớp và đúng bảng đã dùng (tầng 2 và 3 dùng tài liệu, bảng của lớp). subject_id là bài
-- (PROBLEM) hoặc lượt gia sư (TUTOR_FORMULA), nên không có khóa ngoại. Đổi bảng công thức thì lượt cũ stale = true.
-- Trạng thái phát hành luôn suy từ trạng thái tổng: lượt bài SAI → BI_CHAN, KHONG_KIEM_DUOC → CHO_GIAO_VIEN_DUYET,
-- DAT / GV_DUYET → DA_PHAT_HANH; lượt công thức gia sư không phát hành và không duyệt riêng (ADR 013). Trạng thái tổng
-- khớp các tầng do domain kiểm (VerificationRun), vì các tầng ở bảng khác.
CREATE TABLE verification_runs (
    id               uuid        PRIMARY KEY,
    class_id         uuid        NOT NULL REFERENCES classes (id) ON DELETE CASCADE,
    subject_kind     varchar(16) NOT NULL CHECK (subject_kind IN ('PROBLEM', 'TUTOR_FORMULA')),
    subject_id       uuid        NOT NULL,
    content_hash     varchar(64) NOT NULL CHECK (content_hash ~ '^[0-9a-f]{64}$'),
    -- Trống khi lớp chưa có bảng khóa: tầng 3 khi đó KHONG_KIEM_DUOC. Bảng phải của đúng lớp (khóa ngoại hai cột).
    formula_sheet_id uuid,
    overall_status   varchar(16) NOT NULL CHECK (overall_status IN ('DAT', 'SAI', 'KHONG_KIEM_DUOC', 'GV_DUYET')),
    publish_status   varchar(20) CHECK (publish_status IN ('DA_PHAT_HANH', 'BI_CHAN', 'CHO_GIAO_VIEN_DUYET')),
    stale            boolean     NOT NULL DEFAULT false,
    created_at       timestamptz NOT NULL,
    CHECK (CASE subject_kind
        WHEN 'PROBLEM' THEN publish_status IS NOT DISTINCT FROM (CASE overall_status
            WHEN 'SAI' THEN 'BI_CHAN' WHEN 'KHONG_KIEM_DUOC' THEN 'CHO_GIAO_VIEN_DUYET' ELSE 'DA_PHAT_HANH' END)
        ELSE publish_status IS NULL AND overall_status <> 'GV_DUYET' END),
    FOREIGN KEY (formula_sheet_id, class_id) REFERENCES formula_sheets (id, class_id),
    -- Cho khóa ngoại của problem_releases và content_reviews.
    UNIQUE (id, class_id, subject_id, publish_status),
    UNIQUE (id, content_hash)
);

CREATE INDEX verification_runs_subject_idx ON verification_runs (class_id, subject_kind, subject_id, created_at DESC);
CREATE INDEX verification_runs_sheet_idx ON verification_runs (formula_sheet_id);

-- Căn cứ từng tầng của một lượt kiểm (FR-004): kết quả máy, trích đoạn có vị trí, dòng công thức.
CREATE TABLE verification_tier_results (
    run_id      uuid        NOT NULL REFERENCES verification_runs (id) ON DELETE CASCADE,
    tier        smallint    NOT NULL CHECK (tier BETWEEN 1 AND 3),
    status      varchar(16) NOT NULL CHECK (status IN ('DAT', 'SAI', 'KHONG_KIEM_DUOC')),
    result_type varchar(32),
    wrong_steps jsonb,
    error_code  varchar(32),
    confidence  real        CHECK (confidence BETWEEN 0 AND 1),
    reason      text,
    citation    jsonb,
    raw         jsonb,
    PRIMARY KEY (run_id, tier)
);

-- Trạng thái phát hành của bài cho từng lớp; học sinh chỉ thấy bài DA_PHAT_HANH của lớp mình. NHAP không gắn lượt kiểm;
-- trạng thái khác luôn bằng trạng thái phát hành của một lượt kiểm bài của đúng lớp và bài (khóa ngoại bốn cột; lượt
-- công thức gia sư có publish_status trống nên không khớp). Giáo viên duyệt lượt đó thì trạng thái theo sang
-- DA_PHAT_HANH (ON UPDATE CASCADE).
CREATE TABLE problem_releases (
    class_id   uuid        NOT NULL REFERENCES classes (id) ON DELETE CASCADE,
    problem_id uuid        NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    status     varchar(20) NOT NULL CHECK (status IN ('NHAP', 'DA_PHAT_HANH', 'BI_CHAN', 'CHO_GIAO_VIEN_DUYET')),
    run_id     uuid,
    updated_at timestamptz NOT NULL,
    PRIMARY KEY (class_id, problem_id),
    CHECK ((status = 'NHAP') = (run_id IS NULL)),
    FOREIGN KEY (run_id, class_id, problem_id, status)
        REFERENCES verification_runs (id, class_id, subject_id, publish_status) ON UPDATE CASCADE
);

CREATE INDEX problem_releases_class_status_idx ON problem_releases (class_id, status);
CREATE INDEX problem_releases_problem_idx ON problem_releases (problem_id);

-- Giáo viên duyệt bài KHONG_KIEM_DUOC (FR-005): bắt buộc ghi chú; chỉ cho lượt kiểm bài (công thức trong lời gia sư
-- không duyệt riêng, ADR 013). Ai, lúc nào, vì sao, trên đúng nội dung của lượt (content_hash phải bằng hash của lượt).
-- Một lượt duyệt một lần. Bản ghi duyệt là nhật ký: lượt (và lớp) có bản ghi duyệt thì không xóa được; hạn giữ theo
-- ADR 012 (#60).
CREATE TABLE content_reviews (
    id           uuid          PRIMARY KEY,
    run_id       uuid          NOT NULL UNIQUE,
    content_hash varchar(64)   NOT NULL CHECK (content_hash ~ '^[0-9a-f]{64}$'),
    reviewer_id  uuid          NOT NULL REFERENCES users (id),
    decision     varchar(8)    NOT NULL CHECK (decision = 'GV_DUYET'),
    note         varchar(1000) NOT NULL CHECK (btrim(note) <> ''),
    at           timestamptz   NOT NULL,
    FOREIGN KEY (run_id, content_hash) REFERENCES verification_runs (id, content_hash) ON DELETE RESTRICT
);

-- ---- Bất biến chéo bảng ---------------------------------------------------------------------------------------------

-- Bảng KHOA không đổi được nữa: không thêm, sửa dòng hay trích dẫn của nó, không mở khóa, không đổi dấu vân tay. Chỉ
-- locked_by được về trống (người khóa bị xóa, ON DELETE SET NULL). Xóa vẫn được, để xóa lớp xóa dây chuyền.
CREATE FUNCTION formula_sheets_khoa_bat_bien() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status = 'KHOA' AND (NEW.status, NEW.class_id, NEW.version, NEW.note, NEW.fingerprint, NEW.locked_at)
            IS DISTINCT FROM (OLD.status, OLD.class_id, OLD.version, OLD.note, OLD.fingerprint, OLD.locked_at) THEN
        RAISE EXCEPTION 'Bảng công thức % đã khóa, không sửa được', OLD.id USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER formula_sheets_khoa_bat_bien BEFORE UPDATE ON formula_sheets
    FOR EACH ROW EXECUTE FUNCTION formula_sheets_khoa_bat_bien();

-- Dòng: bảng của nó chưa khóa; đoạn trích chính thuộc tài liệu cùng lớp với bảng.
CREATE FUNCTION formulas_kiem_bang() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    lop uuid;
BEGIN
    IF TG_OP = 'UPDATE' AND (SELECT status FROM formula_sheets WHERE id = OLD.formula_sheet_id) = 'KHOA' THEN
        RAISE EXCEPTION 'Dòng % thuộc bảng đã khóa', OLD.id USING ERRCODE = 'check_violation';
    END IF;
    SELECT class_id INTO lop FROM formula_sheets WHERE id = NEW.formula_sheet_id AND status = 'NHAP';
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Chỉ thêm hay sửa dòng của bảng nháp' USING ERRCODE = 'check_violation';
    END IF;
    IF NEW.citation_passage_id IS NOT NULL AND lop IS DISTINCT FROM (SELECT d.class_id FROM document_passages p
            JOIN documents d ON d.id = p.document_id WHERE p.id = NEW.citation_passage_id) THEN
        RAISE EXCEPTION 'Đoạn trích dẫn không thuộc tài liệu của lớp' USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER formulas_kiem_bang BEFORE INSERT OR UPDATE ON formulas
    FOR EACH ROW EXECUTE FUNCTION formulas_kiem_bang();

-- Trích dẫn thêm: dòng thuộc bảng nháp; đoạn thuộc tài liệu cùng lớp với bảng.
CREATE FUNCTION formula_citations_kiem_bang() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    lop uuid;
BEGIN
    SELECT s.class_id INTO lop FROM formulas f JOIN formula_sheets s ON s.id = f.formula_sheet_id
        WHERE f.id = NEW.formula_id AND s.status = 'NHAP';
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Chỉ thêm trích dẫn cho dòng của bảng nháp' USING ERRCODE = 'check_violation';
    END IF;
    IF lop IS DISTINCT FROM (SELECT d.class_id FROM document_passages p JOIN documents d ON d.id = p.document_id
            WHERE p.id = NEW.passage_id) THEN
        RAISE EXCEPTION 'Đoạn trích dẫn không thuộc tài liệu của lớp' USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER formula_citations_kiem_bang BEFORE INSERT OR UPDATE ON formula_citations
    FOR EACH ROW EXECUTE FUNCTION formula_citations_kiem_bang();
