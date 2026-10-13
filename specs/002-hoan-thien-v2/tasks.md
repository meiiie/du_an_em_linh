---
description: "Nhiệm vụ T100+ hoàn thiện MathL+ v2 theo U0–U15"
---

# Tasks: Hoàn thiện MathL+ v2

**Input**: [spec.md](spec.md), [plan.md](plan.md), [API_UX](../../docs/API_UX.md).  
**Trạng thái**: danh sách bàn giao; mọi ô để trống tới khi có bằng chứng hiện thực. Tài liệu đã viết không chứng nhận task mã đã xong.  
**Format**: `[ID] [US] [U] Mô tả`; mỗi nhóm là một issue/PR Conventional Commits, lead review. Không tự chạy hoặc thay đổi mã ngoài lane DOCS của phiên này.

Đường `core/` = `services/core/src/main/java/vn/hoctapcanman/core/`; `core-test/` = `services/core/src/test/java/vn/hoctapcanman/core/`; `fe/` = `apps/frontend/src/app/`. Đường module/component mới bên dưới là nơi dự kiến, rà cấu trúc hiện có trước khi thêm. Migration chỉ thêm `services/core/src/main/resources/db/migration/V12+__*.sql` với số còn trống; V1–V10 bất biến, không dùng V11. Task migration chỉ chạy nếu rà schema cho thấy cần.

## Nền — gỡ v0 và tài liệu

- [ ] T100 Rà/gỡ runtime `apps/web` cùng script/image/cấu hình chỉ v0 trong lane root; giữ `data/v0`, importer core và dịch vụ toán; chứng minh SC-100. `.github/workflows` ngoài quyền phiên này: ghi phần cần chủ sở hữu xử lý nếu còn phụ thuộc v0.
- [ ] T101 Đồng bộ `docs/ASSUMPTIONS.md`, `docs/DESIGN.md`, `docs/QUY-TRINH.md`, `docs/product/LO-TRINH.md`, `docs/AI-HARNESS.md`, `README.md`; ghi lịch sử ADR thay vì xóa bằng chứng; lead rà ADR 014–016.
- [ ] T102 Rà `docs/API_UX.md` với controller/schema thực tế, chốt quyền/DTO/lỗi; đối chiếu V1–V10 và dành số V12+ trước khi thêm schema cho mode/lịch/audit; không sửa migration phát hành.

## US1 (P1) — U0–U4, FR-101–105/115

- [ ] T103 [US1] [U0] Hoàn thiện `scripts/check-flat.mjs` và `scripts/check-flat.test.mjs`: shadow/inset/token/utility, viền nhấn trái/inline-start, dải mép; có ca hợp lệ và vi phạm, báo tệp/dòng. Không sửa workflow trong phiên này.
- [ ] T104 [US1] [U1] Rà `apps/frontend/src/styles.css` và `fe/shared/`, `fe/features/`; đổi bóng/vạch mép thành nền/chữ/viền đều, kiểm token mark tối trên raise bằng `scripts/tuong-phan-token.mjs`; ghi ảnh/cổng SC-101.
- [ ] T105 [US1] [U2] Làm primitive tại `fe/shared/ui/`: TrangRong/KhungCho/LoiTai/ThongBaoNhanh/HopThoai; gộp toast, focus trap/trả focus, khóa cuộn đếm tham chiếu, bottom sheet ≤640 px; Vitest và axe cho tương tác.
- [ ] T106 [US1] [U3] Rà `fe/shared/layout/dieu-huong.ts`, `khung-trang.ts`; một nguồn menu theo vai trò, bottom nav/safe-area dưới 64rem, aria-current; giữ heading/testid khóa. E2E vai trò 390/1440.
- [ ] T107 [US1] [U4] Nối `fe/features/hoc-sinh/` với `/api/hs/trang-hoc`, `/goi-bai`; không tính mastery frontend; xử lý thiếu bài/VDC/tải/rỗng/lỗi và e2e SC-105.
- [ ] T108 [US1] [Motion] Rà `apps/frontend/src/styles.css`, `fe/shared/ui/` và màn dùng primitive: 120–200 ms, không 3D/hover nâng thẻ, reduced-motion tĩnh, auto motion ≤5 giây; kiểm focus/axe và ảnh sáng/tối.

