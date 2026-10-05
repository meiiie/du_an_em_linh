package vn.hoctapcanman.mau.xau.tutor.application.service;

import java.util.UUID;
import vn.hoctapcanman.mau.xau.content.application.port.LoiGiaiSauKhiNop;

/** Vi phạm: gia sư đọc lời giải qua cổng sau khi nộp, đi tắt qua cửa MoLoiGiai (FR-006). Qua cổng nên luật module không bắt. */
public class HoiGiaSuDocLoiGiai {

    private final LoiGiaiSauKhiNop loiGiai;

    public HoiGiaSuDocLoiGiai(LoiGiaiSauKhiNop loiGiai) {
        this.loiGiai = loiGiai;
    }

    public String goiY(UUID lop, UUID bai) {
        return loiGiai.vanBan(lop, bai, 1).orElse("");
    }
}
