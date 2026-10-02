package vn.hoctapcanman.core.content.domain.model;

/**
 * Vì sao giáo viên không duyệt được một lượt kiểm (FR-005, data-model §content), hay vì sao một lượt không được áp vào
 * trạng thái phát hành ({@link ProblemRelease#apply}: chỉ các lý do «độ mới»). Nơi gọi ánh xạ sang HTTP:
 * {@link #NOT_A_PROBLEM}, {@link #BLOCKED}, {@link #NOTHING_TO_APPROVE}, {@link #ALREADY_APPROVED} là yêu cầu không hợp
 * lệ; {@link #INCOMPLETE} và bốn lý do độ mới nghĩa là phải kiểm lại trước (409), để không phát hành bằng phán quyết
 * thiếu căn cứ hay của nội dung, bảng đã cũ.
 */
public enum ApprovalRefusal {
    /** Lượt kiểm công thức trong lời gia sư: không duyệt riêng; giáo viên thêm công thức vào bảng rồi khóa (ADR 013). */
    NOT_A_PROBLEM,
    /** Có tầng {@code SAI}: không bao giờ duyệt được. */
    BLOCKED,
    /** Mọi tầng đã {@code DAT}: không có gì để duyệt. */
    NOTHING_TO_APPROVE,
    /** Đã duyệt rồi. */
    ALREADY_APPROVED,
    /** Thiếu tầng nào trong 1–3: thiếu căn cứ, phải kiểm lại (v0 cũng từ chối duyệt khi thiếu tầng). */
    INCOMPLETE,
    /** Bảng công thức của lớp đã đổi sau lượt này. */
    STALE,
    /** Có lượt kiểm mới hơn cho cùng (lớp, bài). */
    NOT_LATEST,
    /** Nội dung bài đã đổi sau lượt này. */
    CONTENT_CHANGED,
    /** Lượt này kiểm với bảng công thức khác bảng hiện tại của lớp. */
    SHEET_CHANGED
}
