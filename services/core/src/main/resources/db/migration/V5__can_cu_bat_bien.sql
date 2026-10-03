-- Căn cứ tầng 2 không đổi dưới chân kết quả kiểm (#120). Một đoạn tài liệu «đang là căn cứ» khi được trích dẫn bởi một
-- dòng bảng công thức (trích dẫn chính hay thêm, bảng nháp hay khóa) hoặc bởi một lượt kiểm 3 tầng (bảng mới
-- verification_run_citations, quan hệ thay cho chỗ chỉ nằm trong verification_tier_results.citation). Đoạn đang là căn cứ
-- không đổi được vị trí, chữ, hay tài liệu chứa nó; tài liệu có đoạn đang là căn cứ không đổi lớp, quyền dùng, loại, văn
-- bản, phiên bản (hạ xuống chua_ro thì không còn là căn cứ hợp lệ, R9; đổi văn bản thì đoạn không còn nằm trong nguồn). Muốn sửa thì nạp tài liệu thành phiên
-- bản mới (đoạn mới) rồi kiểm lại. Xóa đoạn đang là căn cứ bị khóa ngoại chặn.
-- Đồng thời: ghi trích dẫn khóa đoạn và tài liệu FOR SHARE, xung đột với khóa của UPDATE trên các dòng đó, nên ghi trích
-- dẫn và sửa căn cứ chạy lần lượt; kiểm «đang là căn cứ» của bên sửa chạy sau khi có khóa dòng nên thấy trích dẫn vừa
-- commit.

-- Trích dẫn của lượt kiểm (tầng 2 của bài, công thức trong lời gia sư). Khóa ngoại tới đoạn kiểm lúc commit, như
-- formula_citations, để xóa lớp xóa dây chuyền được.
CREATE TABLE verification_run_citations (
    run_id     uuid NOT NULL REFERENCES verification_runs (id) ON DELETE CASCADE,
    passage_id uuid NOT NULL REFERENCES document_passages (id) DEFERRABLE INITIALLY DEFERRED,
    PRIMARY KEY (run_id, passage_id)
);

CREATE INDEX verification_run_citations_passage_idx ON verification_run_citations (passage_id);

CREATE FUNCTION doan_dang_la_can_cu(doan uuid) RETURNS boolean LANGUAGE sql STABLE AS $$
    SELECT EXISTS (SELECT 1 FROM formulas WHERE citation_passage_id = doan)
        OR EXISTS (SELECT 1 FROM formula_citations WHERE passage_id = doan)
        OR EXISTS (SELECT 1 FROM verification_run_citations WHERE passage_id = doan)
$$;

CREATE FUNCTION document_passages_can_cu_bat_bien() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.document_id, NEW.page, NEW.char_start, NEW.char_end, NEW.text)
            IS DISTINCT FROM (OLD.document_id, OLD.page, OLD.char_start, OLD.char_end, OLD.text)
       AND doan_dang_la_can_cu(OLD.id) THEN
        RAISE EXCEPTION 'Đoạn % đang là căn cứ của kết quả kiểm, không sửa được; nạp tài liệu thành phiên bản mới', OLD.id
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER document_passages_can_cu_bat_bien BEFORE UPDATE ON document_passages
    FOR EACH ROW EXECUTE FUNCTION document_passages_can_cu_bat_bien();

CREATE FUNCTION documents_can_cu_giu_lop_va_quyen() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.class_id, NEW.license_status, NEW.kind, NEW.text_content, NEW.version)
            IS DISTINCT FROM (OLD.class_id, OLD.license_status, OLD.kind, OLD.text_content, OLD.version)
       AND EXISTS (SELECT 1 FROM document_passages p WHERE p.document_id = OLD.id AND doan_dang_la_can_cu(p.id)) THEN
        RAISE EXCEPTION 'Tài liệu % đang là căn cứ của kết quả kiểm, không đổi lớp, quyền dùng, loại, văn bản hay phiên bản được; nạp phiên bản mới', OLD.id
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER documents_can_cu_giu_lop_va_quyen BEFORE UPDATE ON documents
    FOR EACH ROW EXECUTE FUNCTION documents_can_cu_giu_lop_va_quyen();

-- Ghi trích dẫn (dòng bảng công thức, trích dẫn thêm, trích dẫn của lượt kiểm): khóa tài liệu rồi đoạn FOR SHARE cho tới
-- hết giao dịch (xem đầu tệp), và từ chối đoạn của tài liệu quyền dùng chua_ro.
CREATE FUNCTION khoa_can_cu(doan uuid) RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    IF doan IS NOT NULL THEN
        -- Tài liệu trước, đoạn sau: cùng thứ tự với lần ghi tài liệu (dòng documents rồi các đoạn), không deadlock.
        PERFORM 1 FROM documents d WHERE d.id = (SELECT p.document_id FROM document_passages p WHERE p.id = doan) FOR SHARE;
        PERFORM 1 FROM document_passages p WHERE p.id = doan FOR SHARE;
        -- Quyền dùng chưa rõ thì không bao giờ là căn cứ, không được trích dẫn (R9); kiểm khi đang giữ khóa tài liệu.
        IF (SELECT d.license_status FROM document_passages p JOIN documents d ON d.id = p.document_id WHERE p.id = doan) = 'chua_ro' THEN
            RAISE EXCEPTION 'Đoạn % thuộc tài liệu quyền dùng chưa rõ, không làm căn cứ được', doan USING ERRCODE = 'check_violation';
        END IF;
    END IF;
