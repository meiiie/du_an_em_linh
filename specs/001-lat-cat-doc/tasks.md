---
description: "Danh sách việc của epic P2 — lát cắt dọc một chủ đề"
---

# Tasks: P2 — Lát cắt dọc một chủ đề

**Input**: `specs/001-lat-cat-doc/` (spec, plan, research, data-model, contracts)
**Tests**: bắt buộc — spec có 10 tiêu chí đo được; mỗi câu chuyện có test độc lập.

**Định dạng**: `[ID] [P?] [Story] Mô tả` — `[P]` chạy song song được (khác file, không phụ thuộc). Đường dẫn gốc: `services/core/src/main/java/vn/hoctapcanman/core/` viết tắt `core/`; test core `services/core/src/test/java/vn/hoctapcanman/core/` viết tắt `core-test/`; `apps/frontend/src/app/` viết tắt `fe/`.

**Issue**: mỗi nhóm `### Issue` dưới đây thành một issue GitHub, một PR (hiến chương VII). Thứ tự merge theo phụ thuộc ghi ở cuối file.

---

## Phase 1: Setup

### Issue — Chuẩn bị phụ thuộc P2

- [ ] T001 Thêm Spring AI BOM 2.0.1 và starter OpenAI vào `services/core/pom.xml`; trong `application.yaml`: tắt thử lại `spring.ai.retry.max-attempts=0` (Spring AI 2.0 hiểu là số lần gửi lại, research R3) và tắt 6 tự cấu hình model (`spring.ai.model.*=none`) để core khởi động khi không có khóa
- [ ] T002 [P] Thêm `katex` ^0.16.22 và `mathlive` ^0.107.1 (cùng bản v0) vào `apps/frontend/package.json`; khai CSS KaTeX trong `angular.json`
- [ ] T003 [P] `compose.v2.yaml` và `.env.example`: biến `LLM_*`, `OPENROUTER_API_KEY`, `ZAI_API_KEY` cho core (trống = chỉ `offline`); không có khóa trong git

---

## Phase 2: Foundational (chặn mọi câu chuyện)

### Issue — Module `classroom`: lớp, ghi danh, cài lớp, quyền theo lớp

- [ ] T004 `services/core/src/main/resources/db/migration/V3__classroom.sql`: `classes`, `enrollments`, `class_settings`, `escalations` (data-model §classroom)
- [ ] T005 `core/classroom/` domain + application + persistence: lớp, ghi danh, cài lớp; port `ClassMembership` cho module khác kiểm quyền; port `CanhBaoGiaoVien` để tutor («gửi thầy cô», `NHO_GV`) và mastery (kẹt, `KET`) ghi cảnh báo
- [ ] T006 Seeder profile `dev`: lớp «12A1 thử» với An, Bình, Chi, giáo viên thử (`synthetic`)
- [ ] T007 [P] `core-test/classroom/`: Testcontainers + bộ ca phân quyền chép từ `apps/web/tests/e2e/phan-quyen-lop.spec.ts` (F-08)

### Issue — Client dịch vụ toán đóng mặc định

- [ ] T008 `core/shared/infrastructure/math/MathServiceClient.java`: `RestClient` tới `/v1/*`, hết giờ theo `contracts/math-v1.md`; lỗi / hết giờ / JSON hỏng → kết quả «không chấm được / không kiểm được»
- [ ] T009 [P] `core-test/shared/MathServiceClientTest.java`: máy chủ giả trả hết giờ, 500, JSON hỏng, 200 đúng → không bao giờ «đạt»

### Issue — Job `kiem_dong_cong_thuc` (services/math): kiểm dòng bảng công thức khi khóa

- [ ] T042b `services/math/app/dong_cong_thuc.py` + `routers.py` `POST /v1/kiem-dong-cong-thuc` theo `contracts/math-v1.md` (tầng 1 cho đẳng thức và định lí loại đã biết, tầng 2 tìm đoạn trong tài liệu được phép), dùng lại bộ nhận dạng của `verify.py`; bảng 6 dòng của v0 phải `DAT` cả 6
- [ ] T042c [P] pytest: quy tắc thương đổi dấu tử → `SAI`; «đồng biến ⇒ $y' > 0$» → `SAI` nhờ phản ví dụ $y = x^3$; loại máy chưa biết → `KHONG_KIEM_DUOC`; tài liệu `chua_ro` không làm căn cứ tầng 2; hết giờ → không `DAT`

