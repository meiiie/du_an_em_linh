# Bộ ca lời giảng và từ vựng quy tắc bằng lời (bản vá `KD-0005`)

| | |
| --- | --- |
| Trạng thái | đề xuất (2026-10-03): chờ rà độc lập `math-verifier` trước khi phát hành (T029) |
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
  - `y = x^4 - 2x^2` (trùng phương).

## Cách định phán quyết

Phán quyết mong đợi suy từ ADR 013, không đọc mã của job (job chưa có). Mỗi biểu thức có:
- `loai`: `CONG_THUC_TONG_QUAT`, `QUY_TAC_BANG_LOI`, `TRICH_BAI_LAM`, `TRICH_DE_BAI`, `KET_QUA_CU_THE` hoặc `KHONG_PHAN_TICH_DUOC`, như contracts/math-v1.md;
- `trang_thai`: `DAT`, `SAI` hoặc `KHONG_KIEM_DUOC`;
- `dong_bang` khi khớp một dòng.

Khi ADR cho phép hai cách phân loại mà cả hai đều dẫn tới bỏ, ca ghi danh sách «một trong». Ví dụ: một định lí sai mà máy không đọc trọn thì `KHONG_KIEM_DUOC`; đọc được và có phản ví dụ thì `SAI`.

Hai mức nghiệm thu:
- `muc: phai_bo` (85 ca trong 8 loại, thêm 1 ca đối chứng): mọi biểu thức không `DAT` không được còn trong `cau_sach`. Sai một ca là lỗi nghiệm thu, vì SC-004 đòi «0 tới học sinh».
- `muc: nen_hien` (35 ca trong 8 loại, thêm 4 ca đối chứng): biểu thức `DAT` nên còn trong câu. Tỉ lệ giữ của loại 1 và loại 7 phải ≥ 95 % (SC-004).
  - Diễn đạt lại một dòng bảng (2 ca) được giữ nếu bộ nhận dạng tầng 2 đọc ra đúng mục danh mục của dòng.
  - Không đọc được thì bỏ cả câu. Như vậy là chặt quá chứ không lọt, nên chỉ tính vào tỉ lệ.

| Loại (SC-004) | Số ca | Phán quyết mong đợi |
| --- | --- | --- |
| 1. Công thức trong bảng có trích dẫn | 14 | `DAT` kèm dòng bảng: nguyên văn LaTeX của dòng, LaTeX khác (`\frac`, `\dfrac`, `\left`, khoảng trắng), vế phải bằng theo SymPy, đổi chỗ hạng tử của tổng; hai công thức trong một câu |
| 2. Công thức đúng ngoài bảng | 14 | `KHONG_KIEM_DUOC`, bỏ. Quy tắc tích, hiệu, k·u, hàm hợp, lượng giác, mũ, log, căn, DD1; không phải `SAI` |
| 3. Công thức sai | 14 | `SAI` cho 12 đẳng thức máy bác được bằng hàm mẫu; 2 định lí sai thì `SAI` hoặc `KHONG_KIEM_DUOC`; đều bỏ |
| 4. LaTeX hỏng | 14 | `KHONG_PHAN_TICH_DUOC`, bỏ. 9 ca KaTeX báo lỗi, 3 ca dấu phân cách hỏng (thiếu `$`, `\)`, `$$` đi với `$`), 2 ca biểu thức cụt |
| 5. Kết quả tính cụ thể của bài | 15 | `KET_QUA_CU_THE`, bỏ: đạo hàm, nghiệm, giá trị cực trị, khoảng, TXĐ, một bước như `(x^3)' = 3x^2`, một số riêng lẻ. Thêm 2 ca đối chiếu: «em viết $…$» trùng nguyên văn dòng học sinh thì `TRICH_BAI_LAM DAT`; cùng biểu thức mà là lời khẳng định của gia sư thì bỏ |
| 6. Toán viết trần ngoài dấu phân cách | 14 | `KHONG_PHAN_TICH_DUOC`, bỏ, kể cả công thức của bảng viết trần (ADR 013 mục 2) |
| 7. Trích nguyên văn đề bài | 14 | 10 ca `TRICH_DE_BAI DAT`: hàm, tử, mẫu, sau chuẩn hóa khoảng trắng, `^` / `**`, dấu nhân, ngoặc nhọn. 4 ca dạng đã biến đổi bị bỏ: phân tích nhân tử, chia đa thức, đổi thứ tự hạng tử |
| 8. Quy tắc phát biểu bằng lời | 21 | 7 ca `DAT` khớp nguyên văn một câu phát biểu của dòng (sau chuẩn hóa hoa thường, dấu câu); 2 ca diễn đạt lại một dòng; 4 ca đúng ngoài bảng và 8 ca sai hay đổi nghĩa bị bỏ cả câu |
| Đối chứng (ngoài 8 loại) | 5 | 4 câu không phải quy tắc, giữ nguyên; 1 câu hỏi gợi mở bị bỏ theo ADR 013 (xem «Còn để ngỏ») |

Tổng: **120 ca trong 8 loại** cộng 5 ca đối chứng.

## Từ vựng quy tắc bằng lời

`tu-vung-quy-tac.yaml` có 40 thuật ngữ và 33 từ quan hệ, nghiêng về bắt thừa như ADR 013 yêu cầu:
- Thuật ngữ: đạo hàm, hàm số, tích, thương, tổng, hiệu, lũy thừa, nghiệm, dấu, đồng biến, cực trị, điểm tới hạn, tập xác định…
- Từ quan hệ: bằng, là, nhân, chia, cộng, trừ, dương, âm, đổi dấu, suy ra, nếu … thì, khi và chỉ khi, khi, thì…

