package vn.hoctapcanman.core.content.domain.repository;

import java.util.Optional;
import java.util.UUID;
import vn.hoctapcanman.core.content.domain.model.FormulaSheet;

/** Bảng công thức của lớp, có phiên bản (ADR 013). */
public interface FormulaSheetRepository {

    /**
     * Ghi bảng cùng các dòng và trích dẫn thêm của dòng. Bảng nháp: ghi đè theo id, thay toàn bộ dòng. Bảng khóa: ghi như
     * bảng nháp rồi đổi sang {@code KHOA} trong cùng giao dịch (thứ tự mà trigger của V4 đòi); bảng đã khóa ở CSDL thì
     * không đổi được nữa: ghi lại đúng bảng đó là không làm gì, ghi khác đi là {@link IllegalStateException}.
     */
    void save(FormulaSheet sheet);

    Optional<FormulaSheet> findById(UUID id);

    /** Bảng đang dùng của lớp: bảng {@code KHOA} có phiên bản lớn nhất. */
    Optional<FormulaSheet> findCurrent(UUID classId);

    /** Bảng nháp của lớp (nhiều nhất một). */
    Optional<FormulaSheet> findDraft(UUID classId);
}
