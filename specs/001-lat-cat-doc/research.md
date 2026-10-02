# Research — P2 lát cắt dọc

Mỗi mục: **Quyết định**, **Lý do**, **Phương án đã cân nhắc**. Nguồn trong repo ghi đường dẫn; nguồn ngoài ghi ngày tra.

## R1. Chia module trong `services/core`

- **Quyết định:** 6 module mới, mỗi module đủ `domain` → `application` → `infrastructure`: `classroom`, `content`, `practice`, `tutor`, `mastery`, `planner`. Module gọi nhau qua port ở `application` (ví dụ `tutor` hỏi `practice` «bước đang sai của bài làm này»), không đọc repository của module khác.
- **Lý do:** khớp các khối của sơ đồ (C1–C10) và các nhóm bảng của v0 (`apps/web/lib/db/schema.ts`, 37 bảng). Mỗi module tách được thành issue và PR riêng (hiến chương VII). ArchUnit đã kiểm tầng cho mọi gói con.
- **Đã cân nhắc:** một module «hoc-tap» gộp hết (PR quá lớn, ranh giới mờ); một module cho mỗi bảng (nghiệp vụ rải rác, gọi chéo nhiều).

## R2. Gọi `services/math` từ `services/core`

- **Quyết định:** một client duy nhất `MathServiceClient` ở `shared/infrastructure`, dùng `RestClient` của Spring, giữ đúng payload `/v1/*` của v0 (khóa snake_case tiếng Việt). Hết giờ theo v0: chấm 12 s, kiểm định 20 s, lọc 12 s, gợi ý 12 s. Mọi lỗi mạng, hết giờ, mã khác 2xx hoặc JSON hỏng đều đổi thành kết quả «không chấm được / không kiểm được», không bao giờ thành «đạt».
- **Lý do:** hiến chương II (lỗi gọi dịch vụ toán không bao giờ là đạt); hợp đồng `/v1` giữ nguyên ở pha đầu (ADR 011).
- **Đã cân nhắc:** thêm thư viện circuit breaker (chưa cần với một dịch vụ, một lớp); gọi thẳng SymPy từ Java (không có, và ADR 011 loại).

## R3. Nhà AI cho gia sư

- **Quyết định:** Spring AI 2.0.1 (`ChatClient`, OpenAI-compatible), cấu hình bằng biến môi trường như v0 (`LLM_*`, `OPENROUTER_API_KEY`, `ZAI_API_KEY`). Địa chỉ OpenRouter và Z.AI cứng trong mã như v0. Tắt thử lại bằng `spring.ai.retry.max-attempts=0`: Spring AI 2.0.1 đưa giá trị này vào `RetryPolicy.maxRetries` của Spring Framework 7.0.9, tức số lần **gửi lại** sau lần đầu (đọc bytecode `SpringAiRetryAutoConfiguration`, 2026-10-02), nên `1` vẫn gửi lại một lần. Tắt tự cấu hình model của starter (`spring.ai.model.{chat,embedding,image,audio.speech,audio.transcription,moderation}=none`; cả 6 mặc định bật và đòi khóa lúc khởi động); nhà dựng bằng mã cho từng nhà. Hết giờ 30 s, hết giờ 30 s, `max_tokens` 1600. Z.AI giữ `thinking.enabled` + `reasoning_effort: low` và chỉ đọc `content`. Nhà `offline` viết lại bằng Java: câu mẫu + thang gợi ý từ `/v1/goi-y`. Nhà máy cục bộ (Ollama, LM Studio) chỉ bật khi `app.tutor.allow-local=true`, chỉ địa chỉ loopback (`docs/AI-HARNESS.md`: «Local = loopback»). Loopback trong container là chính container, nên hai nhà này chỉ dùng được khi core chạy thẳng trên máy (`./mvnw spring-boot:run`). `compose.v2.yaml` để `allow-local=false`: core không đưa hai nhà vào `cacNhaDuocBat`, và `PUT /api/gv/cai-dat` trả 422 khi `choPhepMayCucBo=true`. Không nới sang `host.docker.internal` hay mạng LAN.
- **Lý do:** ADR 011 chọn Spring AI; Spring AI 2.0.1 dựng trên Spring Boot 4.1.1 (đúng bản của core; tra Maven Central 2026-10-02: starter OpenAI 2.0.1 kéo `spring-boot-starter-restclient` 4.1.1). Các luật an toàn của `docs/AI-HARNESS.md` (không fallback thầm, không phát lại, khóa theo nhà) giữ nguyên.
- **Đã cân nhắc:** LangChain4j (ADR 011 đã chọn Spring AI); gọi HTTP tay bằng `RestClient` (giữ làm đường lui nếu Spring AI thiếu tính năng, theo «điều làm quyết định này sai» của ADR 011).

