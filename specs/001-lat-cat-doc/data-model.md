# Data model — P2 lát cắt dọc

Bảng PostgreSQL 18 của `services/core`, Flyway chỉ thêm. Tên bảng và cột tiếng Anh như `V1__identity.sql`; giá trị trạng thái giữ mã của v0 (`DAT`, `SAI`, `KHONG_KIEM_DUOC`, `GV_DUYET`…) để đối chiếu được. Nguồn: `apps/web/lib/db/schema.ts` của v0. Bỏ các cột chỉ phục vụ khóa do giáo viên dán (`ai_api_key`, `ai_openai_*`).

## classroom (`V3__classroom.sql`)

| Bảng | Cột chính | Ghi chú |
| --- | --- | --- |
| `classes` | `id`, `name`, `grade`, `school_year` | «12A1 thử» |
| `enrollments` | `class_id`, `user_id`, `role_in_class` (`TEACHER`, `STUDENT`) | PK (`class_id`, `user_id`) |
| `class_settings` | `class_id`, `reveal_solution_after_submit` (mặc định `false`), `ai_provider` (mặc định `offline`), `ai_allow_local` | Nhà AI chỉ chọn trong danh sách máy chủ bật |

## content (`V4__content.sql`)

| Bảng | Cột chính | Ghi chú |
| --- | --- | --- |
| `topics` | `code`, `name`, `grade` | `T12.DH` |
| `skills` | `code`, `topic_code`, `name`, `description`, `is_core` | 7 kỹ năng `T12.DH.01`…`07` |
| `skill_prerequisites` | `skill_code`, `prerequisite_code`, `min_level` | |
| `error_types` | `code`, `skill_code`, `step_code`, `name`, `fix_hint`, `result_types` | Từ `ma-loi-DH.csv` |
| `step_templates` | `step_code`, `topic_code`, `ordinal`, `input_kind`, `skill_code`, `description` | 5 bước `B.DH.*` |
| `problems` | `id`, `code`, `skill_code`, `extra_skill_codes`, `level4`, `level3`, `bloom_level`, `difficulty`, `statement_text`, `statement_latex`, `function_sympy`, `answer_form` (`TU_LUAN_5_BUOC`), `start_step`, `origin`, `status`, `content_hash` | `status`: xem chuyển trạng thái |
| `solutions` | `problem_id`, `worked_solution`, `protected_facts`, `final_answer` | **Không bao giờ** vào DTO của học sinh khi đang làm, không vào prompt |
| `hint_levels` | `problem_id`, `step_code`, `level` (1–3), `text` | Thang đã kiểm |
| `documents` | `id`, `class_id`, `title`, `kind`, `source`, `license_status`, `file_ref`, `text_content`, `version`, `uploaded_by`, `created_at` | `license_status = chua_ro` → không làm căn cứ |
| `document_passages` | `id`, `document_id`, `page`, `char_start`, `char_end`, `text`, `text_folded` | Cho tầng 2 và trích dẫn `[n]` |
| `formula_sheets` | `id`, `class_id`, `version`, `status` (`NHAP`, `KHOA`), `fingerprint`, `locked_at`, `locked_by` | Mỗi lần khóa = phiên bản mới |
| `formulas` | `id`, `formula_sheet_id`, `skill_code`, `title`, `latex`, `statement`, `tier1_status`, `tier2_status`, `citation_passage_id` | Khóa được bảng chỉ khi **mọi dòng** có `tier1_status = DAT` và `tier2_status = DAT` (có trích dẫn); `SAI` hay `KHONG_KIEM_DUOC` chặn khóa (ADR 013) |
| `verification_runs` | `id`, `subject_kind` (`PROBLEM`, `TUTOR_FORMULA`), `subject_id`, `content_hash`, `formula_sheet_version`, `overall_status`, `publish_status`, `stale`, `created_at` | |
| `verification_tier_results` | `run_id`, `tier` (1–3), `status`, `result_type`, `wrong_steps`, `error_code`, `confidence`, `reason`, `citation`, `raw` | Căn cứ từng tầng |
| `content_reviews` | `id`, `run_id`, `content_hash`, `reviewer_id`, `decision` (`GV_DUYET`), `note`, `at` | Bắt buộc `note`; chỉ cho run `subject_kind = PROBLEM` (công thức trong lời gia sư không duyệt riêng, ADR 013) |

**Chuyển trạng thái của bài** (`problems.status`):

