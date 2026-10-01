---
paths:
  - "apps/frontend/**"
---

# apps/frontend (v2 — Angular 22)

- Signal-first: `signal` / `computed` / `linkedSignal`; không `zone.js`; không `standalone: true`, `@Input`, `@Output`, `@ViewChild`, `*ngIf`, `*ngFor`, constructor injection.
- Form mới dùng Signal Forms (`form()`, `[formField]`); widget truy cập dùng `@angular/aria`.
- Client không tính đúng / sai, không giữ đáp án; gia sư chỉ hiện câu đã qua bộ lọc, nhận SSE trạng thái, không stream token.
- Giữ route và `data-testid` của v0 cho màn tương đương.
- Chữ tiếng Việt có dấu theo `docs/DESIGN.md`; kiểm 390 px và 1280 px; chạm 44 px. Chi tiết: skill `angular-frontend`.
