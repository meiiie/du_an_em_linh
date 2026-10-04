package vn.hoctapcanman.core.content.infrastructure.nhap;

import vn.hoctapcanman.core.shared.infrastructure.math.MathJob;
import vn.hoctapcanman.core.shared.infrastructure.math.MathResult;

/**
 * Dịch vụ toán không trả lời được một job của importer (lỗi HTTP, hết giờ, JSON hỏng, phong bì lỗi của sandbox). Khác câu
 * trả lời «không dùng được»: lỗi này không nói gì về bài, nên importer dừng thay vì nhập bài thiếu lời giải hay bỏ biến
 * thể. Thông điệp chỉ có job và lý do, không có {@code detail} của {@link MathResult.Failed} (không ghi vào log).
 */
public class DichVuToanKhongTraLoi extends RuntimeException {

    public DichVuToanKhongTraLoi(MathJob job, MathResult.Reason lyDo) {
        super("Dịch vụ toán không trả lời " + job + ": " + lyDo);
    }
}
