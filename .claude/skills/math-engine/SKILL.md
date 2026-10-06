---
name: math-engine
description: Làm việc với dịch vụ toán FastAPI + SymPy — sandbox, chấm khung bước, cổng 3 tầng, bộ lọc lộ đáp án, sinh biến thể, và bộ kiểm định đóng băng. Dùng khi sửa hoặc thêm dạng bài, mã lỗi, luật chấm, cổng, bộ lọc trong services/math, hoặc khi cần chạy bộ kiểm định.
paths:
  - "services/math/**"
---

# Dịch vụ toán

Bản đồ module: `services/math/AGENTS.md`. Ràng buộc ngắn: `.claude/rules/math-service.md`.

Hợp đồng HTTP (`/v1`, chỉ POST): `/grade` chấm bước · `/verify` cổng 3 tầng · `/filter` lọc lộ đáp án · `/generate` sinh biến thể · `/solve` máy tự giải · `/goi-y` thang gợi ý mẫu · `/kiem-dong-cong-thuc` kiểm dòng bảng công thức khi khóa (ADR 013) · `/kiem-loi-giang` cổng công thức cho câu gia sư (ADR 013) · `/extract` trích PDF. Thêm `GET /health`.

## Chạy

- Cài: `cd services/math && python -m venv .venv && .venv/bin/pip install -e ".[dev]"` (Windows: `.venv\Scripts\pip`).
- Test: `pnpm test:math`. Mốc 29/09/2026: 1053 đạt.
- Bộ kiểm định (chạy từ `services/math`, ghi vào `kiemdinh/ket-qua/`):
  - Tầng 1, bộ 102 ca: `.venv/bin/python kiemdinh/tang1/chay_bo_de.py`
  - Định vị bước sai 5 bước: `.venv/bin/python kiemdinh/tang1/chay_5_buoc.py`
  - Bộ lọc lộ đáp án, so M1 / M2 / M3: `.venv/bin/python kiemdinh/loc-lo-dap-an/loc.py`
  - 288 ca đầu vào độc hại: `tests/test_dau_vao_doc_hai_kd.py` (kiểm SHA bộ ca + đủ 288)

## Kỷ luật kiểm định

Giữ đúng cách nhóm Kiểm định đã làm (`kiemdinh/NHAT-KY.md`):

- Bộ ca **đóng băng** bằng SHA-256. Không sửa bộ ca cho khớp kết quả. Bộ kiểm sai thì sửa bộ kiểm và ghi rõ «lỗi của bộ kiểm, không phải của đề».
- Mỗi lần chạy ghi vào `NHAT-KY.md` (giờ Việt Nam): bộ nào, SHA, kết quả, nguyên nhân nếu sai. Giữ cả kết quả trước khi sửa (`*-lan1-truoc-sua-loi.*`).
- Bản vá của lab áp nguyên văn bằng `git apply --3way`; xung đột thì ghi cách giải vào `NHAT-KY.md` và PR.

## Thêm một dạng bài

1. Lab Sư phạm giao gói nội dung (`labs/pedagogy/README.md`).
2. Lab Kiểm định viết bộ ca đúng + sai có nhãn bước / ô / mã lỗi, băm SHA.
3. Hiện thực theo thứ tự: `normalizer` (ký hiệu Việt) → `grader` (chấm từng bước, gán mã lỗi) → `verify` (3 tầng) → `leakfilter` (sự kiện cần bảo vệ của dạng bài) → `generator` (biến thể).
4. Nghiệm thu: 0 báo nhầm, bắt đủ lỗi theo nhãn; thang gợi ý của dạng bài qua bộ lọc: 0 chặn nhầm.

## Bẫy đã gặp (nguồn: `NHAT-KY.md`, commit 0002c–0004)

- SymPy cho `sign(0) = 0` nên điểm gãy của `|u|` trông như «xác định» → dùng `diem_gay()`.
- Nghiệm của tử số `y'` nằm ngoài tập xác định không phải mốc xét dấu.
- Hai điểm cực trị cùng giá trị: so như tập hợp, không so danh sách.
- Chỉ số SGK `x_CT`, `x_{CĐ}`, `x_1` không phải mã lệnh.
- Số dạng `08`, `-02` phải chuẩn hóa trước khi so.
