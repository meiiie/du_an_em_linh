package vn.hoctapcanman.core.content.infrastructure.nhap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Nhập nội dung chung khi khởi động, chỉ khi {@code app.content.import-on-startup=true} (profile {@code dev}, compose v2).
 * Nhập lỗi (thiếu tệp, dịch vụ toán không giải được) thì ghi lỗi và để ứng dụng chạy tiếp, không ghi gì vào CSDL: lần
 * khởi động sau nhập lại.
 */
@Component
@Order(3)
@ConditionalOnBooleanProperty("app.content.import-on-startup")
public class NhapNoiDungKhiKhoiDong implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(NhapNoiDungKhiKhoiDong.class);

    private final NhapNoiDungChung nhap;

    public NhapNoiDungKhiKhoiDong(NhapNoiDungChung nhap) {
        this.nhap = nhap;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            nhap.nhap();
        } catch (RuntimeException e) {
            LOG.error("Nhập nội dung chung không xong, chưa ghi gì: {}", e.getMessage());
        }
    }
}
