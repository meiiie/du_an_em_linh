# ADR 013 — Cổng 3 tầng cho công thức trong lời gia sư

**Trạng thái:** Chấp nhận (2026-10-02) — chủ repo duyệt phương án A «thế giới đóng» trong phiên làm spec P2 (FR-015). Rà PR #78 làm rõ, không nới cổng: trích nguyên văn đề bài được phép như trích bài làm; toán viết trần không phân loại được bị bỏ; câu thay thế cũng qua cổng từ trước; job riêng cho lúc khóa bảng.

## Bối cảnh

Sơ đồ của khách đặt «kiểm tra công thức 3 tầng» (máy tự kiểm, kiểm qua tài liệu, đối chiếu bảng công thức đã cho) trên mũi tên đi **vào** «học cùng AI». Hiến chương II: mọi nội dung toán tới học sinh, kể cả công thức trong lời gia sư, qua cổng 3 tầng.

v0 đã có hai lớp cho lời gia sư:

- bộ lọc lộ đáp án `/v1/filter`: M3 theo ngữ cảnh, M1 khớp chuỗi;
- cổng 3 tầng `/v1/verify`, nhưng **chỉ cho đề bài** (`MUC-TIEU.md` §6, dòng C5).

Mô hình ngôn ngữ có thể bịa quy tắc đạo hàm, đổi điều kiện của định lí, hoặc tính hộ. Bộ lọc lộ đáp án bắt việc tính hộ khi con số dính ngữ cảnh đáp án. Nó không bắt một quy tắc sai không chứa đáp án.

## Phương án

| | Cách | Ưu | Nhược |
| --- | --- | --- | --- |
| A | **Thế giới đóng:** công thức tổng quát chỉ được hiện khi khớp một dòng của bảng đã khóa (tầng 3). Dòng đó đã máy kiểm lúc khóa (tầng 1) và có đoạn trích trong tài liệu được phép (tầng 2). Biểu thức trích nguyên văn đề bài hoặc bài làm của học sinh được phép. Mọi biểu thức khác bị bỏ. | Khớp sơ đồ (đủ 3 tầng); tất định; dễ kiểm bằng bộ ca; giáo viên kiểm soát được qua bảng | Gia sư nói ít công thức hơn; giáo viên phải bổ sung bảng khi thiếu |
| B | Máy kiểm + **một** nguồn (bảng hoặc tài liệu) cho mọi công thức tùy ý | Linh hoạt hơn | Không đủ 3 tầng; nhận dạng công thức tùy ý trong câu tiếng Việt khó, dễ lọt |
| C | Hiện mọi công thức, gắn nhãn «chưa kiểm» khi không qua | Gia sư tự nhiên nhất | Trái hiến chương II: công thức chưa kiểm vẫn tới học sinh |

## Quyết định

Chọn **A**.

1. **Bốn loại biểu thức trong câu gia sư:**
   - **công thức tổng quát** (quy tắc, định lí): phải khớp một dòng của bảng đã khóa, khớp theo SymPy với hàm ký hiệu (ví dụ quy tắc thương với `u(x)`, `v(x)`) hoặc theo phát biểu đã chuẩn hóa;
   - **trích bài làm của học sinh**: trùng nguyên văn một dòng học sinh đã nộp ở bài này, và câu trình bày nó là lời của học sinh («em viết …»);
   - **trích đề bài**: trùng biểu thức của đề (hàm, tử và mẫu của nó) sau khi chuẩn hóa cách viết (khoảng trắng, `^` hay `**`, dấu nhân ẩn), **không rút gọn**: dạng đã biến đổi (khai triển, phân tích) không phải trích. Đề đã qua cổng 3 tầng trước khi phát hành và học sinh đã thấy nó; thang gợi ý mẫu chỉ điền đúng các chuỗi này (`{ham}`, `{tu}`, `{mau}`);
   - **còn lại** (kết quả tính cụ thể, công thức ngoài bảng, LaTeX hỏng): bị bỏ.
2. **Cách nhận ra biểu thức:**
   - mọi đoạn toán trong `$…$`, `\(…\)`, `\[…\]`;
   - các mẫu phát biểu quy tắc bằng lời của chủ đề, dùng lại bộ nhận dạng của tầng 2 trong `services/math/app/verify.py` (đạo hàm dương / âm → đồng biến / nghịch biến, đổi dấu → cực trị);
   - **mọi đoạn trông như toán nằm ngoài dấu phân cách** (dấu `=`, `≠`, `≤`, `≥`, `⇒`, `→`, dấu phẩy trên `'` / `′`, `^`, `/` giữa các ký hiệu, chữ biến kề toán tử hay chữ số, lệnh `\…`) mà không thuộc hai mẫu trên: coi là `KHONG_PHAN_TICH_DUOC`, bị bỏ như mọi biểu thức không kiểm được. Ví dụ `(uv)' = u'v'` viết trần bị bỏ, không lọt qua;
   - prompt yêu cầu mô hình đặt mọi công thức trong `$…$` và chỉ dùng công thức của bảng kèm `[n]`. Prompt không phải ranh giới an toàn: cổng chạy trên mọi câu và đóng mặc định với mọi thứ không phân loại được.
