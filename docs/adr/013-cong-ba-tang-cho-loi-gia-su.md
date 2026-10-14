# ADR 013 — Cổng 3 tầng cho công thức trong lời gia sư

**Trạng thái:** Chấp nhận (2026-10-02) — chủ repo duyệt phương án A «thế giới đóng» trong phiên làm spec P2 (FR-015). Rà PR #78 làm rõ, không nới cổng: trích nguyên văn đề bài được phép như trích bài làm; toán viết trần không phân loại được bị bỏ; câu thay thế cũng qua cổng từ trước; quy tắc phát biểu hoàn toàn bằng lời cũng phải khớp bảng; job riêng cho lúc khóa bảng.

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
   - **quy tắc phát biểu bằng lời**: câu có thuật ngữ toán của chủ đề (đạo hàm, hàm số, tích, thương, tổng, hiệu, lũy thừa, nghiệm, dấu, đồng biến, cực trị…) đi cùng từ chỉ quan hệ hay phép toán (bằng, là, nhân, chia, cộng, trừ, bình phương, lớn hơn, dương, âm, đổi dấu, suy ra, nếu … thì, khi và chỉ khi…) là **ứng viên quy tắc**, dù không có ký hiệu nào. Ví dụ «đạo hàm của tích bằng đạo hàm u nhân v trừ u nhân đạo hàm v». Ứng viên chỉ được giữ khi khớp phát biểu của một dòng bảng đã khóa (so sau chuẩn hóa khoảng trắng, hoa thường, dấu câu) hoặc khớp bộ nhận dạng tầng 2 ứng với một dòng bảng; còn lại là `KHONG_PHAN_TICH_DUOC` và **bỏ cả câu**. Danh sách thuật ngữ và từ quan hệ là dữ liệu của lab Kiểm định (nằm trong bản vá KD-0005), nghiêng về bắt thừa;
   - prompt yêu cầu mô hình đặt mọi công thức trong `$…$`, chỉ dùng công thức của bảng kèm `[n]`, và không tự diễn đạt lại quy tắc: dẫn `[n]` để học sinh mở đúng dòng bảng đã kiểm (FR-021). Prompt không phải ranh giới an toàn: cổng chạy trên mọi câu và đóng mặc định với mọi thứ không phân loại được.
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
  - Bảng 6 dòng của v0 (3 đẳng thức, 3 định lí hay định nghĩa) phải khóa được theo quy tắc này; nếu không, importer báo lỗi thay vì khóa thiếu. Tài liệu của v0 không phát biểu 3 quy tắc đẳng thức (lũy thừa, tổng, thương), nên lab Sư phạm bổ sung một tài liệu tự soạn (bản vá `sp-tai-lieu-0001`) làm căn cứ tầng 2.
- Bộ ca lời giảng mới (≥ 100 câu, 8 loại; spec SC-004) và bộ từ vựng nhận dạng quy tắc bằng lời do lab Kiểm định soạn thành **bản vá có mã** (KD-0005, kèm SHA-256), rà độc lập; PR hiện thực chỉ áp nguyên văn bản vá vào `services/math/kiemdinh/`, rồi chạy trong cổng merge của `services/math`.
- ADR 003 (gia sư không đọc lời giải) và ADR 010 (SSE trạng thái) giữ nguyên.

- Rủi ro còn lại: nhận dạng quy tắc bằng lời dựa trên từ vựng, không chứng minh được là bắt hết mọi cách diễn đạt. Rủi ro này được đo bằng loại ca «quy tắc phát biểu bằng lời» của KD-0005 (đúng ngoài bảng, sai, diễn đạt lại).

## Điều làm quyết định này sai

- Đo trên bộ ca thật thấy gia sư mất gần hết công thức có ích (trên 20 % câu bị thay bằng gợi ý thang vì bỏ công thức trong bảng) → xem lại bộ nhận dạng, không nới cổng.
- Một câu phát biểu quy tắc bằng lời không khớp bảng mà vẫn tới học sinh (trong bộ ca hay thực tế) → lỗi P1, bổ sung từ vựng bằng bản vá mới. Lọt lặp lại theo cách từ vựng không theo kịp → chuyển sang danh sách trắng theo câu: chỉ hiện câu không có thuật ngữ toán, câu trích dẫn dòng bảng hoặc câu gợi ý đã kiểm trước.
- Khách muốn gia sư giải thích ngoài bảng công thức (ví dụ chứng minh một quy tắc) → cần một luồng «giải thích đã duyệt trước» do giáo viên soạn, không nới cổng cho mô hình.

