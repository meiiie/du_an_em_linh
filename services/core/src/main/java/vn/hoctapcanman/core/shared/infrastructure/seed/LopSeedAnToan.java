package vn.hoctapcanman.core.shared.infrastructure.seed;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Guard cho mọi SQL dev: lớp đúng định danh và không có thành viên thật/vai trò sai. */
public final class LopSeedAnToan {
    private LopSeedAnToan() {}
    public static boolean kiem(JdbcClient jdbc, String ten) {
        UUID id = TruongNguyenTrai.id("lop:" + ten);
        return jdbc.sql("""
            SELECT EXISTS(SELECT 1 FROM classes c WHERE c.id=:id AND c.name=:ten AND c.school_year=:nam AND c.enabled
                AND EXISTS(SELECT 1 FROM enrollments e JOIN users u ON u.id=e.user_id
                    WHERE e.class_id=c.id AND e.role_in_class='TEACHER' AND u.synthetic AND u.role='TEACHER' AND u.enabled)
                AND NOT EXISTS(SELECT 1 FROM enrollments e JOIN users u ON u.id=e.user_id
                    WHERE e.class_id=c.id AND (NOT u.synthetic OR u.role<>e.role_in_class)))
            """).param("id", id).param("ten", ten).param("nam", TruongNguyenTrai.NAM_HOC).query(Boolean.class).single();
    }
}
