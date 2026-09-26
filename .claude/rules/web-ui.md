---
paths:
  - "apps/web/**/*.{tsx,css}"
---

# Web UI

Làm theo `docs/DESIGN.md` và `apps/web/AGENTS.md`.

- Lưới 8 px. Nút/ô: cao 40, chạm 44 khi `pointer: coarse`, đệm ngang 16, bán kính 6.
- Dùng `Button` / `buttonClasses`. Không `py-2.5` / `p-1.5` cho điều khiển.
- `padding` nới hit; `margin` không.
- Giữ `data-testid`. `Link` để đi trang, `button` để làm việc.
- Không chép hex thương hiệu LMS. Không cream + serif + terracotta.
