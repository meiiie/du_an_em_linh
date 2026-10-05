package vn.hoctapcanman.mau.xau.content.infrastructure.web;

import java.util.UUID;
import vn.hoctapcanman.mau.xau.content.application.port.LoiGiaiSauKhiNop;

/** Vi phạm: lớp trong chính module nội dung trả lời giải qua cổng sau khi nộp, đi tắt qua cửa MoLoiGiai (phán quyết #142). */
public class LoiGiaiTatApi {

    private final LoiGiaiSauKhiNop loiGiai;

    public LoiGiaiTatApi(LoiGiaiSauKhiNop loiGiai) {
        this.loiGiai = loiGiai;
    }

    public String loiGiai(UUID lop, UUID bai) {
        return loiGiai.vanBan(lop, bai, 1).orElse("");
    }
}
