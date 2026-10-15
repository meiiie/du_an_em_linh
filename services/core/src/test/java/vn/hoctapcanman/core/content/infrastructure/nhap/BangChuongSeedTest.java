package vn.hoctapcanman.core.content.infrastructure.nhap;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.content.domain.model.Formula;
import vn.hoctapcanman.core.content.domain.model.FormulaSheet;
import vn.hoctapcanman.core.content.domain.model.SheetStatus;
import vn.hoctapcanman.core.shared.infrastructure.seed.TruongNguyenTrai;

class BangChuongSeedTest {
    @Test void chiNhanNhapSeedDungIdVaChuaBiSua() {
        UUID cl=TruongNguyenTrai.id("lop:12A3");
        UUID id=TruongNguyenTrai.id("bang-chuong:"+cl+":2");
        List<Formula> rows=List.of(Formula.unchecked(1,"NT-CT-01","T11.NT.LG","Hệ thức lượng giác","\\sin^2 x+\\cos^2 x=1","Tổng bình phương sin và cos bằng 1."));
        String note=NhapTheoLop.ghiChuBangChuong(rows,id);
        FormulaSheet original=new FormulaSheet(id,cl,2,SheetStatus.NHAP,note,null,null,null,Instant.now(),rows);
        assertThat(NhapTheoLop.laBangChuongSeed(original)).isTrue();
        assertThat(NhapTheoLop.laBangChuongSeed(new FormulaSheet(UUID.randomUUID(),cl,3,SheetStatus.NHAP,note,null,null,null,Instant.now(),rows))).isFalse();
        List<Formula> edited=List.of(Formula.unchecked(1,"NT-CT-01","T11.NT.LG","Hệ thức lượng giác","\\sin^2 x+\\cos^2 x=2","Giáo viên đang chỉnh."));
        FormulaSheet changed=new FormulaSheet(id,cl,2,SheetStatus.NHAP,note,null,null,null,original.createdAt(),edited);
        assertThat(NhapTheoLop.laBangChuongSeed(changed)).isFalse();
    }
    @Test void moTaGocNhanDoiDungVaMigrationCapNhatCungNoiDung() throws Exception {
        var json=tools.jackson.databind.json.JsonMapper.builder().build();
        var source=json.readTree(java.nio.file.Path.of("../../data/dev/nguyen-trai/bang-chuong.json").toFile());
        var formula=source.path("formulas").get(1);
        assertThat(formula.path("ma").asString()).isEqualTo("NT-CT-02");
        String statement=formula.path("noiDung").asString();
        assertThat(statement).contains("Công thức góc nhân đôi", "cos 2x", "sin^2 x").doesNotContain("tổng hai bình phương");
        String migration=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/db/migration/V19__qa_hang_duyet_va_cong_thuc.sql"));
        assertThat(migration).contains(statement).contains("s.status='NHAP'");
        var rows=new java.util.ArrayList<Formula>();
        for(var row:source.path("formulas")) rows.add(Formula.unchecked(rows.size()+1,row.path("ma").asString(),
            row.path("skillCode").asString(),row.path("title").asString(),row.path("latex").asString(),row.path("noiDung").asString()));
        String note=NhapTheoLop.ghiChuBangChuong(rows,UUID.randomUUID());
        assertThat(migration).contains(note.substring(0,note.lastIndexOf(" · bảng ")));

    }

}
