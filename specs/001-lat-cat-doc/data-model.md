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
| `topics` | `code`, `name`, `grade` | `T12.DH` |
| `skills` | `code`, `topic_code`, `name`, `description`, `is_core` | 7 kỹ năng `T12.DH.01`…`07` |
| `skill_prerequisites` | `skill_code`, `prerequisite_code`, `min_level` | |
| `error_types` | `code`, `skill_code`, `step_code`, `name`, `fix_hint`, `result_types` | Từ `ma-loi-DH.csv` |
| `step_templates` | `step_code`, `topic_code`, `ordinal`, `input_kind`, `skill_code`, `description` | 5 bước `B.DH.*` |
| `problems` | `id`, `code`, `skill_code`, `extra_skill_codes`, `level4`, `level3`, `bloom_level`, `difficulty`, `statement_text`, `statement_latex`, `function_sympy`, `answer_form` (`TU_LUAN_5_BUOC`), `start_step`, `origin`, `content_hash` | Nội dung chung của chủ đề; trạng thái phát hành nằm ở `problem_releases` theo lớp |
| `solutions` | `problem_id`, `worked_solution`, `protected_facts`, `final_answer` | **Không bao giờ** vào DTO của học sinh khi đang làm, không vào prompt |
| `hint_levels` | `problem_id`, `step_code`, `level` (1–3), `text` | Thang đã kiểm |
| `hint_gate_results` | `problem_id`, `step_code`, `result_kind` (loại kết quả hoặc `chung`), `level`, `text` (câu đã điền tham số của đề), `formula_sheet_id`, `status`, `checked_at` | Câu thay thế lúc chạy chỉ lấy gợi ý `DAT` với phiên bản bảng hiện tại (ADR 013 mục 6) |
| `documents` | `id`, `class_id`, `title`, `kind`, `source`, `license_status`, `file_ref`, `text_content`, `version`, `uploaded_by`, `created_at` | `license_status = chua_ro` → không làm căn cứ |
| `document_passages` | `id`, `document_id`, `page`, `char_start`, `char_end`, `text`, `text_folded` | Cho tầng 2 và trích dẫn `[n]` |
| `formula_sheets` | `id`, `class_id`, `version`, `status` (`NHAP`, `KHOA`), `fingerprint`, `locked_at`, `locked_by` | Mỗi lần khóa = phiên bản mới |
| `formulas` | `id`, `formula_sheet_id`, `skill_code`, `title`, `latex`, `statement`, `tier1_status`, `tier2_status`, `citation_passage_id` | Khóa được bảng chỉ khi **mọi dòng** có `tier1_status = DAT` và `tier2_status = DAT` (có trích dẫn); `SAI` hay `KHONG_KIEM_DUOC` chặn khóa (ADR 013) |
| `verification_runs` | `id`, `class_id`, `subject_kind` (`PROBLEM`, `TUTOR_FORMULA`), `subject_id`, `content_hash`, `formula_sheet_id`, `overall_status`, `publish_status`, `stale`, `created_at` | Tầng 2, tầng 3 dùng tài liệu và bảng của lớp, nên run gắn lớp và đúng bảng đã dùng (bảng có `class_id`, `version`) |
| `problem_releases` | `class_id`, `problem_id`, `status`, `run_id`, `updated_at` | PK (`class_id`, `problem_id`). `status` dùng đúng mã phát hành của v0: `NHAP`, `DA_PHAT_HANH`, `BI_CHAN`, `CHO_GIAO_VIEN_DUYET`; học sinh chỉ thấy bài `DA_PHAT_HANH` của lớp mình |
| `verification_tier_results` | `run_id`, `tier` (1–3), `status`, `result_type`, `wrong_steps`, `error_code`, `confidence`, `reason`, `citation`, `raw` | Căn cứ từng tầng |
| `content_reviews` | `id`, `run_id`, `content_hash`, `reviewer_id`, `decision` (`GV_DUYET`), `note`, `at` | Bắt buộc `note`; chỉ cho run `subject_kind = PROBLEM` (công thức trong lời gia sư không duyệt riêng, ADR 013) |

**Hai lớp trạng thái, giữ mã của v0** (`services/math/app/verify.py`, `cong_phat_hanh`):

- kết quả kiểm (`verification_runs.overall_status`, từng tầng): `DAT`, `SAI`, `KHONG_KIEM_DUOC`, sau duyệt là `GV_DUYET`;
- trạng thái phát hành (`verification_runs.publish_status`, chép sang `problem_releases.status` của lớp): `DA_PHAT_HANH`, `BI_CHAN`, `CHO_GIAO_VIEN_DUYET`.

Bài, kỹ năng, khung bước, thang gợi ý là nội dung chung của chủ đề. Tài liệu, bảng công thức, kết quả kiểm và trạng thái phát hành tính **theo lớp**: một run kiểm bằng tài liệu của lớp A không phát hành bài cho lớp B.

```text
NHAP ──/v1/verify──▶ DA_PHAT_HANH            (mọi tầng DAT)
               ├──▶ BI_CHAN                  (một tầng SAI) ──sửa nội dung──▶ NHAP
               └──▶ CHO_GIAO_VIEN_DUYET      (còn KHONG_KIEM_DUOC) ──GV duyệt (note, GV_DUYET)──▶ DA_PHAT_HANH
Đổi bảng công thức: kết quả kiểm cũ → stale = true; bài DA_PHAT_HANH giữ nguyên, hiện «cần kiểm lại».
GV duyệt chỉ trên run mới nhất của (lớp, bài), không stale, content_hash và formula_sheet_id khớp hiện tại;
ngược lại 409 và phải kiểm lại, để không phát hành bằng phán quyết của nội dung hay bảng đã cũ.
```

