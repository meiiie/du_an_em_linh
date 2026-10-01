---
paths:
  - "services/math/**"
---

# Dịch vụ toán (CAS)

- Tiến trình HTTP **không** `import sympy` / `sympify`. Mọi việc SymPy đi qua `app/sandbox.py` → `app/job_runner.py` (tiến trình con có hạn giờ). ADR 002.
- Đúng / sai do bộ chấm quyết định, không do LLM. Không thêm đường tắt «tin mô hình».
- `k` ô bảng 0-based trong mã; YAML `kiemdinh/` 1-based (`docs/chi-so-o-bang.md`).
- Không có khung bước cho dạng bài → `KHONG_KIEM_DUOC`, không bịa bước.
- Bộ lọc lộ đáp án chạy trên **mọi** câu gia sư, fail-closed.
- Đổi chấm, cổng hoặc bộ lọc: chạy `pnpm test:math` và bộ `kiemdinh/` liên quan; ghi số vào PR. Skill: `math-engine`.
- `kiemdinh/ket-qua/` chỉ do script ghi (hook chặn sửa tay). Bộ ca trong `kiemdinh/bo-de-kiem-thu/` thuộc lab Kiểm định: chỉ sửa khi áp bản vá có mã.
