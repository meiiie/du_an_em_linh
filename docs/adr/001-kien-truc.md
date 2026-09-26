# ADR 001 — Monorepo Next.js và dịch vụ toán FastAPI

Nguyên mẫu tách hai tiến trình.

- `apps/web` là Next.js (App Router), TypeScript, Tailwind. Giao diện tiếng Việt, KaTeX để hiện công thức, MathLive kèm ô LaTeX thường để nhập được khi bàn phím ảo không mở.
- `services/math` là FastAPI. Mọi việc SymPy nằm trong tiến trình con có hạn giờ. Tiến trình HTTP không gọi `sympify` trên chuỗi học sinh.
- PostgreSQL 16, Drizzle ORM, một file SQL `drizzle/0001_init.sql`.

Không thêm neko-core. Đó là công cụ lập trình của Darren, không phải thư viện của phần mềm học.

Giao diện dùng lớp Tailwind trực tiếp (thẻ, nút, bảng) thay vì registry shadcn/ui. Chưa khởi tạo CLI shadcn trong nguyên mẫu này.
