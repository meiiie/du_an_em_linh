# Feature Specification: Hoàn thiện MathL+ v2

**Feature Branch**: `002-hoan-thien-v2` (tên epic; phiên tài liệu ở `lane/docs`)  
**Created**: 2026-10-13  
**Status**: Bản thảo đặc tả nghiệm thu; chưa chứng nhận hiện thực  
**Input**: Chỉ đạo chủ repo: gỡ v0, FLAT tuyệt đối, hoàn thiện U0–U15 trên Angular/core/math.

## Phạm vi và nguồn

Lát tiếp nối [001](../001-lat-cat-doc/spec.md), năng lực C1–C10 và pha P2; lấy phần giao bài, quản trị cơ bản từ P3, lịch trong app từ P4 mà không tuyên bố hoàn thành toàn pha. Một chủ đề Toán 12: đơn điệu và cực trị. Dữ liệu demo tổng hợp; gia sư mặc định `offline`.

Nguồn chỉ đọc: `/workspace/du_an_em_linh-notes/UX_REFERENCE_PLAN.md` (U0–U15), `REQUIREMENTS_ANALYSIS.md`, `/workspace/ref-LMS_hohulili` (tham chiếu thiết kế ghi SHA `34c3f0f2`). Chỉ lấy mẫu tương tác; không lấy màu thương hiệu, bóng, viền nhấn hay mã chưa kiểm giấy phép. Các điểm chặn cũ về giữ v0/quyền Codex được thay bởi chỉ đạo ngày 2026-10-13.

Mặc định câu hỏi mở ở [ASSUMPTIONS](../../docs/ASSUMPTIONS.md). Hợp đồng API ở [API_UX](../../docs/API_UX.md); kế hoạch và nhiệm vụ ở [plan](plan.md), [tasks](tasks.md). Phiên DOCS chỉ sửa tài liệu, không commit, không đổi workflow, bí mật, migration hay dữ liệu sư phạm.

## User Scenarios & Testing

### US1 — Đi học bằng giao diện phẳng (P1, U0–U4)

Học sinh đăng nhập, xem bài tiếp theo và bốn mức hiểu, mở đúng bài trên điện thoại hoặc desktop. Đây là nền cho mọi vòng học.

**Independent Test**: dùng An tổng hợp, kiểm trang Học ở 390/1280/1440 px, sáng/tối, bàn phím và giảm chuyển động.

1. **Given** An có bài được giao và mức hiểu, **When** mở `/hs`, **Then** thấy «Chào An», «Bài tiếp theo», bốn mức NB/TH/VD/VDC; đề xuất do server tính theo kỹ năng yếu và tối đa +1 mức, không vượt VDC.
2. **Given** màn đang tải, rỗng hoặc lỗi, **When** mở từng mục, **Then** có skeleton khớp bố cục, thông báo rỗng hoặc «Thử lại»; lỗi không làm mất dữ liệu đã nhập.
3. **Given** bàn phím hoặc màn 390 px, **When** đổi mục/mở hộp thoại, **Then** focus rõ, không bị bottom nav/bàn phím che, Escape đóng và trả focus về nút mở.
4. **Given** bất kỳ màn nào, **When** rà hình và cổng tĩnh, **Then** không bóng đổ/inset/token bóng/viền nhấn trái/inline-start/dải màu mép/3D.

### US2 — Giáo viên giao bài, học sinh ôn hoặc kiểm tra (P1, U7–U9)

Giáo viên giao cho lớp hoặc từng em, đặt hạn và chế độ. Học sinh ôn bằng gợi ý, tự làm kiểm tra không gia sư. Đây là vòng nghiệm thu chính.

**Independent Test**: hai giao bài tổng hợp với cùng nội dung, một ôn tập và một kiểm tra; kiểm UI và gọi thẳng API.

