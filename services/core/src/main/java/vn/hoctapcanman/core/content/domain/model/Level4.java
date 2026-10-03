package vn.hoctapcanman.core.content.domain.model;

/**
 * 4 mức của học sinh, lưu trên mỗi bài (ADR 004). 3 mức CV 7991 ({@link Level3}) chỉ để hiển thị.
 */
public enum Level4 {
    NHAN_BIET("NB"),
    THONG_HIEU("TH"),
    VAN_DUNG("VD"),
    VAN_DUNG_CAO("VDC");

    private final String shortCode;

    Level4(String shortCode) {
        this.shortCode = shortCode;
    }

    /** Mã ngắn của lab Sư phạm ({@code NB}, {@code TH}, {@code VD}, {@code VDC}), như trong data/supham. */
    public String shortCode() {
        return shortCode;
    }

    /** Đọc mã đầy đủ hoặc mã ngắn, như bảng {@code MUC4} của v0 ({@code apps/web/scripts/seed.ts}). */
    public static Level4 parse(String code) {
        for (Level4 level : values()) {
            if (level.name().equals(code) || level.shortCode.equals(code)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Mức không hợp lệ: " + code);
    }

    /** 3 mức CV 7991 tương ứng, như bảng {@code MUC3} của v0: Vận dụng cao vẫn là Vận dụng. */
    public Level3 toLevel3() {
        return switch (this) {
            case NHAN_BIET -> Level3.BIET;
            case THONG_HIEU -> Level3.HIEU;
            case VAN_DUNG, VAN_DUNG_CAO -> Level3.VAN_DUNG;
        };
    }
}
