-- Lượt kiểm mới nhất và căn cứ của tầng 2 (#121). Ba bất biến, giữ ở CSDL vì lượt kiểm, phát hành và duyệt có thể được
-- ghi từ nhiều giao dịch cùng lúc (importer, giáo viên, kiểm lại sau khi đổi bảng):
--   1. Lượt kiểm bài có tầng 2 DAT phải trích dẫn ít nhất một đoạn tài liệu (verification_run_citations), kiểm lúc commit.
--   2. Phát hành chỉ gắn được vào lượt kiểm mới nhất của (lớp, bài), xếp theo (created_at, id).
--   3. Giáo viên chỉ duyệt được lượt mới nhất, kiểm với bảng công thức đang dùng của lớp.
-- Tuần tự hóa theo bài: ghi lượt kiểm bài, gắn phát hành và duyệt đều khóa dòng bài FOR NO KEY UPDATE (xung đột với nhau
-- và với lần sửa nội dung bài, vốn khóa dòng bài để tăng content_version), nên phép kiểm «mới nhất» chạy sau khi có khóa và
-- thấy mọi lượt đã commit. Thay khóa FOR SHARE của V5 ở hai hàm dưới (FOR SHARE không xung đột với nhau).

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
