# ADR 012 — Quyền riêng tư cho dữ liệu học sinh trước pilot

**Trạng thái:** Đề xuất (2026-10-02) — chờ chủ repo duyệt và luật sư trả lời 6 câu hỏi ở §11 của bản phân tích.

## Bối cảnh

v0 chỉ dùng dữ liệu tổng hợp (ADR 006). Pilot với học sinh thật chịu Luật BVDLCN 91/2025/QH15 và Nghị định 356/2025/NĐ-CP (cùng hiệu lực 01/01/2026), cùng Luật Trí tuệ nhân tạo 134/2025/QH15 (hiệu lực 01/03/2026). Ba điểm đổi cách làm:

- NĐ 356 mở rộng dữ liệu nhạy cảm sang dữ liệu theo dõi hành vi trên dịch vụ trực tuyến. Mức hiểu và lịch học của sản phẩm rất có thể thuộc nhóm này, nên miễn trừ cho doanh nghiệp nhỏ khó áp dụng.
- Gọi LLM nước ngoài là chuyển dữ liệu xuyên biên giới: cần hồ sơ đánh giá tác động, tiền kiểm bởi A05.
- Luật AI buộc báo cho người dùng biết họ đang tương tác với AI, và để con người giám sát.

Phân tích, chấm 4 phương án, độ nhạy, câu hỏi cho luật sư: [`labs/decisions/2026-10-02-quyen-rieng-tu-du-lieu-hoc-sinh.md`](../../labs/decisions/2026-10-02-quyen-rieng-tu-du-lieu-hoc-sinh.md).

## Quyết định

- **Pilot đầu qua nhà trường.** Trường là bên kiểm soát dữ liệu, sản phẩm là bên xử lý, có phụ lục xử lý dữ liệu. B2C cần ADR riêng.
- **Không có học sinh thật khi chưa có bản ghi đồng ý kiểm chứng được.**
  - Đồng ý tách theo mục đích: `HOC_TAP`, `GIA_SU_AI`, `NHAC_LICH_NGOAI`; không mặc định đồng ý.
  - Học sinh dưới 18 tuổi: ghi cả phụ huynh và học sinh. Đây là mức an toàn, luật sư có thể thu hẹp.
- **Chưa đồng ý `GIA_SU_AI` thì dùng gia sư `offline`:** không gọi LLM, vẫn học được.
- **Tối thiểu hóa.**
  - Định danh chỉ ở `services/core`.
  - `services/core` khử định danh văn bản (email, số điện thoại, tên trong lớp, mã học sinh) trước khi gọi LLM.
  - Nhà LLM và `services/math` chỉ nhận nội dung toán, mã lỗi, mã giả danh theo phiên.
  - Ảnh bài làm, kể cả vùng đã cắt, không gửi ra nước ngoài: OCR trong nước hoặc tự host.
- **Nhà LLM cho dữ liệu thật** phải có điều khoản không dùng dữ liệu để huấn luyện, có thỏa thuận xử lý dữ liệu, và CTIA riêng. Z.AI và OpenRouter chỉ dùng với dữ liệu tổng hợp cho tới khi đánh giá xong.
- **Quyền chủ thể trong ứng dụng:** xem, sửa, tải về, xóa, rút đồng ý, theo thời hạn NĐ 356 (xóa: 20 ngày). Xóa và hết hạn lưu chạy tự động, có nhật ký.
- **Minh bạch và giám sát AI.**
  - Mọi câu gia sư gắn nhãn «Gia sư AI».
  - Giáo viên xem và ghi đè mức hiểu, gợi ý bài; mỗi gợi ý có lý do xem được.
- **Bảo mật.**
  - CSDL của `services/core` giữ mức RLS của v0 (migration 0010: `ENABLE` + `FORCE`) cho mọi bảng dữ liệu học sinh, kể cả bảng mới; vai trò CSDL của ứng dụng không phải superuser.
  - Kiểm quyền theo lớp ở use case.
  - Mã hóa khi lưu và khi truyền; nhật ký kiểm toán mọi lần đọc.
  - Kế hoạch ứng phó sự cố.
- **Trước ngày pilot ít nhất 2 tháng:**
  - DPIA và CTIA (Mẫu 09, 10 NĐ 356) có sơ đồ luồng dữ liệu;
  - chỉ định nhân sự hoặc dịch vụ bảo vệ dữ liệu.

## Hệ quả

- ADR 006 giữ hiệu lực cho tới khi có hồ sơ và bản ghi đồng ý đầu tiên. `consent_records` của v0 là móc, v2 viết lại ở `services/core`. Câu «RLS chưa `FORCE`» của ADR 006 đã cũ: migration 0010 đã bật `FORCE` cho 5 bảng dữ liệu học của v0.
- Việc mới, mỗi việc một issue khi ADR được chấp nhận:
  - mô hình đồng ý và quyền chủ thể ở `services/core`;
  - bộ khử định danh, có bộ đo tỉ lệ lọt ở `labs/evals`;
  - lịch xóa và hết hạn lưu;
  - nhãn AI, màn ghi đè cho giáo viên;
  - hồ sơ DPIA / CTIA.
- Port identity (#55) giữ chỗ cho tuổi, người đại diện, trạng thái đồng ý; không thu ngày sinh đầy đủ nếu năm sinh đủ dùng.
- Mở lại ADR này khi gặp một trong các tín hiệu ở §10 của bản phân tích: luật sư trả lời khác giả định, khách chọn B2C, mô hình chạy hoàn toàn tại Việt Nam (đã xác minh nơi đặt dữ liệu và bên xử lý phụ) đạt bộ AI 70 ca, danh mục AI rủi ro cao được ban hành.
