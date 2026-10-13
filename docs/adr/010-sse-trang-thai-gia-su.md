# ADR 010 — SSE trạng thái gia sư, không stream token

## Bối cảnh

Cột gia sư chờ cả lượt (5–15 s FlashX) rồi mới hết «Đang nghĩ…». Dừng chỉ tăng `seq`, request vẫn chạy. Học sinh không biết đang mở kho, gọi nhà, hay lọc.

Tham khảo (không copy mã AGPL): [meiiie/wiii](https://github.com/meiiie/wiii) SSE event rồi câu đủ + cùng guardrail sync/stream; Open WebUI / NekoCore-OS `step` → `done`. Khóa ADR 007: không stream token vì lọc cần cả câu.

## Quyết định

- `POST /api/hs/gia-su` trả `text/event-stream`: `trang_thai` (`kho`|`goi`|`loc`) rồi `xong`.
- Cùng `chayHoiGiaSu` với server action — không lệch lọc.
- Dừng = `AbortController` trên fetch và trên `completeChat`. Escape cùng việc đó.
- Cột phải dính, nhật ký co, composer đáy. Không bong bóng gradient.
- SSE 401/lỗi/`!body`: trả chữ lỗi. Không fallback thầm sang server action (tránh gọi model lần hai).

## Hệ quả

Học sinh thấy bước đang làm. Câu chỉ hiện sau lọc. Token thinking của nhà không lên UI.

## Bổ sung ngày 2026-10-14 — GLM và thang dự phòng

Theo yêu cầu hiện thực của chủ repo, v2 gọi Z.ai Coding Plan qua cổng `HintModel` với adapter GLM; khóa trống dùng adapter offline. SSE giữ các tên sự kiện hiện hành `kho → goi → loc → xong|loi`, mỗi sự kiện mang `{status,message}`. `kho` đọc đề, bảng công thức đã khóa và bài làm của em; `goi` bao trùm lời gọi mô hình hoặc luật từ chối; `loc` bắt đầu trước bộ lọc lộ đáp án và cổng lời giảng ADR 013. Chỉ `xong` hoặc `loi` có lời nhắn để hiển thị.

Không stream token GLM: các token có thể chứa kết quả hoặc công thức chưa kiểm. Core phải nhận trọn bản nháp rồi kiểm duyệt đủ hai cổng trước khi gửi câu. `thinking.type=disabled`, `stream=false`, tối đa 350 token; prompt tiếng Việt ở `prompts/gia-su.vi.txt`, không có trường lời giải hay dữ kiện bảo vệ. Bản nháp quá ba câu hoặc dẫn mã `[n]` ngoài bảng chuyển về thang; thang cũng phải đạt cùng điều kiện trước khi qua cổng. Luật xin đáp án/đổi vai không gọi nhà.

Chỉ retry 429/500/502/503/504, tối đa một lần cùng nhà, chờ `Retry-After` trong ngân sách `GLM_TIMEOUT` của toàn lời gọi. Không retry timeout/lỗi khóa. Nhà lỗi, hết giờ hoặc cổng chặn thì kiểm thang gợi ý bằng cùng cổng; thang cũng không đạt thì phát câu cố định không chứa toán, không phát bản nháp. Đây là fallback tường minh được chủ repo yêu cầu, không fallback sang nhà thứ hai.

Cấp quyền và giới hạn mỗi học sinh trước khi mở SSE; vượt giới hạn là HTTP 429. Kiểm lại quyền lớp, mode và hạn trước khi phát kết quả. Hủy/đóng SSE ngắt luồng gọi, adapter hủy tác vụ đang chờ. SSE có hạn 120 giây để đủ các job có timeout riêng; không thay hạn của GLM/CAS. Không có schema hoặc migration mới. Log chỉ chứa id đã băm, mã bài, cấp, nhà, độ trễ và kết quả; không chứa khóa, câu hỏi hay bản nháp.