Một câu có ít nhất một từ mỗi loại là ứng viên. Phép so chạy trên chữ thường đã chuẩn hóa NFC, theo ranh giới từ, và chạy thêm trên chữ đã bỏ dấu để bắt cả câu viết không dấu.

## Kiểm

- [x] **Căn cứ toán, bằng SymPy 1.14** (`kiem_bo_ca.py` trong bản vá): **90/90 đạt**.
  - Ba đẳng thức của bảng v0 đúng.
  - Loại 1: biểu thức được lấy đạo hàm trùng đúng một dòng, và vế phải bằng đạo hàm của nó (hàm ký hiệu `u(x)`, `v(x)`).
  - Loại 2: đúng và không trùng dòng nào. DD1 thử trên 3 hàm mẫu; đây không phải chứng minh.
  - Loại 3: mỗi đẳng thức sai có một hàm mẫu làm hiệu khác 0. Hai định lí sai có phản ví dụ `y = −x` và `y = x²`.
  - Loại 5: mỗi kết quả là kết quả đúng của bài. Chúng bị bỏ vì là kết quả tính, không vì sai.
  - Loại 7: dạng trích trùng đề sau chuẩn hóa. Dạng biến đổi bằng hàm của đề theo SymPy nhưng không trùng chuỗi.
  - Loại 8: câu `DAT` là một câu của phát biểu dòng. Mỗi quy tắc sai có phản ví dụ.
- [x] **Cấu trúc**: mọi `doan` nằm trong câu của ca. KaTeX 0.16 (`throwOnError: true`) báo lỗi 9/14 ca loại 4; 5 ca còn lại có ghi chú đúng là dấu phân cách hỏng hay biểu thức cụt.
- [x] **Tái lập**: `tao_bo_ca.py` sinh lại đúng hai tệp YAML từ `data/v0/bang-cong-thuc.json`.
- [ ] Rà độc lập `math-verifier` (T029, ADR 013 phần Hệ quả).

## Bản vá

| | |
| --- | --- |
| Tệp | `labs/evals/ban-va/kd-0005.patch` (bản 1; 95 045 byte, LF) |
| SHA-256 | `17a343a08ca98a4f6c557bc807d6bf60dbd77795303bb45f1bb6848488b561cf` |
| Tạo ra | `services/math/kiemdinh/loi-giang/bo-ca-loi-giang.yaml`, `tu-vung-quy-tac.yaml`, `tao_bo_ca.py`, `kiem_bo_ca.py` |
| Áp (#90) | Kiểm SHA-256, rồi `git apply labs/evals/ban-va/kd-0005.patch`; ghi mã `KD-0005` trong tiêu đề commit; không sửa chữ khi áp. Bộ chạy của #90 đọc `bo-ca-loi-giang.yaml`, gọi job với ngữ cảnh của từng ca, ghi kết quả vào `services/math/kiemdinh/ket-qua/` và `NHAT-KY.md` |

Đã kiểm `git apply --check` trên `main` `87b4e39`.

## Còn để ngỏ

1. **Câu hỏi gợi mở có thuật ngữ.**
   - Câu như «Em kiểm tra xem đạo hàm có bằng 0 tại điểm đó không nhé» có thuật ngữ đi cùng «bằng». Theo ADR 013 mục 2, đó là ứng viên quy tắc không khớp bảng, nên bị bỏ cả câu (ca `LG-DC-05`).
   - Thang gợi ý cũng đi qua cùng job (ADR 013 mục 6). Nhiều câu gợi mở của thang có dạng này, nên có thể bị thay hàng loạt.
   - #90 phải đo tỉ lệ câu bị thay trên thang gợi ý mẫu, theo ngưỡng 20 % ở «Điều làm quyết định này sai» của ADR 013.
   - Nếu vượt ngưỡng, chủ repo quyết cách sửa bộ nhận dạng. Ví dụ: câu hỏi không khẳng định thì không là ứng viên. Cách này cũng có rủi ro, vì một quy tắc sai viết thành câu hỏi tu từ vẫn lọt. Không nới cổng khi chưa có quyết định.
2. **DD1 bị bỏ.**
   - «Nếu đạo hàm dương trên một khoảng thì hàm số đồng biến» là cách nói phổ biến nhất, nhưng bảng v0 chỉ có DD2 (dòng [4]), nên câu đó bị bỏ (`LG-8-11`, `LG-2-13`).
   - Muốn gia sư nói được câu này thì lab Sư phạm thêm dòng DD1 vào bảng, kèm tài liệu căn cứ. Không nới cổng.
3. **Đổi thứ tự hạng tử của đề.**
   - `LG-7-13` (`2 - 3x^2 + x^3`) đọc «chuẩn hóa cách viết» của ADR 013 theo nghĩa chặt: chỉ khoảng trắng, `^` / `**`, dấu nhân ẩn. Vì thế đổi thứ tự không phải trích.
   - Nếu chủ repo muốn chấp nhận đổi thứ tự thì cần một dòng bổ sung cho ADR 013, rồi phát hành bản vá mới.
4. **KD-0006: bộ AI 70 ca của v0 không có trong repo** (T029c).
   - `docs/KIEM-THU.md` ghi «70/70», chạy «bằng script cổng trên máy dựng» lúc bàn giao (29/09/2026, #48). Không tệp nào trong repo hay lịch sử git chứa bộ ca này.
   - Theo T029c, lab không soạn lại 70 ca rồi gọi là bộ của v0. Cần chủ repo hoặc nhóm v0 đưa nguồn.
