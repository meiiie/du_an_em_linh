# services/math

FastAPI. Mọi việc SymPy chạy trong tiến trình con có hạn giờ. Tiến trình HTTP **không** `import sympy` / `sympify`.

## Lệnh (từ `services/math`)

```bash
.venv/bin/pytest
.venv/bin/uvicorn app.main:app --host 127.0.0.1 --port 8000
```

Hoặc từ gốc: `pnpm test:math`, `pnpm dev:math`.

## Bản đồ module

| File | Việc |
| --- | --- |
| `app/main.py` | `/health`, CORS :3000, gắn router `/v1` |
| `app/routers.py` | HTTP `/v1/*` |
| `app/schemas.py` | Pydantic |
| `app/sandbox.py` | Tiến trình con + timeout |
| `app/job_runner.py` | Điều phối việc |
| `app/grader.py` | Chấm 5 bước (`k` từ 0) |
| `app/verify.py` | Cổng 3 tầng |
| `app/dong_cong_thuc.py` | Kiểm dòng bảng công thức khi khóa (ADR 013): tầng 1 máy kiểm, tầng 2 tài liệu được phép |
| `app/leakfilter.py` | Lọc lộ đáp án |
| `app/generator.py` | Sinh biến thể |
| `app/normalizer.py` | LaTeX / biểu thức |
| `app/machine.py` | Máy tự giải |

## Gotcha

- `k` sản phẩm 0-based. YAML `kiemdinh/` 1-based — dịch khi đối chiếu (`docs/chi-so-o-bang.md`).
- Không có khung 5 bước → `/v1/solve` + `KHONG_KIEM_DUOC`, không bịa bước.
- Giữ tên hàm khi parse (`Abs`, …).
- Bộ lọc chạy trên **mọi** câu gia sư, kể cả offline.

## Skill

`.cursor/skills/fastapi-routers`. Router là `app/routers.py` (một file), không phải gói `app/routers/`.
