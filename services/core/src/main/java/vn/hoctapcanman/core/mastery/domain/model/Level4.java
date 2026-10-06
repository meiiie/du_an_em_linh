package vn.hoctapcanman.core.mastery.domain.model;

/** Thang 4 mức học sinh thấy ({@code MUC4} của v0), theo thứ tự từ thấp lên cao. */
public enum Level4 {
    NHAN_BIET,
    THONG_HIEU,
    VAN_DUNG,
    VAN_DUNG_CAO;

    private static final Level4[] THU_TU = values();

    Level4 up() {
        return THU_TU[Math.min(ordinal() + 1, THU_TU.length - 1)];
    }

    Level4 down() {
        return THU_TU[Math.max(ordinal() - 1, 0)];
    }
}