1. **Given** giáo viên thuộc lớp, **When** chọn bài đã phát hành, người nhận, `ON_TAP`/`KIEM_TRA` và hạn, **Then** server lưu giao bài, trả chế độ/hạn; người ngoài lớp không đọc/ghi được.
2. **Given** bài ôn, **When** An hỏi «Cần gợi ý?» cấp 1–3 hoặc xin đáp án, **Then** chỉ nhận gợi mở đã kiểm, không làm hộ bước/kết quả cuối; gia sư không đọc lời giải chuẩn.
3. **Given** bài kiểm tra, **When** An sửa payload thành ôn, gọi trực tiếp tutor, mở tab cũ hay phát lại yêu cầu, **Then** server đọc giao đang hiệu lực và trả 403 trước SSE, trước truy hồi/gọi nhà AI; không có câu gia sư.
4. **Given** hạn đã qua, **When** gửi bước/nộp hoặc hỏi gia sư, **Then** server dùng thời gian của mình để từ chối thay đổi; đổi đồng hồ trình duyệt không vượt hạn. Xem lại phần đã lưu vẫn được theo quyền.
5. **Given** CAS lỗi/hết giờ hoặc SSE ngắt, **When** hỏi gia sư, **Then** không gửi câu chưa lọc, không tự phát lại hay tự đổi nhà; chỉ gợi ý đã kiểm với phiên bản bảng hiện tại hoặc câu cố định không chứa toán.

### US3 — Xem công thức, tài liệu và flashcard (P1, U5–U6)

Học sinh ôn nội dung của lớp và xuất thẻ để nhập thủ công vào Quizlet.

**Independent Test**: lớp có công thức khóa và một tài liệu tự soạn; lớp khác có nội dung riêng.

1. **Given** An thuộc lớp, **When** mở `/hs/kho`, **Then** chỉ thấy nội dung được phép của lớp, công thức có phiên bản đã khóa; nháp/chặn/stale không tới học sinh.
2. **Given** flashcard, **When** dùng Enter/Space để đổi mặt, **Then** đọc được cả hai mặt, focus không mất, không xoay 3D; chế độ giảm chuyển động đổi tức thì.
3. **Given** dữ liệu có tiếng Việt, dấu phẩy, nháy, xuống dòng hoặc đầu ô `= + - @`, **When** xuất CSV, **Then** hai cột thuật ngữ/định nghĩa UTF-8 có escape và chống công thức bảng tính; không có định danh học sinh hay nội dung lớp khác. Hướng dẫn nhập thủ công, không hứa đồng bộ API Quizlet.

### US4 — Giáo viên quản lý tiến độ và nội dung (P2, U10–U12)

Giáo viên theo dõi học sinh kẹt, ghi đè mức có lý do, soạn nội dung và xử lý mục chờ duyệt.

**Independent Test**: giáo viên một lớp, học sinh tổng hợp, bài `DAT`, `SAI`, `KHONG_KIEM_DUOC`, một phiên bản công thức stale.

1. **Given** tổng quan, **When** bấm chỉ số, **Then** đến danh sách chi tiết tương ứng; bảng học sinh × kỹ năng xem 4 mức hoặc 3 mức CV 7991, không thay dữ liệu 4 mức.
2. **Given** học sinh thuộc lớp, **When** giáo viên ghi đè mức hoặc bài kế có lý do, **Then** nhật ký lưu người/lúc/lý do; học sinh khác lớp bị từ chối.
3. **Given** tài liệu tự soạn TXT/MD/PDF có chữ ≤5 MiB, **When** tải lên, **Then** nội dung được tách chữ; PDF scan, tệp không hợp lệ hoặc quá cỡ báo lỗi tiếng Việt, không suy diễn có OCR.
4. **Given** bài/công thức mới, **When** kiểm, **Then** có kết quả ba tầng và căn cứ; `SAI` bị chặn, `KHONG_KIEM_DUOC` chờ giáo viên, sửa nội dung/bảng buộc kiểm lại theo hash/phiên bản.
5. **Given** hàng chờ, **When** Duyệt/Chặn với lý do, **Then** xử lý lượt mới nhất còn hiệu lực và cập nhật badge; không duyệt `SAI`, không duyệt lượt stale. Duyệt lời gia sư chỉ ghi đã xem, không bỏ cổng ADR 013.

### US5 — Lập lịch và nhận nhắc trong ứng dụng (P2, U13)

Học sinh chọn giờ rảnh, xem lịch tuần, hoàn thành phiên học và nhận nhắc khi mở app.

**Independent Test**: đồng hồ test cố định, lịch hôm nay và hạn nộp trong 24 giờ.

1. **Given** giờ rảnh hợp lệ, **When** tạo phiên học, **Then** lịch T2–CN × giờ hiện đúng theo Asia/Ho_Chi_Minh; server lưu thời điểm có múi giờ, kết thúc sau bắt đầu.
2. **Given** lịch/hạn trong 24 giờ, **When** mở app, **Then** thấy nhắc của mình; phiên đã hoàn thành không nhắc lại, không gửi email/push/Zalo.
3. **Given** bàn phím hoặc 390 px, **When** chọn buổi, **Then** đọc được nhãn ngày/giờ, mở đúng hoạt động, không cần kéo thả để hoàn tất tác vụ.

