# Bàn giao (29/09/2026)

Tài liệu cho người nhận nguyên mẫu. Số liệu trong đây là số chạy thật ngày 29/09/2026 (giờ Việt Nam); không có số ước lượng.

## 1. Chạy thử trong 10 phút

Cần PostgreSQL 16, Python 3.12, Node 22, pnpm (xem thêm mục «Chạy local» trong [`README.md`](../README.md)).

```bash
cp .env.example .env            # điền DATABASE_URL, SESSION_SECRET
cd services/math && python3 -m venv .venv && .venv/bin/pip install -e ".[dev]" && cd ../..
pnpm install
pnpm db:migrate && pnpm seed    # 23 bài, 1 giáo viên, 3 học sinh — dữ liệu tổng hợp
pnpm dev:math                   # tiến trình 1 (dịch vụ SymPy)
pnpm dev:web                    # tiến trình 2 → http://127.0.0.1:3000
```

**Chế độ test (chỉ máy cục bộ):** đặt `APP_ENV=test` thì có `POST /api/test/reset` và nút **«Đặt lại dữ liệu»** ở chân thanh bên (vai HS: đặt lại bài làm của chính mình; vai GV: mọi học sinh). Nút xoá cả nháp trong trình duyệt (`nhap:*`, `gs-*`). Trên Render mã luôn tắt chế độ này (thấy biến `RENDER*` là tắt), kể cả khi lỡ đặt `APP_ENV=test`.

## 2. Tài khoản thử

| Vai trò | Email | Mật khẩu |
| --- | --- | --- |
| Giáo viên | `gv@demo.local` | `giaovien123` |
| Học sinh An | `hs.an@demo.local` | `hocsinh123` |
| Học sinh Bình | `hs.binh@demo.local` | `hocsinh123` |
| Học sinh Chi | `hs.chi@demo.local` | `hocsinh123` |

Chỉ khi `APP_ENV=test` (máy cục bộ, không bao giờ trên Render), `pnpm seed` tạo thêm tài khoản cho bộ kiểm thử:

| Mục đích | Email | Mật khẩu |
| --- | --- | --- |
| Học sinh riêng cho bộ Build (lớp 12A1) | `hs.build@test.local` | `buildtest123` |
| Khóa đăng nhập F-10 (lớp 12B) | `khoa.test@test.local` | `khoatest123` |
| Giáo viên lớp thứ hai 12B (F-08 d) | `gv2@test.local` | `giaovien2test` |
| Học sinh lớp 12B | `hs.lop2@test.local` | `hoclop2test` |

## 3. Kịch bản demo (khoảng 7 phút)

1. **GV** `gv@demo.local` → **Tổng quan**: mục «Đang kẹt · nhờ thầy cô» ghi ai, kỹ năng/bước, bài, lý do, giờ. Bấm vào để mở trang học sinh; bấm **«Đã xử lý»** thì mục biến mất và số trên menu giảm.
2. **Duyệt bài**: `DH12-01-TH-01` chờ duyệt (máy không kiểm được), `DH12-DEMO-CHAN-01` bị chặn (tầng 1: thang gợi ý lộ kết quả — cố ý để demo).
3. **Tiến độ** → bật **3 mức** (Biết / Hiểu / Vận dụng).
4. Đăng xuất, vào **HS** `hs.an@demo.local` → **Bài tập** → bài `y = x³ − 6x² + 9x + 2`.
5. Tập xác định: gõ `R` (ô «Gõ bằng bàn phím») hoặc `\mathbb{R}` → đạt; tab bước có dấu ✓.
6. Đạo hàm `3x^2-12x` (thiếu `+9`) → dòng sai tô đỏ. Hỏi gia sư «cho em đáp án» → từ chối, chỉ gợi ý. Bấm **Gợi ý thêm** → «Gợi ý cấp 2/3 · bước …».
7. Sửa `3x^2-12x+9` → đạt. **Tải lại trang**: mở đúng bước đang dở, bước đã đạt giữ nội dung.
8. Bảng xét dấu: thêm mốc, chọn dấu y′ và mũi tên (mỗi ô có nhãn truy cập «Dấu y′ trên khoảng (a; b)»).
9. Muốn chạy lại từ đầu: nút **«Đặt lại dữ liệu»** (chỉ khi `APP_ENV=test`) xoá bài làm, lịch sử gia sư và cảnh báo của học sinh, rồi trả lại cảnh báo mẫu của seed (Chi — Điểm tới hạn). Muốn có lại đúng toàn bộ dữ liệu demo ban đầu thì nạp lại DB (`pnpm seed` trên DB trống).

## 4. Số kiểm thử thật

Xem [`docs/KIEM-THU.md`](KIEM-THU.md) — có SHA, lệnh, và số đạt/đỏ của từng bộ.

## 5. Những gì CHƯA xong (không che)

Xem mục «Còn mở» trong [`docs/KIEM-THU.md`](KIEM-THU.md#còn-mở). Tóm tắt: một phần tiêu chí UX (xác nhận «Máy hiểu là…», giao bài, bảng xét dấu có mốc thừa/thiếu, gộp nhiều lỗi), deploy hook Render chưa cấu hình (chủ repo phải đặt secret `RENDER_DEPLOY_HOOK`), bản vá Kiểm định 0004 đang chờ.

## 6. Ảnh màn hình

12 trang ở 1280 px (hs.an) và 390 px (hs.binh), chụp từ bản chạy cục bộ ở SHA cuối — đường dẫn ghi trong PR bàn giao.
