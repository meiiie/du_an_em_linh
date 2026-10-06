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

Thuần hàm, không đọc CSDL. Chạy **sau** `/v1/filter`. Mã: `services/math/app/loi_giang.py` (T028).

**Vào:**

```json
{
  "cau": "Em viết $3x^2-6x$, giờ em tìm nghiệm của nó nhé. Đạo hàm của tổng bằng tổng các đạo hàm [2]. Theo [3]: $(u/v)' = \\frac{u'v - uv'}{v^2}$. Ta có $y' = 3x^2 - 6x$. Nhớ là (uv)' = u'v'.",
  "bang_cong_thuc": [
    {"id": "d-2", "tieu_de": "Đạo hàm tổng", "latex": "(u+v)' = u' + v'", "phat_bieu": "Đạo hàm của tổng bằng tổng các đạo hàm.", "trich_dan": {"tai_lieu": "sp-tai-lieu-0001", "doan": "p-2"}},
    {"id": "d-3", "tieu_de": "Đạo hàm thương", "latex": "(u/v)' = (u'v - uv') / v^2", "phat_bieu": "Với thương, tử là u'v trừ uv', mẫu là v bình.", "trich_dan": {"tai_lieu": "sp-tai-lieu-0001", "doan": "p-3"}}
  ],
  "bai_lam_hoc_sinh": ["3x^2-6x"],
  "du_kien_bao_ve": [],
  "ham": "x**3 - 3*x**2 + 2",
  "timeout_s": 12
}
```

**Ra** (kết quả thật của job với đầu vào trên):

```json
{
  "cau_sach": "Em viết $3x^2-6x$, giờ em tìm nghiệm của nó nhé. Đạo hàm của tổng bằng tổng các đạo hàm [2]. Theo [3]: $(u/v)' = \\frac{u'v - uv'}{v^2}$.",
  "thay_bang_goi_y": false,
  "bieu_thuc": [
    {"doan": "3x^2-6x", "loai": "TRICH_BAI_LAM", "trang_thai": "DAT", "cau": 0},
    {"doan": "Đạo hàm của tổng bằng tổng các đạo hàm [2]", "loai": "QUY_TAC_BANG_LOI", "trang_thai": "DAT", "dong_bang": "d-2", "tang": {"1": "DAT", "2": "DAT", "3": "DAT"}, "cau": 1},
    {"doan": "(u/v)' = \\frac{u'v - uv'}{v^2}", "loai": "CONG_THUC_TONG_QUAT", "trang_thai": "DAT", "dong_bang": "d-3", "tang": {"1": "DAT", "2": "DAT", "3": "DAT"}, "cau": 2},
    {"doan": "y' = 3x^2 - 6x", "loai": "KET_QUA_CU_THE", "trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Kết quả tính cụ thể: …", "cau": 3},
    {"doan": "(uv)' = u'v'", "loai": "KHONG_PHAN_TICH_DUOC", "trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Toán viết trần ngoài $…$, không phân loại được.", "cau": 4}
  ],
  "cac_cau": [
    {"cau": "Em viết $3x^2-6x$, giờ em tìm nghiệm của nó nhé.", "giu": true},
    {"cau": "Đạo hàm của tổng bằng tổng các đạo hàm [2].", "giu": true},
    {"cau": "Theo [3]: $(u/v)' = \\frac{u'v - uv'}{v^2}$.", "giu": true},
    {"cau": "Ta có $y' = 3x^2 - 6x$.", "giu": false},
    {"cau": "Nhớ là (uv)' = u'v'.", "giu": false}
  ]
}
```

**Đầu vào.**