## US3 (P1) — U5–U6, FR-106

- [ ] T109 [US3] [U5] Rà/hoàn thiện `core/content/` API `/api/hs/kho`, `/api/gv/kho`; chỉ thành viên đúng lớp và công thức khóa không stale; test `core-test/content/` với hai lớp.
- [ ] T110 [US3] [U5] Nối `fe/features/hoc-sinh/` trang Kho, tab Công thức/Tài liệu và KaTeX; rỗng/tải/lỗi, bàn phím và quyền nội dung.
- [ ] T111 [US3] [U6] API `/api/hs/flashcards/export` tại `core/content/`: UTF-8 hai cột thuật ngữ/định nghĩa, escape và chống spreadsheet injection; test dấu phẩy/nháy/xuống dòng/tiếng Việt/đầu ô nguy hiểm và chéo lớp trong `core-test/content/`.
- [ ] T112 [US3] [U6] Flashcard và tải CSV tại `fe/features/hoc-sinh/`; Enter/Space đổi mặt 2D, tên/mặt được đọc, hướng dẫn nhập Quizlet thủ công; test download đọc lại nội dung, không hứa PDF/Word/API write.

## US2 (P1) — U9 trước khi bật U7–U8, FR-107–109

- [ ] T113 [US2] [U9] Rà `core/practice/` và `core/classroom/`; bổ sung schema mode/hạn chỉ bằng migration V12+ nếu thiếu; triển khai `/api/gv/giao`, `/api/hs/che-do`, lớp/từng em, một giao hiệu lực mỗi bài/em, quyền và audit.
- [ ] T114 [US2] [U9] Policy application service dùng chung tại `core/practice/`/`core/tutor/`: mode/hạn từ DB; kiểm trước tutor/SSE và trước ghi bước/nộp; kiểm lại trước phát câu, tuần tự hóa đổi giao/ghi bài; lỗi 403 có mã phân biệt, không có provider call.
- [ ] T115 [US2] [U9] Integration `core-test/practice/`, `core-test/tutor/`: direct API, mode giả, không giao, hạn null/ranh giới/quá hạn, tab cũ, đổi chế độ lúc đang xử lý, quyền hai lớp; clock/latch tất định, assert không gọi nhà/không phát câu.
- [ ] T116 [US2] [U9] Màn giao bài `fe/features/giao-vien/` và luyện `fe/features/hoc-sinh/`: chế độ/hạn server, ẩn gia sư kiểm tra, giữ phần đã lưu khi hết hạn; Playwright ôn/kiểm tra và sửa đồng hồ client.
- [ ] T117 [US2] [U7] Rà/hoàn thiện `core/tutor/`: offline, luật xin đáp án, thang ba cấp, kho lớp, lọc `/v1/filter` và kiểm lời theo ADR 013; SSE API_UX chỉ câu đủ đã lọc, không đọc lời giải, không tự retry/fallback.
- [ ] T118 [US2] [U7] `core-test/tutor/`: xin đáp án, công thức ngoài bảng/stale, CAS lỗi/hết giờ/JSON hỏng, nguồn chéo lớp; chạy eval hiện có khi chạm prompt/bộ lọc; không tự sửa `services/math/kiemdinh/`.
- [ ] T119 [US2] [U8] Nối gia sư `fe/features/hoc-sinh/`: mở khi hỏi, cột desktop/bottom sheet mobile, ba cấp, hủy/không replay, aria-live, cuộn giữ chỗ/focus/IME; e2e hint-only và SSE ngắt.

## US4 (P2) — U10–U12, FR-110–112

