# Implementation Plan: Hoàn thiện MathL+ v2

**Branch**: `002-hoan-thien-v2` (epic; phiên DOCS ở `lane/docs`) | **Date**: 2026-10-13 | **Spec**: [spec.md](spec.md)

## Summary

Hoàn thiện vòng học một chủ đề trên Angular + Spring Boot + dịch vụ toán. Gỡ đường chạy v0, áp FLAT, làm U0–U15 theo lát có API thật và nghiệm thu độc lập. Kế thừa [001](../001-lat-cat-doc/plan.md); hợp đồng hiện hành ở [API_UX](../../docs/API_UX.md), mặc định ở [ASSUMPTIONS](../../docs/ASSUMPTIONS.md). Đây là kế hoạch triển khai, không là báo cáo test hay xác nhận tất cả API đã hoạt động.

## Technical Context

- **Ngôn ngữ/nền tảng**: Angular 22, Spring Boot 4.1/Java 25, FastAPI/SymPy; PostgreSQL 18. Dùng phiên bản đang khóa trong repo, không nâng phụ thuộc trong lát này.
- **Kiến trúc**: frontend signal-first; core domain → application → infrastructure, ArchUnit; math thuần hàm/CAS tiến trình con. Không sao chép đáp án hoặc tính mức vào frontend.
- **Lưu trữ**: Flyway V1–V10 bất biến; không sửa hoặc dùng V11 trong epic này; chỉ thêm tên V12+ còn trống sau khi phối hợp lane core, không tạo hai migration cùng số. Không sửa dữ liệu sư phạm/kiểm định nếu thiếu bản vá nguyên văn có mã.
- **Nghiệm thu**: Vitest, Playwright, axe; JUnit/Testcontainers/ArchUnit; pytest chỉ khi đổi math. Browser 390/1280/1440 px, sáng/tối, bàn phím/reduced-motion. Demo offline và dữ liệu tổng hợp.
- **Phạm vi phiên DOCS**: chỉ docs/**, specs/**, .specify/**, DESIGN.md, labs/**/*.md, README.md. Không commit/push/deploy, không sửa .github/workflows hoặc bí mật. Task mã bên dưới là bàn giao cho lane hiện thực.
- **Giới hạn**: không token streaming trước kiểm; không tự retry POST/tutor hoặc đổi nhà. GET có thể thử lại tối đa một lần khi ≥500, không retry 401/403/400, giữ AbortController.

## Constitution Check

Bảng là cam kết thiết kế trước hiện thực, cần rà lại sau khi có bằng chứng; không ghi cổng thực thi đã đạt.

| # | Trả lời | Cách đáp ứng |
| --- | --- | --- |
| I — Không đưa đáp án | CÓ | FR-107–109: hint-only, không đọc lời giải, lọc CAS fail-closed, test mode cấm server |
| II — Đúng toán | CÓ | FR-106/111/112: ba tầng, hash/phiên bản/stale, SAI bị chặn, ADR 013 không bị bypass bởi duyệt |
| III — Trẻ vị thành niên | CÓ | Tổng hợp, offline; không social chat, không định danh sang nhà AI, kiểm quyền hai lớp; pilot thật theo ADR 012 |
| IV — Bằng chứng | CÓ | SC-100–107 có lệnh/bộ ca; ADR 014/015/016 và ghi chú quyết định liên kết |
| V — Tiếng Việt | CÓ | Copy đầy đủ dấu, lỗi có việc tiếp theo; không thuật ngữ kỹ thuật trên màn |
| VI — Ranh giới | CÓ | Core giữ mode/hạn/quyền/mastery; math giữ CAS; migration chỉ V12+ |
| VII — Thay đổi nhỏ | CÓ | Một lát/issue/PR, lead review, giữ testid; phiên này không commit |
| VIII — Truy cập được | CÓ | Focus outline, 44 px, axe/bàn phím/3 viewport, reduced-motion |

## Project Structure

```text
specs/002-hoan-thien-v2/{spec.md,plan.md,tasks.md}
docs/{ASSUMPTIONS.md,API_UX.md,DESIGN.md,QUY-TRINH.md}
docs/adr/{014-giao-dien-phang.md,017-che-do-kiem-tra-phia-server.md,016-go-v0.md}
labs/decisions/2026-10-13-flat-va-che-do-kiem-tra.md
apps/frontend/src/app/{core,shared,features}  # lane frontend
services/core/src/main/java/vn/hoctapcanman/core/  # lane core
services/core/src/main/resources/db/migration/  # chỉ V12+
services/math/  # giữ CAS; đổi chỉ khi hợp đồng thật sự cần
```

## Ranh giới API và dữ liệu

Không thay hợp đồng `specs/001-lat-cat-doc/contracts/math-v1.md`. Không tạo API song song khác với API_UX: bảng dưới là danh mục bàn giao, DTO/response chi tiết lấy từ tài liệu đó. Tất cả API xác thực JWT, người thao tác lấy từ token; core kiểm vai trò và phạm vi đối tượng trước truy vấn/ghi. Lỗi problem+json, message tiếng Việt; không trả stacktrace, lời giải chuẩn hoặc khóa. GET danh sách hiện theo Item[]; nếu thêm phân trang phải sửa hợp đồng và cả hai phía trong lát riêng.

