# Data model — P2 lát cắt dọc

Bảng PostgreSQL 18 của `services/core`, Flyway chỉ thêm. Tên bảng và cột tiếng Anh như `V1__identity.sql`; giá trị trạng thái giữ mã của v0 (`DAT`, `SAI`, `KHONG_KIEM_DUOC`, `GV_DUYET`…) để đối chiếu được. Nguồn: `apps/web/lib/db/schema.ts` của v0. Bỏ các cột chỉ phục vụ khóa do giáo viên dán (`ai_api_key`, `ai_openai_*`).

## classroom (`V3__classroom.sql`)

| Bảng | Cột chính | Ghi chú |
| --- | --- | --- |
| `classes` | `id`, `name`, `grade`, `school_year` | «12A1 thử» |
| `enrollments` | `class_id`, `user_id`, `role_in_class` (`TEACHER`, `STUDENT`) | PK (`class_id`, `user_id`) |
| `class_settings` | `class_id`, `reveal_solution_after_submit` (mặc định `false`), `ai_provider` (mặc định `offline`), `ai_allow_local` | Nhà AI chỉ chọn trong danh sách máy chủ bật |
| `escalations` | `id`, `class_id`, `student_id`, `skill_code`, `problem_code`, `step_code`, `kind` (`KET`, `NHO_GV`), `reason`, `created_at`, `handled_at`, `handled_by` | Cảnh báo cho giáo viên của lớp: kẹt (mastery ghi) và «gửi thầy cô» (tutor ghi), qua port `CanhBaoGiaoVien`. Tham chiếu bài và kỹ năng bằng mã, không khóa ngoại sang `V4` (bảng nằm ở `V3`, module khác) |

## content (`V4__content.sql`)