## R4. Trạng thái lượt gia sư (SSE)

- **Quyết định:** `POST /api/hs/gia-su` trả `text/event-stream` bằng `SseEmitter`, chạy trên luồng ảo (đã bật). Sự kiện giữ như ADR 010: `trang_thai` với `kho` → `goi` → `loc`, rồi `xong` mang câu đã kiểm. Frontend đọc bằng `fetch` + `ReadableStream` (cần POST và header `Authorization`, nên không dùng `EventSource`). Dừng = `AbortController`; phía core hủy tác vụ khi kết nối đóng. Gặp 401 thì gọi `Phien.lamMoi()` một lần rồi gửi lại **trước khi** lượt bắt đầu; lỗi giữa lượt thì báo lỗi, không gửi lại.
- **Lý do:** ADR 010 (không stream token vì lọc cần cả câu); ADR 007 (không phát lại).
- **Đã cân nhắc:** WebSocket (thừa cho một chiều); trả JSON đồng bộ (mất trạng thái, học sinh chờ mù 5–15 s).

## R5. Công thức trong lời giảng (FR-015)

- **Quyết định:** theo [ADR 013](../../docs/adr/013-cong-ba-tang-cho-loi-gia-su.md) (chủ repo chấp nhận 2026-10-02, phương án A). Tóm tắt: lời gia sư chỉ được dùng công thức tổng quát có trong bảng đã khóa của lớp, đã máy kiểm lúc khóa, và có đoạn trích dẫn trong tài liệu được phép. Biểu thức trích nguyên văn đề bài, hoặc bài làm của học sinh (trình bày là lời của học sinh), được phép. Mọi biểu thức khác, kể cả toán viết trần không phân loại được, bị bỏ khỏi câu; nếu câu mất nghĩa thì thay bằng gợi ý đã qua cùng cổng từ trước. Mỗi lần bỏ ghi một mục `KHONG_KIEM_DUOC` hoặc `SAI` cho giáo viên. Phần kiểm chạy trong job mới `kiem_loi_giang` của `services/math`, ngay sau `/v1/filter`; lúc khóa bảng dùng job mới `kiem_dong_cong_thuc`.
- **Lý do:** sơ đồ đặt cổng 3 tầng trên mũi tên vào «học cùng AI»; hiến chương II; gia sư không được tính hộ (hiến chương I) nên chặn mọi kết quả tính cụ thể không mất gì về sư phạm.
- **Đã cân nhắc:** xem ADR 013 (đủ 3 tầng cho mọi công thức tùy ý; dán nhãn «chưa kiểm»; chỉ máy kiểm).

## R6. Nhập nội dung chủ đề từ v0