### US6 — Quản trị đúng phạm vi (P2, U14)

ADMIN quản lý tài khoản/lớp; SCHOOL_ADMIN thao tác trong phạm vi phân công; giáo viên chỉ cài lớp mình.

**Independent Test**: ADMIN, SCHOOL_ADMIN, TEACHER, STUDENT tổng hợp và hai lớp.

1. **Given** ADMIN, **When** tạo tài khoản/lớp và phân công giáo viên, **Then** vai trò hợp lệ, nhật ký có người/lúc/hành động.
2. **Given** SCHOOL_ADMIN, **When** thao tác ngoài phạm vi hoặc tạo tài khoản/lớp, **Then** server từ chối; giáo viên/học sinh không gọi được `/api/qt/**`.
3. **Given** bulk bật/tắt tài khoản, **When** tập người nhận có một người ngoài phạm vi hoặc chính người thao tác, **Then** từ chối toàn bộ, không cập nhật một phần; khóa hợp lệ thu hồi phiên.

### Edge Cases

- Không còn bài phát hành ở mức kế: báo chưa có bài phù hợp, không bịa bài hoặc vượt +1 mức; VDC báo hoàn thành kỹ năng.
- Cùng mã bài chỉ có một giao đang hiệu lực cho mỗi học sinh; giao lại cập nhật chế độ/hạn. Đọc lại server ở mỗi yêu cầu; không dùng mode client hay cache cũ để cấp quyền tutor.
- Tab gia sư đang mở khi giáo viên đổi sang kiểm tra: câu đang xử lý phải kiểm lại trước khi phát, bị hủy nếu mất quyền.
- Hết hạn tại ranh giới: `now >= dueAt` là hết hạn; `dueAt=null` không có hạn, `KIEM_TRA` vẫn cấm tutor.
- Hết phiên đăng nhập, sửa ID người/lớp, GET bằng URL đoán được: server kiểm token và thành viên cho mọi API, không tin ID người thao tác trong body.
- Nội dung Markdown/LaTeX độc hại: không HTML thô; tệp, nhãn và CSV không thực thi mã.

## Requirements

### Functional Requirements

| ID | Yêu cầu bắt buộc | Lát |
| --- | --- | --- |
| FR-100 | Gỡ v0 `apps/web` và đường chạy chỉ v0; giữ `data/v0` cho importer; demo chỉ frontend/core/math. | Nền |
| FR-101 | FLAT tuyệt đối, cổng `scripts/check-flat.mjs` và test ca hợp lệ/vi phạm; kiểm hình cho CSS động/hình ảnh. | U0–U1 |
| FR-102 | Giữ token MathL+, lưới 8 px, nút 40/chạm 44, focus outline 2 px; chữ ≥4,5:1, điều khiển ≥3:1; tiếng Việt đủ dấu. | U1 |
| FR-103 | Primitive rỗng/tải/lỗi/toast/dialog phẳng; toast gộp trùng 3/4/5 giây, lỗi quan trọng còn tại vùng tác vụ; focus trap/khóa cuộn/trả focus. | U2 |
| FR-104 | Một cấu hình điều hướng theo vai trò; sidebar 260 px desktop, bottom nav 4–5 mục dưới 64rem và safe-area; giữ testid/heading khóa. | U3 |
| FR-105 | Trang Học dùng mastery server, bài kế ưu tiên yếu/+1 mức; tải/rỗng/lỗi đầy đủ. | U4 |
| FR-106 | Kho theo lớp, KaTeX, phiên bản công thức khóa; flashcard bàn phím và CSV an toàn. | U5–U6 |
| FR-107 | Tutor hint-only ba cấp, xin đáp án bị từ chối; offline mặc định, không lời giải chuẩn, lọc lộ đáp án và cổng ADR 013 fail-closed. | U7 |
| FR-108 | Gia sư mở khi hỏi: cột phải desktop, bottom sheet ≤640 px, vùng đọc thích ứng ở giữa; SSE chỉ trạng thái rồi câu đủ sau lọc, aria-live không đọc lặp từng token. | U8 |
| FR-109 | Giao lớp/từng em, chế độ/hạn lưu server; test mode cấm mọi đường tutor, hạn server chặn ghi bài; UI phản ánh cùng dữ liệu. | U9 |
| FR-110 | Tổng quan có drill-down, tiến độ 4 mức/3 mức lúc xem, chi tiết và ghi đè có lý do/nhật ký. | U10 |
| FR-111 | Nhập bài hiện có chủ đề, tài liệu tự soạn và bảng công thức; lọc mức/kỹ năng/trạng thái; kiểm ba tầng/hash/stale. | U11 |
| FR-112 | Hàng chờ Duyệt/Chặn có lý do, không duyệt SAI/stale, badge đếm đúng phạm vi; không bỏ bộ lọc gia sư. | U12 |
| FR-113 | Lịch tuần/giờ rảnh/phương pháp theo luật; nhắc trong app của mình, không kênh ngoài. | U13 |
| FR-114 | Người dùng/lớp/phân công/bulk/kiểm toán theo quyền ADMIN/SCHOOL_ADMIN, không tự khóa, thu hồi phiên khi khóa. | U14 |
| FR-115 | Motion 120–200 ms cho chuyển trạng thái/tờ mở, tối đa 6–16 px; không nâng thẻ/3D, không animation thành công phô trương; reduced-motion tắt dịch chuyển/lặp. Chuyển động tự chạy dừng ≤5 giây hoặc có nút dừng. | Xuyên suốt |
| FR-116 | E2E một vòng và ca từ chối API, ảnh 390/1280/1440 sáng/tối, axe và bàn phím; số đo thật kèm SHA/lệnh/ngày. | U15 |

