---
name: implement-issue
description: Hiện thực một GitHub issue thành PR theo quy trình repo — nhánh, mã, cổng kiểm, PR, xử lý CodeRabbit. Dùng khi chủ repo gõ «check đi» (lấy hàng đợi theo ưu tiên), «check #N» (một issue), hoặc giao làm một issue cụ thể.
argument-hint: "[số issue — để trống để lấy hàng đợi]"
allowed-tools: Bash(gh issue view *) Bash(gh issue list *) Bash(gh pr list *) Bash(gh pr view *) Bash(gh pr checks *) Bash(git status *) Bash(git diff *) Bash(git log *) Bash(git fetch *)
---

# Issue → PR

Nhánh hiện tại: !`git branch --show-current`

Hàng đợi (issue mở chưa có nhãn `status/*`):
!`gh issue list --state open --limit 30 --json number,title,labels --jq '.[] | select([.labels[].name] | any(startswith("status/")) | not) | "#\(.number) [\([.labels[].name | select(startswith("priority/"))] | join(","))] \(.title)"' 2>/dev/null || echo "(không lấy được danh sách issue)"`

«check đi» / «check #N» là sự cho phép cho các thao tác GitHub trong skill này: bình luận, gắn nhãn, đẩy nhánh, mở PR. **Không** gồm merge.

## Chọn việc

- Có số issue trong `$ARGUMENTS` → chỉ làm issue đó.
- Không có → issue đầu theo `priority/p0` → `p1` → `p2` → không nhãn. Hàng rỗng → báo «Hàng đợi trống», dừng.
- Báo một dòng: «Bắt đầu #N — <tiêu đề>».

## Các bước

1. `gh issue view N --comments`. Thiếu tiêu chí nghiệm thu hoặc mâu thuẫn hiến chương → hỏi chủ repo, không đoán.
2. `gh pr list --state open` — có PR khác sửa cùng file → báo và chờ.
3. `gh issue comment N --body "Claude bắt đầu — sẽ báo khi PR sẵn sàng."`; `gh issue edit N --add-label status/claude-implementing`.
4. `git fetch origin && git switch -c <loại>/<tên-ngắn> origin/main` (loại: feat, fix, docs, chore, refactor, test).
5. Đọc `AGENTS.md` của thư mục sẽ sửa; nạp skill theo vùng (`docs/QUY-TRINH.md` §7).
6. Lỗi: viết test tái hiện trước. Hiện thực tối thiểu, đúng phạm vi issue.
7. Chạy `/ship-check`. Đỏ → sửa nguyên nhân gốc, không bỏ qua test.
8. Stage từng đường dẫn; commit Conventional Commits bằng heredoc, kết thúc trailer `Co-Authored-By` (hook kiểm).
9. `git push -u origin <nhánh>`; `gh pr create` theo `.github/PULL_REQUEST_TEMPLATE.md`: `Closes #N`, số đo thật (lệnh + kết quả + SHA), ảnh 390/1280 nếu đụng UI.
10. `gh pr checks` cho tới khi xong. Kích hoạt CodeRabbit (repo đang ở chế độ thủ công): `gh pr comment <PR> --body "@coderabbitai review"`. Đọc review của Codex và CodeRabbit: `gh api repos/meiiie/du_an_em_linh/pulls/<PR>/comments` (bình luận inline) và `gh pr view <PR> --comments`. Kiểm chứng từng điều trước khi sửa (`docs/QUY-TRINH.md` §6), mỗi mối lo một commit, trả lời từng bình luận kèm SHA hoặc bằng chứng.
11. Xong: đổi nhãn sang `status/claude-pr-ready`; bình luận issue: link PR + bằng chứng từng tiêu chí. Với «check đi»: quay lại «Chọn việc».

## Dừng và hỏi khi

- Cần thay đổi ngoài phạm vi issue, hoặc chạm vùng có ADR «Đề xuất» chưa duyệt.
- Ba lần liên tiếp đỏ không rõ nguyên nhân.
- Cần bí mật, dữ liệu thật, deploy, đổi secret hay nhánh bảo vệ.
