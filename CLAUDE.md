@AGENTS.md

<!-- Chủ sở hữu harness: @meiiie. Rà lại: 2027-01 hoặc sau mỗi bản mô hình lớn (`/doctor prompt-audit`). Sửa tệp này qua PR như mã. -->

# Claude Code

Harness theo hướng dẫn chính thức của Anthropic (2026): [large codebases](https://claude.com/blog/how-claude-code-works-in-large-codebases-best-practices-and-where-to-start), [steering](https://claude.com/blog/steering-claude-code-skills-hooks-rules-subagents-and-more). Quy ước: sự thật và quy ước ở đây; quy trình ở skill; ràng buộc theo vùng ở rule; điều bắt buộc ở hook.

- **Mở phiên** ở gốc repo khi việc chạm nhiều phần; ở `apps/web` hoặc `services/math` khi chỉ ở đó (CLAUDE.md lớp dưới tự nạp). Hook và quyền chỉ có hiệu lực khi phiên mở trong repo này.
- **Hook** (`.claude/settings.json`, test: `node --test .claude/hooks/*.test.mjs`):
  - `SessionStart` → `session-context.mjs`: nhánh, tệp chưa commit, PR đang mở.
  - `PreToolUse` → `guard.mjs` chặn: push lên `main`, `--force`, `--no-verify`, `gh pr merge`, `git add -A`, commit sai Conventional Commits hoặc thiếu `Co-Authored-By`, đọc / ghi / sao chép file bí mật, sửa tay `services/math/kiemdinh/ket-qua/`.
  - Bị chặn → đọc lý do và làm đúng cách; không tìm đường vòng.
- **Rule theo đường dẫn** (`.claude/rules/`): `math-service`, `tutor-safety`, `migrations`, `docs-labs`, `pedagogy-data`, `web-ui`, `spring-core`, `angular-frontend`.
- **Skill dự án**: `/implement-issue` («check đi», «check #N»), `/ship-check`, `/lab`, `/retro` (chỉ khi gọi); tri thức: `tutor-safety`, `math-engine`, `math-pedagogy`, `spring-core`, `angular-frontend`, `research-sota`, `decision-record`, `design-study`.
- **Subagent** (`.claude/agents/`): `pedagogy-reviewer`, `math-verifier`, `privacy-reviewer`, `design-critic` (rà soát, chỉ đọc); `researcher` (nền, chỉ ghi `labs/research/`). Rà trước PR theo bảng `docs/QUY-TRINH.md` §7.
- **Code intelligence**: cài `typescript-lsp` và `pyright-lsp` (`/plugin install <tên>@claude-plugins-official`); thêm `jdtls-lsp` khi có `services/core`.
- **Commit**: kết thúc thân commit bằng trailer `Co-Authored-By` (hook kiểm); thân PR kết thúc bằng dòng «Generated with Claude Code».
- **Tìm kiếm**: tôn trọng `.gitignore`; lockfile bị chặn đọc (`permissions.deny`) — dùng `pnpm list` để tra phiên bản.
