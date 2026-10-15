package vn.hoctapcanman.core.workspace.infrastructure.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.Item;

class BoBaiSeedTest {
    @Test void gomNhieuBaiVaHocSinhThanhMotBoNhungGiuHanVaModeRieng() {
        var rows=List.of(row("a","s1","P1","2026-10-20","ON_TAP",true),
            row("b","s2","P1","2026-10-20","ON_TAP",false),
            row("c","s1","P2","2026-10-20","ON_TAP",true),
            row("d","s2","P2","2026-10-20","ON_TAP",true),
            row("e","s1","P1","2026-10-21","ON_TAP",false),
            row("f","s1","P1","2026-10-20","KIEM_TRA",false));
        var sets=WorkspaceNguyenTraiSeedAdapter.boBai(rows);
        assertThat(sets).hasSize(3);
        var first=sets.stream().filter(s -> s.details().get("assignmentCount").equals(4)).findFirst().orElseThrow();
        assertThat(first.title()).isEqualTo("Ôn tập tích phân – tuần 7");
        assertThat(first.details()).containsEntry("problemCount",2).containsEntry("studentCount",2)
            .containsEntry("submittedCount",3).containsEntry("dueAt","2026-10-20")
            .containsEntry("problemCodes",List.of("P1","P2"));
        assertThat(WorkspaceNguyenTraiSeedAdapter.boBai(rows)).isEqualTo(sets);
        assertThat(sets.stream().map(Item::id).distinct()).hasSize(3);
    }
    @Test void boKhongCoHanKhongBiMatVaDanhSachRongAnToan() {
        assertThat(WorkspaceNguyenTraiSeedAdapter.boBai(List.of())).isEmpty();
        assertThat(WorkspaceNguyenTraiSeedAdapter.boBai(List.of(row("a","s1","P1","","ON_TAP",false))))
            .singleElement().satisfies(s -> assertThat(s.details()).containsEntry("dueAt",""));
    }
    @Test void productionGroupsByAssignmentBatchRatherThanStudentOrTitle() {
        var first=row("a","s1","P1","2026-10-20","ON_TAP",true);
        var details=new java.util.HashMap<>(first.details());details.put("setKey","batch1");
        var second=new java.util.HashMap<>(details);second.put("studentId","s2");
        var third=new java.util.HashMap<>(details);third.put("setKey","batch2");
        var groups=vn.hoctapcanman.core.workspace.infrastructure.persistence.WorkspaceStoreAdapter.groupAssignments(List.of(
            new Item("a",first.title(),"","ON_TAP",details),new Item("b",first.title(),"","ON_TAP",second),new Item("c",first.title(),"","ON_TAP",third)));
        assertThat(groups).hasSize(2);
        assertThat(groups.getFirst().title()).isEqualTo(first.title());
        assertThat(groups.getFirst().details()).containsEntry("assignmentCount",2).containsEntry("studentCount",2).containsEntry("submittedCount",2);
    }
    private Item row(String id,String student,String problem,String due,String mode,boolean submitted) {
        return new Item(id,"Ôn tập tích phân – tuần 7","",mode,
            Map.of("tenBo","Ôn tập tích phân – tuần 7","classId","lop","studentId",student,
                "problemCode",problem,"dueAt",due,"mode",mode,"submitted",submitted));
    }
}