## Bổ sung 2026-10-14 — KD-0006: thu hẹp ứng viên quy tắc bằng lời

KD-0006 sửa bộ nhận diện trong `services/math/app/loi_giang.py`, giữ quyết định A và nguyên bộ assertion KD-0005. Không sửa dữ liệu kiểm định đã khóa. Cổng vẫn xét **cả câu**, tất định và đóng mặc định khi lỗi hoặc không phân loại được. Luật dưới đây mô tả vòng đầu; phần «Kiểm lại trên corpus mới» bổ sung nhận diện theo vai trò cú pháp.

### Luật nhận diện

- Giữ bộ thuật ngữ/quan hệ và cách khớp KD-0005 làm mặc định. Chỉ loại khỏi ứng viên khi nhận ra thao tác học sinh phải làm hoặc chỗ trống cần trả lời, và không còn quan hệ khẳng định chưa kiểm. Dữ liệu mẫu nằm trong `_CHO_TRONG`, `_QUAN_HE_MANH`, `_THAO_TAC` và các mẫu hỗ trợ của KD-0006.
- Các chỗ trống như «là tập nào», «bằng bao nhiêu», «đổi dấu thế nào», «dương trên khoảng nào»; câu hỏi tìm giá trị/chỗ nào làm hàm không xác định; chỉ dẫn «em hãy lấy đạo hàm», «trước khi», «bằng cách», «bằng [n]» không tự phát biểu một quy tắc. Chỉ che đúng cách hỏi/chỉ dẫn, giữ mệnh đề khác trong cùng câu để kiểm. Ví dụ «Đạo hàm của hằng số không xác định, em cần tìm điểm nào?» vẫn bị bỏ.
- **Không miễn theo dấu hỏi, «đúng không», «chưa» hay «em hãy».** Quan hệ bằng/là/nhân/chia/cộng/trừ, dấu, đơn điệu, cực trị, suy luận hoặc «nếu … thì» còn lại phải khớp bảng. Quy tắc sai, đúng nhưng ngoài bảng, diễn đạt lại không được bộ đọc xác nhận, và câu mơ hồ vẫn bị bỏ. Câu «Hàm đã cho là đa thức» trong corpus cũng bị bỏ; câu hỏi độc lập sau nó có thể giữ.
- Unicode và cách khớp không dấu theo KD-0005; từ có dấu phải khớp đúng từ, tránh nhập nhằng «bảng» với «bằng». Không gọi mô hình hoặc dùng xác suất.
- Thêm `KY_HIEU/DAT` cho tên `$x$`, `$y$` trong câu hỏi có chỗ cần trả lời rõ ràng, không có đẳng thức/giá trị. Giữ cách đọc `$y'$`, `$f'(x)$` như thuật ngữ đạo hàm hiện có. Không cho qua tên có chỉ số/đối số cụ thể như `$x_0$`, `$y'(0)$`. Ca KD-0005 «Em kiểm tra lại $x$ ở dòng đầu nhé.» vẫn đóng: chỉ dẫn này chưa xác định được ý nghĩa toán cần kiểm; không mở danh sách trắng cho mọi câu chứa tên biến.
- **Tên phương trình `$y' = 0$`:** chỉ cho qua khi đứng ngay sau «giải phương trình» hoặc «viết phương trình», trùng nguyên mảnh `y'=0` có ranh giới trong LaTeX của một dòng bảng dùng được. Phải có trích dẫn và kiểm lại tầng 1 đạt `DAT`; ghi mã dòng và ba tầng trong phán quyết. Không suy ra mảnh bằng SymPy, không biến đổi `0+0`, không cho qua `y'=1`, `x=0`, «ta có», «đạo hàm là», «đặt», hoặc chỉ «tìm nghiệm của». Cách gọi bằng lời «giải phương trình đạo hàm bằng không» cần cùng mảnh và dòng dùng được. Trong corpus, dòng `d-4` đáp ứng điều kiện; không coi `d-6` thiếu tiêu đề và không đạt kiểm lại là dòng dùng được.
- Tên phương trình chỉ xác định nhiệm vụ, không cung cấp nghiệm hoặc đạo hàm đã tính. Câu vẫn qua kiểm tra quy tắc và mọi đoạn khác: thêm `$x=3$`, giá trị số hoặc quy tắc sai vào cùng câu thì bỏ cả câu. Các chữ số viết trong lời văn cũng bị chặn, ngoại trừ chỉ số bước/mức/dòng và dấu dẫn `[n]`; số nằm trong trích nguyên văn đề/bài làm tiếp tục theo hợp đồng cũ.