END $$;

CREATE FUNCTION formulas_khoa_can_cu() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    PERFORM khoa_can_cu(NEW.citation_passage_id);
    RETURN NEW;
END $$;

CREATE TRIGGER formulas_khoa_can_cu BEFORE INSERT OR UPDATE OF citation_passage_id ON formulas
    FOR EACH ROW EXECUTE FUNCTION formulas_khoa_can_cu();

CREATE FUNCTION trich_dan_khoa_can_cu() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    PERFORM khoa_can_cu(NEW.passage_id);
    RETURN NEW;
END $$;

CREATE TRIGGER formula_citations_khoa_can_cu BEFORE INSERT OR UPDATE ON formula_citations
    FOR EACH ROW EXECUTE FUNCTION trich_dan_khoa_can_cu();

-- Trích dẫn của lượt kiểm chỉ thêm, như lượt kiểm: không sửa; chỉ xóa theo dây chuyền khi lượt kiểm cha đã bị xóa (lúc
-- ON DELETE CASCADE chạy, dòng cha đã mất). Đoạn phải thuộc tài liệu cùng lớp với lượt kiểm (tầng 2 dùng tài liệu của lớp).
CREATE FUNCTION verification_run_citations_kiem() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION 'Trích dẫn của lượt kiểm không sửa được' USING ERRCODE = 'check_violation';
    END IF;
    IF TG_OP = 'DELETE' THEN
        IF EXISTS (SELECT 1 FROM verification_runs WHERE id = OLD.run_id) THEN
            RAISE EXCEPTION 'Trích dẫn của lượt kiểm % không xóa riêng được', OLD.run_id USING ERRCODE = 'check_violation';
        END IF;
        RETURN OLD;
    END IF;
    PERFORM khoa_can_cu(NEW.passage_id);
    IF (SELECT class_id FROM verification_runs WHERE id = NEW.run_id) IS DISTINCT FROM (SELECT d.class_id
            FROM document_passages p JOIN documents d ON d.id = p.document_id WHERE p.id = NEW.passage_id) THEN
        RAISE EXCEPTION 'Đoạn trích dẫn không thuộc tài liệu của lớp của lượt kiểm' USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER verification_run_citations_kiem BEFORE INSERT OR UPDATE OR DELETE ON verification_run_citations
    FOR EACH ROW EXECUTE FUNCTION verification_run_citations_kiem();

-- Bài đổi nội dung thì kết quả kiểm cũ không còn nói về nội dung đang có: mọi lượt kiểm của bài thành cũ và bản phát hành
-- của bài ở mọi lớp về NHAP (học sinh không thấy) cho tới khi kiểm lại. Đóng mặc định, cùng giao dịch với lệnh sửa. Nội dung
-- gồm đề (content_hash của problems), lời giải và dữ kiện bảo vệ (solutions), thang gợi ý (hint_levels): đổi bảng nào cũng
-- vô hiệu. Adapter chỉ ghi khi giá trị thật sự khác, nên importer chạy lại y như cũ không vô hiệu gì.
CREATE FUNCTION vo_hieu_ket_qua_bai(bai uuid) RETURNS void LANGUAGE sql AS $$
    UPDATE verification_runs SET stale = true WHERE subject_kind = 'PROBLEM' AND subject_id = bai AND NOT stale;
    UPDATE problem_releases SET status = 'NHAP', run_id = NULL, updated_at = now() WHERE problem_id = bai AND status <> 'NHAP';
$$;

CREATE FUNCTION problems_doi_noi_dung() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.content_hash IS DISTINCT FROM OLD.content_hash THEN
        PERFORM vo_hieu_ket_qua_bai(NEW.id);
    END IF;
    RETURN NULL;
END $$;

CREATE TRIGGER problems_doi_noi_dung AFTER UPDATE OF content_hash ON problems
    FOR EACH ROW EXECUTE FUNCTION problems_doi_noi_dung();

CREATE FUNCTION loi_giai_goi_y_doi() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    PERFORM vo_hieu_ket_qua_bai(CASE WHEN TG_OP = 'DELETE' THEN OLD.problem_id ELSE NEW.problem_id END);
    IF TG_OP = 'UPDATE' AND NEW.problem_id IS DISTINCT FROM OLD.problem_id THEN
        PERFORM vo_hieu_ket_qua_bai(OLD.problem_id);
    END IF;
    RETURN NULL;
END $$;

CREATE TRIGGER solutions_doi AFTER INSERT OR UPDATE OR DELETE ON solutions
    FOR EACH ROW EXECUTE FUNCTION loi_giai_goi_y_doi();

CREATE TRIGGER hint_levels_doi AFTER INSERT OR UPDATE OR DELETE ON hint_levels
    FOR EACH ROW EXECUTE FUNCTION loi_giai_goi_y_doi();