- [ ] T120 [US4] [U10] `core/mastery/`, `core/classroom/`: `/api/gv/tong-quan`, `/tien-do`, `/hoc-sinh/{id}`, `/muc-hieu`, `/goi-bai`; quyền lớp và audit reason; test ghi đè, level3 chỉ xem.
- [ ] T121 [US4] [U10] `fe/features/giao-vien/`: KPI drill-down, chu kỳ 7/30/90 ngày chỉ khi server hỗ trợ hợp đồng tương ứng; bảng tiến độ và chi tiết, mobile danh sách; e2e ghi đè có lý do.
- [ ] T122 [US4] [U11] `core/content/`: ngân hàng/nhập bài đúng chủ đề, tài liệu tự soạn TXT/MD/PDF chữ ≤5 MiB, công thức nháp/kiểm/khóa; ba tầng/hash/stale; test sai/chờ/đạt/tệp scan/quá cỡ/quyền.
- [ ] T123 [US4] [U11] `fe/features/giao-vien/`: bảng ngân hàng lọc mức/kỹ năng/cổng, màn tài liệu/công thức và toolbar toán; không gọi nháp là đã phát hành; e2e nạp/khóa.
- [ ] T124 [US4] [U12] `core/content/`: hàng chờ `/api/gv/duyet` và Duyệt/Chặn reason; lượt mới nhất, không SAI/stale, audit; duyệt tutor chỉ xác nhận đã xem, không bỏ ADR 013; test song song/chéo lớp.
- [ ] T125 [US4] [U12] `fe/features/giao-vien/`, `fe/shared/layout/`: màn Duyệt, căn cứ/lý do và badge đúng phạm vi; e2e duyệt/chặn/badge và từ chối stale.

## US5 (P2) — U13, FR-113

- [ ] T126 [US5] [U13] `core/planner/`: `/api/hs/lich`, `/lich/{id}/xong`, `/nhac`; khung rảnh, giờ hợp lệ/timezone, phương pháp theo luật, nhắc 24h chỉ in-app; V12+ nếu thiếu schema; test clock cố định/quyền/hoàn thành.
- [ ] T127 [US5] [U13] `fe/features/hoc-sinh/`: bảng tuần T2–CN × giờ, tạo phiên không bắt kéo thả, nhắc khi mở; e2e ngày/giờ Asia/Ho_Chi_Minh và bàn phím 390 px.

## US6 (P2) — U14, FR-114

- [ ] T128 [US6] [U14] `core/identity/`, `core/classroom/`: API `/api/qt/**`, phạm vi ADMIN/SCHOOL_ADMIN, tạo/phân công, bulk nguyên tử không tự khóa, thu hồi phiên, nhật ký; test hai lớp và cả bốn vai trò.
- [ ] T129 [US6] [U14] `fe/features/quan-tri/`, `fe/shared/layout/`: bảng người dùng/lớp, bulk action/xác nhận và phân công, nhật ký; e2e bị từ chối ngoài quyền và phiên đã thu hồi.

## U15 — nghiệm thu và bàn giao, FR-116

- [ ] T130 [U15] Tại `apps/frontend/` bộ Playwright hiện có: vòng GV nạp/khóa/giao ôn → HS gợi ý/chấm/nộp → mastery/bài +1 → lịch/nhắc; thêm vòng tới VDC, thiếu bài và duyệt/chặn theo SC-103/105/107; gọi API thật.
- [ ] T131 [U15] Playwright kiểm tra: tutor 403 cả khi gọi trực tiếp/mode giả/tab cũ, hạn server chặn ghi, quyền hai lớp và quản trị; không lời giải/protected_facts trong response/SSE; CSV an toàn và lỗi CAS đóng.
- [ ] T132 [U15] Axe + bàn phím/focus/reduced-motion ở 390/1280/1440 px sáng/tối; ảnh có nhãn, rà bóng/dải màu/3D mà scanner không thấy; giữ «Chào An», «Lớp 12A1 thử», `/3 mức/`, «Vào học» và testid.
- [ ] T133 [U15] Chạy cổng vùng đã đổi theo plan; ghi `docs/KIEM-THU.md` với ngày/SHA/lệnh/số pass-fail-skipped và hạn chế. Lead review diff; phiên này kết thúc bằng done/remaining, không commit/deploy.

