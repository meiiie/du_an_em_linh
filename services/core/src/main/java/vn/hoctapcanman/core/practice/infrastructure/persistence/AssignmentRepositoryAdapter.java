package vn.hoctapcanman.core.practice.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.practice.domain.model.Assignment;
import vn.hoctapcanman.core.practice.domain.model.AssignmentStatus;
import vn.hoctapcanman.core.practice.domain.repository.AssignmentRepository;

/** Giao bài trên {@code assignments}; trigger của V7 kiểm học sinh của lớp và bài đã phát hành cho lớp lúc ghi. */
@Repository
public class AssignmentRepositoryAdapter implements AssignmentRepository {

    private final JdbcClient jdbc;

    public AssignmentRepositoryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void saveAll(List<Assignment> giao) {
        for (Assignment a : giao) {
            jdbc.sql("""
                    insert into assignments (id, class_id, problem_id, student_id, status, set_name, due_at, assigned_by, assigned_at)
                    values (:id, :lop, :bai, :hs, :st, :bo, :han, :nguoi, :luc)
                    on conflict (class_id, problem_id, student_id) do update set status = excluded.status,
                        set_name = excluded.set_name, due_at = excluded.due_at, assigned_by = excluded.assigned_by,
                        assigned_at = excluded.assigned_at""")
                .param("id", a.id()).param("lop", a.classId()).param("bai", a.problemId()).param("hs", a.studentId())
                .param("st", a.status().name()).param("bo", a.setName()).param("han", Cot.lucNeuCo(a.dueAt()))
                .param("nguoi", a.assignedBy()).param("luc", Cot.luc(a.assignedAt()))
                .update();
        }
    }

    @Override
    public List<Assignment> forStudent(UUID classId, UUID studentId) {
        return jdbc.sql("""
                select id, class_id, problem_id, student_id, status, set_name, due_at, assigned_by, assigned_at from assignments
                where class_id = :lop and student_id = :hs and status = 'DA_GIAO' order by assigned_at, id""")
            .param("lop", classId).param("hs", studentId).query(AssignmentRepositoryAdapter::giao).list();
    }

    private static Assignment giao(ResultSet rs, int n) throws SQLException {
        return new Assignment(Cot.uuid(rs, "id"), Cot.uuid(rs, "class_id"), Cot.uuid(rs, "problem_id"), Cot.uuid(rs, "student_id"),
            AssignmentStatus.valueOf(Cot.chu(rs, "status")), rs.getString("set_name"), Cot.thoiDiemNeuCo(rs, "due_at"),
            Cot.uuidNeuCo(rs, "assigned_by"), Cot.thoiDiem(rs, "assigned_at"));
    }
}
