# Bộ ca lời giảng và từ vựng quy tắc bằng lời (bản vá `KD-0005`)

| | |
| --- | --- |
| Trạng thái | bản 2 (2026-10-05): sửa 4 mục CHẶN và các mục NÊN SỬA của rà độc lập `math-verifier` trên bản 1; chờ rà độc lập bản 2 trước khi phát hành (T029) |
| Người làm | Claude Code, vai lab Kiểm định |
| Câu hỏi | Bộ ca nào chứng minh được «0 công thức sai hoặc không kiểm được tới học sinh» cho job `kiem-loi-giang` (spec SC-004, ADR 013)? |
| Issue | #89 (T029, epic #79); áp ở #90 (job `kiem-loi-giang`), chạy trong cổng merge của `services/math` |

## Vì sao cần

ADR 013 chọn «thế giới đóng»: công thức trong lời gia sư chỉ tới học sinh khi khớp một dòng của bảng đã khóa, hoặc là trích nguyên văn đề bài hay bài làm của học sinh. Mọi thứ khác bị bỏ, đóng mặc định. SC-004 đòi đo điều đó trên một bộ ca mới: tối thiểu 100 câu, đủ 8 loại. ADR 013 cũng giao bộ từ vựng nhận dạng quy tắc phát biểu bằng lời cho lab Kiểm định, nằm trong cùng bản vá.

v0 chưa có bộ nào như vậy: cổng 3 tầng của v0 chỉ chạy cho đề bài (`MUC-TIEU.md` §6).

## Ngữ cảnh của bộ ca

