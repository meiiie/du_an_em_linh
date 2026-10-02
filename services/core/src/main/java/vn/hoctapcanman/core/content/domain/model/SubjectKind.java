package vn.hoctapcanman.core.content.domain.model;

/** Đối tượng của một lượt kiểm 3 tầng. */
public enum SubjectKind {
    /** Bài của ngân hàng; lượt kiểm quyết trạng thái phát hành của bài cho lớp. */
    PROBLEM,
    /** Công thức trong lời gia sư (ADR 013); không duyệt riêng, không có trạng thái phát hành. */
    TUTOR_FORMULA
}