- **Quyết định:** importer trong module `content` đọc đúng các file nguồn v0 dùng (`data/supham/danh-muc-ky-nang-DH.json`, `ma-loi-DH.csv`, `03-vi-du-bai-tap.json`, `bai-khung-ngan.seed-v01.json`), dựng lại tập bài như `apps/web/scripts/seed.ts` (kể cả biến thể sinh qua `/v1/generate` với hạt giống cố định và các bài demo `DH12-NB-01`, `DH12-TH-02`, `DH12-DEMO-CHAN-01`), rồi chạy `/v1/verify` cho từng bài. Chạy lặp lại không nhân bản (khóa theo mã bài + dấu vân tay nội dung). Chạy ở profile `dev` và trong e2e. Một test đối chiếu danh sách (mã bài, dấu vân tay, trạng thái cổng) với tệp vàng xuất một lần từ v0.
- **Nguồn chuẩn cho phần v0 viết thẳng trong mã:** `apps/web/scripts/seed.ts` (dòng 515–634) tạo khung 5 bước, cấu hình BKT, 3 tài liệu và 6 dòng bảng công thức bằng hằng trong mã, rồi đưa tài liệu và bảng đó vào mọi lần `/v1/verify`. Chép **nguyên văn** các hằng này ra `data/v0/` (`khung-buoc.json`, `bkt.json`, `tai-lieu.json`, `bang-cong-thuc.json`) kèm `data/v0/NGUON.md` ghi đường dẫn, dòng, SHA của `seed.ts`; importer đọc từ đó, không viết lại nội dung trong Java. Ba tài liệu của v0 không phát biểu quy tắc lũy thừa, tổng, thương (3 dòng đầu của bảng), nên tầng 2 không đạt và bảng không khóa được theo ADR 013. Lab Sư phạm bổ sung một tài liệu tự soạn bằng bản vá `sp-tai-lieu-0001`. Tầng 2 còn đòi mọi mệnh đề của dòng định lí có đoạn trích (rà `math-verifier` trên #101), mà v0 không phát biểu «y' = 0 mà không đổi dấu thì chưa phải cực trị» và không định nghĩa điểm tới hạn, nên lab thêm `sp-tai-lieu-0002` (#103); lớp có 5 tài liệu. Test so trạng thái cổng với tệp vàng của v0 (T014) bắt mọi lệch; tệp vàng xuất bằng cổng của v0 chạy với cùng 4 tài liệu (T013), để hai bên cùng đầu vào.
- **Đóng gói:** ảnh Docker của core build với ngữ cảnh là gốc repo (như `apps/frontend`), `services/core/Dockerfile.dockerignore` chỉ cho `services/core/`, `data/supham/`, `data/v0/` vào; `COPY data/supham /app/noi-dung/supham`, `COPY data/v0 /app/noi-dung/v0` (chỉ đọc); importer đọc `app.content.source` (mặc định `/app/noi-dung`; test Maven đặt `${project.basedir}/../../data` qua `systemPropertyVariables` của Surefire, nên không phụ thuộc thư mục đang đứng). Không phụ thuộc bố cục thư mục của máy chủ lúc chạy.
- **Lý do:** dữ liệu sư phạm thuộc lab, chỉ có một nguồn; tiêu chí SC-006 (chấm trùng v0 100 %) cần cùng tập bài.
- **Đã cân nhắc:** chụp bảng từ CSDL v0 thành SQL (bản thứ hai của dữ liệu lab, lệch dần); nhập bằng Flyway (nội dung giáo viên sửa được, không hợp với migration chỉ thêm).

## R7. Mức hiểu và bài kế

- **Quyết định:** chép BKT của v0 (`apps/web/lib/learning.ts`) sang module `mastery` với đúng tham số: `p_t` 0,12, `p_g` 0,2, `p_s` 0,1, ngưỡng Thông hiểu 0,4 / Vận dụng 0,62 / Vận dụng cao 0,82, ngưỡng tin cậy mã lỗi 0,65, kẹt sau 3 lượt, nghi đoán mò sau 4 lần đổi ô. Tham số lưu trong `mastery_config` có phiên bản. Chép luật chọn bài kế (`de-hoc-sinh.ts`, gợi bài theo kỹ năng yếu, cùng mức, lên mức) và thêm bất biến «không nhảy quá 1 nấc». Một script Node chạy các hàm của v0 trên kịch bản dựng sẵn, xuất tệp vàng; test Java phải ra đúng các số đó.
- **Lý do:** cùng hành vi với v0 là điều kiện để gỡ v0; tệp vàng biến «chép đúng» thành điều đo được.
- **Đã cân nhắc:** mô hình mới (IRT, DKT) — để P4 khi có dữ liệu nhiều chủ đề.

## R8. Nhập và hiện công thức trên Angular

- **Quyết định:** MathLive 0.107 (`<math-field>`) bọc thành control của Signal Forms; luôn kèm ô LaTeX dự phòng giữ `data-testid` của v0 (`latex-txd`, `latex-dh`…). Nạp MathLive lười, chỉ ở trang luyện. KaTeX 0.16 với `throwOnError: false` (công thức hỏng hiện chữ mờ, như v0).
- **Lý do:** cùng bản thư viện v0 đã kiểm trên điện thoại; skill `angular-frontend`.
- **Đã cân nhắc:** chỉ ô LaTeX (khó với học sinh trên điện thoại); MathQuill (ngừng phát triển).

## R9. Tài liệu lớp

- **Quyết định:** giáo viên tải PDF (tối đa 10 MB) → core trích chữ ngay trong tiến trình bằng Apache PDFBox 3.0.8 (tra Maven Central 2026-10-02) → lưu văn bản và các đoạn có vị trí (trang, ký tự đầu, ký tự cuối). Không dùng `/v1/extract`: job đó đọc đường dẫn tệp cục bộ, mà core và math chạy ở hai container không chung ổ. Trích chữ không cần SymPy nên không thuộc `services/math`. Bắt buộc khai quyền dùng; tài liệu `chua_ro` không làm căn cứ tầng 2 và không được trích dẫn. Tìm đoạn bằng cụm từ đã bỏ dấu như v0 (`apps/web/lib/kien-thuc.ts`), không vector.
- **Lý do:** ADR 005 và ADR 008 của v0; OCR ảnh là P3.
- **Đã cân nhắc:** pgvector (ADR 008 đã loại cho quy mô này); gửi PDF dạng base64 sang `/v1/extract` (JSON tới 13 MB qua sandbox, thêm một đường lỗi mà không được gì); ổ chung giữa hai container (ràng buộc triển khai).

## R10. Quyền

- **Quyết định:** mỗi use case kiểm ghi danh: giáo viên chỉ thao tác trên lớp mình dạy, học sinh chỉ đọc và ghi dữ liệu của mình. Chép bộ ca F-08 của v0 (`apps/web/tests/e2e/phan-quyen-lop.spec.ts`) thành test. Nhà AI ngoài chỉ nhận lượt của tài khoản `synthetic` cho tới khi ADR 012 được duyệt. RLS của PostgreSQL làm sau, theo ADR 012.
- **Lý do:** hiến chương III; ADR 012 còn «Đề xuất», chưa chốt cách bật RLS cho đường xác thực.
- **Đã cân nhắc:** bật RLS ngay (ADR 012 chưa duyệt, dễ phải làm lại).

## R11. Cấu trúc frontend

- **Quyết định:** `features/hoc-sinh/*`, `features/giao-vien/*`; đọc bằng `httpResource`, ghi bằng `HttpClient` qua interceptor sẵn có; trạng thái trong signal. Khung có thanh bên, giữ `mo-sidebar`, `dong-sidebar`, `nav-*`, `sidebar` của v0. Màn và chữ chép từ v0 đã qua lab Thiết kế; ảnh 390 / 1280 cho mỗi màn mới vào `labs/design/audits/`.
- **Lý do:** skill `angular-frontend`; giữ `data-testid` để e2e đối chiếu.
- **Đã cân nhắc:** thư viện quản lý trạng thái riêng (thừa với signal).

## R12. Chiến lược kiểm

- **Quyết định:**
  - unit thuần cho domain mỗi module; Testcontainers PostgreSQL 18 cho persistence; ArchUnit cho mọi gói mới;
  - client dịch vụ toán: test hợp đồng với máy chủ giả trả các mẫu lỗi (hết giờ, 500, JSON hỏng) để chứng minh «không bao giờ đạt»;
  - nhà AI giả `gia-lap` chỉ bật ở profile `test` và `e2e`, trả câu theo kịch bản: dùng để chạy bộ dụ đáp án, bộ ác ý 288 ca và bộ ca lời giảng mới qua đúng luồng của core;
  - các bộ của cổng merge `services/math` (pytest, nghiệm thu Sư phạm 80 ca, bộ AI 70 ca, thang gợi ý qua bộ lọc) giữ nguyên (`docs/KIEM-THU.md`); bộ ca lời giảng mới vào qua bản vá có mã KD-0005 của lab Kiểm định, áp nguyên văn;
  - e2e Playwright trên compose v2: «một vòng», «duyệt», «tới VDC», bản tương đương `luong-hoc-sinh` và `gia-su-harness`, phân quyền lớp; 390 + 1280 px.
- **Lý do:** mỗi tiêu chí SC có lệnh đo (hiến chương IV); nhà giả làm e2e tất định, không cần khóa thật.
- **Đã cân nhắc:** chạy e2e với nhà thật (tốn khóa, không tất định, dữ liệu rời hệ thống).