- **Bảng đã khóa**: 6 dòng của v0 (`data/v0/bang-cong-thuc.json`, mã `d-1` … `d-6`), lấy nguyên văn. Mỗi dòng ứng với một mục danh mục của `services/math/app/dong_cong_thuc.py` (#101):

  | Dòng | Mục danh mục |
  | --- | --- |
  | d-1 lũy thừa | `x^n` và câu «Hằng số có đạo hàm bằng 0.» |
  | d-2 tổng | `u+v` |
  | d-3 thương | `u/v` |
  | d-4 đơn điệu | DD2, cả chiều đồng biến lẫn nghịch biến |
  | d-5 cực trị | CT1 và CT2 |
  | d-6 điểm tới hạn | TH |

- **Ba bài**, chọn để phủ ba dạng hàm của chủ đề. Mỗi bài có dòng bài làm của học sinh và dữ kiện bảo vệ:
  - `y = x^3 - 3x^2 + 2` (bậc ba);
  - `y = (x^2 + 3)/(x - 1)` (hữu tỉ: có tử và mẫu để kiểm phần trích đề);
  - `y = x^4 - 2x^2` (trùng phương; bản 2 thêm dữ kiện `y(-1) = -1`).

## Cách định phán quyết

Phán quyết mong đợi suy từ ADR 013, không đọc mã của job (job chưa có). Mỗi biểu thức có:
- `loai`: `CONG_THUC_TONG_QUAT`, `QUY_TAC_BANG_LOI`, `TRICH_BAI_LAM`, `TRICH_DE_BAI`, `KET_QUA_CU_THE` hoặc `KHONG_PHAN_TICH_DUOC`, như contracts/math-v1.md;
- `trang_thai`: `DAT`, `SAI` hoặc `KHONG_KIEM_DUOC`;
- `dong_bang` khi khớp một dòng.

Khi ADR cho phép hai cách phân loại mà cả hai đều dẫn tới bỏ, ca ghi danh sách «một trong». Ví dụ: một định lí sai mà máy không đọc trọn thì `KHONG_KIEM_DUOC`; đọc được và có phản ví dụ thì `SAI`.

### Luật kiểm cho bộ chạy của #90 (bản 2 ghi rõ ở đầu `bo-ca-loi-giang.yaml`)

1. Ca `muc: phai_bo`: mỗi đoạn mong không `DAT` phải vắng khỏi `cau_sach`, và job phải trả cho nó `trang_thai` và `loai` thuộc giá trị mong đợi. Sai một điều là lỗi nghiệm thu, vì SC-004 đòi «0 tới học sinh».
2. Đoạn mong đúng một giá trị `DAT` đạt khi còn trong `cau_sach`, phán quyết `DAT`, đúng `loai`, đúng `dong_bang`. Tỉ lệ giữ tính **riêng** cho loại 1 (17 ca) và loại 7 (10 ca), mỗi loại ≥ 95 % (SC-004). Với cỡ này, ≥ 95 % nghĩa là không được trượt ca nào. Loại khác và đoạn `DAT` nằm trong ca `phai_bo`: báo cáo, không chặn.
3. `trang_thai: [DAT, KHONG_KIEM_DUOC]`: nhận cả hai. Nếu `DAT` thì `dong_bang` phải đúng. Không tính vào tỉ lệ (3 ca: hai câu diễn đạt lại một dòng, một câu dẫn sai `[n]`).
4. `thay_bang_goi_y`: so với cờ cùng tên của job (contracts/math-v1.md).
5. Đối chứng: câu không có biểu thức phải còn nguyên (báo cáo, đo giá «bắt thừa»); câu có biểu thức theo luật 1.

**Chuẩn hóa trích đề** (loại 7): gộp khoảng trắng, `^` ≡ `**`, dấu nhân ẩn ≡ `*`, bỏ ngoặc nhọn của số mũ, `\frac{a}{b}` / `\dfrac{a}{b}` ≡ `(a)/(b)`, bỏ «y =» đứng đầu. Không đổi thứ tự hạng tử, không rút gọn, không bỏ vế trái nào khác «y». Lab đọc danh sách «khoảng trắng, `^` hay `**`, dấu nhân ẩn» của ADR 013 mục 1 là ví dụ của «cách viết». Ba quy tắc thêm cũng chỉ là cách viết: job chỉ nhận `ham` (chuỗi SymPy), không nhận chữ đề, nên không có chúng thì 5/10 ca trích đề không khớp được. **Chờ chủ repo duyệt** (xem «Còn để ngỏ»).

### Các loại ca

| Loại (SC-004) | Số ca (phai_bo / nen_hien) | Phán quyết mong đợi |
| --- | --- | --- |
| 1. Công thức trong bảng có trích dẫn | 18 (0 / 18) | `DAT` kèm dòng bảng: nguyên văn LaTeX của dòng, LaTeX khác (`\frac`, `\dfrac`, `\left`, khoảng trắng), vế phải bằng theo SymPy, đổi chỗ hạng tử của tổng, hai công thức trong một câu. Bản 2 thêm: ba dấu phân cách `\(…\)`, `\[…\]`, `$$…$$`; một câu dẫn `[1]` mà công thức là dòng [2] (`[DAT, KHONG_KIEM_DUOC]`) |
| 2. Công thức đúng ngoài bảng | 14 (14 / 0) | `KHONG_KIEM_DUOC`, bỏ. Quy tắc tích, hiệu, k·u, hàm hợp, lượng giác, mũ, log, căn, DD1. Bản 2: giả thiết `x > 0` chỉ được `KHONG_KIEM_DUOC`, không được gắn `SAI` |
| 3. Công thức sai | 24 (24 / 0) | 10 đẳng thức máy #101 bác được: `SAI`. Căn và mũ (`\sqrt`, `e^{…}`) parser #101 chưa đọc: `SAI` hoặc `KHONG_KIEM_DUOC`. 2 định lí sai. Bản 2 thêm 10 ca: 7 biến thể gần của dòng định lí trong `$…$` (bỏ điều kiện, đổi ≥ thành ≤, đảo chiều dấu, đổi kết luận, tiền tố trùng dòng [5] rồi nối vế sai, chiều ngược thêm điều kiện chặt, đổi y' thành y ở dòng [6]), 2 chuỗi đẳng thức có vế đầu trùng dòng, 1 câu có công thức của bảng lẫn công thức sai |
| 4. LaTeX hỏng | 14 (14 / 0) | `KHONG_PHAN_TICH_DUOC`, bỏ. 9 ca KaTeX báo lỗi, 3 ca dấu phân cách hỏng (thiếu `$`, `\)`, `$$` đi với `$`), 2 ca biểu thức cụt |
| 5. Kết quả tính cụ thể của bài | 21 (19 / 2) | `KET_QUA_CU_THE`, bỏ. 2 ca «em viết $…$» trùng nguyên văn dòng học sinh: `TRICH_BAI_LAM DAT`. Bản 2 thêm 6 ca: «em viết» với công thức sai, với kết quả không có trong bài làm, với chuỗi mà dòng học sinh là tiền tố, với chuỗi con của dòng học sinh, một câu trộn một đoạn trùng và một đoạn không; và «3» là chuỗi con của đề mà không phải hàm, tử hay mẫu |
| 6. Toán viết trần ngoài dấu phân cách | 18 (18 / 0) | `KHONG_PHAN_TICH_DUOC`, bỏ, kể cả công thức của bảng viết trần (ADR 013 mục 2). Bản 2 thêm 4 ca ký hiệu Unicode `′`, `−`, `²`, `√` |
| 7. Trích nguyên văn đề bài | 15 (5 / 10) | 10 ca `TRICH_DE_BAI DAT`: hàm, tử, mẫu, sau chuẩn hóa. 4 ca dạng đã biến đổi bị bỏ. Bản 2: chữ quanh 5 câu viết lại cho không còn cặp thuật ngữ + từ quan hệ; thêm ca bẫy `$y' = <hàm của đề>$` phải bỏ |
| 8. Quy tắc phát biểu bằng lời | 50 (41 / 9) | 7 ca `DAT` khớp nguyên văn một câu phát biểu của dòng; 2 ca diễn đạt lại một dòng; 4 ca đúng ngoài bảng và 8 ca sai bị bỏ cả câu. Bản 2 thêm 29 ca, mỗi họ bản 1 để lọt (xem «Từ vựng»): câu sau kế thừa câu trước, từ nối, vị ngữ, câu không dấu, câu bảng sửa một chữ, đáp án nói bằng lời |
| Đối chứng (ngoài 8 loại) | 15 (6 / 9) | 4 câu không phải quy tắc và 5 câu gợi ý vô hại mà lượt bỏ dấu của bản 1 bắt nhầm: giữ nguyên. 1 câu hỏi gợi mở và 5 câu loại 7 cũ («Hàm số của đề là $…$», «Tử là $…$»…) là ứng viên quy tắc không khớp bảng: bỏ cả câu |

Tổng: **174 ca trong 8 loại** (135 `phai_bo`, 39 `nen_hien`; 187 biểu thức) cộng 15 ca đối chứng. Bản 1: 120 + 5.

## Từ vựng quy tắc bằng lời (bản 2)

`tu-vung-quy-tac.yaml` có 60 thuật ngữ và 78 từ quan hệ; 15 mục ở cả hai danh sách. `cach_dung` thành 8 luật, kèm mô phỏng tham chiếu `mo_phong_tu_vung.py` để #90 hiện thực đúng chữ:

1. Chỉ xét chữ **ngoài** dấu phân cách toán. Chữ trong `\text{}` đi đường tầng 1/2 (bản 1 không nói, nên LG-1-08 phụ thuộc cách hiểu).
2. Không tách câu ở dấu nằm trong ngoặc, như khoảng «(0; 2)», hay ở dấu chấm giữa hai chữ số.
3. Khớp cụm dài nhất, không chồng nhau. «nhân tử», «hạng tử», «tử số» là một thuật ngữ, không phải «nhân» + «tử».
4. Lượt không dấu chỉ áp cho từ viết toàn ASCII. Bản 1 bỏ dấu cả câu nên «bảng» thành «bằng», «hiểu» thành «hiệu», «màu» thành «mẫu», «đặt» thành «đạt».
5. Dấu hai chấm là từ quan hệ khi có thuật ngữ ở cả hai bên («Đạo hàm của tích: tích các đạo hàm.»), không khi sau nó chỉ có đoạn toán («Quy tắc thương [3]: $…$»).
6. Ứng viên = một cụm thuật ngữ và một cụm quan hệ khác nhau.
7. «nếu … thì» giữ theo ADR, không thêm phép khớp.
8. Ứng viên chỉ được giữ khi trùng **nguyên** một câu phát biểu của dòng bảng, hoặc tầng 2 đọc ra đúng mục danh mục. Không khớp mờ, không khớp tiền tố.

Từ thêm, theo ba họ câu mà rà độc lập thấy bản 1 để lọt:
- **câu kế thừa câu trước**: «cũng vậy», «cũng thế», «cũng như», «tương tự», «như vậy», «như thế», «giống», «giống như»;
- **từ nối**: «ứng với», «tương ứng», «đồng nghĩa», «nghĩa là», «tức», «tức là», «cho ra», «trùng», «trùng với», «cao hơn», «thấp hơn», «lớn nhất», «nhỏ nhất», «vì vậy», «vì thế», «do đó», «cho nên», «nên», «dẫn đến», «nếu», «lấy», «các đạo hàm», «luôn», «mọi», «triệt tiêu», «không xác định»;
- **vị ngữ ở cả hai danh sách**: «đồng biến», «nghịch biến», «đơn điệu», «cực trị», «cực đại», «cực tiểu», «điểm cực trị», «điểm cực đại», «điểm cực tiểu», «tăng», «giảm», «đi lên», «đi xuống». «mũ» và «lập phương» ở cả hai danh sách từ bản 1: vừa là danh từ («số mũ»), vừa là phép toán («x mũ n»).

Thuật ngữ thêm: «tử số», «mẫu số», «nhân tử», «thừa số», «hạng tử», «giá trị lớn nhất», «giá trị nhỏ nhất», «phương trình», «tam thức», «biệt thức», «đồ thị», «trục số», «trùng phương», «số mũ».

## Kiểm (bản 2, 2026-10-05)

Bản vá áp trên `main` `4d43cb1` vào một worktree sạch, chạy từ gốc repo, Python 3.13.7, SymPy 1.14.0, PyYAML 6.0.3.

- [x] **Căn cứ toán, bằng SymPy** (`kiem_bo_ca.py`): **167/167 đạt** (bản 1: 90/90).
  - Mỗi ca mới có phản ví dụ chạy thật: y = 1, y = −x, y = ±x², y = x³, y = x⁴, y = 1/x, y = |x|, bài hữu tỉ (cực đại −2 < cực tiểu 6, y → +∞ khi x → 1⁺), hàm ký hiệu u(x), v(x).
  - Bản 1 gõ cứng `True` cho «điểm tới hạn» và không chạy `kiem` của LG-6-14, LG-8-08…13. Bản 2 kiểm |x| bằng giới hạn một phía của tỉ sai phân (không dùng `diff(Abs)`, vì nó cho 0 sai) và chạy đủ.
  - Loại 6: mọi đoạn nằm trong câu, câu không có dấu phân cách.
  - Đột biến 4 ca trong YAML (LG-1-15, LG-3-19, LG-5-18, LG-8-22): mỗi lần đúng một căn cứ đỏ.
- [x] **Từ vựng, bằng mô phỏng tham chiếu** (`mo_phong_tu_vung.py`): 60/60 đoạn `QUY_TAC_BANG_LOI` là ứng viên và khớp hay không khớp bảng đúng mong đợi. Không câu phải giữ nào (trích đề, công thức của bảng, câu đối chứng vô hại) là ứng viên.
- [x] **Bộ câu dò của rà độc lập** (danh sách câu trong `vocab_sim.py`, cùng chữ). Chạy lại chính script đó trên bản 1: 23/37 câu lọt (sau khi script tách câu; báo cáo rà ghi 28/42). Bản 2: 0/34 câu sai hay đáp án lọt. Hai câu còn lại (36 câu, vì bản 2 không tách «(0; 2)») là câu đầu đúng của cặp hai câu, trùng dòng [2], được giữ đúng. Câu gợi ý vô hại bị bỏ: bản 1 8/15, bản 2 2/15. Hai câu còn bị bỏ đều có thuật ngữ + từ quan hệ thật: «… trước **khi** tính **đạo hàm**», «**Hàm số** của đề **là** …».
- [x] **Bóc từng từ**: bỏ riêng từng từ quan hệ mới, chạy lại. Hầu hết ca có hai từ bắt, nên bỏ một từ chỉ làm lọt 0–2 ca (`kd5-boc-tu.py` trong scratchpad của phiên, không vào bản vá).
- [x] **KaTeX 0.16.47** (`throwOnError: true`, `strict: "ignore"`): 119 đoạn có dấu phân cách; mọi đoạn ngoài loại 4 vẽ được; loại 4 báo lỗi 9/14 như ghi chú.
- [x] **Tái lập**: `tao_bo_ca.py` sinh lại hai tệp YAML trùng từng byte.
- [ ] Rà độc lập bản 2 (`math-verifier`, T029, ADR 013 phần Hệ quả).

### Giá «bắt thừa» trên câu gợi ý thật của v0

ADR 013 mục 6: câu gợi ý cũng qua job này lúc khóa bảng, và chỉ câu `DAT` mới được dùng làm câu thay thế. Lab đo trên thang gợi ý thật của 17 bài v0 (`specs/001-lat-cat-doc/doi-chieu/phan-hoi-toan.json`, T013): 125 chuỗi khác nhau, 276 câu.

| | Ứng viên quy tắc không khớp bảng |
| --- | --- |
| Bản 1 (luật và danh sách bản 1) | 164 câu (59 %) |
| Danh sách bản 1, luật bản 2 | 154 câu (56 %) |
| Bản 2 | 170 câu (62 %) |

Thêm vào đó, 133 câu (48 %) có dấu hiệu toán viết trần (ước lượng thô bằng regex: `'`, `=`, `^`, chỉ số trên…). Gộp hai đường: 198/276 câu (72 %) sẽ không `DAT`.

Đọc số này:
- Phần lớn giá đến từ định nghĩa ứng viên của ADR 013, không phải từ bản 2: các từ «là», «bằng», «khi», «thì», «chia», «gồm» có từ bản 1. Thang gợi ý của v0 phát biểu nhiều quy tắc ngoài bảng (DD1 «nếu y' > 0 thì đồng biến», định nghĩa giá trị cực trị) và viết toán trần (`(xⁿ)' = n·xⁿ⁻¹`).
- Luật bản 2 bớt 10 câu bắt nhầm; từ mới thêm 16 câu. Riêng từng từ mới thêm nhiều nhất 4 câu («lấy»).
- Đây không phải ngưỡng 20 % của ADR 013. Ngưỡng đó đo câu **gia sư** bị thay, trên bộ ca thật. Nhưng nó báo trước cho #90: với thang gợi ý của v0, phần lớn câu thay thế sẽ rơi về câu cố định không chứa toán.

## Bản vá

| | |
| --- | --- |
| Tệp | `labs/evals/ban-va/kd-0005.patch` (bản 2; 168 318 byte, LF) |
| SHA-256 | `8f4dda3eb2676924dc9eab23bb4a0aa4d6e75c8511c840374d1495cf16f76928` |
| Tạo ra | `services/math/kiemdinh/loi-giang/bo-ca-loi-giang.yaml`, `tu-vung-quy-tac.yaml`, `tao_bo_ca.py`, `kiem_bo_ca.py`, `mo_phong_tu_vung.py` |
| Áp (#90) | Kiểm SHA-256, rồi `git apply labs/evals/ban-va/kd-0005.patch`; ghi mã `KD-0005` trong tiêu đề commit; không sửa chữ khi áp. Bộ chạy của #90 đọc `bo-ca-loi-giang.yaml`, gọi job với ngữ cảnh của từng ca, so theo luật kiểm ở đầu tệp, ghi kết quả vào `services/math/kiemdinh/ket-qua/` và `NHAT-KY.md` |
| Thay | bản 1 (95 045 byte, SHA-256 `17a343a08ca98a4f6c557bc807d6bf60dbd77795303bb45f1bb6848488b561cf`, commit `1f38461`), bị rà độc lập chặn; chưa từng được áp |

Đã kiểm `git apply --check` và `git apply` trên `main` `4d43cb1`: không cảnh báo khoảng trắng, 5 tệp mới, không CR.

## Rà độc lập bản 1 và cách bản 2 xử lý

`math-verifier` rà bản 1 (2026-10-03): toán của mọi ca đúng (108 đúng, 24 sai cố ý, 6 không kiểm được), nhưng chưa phát hành được vì 4 lỗi chặn.

| Mức | Phát hiện | Bản 2 |
| --- | --- | --- |
| CHẶN | 5 ca loại 7 mong `DAT` nhưng chữ quanh có thuật ngữ + «là» của chính từ vựng: theo ADR phải bỏ cả câu | Viết lại 5 câu; câu cũ thành đối chứng «bắt thừa» mong bỏ cả câu |
| CHẶN | Từ vựng lọt câu dò (báo cáo: 28/42; chạy lại: 23/37): câu kế thừa, thiếu từ nối, vị ngữ chỉ là thuật ngữ | Thêm từ (mục «Từ vựng»), 29 ca loại 8, mô phỏng tham chiếu; lọt 0/34 |
| CHẶN | Không có biến thể gần của dòng định lí trong `$…$`, không có chuỗi trộn đúng với sai | 10 ca loại 3 |
| CHẶN | Không có ca «em viết $…$» với biểu thức không có trong bài làm | 5 ca loại 5 |
| NÊN SỬA | Chuẩn hóa trích đề cần thêm quy tắc ngoài danh sách của ADR; chưa có ca `y' = <hàm của đề>` | Ghi rõ danh sách, xin chủ repo duyệt; thêm ca bẫy |
| NÊN SỬA | `x > 0` được phép nhận `SAI` | Chỉ `KHONG_KIEM_DUOC` |
| NÊN SỬA | LG-3-09, -11 ghi `SAI` mà parser #101 chưa đọc `\sqrt`, `e^{…}` | `[SAI, KHONG_KIEM_DUOC]`; «12 đẳng thức» sửa thành 10 |
| NÊN SỬA | Chưa nói bộ chạy kiểm gì; ngưỡng 95 % trên 14 và 10 ca thực chất là 100 % | Luật kiểm 1–5 ở đầu YAML và ở trên |
| NÊN SỬA | `cach_dung` chưa nói chữ trong `\text{}`, `đ → d`, dấu `;` trong ngoặc | Luật 1, 2, 4 |
| NÊN SỬA | Lượt bỏ dấu bắt nhầm từ thường gặp | Luật 4; 5 câu đối chứng |
| NÊN SỬA | Thiếu họ ca: không dấu, Unicode viết trần, `\(…\)` `\[…\]` `$$…$$`, nhiều câu, câu bảng sửa một chữ, «đồng biến ⇒ y' > 0», đáp án là chuỗi con của đề, kết quả viết bằng lời | Đủ các họ, 54 ca mới |
| NÊN SỬA | `kiem_bo_ca.py` gõ cứng `True`; nhiều `kiem` không chạy | Kiểm đủ theo mã ca (167 căn cứ) |
| GỢI Ý | LG-1-09 ở bài hữu tỉ làm lộ chỗ dạng tóm tắt của d-6 thiếu «thuộc TXĐ» | Chuyển sang bài bậc ba; báo lab Sư phạm (để ngỏ 5) |
| GỢI Ý | Ghi chú «chiều ngược của dòng [4]» ở LG-8-12 không chính xác; «mũ», «lập phương» ở hai danh sách không giải thích | Sửa lời (DD3 là điều kiện cần, y = x − sin x); giải thích ở từ vựng |
| GỢI Ý | KaTeX `strict: "error"` báo lỗi chữ Việt trong `\text{}` | Ghi `strict: "warn"` ở đầu YAML |
| GỢI Ý | Bài trùng phương thiếu `y(-1) = -1`; LG-1-05 không có `[n]`; chưa có ca dẫn sai dòng; LG-DC-05 thiếu `thay_bang_goi_y` | Thêm dữ kiện; ghi chú LG-1-05 (cổng không đòi `[n]`); LG-1-18; thêm cờ |

## Còn để ngỏ

1. **Câu hỏi gợi mở có thuật ngữ.**
   - Câu như «Em kiểm tra xem đạo hàm có bằng 0 tại điểm đó không nhé» là ứng viên quy tắc không khớp bảng, nên bị bỏ cả câu (LG-DC-05).
   - #90 phải đo tỉ lệ câu bị thay trên thang gợi ý mẫu, theo ngưỡng 20 % ở «Điều làm quyết định này sai» của ADR 013. Số đo ở mục «Giá bắt thừa» cho thấy thang của v0 sẽ vượt xa.
   - Nếu vượt ngưỡng, chủ repo quyết cách sửa bộ nhận dạng. Ví dụ: câu hỏi không khẳng định thì không là ứng viên. Cách này có rủi ro: một quy tắc sai viết thành câu hỏi tu từ vẫn lọt. Không nới cổng khi chưa có quyết định.
2. **Câu chỉ nêu trích đề** («Hàm số của đề là $…$», «Tử là $…$») bị bỏ cả câu theo ADR và từ vựng (5 ca đối chứng). Muốn giữ thì cần chủ repo quyết và thêm một dòng cho ADR 013, rồi phát hành bản vá mới.
3. **Chuẩn hóa trích đề** (mục «Luật kiểm»): chủ repo duyệt ba quy tắc thêm (ngoặc nhọn số mũ, `\frac`, bỏ «y =»). Không duyệt thì 5 ca loại 7 đổi thành `[DAT, KHONG_KIEM_DUOC]` ở bản vá mới.
4. **DD1 bị bỏ.**
   - «Nếu đạo hàm dương trên một khoảng thì hàm số đồng biến» là cách nói phổ biến nhất, nhưng bảng v0 chỉ có DD2 (dòng [4]), nên câu đó bị bỏ (LG-8-11, LG-2-13). Thang gợi ý của v0 cũng dùng DD1.
   - Muốn gia sư nói được câu này thì lab Sư phạm thêm dòng DD1 vào bảng, kèm tài liệu căn cứ. Không nới cổng.
5. **Báo lab Sư phạm** (từ rà độc lập): LaTeX tóm tắt của d-4 không ghi «trên khoảng» (trên tập rời thì sai: y = −1/x); dạng tóm tắt của d-6 không ghi «thuộc tập xác định». Phát biểu bằng lời của hai dòng thì đủ.
6. **Đổi thứ tự hạng tử của đề.**
   - LG-7-13 (`2 - 3x^2 + x^3`) không phải trích, vì chuẩn hóa không đổi thứ tự.
   - Nếu chủ repo muốn chấp nhận đổi thứ tự thì cần một dòng bổ sung cho ADR 013, rồi phát hành bản vá mới.
7. **KD-0006: bộ AI 70 ca của v0 không có trong repo** (T029c).
   - `docs/KIEM-THU.md` ghi «70/70», chạy «bằng script cổng trên máy dựng» lúc bàn giao (29/09/2026, #48). Không tệp nào trong repo hay lịch sử git chứa bộ ca này.
   - Theo T029c, lab không soạn lại 70 ca rồi gọi là bộ của v0. Cần chủ repo hoặc nhóm v0 đưa nguồn.