### Số đo trên corpus

Nguồn: `services/math/tests/data/ban-nhap-glm-2026-10-14.json`, SHA-256 `7e861056065a594c9e57f0d1015e5116e6510476ee087c32e6c409fcb25f45b4`. Corpus có **45 bản nháp, 71 câu**, kèm hàm từng bài và bảng khóa. Đo trực tiếp bằng `kiem_loi_giang`, không tính câu thay thế hay kết quả sau retry.

Mốc trước sửa: mã tại SHA `96000f169c4c00c22efe42f80e7d2eebbc42f7b5`; nạp bản `git show HEAD:services/math/app/loi_giang.py` từ tệp tạm để so với bản KD-0006 trên cùng đầu vào. Bản mã sau sửa chưa commit có SHA-256 `591122c76ed4ec92b1c3138408f777ea4d775f99544f2aea71ec1a84db780003`. Tỷ lệ 63% được nêu ban đầu không phải số đo trên đúng corpus này.

| Số đo | Trước KD-0006 | Sau KD-0006 |
| --- | ---: | ---: |
| Bản nháp bị bỏ toàn bộ | 32/45 | 8/45 |
| Tỷ lệ bản nháp bị bỏ toàn bộ | 71,11% | **17,78%** |
| Câu bị bỏ | 58/71 | 25/71 |
| Phán quyết bỏ `QUY_TAC_BANG_LOI` | 53 | 20 |
| Phán quyết bỏ `KHONG_PHAN_TICH_DUOC` | 6 | 4 |
| Phán quyết bỏ `KET_QUA_CU_THE` | 15 | 10 |

Năm phán quyết kết quả cụ thể chuyển sang tên phương trình có căn cứ bảng; **không** chuyển giá trị số sang đạt. Bốn đoạn giá trị `$0$`, `$1$`, `$-2$` trong corpus đều còn bị bỏ, kể cả `$0$` dùng làm chỉ dẫn đặt đạo hàm bằng không. Tám bản còn bị bỏ toàn bộ chứa tên phương trình thiếu ngữ cảnh cho phép, khẳng định ngoài bảng hoặc nội dung mơ hồ; không giảm tỷ lệ bằng cách giữ những câu này.

### Kiểm tra chống lọt

`tests/test_loi_giang_kd0006.py` có **122 ca**, kiểm các câu giữ từ corpus, quy tắc sai trong câu hỏi/mệnh lệnh, quy tắc diễn đạt ngoài bảng, số/khoảng/nghiệm/dấu/cực trị cụ thể, «đáp án là», câu ghép cài quy tắc sai, khoảng trắng/Unicode, và tên phương trình khi bảng rỗng/thiếu mảnh/thiếu trích dẫn/sai tầng 1. Kiểm corpus nhận diện đoạn giá trị số độc lập với nhãn của cổng, buộc câu chứa chúng bị bỏ, buộc chúng không còn trong `cau_sach`, kiểm lặp kết quả tất định và buộc tỷ lệ bỏ toàn bộ ≤ 0,20.

Lệnh kiểm từ `services/math`: `.venv/bin/pytest -q tests/test_loi_giang_kd0006.py` và `.venv/bin/pytest -q` cho toàn bộ kiểm thử hiện có cùng KD-0006. Bảo đảm chống lọt là ràng buộc bắt buộc của cổng và được kiểm trên các bộ ca này; nhận diện từ vựng vẫn không phải chứng minh cho mọi cách diễn đạt tiếng Việt. Rủi ro còn lại và điều kiện chuyển sang danh sách trắng theo câu của ADR này giữ nguyên.

