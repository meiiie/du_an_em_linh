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

KD-0006 sửa bộ nhận diện trong `services/math/app/loi_giang.py`, giữ quyết định A và nguyên bộ assertion KD-0005. Không sửa dữ liệu kiểm định đã khóa. Cổng vẫn xét **cả câu**, tất định và đóng mặc định khi lỗi hoặc không phân loại được.

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
