package vn.hoctapcanman.core.content.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Tài liệu của lớp, căn cứ tầng 2 (R9). {@code licenseStatus} là mã quyền dùng như v0 và {@code services/math}
 * ({@code app/verify.py}, {@code QUYEN_HOP_LE}); dịch vụ toán quyết mã nào làm căn cứ, còn {@code chua_ro} thì không bao
 * giờ ({@link #usableAsBasis}). {@code code} là mã ổn định của tài liệu nhập ({@code v0-don-dieu}, {@code sp-tai-lieu-0001}…)
 * để importer nhận lại khi nạp lần hai; tài liệu giáo viên tải lên để trống.
 */
public record Document(
        UUID id,
        UUID classId,
        @Nullable String code,
        String title,
        DocumentKind kind,
        @Nullable String source,
        String licenseStatus,
        @Nullable String fileRef,
        String textContent,
        int version,
        @Nullable UUID uploadedBy,
        Instant createdAt) {

    /** Quyền dùng chưa rõ: gia sư bỏ qua, không làm căn cứ, không được trích dẫn (v0). */
    public static final String CHUA_RO = "chua_ro";

    private static final Pattern MA_QUYEN = Pattern.compile("[a-z][a-z0-9_]{0,31}");

    public Document {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(classId, "classId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(licenseStatus, "licenseStatus");
        Objects.requireNonNull(textContent, "textContent");
        Objects.requireNonNull(createdAt, "createdAt");
        Kiem.maNeuCo(code, 64, "tài liệu");
        Kiem.toiDa(Kiem.khongTrong(title, "Tên tài liệu"), 300, "Tên tài liệu");
        if (source != null) {
            Kiem.toiDa(source, 300, "Nguồn tài liệu");
        }
        if (!MA_QUYEN.matcher(licenseStatus).matches()) {
            throw new IllegalArgumentException("Mã quyền dùng không hợp lệ");
        }
        if (fileRef != null) {
            Kiem.toiDa(fileRef, 500, "Tệp của tài liệu");
        }
        Kiem.hopLeUtf16(Kiem.khongTrong(textContent, "Văn bản của tài liệu"), "Văn bản của tài liệu");
        if (version < 1) {
            throw new IllegalArgumentException("Phiên bản tài liệu phải từ 1");
        }
    }

    /** Được làm căn cứ tầng 2 và được trích dẫn: quyền dùng không phải «chưa rõ». */
    public boolean usableAsBasis() {
        return !CHUA_RO.equals(licenseStatus);
    }

    /** Không in văn bản tài liệu vào log. */
    @Override
    public String toString() {
        return "Document[id=" + id + ", code=" + code + ", version=" + version + "]";
    }
}
