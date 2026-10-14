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

## Bổ sung 2026-10-14 — phiên v2 và phát từng câu đã kiểm

Theo chỉ đạo hiện thực backend của chủ repo, bổ sung này **thay** phần thang dự phòng, luật từ chối cố định, `stream=false` và hủy bằng ngắt kết nối ở các phần trên. Các rào chắn ADR 003/013 giữ nguyên. Frontend chưa được chuyển trong thay đổi backend này.

- Namespace `/api/hs/gia-su/v2`: `POST /phien` nhận `giaoBaiId`; `GET /phien/{id}/luot` nhận `offset/limit`; `POST /phien/{id}/luot` nhận `requestId,maBuoc,cap,cauHoi`; `GET /phien/{id}/su-kien` nhận `lastSeq` hoặc `Last-Event-ID`; `POST /phien/{id}/cancel` (alias `/huy`) nhận `turnId`. Mọi đường kiểm học sinh đăng nhập và giao bài/lớp hiện hành. Request trùng không gọi model lại; payload khác cùng id hoặc hai writer là 409.
- Event `start → (delta|regen|heartbeat)* → done|error`. ID nghiệp vụ `<sessionId>:<seq>`, JSON có `v:2,sessionId,turnId,seq`. `delta` chỉ chứa nguyên câu đạt cả hai cổng, không token thô. `regen` 1–2 viết lại, 3 là câu hỏi nghiêm ngặt cuối. Heartbeat mỗi 15 giây, không cấp seq hay lưu DB. Cursor không khớp/vượt mốc trả 409 trước mở SSE; `Cache-Control: no-cache, no-transform` và `X-Accel-Buffering: no`.
- GLM trả SSE thật (`stream:true`, `thinking.type=disabled`); parser UTF-8 giữ chunk nội dung, bỏ reasoning/tool output, yêu cầu `finish_reason=stop` và `[DONE]`. Bộ đệm tìm ranh giới câu ngoài `$…$`, `$$…$$`, `\(…\)`, `\[…\]`. EOF thiếu tín hiệu kết thúc là lỗi; không coi bản nháp dở dang là lời trả lời. Không retry HTTP tự động, không gọi nhà thứ hai.
- Mặc định `glm-5.3` qua endpoint coding với `thinking: disabled` (đã kiểm chứng thực tế trong PR #161/#162); đổi bằng `GLM_MODEL`. Model offline chỉ được cấu hình ở profile `dev/test`, theo lựa chọn `offline` của lớp (kể cả khi GLM đã có khóa). Lớp chọn `glm` không chuyển sang offline khi thiếu khóa hoặc lỗi.
- V14 thêm `phien_gia_su`, `luot_gia_su` (message có role/content/tokens), `yeu_cau_gia_su` (cặp hỏi–đáp, trạng thái/lease và nhật ký event), `gia_su_usage`. FK ràng buộc student/assignment/class; một phiên mỗi student/assignment. DB cấp seq và commit câu đã kiểm trước khi gửi; khóa phiên cùng thao tác hủy, kiểm lại mode/hạn/quyền và phiên bản đề/bảng trước commit delta. Lịch sử phân trang đọc trong snapshot REPEATABLE READ.
- Worker độc lập với kết nối SSE. Ngắt mạng chỉ dừng đọc; resume đọc log đã commit, không sinh lại. POST hủy ghi terminal nguyên tử và ngắt worker đang chạy trong tiến trình; buffer chưa kiểm không vào lịch sử. Lease và deadline toàn lượt là 120 giây; lượt hết lease được ghi `error` khi đọc lại phiên. Restart không khôi phục bản nháp.
- `/api/hs/gia-su` giữ SSE tương thích, kể cả fetch không có header Accept của frontend hiện hành; JSON đồng bộ ở `/api/hs/gia-su/tra-loi`. Cả hai chạy cùng service phiên bền vững và cổng với v2. SSE cũ trả `goi → xong|loi`; frontend hiện hành chưa nhận hợp đồng v2. HTTP 429 có `Retry-After` theo cửa sổ giới hạn.
- Đồng hồ sử dụng: `GET /api/gv/lop/{cl}/gia-su/su-dung` hoặc `/api/qt/lop/{cl}/gia-su/su-dung` trả tổng lớp và từng học sinh. Giáo viên phải có quyền lớp, ADMIN được đọc; ghi mỗi lần sinh/regen/nén, gồm request lỗi. Không có thanh toán.

Bổ sung retry ngày 2026-10-14 thay quy định không retry HTTP tự động ở trên: đúng một lần cùng nhà, backoff 800 ms ±20% (mẫu neko-core), chỉ khi lỗi transport, HTTP 429/5xx hoặc stall trước token nội dung đầu tiên; không retry sau token, 4xx khác 429, hủy, hay phản hồi hỏng/tool-call. `GLM_FIRST_TOKEN_TIMEOUT=20s` tách khỏi `GLM_TIMEOUT=45s` mỗi attempt; child cancellation đóng stream riêng, không hủy cả lượt hay interrupt consumer, hủy backoff dừng ngay; tổng lời gọi kể cả backoff bị chặn ở 110s trong deadline lượt 120s.

### Bằng chứng backend

Mốc nền: `b9d50f32dce34eeb0f43cba0c552dcb9ab585828`; thay đổi để trong worktree, chưa commit. JDK dùng `JAVA_HOME=$HOME/.local/jdk/jdk-25.0.4.1+1`. Số đo cuối và giới hạn môi trường được ghi dưới đây. Không chạy 30 kịch bản GLM thật, không tuyên bố leaks/TTFT/regen trên nhà thật.

Ngày chạy: **2026-10-14**, nguồn core (361 tệp gồm Java, prompt, V14 và application.yaml; đường dẫn sắp thứ tự, ghép path/NUL/bytes/NUL): SHA-256 **`716262789c73984d0ea5272f4cc67b32bcc6e49f0099e2de1c6643504e95d9da`**. Không có SHA commit mới.

```bash
export JAVA_HOME="$HOME/.local/jdk/jdk-25.0.4.1+1"
cd services/core
./mvnw -B "-DargLine=-javaagent:$HOME/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar" verify
./mvnw -B "-DargLine=-javaagent:$HOME/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar" '-Dtest=*,!MathServiceClientTest' verify
```

| Kiểm | Kết quả thực |
| --- | --- |
| `verify` toàn bộ, có Mockito agent từ đầu JVM | Exit **1**: 556 ca, 0 assertion failure, **1 lỗi môi trường** tại `MathServiceClientTest.start:41` (`Socket Operation not permitted`), 195 skip. |
| `verify` loại đúng test không thể mở socket ở sandbox | Exit **0 / BUILD SUCCESS**: 555 ca, **360 đạt**, 0 failure/error, 195 skip; tạo jar thành công. |
| ArchUnit trong cả hai lượt | **54/54 đạt**: Clean 7, DDD 10, tự kiểm luật 36, NullMarked 1. |
| Các ca skip | **194** do Docker không khả dụng (socket daemon bị sandbox từ chối); **1** seed integration do chưa đặt `MATHL_SEED_INTEGRATION`. Chín ca Testcontainers phiên mới đã viết/biên dịch nhưng chưa chạy. |
| PostgreSQL kiểm SQL bổ sung, không mở socket | PostgreSQL **17.11**, `postgres --single -j`: chạy V1–V14 và PREPARE **29 câu SQL** của adapter phiên, **0 ERROR/FATAL**. Log tạm `/tmp/mathl-tutor-migrations.log`, `/tmp/mathl-tutor-queries.log`; không thay cho Testcontainers PostgreSQL 18/JDBC. |
| Kiểm diff | `git diff --check`: exit 0; không sửa V1–V13 hoặc `.github`. |

Sandbox không cho JVM tự attach Mockito, nên lệnh dùng `-javaagent` lúc khởi động; không thay mock maker hay bỏ assertion để làm xanh. Test HTTP mở socket được giữ nguyên trong repo, không đánh dấu skip/xóa. Full `verify` **chưa xanh trong môi trường này**; cần chạy lại với quyền mở socket và Docker. Không đọc `.env`, không in khóa; không commit.

## Bổ sung 2026-10-14 — delta trực tiếp theo quyết định Darren

Bổ sung này thay hợp đồng phát từng câu đã kiểm ở trên theo [quyết định Darren trong ADR 013](013-cong-ba-tang-cho-loi-gia-su.md#bổ-sung-2026-10-14--quyết-định-darren-bỏ-cổng-từng-câu-khỏi-luồng-trực-tiếp).

- Trình tự v2 là `start → (delta|heartbeat)* → done|error`; không có `regen`. `delta` mang `{text}` là chunk nội dung từ nhà, có thể cắt giữa câu, Markdown hoặc LaTeX, kể cả chỉ chứa khoảng trắng. Chunk tối đa 64 đơn vị UTF-16, không tách cặp surrogate; vòng đọc log SSE chờ 50 ms giữa các lần quét. Reasoning và tool output vẫn không được phát.
- Envelope `v:2,sessionId,turnId,seq` và ID `<sessionId>:<seq>` giữ nguyên. `seq` là cursor thứ tự và chống lặp duy nhất; không còn `sentenceIndex`/`lastSentenceIndex`. `done` mang `status,tokens,durationMs`; không gửi lời trả lời để ghi đè chunk đã phát.
- Lưu chunk trước gửi SSE, giữ kiểm quyền/mode/hạn/phiên bản và tranh chấp với cancel. Chunk đã commit còn trong lịch sử khi hủy/lỗi; resume đọc đúng log cũ, không gọi model lại. Heartbeat không lưu/không cấp seq/không tính TTFT. Worker, lease và thời hạn toàn lượt không thay đổi.
- Không có buffer câu, kiểm cổng từng câu, sinh lại hoặc strict/fast-path. Nén summary chạy bất đồng bộ sau `done` và không gọi cổng lời giảng. Usage tiếp tục ghi lời gọi model và nén; retry GLM vẫn tối đa một lần trước token nội dung đầu tiên theo quy định retry trên.
- Frontend nối đúng mọi chunk, giữ khoảng trắng và render được Markdown/LaTeX đang dở. TTFT tính lúc nhận delta đầu tiên, không chờ câu đủ hay EOF. Eval báo rò đáp án để quan sát, không dùng làm cổng đạt/trượt; bỏ số đo regen. Chưa có benchmark GLM thật cho thay đổi này.