| Bảng | Cột chính | Ghi chú |
| --- | --- | --- |
| `topics` | `code`, `name`, `grade` | `DH12`: mã chủ đề của v0, để đối chiếu với `data/v0` |
| `skills` | `code`, `topic_code`, `name`, `description`, `grade`, `is_core` | 11 kỹ năng của danh mục lab: 7 kỹ năng `T12.DH.01`…`07` và 4 kỹ năng tiên quyết ngoài chủ đề. `description` là YCCĐ trích nguyên văn, như v0 |
| `skill_prerequisites` | `skill_code`, `prerequisite_code`, `min_level` | |
| `error_types` | `code`, `skill_code`, `step_code`, `name`, `fix_hint`, `result_types` | Từ `ma-loi-DH.csv`. `step_code` không có khóa ngoại: CSV có bước của dạng khác ngoài khung 5 bước (`B.DH.DOCBANG`, `B.DH.THAMSO`…) |
| `step_templates` | `step_code`, `topic_code`, `ordinal`, `input_kind`, `skill_code`, `description` | 5 bước `B.DH.*` |
| `problems` | `id`, `code`, `skill_code`, `extra_skill_codes`, `level4`, `level3`, `bloom_level`, `difficulty`, `statement_text`, `statement_latex`, `function_sympy`, `answer_form` (`TU_LUAN_5_BUOC`), `start_step`, `origin`, `content_hash`, `created_by`, `created_at`, `updated_at` | Nội dung chung của chủ đề; trạng thái phát hành nằm ở `problem_releases` theo lớp. `content_hash` phủ mọi đầu vào của `/v1/verify` và mọi thứ học sinh thấy: đề (chữ, LaTeX), `function_sympy`, `answer_form`, `start_step`, lời giải và dữ kiện bảo vệ, thang gợi ý. Đối chiếu tệp vàng của v0 (T014) dùng hash kiểu v0 (`{de, bl, hints}`) tính riêng trong test. Mức Bloom của lab Sư phạm 6 mức; importer quy đổi hai mã khác của v0: `APPLY` (của `/v1/generate`) → `VAN_DUNG`, `NHAN_BIET` (bài demo `DH12-DEMO-CHAN-01`) → `NHO` (chờ lab Sư phạm xác nhận) |
| `solutions` | `problem_id`, `worked_solution`, `protected_facts`, `final_answer` | **Không bao giờ** vào DTO của học sinh khi đang làm, không vào prompt. Mọi trường mang đáp án của lab (`dap_an_noi_bo`, `loi_giai_tung_buoc`, `tieu_chi_cham`) chỉ được nhập vào bảng này |
| `hint_levels` | `problem_id`, `step_code`, `level` (1–3), `text` | Thang đã kiểm |
| `hint_gate_results` | `problem_id`, `step_code`, `result_kind` (loại kết quả hoặc `chung`), `level`, `text` (câu đã điền tham số của đề), `formula_sheet_id`, `status`, `checked_at` | Câu thay thế lúc chạy chỉ lấy gợi ý `DAT` với phiên bản bảng hiện tại (ADR 013 mục 6) |
| `documents` | `id`, `class_id`, `code`, `title`, `kind`, `source`, `license_status`, `file_ref`, `text_content`, `version`, `uploaded_by`, `created_at` | `license_status = chua_ro` → không làm căn cứ. `code` là mã ổn định của tài liệu nhập (`v0-don-dieu`, `sp-tai-lieu-0001`…), duy nhất trong lớp; tài liệu giáo viên tải lên để trống |
| `document_passages` | `id`, `document_id`, `page`, `char_start`, `char_end`, `text`, `text_folded` | Cho tầng 2 và trích dẫn `[n]` |
| `formula_sheets` | `id`, `class_id`, `version`, `status` (`NHAP`, `KHOA`), `note`, `fingerprint`, `locked_at`, `locked_by`, `created_at` | Mỗi lần khóa = phiên bản mới; bảng đang dùng là bảng `KHOA` mới nhất của lớp; mỗi lớp nhiều nhất một bảng nháp. `locked_by` trống khi importer khóa bảng của v0. Bảng `KHOA` không đổi được nữa (trigger): không sửa hay thêm dòng, trích dẫn, không mở khóa; domain còn so `fingerprint` với các dòng khi nạp lại. Lưu bảng khóa theo thứ tự: bảng `NHAP` và dòng, rồi đổi sang `KHOA` |
| `formulas` | `id`, `formula_sheet_id`, `ordinal`, `code`, `skill_code`, `title`, `latex`, `statement`, `kind`, `tier1_status`, `tier2_status`, `tier1_detail`, `tier2_detail`, `citation_passage_id`, `checked_fingerprint` | Khóa được bảng chỉ khi **mọi dòng** có `tier1_status = DAT` và `tier2_status = DAT` (có trích dẫn); `SAI` hay `KHONG_KIEM_DUOC` chặn khóa (ADR 013). `code` là mã dòng ổn định (`d-1`… của v0), job trả kết quả theo mã. Kết quả kiểm gắn dấu vân tay của đúng dòng đã gửi job (`checked_fingerprint`): dòng đổi sau khi kiểm thì kết quả bỏ; domain không dựng được dòng mang kết quả của nội dung cũ, trigger chặn sửa nội dung mà giữ kết quả. Căn cứ đầy đủ của từng tầng (`can_cu`, phản ví dụ) ở `tier*_detail`; trích dẫn chính ở `citation_passage_id`, phải thuộc tài liệu cùng lớp |
| `formula_citations` | `formula_id`, `passage_id` | Đoạn trích thêm của tầng 2 (`trich_dan_them`): dòng định lí cần một đoạn cho mỗi mệnh đề. Khóa ngoại giữ tài liệu không bị xóa khi còn là căn cứ; đoạn phải thuộc tài liệu cùng lớp với bảng. Khóa ngoại tới đoạn kiểm lúc commit để xóa lớp xóa dây chuyền được. Domain: `Formula.extraCitationPassageIds` |
| `verification_runs` | `id`, `class_id`, `subject_kind` (`PROBLEM`, `TUTOR_FORMULA`), `subject_id`, `content_hash`, `formula_sheet_id` (trống khi lớp chưa có bảng khóa), `overall_status`, `publish_status`, `stale`, `created_at` | Tầng 2, tầng 3 dùng tài liệu và bảng của lớp, nên run gắn lớp và đúng bảng đã dùng; bảng phải là bảng `KHOA` của cùng lớp (khóa ngoại ba cột với cột hằng `formula_sheet_status`). `publish_status` luôn suy từ `overall_status` (CHECK); lượt `TUTOR_FORMULA` không có `publish_status`, không `GV_DUYET`, trạng thái tổng cũng phải khớp các tầng. Chỉ thêm: chỉ được đánh dấu `stale` hoặc duyệt `KHONG_KIEM_DUOC` → `GV_DUYET` khi chưa cũ (trigger). «Mới nhất» xếp theo (`created_at`, `id`) |
| `problem_releases` | `class_id`, `problem_id`, `status`, `run_id`, `updated_at` | PK (`class_id`, `problem_id`). `status` dùng đúng mã phát hành của v0: `NHAP`, `DA_PHAT_HANH`, `BI_CHAN`, `CHO_GIAO_VIEN_DUYET`; học sinh chỉ thấy bài `DA_PHAT_HANH` của lớp mình. `NHAP` không gắn run; trạng thái khác bằng `publish_status` của một run bài của đúng lớp và bài (khóa ngoại bốn cột, `ON UPDATE CASCADE`: duyệt run thì trạng thái theo sang). Domain chỉ áp run còn mới (như điều kiện duyệt) |
| `verification_tier_results` | `run_id`, `tier` (1–3), `status`, `result_type`, `wrong_steps`, `error_code`, `confidence`, `reason`, `citation`, `raw` | Căn cứ từng tầng |
| `verification_run_citations` | `run_id`, `passage_id` | Đoạn tài liệu mà lượt kiểm trích dẫn (tầng 2), quan hệ thay cho chỗ chỉ nằm trong `verification_tier_results.citation` (V5, #120). Đoạn đang được trích dẫn (ở đây, ở `formulas`, `formula_citations`) không đổi vị trí, chữ, tài liệu; tài liệu của nó không đổi lớp, quyền dùng: sửa thì nạp phiên bản mới và kiểm lại |
| `problems.content_version`, `verification_runs.content_version` | phiên bản nội dung của bài | V5 (#120): mỗi thay đổi thật của đề, lời giải, thang gợi ý tăng phiên bản (khóa dòng bài), đánh dấu mọi lượt kiểm của bài là cũ, đưa phát hành ở mọi lớp về `NHAP`. Lượt kiểm bài ghi phiên bản đã kiểm; ghi lượt kiểm hay gắn phát hành khóa dòng bài `FOR SHARE` và đòi phiên bản khớp, lượt còn mới. Domain `VerificationRun` mang trường này ở phần 3b |
| `content_reviews` | `id`, `run_id`, `content_hash`, `reviewer_id`, `decision` (`GV_DUYET`), `note`, `at` | Bắt buộc `note`; chỉ cho run `subject_kind = PROBLEM` (công thức trong lời gia sư không duyệt riêng, ADR 013). Một run duyệt một lần; `content_hash` phải bằng hash của run. Lượt `GV_DUYET` và bản ghi duyệt đi đôi, kiểm lúc commit (khóa ngoại tới `(id, content_hash, overall_status = GV_DUYET)` và trigger). Là nhật ký: không sửa, không xóa; run (và lớp) có bản ghi duyệt thì không xóa được; hạn giữ theo ADR 012 (#60) |

**Hai lớp trạng thái, giữ mã của v0** (`services/math/app/verify.py`, `cong_phat_hanh`):

- kết quả kiểm (`verification_runs.overall_status`, từng tầng): `DAT`, `SAI`, `KHONG_KIEM_DUOC`, sau duyệt là `GV_DUYET`;
- trạng thái phát hành (`verification_runs.publish_status`, chép sang `problem_releases.status` của lớp): `DA_PHAT_HANH`, `BI_CHAN`, `CHO_GIAO_VIEN_DUYET`.

Core tính trạng thái tổng từ các tầng như `cong_phat_hanh`: có tầng `SAI` thì `SAI`, còn tầng `KHONG_KIEM_DUOC` thì `KHONG_KIEM_DUOC`, mọi tầng `DAT` thì `DAT`. Trên 27 tổ hợp đủ ba tầng, core trùng v0 (test so với tệp vàng xuất từ `cong_phat_hanh`, `doi-chieu/cong-phat-hanh-v0.py`). Thiếu tầng nào trong 1–3: v0 phát hành nếu các tầng có mặt đều `DAT`; core coi tầng thiếu là `KHONG_KIEM_DUOC`, không tự phát hành và không cho giáo viên duyệt (lý do `INCOMPLETE`, phải kiểm lại), như v0 từ chối duyệt khi thiếu tầng. Trạng thái tổng của run nạp lại phải khớp các tầng (domain), trạng thái phát hành khớp trạng thái tổng (domain và CHECK): không có tổ hợp lệch.

Bài, kỹ năng, khung bước, thang gợi ý là nội dung chung của chủ đề. Tài liệu, bảng công thức, kết quả kiểm và trạng thái phát hành tính **theo lớp**: một run kiểm bằng tài liệu của lớp A không phát hành bài cho lớp B.

```text
NHAP ──/v1/verify──▶ DA_PHAT_HANH            (mọi tầng DAT)
               ├──▶ BI_CHAN                  (một tầng SAI) ──sửa nội dung──▶ NHAP
               └──▶ CHO_GIAO_VIEN_DUYET      (còn KHONG_KIEM_DUOC) ──GV duyệt (note, GV_DUYET)──▶ DA_PHAT_HANH
Đổi bảng công thức: kết quả kiểm cũ → stale = true; bài DA_PHAT_HANH giữ nguyên, hiện «cần kiểm lại».
GV duyệt chỉ trên run đủ ba tầng, mới nhất của (lớp, bài), không stale, content_hash và formula_sheet_id khớp hiện tại;
ngược lại 409 và phải kiểm lại, để không phát hành bằng phán quyết thiếu căn cứ hay của nội dung, bảng đã cũ.
Áp một run vào problem_releases cũng chỉ khi run còn mới như vậy.
```

## practice (`V7__practice.sql`, `V8__nop_bai_co_can_cu.sql`)

Số V của các migration sau content là số kế tiếp lúc merge: V5, V6 đã dùng cho content (#120, #121), practice là V7 (#87); tutor, mastery, planner lấy số kế tiếp khi làm.

| Bảng | Cột chính | Ghi chú |
| --- | --- | --- |
| `assignments` | `id`, `class_id`, `problem_id`, `student_id`, `status` (`DA_GIAO`, `DA_HUY`), `set_name`, `due_at`, `assigned_by`, `assigned_at` | Giao cho lớp = một dòng mỗi học sinh, duy nhất theo (lớp, bài, học sinh); chỉ giao bài `DA_PHAT_HANH` của chính lớp đó cho học sinh của lớp (trigger) |
| `submissions` | `id`, `class_id`, `student_id`, `problem_id`, `content_version`, `status` (`DANG_LAM`, `DA_NOP`), `guess_suspected`, `guess_reason`, `result`, `result_grading_id`, `skill_code`, `level4` (V8), `started_at`, `submitted_at` | Một bài làm đang làm mỗi (học sinh, lớp, bài, phiên bản nội dung) (chỉ mục duy nhất từng phần): nội dung đổi thì bài làm dở cũ thôi được chấm; mở chỉ khi bài đã phát hành cho lớp, đúng phiên bản nội dung hiện tại; nộp rồi thì bài làm và mọi phần con không đổi; lớp quyết định tài liệu, bảng công thức, cài đặt gia sư dùng cho bài làm. `result_grading_id` là căn cứ của kết quả nộp: có khi và chỉ khi `DA_NOP`, là lần chấm có phán quyết của chính bài làm với cùng `result` (khóa ngoại nhiều cột tới `grading_results`); đó là lần chấm bước kết luận trên nội dung lúc nộp do `NopBaiUseCase` bảo đảm, CSDL không kiểm `step_code`; màn giáo viên đọc lỗi từng bước của bài đã nộp qua đây, kể cả sau khi đề đổi. `skill_code`, `level4`: kỹ năng và mức của bài lúc nộp, có khi và chỉ khi `DA_NOP` (bản chụp, không khóa ngoại): sửa hai trường này ở bài không đổi `content_hash` nên không tăng phiên bản, bài đã nộp vẫn mang phân loại nó được chấm theo |
| `submission_steps` | `submission_id`, `step_code`, `line_no`, `latex`, `line_kind` | Nội dung mới nhất của bước kiểu dòng; nộp lại cùng bước thì thay trọn. `line_kind` là nhãn `loai` của v0 (`NGHIEM`, `KHONG_XD`, `DONG_BIEN`…) |
| `submission_tables`, `submission_table_cells` | bảng: `table_kind`; ô: `ordinal` (thứ tự gửi), `row_code`, `k` (0-based, `docs/chi-so-o-bang.md`), `value` | Giữ thứ tự ô để dựng lại đúng payload chấm của v0 |
| `input_events` | `submission_id`, `step_code`, `cell_row`, `cell_k`, `old_value`, `new_value`, `at` | Cho nghi đoán mò; chỉ thêm |
| `grading_results` | `submission_id`, `step_code` (bước nộp tới), `request_hash`, `result`, `result_type`, `wrong_steps`, `error_code`, `confidence`, `per_step`, `message`, `issues`, `math_ok`, `unfinished`, `normalizer_version`, `normalization`, `graded_at` | Chỉ thêm. Một lần chấm có phán quyết mỗi (bài làm, `request_hash` = SHA-256 của payload `/v1/grade`): hai tab nộp cùng bước ghi một lần. Lỗi dịch vụ toán → `result = KHONG_CHAM_DUOC`, không phán quyết nào, không bao giờ `DAT`, không chặn lần chấm lại. Chuẩn hóa (`chuan_hoa` của v0) gắn với lần chấm, không với dòng |

## tutor (`V<n>__tutor.sql`)

| Bảng | Cột chính | Ghi chú |
| --- | --- | --- |
| `tutor_sessions` | `id`, `class_id`, `student_id`, `problem_id`, `state`, `hint_levels` (theo bước), `answer_requests`, `answer_requests_step`, `same_error_repeats`, `last_step`, `started_at` | Một phiên mỗi (học sinh, lớp, bài); kho lớp, bảng công thức và nhà AI lấy theo lớp |
| `tutor_messages` | `id`, `session_id`, `role`, `content`, `blocked_by_filter`, `formula_verdicts`, `citations`, `provider`, `created_at` | Chỉ lưu câu đã kiểm; không lưu phần «suy nghĩ» của nhà |
| `llm_calls` | `id`, `purpose`, `provider`, `model`, `pseudonym_id`, `offline`, `duration_ms`, `outcome` | Không lưu nội dung gửi đi |

**Chuyển trạng thái của lượt gia sư:** `MOI` → `KHO` (mở kho lớp) → `GOI` (hỏi nhà) → `LOC` (lọc lộ đáp án + cổng công thức) → `XONG` hoặc `LOI` hoặc `DUNG` (học sinh dừng). Chỉ `XONG` có câu hiện cho học sinh.

## mastery (`V<n>__mastery.sql`)

| Bảng | Cột chính | Ghi chú |
| --- | --- | --- |
| `mastery_config` | `key`, `value`, `version` | Tham số BKT của v0 (research R7) |
| `mastery_states` | `student_id`, `skill_code`, `mastery`, `level4`, `bloom_level`, `attempts`, `stuck_counter`, `last_error_codes`, `completed_at` | `completed_at` khi đạt Vận dụng cao (FR-026) |
| `mastery_events` | `id`, `student_id`, `skill_code`, `submission_id`, `delta`, `rule_applied`, `wrong_steps`, `error_code`, `confidence`, `guess_suspected` | |
| `mastery_overrides` | `id`, `student_id`, `skill_code`, `level4`, `reason`, `teacher_id`, `created_at`, `removed_at`, `removed_by` | Ghi đè mức của giáo viên (FR-034); bản ghi đang hiệu lực là bản chưa gỡ mới nhất |
| `next_problem_overrides` | `id`, `student_id`, `problem_id`, `reason`, `teacher_id`, `created_at`, `consumed_at` | Bài kế chọn tay (FR-035); hết hiệu lực khi học sinh mở bài |

Đề xuất bài kế tính khi đọc: bài kế chọn tay của giáo viên (nếu có) trước, rồi đề xuất của máy (bài, lý do ∈ {`CHUA_LOI`, `CUNG_CO`, `NANG_1_NAC`, `DE_HON`, `THAY_CO_GIAO`}). Máy dùng mức ghi đè nếu có. Bất biến: mức của bài máy đề xuất ≤ mức đang dùng của kỹ năng + 1.

## planner (`V<n>__planner.sql`)

| Bảng | Cột chính | Ghi chú |
| --- | --- | --- |
| `study_schedules` | `id`, `student_id`, `week_start`, `weekly_slots`, `method_advice`, `created_at` | Lập lại khi mức hiểu đổi |
| `reminders` | `id`, `schedule_id`, `channel` (`IN_APP`), `title`, `body`, `send_at`, `status` | Kênh ngoài để P4 |
