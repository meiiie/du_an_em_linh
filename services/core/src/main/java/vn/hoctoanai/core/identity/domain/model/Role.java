package vn.hoctoanai.core.identity.domain.model;

/** Bốn vai trò của sản phẩm (issue #55). Quyền theo lớp kiểm ở use case, không chỉ theo vai trò. */
public enum Role {
    ADMIN,
    SCHOOL_ADMIN,
    TEACHER,
    STUDENT
}
