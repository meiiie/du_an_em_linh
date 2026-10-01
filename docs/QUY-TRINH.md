# Quy trình làm việc — người, agent và lab

> Hợp đồng làm việc giữa chủ repo và các agent. Kế thừa `docs/process/AI_WORKFLOW.md` của LMS_hohulili (chạy thật từ 04/2026), bổ sung tầng lab. Hiến chương: [`HIEN-CHUONG.md`](HIEN-CHUONG.md).

## 1. Vai trò

| Ai | Làm | Không làm |
| --- | --- | --- |
| Chủ repo (`@meiiie`) | Định hướng, duyệt ADR / spec / thiết kế, **merge** | — |
| Codex | Rà soát mã theo luồng, viết issue có tiêu chí nghiệm thu, dẫn nguồn | Viết mã, merge |
| Claude Code | Hiện thực issue thành PR, chạy cổng, trả lời review; điều phối lab khi được giao | Tự mở việc ngoài issue, merge, đẩy `main` |
| CodeRabbit | Review tự động theo `.coderabbit.yaml` | Duyệt thay người |
| Lab | Sản xuất tri thức, đặc tả, bộ ca; bàn giao qua issue + tệp trong `labs/` | Sửa mã sản phẩm trực tiếp |

Năm lab, mỗi lab một thư mục, chi tiết ở [`labs/README.md`](../labs/README.md):

| Lab | Thư mục | Sản phẩm chính |
| --- | --- | --- |
| Thiết kế (UI/UX) | `labs/design/` | Tham chiếu, nghiên cứu thiết kế, nguyên mẫu, audit → `docs/DESIGN.md` |
| Sư phạm | `labs/pedagogy/` | Đồ thị kỹ năng, mã lỗi, thang gợi ý, bài mẫu → `data/supham/` |
| Kiểm định & Đánh giá | `labs/evals/` | Bộ ca nghiệm thu, eval gia sư, benchmark → `services/math/kiemdinh/`, CI |
| Nghiên cứu | `labs/research/` | Ghi chú SOTA có nguồn và ngày → được ADR trích |
| Quyết định | `labs/decisions/` | Phân tích phương án có chấm điểm → `docs/adr/` |

## 2. Vòng đời một việc

```text
yêu cầu ─► (lab nếu cần) ─► issue có tiêu chí ─► nhánh ─► hiện thực ─► cổng ─► PR
   ─► CodeRabbit + reviewer lab ─► chủ repo merge ─► nâng tri thức lên docs/ ─► lưu trữ ghi chú lab
```

| Loại việc | Đường đi |
| --- | --- |
| Sửa nhỏ, không đổi hành vi | PR trực tiếp (nêu lý do không có issue) |
| Lỗi / tính năng thường | Issue có tiêu chí → PR |
| Epic: module mới, luồng mới, đổi kiến trúc | Lab liên quan → Spec Kit (`/speckit-specify` → `plan` → `tasks`) → nhiều PR nhỏ |
| Quyết định khó đảo ngược | Phân tích ở `labs/decisions/` → ADR → chủ repo duyệt **trước** khi viết mã |

## 3. Lệnh kích hoạt

| Chủ repo gõ | Agent làm |
| --- | --- |
| `check đi` | Lấy issue mở theo ưu tiên `priority/p0` → `p1` → `p2`, bỏ qua issue đã có `status/*`; làm lần lượt theo skill `/implement-issue`; dừng khi hết hàng, khi gặp lỗi cần người, hoặc khi được bảo dừng |
| `check #N` | Chỉ làm issue N |
| `lab <tên> <chủ đề>` | Mở một phiên lab theo `labs/<tên>/README.md`; kết quả là ghi chú có ngày + đề xuất issue |
| `/retro` | Cuối phiên: rút bài học, đề xuất sửa `AGENTS.md`, rule, skill (không tự sửa) |
| Im lặng | Không làm gì trên repo |

## 4. Chuẩn GitHub

- Nhánh: `feat/` `fix/` `docs/` `chore/` `refactor/` `test/` + tên ngắn kebab-case, sống < 7 ngày. Không chồng PR. Ngoại lệ: epic Spec Kit dùng nhánh `NNN-<slug>` do `/speckit-git-feature` tạo (`.specify/memory/constitution.md`).
- Commit: Conventional Commits, tiêu đề tiếng Việt được phép, có trailer `Co-Authored-By` khi agent viết. Squash merge, lịch sử tuyến tính (release-please đọc tiêu đề squash).
- PR: điền đủ `.github/PULL_REQUEST_TEMPLATE.md`. Có số đo thật (lệnh + kết quả), ảnh 390/1280 nếu đụng UI.
- Nhãn: `.github/labels.json` (`priority/*`, `status/*`, `area/*`, `lab/*`). Đồng bộ: `node scripts/github/sync-labels.mjs` (chủ repo chạy).

**Hook chặn tất định** (`.claude/hooks/guard.mjs`): đẩy lên `main`, `push --force` (trừ `--force-with-lease`), `--no-verify`, `gh pr merge`, `git add -A` / `git add .`, commit sai định dạng hoặc thiếu `Co-Authored-By`, đọc / ghi file bí mật, sửa tay kết quả kiểm định trong `services/math/kiemdinh/ket-qua/`.

## 5. Cổng chất lượng