| Nhóm/lát | Hợp đồng API_UX | Trách nhiệm server/tiêu chí |
| --- | --- | --- |
| Học U4 | `GET /api/hs/trang-hoc`, `/api/hs/goi-bai`, `/api/hs/bai` | Mastery hiện có; ưu tiên kỹ năng yếu, +1 mức, xử lý thiếu bài/VDC |
| Kho U5–U6 | `GET /api/hs/kho`, `/api/hs/flashcards/export` | Chỉ nội dung lớp được phép, bảng khóa; export `{filename,content}`, UTF-8 hai cột an toàn |
| Gia sư U7–U8 | `POST /api/hs/gia-su` `{maBai,maBuoc,cap,cauHoi}` | SSE `kho`, `goi`, `loc`, `xong`/`loi`; chỉ `xong` chứa câu đã lọc, không token thô. Hợp đồng này thay event `trang_thai` của v0 |
| Giao U9 | `GET/POST /api/gv/giao`, `GET /api/hs/che-do` | `{lopId,studentIds,problemIds,mode,dueAt,setName}`; rỗng studentIds = cả lớp; cập nhật một giao hiệu lực mỗi bài/em |
| Nộp U9 | `POST /api/hs/bai/{ma}/buoc`, `/nop` | Kiểm thành viên, giao hiện hành, hạn server; `now >= dueAt` từ chối ghi |
| GV U10 | `/api/gv/lop`, `/tong-quan`, `/tien-do`, `/hoc-sinh/{id}`, `POST /muc-hieu`, `/goi-bai` | Nhật ký ghi đè, reason bắt buộc; level4 nguồn chuẩn, level3 chỉ xem |
| Nội dung U11 | `GET/POST /api/gv/ngan-hang`, `GET /api/gv/kho`, `POST /tai-lieu`, `GET/POST /cong-thuc`, `POST /cong-thuc/kiem` | Tài liệu tự soạn ≤5 MiB, PDF chữ; bài đúng chủ đề; ba tầng, phiên bản và stale |
| Duyệt U12 | `GET /api/gv/duyet`, `POST /api/gv/duyet/{id}` | `{lopId,approve,reason}`; chỉ lượt mới nhất, không duyệt SAI/stale; tutor vẫn qua cổng |
| Cài lớp | `GET/PUT /api/gv/cai-dat` | Offline mặc định; không bật trả phí từ UI placeholder; lời giải sau nộp mặc định tắt |
| Lịch U13 | `GET/POST /api/hs/lich`, `POST /lich/{id}/xong`, `GET /api/hs/nhac` | ISO có timezone, kết thúc > bắt đầu; nhắc 24h trong app, chỉ chủ sở hữu |
| Quản trị U14 | `/api/qt/nguoi-dung`, `/nguoi-dung/bulk`, `/lop`, `/lop/phan-cong`, `/nhat-ky` | ADMIN tạo; SCHOOL_ADMIN chỉ phạm vi được giao; bulk toàn bộ hoặc không ghi, thu hồi phiên khi khóa |

### Chế độ kiểm tra và cạnh tranh yêu cầu

Chính sách chung nằm tại application service của core, được dùng bởi mọi adapter tutor và đường nộp bài. Thứ tự: xác thực → quyền/đối tượng → mode/hạn từ giao hiện hành → xử lý. Mode `KIEM_TRA` trả 403 problem+json trước khi mở SSE; lỗi không có dữ liệu gia sư. Hạn quá hạn trả 403 với mã lỗi phân biệt. Không có giao hợp lệ thì từ chối; nếu có luồng tự luyện riêng, phải tạo/lấy giao ôn server trước lượt hỏi, không để client tự chọn mode.

Một giao mỗi bài/em giải quyết bất định khi cùng mã bài thuộc ôn và kiểm tra. Khi giao lại, server cập nhật mode/hạn; tutor kiểm lại ngay trước khi phát `xong`, hủy kết quả nếu đổi chế độ/hết hạn. Việc ghi bước/nộp và đổi giao phải được tuần tự hóa bằng khóa/phiên bản giao dịch phù hợp để không ghi sau khi quyền đổi. Request đang chờ CAS không được giữ quyền tutor vô thời hạn. Test bằng latch/clock cố định, tránh test sleep phụ thuộc thời gian máy.

Kiểm tra này là bộ bài tự kiểm có hạn, chưa là engine thi chuẩn: không ma trận đề, giám sát thi, chống gian lận trình duyệt hay tự động mở lời giải sau giờ thi. Xem [ADR 017](../../docs/adr/017-che-do-kiem-tra-phia-server.md).

### UI, motion và nguồn tham chiếu

