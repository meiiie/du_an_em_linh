@AGENTS.md

# Claude Code

Harness theo [How Claude Code works in large codebases](https://claude.com/blog/how-claude-code-works-in-large-codebases-best-practices-and-where-to-start) (Anthropic, 2026):

- Tệp này gầy. Quy ước cục bộ: `apps/web/CLAUDE.md`, `services/math/CLAUDE.md`. Claude cộng dồn khi đi xuống cây thư mục.
- Skill on-demand — đừng nhét workflow chuyên biệt vào đây.
- Quyền chung: `.claude/settings.json` (`permissions.deny` bí mật).
- Luật theo đường dẫn: `.claude/rules/`.
- Bản đồ khi cây thư mục chưa đủ: `docs/CODEMAP.md`.
- Bỏ qua `.venv`, `node_modules`, `.next` (`.claudeignore`).
- Khởi tạo phiên trong `apps/web` hoặc `services/math` khi việc chỉ nằm ở đó; ngữ cảnh gốc vẫn được nạp khi đi lên.
