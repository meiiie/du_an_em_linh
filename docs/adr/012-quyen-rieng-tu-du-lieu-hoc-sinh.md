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
  - Định danh chỉ ở `services/core`. Ngoại lệ duy nhất là kênh nhắc lịch ngoài người dùng đã đồng ý (`NHAC_LICH_NGOAI`).
    - Mặc định nhắc trong ứng dụng.
    - Nhà cung cấp kênh (email, Zalo…) là bên xử lý theo mục đích: chỉ nhận địa chỉ nhận và nội dung nhắc tối thiểu, không có điểm hay mức hiểu.
    - Cần thỏa thuận xử lý, thời hạn lưu, đánh giá nơi đặt dữ liệu; có CTIA nếu dữ liệu ra nước ngoài.
  - `services/core` khử định danh văn bản trước khi gọi LLM, **đóng mặc định**:
    - lọc mẫu đã biết (email, số điện thoại, tên trong lớp, mã học sinh, số CCCD);
    - chạy thêm phát hiện thông tin cá nhân cục bộ (tên người, địa chỉ, tài khoản mạng xã hội, vị trí);
    - câu bộ phát hiện không chắc chắn thì không gửi LLM: trả lời bằng gia sư `offline`;
    - tỉ lệ lọt đo ở `labs/evals` để theo dõi, không thay cho việc chặn.
  - Nhà LLM và `services/math` chỉ nhận nội dung toán, mã lỗi, mã giả danh theo phiên.
  - Ảnh bài làm của học sinh, kể cả vùng đã cắt, **chỉ OCR tự host**. Học sinh có thể viết tên, số điện thoại ở bất kỳ đâu trên trang, nên che vùng định sẵn không đủ.
    - Dịch vụ OCR bên ngoài (kể cả trong nước) chỉ dùng cho tài liệu của giáo viên không chứa dữ liệu học sinh (đề, sách).
    - Khi đó nhà OCR là bên xử lý: có thỏa thuận, thời hạn lưu, không dùng tài liệu để huấn luyện.
- **Nhà LLM cho dữ liệu thật** phải có điều khoản không dùng dữ liệu để huấn luyện, có thỏa thuận xử lý dữ liệu, và CTIA riêng. Z.AI và OpenRouter chỉ dùng với dữ liệu tổng hợp cho tới khi đánh giá xong.
- **Quyền chủ thể trong ứng dụng:** xem, sửa, tải về, xóa, rút đồng ý, theo thời hạn NĐ 356 (xóa: 20 ngày). Xóa và hết hạn lưu chạy tự động, có nhật ký.
  - Rút đồng ý có hiệu lực ngay: mục đích bị tắt trong cùng giao dịch ghi nhận yêu cầu, trước khi xếp việc dọn ở bên xử lý. Từ lúc đó gia sư dùng `offline`, nhắc lịch chỉ trong ứng dụng.
  - Nhật ký sự kiện đồng ý lưu ngoài dữ liệu được sao lưu. Mỗi dòng: mã giả danh, loại sự kiện (xóa toàn bộ, rút đồng ý, đồng ý lại), mục đích, thứ tự; không có dữ liệu cá nhân khác.
  - Cùng kho đó giữ sổ chuyển dữ liệu cho bên xử lý và trạng thái dọn: bên nhận, mục đích, mã yêu cầu, xác nhận.
  - Khôi phục bản sao lưu thì phát lại nhật ký theo thứ tự trước khi mở lại dịch vụ: dữ liệu đã xóa không sống lại, mỗi mục đích về đúng trạng thái mới nhất, việc dọn còn dở ở bên xử lý chạy tiếp.
  - Phát lại «đồng ý lại» đóng mặc định: bản ghi đồng ý đầy đủ (người đồng ý, vai trò, phiên bản văn bản, thời điểm, kênh, bằng chứng) không có trong dữ liệu đã khôi phục thì mục đích giữ tắt và ứng dụng xin đồng ý lại.
  - Mỗi lần chuyển dữ liệu cho bên xử lý ghi kèm mục đích, để yêu cầu đi đúng nơi:
    - rút đồng ý một mục đích chỉ gửi tới các bên xử lý của mục đích đó; rút `NHAC_LICH_NGOAI` không đụng nhà LLM của `GIA_SU_AI`;
    - yêu cầu xóa toàn bộ gửi tới mọi bên xử lý đã nhận dữ liệu của người đó (kênh nhắc lịch, nhà LLM nếu có lưu, OCR ngoài nếu có).
  - Theo dõi xác nhận và thời hạn của từng bên. Yêu cầu chỉ hoàn tất khi mọi bên liên quan đã xác nhận.
- **Minh bạch và giám sát AI.**
  - Mọi câu gia sư gắn nhãn «Gia sư AI».
  - Giáo viên xem và ghi đè mức hiểu, gợi ý bài; mỗi gợi ý có lý do xem được.
- **Bảo mật.**
  - CSDL của `services/core`: `ENABLE` + `FORCE` RLS cho mọi bảng dữ liệu học sinh, kể cả bảng mới.
    - Chính sách **đóng mặc định**: thiếu ngữ cảnh người dùng (`app.user_id`) thì không thấy dòng nào.
    - Ngữ cảnh đặt **trong từng giao dịch** bằng `set_config('app.user_id', …, true)`, như `apps/web/lib/rls.ts`; không đặt ở mức phiên kết nối, vì pool tái dùng kết nối sẽ mang ngữ cảnh của học sinh trước. Truy cập dữ liệu học sinh ngoài giao dịch bị từ chối.
    - Khác v0: hàm `rls_duoc_xem_hs` của migration 0010 cho qua mọi dòng khi chưa đặt ngữ cảnh (để chạy tác vụ hệ thống), nên không làm mốc an toàn cho dữ liệu thật.
    - Tác vụ bảo trì (migration, seed, báo cáo tổng hợp) dùng vai trò CSDL riêng, có kiểm soát.
    - Vai trò CSDL của ứng dụng không phải superuser và không có `BYPASSRLS` (cả hai đều bỏ qua RLS).
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
