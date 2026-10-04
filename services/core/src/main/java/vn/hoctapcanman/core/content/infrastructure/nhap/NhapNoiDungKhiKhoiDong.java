package vn.hoctapcanman.core.content.infrastructure.nhap;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import vn.hoctapcanman.core.classroom.application.port.DanhSachLop;

/**
 * Nhập nội dung khi khởi động, chỉ khi {@code app.content.import-on-startup=true} (profile {@code dev}, compose v2): nội
 * dung chung, rồi nội dung theo từng lớp đã có ({@code LopThuSeeder} chạy trước). Nhập lỗi (thiếu tệp, dịch vụ toán không
 * trả lời, bảng không khóa được) thì ghi lỗi và để ứng dụng chạy tiếp: phần chung không ghi gì; lớp lỗi không khóa bảng thiếu
 * hay phát hành theo phán quyết không có, lớp khác vẫn nhập. Lần khởi động sau nhập lại (không làm gì thừa).
 */
@Component
@Order(3)
@ConditionalOnBooleanProperty("app.content.import-on-startup")
public class NhapNoiDungKhiKhoiDong implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(NhapNoiDungKhiKhoiDong.class);

    private final NhapNoiDungChung nhap;
    private final NhapTheoLop theoLop;
    private final DanhSachLop danhSachLop;

    public NhapNoiDungKhiKhoiDong(NhapNoiDungChung nhap, NhapTheoLop theoLop, DanhSachLop danhSachLop) {
        this.nhap = nhap;
        this.theoLop = theoLop;
        this.danhSachLop = danhSachLop;
    }

    @Override
    public void run(ApplicationArguments args) {
        NhapNoiDungChung.DaNhap chung;
        try {
            chung = nhap.nhapGiuBai();
        } catch (RuntimeException e) {
            LOG.error("Nhập nội dung chung không xong, chưa ghi gì: {}", e.getMessage());
            return;
        }
        for (UUID lop : danhSachLop.moiLop()) {
            try {
                theoLop.nhap(lop, chung.bai());
            } catch (RuntimeException e) {
                LOG.error("Nhập nội dung cho lớp {} không xong: {}", lop, e.getMessage());
            }
        }
    }
}