Điều kiện chạy ngày 2026-10-14: lệnh pytest trực tiếp kẹt trong `TestClient`/AnyIO; tái hiện cả với `/health` không qua SymPy. Stack trace cho thấy luồng gọi chờ callback trong khi vòng lặp chờ selector. Tệp tạm `/tmp/mathl-kd0006-runtime/sitecustomize.py` giới hạn thời gian mỗi lần `EpollSelector.select` chờ tối đa 0,01 giây để vòng lặp quan sát callback. Lệnh toàn bộ dùng `PYTHONPATH=/tmp/mathl-kd0006-runtime:. .venv/bin/pytest -q`; không đổi kiểm thử/assertion, handler HTTP, sandbox CAS hay tệp phụ thuộc của repo. Bộ KD-0006 trực tiếp chạy không cần cấu hình polling này.

Kết quả trên bản mã có SHA-256 nêu trên: **122/122 ca KD-0006 đạt** với lệnh trực tiếp; **2.129/2.129 ca toàn bộ đạt**, mã thoát 0, với lệnh có cấu hình polling tạm. Giữ nguyên 2.007 ca hiện có, gồm 159 ca `test_loi_giang.py`; không sửa/xóa assertion KD-0005. Không có đoạn giá trị số nào trong corpus hoặc ca đối kháng được giữ nhầm.

### Kiểm lại trên corpus mới — cùng ngày 2026-10-14

Vòng đầu còn bắt thừa trên prompt mới. Vòng tiếp phân biệt **thức của câu và vai trò của quan hệ**: thao tác học sinh cần làm, bổ ngữ danh từ, chỗ hỏi chưa có câu trả lời, lựa chọn chưa chốt, và mệnh đề có quan hệ được khẳng định. Các mẫu là cấu trúc cú pháp dùng lại với từ vựng của chủ đề; không so câu với danh sách bản nháp hay dùng ID của dòng/corpus để miễn kiểm.