3. **Thứ tự trong lượt:** luật xin đáp án → mô hình → `/v1/filter` (lộ đáp án) → job mới `kiem_loi_giang` (cổng 3 tầng cho công thức) → hiện. Hai lớp lọc đều trong `services/math`, đúng ADR 011 (bộ lọc chỉ có một bản).
4. **Khi bỏ:** đoạn bị bỏ được rút khỏi câu. Câu còn lại vô nghĩa (không còn mệnh đề nào ngoài từ nối) thì thay cả câu bằng gợi ý đã kiểm trước của bước (mục 6). Mỗi lần bỏ ghi một mục cho giáo viên: `SAI` nếu máy kiểm ra sai, `KHONG_KIEM_DUOC` nếu không khớp bảng. Giáo viên xử lý bằng cách sửa bảng rồi khóa phiên bản mới. Không có đường «duyệt riêng một công thức» trong P2.
5. **Đóng mặc định:** job lỗi hoặc hết giờ thì câu không hiện; học sinh nhận gợi ý theo thang **đã kiểm trước**.
6. **Gợi ý thay thế cũng qua cổng, và kiểm trước:** thang gợi ý (của bài và thang mẫu `/v1/goi-y`) có chứa quy tắc toán. Khi khóa bảng công thức hoặc nhập bài, mỗi câu gợi ý chạy qua cùng job `kiem_loi_giang`; kết quả gắn với phiên bản bảng. Lúc chạy, câu thay thế chỉ lấy từ các gợi ý đã `DAT` với phiên bản bảng hiện tại, nên không cần gọi job lần nữa. Không còn gợi ý nào đạt thì dùng một câu cố định không chứa toán («Em xem lại bước này theo bảng công thức của lớp rồi thử lại, hoặc gửi thầy cô.»).

## Hệ quả

- Job mới `POST /v1/kiem-loi-giang` (không trạng thái): đầu vào là câu đã qua lọc, các dòng bảng đã khóa (LaTeX, phát biểu, mã dòng, trích dẫn), các dòng bài làm của học sinh, biểu thức của đề, dữ kiện bảo vệ của bài; đầu ra là câu đã làm sạch và phán quyết từng biểu thức.
- Job mới `POST /v1/kiem-dong-cong-thuc` (không trạng thái) cho lúc khóa bảng: đầu vào là các dòng nháp và đoạn của tài liệu được phép; đầu ra là loại dòng và phán quyết tầng 1, tầng 2 từng dòng.
- Bảng công thức: lúc khóa, mỗi dòng chạy tầng 1 và tầng 2. Chỉ khóa được bảng khi **mọi dòng đạt `DAT` ở cả hai tầng**; `SAI` hay `KHONG_KIEM_DUOC` ở dòng nào thì giáo viên phải sửa dòng đó hoặc bổ sung tài liệu. Đây cũng là cách giáo viên «duyệt» công thức cho gia sư.
  - Tầng 1 với dòng **đẳng thức** (quy tắc lũy thừa, tổng, thương…): SymPy kiểm tương đương, dùng hàm ký hiệu `u(x)`, `v(x)`.
  - Tầng 1 với dòng **định lí hay định nghĩa** thuộc loại máy đã biết (đơn điệu, cực trị, điểm tới hạn; bộ nhận dạng tầng 3 của `verify.py`): so phát biểu với ngữ nghĩa có sẵn của máy (chiều suy luận, điều kiện) rồi tìm phản ví dụ trên bộ hàm mẫu của chủ đề; không có phản ví dụ thì `DAT`, có thì `SAI`. Máy không chứng minh định lí; đây là kiểm nhất quán có giới hạn và được ghi rõ trong căn cứ.
  - Dòng thuộc loại máy chưa biết: `KHONG_KIEM_DUOC`, chặn khóa. Thêm loại mới là việc của lab Kiểm định.
  - Bảng 6 dòng của v0 (3 đẳng thức, 3 định lí hay định nghĩa) phải khóa được theo quy tắc này; nếu không, importer báo lỗi thay vì khóa thiếu.
- Bộ ca lời giảng mới (≥ 100 câu, 7 loại; spec SC-004) do lab Kiểm định soạn thành **bản vá có mã** (KD-0005, kèm SHA-256), rà độc lập; PR hiện thực chỉ áp nguyên văn bản vá vào `services/math/kiemdinh/`, rồi chạy trong cổng merge của `services/math`.
- ADR 003 (gia sư không đọc lời giải) và ADR 010 (SSE trạng thái) giữ nguyên.

## Điều làm quyết định này sai

- Đo trên bộ ca thật thấy gia sư mất gần hết công thức có ích (trên 20 % câu bị thay bằng gợi ý thang vì bỏ công thức trong bảng) → xem lại bộ nhận dạng, không nới cổng.
- Khách muốn gia sư giải thích ngoài bảng công thức (ví dụ chứng minh một quy tắc) → cần một luồng «giải thích đã duyệt trước» do giáo viên soạn, không nới cổng cho mô hình.