## Dependencies & Execution Order

T100–102 là nền. U0 → U1 → U2 → U3 → U4. U5 → U6; U9 policy là điều kiện để phát hành U7/U8, U7 → U8. U10 dùng mastery/quyền lớp; U11 → U12; U13 dùng giao bài và học; U14 dùng identity/classroom. U15 sau các lát được nghiệm thu. Không đánh dấu task xong chỉ vì spec đã mô tả hoặc có endpoint placeholder.

MVP: nền, U0–U9, U13 và phần U15 vòng lõi. Phần U10–U12/U14 vẫn cần issue/tiêu chí riêng. Không tự tạo PR, không commit trong phiên DOCS; nội dung code/test được bàn giao cho lane phù hợp.

## LANE SEED — trường THPT hư cấu (2026-10-13)

Phạm vi: `services/core/**`, tệp mới `data/**`, `services/math/tests/**`, `docs/SEED.md` và mục này. Không commit; không sửa V1–V12, không thêm seed vào migration production. Chỉ runner profile `dev`. Yêu cầu trực tiếp của chủ repo thay tài khoản/lớp thử trong seed; heading frontend/e2e nằm ngoài lane này.

- [x] T140 [US6] [U14] Thay seed tài khoản/lớp bằng Trường THPT Nguyễn Trãi hư cấu, 2026–2027, 10A1/11A2/12A3, 4 giáo viên, 30 học sinh, 1 ADMIN; mật khẩu chỉ đọc `MATHL_DEV_SEED_PASSWORD`; thiếu/trắng thì bỏ tạo và cảnh báo không bí mật. Kiểm unit trùng tài khoản, đủ roster, production không có runner.
- [x] T141 [US4] [U11] Thêm 44 bài tự soạn ở `data/dev/nguyen-trai`, 7 chương, đủ 4 mức, lời giải từng bước/đáp án/dữ kiện SymPy; ghi 7 tài liệu/lớp, bảng đạo hàm chỉ khóa qua CAS, bảng chương chưa hỗ trợ giữ nháp. Không sửa `data/supham`/kiểm định hoặc tự đặt cổng DAT.
- [ ] T142 [US2/US4/US5] [U9/U10/U12/U13] Hiện thực giao ôn/kiểm tra có hạn, lịch/nhắc, lịch sử 3 tuần có căn cứ CAS và BKT, tình huống chờ giáo viên; xác minh số lượng và snapshot idempotency trên PostgreSQL thật bằng `NguyenTraiSeedIntegrationTest`. Mã/test đã có; nghiệm thu PostgreSQL còn chờ quyền socket của môi trường.
- [x] T143 [U15] Chạy pytest mới, kiểm đáp án và bước độc lập bằng SymPy, grade payload lịch sử, khóa bảng và cổng phát hành; sửa mọi sai lệch đề/đáp án phát hiện khi review. Bằng chứng và giới hạn cổng tại `docs/SEED.md`.
- [ ] T144 [U15] Chạy đầy đủ `JAVA_HOME=$HOME/.local/jdk/jdk-25.0.4.1+1 ./mvnw -B -q verify`; core đã build/unit/ArchUnit qua khi cấp Mockito javaagent và bỏ riêng test HTTP dùng socket. Lệnh đầy đủ còn bị môi trường chặn test socket/Testcontainers, không coi là xanh toàn bộ.
- [x] T145 [U15] Viết `docs/SEED.md`: bật dev, tên biến mật khẩu (không giá trị), danh sách lớp/tài khoản, idempotency, số lượng mục tiêu và phần chưa có bằng chứng tích hợp; giữ diff đúng lane, không commit.
