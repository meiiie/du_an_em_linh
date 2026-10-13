# Gia sư AI — hợp đồng an toàn v2

**Hiện hành từ 2026-10-13:** điều phối trong `services/core`, giao diện `apps/frontend`, lọc CAS trong `services/math`. Ứng dụng v0 đã gỡ theo [ADR 016](adr/016-go-v0.md). Hợp đồng REST/SSE ở [API_UX](API_UX.md); mặc định khách ở [ASSUMPTIONS](ASSUMPTIONS.md), nghiệm thu U7–U9 ở [spec 002](../specs/002-hoan-thien-v2/spec.md). Tài liệu này nêu hành vi phải đạt, không chứng nhận adapter trực tuyến hoặc cổng đã chạy.

## Một lượt gia sư

1. Core xác thực và kiểm thành viên, giao bài/mode/hạn từ server. Kiểm tra từ chối HTTP 403 trước mở SSE/truy hồi/gọi nhà; xem [ADR 017](adr/017-che-do-kiem-tra-phia-server.md).
2. Luật xin đáp án chạy trước; thang ba cấp gợi mở, không bottom-out, không làm hộ bước. Không nhận lời giải chuẩn hoặc `protected_facts` vào ngữ cảnh gia sư (ADR 003).
3. Truy hồi tài liệu/công thức được phép của lớp, bảng đã khóa và phiên bản hiện tại. Số nguồn `[n]` nhất quán trong ngữ cảnh, lời và đường mở tài liệu.
4. Nhà đã chọn xử lý: mặc định offline bằng thang gợi ý hiện có. Nhà trả phí chỉ bật sau tích hợp/duyệt ngân sách và dữ liệu; không coi nút chọn nhà là adapter đã hoạt động.
5. Lọc lộ đáp án `/v1/filter`, rồi cổng công thức/lời giảng theo ADR 013 trong dịch vụ toán. Không gửi token thô. CAS lỗi/hết giờ không cho câu chưa kiểm tới học sinh; chỉ dùng gợi ý đã kiểm với phiên bản hiện hành hoặc câu cố định không chứa toán.
6. Kiểm lại quyền mode/hạn trước phát câu. Đổi sang kiểm tra trong lúc xử lý hủy phát kết quả. Không tự gửi lại sau ngắt SSE/đổi tab/hết phiên.

`POST /api/hs/gia-su` nhận `{maBai,maBuoc,cap:1|2|3,cauHoi}`; event `kho`, `goi`, `loc`, `xong`/`loi` với `{status,message}`. Chỉ `xong` có câu hoàn chỉnh sau lọc. Đây là hợp đồng v2, thay event `trang_thai` và server action của nguyên mẫu.

## Nhà AI và khóa

Demo không khóa vẫn học được bằng `offline`. Các lựa chọn nhà chính thức theo ADR 007–009 là hướng tích hợp: ChatGPT, OpenRouter lập trình, Z.AI coding. Không device-OAuth Codex, không mượn cookie/tài khoản, không nhập URL nhà từ form. Việc tích hợp adapter cần xác minh endpoint/model/điều khoản tại lúc triển khai, không chép mặc định model của v0 làm phiên bản hiện hành.

Khóa chính thức do người quản lý lớp được ủy quyền cấp cho máy chủ, không vào frontend, log, prompt, repo hay response. Không đọc/ghi file bí mật trong phiên agent. Không gửi khóa một nhà sang nhà khác. Không tự fallback nhà, thử lại POST chat hoặc phát lại khi SSE lỗi. Người dùng chủ động gửi lại sau thông báo lỗi có hành động rõ.

Pilot dữ liệu thật phải đáp ứng hiến chương III và ADR 012; xóa định danh trước khi gửi ngoài. Ngân sách/bên thanh toán theo A03, không mặc định nhà phát triển hay học sinh chịu phí.

## Composer và lời gia sư

Mở khi học sinh hỏi «Cần gợi ý?». Cột phải desktop, bottom sheet ≤640 px, thích ứng ở giữa; FLAT theo ADR 014. Không mở gia sư ở bài kiểm tra. Textarea tối thiểu 44 px, Gửi/Dừng 44×44; Enter gửi, Shift+Enter xuống dòng, bỏ qua IME; Escape dừng/đóng và trả focus. Hủy bằng AbortController, bỏ kết quả cũ. Ô nhập không mất nháp khi lỗi.

SSE trạng thái qua aria-live, chỉ báo câu đủ đã kiểm, không đọc lặp token. Cuộn theo đáy khi đang ở đáy; kéo lên giữ chỗ và có «Xuống». «Hỏi lại» đưa câu vào ô, người dùng tự gửi. Motion/reduced-motion theo DESIGN, không bóng/viền nhấn/3D.

Lời dùng Markdown + KaTeX, không HTML thô; đoạn ngắn/công thức cuộn ngang, lỗi KaTeX báo gọn. Nguồn bấm xem đúng đoạn trên phiếu, «Mở công thức»/«Mở tài liệu» mới chuyển về kho lớp. Không đổ toàn bộ tài liệu hoặc dữ liệu lớp khác vào lời.

## Vị trí và kiểm chứng

| Vùng | Trách nhiệm |
| --- | --- |
| `services/core/.../tutor/` | Policy/gợi ý/kho/nhà/SSE, không lời giải chuẩn |
| `services/core/.../practice/`, `classroom/` | Giao bài, mode/hạn, quyền lớp |
| `services/math/app/` | Lọc lộ đáp án, kiểm công thức/lời, CAS sandbox |
| `apps/frontend/src/app/features/hoc-sinh/` | Composer, phiếu, KaTeX, nguồn và trạng thái |

Đường module core viết tắt từ `src/main/java/vn/hoctapcanman/core/`; rà file thật trước sửa. Không còn lệnh `pnpm --filter web` hoặc đường mã Next.js làm hướng dẫn hiện hành. Tham chiếu lịch sử nằm trong Git/ADR và bộ đối chiếu `specs/001-lat-cat-doc/doi-chieu/`.

Cổng: core `./mvnw verify`, frontend build/test/e2e; math và bộ eval khi chạm bộ lọc/prompt. Ca bắt buộc: xin đáp án, không làm hộ bước, công thức ngoài bảng/stale, CAS lỗi, hai lớp, direct tutor API ở kiểm tra/hết hạn/tab cũ, SSE ngắt không replay. Ghi lệnh/SHA/ngày/số ca thật; không dùng số đo v0 để khẳng định v2 đạt.
