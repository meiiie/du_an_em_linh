package vn.hoctapcanman.core.content.domain.model;

/**
 * Trạng thái phát hành của bài cho một lớp, giữ mã của v0 ({@code cong_phat_hanh} trong
 * {@code services/math/app/verify.py}). Học sinh chỉ thấy bài {@link #DA_PHAT_HANH} của lớp mình.
 */
public enum ReleaseStatus {
    /** Chưa kiểm, hoặc nội dung vừa đổi và chờ kiểm lại. */
    NHAP,
    /** Mọi tầng {@code DAT}, hoặc giáo viên đã duyệt ({@code GV_DUYET}). */
    DA_PHAT_HANH,
    /** Một tầng {@code SAI}: không bao giờ tới học sinh, không duyệt được. */
    BI_CHAN,
    /** Còn tầng {@code KHONG_KIEM_DUOC}: chờ giáo viên duyệt. */
    CHO_GIAO_VIEN_DUYET
}