### Issue — Lab Sư phạm: bản vá `sp-tai-lieu-0001` (tài liệu tự soạn cho quy tắc đạo hàm)

<!-- #84; sp-tai-lieu-0002 tách thành #103 sau lượt rà math-verifier của #101 -->

- [ ] T011c Lab Sư phạm soạn bản vá có mã `sp-tai-lieu-0001`: «Ghi chú tự soạn: quy tắc tính đạo hàm» (`tu_soan`, không chép sách) có đoạn phát biểu quy tắc lũy thừa, tổng, thương đúng như 3 dòng bảng của v0 (công thức và lời), làm căn cứ tầng 2 (ADR 013); rà bằng subagent `pedagogy-reviewer` và `math-verifier` trước khi phát hành
- [ ] T011e Lab Sư phạm soạn bản vá có mã `sp-tai-lieu-0002` (#103): «Ghi chú tự soạn: điểm tới hạn và dấu hiệu cực trị» có nguyên văn câu «Đạo hàm bằng 0 mà không đổi dấu thì chưa phải cực trị.» (dòng cực trị) và định nghĩa điểm tới hạn (dòng điểm tới hạn) của bảng v0; tầng 2 đòi mọi mệnh đề của dòng định lí có đoạn trích; rà độc lập bằng `math-verifier` và `pedagogy-reviewer`

### Issue — Module `content`: nhập nội dung chủ đề từ v0 và cổng 3 tầng cho bài

- [ ] T010 `V4__content.sql`: `topics`, `skills`, `skill_prerequisites`, `error_types`, `step_templates`, `problems`, `solutions`, `hint_levels`, `problem_releases`, `documents`, `document_passages`, `formula_sheets`, `formulas`, `hint_gate_results`, `verification_runs`, `verification_tier_results`, `content_reviews`
- [ ] T011 `core/content/domain/`: bài (chung) và trạng thái phát hành theo lớp (data-model §content), bảng công thức có phiên bản, kết quả kiểm gắn lớp và bảng đã dùng, có cờ «cũ»
- [ ] T011d Áp **nguyên văn** bản vá `sp-tai-lieu-0001` và `sp-tai-lieu-0002` vào `data/supham/` (kiểm SHA-256, ghi mã trong tiêu đề commit); importer nạp chúng như tài liệu thứ tư và thứ năm của lớp
- [ ] T011b Chép **nguyên văn** các hằng nội dung của `apps/web/scripts/seed.ts` (khung 5 bước, cấu hình BKT, 3 tài liệu, 6 dòng bảng công thức) ra `data/v0/*.json` kèm `data/v0/NGUON.md` (đường dẫn, dòng, SHA); không sửa chữ
- [x] T003b Đóng gói nội dung vào ảnh core (sau T011b, vì cần `data/v0/`): ngữ cảnh build là gốc repo, `services/core/Dockerfile.dockerignore` (chỉ `services/core/`, `data/supham/`, `data/v0/`), `COPY` vào `/app/noi-dung/{supham,v0}`; sửa `compose.v2.yaml` và job CI «Core — Maven + image» (`docker build -f services/core/Dockerfile .`)
- [ ] T012 `core/content/infrastructure/import/`: importer đọc `${app.content.source}` (ảnh: `/app/noi-dung`; test: `${project.basedir}/../../data` đặt trong `systemPropertyVariables` của Surefire), gồm `supham/` và `v0/`, như `apps/web/scripts/seed.ts` (cả biến thể `/v1/generate` hạt giống cố định và 3 bài demo), idempotent theo mã + dấu vân tay; với mỗi lớp đã có (profile `dev`: «12A1 thử»): nạp 3 tài liệu của v0, `sp-tai-lieu-0001` và `sp-tai-lieu-0002`, khóa bảng 6 dòng qua `/v1/kiem-dong-cong-thuc` (không đạt thì báo lỗi, không khóa thiếu; ADR 013), chạy `/v1/verify` từng bài và ghi `problem_releases` của lớp; phát sự kiện miền `BangCongThucDaKhoa` và `BaiDaNhap`
- [ ] T013 [P] Script một lần `specs/001-lat-cat-doc/doi-chieu/xuat-v0.ts` (chạy trên v0, với cùng 5 tài liệu của lớp v2: 3 của v0, `sp-tai-lieu-0001`, `sp-tai-lieu-0002`): xuất (mã bài, dấu vân tay, trạng thái cổng) ra `doi-chieu/v0-bai.json`
- [x] T014 (#135) `core-test/content/NhapNoiDungTest.java`: nhập trên Testcontainers + dịch vụ toán giả; so `v0-bai.json`
- [ ] T015 `core/content/application/`: port đọc bài cho học sinh **không** có lời giải; ArchUnit thêm luật: DTO của học sinh không phụ thuộc `Solution`

### Issue — Khung frontend P2

- [ ] T016 `fe/shared/layout/`: khung có thanh bên như v0 (`sidebar`, `mo-sidebar`, `dong-sidebar`, `nav-hs-*`, `nav-gv-*`); dưới `lg` là ngăn kéo
- [ ] T017 [P] `fe/shared/toan/`: `KatexComponent` (`throwOnError: false`), `MathFieldControl` (MathLive bọc control Signal Forms, nạp lười) + ô LaTeX dự phòng
- [ ] T018 [P] `fe/api/`: kiểu theo `contracts/api-core.md`; route `/hs/*`, `/gv/*` theo bảng phụ lục của spec, guard vai trò (#73)

**Checkpoint**: lớp, client toán, nội dung đã nhập, khung frontend — các câu chuyện bắt đầu được song song.

---

## Phase 3: User Story 1 — Học sinh làm bài theo bước (P1) 🎯 MVP

**Goal**: An làm một bài 5 bước, máy chấm từng bước, lưu bài làm dở. **Independent Test**: kịch bản spec US1.

### Issue — Module `practice` (core)

- [x] T019 (#136) [US1] `V7__practice.sql` (V5, V6 đã dùng cho content): `assignments`, `submissions`, `submission_steps`, `submission_tables`, `submission_table_cells`, `input_events`, `grading_results`
- [x] T020 [US1] `core/practice/`: nộp bước (idempotent), chấm qua `MathServiceClient.grade`, ghi kết quả; nộp bài; cờ mở lời giải; nghi đoán mò
- [ ] T021 [US1] `core/practice/infrastructure/web/`: `GET /api/hs/bai`, `GET /api/hs/bai/{maBai}`, `POST …/buoc`, `POST …/nop`, `POST /api/gv/giao-bai` theo hợp đồng; `GET /api/hs/trang-hoc` phần `ten`, `soBaiGiao` (mastery, planner bổ sung phần của mình)
- [ ] T022 [P] [US1] `core-test/practice/`: chấm sai không lộ đáp án; dịch vụ toán lỗi → `KHONG_CHAM_DUOC`; tải lại giữ bài làm; hai tab nộp cùng bước ghi một lần
- [x] T023 [P] [US1] `core-test/practice/DoiChieuChamV0Test.java`: chấm toàn ngân hàng với bài làm mẫu, so tệp vàng do script `specs/001-lat-cat-doc/doi-chieu/cham-v0.ts` chạy trên v0 xuất ra, như T013 (SC-006)

### Issue — Trang Học, Đề bài, Luyện (frontend)

- [ ] T024 [US1] `fe/features/hoc-sinh/trang-hoc/`: «Chào <tên>», phiếu việc tiếp, sổ «Bài giao» (`tab-bai-giao`, `bai-<mã>`)
- [ ] T025 [US1] `fe/features/hoc-sinh/de-bai/`: danh sách bài (`nav-hs-bai`)
- [ ] T026 [US1] `fe/features/hoc-sinh/luyen/`: phiếu 5 bước (`solve-screen`, `latex-txd`, `latex-dh`, `nop-buoc`, `cham-thong-bao`), bảng xét dấu (`k` 0-based), ô sai tô (`.cell-bad`)
- [ ] T027 [P] [US1] Vitest cho ba màn; e2e `apps/frontend/e2e/luyen.spec.ts` (390 + 1280): nộp sai, sửa, tải lại

---

## Phase 4: User Story 2 — Gia sư không đưa đáp án, cổng 3 tầng cho lời giảng (P1)

**Goal**: gia sư theo thang, luật xin đáp án, lọc lộ đáp án, cổng công thức, SSE trạng thái. **Independent Test**: kịch bản spec US2 với nhà giả.

### Issue — Lab Kiểm định: bản vá KD-0005 (bộ ca lời giảng) và KD-0006 (bộ AI 70 ca)

- [ ] T029 [US2] Lab Kiểm định soạn bản vá có mã KD-0005 (`.patch` + SHA-256): ≥ 100 câu lời gia sư, 8 loại (spec SC-004), mỗi câu có phán quyết mong đợi theo ADR 013, kèm bộ từ vựng nhận dạng quy tắc bằng lời (thuật ngữ + từ quan hệ); rà độc lập bằng subagent `math-verifier` trước khi phát hành bản vá
- [ ] T029c [US2] Lab Kiểm định định vị bộ AI 70 ca của v0 (`labs/evals/README.md` chưa ghi chỗ) và đưa ra dạng dữ liệu chạy được với `services/core` (đầu vào lượt gia sư + phán quyết mong đợi) thành bản vá có mã KD-0006 (`.patch` + SHA-256, ghi nguồn gốc). Không tìm được nguồn kiểm chứng được thì báo chủ repo; không soạn lại 70 ca rồi gọi là bộ của v0

### Issue — Job `kiem_loi_giang` (services/math), áp nguyên văn KD-0005

- [ ] T028 [US2] `services/math/app/loi_giang.py` + `routers.py` `POST /v1/kiem-loi-giang` theo `contracts/math-v1.md`; toán ngoài dấu phân cách không phân loại được → `KHONG_PHAN_TICH_DUOC`, bị bỏ; câu có thuật ngữ toán + từ quan hệ (từ vựng KD-0005) không khớp phát biểu dòng bảng → `QUY_TAC_BANG_LOI` không đạt, bỏ cả câu
- [ ] T029b [US2] Áp **nguyên văn** bản vá KD-0005 vào `services/math/kiemdinh/loi-giang/` (ghi SHA-256 trong PR và `NHAT-KY.md`); pytest chạy bộ ca trong cổng merge
- [ ] T030 [P] [US2] pytest: thế giới đóng (ADR 013): khớp bảng + trích dẫn → giữ; ngoài bảng → bỏ; sai → `SAI`; trích bài làm → giữ; kết quả cụ thể → bỏ; LaTeX hỏng → bỏ; quy tắc bằng lời ngoài bảng hay sai → bỏ cả câu; phát biểu trùng dòng bảng → giữ

### Issue — Module `tutor` (core)

- [ ] T031 [US2] `V<n>__tutor.sql`: `tutor_sessions`, `tutor_messages`, `llm_calls`
- [ ] T032 [US2] `core/tutor/domain/`: luật xin đáp án, sai chỗ, gợi ý (chép `apps/web/lib/tutor.ts`), thang 3 cấp, trạng thái lượt
- [ ] T033 [US2] `core/tutor/infrastructure/ai/`: nhà `offline` (`/v1/goi-y`), OpenAI-compatible, OpenRouter, Z.AI qua Spring AI (research R3); xóa định danh (chép `apps/web/lib/llm.ts`); chỉ tài khoản `synthetic` được gửi ra ngoài; test máy chủ giả: nhà trả 503 hay hết giờ → đúng 1 yêu cầu HTTP, không gửi lại
- [ ] T034 [US2] `core/tutor/application/LuotGiaSu`: kho lớp + trích dẫn `[n]` (chép `kien-thuc.ts`, `kho-lop.ts`) → nhà → `/v1/filter` → `/v1/kiem-loi-giang` → ghi mục duyệt cho biểu thức bị bỏ; câu thay thế chỉ từ gợi ý đã kiểm trước với phiên bản bảng hiện tại, không có thì câu cố định không chứa toán (ADR 013 mục 6)
- [ ] T034b [US2] `core/content/`: nghe `BangCongThucDaKhoa` và `BaiDaNhap`, chạy mọi câu gợi ý (thang của bài, thang mẫu đã điền tham số của đề) qua `/v1/kiem-loi-giang`, lưu `hint_gate_results` theo (gợi ý, phiên bản bảng); job lỗi thì không ghi `DAT`
- [ ] T035 [US2] `core/tutor/infrastructure/web/`: `POST /api/hs/gia-su` SSE (`trang_thai` kho/goi/loc, `xong`, `loi`), hủy khi kết nối đóng; `GET /api/hs/gia-su/{maBai}`; chip `GUI_THAY_CO` ghi cảnh báo `NHO_GV` qua `CanhBaoGiaoVien`; `GET /api/hs/kho`
- [ ] T036 [P] [US2] Nhà giả `gia-lap` (profile `test`, `e2e`) + `core-test/tutor/BoCaTest.java` `@Tag("bo-ca")`: bộ dụ đáp án, 288 ca ác ý, bộ ca lời giảng, bộ AI 70 ca (áp nguyên văn KD-0006) chạy qua `LuotGiaSu` (SC-003, SC-004, SC-005)
- [ ] T037 [P] [US2] `core-test/tutor/ThoiGianGiaSuTest.java`: trạng thái đầu ≤ 1 s; offline ≤ 3 s p95 trên 50 lượt (SC-007)

### Issue — Cột gia sư (frontend)

- [ ] T038 [US2] `fe/features/hoc-sinh/luyen/gia-su/`: cột phải, tờ toàn màn dưới `lg` (`tutor-panel`, `tutor-log`, `tutor-input`, `tutor-send`, `tutor-composer`, `dong-gia-su`, `chip-goi-y`, `tutor-che-do`); đọc SSE bằng `fetch` + `ReadableStream`; Dừng = `AbortController`; Escape; `aria-live` cho trạng thái
- [ ] T039 [US2] `fe/features/hoc-sinh/kho/`: trang «Công thức và tài liệu» (nhãn menu «Công thức», `nav-hs-kho`; đích `#ct-…`, `#tl-…`), badge `[n]` mở đúng đoạn không rời phiếu
- [ ] T040 [P] [US2] e2e `apps/frontend/e2e/gia-su.spec.ts`: bản tương đương `gia-su-harness` (composer, nhà lỗi không chuyển, kho) với nhà giả

---

## Phase 5: User Story 3 — Giáo viên chuẩn bị nội dung đã kiểm (P1)

### Issue — Tài liệu, bảng công thức, ngân hàng (core + frontend)

- [ ] T041 [US3] `core/content/`: tải PDF (≤ 10 MB) → trích chữ bằng Apache PDFBox 3.0.8 trong core (research R9) → đoạn có vị trí; quyền dùng bắt buộc; `chua_ro` không làm căn cứ
- [ ] T042 [US3] `core/content/`: bảng công thức nháp → khóa chỉ khi mọi dòng `DAT` ở tầng 1 và tầng 2 (ADR 013: đẳng thức kiểm tương đương; định lí loại đã biết kiểm ngữ nghĩa + tìm phản ví dụ), 422 kèm dòng chưa qua → phiên bản mới → đánh dấu «cũ» các kết quả kiểm trước → phát `BangCongThucDaKhoa`
- [ ] T043 [US3] API `GET/POST /api/gv/tai-lieu`, `GET/PUT /api/gv/cong-thuc`, `POST /api/gv/cong-thuc/khoa`, `GET /api/gv/ngan-hang`, `POST /api/gv/ngan-hang/kiem` (giao bài: `POST /api/gv/giao-bai` của practice, T021)
- [ ] T044 [US3] `fe/features/giao-vien/{tai-lieu,cong-thuc,ngan-hang}/` (giữ tiêu đề v0: «Tài liệu», «Công thức», «Đề bài»)
- [ ] T045 [P] [US3] Test: khóa bảng tạo phiên bản và «cũ»; tài liệu `chua_ro` bị bỏ qua; tài liệu vừa nạp có mặt trong kho lớp mà gia sư đọc (port của content); trích dẫn đầu–cuối do T061 kiểm (spec US3 kịch bản 5)

---

## Phase 6: User Story 4 — Giáo viên duyệt (P1)

### Issue — Hàng đợi duyệt (core + frontend)

- [ ] T046 [US4] `core/content/`: `GET /api/gv/duyet`, `POST /api/gv/duyet/{runId}` (ghi chú bắt buộc; chỉ bài `KHONG_KIEM_DUOC`; `SAI` → 409; run cũ, không phải run mới nhất của (lớp, bài), hay nội dung / bảng đã đổi → 409 «kiểm lại»; công thức trong lời gia sư → 422 «thêm vào bảng»); ghi người, thời điểm, lý do
- [ ] T047 [US4] `fe/features/giao-vien/duyet/` (`hang-doi`, `duyet-<mã>`)
- [ ] T048 [P] [US4] e2e `apps/frontend/e2e/duyet.spec.ts`: `KHONG_KIEM_DUOC` → `GV_DUYET` → tới An; `SAI` không có nút, An không thấy; run cũ chỉ có «Kiểm lại»; mục công thức gia sư chỉ có «Thêm vào bảng» (SC-002)

---

## Phase 7: User Story 5 — Mức hiểu và bài kế tới VDC (P2)

### Issue — Module `mastery` (core)

- [ ] T049 [US5] `V<n>__mastery.sql`: `mastery_config`, `mastery_states`, `mastery_events`, `mastery_overrides`, `next_problem_overrides`
- [ ] T050 [US5] `core/mastery/`: BKT chép `apps/web/lib/learning.ts` đúng tham số (research R7); ngưỡng 4 mức; mức Bloom lưu kèm; kẹt (ghi `KET` qua `CanhBaoGiaoVien`); hoàn thành kỹ năng / chủ đề
- [ ] T051 [US5] `core/mastery/`: bài kế (chép `de-hoc-sinh.ts`) với lý do; bất biến «không quá 1 nấc»; bổ sung `baiKe`, `soKyNang`, `hoanThanh` vào `GET /api/hs/trang-hoc`
- [ ] T052 [P] [US5] Script `specs/001-lat-cat-doc/doi-chieu/bkt-v0.ts` xuất tệp vàng; `core-test/mastery/DoiChieuBktV0Test.java` so từng bước
- [ ] T053 [P] [US5] `fe/features/hoc-sinh/trang-hoc/`: sổ «Kỹ năng» 4 mức bằng lời, lý do bài kế, báo hoàn thành
- [ ] T054 [P] [US5] e2e `apps/frontend/e2e/toi-vdc.spec.ts` (SC-010)

---

## Phase 8: User Story 6 — Lịch tuần (P3)

### Issue — Module `planner` + màn Lịch

- [ ] T055 [US6] `V<n>__planner.sql`; `core/planner/`: lịch tuần + lời khuyên (chép `lich.ts`, `counsel.ts`), nhắc trong app; `GET /api/hs/lich`; bổ sung `viecHomNay` vào `GET /api/hs/trang-hoc`
- [ ] T056 [US6] `fe/features/hoc-sinh/lich/` («Lịch học», `lich-tuan`, không tràn ở 390 px); nhắc hôm nay trên trang Học
- [ ] T057 [P] [US6] Test: lịch mặc định, kỹ năng kẹt, nhắc hôm nay

---

## Phase 9: User Story 7 — Giáo viên theo dõi (P3)

### Issue — Trang Lớp, Mức, Học sinh, Cài lớp, Gia sư (giáo viên)

- [ ] T058 [US7] API `GET /api/gv/lop`, `GET /api/gv/tien-do?muc=4|3`, `GET /api/gv/hoc-sinh/{id}`, `GET/PUT /api/gv/cai-dat`, `GET /api/gv/gia-su`
- [ ] T059 [US7] `fe/features/giao-vien/{lop,tien-do,hoc-sinh,cai-dat,gia-su}/` («Lớp 12A1 thử», `canh-bao-ket`, `san-sang-ai`, `tien-do`, `toggle-muc`, `mo-loi-giai`, `ai-provider-offline`)
- [ ] T060 [P] [US7] e2e: 4 mức / 3 mức, cảnh báo kẹt có tên Chi, giáo viên lớp khác bị từ chối
- [ ] T060b [US7] `core/mastery/`: ghi đè mức và bài kế (`mastery_overrides`, `next_problem_overrides` trong `V<n>__mastery.sql`), API `PUT/DELETE /api/gv/hoc-sinh/{id}/muc/{kyNang}`, `POST /api/gv/hoc-sinh/{id}/bai-ke`; màn học sinh của giáo viên có thao tác ghi đè
- [ ] T060c [P] [US7] Test: bài kế theo mức ghi đè; bài chọn tay hiện trước với lý do «thầy cô giao»; nhật ký người, thời điểm, lý do; gỡ ghi đè

---

## Phase 10: Polish — nghiệm thu lát cắt

### Issue — e2e «một vòng» và nghiệm thu P2

- [ ] T061 e2e `apps/frontend/e2e/mot-vong.spec.ts` (390 + 1280): GV nạp tài liệu, khóa bảng → An làm bài, hỏi gia sư, trích dẫn tài liệu vừa nạp → mức hiểu tăng → nâng 1 nấc → lịch (SC-001)
- [ ] T062 [P] Bản tương đương `luong-hoc-sinh` của v0 trên v2 (SC-008), đối chiếu bảng phụ lục
- [ ] T063 [P] Lab Thiết kế: ảnh 390 / 1280 mọi màn mới, `labs/design/audits/`
- [ ] T064 Cập nhật `docs/KIEM-THU.md` (số đo P2), `docs/CODEMAP.md`, `docs/product/LO-TRINH.md` (P2 xong)

---

## Phụ thuộc và thứ tự merge

```text
kiem_dong_cong_thuc (math) ─▶ content (nhập) ◀── bản vá sp-tai-lieu-0001, 0002 (lab Sư phạm)
Setup ─▶ classroom ─┬─▶ practice (US1) ─▶ Trang Học/Luyện ─┬─▶ tutor (US2) ─▶ Cột gia sư
math client ────────┤                                      │        ▲
content (nhập) ─────┘                                      │   kiem_loi_giang ◀── bản vá KD-0005 (lab Kiểm định)
Khung frontend ─────────────────────────────────────────── ┘
content + practice ─▶ Tài liệu/Công thức/Ngân hàng (US3) ─▶ Duyệt (US4) ◀── tutor (mục công thức gia sư)
Trang Học/Luyện ─▶ mastery (US5) ─▶ planner (US6)
practice + mastery ─▶ Giáo viên theo dõi (US7)
Tất cả ─▶ e2e «một vòng» và nghiệm thu
```

- MVP sau Phase 3: An làm bài theo bước trên v2 với nội dung đã nhập.
- Số `V<n>` của migration trong các việc là dự kiến. PR lấy số kế tiếp trên `main` khi rebase: Flyway mặc định `outOfOrder=false`, không áp migration có số nhỏ hơn số đã áp, nên mastery merge trước tutor thì mastery lấy `V6`.
- Song song được: `kiem_dong_cong_thuc` (math) với classroom và client toán (core); `kiem_loi_giang` (math) với `practice` (core); các màn giáo viên US3, US4, US7 với các màn học sinh.
- Kiểm trước câu gợi ý (T034b) nghe sự kiện miền, nên US2 và US3 merge theo thứ tự nào cũng được.
- ADR 013 đã chấp nhận (2026-10-02). Chặn: content chờ bản vá `sp-tai-lieu-0001` và `sp-tai-lieu-0002` của lab Sư phạm; job `kiem_loi_giang` chờ bản vá KD-0005; bộ ca của tutor (T036) chờ KD-0006 (bộ AI 70 ca). Hai bản vá sau thuộc issue lab Kiểm định.
