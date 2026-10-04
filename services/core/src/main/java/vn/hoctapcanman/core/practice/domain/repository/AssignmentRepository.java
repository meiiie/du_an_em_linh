package vn.hoctapcanman.core.practice.domain.repository;

import java.util.List;
import java.util.UUID;
import vn.hoctapcanman.core.practice.domain.model.Assignment;

/** Giao bài (V7: chỉ bài đã phát hành của lớp, chỉ học sinh của lớp). */
public interface AssignmentRepository {

    /**
     * Giao các bài trong một giao dịch: thiếu điều kiện ở một dòng thì không dòng nào được ghi. Giao lại cùng (lớp, bài,
     * học sinh) thì cập nhật dòng cũ, giữ id cũ.
     */
    void saveAll(List<Assignment> giao);

    /** Bài đang giao cho học sinh ở lớp, theo lúc giao. */
    List<Assignment> forStudent(UUID classId, UUID studentId);
}
