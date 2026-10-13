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
}
