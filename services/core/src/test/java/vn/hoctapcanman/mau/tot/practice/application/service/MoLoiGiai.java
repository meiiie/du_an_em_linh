package vn.hoctapcanman.mau.tot.practice.application.service;

import java.util.Optional;
import java.util.UUID;
import vn.hoctapcanman.mau.tot.content.application.port.LoiGiaiSauKhiNop;

/** Đúng: cửa duy nhất được dùng cổng lời giải sau khi nộp. */
public class MoLoiGiai {

    private final LoiGiaiSauKhiNop loiGiai;

    public MoLoiGiai(LoiGiaiSauKhiNop loiGiai) {
        this.loiGiai = loiGiai;
    }

    public Optional<String> cho(UUID lop, UUID bai, int phienBan) {
        return loiGiai.vanBan(lop, bai, phienBan);
    }
}