## practice (`V5__practice.sql`)

| Bảng | Cột chính | Ghi chú |
| --- | --- | --- |
| `assignments` | `id`, `class_id`, `problem_id`, `student_id`, `status`, `set_name`, `due_at`, `assigned_by`, `assigned_at` | Giao cho lớp = một dòng mỗi học sinh; chỉ giao bài `DA_PHAT_HANH` của chính lớp đó (`problem_releases`) |
| `submissions` | `id`, `class_id`, `student_id`, `problem_id`, `status` (`DANG_LAM`, `DA_NOP`), `guess_suspected`, `guess_reason`, `result`, `started_at`, `submitted_at` | Một bài làm đang mở mỗi (học sinh, lớp, bài); lớp quyết định tài liệu, bảng công thức, cài đặt gia sư dùng cho bài làm |
| `submission_steps` | `id`, `submission_id`, `step_code`, `line_no`, `latex`, `raw_input`, `normalized_input`, `normalizer_version`, `normalize_status` | Nộp lại cùng bước: idempotent theo (`submission_id`, `step_code`, nội dung) |
| `submission_tables`, `submission_table_cells` | bảng xét dấu: `row`, `k` (0-based, `docs/chi-so-o-bang.md`), `value` | |
| `input_events` | `submission_id`, `step_code`, `cell`, `old_value`, `new_value`, `at` | Cho nghi đoán mò |
| `grading_results` | `submission_id`, `step_code`, `result`, `result_type`, `wrong_steps`, `error_code`, `confidence`, `per_step`, `message`, `issues`, `math_ok` | Lỗi dịch vụ toán → `result = KHONG_CHAM_DUOC`, không bao giờ `DAT` |

## tutor (`V6__tutor.sql`)

| Bảng | Cột chính | Ghi chú |
| --- | --- | --- |
| `tutor_sessions` | `id`, `class_id`, `student_id`, `problem_id`, `state`, `hint_levels` (theo bước), `answer_requests`, `answer_requests_step`, `same_error_repeats`, `last_step`, `started_at` | Một phiên mỗi (học sinh, lớp, bài); kho lớp, bảng công thức và nhà AI lấy theo lớp |
| `tutor_messages` | `id`, `session_id`, `role`, `content`, `blocked_by_filter`, `formula_verdicts`, `citations`, `provider`, `created_at` | Chỉ lưu câu đã kiểm; không lưu phần «suy nghĩ» của nhà |
| `llm_calls` | `id`, `purpose`, `provider`, `model`, `pseudonym_id`, `offline`, `duration_ms`, `outcome` | Không lưu nội dung gửi đi |

**Chuyển trạng thái của lượt gia sư:** `MOI` → `KHO` (mở kho lớp) → `GOI` (hỏi nhà) → `LOC` (lọc lộ đáp án + cổng công thức) → `XONG` hoặc `LOI` hoặc `DUNG` (học sinh dừng). Chỉ `XONG` có câu hiện cho học sinh.

## mastery (`V7__mastery.sql`)

| Bảng | Cột chính | Ghi chú |
| --- | --- | --- |
| `mastery_config` | `key`, `value`, `version` | Tham số BKT của v0 (research R7) |
| `mastery_states` | `student_id`, `skill_code`, `mastery`, `level4`, `bloom_level`, `attempts`, `stuck_counter`, `last_error_codes`, `completed_at` | `completed_at` khi đạt Vận dụng cao (FR-026) |
| `mastery_events` | `id`, `student_id`, `skill_code`, `submission_id`, `delta`, `rule_applied`, `wrong_steps`, `error_code`, `confidence`, `guess_suspected` | |
| `mastery_overrides` | `id`, `student_id`, `skill_code`, `level4`, `reason`, `teacher_id`, `created_at`, `removed_at`, `removed_by` | Ghi đè mức của giáo viên (FR-034); bản ghi đang hiệu lực là bản chưa gỡ mới nhất |
| `next_problem_overrides` | `id`, `student_id`, `problem_id`, `reason`, `teacher_id`, `created_at`, `consumed_at` | Bài kế chọn tay (FR-035); hết hiệu lực khi học sinh mở bài |

Đề xuất bài kế tính khi đọc: bài kế chọn tay của giáo viên (nếu có) trước, rồi đề xuất của máy (bài, lý do ∈ {`CHUA_LOI`, `CUNG_CO`, `NANG_1_NAC`, `DE_HON`, `THAY_CO_GIAO`}). Máy dùng mức ghi đè nếu có. Bất biến: mức của bài máy đề xuất ≤ mức đang dùng của kỹ năng + 1.

## planner (`V8__planner.sql`)

| Bảng | Cột chính | Ghi chú |
| --- | --- | --- |
| `study_schedules` | `id`, `student_id`, `week_start`, `weekly_slots`, `method_advice`, `created_at` | Lập lại khi mức hiểu đổi |
| `reminders` | `id`, `schedule_id`, `channel` (`IN_APP`), `title`, `body`, `send_at`, `status` | Kênh ngoài để P4 |
