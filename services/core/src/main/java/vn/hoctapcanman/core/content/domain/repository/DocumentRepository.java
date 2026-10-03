package vn.hoctapcanman.core.content.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import vn.hoctapcanman.core.content.domain.model.Document;
import vn.hoctapcanman.core.content.domain.model.DocumentPassage;

/** Tài liệu của lớp và các đoạn có vị trí (R9). */
public interface DocumentRepository {

    /**
     * Ghi mới hoặc ghi đè tài liệu theo id, rồi đặt các đoạn của nó; mọi đoạn phải thuộc tài liệu này. Đoạn nhận ra theo
     * vị trí ({@code charStart}): đoạn cùng vị trí giữ id cũ, nên nạp lại không làm gãy trích dẫn của bảng công thức.
     * Đoạn cũ không còn trong danh sách bị xóa; đoạn đang là căn cứ của một bảng công thức thì không xóa được (khóa ngoại,
     * lỗi ràng buộc). Trả các đoạn đã lưu, theo vị trí, với id thật.
     */
    List<DocumentPassage> save(Document document, List<DocumentPassage> passages);

    Optional<Document> findById(UUID id);

    /** Tài liệu nhập có mã ổn định {@code code} của lớp. */
    Optional<Document> findByClassAndCode(UUID classId, String code);

    /** Tài liệu của lớp, cũ trước. */
    List<Document> findByClass(UUID classId);

    /** Đoạn của tài liệu, theo vị trí. */
    List<DocumentPassage> findPassages(UUID documentId);
}
