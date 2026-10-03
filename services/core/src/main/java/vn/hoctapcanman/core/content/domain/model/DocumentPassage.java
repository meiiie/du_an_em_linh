package vn.hoctapcanman.core.content.domain.model;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Một đoạn của tài liệu có vị trí (trang, ký tự đầu, ký tự cuối), cho tầng 2 và trích dẫn {@code [n]} (R9).
 * {@code textFolded} là chữ đã gập để tìm cụm từ như v0 ({@link #fold}), không vector.
 */
public record DocumentPassage(UUID id, UUID documentId, @Nullable Integer page, int charStart, int charEnd, String text, String textFolded) {

    public DocumentPassage {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(documentId, "documentId");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(textFolded, "textFolded");
        if (page != null && page < 1) {
            throw new IllegalArgumentException("Trang phải từ 1");
        }
        if (charStart < 0 || charEnd <= charStart) {
            throw new IllegalArgumentException("Vị trí đoạn không hợp lệ");
        }
        Kiem.khongTrong(text, "Chữ của đoạn");
    }

    /**
     * Đoạn này đúng là một đoạn của {@code document}: cùng tài liệu, vị trí nằm trong văn bản, chữ bằng đúng phần
     * {@code [charStart, charEnd)} của văn bản, chữ gập tính lại từ chữ. Đoạn là căn cứ tầng 2, nên vị trí hay trích dẫn
     * bịa (do trích xuất hay nhập hỏng) không bao giờ được ghi.
     */
    public void requireWithin(Document document) {
        if (!documentId.equals(document.id())) {
            throw new IllegalArgumentException("Đoạn không thuộc tài liệu " + document.id());
        }
        String vanBan = document.textContent();
        if (charEnd > vanBan.length() || !vanBan.substring(charStart, charEnd).equals(text)) {
            throw new IllegalArgumentException("Đoạn ở [" + charStart + ", " + charEnd + ") không khớp văn bản của tài liệu");
        }
        if (!fold(text).equals(textFolded)) {
            throw new IllegalArgumentException("Chữ gập của đoạn không khớp chữ của đoạn");
        }
    }

    /** Đoạn bắt đầu ở {@code charStart} của tài liệu, dài bằng {@code text}; chữ gập tính từ {@code text}. */
    public static DocumentPassage of(UUID documentId, @Nullable Integer page, int charStart, String text) {
        return new DocumentPassage(UUID.randomUUID(), documentId, page, charStart, charStart + text.length(), text, fold(text));
    }

    /**
     * Chữ thường, bỏ dấu, «đ» thành «d», như {@code khongDau} của v0 ({@code apps/web/lib/kien-thuc.ts}): chuẩn hóa NFD rồi
     * bỏ các dấu kết hợp U+0300–U+036F. Tìm cụm từ trên chữ đã gập thì bắt được cả câu viết không dấu.
     */
    public static String fold(String text) {
        String nfd = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return nfd.replaceAll("[\\u0300-\\u036f]", "").replace('đ', 'd');
    }
}
