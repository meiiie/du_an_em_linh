package vn.hoctapcanman.core.practice.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.practice.infrastructure.client.MayChamQuaDichVu;
import vn.hoctapcanman.core.shared.infrastructure.math.*;
import vn.hoctapcanman.core.tutor.application.port.TermFeedback;

class MayChamPhanHoiTest {
    @Test void chiRaSoHangDoHocSinhNhapKhongTraGiaTriDung() {
        var math=mock(MathServiceClient.class);
        var terms=mock(TermFeedback.class);
        var request=Map.<String,Object>of("ham","x**2-6*x+4","cac_buoc",List.of(
            Map.of("ma_buoc","B.DH.DAOHAM","cac_dong",List.of(Map.of("dong",0,"latex","2x-6x+4")))));
        when(math.call(MathJob.GRADE,request)).thenReturn(new MathResult.Ok(Map.of(
            "ket_qua","SAI","buoc_sai",Map.of("ma_buoc","B.DH.DAOHAM","dong",0),"thong_bao","Cần xem lại.")));
        when(terms.derivative("x**2-6*x+4","2x-6x+4")).thenReturn(Optional.of(new TermFeedback.Feedback(List.of(
            new TermFeedback.Term(1,"2x","ok"),new TermFeedback.Term(2,"-6x","sai"),new TermFeedback.Term(3,"+4","thua")),0)));
        var result=new MayChamQuaDichVu(math,terms).cham(request).orElseThrow();
        assertThat(result.get("thong_bao").toString()).contains("Số hạng thứ 2 «-6x» cần xem lại", "Số hạng thứ 3 «+4» đang thừa").doesNotContain("2*x-6");
        assertThat(result.get("ket_qua")).isEqualTo("SAI");
    }
    @Test void soHangKhongKiemDuocThiGiuPhanHoiCham() {
        var math=mock(MathServiceClient.class);var terms=mock(TermFeedback.class);
        var request=Map.<String,Object>of("ham","x**2","cac_buoc",List.of(Map.of("ma_buoc","B.DH.DAOHAM","cac_dong",List.of(Map.of("dong",0,"latex","?")))));
        var body=Map.<String,Object>of("ket_qua","SAI","buoc_sai",Map.of("ma_buoc","B.DH.DAOHAM","dong",0),"thong_bao","Cần xem lại.");
        when(math.call(MathJob.GRADE,request)).thenReturn(new MathResult.Ok(body));
        when(terms.derivative("x**2","?")).thenReturn(Optional.empty());
        assertThat(new MayChamQuaDichVu(math,terms).cham(request).orElseThrow()).isEqualTo(body);
    }
}
