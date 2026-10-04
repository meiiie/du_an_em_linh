-- Lượt kiểm mới nhất và căn cứ của tầng 2 (#121). Ba bất biến, giữ ở CSDL vì lượt kiểm, phát hành và duyệt có thể được
-- ghi từ nhiều giao dịch cùng lúc (importer, giáo viên, kiểm lại sau khi đổi bảng):
--   1. Lượt kiểm bài có tầng 2 DAT phải trích dẫn ít nhất một đoạn tài liệu (verification_run_citations), kiểm lúc commit.
--   2. Phát hành chỉ gắn được vào lượt kiểm mới nhất của (lớp, bài), xếp theo (created_at, id).
--   3. Giáo viên chỉ duyệt được lượt mới nhất, kiểm với bảng công thức đang dùng của lớp.
--   4. Lớp khóa bảng công thức mới: mọi lượt kiểm của lớp với bảng khác thành cũ, cùng giao dịch với lệnh khóa; lượt kiểm
--      mới chỉ ghi được với bảng đang dùng của lớp.
--   5. Lượt kiểm thành cũ (vì bất kỳ lý do gì) thì phát hành gắn với nó về NHAP; ghi lượt kiểm bài mới thì phát hành đang
--      theo lượt cũ của (lớp, bài) về NHAP, chờ áp lượt mới.
--   6. Chỉ khóa được bảng mới hơn mọi bảng đã khóa của lớp.
-- Tuần tự hóa theo bài: ghi lượt kiểm bài, gắn phát hành và duyệt đều khóa dòng bài FOR NO KEY UPDATE (xung đột với nhau
-- và với lần sửa nội dung bài, vốn khóa dòng bài để tăng content_version), nên phép kiểm «mới nhất» chạy sau khi có khóa và
-- thấy mọi lượt đã commit. Thay khóa FOR SHARE của V5 ở hai hàm dưới (FOR SHARE không xung đột với nhau).
-- Tuần tự hóa theo lớp: ba việc trên khóa dòng lớp FOR SHARE (không chặn nhau), lần khóa bảng mới khóa dòng lớp FOR NO KEY
-- UPDATE (chặn cả ba), nên không lượt nào chen được giữa lúc đổi bảng và lúc quét lượt cũ. Thứ tự khóa luôn lớp rồi bài.

CREATE FUNCTION luot_moi_nhat(lop uuid, bai uuid) RETURNS uuid LANGUAGE sql STABLE AS $$
    SELECT id FROM verification_runs WHERE class_id = lop AND subject_kind = 'PROBLEM' AND subject_id = bai
    ORDER BY created_at DESC, id DESC LIMIT 1
$$;

CREATE FUNCTION bang_dang_dung(lop uuid) RETURNS uuid LANGUAGE sql STABLE AS $$
    SELECT id FROM formula_sheets WHERE class_id = lop AND status = 'KHOA' ORDER BY version DESC LIMIT 1
$$;

-- 1. Tầng 2 DAT có trích dẫn. Tầng và trích dẫn ghi sau lượt trong cùng giao dịch, nên kiểm lúc commit.
CREATE FUNCTION luot_bai_tang2_co_trich_dan() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.subject_kind = 'PROBLEM'
       AND EXISTS (SELECT 1 FROM verification_tier_results WHERE run_id = NEW.id AND tier = 2 AND status = 'DAT')
       AND NOT EXISTS (SELECT 1 FROM verification_run_citations WHERE run_id = NEW.id) THEN
        RAISE EXCEPTION 'Lượt kiểm % có tầng 2 DAT mà không trích dẫn đoạn tài liệu nào', NEW.id USING ERRCODE = 'check_violation';
    END IF;
    RETURN NULL;
END $$;

CREATE CONSTRAINT TRIGGER luot_bai_tang2_co_trich_dan AFTER INSERT ON verification_runs
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION luot_bai_tang2_co_trich_dan();

-- Ghi lượt kiểm bài: như V5 (đúng phiên bản nội dung hiện tại), nhưng khóa dòng bài FOR NO KEY UPDATE.
CREATE OR REPLACE FUNCTION verification_runs_dung_phien_ban() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    hien_tai integer;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        IF NEW.content_version IS DISTINCT FROM OLD.content_version THEN
            RAISE EXCEPTION 'Phiên bản nội dung của lượt kiểm % không đổi được', OLD.id USING ERRCODE = 'check_violation';
        END IF;
        RETURN NEW;
    END IF;
    PERFORM 1 FROM classes WHERE id = NEW.class_id FOR SHARE;
    IF NEW.formula_sheet_id IS DISTINCT FROM bang_dang_dung(NEW.class_id) THEN
        RAISE EXCEPTION 'Lượt kiểm phải kiểm với bảng công thức đang dùng của lớp' USING ERRCODE = 'check_violation';
    END IF;
    IF NEW.subject_kind = 'PROBLEM' THEN
        SELECT content_version INTO hien_tai FROM problems WHERE id = NEW.subject_id FOR NO KEY UPDATE;
        IF hien_tai IS DISTINCT FROM NEW.content_version THEN
            RAISE EXCEPTION 'Lượt kiểm cho phiên bản nội dung % của bài, hiện tại là %', NEW.content_version, hien_tai
                USING ERRCODE = 'check_violation';
        END IF;
    END IF;
    RETURN NEW;
END $$;

-- 2. Gắn phát hành: lượt còn mới, đúng phiên bản nội dung, và là lượt mới nhất của (lớp, bài).
CREATE OR REPLACE FUNCTION problem_releases_luot_con_moi() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE
    hien_tai integer;
    luot     record;
BEGIN
    IF NEW.run_id IS NULL THEN
        RETURN NEW;
    END IF;
    PERFORM 1 FROM classes WHERE id = NEW.class_id FOR SHARE;
    SELECT content_version INTO hien_tai FROM problems WHERE id = NEW.problem_id FOR NO KEY UPDATE;
    SELECT stale, content_version INTO luot FROM verification_runs WHERE id = NEW.run_id;
    IF luot.stale OR luot.content_version IS DISTINCT FROM hien_tai THEN
        RAISE EXCEPTION 'Phát hành theo lượt kiểm cũ hay của phiên bản nội dung cũ' USING ERRCODE = 'check_violation';
    END IF;
    IF NEW.run_id IS DISTINCT FROM luot_moi_nhat(NEW.class_id, NEW.problem_id) THEN
        RAISE EXCEPTION 'Phát hành phải theo lượt kiểm mới nhất của bài ở lớp' USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;

-- 3. Duyệt (KHONG_KIEM_DUOC → GV_DUYET): lượt mới nhất của (lớp, bài) và kiểm với bảng đang dùng của lớp.
CREATE FUNCTION verification_runs_duyet_luot_moi_nhat() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.overall_status = 'GV_DUYET' AND OLD.overall_status IS DISTINCT FROM 'GV_DUYET' THEN
        PERFORM 1 FROM classes WHERE id = NEW.class_id FOR SHARE;
        PERFORM 1 FROM problems WHERE id = NEW.subject_id FOR NO KEY UPDATE;
        IF NEW.id IS DISTINCT FROM luot_moi_nhat(NEW.class_id, NEW.subject_id) THEN
            RAISE EXCEPTION 'Chỉ duyệt được lượt kiểm mới nhất của bài ở lớp' USING ERRCODE = 'check_violation';
        END IF;
        IF NEW.formula_sheet_id IS DISTINCT FROM bang_dang_dung(NEW.class_id) THEN
            RAISE EXCEPTION 'Lượt kiểm % không kiểm với bảng công thức đang dùng của lớp', NEW.id USING ERRCODE = 'check_violation';
        END IF;
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER verification_runs_duyet_luot_moi_nhat BEFORE UPDATE OF overall_status ON verification_runs
    FOR EACH ROW EXECUTE FUNCTION verification_runs_duyet_luot_moi_nhat();

-- 4. Khóa bảng mới (NHAP → KHOA): lượt kiểm của lớp với bảng khác thành cũ, phát hành dựa trên lượt cũ về NHAP.
CREATE FUNCTION formula_sheets_kich_hoat() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status = 'NHAP' AND NEW.status = 'KHOA' THEN
        PERFORM 1 FROM classes WHERE id = NEW.class_id FOR NO KEY UPDATE;
        -- Chỉ bảng mới hơn mọi bảng đã khóa của lớp mới thành bảng đang dùng; khóa bản cũ hơn thì từ chối, không rút oan
        -- phát hành đang theo bảng mới nhất. (class_id, version) là duy nhất từ V4.
        IF EXISTS (SELECT 1 FROM formula_sheets WHERE class_id = NEW.class_id AND status = 'KHOA' AND id <> NEW.id
                   AND version >= NEW.version) THEN
            RAISE EXCEPTION 'Bảng phiên bản % không mới hơn bảng đang dùng của lớp', NEW.version USING ERRCODE = 'check_violation';
        END IF;
        -- Phát hành dựa trên các lượt này về NHAP qua trigger verification_runs_cu_rut_phat_hanh bên dưới.
        UPDATE verification_runs SET stale = true
            WHERE class_id = NEW.class_id AND NOT stale AND formula_sheet_id IS DISTINCT FROM NEW.id;
    END IF;
    RETURN NULL;
END $$;

CREATE TRIGGER formula_sheets_kich_hoat AFTER UPDATE OF status ON formula_sheets
    FOR EACH ROW EXECUTE FUNCTION formula_sheets_kich_hoat();

-- Lượt kiểm thành cũ (đổi bảng, sửa nội dung bài, hay đánh dấu tay) thì không còn là căn cứ phát hành: phát hành gắn với nó
-- về NHAP ngay, đóng mặc định, chờ kiểm lại.
CREATE FUNCTION verification_runs_cu_rut_phat_hanh() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.stale AND NOT OLD.stale THEN
        UPDATE problem_releases SET status = 'NHAP', run_id = NULL, updated_at = now() WHERE run_id = NEW.id;
    END IF;
    RETURN NULL;
END $$;

CREATE TRIGGER verification_runs_cu_rut_phat_hanh AFTER UPDATE OF stale ON verification_runs
    FOR EACH ROW EXECUTE FUNCTION verification_runs_cu_rut_phat_hanh();

-- Ghi lượt kiểm bài mới: phát hành đang theo lượt cũ của (lớp, bài) về NHAP trong cùng giao dịch; nơi gọi áp lượt mới ngay
-- sau (ProblemRelease.apply). Nơi gọi hỏng trước khi áp thì bài ở NHAP: đóng mặc định, không để lượt cũ còn phát hành.
CREATE FUNCTION verification_runs_rut_phat_hanh_cu() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.subject_kind = 'PROBLEM' THEN
        UPDATE problem_releases SET status = 'NHAP', run_id = NULL, updated_at = now()
            WHERE class_id = NEW.class_id AND problem_id = NEW.subject_id AND run_id IS NOT NULL AND run_id <> NEW.id;
    END IF;
    RETURN NULL;
END $$;

CREATE TRIGGER verification_runs_rut_phat_hanh_cu AFTER INSERT ON verification_runs
    FOR EACH ROW EXECUTE FUNCTION verification_runs_rut_phat_hanh_cu();