- `bang_cong_thuc`: các dòng của bảng đã khóa. `tieu_de` nên gửi như lúc khóa: thiếu nó thì dòng ký hiệu của điểm tới hạn (`y'=0 \text{ hoặc } y' \text{ không xác định}`) không đọc lại được, nên không dùng được.
- `bai_lam_hoc_sinh`: các dòng học sinh đã nộp ở bài này, nguyên văn.
- `ham`: hàm của đề, cú pháp SymPy như `/v1/grade`.
- `du_kien_bao_ve`: tùy chọn, job không đọc. Lọc lộ đáp án là việc của `/v1/filter` chạy trước. Mọi biểu thức `DAT` của job là dòng bảng, chuỗi của đề hay dòng học sinh tự viết, nên không mang dữ kiện mà học sinh chưa thấy.

**Đơn vị giữ hay bỏ là câu.** Câu tách tại `.` `;` `?` `!` và xuống dòng, trừ dấu nằm trong ngoặc (mọi dấu ngoặc Unicode), trong khoảng `(a; b)` hay `]a; b[`, trong đoạn toán hay dấu chấm giữa hai chữ số (luật 1 của KD-0005). Một câu còn trong `cau_sach` khi mọi biểu thức của nó `DAT` và nó không là ứng viên quy tắc bằng lời, hoặc khi cả câu khớp một dòng bảng (`QUY_TAC_BANG_LOI` `DAT`). Câu có một biểu thức không `DAT` bị bỏ cả câu. Chặt hơn ADR 013 mục 4 (rút riêng đoạn khỏi câu): rút một đoạn giữa câu có thể làm phần còn lại đổi nghĩa, ví dụ «Hàm số đồng biến khi $x > 1$.» còn lại «Hàm số đồng biến khi.».

**Loại biểu thức.**

| `loai` | `DAT` khi | Không `DAT` |
| --- | --- | --- |
| `CONG_THUC_TONG_QUAT` | Khớp một dòng dùng được: trùng LaTeX sau khi bỏ khoảng trắng; cùng `(E)' = R` theo SymPy với `u(x)`, `v(x)` ký hiệu; trùng vế phải của dòng; hoặc là mệnh đề định lí mà bộ đọc của `dong_cong_thuc.py` đọc trọn ra các mục cùng có ở một dòng. Dòng dùng được khi `trich_dan.tai_lieu` là chuỗi còn chữ sau khi bỏ khoảng trắng và tầng 1 tính lại ra `DAT` như lúc khóa. Kèm `dong_bang` và `tang`. | `SAI` khi máy có phản ví dụ (kèm `phan_vi_du`), còn lại `KHONG_KIEM_DUOC` |
| `QUY_TAC_BANG_LOI` | Câu (bỏ `[n]`) trùng nguyên một câu của `phat_bieu` của dòng dùng được sau chuẩn hóa luật 8 của KD-0005, hoặc bộ đọc định lí đọc trọn câu ra các mục cùng có ở một dòng. Khoảng cụ thể như `(0; 2)` không khớp khoảng tổng quát của dòng. | `SAI` khi bộ đọc đọc trọn và có phản ví dụ, còn lại `KHONG_KIEM_DUOC`; bỏ cả câu |
| `TRICH_DE_BAI` | Trùng `ham`, hoặc tử hay mẫu khi `ham` là phân thức, sau chuẩn hóa cách viết của KD-0005: khoảng trắng, `^` ≡ `**`, nhân ẩn, ngoặc nhọn của số mũ, `\frac{a}{b}` ≡ `(a)/(b)`, bỏ `y =` hay `f(x) =` đứng đầu. Không đổi thứ tự hạng tử, không rút gọn. Chỉ biểu thức có `x`. | dạng đã biến đổi là `KET_QUA_CU_THE` |
| `TRICH_BAI_LAM` | Trùng nguyên cả một dòng của `bai_lam_hoc_sinh` sau khi bỏ khoảng trắng, và chữ đứng ngay trước đoạn là lời của học sinh: «em / bạn (đã / vừa / có) viết / ghi / nộp / chép / ra / tính ra / tìm ra / tính được / tìm được (là / rằng / được)», «dòng em viết», «dòng của em». Đoạn nối ngay sau một đoạn trích bằng «,» «và» «hoặc» «rồi» cũng là trích. | `KET_QUA_CU_THE` |
| `KET_QUA_CU_THE` | không bao giờ | `KHONG_KIEM_DUOC` |
| `KHONG_PHAN_TICH_DUOC` | không bao giờ | `KHONG_KIEM_DUOC`: LaTeX hỏng (ngoặc không cân, `\left` / `\right` lẻ, lệnh lạ, `\frac` thiếu nhóm, biểu thức cụt); dấu phân cách không đóng (đoạn chạy tới hết câu trả lời) hay `$$` đóng bằng `$`; toán viết trần ngoài `$…$`, `$$…$$`, `\(…\)`, `\[…\]`; ký hiệu đứng riêng như `$x$`, `$y'(0)$` |