### Key Entities

- **Giao bài**: lớp, người nhận, bài đã phát hành, chế độ, hạn; một giao hiệu lực mỗi bài/học sinh.
- **Lượt làm**: học sinh, giao bài, năm bước, thời điểm nộp, kết quả; quyền/chế độ đọc từ server.
- **Nội dung lớp**: tài liệu tự soạn, công thức phiên bản, bài bốn mức; hash, kết quả ba tầng và trạng thái phát hành.
- **Lượt gia sư**: bước, cấp gợi ý, nguồn đã khóa, trạng thái và câu đã lọc; không chứa lời giải chuẩn.
- **Mức hiểu**: học sinh × kỹ năng, mức bốn bậc, ghi đè giáo viên và căn cứ.
- **Phiên học/nhắc**: chủ sở hữu, bắt đầu/kết thúc, trạng thái hoàn thành; nhắc tính từ lịch/hạn.
- **Nhật ký**: người thao tác, phạm vi, thời gian, hành động/lý do; không khóa API, không nội dung prompt chứa định danh.

## Success Criteria

| ID | Kết quả đo được | Cách đo |
| --- | --- | --- |
| SC-100 | Không đường chạy phụ thuộc v0; importer snapshot vẫn nạp lớp tổng hợp. | Rà tham chiếu thực thi và smoke compose của lane hiện thực |
| SC-101 | Không vi phạm FLAT; tương phản đạt ngưỡng hai theme. | Cổng FLAT, test scanner, script tương phản và ảnh |
| SC-102 | 100% ca test mode/hạn/sửa ID bị từ chối server, không gọi nhà AI hay phát câu. | Integration core + API Playwright, đồng hồ cố định |
| SC-103 | 0 đáp án/công thức sai lọt trong bộ ca nghiệm thu tutor; lỗi CAS đóng. | Bộ ca hiện có + ca fail-closed của core/math |
| SC-104 | CSV đọc lại đúng tiếng Việt/dấu đặc biệt, ô nguy hiểm không thành công thức; không rò nội dung khác lớp. | Contract export và fixture CSV |
| SC-105 | Vòng GV giao → HS ôn → mức → bài kế → lịch/nhắc chạy được; vòng kiểm tra cấm tutor. | Playwright U15 trên dịch vụ thật với dữ liệu tổng hợp |
| SC-106 | Các tác vụ US1–US6 hoàn tất ở 390/1280/1440 px sáng/tối; axe không có lỗi serious/critical, kiểm bàn phím/focus/reduced-motion đạt. | Axe + kiểm thủ công + ảnh có nhãn viewport/theme |
| SC-107 | Nội dung chặn/stale không tới HS; GV ghi đè/duyệt/bulk có quyền và nhật ký. | Testcontainers quyền hai lớp + e2e GV/quản trị |

Không đánh dấu đạt nếu chỉ có mock UI. Ngoài phạm vi: mạng xã hội/chat bạn bè, kho public, OCR, ma trận đề/engine thi chuẩn quốc gia, khóa học online, Word/PDF flashcard, mở rộng chủ đề hoặc adapter AI trả phí chưa được duyệt.
