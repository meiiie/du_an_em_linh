-- Căn cứ tầng 2 không đổi dưới chân bảng công thức (#120). Một đoạn tài liệu đang được trích dẫn (trích dẫn chính hay
-- trích dẫn thêm của một dòng, ở bảng nháp hay bảng khóa) thì không đổi được vị trí, chữ, hay tài liệu chứa nó; tài liệu
-- có đoạn đang được trích dẫn thì không chuyển sang lớp khác được. Kết quả kiểm của dòng gắn với đúng chữ đã đọc lúc
-- kiểm: muốn sửa thì nạp tài liệu thành phiên bản mới (đoạn mới) và kiểm lại bảng. Xóa đoạn đang được trích dẫn đã bị
-- khóa ngoại của V4 chặn.

CREATE FUNCTION document_passages_can_cu_bat_bien() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.document_id, NEW.page, NEW.char_start, NEW.char_end, NEW.text)
            IS DISTINCT FROM (OLD.document_id, OLD.page, OLD.char_start, OLD.char_end, OLD.text)
       AND (EXISTS (SELECT 1 FROM formulas WHERE citation_passage_id = OLD.id)
            OR EXISTS (SELECT 1 FROM formula_citations WHERE passage_id = OLD.id)) THEN
        RAISE EXCEPTION 'Đoạn % đang là căn cứ của bảng công thức, không sửa được; nạp tài liệu thành phiên bản mới', OLD.id
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER document_passages_can_cu_bat_bien BEFORE UPDATE ON document_passages
    FOR EACH ROW EXECUTE FUNCTION document_passages_can_cu_bat_bien();

CREATE FUNCTION documents_can_cu_giu_lop() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.class_id IS DISTINCT FROM OLD.class_id AND EXISTS (
            SELECT 1 FROM document_passages p
            WHERE p.document_id = OLD.id
              AND (EXISTS (SELECT 1 FROM formulas f WHERE f.citation_passage_id = p.id)
                   OR EXISTS (SELECT 1 FROM formula_citations c WHERE c.passage_id = p.id))) THEN
        RAISE EXCEPTION 'Tài liệu % đang là căn cứ của bảng công thức, không chuyển lớp được', OLD.id
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER documents_can_cu_giu_lop BEFORE UPDATE ON documents
    FOR EACH ROW EXECUTE FUNCTION documents_can_cu_giu_lop();