```text
NHAP ──kiểm──▶ DAT ──────────────▶ PHAT_HANH
          ├──▶ SAI (BI_CHAN) ──sửa nội dung──▶ NHAP
          └──▶ CHO_DUYET ──GV duyệt (note)──▶ PHAT_HANH
Đổi bảng công thức: kết quả kiểm cũ → stale = true; bài PHAT_HANH giữ nguyên, hiện «cần kiểm lại».
```

## practice (`V5__practice.sql`)

| Bảng | Cột chính | Ghi chú |
| --- | --- | --- |
| `assignments` | `id`, `problem_id`, `student_id`, `status`, `set_name`, `due_at`, `assigned_by`, `assigned_at` | Giao cho lớp = một dòng mỗi học sinh |
| `submissions` | `id`, `student_id`, `problem_id`, `status` (`DANG_LAM`, `DA_NOP`), `guess_suspected`, `guess_reason`, `result`, `started_at`, `submitted_at` | Một bài làm đang mở mỗi (học sinh, bài) |
| `submission_steps` | `id`, `submission_id`, `step_code`, `line_no`, `latex`, `raw_input`, `normalized_input`, `normalizer_version`, `normalize_status` | Nộp lại cùng bước: idempotent theo (`submission_id`, `step_code`, nội dung) |
| `submission_tables`, `submission_table_cells` | bảng xét dấu: `row`, `k` (0-based, `docs/chi-so-o-bang.md`), `value` | |
| `input_events` | `submission_id`, `step_code`, `cell`, `old_value`, `new_value`, `at` | Cho nghi đoán mò |
| `grading_results` | `submission_id`, `step_code`, `result`, `result_type`, `wrong_steps`, `error_code`, `confidence`, `per_step`, `message`, `issues`, `math_ok` | Lỗi dịch vụ toán → `result = KHONG_CHAM_DUOC`, không bao giờ `DAT` |

## tutor (`V6__tutor.sql`)

| Bảng | Cột chính | Ghi chú |
| --- | --- | --- |
| `tutor_sessions` | `id`, `student_id`, `problem_id`, `state`, `hint_levels` (theo bước), `answer_requests`, `answer_requests_step`, `same_error_repeats`, `last_step`, `started_at` | Một phiên mỗi (học sinh, bài) |
| `tutor_messages` | `id`, `session_id`, `role`, `content`, `blocked_by_filter`, `formula_verdicts`, `citations`, `provider`, `created_at` | Chỉ lưu câu đã kiểm; không lưu phần «suy nghĩ» của nhà |
| `llm_calls` | `id`, `purpose`, `provider`, `model`, `pseudonym_id`, `offline`, `duration_ms`, `outcome` | Không lưu nội dung gửi đi |

**Chuyển trạng thái của lượt gia sư:** `MOI` → `KHO` (mở kho lớp) → `GOI` (hỏi nhà) → `LOC` (lọc lộ đáp án + cổng công thức) → `XONG` hoặc `LOI` hoặc `DUNG` (học sinh dừng). Chỉ `XONG` có câu hiện cho học sinh.

## mastery (`V7__mastery.sql`)

| Bảng | Cột chính | Ghi chú |
| --- | --- | --- |
| `mastery_config` | `key`, `value`, `version` | Tham số BKT của v0 (research R7) |
| `mastery_states` | `student_id`, `skill_code`, `mastery`, `level4`, `bloom_level`, `attempts`, `stuck_counter`, `last_error_codes`, `completed_at` | `completed_at` khi đạt Vận dụng cao (FR-026) |
| `mastery_events` | `id`, `student_id`, `skill_code`, `submission_id`, `delta`, `rule_applied`, `wrong_steps`, `error_code`, `confidence`, `guess_suspected` | |
| `escalations` | `id`, `student_id`, `skill_code`, `problem_id`, `step_code`, `kind` (`KET`, `NHO_GV`), `reason`, `created_at`, `handled_at`, `handled_by` | Cảnh báo kẹt và «gửi thầy cô» |

Đề xuất bài kế tính khi đọc (không lưu bảng riêng): (bài, lý do ∈ {`CHUA_LOI`, `CUNG_CO`, `NANG_1_NAC`, `DE_HON`}). Bất biến: mức của bài đề xuất ≤ mức hiện tại của kỹ năng + 1.

## planner (`V8__planner.sql`)

| Bảng | Cột chính | Ghi chú |
| --- | --- | --- |
| `study_schedules` | `id`, `student_id`, `week_start`, `weekly_slots`, `method_advice`, `created_at` | Lập lại khi mức hiểu đổi |
| `reminders` | `id`, `schedule_id`, `channel` (`IN_APP`), `title`, `body`, `send_at`, `status` | Kênh ngoài để P4 |
