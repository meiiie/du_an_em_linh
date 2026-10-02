# Hợp đồng `services/core` ↔ `services/math` — P2

Giữ nguyên `/v1` của v0 (`services/math/app/routers.py`). Mọi job chạy trong sandbox, trần hết giờ 20 s. Payload là JSON tự do với khóa snake_case tiếng Việt như v0; `services/core` gọi qua một client duy nhất (research R2).

## Job dùng lại

| Đường dẫn | Dùng ở P2 | Hết giờ phía core | Lỗi / hết giờ được hiểu là |
| --- | --- | --- | --- |
| `POST /v1/grade` | chấm từng bước, chấm cả bài (`ham`, `cac_buoc`, `buoc_bat_dau`, `nop_toi`, bảng xét dấu) | 12 s | `KHONG_CHAM_DUOC` |
| `POST /v1/verify` | cổng 3 tầng cho bài (tầng 2 nhận đoạn tài liệu được phép, tầng 3 nhận bảng đã khóa) | 20 s | `KHONG_KIEM_DUOC` |
| `POST /v1/filter` | bộ lọc lộ đáp án trên cả câu gia sư (M3 + M1) | 12 s | chặn câu |
| `POST /v1/goi-y` | thang gợi ý mẫu theo (bước, loại kết quả, cấp). Lúc nhập bài: sinh sẵn câu đã điền tham số của đề để kiểm trước (ADR 013 mục 6). Lúc chạy: nhà `offline`, câu ra vẫn qua `/v1/filter` và `/v1/kiem-loi-giang` như mọi nhà | 12 s | lúc nhập: bài thiếu thang, ghi lỗi nhập; lúc chạy: câu cố định không chứa toán |
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
    {"doan": "x^3 - 6x^2 + 9x + 2", "loai": "TRICH_DE_BAI", "trang_thai": "DAT"},
    {"doan": "đạo hàm của tích bằng đạo hàm u nhân v trừ u nhân đạo hàm v", "loai": "QUY_TAC_BANG_LOI", "trang_thai": "KHONG_KIEM_DUOC", "ly_do": "không khớp phát biểu dòng bảng nào; bỏ cả câu"},
    {"doan": "y' = 3x^2 - 12x + 9", "loai": "KET_QUA_CU_THE", "trang_thai": "KHONG_KIEM_DUOC", "ly_do": "kết quả tính cụ thể của bài"}
  ]
}
```

- `loai` ∈ `CONG_THUC_TONG_QUAT`, `QUY_TAC_BANG_LOI`, `TRICH_BAI_LAM`, `TRICH_DE_BAI`, `KET_QUA_CU_THE`, `KHONG_PHAN_TICH_DUOC`.
- `QUY_TAC_BANG_LOI`: câu có thuật ngữ toán đi cùng từ quan hệ hay phép toán (từ vựng của lab, KD-0005). `DAT` chỉ khi khớp phát biểu một dòng bảng (`dong_bang`) hoặc bộ nhận dạng tầng 2 ứng với một dòng bảng; ngược lại `KHONG_KIEM_DUOC` và bỏ cả câu (ADR 013 mục 2).
- `TRICH_DE_BAI`: so với `ham` sau chuẩn hóa cách viết, không rút gọn (ADR 013 mục 1).
- `trang_thai` ∈ `DAT`, `SAI`, `KHONG_KIEM_DUOC`. Chỉ biểu thức `DAT` còn lại trong `cau_sach`.
- `thay_bang_goi_y = true` khi câu còn lại mất nghĩa: core thay cả câu bằng gợi ý đã kiểm trước của bước (ADR 013 mục 6).
- Core ghi mỗi biểu thức `SAI` hoặc `KHONG_KIEM_DUOC` thành một `verification_run` (`subject_kind = TUTOR_FORMULA`) vào hàng đợi duyệt.
- Lỗi, hết giờ, JSON hỏng → core không hiện câu; câu thay thế chỉ lấy từ gợi ý **đã qua job này từ trước** với phiên bản bảng hiện tại (ADR 013 mục 6), không có thì câu cố định không chứa toán.
- Biểu thức trông như toán nằm ngoài `$…$`, `\(…\)`, `\[…\]` mà không phân loại được → `KHONG_PHAN_TICH_DUOC`, bị bỏ (đóng mặc định).
- Cùng job chạy **trước** cho mọi câu gợi ý (thang của bài, thang mẫu) khi khóa bảng hoặc nhập bài; core lưu phán quyết theo (câu gợi ý, phiên bản bảng).

## Job mới: `POST /v1/kiem-dong-cong-thuc` (ADR 013, khóa bảng)

Thuần hàm. Core gọi khi giáo viên bấm khóa bảng nháp (T042) và khi importer khóa bảng của v0.

**Vào:**

```json
{
  "dong": [
    {"id": "d-1", "tieu_de": "Đạo hàm thương", "latex": "(u/v)' = (u'v - uv') / v^2", "phat_bieu": "Với thương, tử là u'v trừ uv', mẫu là v bình."},
    {"id": "d-4", "tieu_de": "Đơn điệu", "latex": "y' \\ge 0 … \\Rightarrow \\text{đồng biến}", "phat_bieu": "Hàm đồng biến trên khoảng khi …"}
  ],
  "tai_lieu": [{"id": "tl-1", "ten": "…", "doan": [{"id": "p-12", "trang": 3, "text": "…"}], "license_status": "tu_soan"}],
  "timeout_s": 20
}
```

**Ra:**

```json
{
  "dong": [
    {"id": "d-1", "loai": "DANG_THUC",
     "tang1": {"trang_thai": "DAT", "muc_bang_chung": "CAS", "can_cu": "SymPy: d/dx(u/v) − ((Du*v-u*Dv)/v^2) rút gọn bằng 0 với u(x), v(x) ký hiệu"},
     "tang2": {"trang_thai": "DAT", "trich_dan": {"tai_lieu": "tl-1", "doan": "p-12", "trich": "…"}, "trich_dan_them": [{"tai_lieu": "tl-1", "doan": "p-13", "trich": "…"}]}},
    {"id": "d-4", "loai": "DINH_LI",
     "tang1": {"trang_thai": "DAT", "muc_bang_chung": "DANH_MUC", "can_cu": "«y' ≥ 0, y' = 0 chỉ tại hữu hạn điểm ⇒ đồng biến» khớp DD2. …"},
     "tang2": {"trang_thai": "DAT", "trich_dan": {"tai_lieu": "tl-2", "doan": "p-7", "trich": "…"}}},
    {"id": "d-9", "loai": "DINH_LI",
     "tang1": {"trang_thai": "SAI", "can_cu": "Phản ví dụ cho «đồng biến ⇒ y' > 0».", "phan_vi_du": {"ham": "y = x**3", "khoang": "Reals"}},
     "tang2": {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "…"}}
  ],
  "bo_qua": [{"tai_lieu": "tl-3", "ly_do": "quyen_khong_hop_le"}]
}
```

- `loai` ∈ `DANG_THUC`, `DINH_LI` (đơn điệu, dấu hiệu cực trị, định nghĩa điểm tới hạn), `KHONG_BIET` (tầng 1 `KHONG_KIEM_DUOC`, kèm mệnh đề máy chưa đọc trọn).
- **Tầng 1, đẳng thức** (`muc_bang_chung = CAS`): `DAT` khi d/dx E − R rút gọn bằng 0 **và** mỗi câu của `phat_bieu` là một câu đọc đã kiểm của chính E (`DANH_MUC_CAU_DOC` trong `app/dong_cong_thuc.py`, so E bằng CAS; máy không đọc nghĩa lời, thêm câu qua lab Kiểm định); câu khác thì `KHONG_KIEM_DUOC`. `SAI` chỉ khi thế hàm mẫu ra hiệu khác 0; kết quả kèm `phan_vi_du` và `may_doc` (vế máy đã đọc).
- **Tầng 1, định lí** (`muc_bang_chung = DANH_MUC`, thế giới đóng): `DAT` chỉ khi **mọi** mệnh đề của dòng (LaTeX và lời) đọc được trọn và khớp danh mục định lí của chủ đề (`DANH_MUC_DINH_LI` trong `app/dong_cong_thuc.py`: DD1–DD3 đơn điệu trên khoảng, CT1–CT2 dấu hiệu cực trị, TH định nghĩa điểm tới hạn đủ ba thành phần). «Đọc được trọn» nghĩa là: đúng chiều suy ra («A chỉ khi B» là A ⇒ B; câu đảo của dấu hiệu cực trị không có trong danh mục); hai vế, và phần đứng trước «nếu», nói về cùng một khoảng; mỗi vế khớp trọn một mẫu có vị trí (chủ ngữ của «đổi dấu / không đổi dấu» là y' hay đạo hàm, vế điều kiện chỉ gồm dấu của y', vế đơn điệu chỉ gồm chủ ngữ hàm số; «khoảng đó» trỏ về khoảng đứng trước nó), không theo túi từ; định nghĩa điểm tới hạn viết theo một trong hai dạng của `_TH_DINH_NGHIA`. Không khớp thì máy tìm phản ví dụ trên bộ hàm mẫu: có thì `SAI` kèm phản ví dụ đúng mệnh đề đã viết, không có thì `KHONG_KIEM_DUOC`. Phủ định, lượng từ, điều kiện tại một điểm, phát biểu trên cả tập xác định, hai vế khác khoảng, từ máy không biết đều không bao giờ `DAT`. Cách làm này chặt hơn câu «không có phản ví dụ thì DAT» của ADR 013 phần Hệ quả (rà `math-verifier` trên #101: bộ mẫu không phủ được định lí tổng quát, ví dụ y = x − sin x).
- **Tầng 2:** chỉ dùng tài liệu có quyền dùng hợp lệ; `chua_ro` không làm căn cứ và được liệt kê trong `bo_qua`. Tài liệu có câu nói một điều là sai («Mệnh đề trên là sai.», «… hay nhầm …», «không đúng») cũng không làm căn cứ, kể cả các đoạn khác của nó (`bo_qua` với `ly_do = co_cau_phu_nhan`): máy không biết câu đó phủ nhận đoạn nào. Đẳng thức: có đoạn phát biểu trọn công thức của dòng — một mệnh đề dạng «nhãn: $công thức$» hay «$công thức$», công thức đóng khung (`$…$`, `$$…$$`, `\(…\)`, `\[…\]`) khớp trọn chứ không là phần của công thức dài hơn, đứng cuối mệnh đề, nhãn không phủ định hay nói sai — **và** mỗi câu của phát biểu trùng trọn một câu của đoạn. Định lí: **mọi** mệnh đề của dòng có đoạn phát biểu cùng mệnh đề (cùng điều kiện, cùng chiều); đoạn nói về một khoảng cụ thể không làm căn cứ cho định lí tổng quát. Trích dẫn chính ở `trich_dan`, các đoạn còn lại ở `trich_dan_them`.
- Core chỉ khóa bảng khi mọi dòng có `tang1` và `tang2` đều `DAT`; ngược lại trả 422 kèm danh sách dòng chưa qua.