- Dẫn `[n]` sau tên công thức/quy tắc là tham chiếu. Chỉ bỏ tên dẫn khi xét quan hệ; mọi nội dung nói lại sau nó vẫn phải khớp bảng. Dấu hỏi, «chưa», «em hãy» không cứu một quy tắc sai.
- Chỉ dẫn lấy/tính đạo hàm giữ chủ ngữ toán khi xét phần còn lại. Các bổ ngữ như loại kết luận hoặc tổng các đạo hàm **đó/này** được nhận là đối tượng thao tác khi không có bổ sung nguồn phép toán theo sau; toán hạng đi sau «cộng lại» vẫn được kiểm. Không xóa một phép toán nói lại quy tắc của tích/thương bằng cách chèn «đó/này». Một quan hệ chen giữa chủ ngữ và «theo chiều nào» vẫn được kiểm.
- `$x$`, `$y$` được giữ trong câu hỏi có chỗ cần trả lời hoặc chỉ dẫn dùng một biến bất kỳ trong khoảng; không mở cho chỉ dẫn mơ hồ «kiểm tra lại x ở dòng đầu», chỉ số hay đối số cụ thể.
- Điều kiện tổng quát `$y'=0$`, `$y'>0$`, `$y'<0$` được gọi tên sau giải/viết phương trình, xét/kiểm tra điều kiện hoặc trong tham chiếu nghiệm của một câu hỏi chưa trả lời. Phải có mảnh đạo hàm so với **0** trong LaTeX của dòng dùng được. Với dấu chặt, chiều dấu còn phải nhất quán với mệnh đề đơn điệu đã đọc và kiểm của dòng; bảng hiện tại `d-4` có mảnh không âm cùng hai chiều đơn điệu. Không suy ra giá trị khác, không rút gọn `0+0`, không chỉ tin phát biểu bằng lời khi LaTeX thiếu mảnh. Phán quyết ghi dòng và ba tầng.
- Tên bằng lời «giải phương trình đạo hàm bằng 0/không», «điểm đạo hàm bằng 0/không/triệt tiêu» cần cùng mảnh `y'=0` trong bảng. Tên điều kiện hoặc các chiều đổi dấu kèm kết luận đã chọn («y'>0 để kết luận nghịch biến», kể cả nối bằng «và/rồi») là quy tắc phải kiểm toàn câu. «Hay/hoặc» là lựa chọn chưa chốt; liệt kê bằng «và» hoặc dấu phẩy không miễn một kết luận đã chốt. Riêng động từ phân biệt/phân loại với nhiều loại là yêu cầu chọn loại, không khẳng định tất cả loại cùng đúng.
- Số đứng riêng trong `$…$` vẫn bị bỏ, kể cả `$0$` đứng sau «đạo hàm bằng»; không ghép các đoạn số thành điều kiện để lách cổng. Các giá trị sau «tại/ở/bằng/từ/đến/ra» hoặc dấu hai chấm cũng bị kiểm, gồm số viết bằng chữ. Khoảng, nghiệm, dấu và giá trị cực trị cụ thể giữ ràng buộc cũ.
- «Hàm đã cho là đa thức» vẫn là khẳng định ngoài bảng theo các ca cũ. Trong bản nháp có câu hỏi độc lập sau nó, câu hỏi còn lại có thể giữ; không miễn khẳng định bằng dấu hỏi của câu ghép.

Đo cùng hai đầu vào bằng `kiem_loi_giang`, với bảng và hàm của từng bản nháp, không tính retry/câu thay thế. Mốc trước vòng tiếp: `HEAD` **`17a638be4a8255daaa58dbe7d557eafed8229026`** (`loi_giang.py` lưu nguyên ở tệp tạm trước sửa). Mã sau vòng tiếp: SHA-256 **`023e532d8568b4220033dbcb3e1e1affe04b946ad1acf4dd2332b399b5a48eb3`**.

| Corpus | Bỏ toàn bộ trước | Bỏ toàn bộ sau | Câu bị bỏ trước → sau |
| --- | ---: | ---: | ---: |
| `ban-nhap-glm-2026-10-14.json` — 45 bản, 71 câu | 8/45 (17,78%) | **5/45 (11,11%)** | 25/71 → 19/71 |
| `ban-nhap-glm-2026-10-14b.json` — 42 bản, 73 câu | 21/42 (50,00%) | **8/42 (19,05%)** | 51/73 → 33/73 |

SHA-256 corpus đầu giữ `7e861056065a594c9e57f0d1015e5116e6510476ee087c32e6c409fcb25f45b4`; corpus mới `c47677bb477786ea69394b886c451688bed8e8e4cb61e08a58722a9cc6b43f7d`. Riêng 30 bản đầu của corpus mới: **10/30 → 6/30** (33,33% → 20,00%). Số 14/30 được báo ở đầu phiên không tái hiện trên mốc mã/đầu vào có SHA nêu trên; không dùng nó làm số đo trước sửa.

`tests/test_loi_giang_kd0006_tong_quat.py` bổ sung ca ngoài corpus cho các cấu trúc trên và ca đối kháng: quy tắc sai trong câu hỏi/chỉ dẫn dù có `[n]`, điều kiện có kết luận sai, giá trị `x=3`, khoảng `(3; +∞)`, đồng biến trên khoảng cụ thể, cực tiểu `-5`, số viết bằng chữ sau dấu hai chấm, và quan hệ sai chen vào chỗ hỏi. Kiểm bảng rỗng/thiếu mảnh/thiếu trích dẫn/sai tầng 1 cho cả ba điều kiện. Hai test corpus độc lập đều buộc tỷ lệ bỏ toàn bộ **≤20%**, tính tất định và không giữ câu có số đứng riêng trong `$…$`. Giữ nguyên toàn bộ tệp và assertion KD-0005/KD-0006 vòng đầu.

Kết quả trên SHA-256 mã vòng tiếp nêu trên: **204/204 ca KD-0006 đạt** với `.venv/bin/pytest -q -p no:warnings tests/test_loi_giang_kd0006.py tests/test_loi_giang_kd0006_tong_quat.py` (122 ca cũ + 82 ca mới: 19 ca giữ, 49 ca phải bỏ, 12 ca bảng không dùng được, 2 test corpus). **2.211/2.211 ca toàn bộ đạt**, mã thoát 0, với `PYTHONPATH=/tmp/mathl-kd0006-runtime:. .venv/bin/pytest -q -p no:warnings`. Lệnh trần gặp lại chờ `TestClient`/AnyIO tại `test_bao_mat_dich_vu.py::test_tran_timeout_20s`; stack trace nằm ở portal/callback và selector. Dùng đúng polling tạm đã mô tả ở vòng đầu, không đổi test cũ, HTTP handler, sandbox CAS hay phụ thuộc. Rà độc lập bổ sung các ca chèn «đó/này» để nói lại phép toán sai, điều kiện có kết luận đã chốt và kết quả «ra ba» sau tên phương trình; tất cả đều bị bỏ. **0 ca phải-bỏ được giữ nhầm** trong các bộ ca đã chạy; đây là số đo kiểm thử, không phải chứng minh bao phủ mọi cách diễn đạt.

## Bổ sung 2026-10-14 — chỉ model sinh lời thoại, sinh lại có giới hạn

Theo chỉ đạo hiện thực gia sư v2 của chủ repo, bổ sung này thay cơ chế thang/câu cố định ở mục 4–6 và phần bổ sung GLM cũ. **Mọi lời thoại**, kể cả từ chối xin đáp án hay câu hỏi cuối sau nhiều lần bị chặn, đều do `HintModel` sinh và qua hai cổng. Không đọc `hint_levels` để dựng ngữ cảnh tutor, không nhúng thang vào prompt, không có nguồn trả lời `THANG/CO_DINH/TU_CHOI`. Bộ phân loại xin đáp án/jailbreak chỉ thêm chỉ dẫn để model từ chối bằng lời của mình.

Mỗi câu được giữ nguyên tới `ContentWorkflow.sanitize`; chỉ phát nếu toàn câu được xác minh và trả nguyên văn. Rejection thêm feedback bằng mã vi phạm, bước và tiền tố đã đạt; tuyệt đối không đưa bản nháp bị chặn hoặc dữ kiện bảo vệ vào prompt. Tối đa hai lần viết lại, sau đó một lần yêu cầu đúng một câu hỏi Socratic ngắn, không ký hiệu/kết quả toán; câu cuối vẫn qua cổng. Phần đã phát không bị thay/xóa. Model không sẵn sàng, câu cuối bị chặn, lỗi vận chuyển/cổng hoặc hết deadline tạo `error` có thông báo giao diện, không câu gia sư mẫu và không đáp án. Adapter cổng phân biệt lỗi hạ tầng với rejection để tránh gọi model lặp khi cổng mất kết nối.

Prompt nền và ba cấp hỗ trợ ở `services/core/src/main/resources/prompts/`, tiếng Việt có dấu. Bốn mức học tập lấy từ mastery/ghi đè giáo viên theo kỹ năng của bài, khác cấp hỗ trợ 1–3. Đề, bảng, bài làm, lịch sử và summary nằm trong phần dữ liệu có quyền thấp; không lời giải chuẩn/dữ kiện bảo vệ. Khử định danh trước lưu câu hỏi và trước lời gọi ngoài; còn nghi định danh thì báo lỗi để sửa câu hỏi. Không log prompt, nháp hay summary.

`GIA_SU_TOKEN_BUDGET` mặc định **6000**, cấu hình từ 1000 đến 8192. Theo yêu cầu hiện thực, bộ đếm đầu vào dùng ước lượng ký tự/4 (cộng envelope và dự trữ đầu ra/feedback), tính lại trước từng lần sinh/regen/nén. Đây là ước lượng, không tokenizer chính xác; `gia_su_usage` phân biệt usage nhà trả về với estimate khi lỗi/hủy/không có usage. Đạt 75% ngân sách thì nén các lượt hoàn tất cũ qua cùng model, giữ ít nhất ba cặp gần nhất và câu hỏi mới. Không cắt system/công thức/câu hỏi để ép vừa.

Summary JSON có sáu trường văn bản `mucTieuBuoc,cachDaThu,loiDaDuocCham,cauHoiConMo,goiYDaDung,dieuDaSua` và `luotNguon` đúng danh sách/thứ tự nguồn. Kiểm schema/định danh; từng câu trong từng trường qua cùng hai cổng, rồi kiểm giới hạn `min(600, budget/10)` token. Lưu text JSON vào `tom_tat` cùng con trỏ/revision nguyên tử; giữ mọi lượt gốc. Revision đổi thì bỏ summary cũ. Nén lỗi giữ memory cũ nếu còn vừa ngân sách, vượt giới hạn thì báo lỗi; không thay bằng tóm tắt mẫu. Đề hoặc bảng đổi làm mất hiệu lực summary/context cũ; lịch sử vẫn giữ bản gốc.

Unit/mock kiểm giao thức và đường lỗi; Testcontainers kiểm persistence/migration trên PostgreSQL. Những kiểm này không thay eval GLM thật hoặc kiểm toán/sư phạm độc lập; không sửa dữ liệu lab đã khóa.

Bằng chứng core tại mốc nền `b9d50f32dce34eeb0f43cba0c552dcb9ab585828`, snapshot nguồn SHA-256 `716262789c73984d0ea5272f4cc67b32bcc6e49f0099e2de1c6643504e95d9da`: lệnh, 360 ca đạt, 54 ca ArchUnit, các ca Docker/seed bị skip và lỗi socket của full verify được ghi trong [phụ lục kiểm ADR 010](010-sse-trang-thai-gia-su.md#bằng-chứng-backend). Không suy ra số đo sư phạm/GLM từ test mock.

## Bổ sung 2026-10-14 — phát câu sớm, nén và hủy hợp tác

Câu tiếng Việt được kiểm ngay khi dấu kết thúc tới, không chờ EOF; giữ delimiter LaTeX, số thập phân và không tính dấu câu nối tiếp thành câu mới. Hai cổng phải trả nguyên văn trước khi lưu `delta`; `SseEmitter.send` phát/flush từng sự kiện trong lúc model còn stream. Không đổi cổng toán/chống lộ của `services/math`.

Feedback phân biệt định danh/ngôn ngữ/dẫn bảng và hình thức toán/số/khẳng định chưa xác minh, không chứa nháp bị chặn. Prompt theo cấp và tín hiệu jailbreak ưu tiên hỏi thao tác hiện tại. Lần Socratic cuối dùng prompt riêng chỉ mang tên bước, yêu cầu một câu hỏi ngắn không số/kết quả; vẫn qua hai cổng, không câu cố định.

Nén từ **70%** ngân sách ước lượng toàn prompt, tính cả system và mọi lượt cũ đã kết thúc (kể cả lỗi/hủy); giữ ba lượt gần nhất và mọi bản gốc. Lịch sử API xuất `session.tomTat`, `session.summaryThrough`, `session.tokenCount`. Hủy/lỗi SSE/deadline dùng cờ hợp tác và đóng HTTP, không `Thread.interrupt`/`Future.cancel(true)` vào kiểm cổng/JDBC. Trước khi có header HTTP, cờ chặn callback và timeout vận chuyển giới hạn yêu cầu còn chờ.

Kiểm JDK 25.0.4.1 trên nền `77ca4624a9cb6d8e1a5b83782e9680bc32adfd82`; SHA-256 snapshot các tệp core đã đổi, gồm prompt mới: `632377e7563464f47612dea9324383552b850f686f9380031ada1b72b5f7b016` (ghép theo đường dẫn tăng dần: tên + NUL + nội dung + NUL). `./mvnw -B -q -Dtest='*Tutor*,*GiaSu*,*Sentence*,*Glm*,Architecture*' -Dsurefire.failIfNoSpecifiedTests=false test`: **71 đạt, 9 skip Docker**; thêm `-DargLine=-javaagent:<mockito-core-5.23.0.jar>` vì sandbox chặn tự attach. `-Dtest='*Architecture*,KienTruc*,NullMarkedPackagesTest'`: **54 đạt**. Runtime tạm SymPy 1.14.0: **362 ca công thức + 60 ca chống lộ đạt**; chạy ba tệp `test_loi_giang*.py` KD-0006 với `-k 'not router_kiem_loi_giang'`, và `test_leakfilter_fail_closed.py`, `test_leak_san_pham.py`, `test_leak.py`. Ca router đã dừng do chờ socket. Sandbox chặn PostgreSQL/math bind socket; chưa chạy lại eval GLM v2, chưa xác nhận lỗi ≤2/30, regen <25% hay TTFT thật.


## Bổ sung 2026-10-14 — nén sau `done`, lời thao tác và đường nhanh không toán

Theo yêu cầu chủ repo, bổ sung này thay thời điểm nén trong lượt ở các bổ sung trước. **Không nới `services/math/kiem_loi_giang`; 0 lộ đáp án là điều kiện cứng.**

- Trong lượt hỏi, không gọi model summary: chỉ dùng `tom_tat` đã kiểm và cửa sổ trượt các lượt kết thúc. Bỏ lượt cũ nhất khỏi prompt khi cần, dự trữ 600 token cho đầu ra/feedback; không cắt system, đề, bảng, câu hỏi hoặc summary. Không vừa sau khi bỏ toàn bộ cửa sổ thì báo lỗi giới hạn; giữ mọi bản gốc trong DB.
- Sau khi lưu sự kiện `done` với trạng thái `completed`, một virtual thread riêng xét nén từ 70% ngân sách toàn ngữ cảnh. Khóa độc quyền theo phiên dùng `putIfAbsent/remove`; đang nén thì bỏ qua lịch nén mới, không chặn lượt hỏi tiếp. Giữ ít nhất ba lượt kết thúc gần nhất, gồm lượt vừa xong; lượt lỗi/hủy cũ vẫn là nguồn lịch sử. Prompt summary quá dài thì thu nhỏ nhóm lượt cũ cần nén.
- Nén có cờ hủy và timeout riêng, mặc định 30 giây (`mathl.gia-su.compaction-timeout-seconds`, từ 1 đến 120). Hủy hợp tác/đóng HTTP, không ngắt thread có thể đang ghi JDBC. Lỗi/hết giờ/từ chối summary giữ memory cũ, không phát thêm hay sửa terminal của lượt đã xong. Usage summary ghi riêng với `purpose=summary`, dùng usage nhà hoặc estimate nếu không nhận được usage; không cộng vào token/thời gian trong `done` của lượt trả lời.
- Summary vẫn kiểm schema, nguồn/thứ tự, định danh, giới hạn token và **từng câu qua đầy đủ cổng hiện có**, kể cả câu thuần lời. Kiểm lại quyền theo lớp, đề/bảng và nhà đã chọn trước khi gọi model/kiểm câu/lưu. Lưu bằng CAS revision hiện có; một lượt hoặc context mới làm revision đổi thì summary đang nén không được ghi đè memory mới.
- Prompt lượt thường bắt đầu bằng đúng một câu định hướng ngắn không có nội dung toán, rồi tối đa hai câu gợi ý. Gợi ý mô tả thao tác bằng lời (lấy đạo hàm từng hạng tử, xét dấu trên từng khoảng), không thực hiện hộ, không biểu thức suy ra, số, nghiệm, dấu cụ thể hay kết luận. Chỉ có thể trích nguyên văn toàn bộ hàm đúng cách viết trong đề và dẫn `[n]` thật trong bảng; không chép công thức hoặc bài làm. Prompt Socratic cuối giữ ngoại lệ đúng một câu hỏi, không câu định hướng. Feedback viết lại nêu cách sửa cụ thể theo từng mã vi phạm, không đưa nháp bị chặn vào prompt.
- Đường nhanh chỉ áp dụng câu trả lời thường sau kiểm hình thức/định danh/ngôn ngữ/dẫn bảng: chỉ chữ Latin và dấu câu thông thường, không chữ số, ký hiệu toán, LaTeX hoặc chữ biến Latin đơn. Chuẩn hóa Unicode/hoa thường/dấu để kiểm từ vựng kết luận/đáp án: đồng biến, nghịch biến, cực đại, cực tiểu, nghiệm, bằng, dương, âm, tăng, giảm, lớn hơn, nhỏ hơn, khoảng, giá trị, kết quả, đáp án, tập xác định, đạo hàm là; chặn thêm quan hệ/quy tắc và số viết bằng chữ theo hướng bắt thừa. Để không lọt khẳng định/đáp án gián tiếp mà blacklist chưa bao phủ, còn yêu cầu câu phải khớp nguyên câu trong tập nhỏ câu định hướng/câu hỏi trung tính đã duyệt, sau chuẩn hóa NFC, hoa thường và khoảng trắng; không có chỗ điền nội dung tùy ý. Đây chỉ là tập nhận dạng điều kiện bỏ cổng, không phải nguồn câu trả lời hay fallback. Mọi câu không khớp, ký hiệu lạ hoặc từ bị chặn đều chuyển qua cổng hiện có, không tự đánh giá đúng/sai bằng Java. Câu Socratic cuối và summary không dùng đường nhanh. Trước khi phát vẫn kiểm quyền, đề/bảng, nhà và hủy như trước.

Unit test kiểm thứ tự `done`/nén, lượt kế không bị chặn, khóa theo phiên, timeout/usage riêng, revision/đề đổi, summary bị cổng từ chối và các câu đối kháng phải đi qua cổng. Đây là kiểm giao thức với model/cổng mock; không suy ra số đo GLM thật hoặc chứng minh mọi cách diễn đạt đều an toàn.
