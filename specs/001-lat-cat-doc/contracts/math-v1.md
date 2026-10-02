# Hợp đồng `services/core` ↔ `services/math` — P2

Giữ nguyên `/v1` của v0 (`services/math/app/routers.py`). Mọi job chạy trong sandbox, trần hết giờ 20 s. Payload là JSON tự do với khóa snake_case tiếng Việt như v0; `services/core` gọi qua một client duy nhất (research R2).

## Job dùng lại

| Đường dẫn | Dùng ở P2 | Hết giờ phía core | Lỗi / hết giờ được hiểu là |
| --- | --- | --- | --- |
| `POST /v1/grade` | chấm từng bước, chấm cả bài (`ham`, `cac_buoc`, `buoc_bat_dau`, `nop_toi`, bảng xét dấu) | 12 s | `KHONG_CHAM_DUOC` |
| `POST /v1/verify` | cổng 3 tầng cho bài (tầng 2 nhận đoạn tài liệu được phép, tầng 3 nhận bảng đã khóa) | 20 s | `KHONG_KIEM_DUOC` |
| `POST /v1/filter` | bộ lọc lộ đáp án trên cả câu gia sư (M3 + M1) | 12 s | chặn câu |
| `POST /v1/goi-y` | thang gợi ý mẫu theo (bước, loại kết quả, cấp); dùng cho nhà `offline` và câu thay thế | 12 s | câu từ chối chung, không chứa kết quả |
| `POST /v1/generate` | biến thể có hạt giống khi nhập ngân hàng (research R6) | 20 s | bỏ biến thể, ghi lỗi nhập |

`/v1/extract` không dùng ở P2: job đọc đường dẫn tệp cục bộ, còn core và math là hai container không chung ổ. Core trích chữ PDF bằng PDFBox (research R9).

## Job mới: `POST /v1/kiem-loi-giang` (ADR 013)

Thuần hàm, không đọc CSDL. Chạy **sau** `/v1/filter`.

**Vào:**

```json
{
  "cau": "câu gia sư đã qua bộ lọc lộ đáp án",
  "bang_cong_thuc": [
    {"id": "f-07", "latex": "\\left(\\frac{u}{v}\\right)' = \\frac{u'v-uv'}{v^2}", "phat_bieu": "đạo hàm của thương", "trich_dan": {"tai_lieu": "…", "trang": 12, "doan": "…"}}
  ],
  "bai_lam_hoc_sinh": ["3x^{2}-12x"],
  "du_kien_bao_ve": ["…dữ kiện bảo vệ của bài, chỉ dùng để chặn…"],
  "ham": "x**3 - 6*x**2 + 9*x + 2",
  "timeout_s": 12
}
```

**Ra:**

```json
{
  "cau_sach": "câu sau khi bỏ biểu thức không qua cổng",
  "thay_bang_goi_y": false,
  "bieu_thuc": [
    {"doan": "\\frac{u'v-uv'}{v^2}", "loai": "CONG_THUC_TONG_QUAT", "trang_thai": "DAT", "dong_bang": "f-07", "tang": {"1": "DAT", "2": "DAT", "3": "DAT"}},
    {"doan": "3x^{2}-12x", "loai": "TRICH_BAI_LAM", "trang_thai": "DAT"},
    {"doan": "y' = 3x^2 - 12x + 9", "loai": "KET_QUA_CU_THE", "trang_thai": "KHONG_KIEM_DUOC", "ly_do": "kết quả tính cụ thể của bài"}
  ]
}
```

- `loai` ∈ `CONG_THUC_TONG_QUAT`, `TRICH_BAI_LAM`, `KET_QUA_CU_THE`, `KHONG_PHAN_TICH_DUOC`.
- `trang_thai` ∈ `DAT`, `SAI`, `KHONG_KIEM_DUOC`. Chỉ biểu thức `DAT` còn lại trong `cau_sach`.
- `thay_bang_goi_y = true` khi câu còn lại mất nghĩa: core thay cả câu bằng gợi ý theo thang của bước.
- Core ghi mỗi biểu thức `SAI` hoặc `KHONG_KIEM_DUOC` thành một `verification_run` (`subject_kind = TUTOR_FORMULA`) vào hàng đợi duyệt.
- Lỗi, hết giờ, JSON hỏng → core không hiện câu, dùng gợi ý theo thang (đóng mặc định).