Giữ token/typography MathL+ trong DESIGN, bảng toán tối/Manim; không nhập palette LMS. Điều hướng tập trung học từ `fe/src/app/shared/components/navigation/sidebar.config.ts`; dialog học mẫu focus trap/scroll lock từ `shared/components/dialog/dialog.component.ts` và `services/body-scroll-lock.service.ts` trong repo tham chiếu. Nếu chép mã, lane hiện thực kiểm license và ghi SHA/path thật; đọc tham chiếu không đồng nghĩa cho phép chép.

Desktop sidebar 260 px, bottom nav dưới 64rem. Gia sư chỉ mở khi hỏi; desktop cột phải, ≤640 px bottom sheet, phần giữa thích ứng. Dialog thường ≤640 px thành bottom sheet, không bóng. Tab có mũi tên/Home/End, ARIA; flashcard đổi nội dung 2D, không rotateY/perspective. Motion ngắn 120–200 ms, dịch chuyển tối đa 6–16 px; không hover nâng thẻ; reduced-motion đổi tức thì, chỉ báo chờ tĩnh. Toast không là nơi duy nhất chứa lỗi.

## Thứ tự và phụ thuộc U0–U15

| Lát | Cổng riêng | Phụ thuộc |
| --- | --- | --- |
| Nền gỡ v0 | Rà runtime, importer, ADR 016 | Không |
| U0 cổng FLAT | Ca đúng/sai scanner | Không |
| U1 đồng bộ thiết kế | Scanner + tương phản + ảnh | U0 |
| U2 primitive | Focus/bàn phím/axe/trạng thái | U1 |
| U3 điều hướng | Vai trò + 390/1440 | U2 |
| U4 trang Học | Mastery API + e2e | U3, API mastery hiện có |
| U5 kho lớp | Quyền/phiên bản + KaTeX | U3, content core |
| U6 flashcard CSV | CSV round-trip/quyền/bàn phím | U5 (không lấy U6 trước U5 khi thiếu API) |
| U7 tutor core | Xin đáp án/fail-closed/SSE | Nội dung đã khóa, policy U9 trước khi bật tutor cho bài kiểm tra |
| U8 tutor UI | Hint-only, hủy/không replay, focus | U2, U7, U9 |
| U9 giao/kiểm tra | API trực tiếp/mode/hạn/quyền | Core classroom/content/practice |
| U10 tổng quan/tiến độ | Ghi đè/audit/drill-down | U3, mastery, quyền lớp |
| U11 soạn nội dung | Ba tầng/stale/tải tệp | U3, content, math |
| U12 duyệt | SAI/stale/quyền/lý do/badge | U11 |
| U13 lịch/nhắc | Giờ/ràng buộc/bàn phím | U4, U9 |
| U14 quản trị | Vai trò/bulk/thu hồi phiên/audit | U3, identity/classroom |
| U15 nghiệm thu | SC-100–107 | Tất cả lát được nghiệm thu |

MVP ưu tiên nền + U0–U9 + U13 vòng lõi. U10–U12/U14 là lát kế tiếp trong epic; không loại khỏi backlog theo ước tính ngân sách báo cáo ngoài. Trước mỗi lát rà mã đã có và test còn thiếu, không viết lại module đã đáp ứng. Mỗi nhóm tạo issue có tiêu chí; lead review trước commit/PR, chủ repo merge.

## Verification và bàn giao

| Vùng hiện thực | Lệnh/cách kiểm |
| --- | --- |
| Docs lane | `git diff --check`; kiểm link nội bộ và ID FR/SC/T; rà stale reference, phạm vi diff |
| FLAT | `node scripts/check-flat.mjs`; `node --test scripts/check-flat.test.mjs` |
| Tương phản | `node scripts/tuong-phan-token.mjs` |
| Frontend | `pnpm --filter frontend build`; `pnpm --filter frontend test`; `pnpm --filter frontend e2e` |
| Core | `(cd services/core && ./mvnw verify)` với JDK 25 và hạ tầng Testcontainers |
| Math nếu đổi | `pnpm test:math` và bộ eval tương ứng; không tự sửa fixture kiểm định |
| E2E thật | Compose v2/tài khoản tổng hợp; GV giao ôn → HS hỏi/chấm/nộp → mức/bài kế → lịch/nhắc; giao kiểm tra → API tutor 403, deadline 403; GV duyệt và ADMIN bulk |

Bằng chứng mỗi lát: SHA, ngày, lệnh, số ca pass/fail/skipped, ảnh viewport/theme, môi trường và hạn chế. Mock/ảnh không thay thế API thật. Thiếu JDK/Docker hay không chạy được cổng phải ghi còn lại, không đổi số đo lịch sử.

## Complexity Tracking

Không chủ động vi phạm hiến chương. ADR 014 và 016 ghi chỉ đạo đã có; ADR 017 là đề xuất chi tiết để lead/chủ repo rà trước hiện thực policy cạnh tranh. Không yêu cầu phê duyệt lại FLAT, gỡ v0 hoặc quyền Codex.