- Đoạn có dạng quy tắc (vế trái `(…)'`, chữ trong `\text{…}` hay mũi tên suy ra, ký hiệu chung ngoài `x`, `y`, `f`, `D`) chỉ `DAT` qua bảng, không bao giờ là trích, kể cả khi trùng dòng học sinh: job không trích lại một quy tắc sai học sinh đã viết để rồi câu xác nhận nó.
- Toán viết trần nhận theo danh sách trắng. Ngoài dấu phân cách, chỉ chữ Latin (cả tiếng Việt), chữ số, khoảng trắng, dấu câu của lời văn (`,` `.` `;` `:` `!` `?` `«` `»` `"` `“` `”` `…`) và các dấu tùy chỗ `(` `)` `+` `-` `*` `/` `'` là lời. Mọi ký tự khác là dấu hiệu toán: dấu quan hệ, mũi tên, `^`, `_`, `[ ]`, `{ }`, ngoặc Unicode như `⟨ ⟩`, dấu giống dấu bằng như `꞊` `゠` `⹀`, chữ không phải Latin, emoji, ký tự điều khiển. Dấu gạch Unicode (`‐` `‒` `–` `—` `−`…) quy về `-`. Dấu tùy chỗ là toán khi: phẩy trên dính sau chữ hay ngoặc, phép toán giữa hai toán hạng, `-` hay `+` trước chữ số (`−2`), chữ số kề chữ biến dính hay cách (`3x`, `3 x`), hàm áp lên đối số, khoảng `(a; b)`. Ký tự toàn khổ (`＝`, `２`) quy về dạng thường trước khi nhận. Câu có toán viết trần cũng được thử đường `QUY_TAC_BANG_LOI` trước khi bỏ, vì một dòng định lí của bảng có thể viết trần.
- Danh sách trắng chọn bỏ thừa: «bước 2 y như bước 1» (chữ số cách một chữ cái) và câu có emoji bị bỏ.
- Ký hiệu chung đứng riêng, viết trần hay trong `$…$`, không là biểu thức và không có phán quyết: `y'`, `f'(x)` đọc như thuật ngữ «đạo hàm» khi xét ứng viên quy tắc bằng lời (`y' âm giữa 0 và 2` là ứng viên, bị bỏ); `x_0`, `y(x_0)` là ký hiệu điểm, không tính là nội dung. `y'(0)`, `x_0 = 0` vẫn là biểu thức.
- Ứng viên quy tắc bằng lời theo luật 2–6 của KD-0005: câu có một cụm thuật ngữ và một cụm quan hệ khác nhau (khớp dài nhất, lượt không dấu, dấu hai chấm giữa hai thuật ngữ). Trước khi so, job bỏ ký tự định dạng (ZWSP, gạch mềm) và đổi dấu thanh kiểu mới của vần mở («luỹ», «hoà») về kiểu của từ vựng. Từ vựng tạm là KD-0005 bản 2 (#116) cho tới khi T029b áp bản vá.
- `doan`: ruột đoạn toán (không kèm dấu phân cách), đoạn viết trần, hay cả câu với `QUY_TAC_BANG_LOI` (bỏ dấu câu cuối). `cau`: chỉ số câu trong `cac_cau`.
- `trang_thai` ∈ `DAT`, `SAI`, `KHONG_KIEM_DUOC`. Chỉ biểu thức `DAT` còn lại trong `cau_sach`. Biểu thức `DAT` nằm trong câu bị bỏ vẫn được liệt kê, với `cac_cau[cau].giu = false`.
- `thay_bang_goi_y = true` khi không câu nào được giữ còn nội dung (chỉ còn từ nối, trợ từ, dấu dẫn `[n]`); khi đó `cau_sach = ""`. Core thay cả câu bằng gợi ý đã kiểm trước của bước (ADR 013 mục 6).
- Đóng mặc định: đầu vào sai kiểu (`loi = DAU_VAO_KHONG_HOP_LE`), quá giới hạn (`QUA_GIOI_HAN`: `cau` trên 4000 ký tự, trên 40 đoạn toán, trên 60 dòng bảng, trên 80 dòng bài làm hay dòng trên 400 ký tự, `ham` trên 200 ký tự), lỗi bên trong (`LOI_KIEM_TRA`): `cau_sach = ""`, `thay_bang_goi_y = true`, `bieu_thuc = []`.
- Core chỉ hiện `cau_sach` khi phản hồi có `cau_sach` là chuỗi, không có `loi` và `thay_bang_goi_y` là `false`. Hết giờ, sandbox lỗi hay JSON hỏng trả phong bì của sandbox, không có `cau_sach`: coi như lỗi.
- Core ghi mỗi biểu thức `SAI` hoặc `KHONG_KIEM_DUOC` thành một `verification_run` (`subject_kind = TUTOR_FORMULA`) vào hàng đợi duyệt.
- Lỗi, hết giờ, JSON hỏng → core không hiện câu; câu thay thế chỉ lấy từ gợi ý **đã qua job này từ trước** với phiên bản bảng hiện tại (ADR 013 mục 6), không có thì câu cố định không chứa toán.
- Cùng job chạy **trước** cho mọi câu gợi ý (thang của bài, thang mẫu) khi khóa bảng hoặc nhập bài; core lưu phán quyết theo (câu gợi ý, phiên bản bảng). Thang mẫu điền `{ham}`, `{tu}`, `{mau}` trong `$…$` thì mới là trích đề; điền trần là toán viết trần.

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
- **Tầng 2:** chỉ dùng tài liệu có quyền dùng hợp lệ; `chua_ro` không làm căn cứ và được liệt kê trong `bo_qua`. Tài liệu có câu nói một điều là sai («Mệnh đề trên là sai.», «… hay nhầm …», «không đúng») cũng không làm căn cứ, kể cả các đoạn khác của nó (`bo_qua` với `ly_do = co_cau_phu_nhan`): máy không biết câu đó phủ nhận đoạn nào. Đẳng thức: có đoạn phát biểu trọn công thức của dòng — một mệnh đề dạng «nhãn: $công thức$» hay «$công thức$», công thức đóng khung (`$…$`, `$$…$$`, `\(…\)`, `\[…\]`) khớp trọn chứ không là phần của công thức dài hơn, đứng cuối mệnh đề, nhãn (nếu có) khớp trọn một tên đã kiểm của chính quy tắc đó (`DANH_MUC_NHAN`) — **và** mỗi câu của phát biểu trùng trọn một câu của đoạn. Định lí: **mọi** mệnh đề của dòng có đoạn phát biểu cùng mệnh đề (cùng điều kiện, cùng chiều); đoạn nói về một khoảng cụ thể không làm căn cứ cho định lí tổng quát, đoạn «trên K» không làm căn cứ cho dòng «trên tập xác định»; chỉ mệnh đề mà mọi phần đều đúng theo danh mục mới làm căn cứ (một chiều của «⇔» sai thì bỏ cả câu). Trích dẫn chính ở `trich_dan`, các đoạn còn lại ở `trich_dan_them`.
- Core chỉ khóa bảng khi mọi dòng có `tang1` và `tang2` đều `DAT`; ngược lại trả 422 kèm danh sách dòng chưa qua.
