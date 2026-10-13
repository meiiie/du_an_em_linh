package vn.hoctapcanman.core.practice.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.classroom.application.port.ClassMembership;
import vn.hoctapcanman.core.content.application.dto.hocsinh.BaiChoLamBai;
import vn.hoctapcanman.core.content.application.port.BaiDeLam;
import vn.hoctapcanman.core.practice.application.service.BaiLamHienTai;
import vn.hoctapcanman.core.practice.domain.model.Assignment;
import vn.hoctapcanman.core.practice.domain.model.SubmissionHistory;
import vn.hoctapcanman.core.practice.domain.repository.*;

class GetBaiCuaHocSinhPreviewTest {
    @Test void danhSachDungLatexHienThiVaTenBoTuLanGiao() {
        UUID student=UUID.randomUUID(),cl=UUID.randomUUID(),problem=UUID.randomUUID();
        var membership=mock(ClassMembership.class);
        var content=mock(BaiDeLam.class);
        var assignments=mock(AssignmentRepository.class);
        var submissions=mock(SubmissionRepository.class);
        var grades=mock(GradingResultRepository.class);
        String preview="\\text{Đường chéo hộp chữ nhật, ba cạnh }4,7,8\\text{ cm}";
        var bai=new BaiChoLamBai(problem,"NT-KG12-VD-04",1,"T12.NT.KG","Hình học không gian","VAN_DUNG",
            "Hộp chữ nhật có ba cạnh 4, 7, 8 cm. Tính đường chéo.",preview,"TU_LUAN",null,List.of(),List.of(),null);
        when(membership.laHocSinh(student,cl)).thenReturn(true);
        when(content.baiDaPhatHanh(cl)).thenReturn(List.of(bai));
        when(assignments.forStudent(cl,student)).thenReturn(List.of(Assignment.of(cl,problem,student,
            "Ôn tập hình học không gian – tuần 7",Instant.parse("2026-10-20T12:00:00Z"),UUID.randomUUID(),Instant.EPOCH)));
        when(submissions.findOpen(student,cl,problem,1)).thenReturn(Optional.empty());
        when(submissions.history(student,problem)).thenReturn(new SubmissionHistory(student,problem,List.of()));
        var result=new GetBaiCuaHocSinhUseCase(membership,content,assignments,new BaiLamHienTai(submissions,grades)).execute(student,cl);
        assertThat(result).singleElement().satisfies(row -> {
            assertThat(row.deBaiLatex()).isEqualTo(preview);
            assertThat(row.tenBo()).isEqualTo("Ôn tập hình học không gian – tuần 7");
            var json=JsonMapper.builder().build().valueToTree(row);
            assertThat(json.get("tenBo").asString()).isEqualTo(row.tenBo());
            assertThat(json.get("deBaiLatex").asString()).isEqualTo(preview);
        });
    }
}
