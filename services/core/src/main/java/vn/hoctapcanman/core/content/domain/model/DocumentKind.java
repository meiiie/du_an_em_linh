package vn.hoctapcanman.core.content.domain.model;

/** Loại tài liệu của lớp, mã như v0 ({@code apps/web/app/gv/tai-lieu/page.tsx}). */
public enum DocumentKind {
    TU_SOAN("tu_soan"),
    DE_MAU("de_mau"),
    THAM_KHAO("tham_khao");

    private final String code;

    DocumentKind(String code) {
        this.code = code;
    }

    /** Mã lưu trong CSDL và trao đổi với dịch vụ toán. */
    public String code() {
        return code;
    }

    public static DocumentKind parse(String code) {
        for (DocumentKind kind : values()) {
            if (kind.code.equals(code)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Loại tài liệu không hợp lệ: " + code);
    }
}