| Lúc | Ai | Kiểm |
| --- | --- | --- |
| Trước commit | Agent | Test đúng vùng đụng (xem `AGENTS.md` từng thư mục); không bí mật; diff đúng phạm vi |
| Trước PR | Agent | Skill `/ship-check`: cổng theo vùng, tiêu chí nghiệm thu có bằng chứng, tài liệu đi kèm |
| Trước merge | CI + CodeRabbit + reviewer lab | CI xanh; bình luận đã xử lý; với thay đổi gia sư: eval đạt |
| Merge | Chủ repo | Đọc mô tả PR, xem 1–2 file, đọc nhận định CodeRabbit |

## 6. Kịch bản xử lý

- **Nhận một issue:** dùng skill `/implement-issue`.
- **Test đỏ:** không bỏ qua test, không mock để lách. Đọc stack trace, tìm nguyên nhân gốc, sửa đúng chỗ (test sai do API đổi thì sửa test; mã sai thì sửa mã).
- **CI đỏ:** `gh run view <id> --log-failed`; sửa, đẩy lại (không `--force`).
- **CodeRabbit bình luận:** lỗi thật thì sửa; gợi ý phong cách thì sửa cho nhất quán; khác quan điểm thì trả lời lý do; báo nhầm thì ghi «đồng ý là báo nhầm» kèm lý do.
- **Xung đột:** `git fetch && git rebase origin/main`, giải quyết, `push --force-with-lease`. Quá 10 file thì dừng, hỏi chủ repo.
- **Áp «bản vá nguyên văn» từ lab:** áp đúng nguyên văn, ghi mã bản vá (ví dụ `kd-0004b`) trong tiêu đề commit. Không «cải tiến» bản vá. Bản vá xung đột với mã hiện tại thì dừng và báo lab, không tự chế.
- **Lỗi lặp lại:** dừng, hỏi «đây có phải một lớp lỗi?». Nếu phải: phòng vệ nhiều lớp (mã → kiểm lúc build → chặn lúc chạy → sửa thiết kế), ghi vào `docs/` (bài học LMS 2026-04-27).

## 7. Skill và subagent theo việc

| Việc | Skill chính | Subagent review |
| --- | --- | --- |
| Hiện thực issue | `/implement-issue`, `karpathy-guidelines` | — |
| Trước khi mở PR | `/ship-check` | theo vùng bên dưới |
| Gia sư, prompt, bộ lọc, nhà cung cấp AI | `tutor-safety` | `pedagogy-reviewer`, `privacy-reviewer` |
| CAS, cổng 3 tầng, chấm bước | `math-engine` | `math-verifier` |
| Backend v2 (`services/core`) | `spring-core` | `privacy-reviewer` khi chạm quyền / dữ liệu học sinh |
| Frontend v2 (`apps/frontend`) | `angular-frontend`, `design-study` | `design-critic` |
| Nội dung, mức độ, mã lỗi, thang gợi ý | `math-pedagogy` | `pedagogy-reviewer` |
| Giao diện | `design-study`, `better-*` (cấp người dùng) | `design-critic` |
| Nghiên cứu SOTA | `research-sota` | `researcher` (chạy nền) |
| Quyết định kiến trúc | `decision-record` | — |
| Cuối phiên | `/retro` | — |

## 8. Tài liệu ở đâu

| Loại | Chỗ | Quy tắc |
| --- | --- | --- |
| Nguồn chuẩn | `docs/` (`product/`, `adr/`, `HIEN-CHUONG.md`, `QUY-TRINH.md`, `DESIGN.md`, `KIEM-THU.md`, …) | Sửa qua PR; ADR chỉ đổi trạng thái, không viết lại |
| Ghi chú làm việc | `labs/*/YYYY-MM-DD-<chủ-đề>.md` | Có ngày, nguồn, trạng thái; xong thì nâng phần cốt lõi lên `docs/` |
| Theo dõi việc | GitHub Issues | Không lập file theo dõi rời |
| Ngôn ngữ | Tiếng Việt có dấu; tên công nghệ giữ tiếng Anh | — |

## 9. Nhịp

- **Mỗi phiên:** hook `SessionStart` đưa nhánh, thay đổi chưa commit, PR đang mở vào ngữ cảnh. Trước khi sửa file, kiểm không có PR mở đang đụng cùng file.
- **Cuối phiên:** `/retro`; dọn nhánh đã gộp.
- **Hằng tuần:** xử lý PR Dependabot.
- **Hằng quý hoặc sau bản mô hình lớn:** `/doctor prompt-audit`; rà hiến chương, `AGENTS.md`, rule, skill; bỏ chỉ dẫn viết cho mô hình cũ.

## 10. Không làm

| Việc | Vì sao |
| --- | --- |
| Đẩy thẳng `main`, tự merge, `--force`, `--no-verify` | Vượt cổng người duyệt (hook chặn) |
| `git add -A` | Dễ kéo theo bí mật, file rác (hook chặn) |
| Sửa lân cận khi sửa lỗi | Ngoài phạm vi, khó review |
| Trừu tượng hóa cho mã dùng một lần | Phức tạp không cần thiết |
| «Đã test» không kèm số | Vi phạm hiến chương IV |
| Đưa đáp án hay công thức chưa kiểm tới học sinh | Vi phạm hiến chương I, II |
| Dữ liệu học sinh thật trong repo / issue / prompt | Vi phạm hiến chương III |

## Nhật ký thay đổi quy trình

- **2026-10-01** — Thiết lập: kế thừa pipeline Codex → Claude → CodeRabbit → chủ repo của LMS; thêm 5 lab; chuyển guardrail từ chữ sang hook; thay `.claudeignore` (không phải tính năng của Claude Code) bằng `permissions.deny`.
