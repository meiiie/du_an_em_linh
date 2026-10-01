---
paths:
  - "apps/web/drizzle/**"
  - "**/db/migration/**"
---

# Migration CSDL

- Chỉ thêm. Không sửa hay xóa migration đã gộp vào `main`; cần đổi thì viết migration mới.
- Số thứ tự tăng liên tục, tên mô tả việc (`0013_<viec>.sql`, Flyway `V<n>__<viec>.sql`).
- Bảng chứa dữ liệu học sinh: bật RLS hoặc kiểm vai trò ở tầng ứng dụng như ADR 006; ghi rõ trong mô tả PR.
- Migration mới phải chạy được trên CSDL trống **và** trên CSDL đã seed (`pnpm db:migrate && pnpm seed`).
