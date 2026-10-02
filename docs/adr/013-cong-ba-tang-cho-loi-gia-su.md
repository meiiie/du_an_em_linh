# ADR 013 — Cổng 3 tầng cho công thức trong lời gia sư

**Trạng thái:** Đề xuất (2026-10-02) — chờ chủ repo duyệt trước khi hiện thực (spec P2, FR-015).

## Bối cảnh

Sơ đồ của khách đặt «kiểm tra công thức 3 tầng» (máy tự kiểm, kiểm qua tài liệu, đối chiếu bảng công thức đã cho) trên mũi tên đi **vào** «học cùng AI». Hiến chương II: mọi nội dung toán tới học sinh, kể cả công thức trong lời gia sư, qua cổng 3 tầng.

v0 đã có hai lớp cho lời gia sư:

- bộ lọc lộ đáp án `/v1/filter`: M3 theo ngữ cảnh, M1 khớp chuỗi;
- cổng 3 tầng `/v1/verify`, nhưng **chỉ cho đề bài** (`MUC-TIEU.md` §6, dòng C5).

Mô hình ngôn ngữ có thể bịa quy tắc đạo hàm, đổi điều kiện của định lí, hoặc tính hộ. Bộ lọc lộ đáp án bắt việc tính hộ khi con số dính ngữ cảnh đáp án. Nó không bắt một quy tắc sai không chứa đáp án.

## Phương án

| | Cách | Ưu | Nhược |
| --- | --- | --- | --- |
| A | **Thế giới đóng:** công thức tổng quát chỉ được hiện khi khớp một dòng của bảng đã khóa (tầng 3). Dòng đó đã máy kiểm lúc khóa (tầng 1) và có đoạn trích trong tài liệu được phép (tầng 2). Biểu thức trích nguyên văn bài làm của học sinh được phép. Mọi biểu thức khác bị bỏ. | Khớp sơ đồ (đủ 3 tầng); tất định; dễ kiểm bằng bộ ca; giáo viên kiểm soát được qua bảng | Gia sư nói ít công thức hơn; giáo viên phải bổ sung bảng khi thiếu |
| B | Máy kiểm + **một** nguồn (bảng hoặc tài liệu) cho mọi công thức tùy ý | Linh hoạt hơn | Không đủ 3 tầng; nhận dạng công thức tùy ý trong câu tiếng Việt khó, dễ lọt |
| C | Hiện mọi công thức, gắn nhãn «chưa kiểm» khi không qua | Gia sư tự nhiên nhất | Trái hiến chương II: công thức chưa kiểm vẫn tới học sinh |

## Quyết định (đề xuất)

Chọn **A**.

1. **Ba loại biểu thức trong câu gia sư:**
   - **công thức tổng quát** (quy tắc, định lí): phải khớp một dòng của bảng đã khóa, khớp theo SymPy với hàm ký hiệu (ví dụ quy tắc thương với `u(x)`, `v(x)`) hoặc theo phát biểu đã chuẩn hóa;
   - **trích bài làm của học sinh**: trùng nguyên văn một dòng học sinh đã nộp ở bài này, và câu trình bày nó là lời của học sinh («em viết …»);
   - **còn lại** (kết quả tính cụ thể, công thức ngoài bảng, LaTeX hỏng): bị bỏ.
2. **Cách nhận ra biểu thức:**
   - mọi đoạn toán trong `$…$`, `\(…\)`, `\[…\]`;
   - các mẫu phát biểu quy tắc bằng lời của chủ đề, dùng lại bộ nhận dạng của tầng 2 trong `services/math/app/verify.py` (đạo hàm dương / âm → đồng biến / nghịch biến, đổi dấu → cực trị);
   - prompt yêu cầu mô hình đặt mọi công thức trong `$…$` và chỉ dùng công thức của bảng kèm `[n]`. Prompt không thay cổng: cổng chạy trên mọi câu.
3. **Thứ tự trong lượt:** luật xin đáp án → mô hình → `/v1/filter` (lộ đáp án) → job mới `kiem_loi_giang` (cổng 3 tầng cho công thức) → hiện. Hai lớp lọc đều trong `services/math`, đúng ADR 011 (bộ lọc chỉ có một bản).
4. **Khi bỏ:** đoạn bị bỏ được rút khỏi câu. Câu còn lại vô nghĩa (không còn mệnh đề nào ngoài từ nối) thì thay cả câu bằng gợi ý theo thang của bước. Mỗi lần bỏ ghi một mục cho giáo viên: `SAI` nếu máy kiểm ra sai, `KHONG_KIEM_DUOC` nếu không khớp bảng. Giáo viên xử lý bằng cách sửa bảng rồi khóa phiên bản mới. Không có đường «duyệt riêng một công thức» trong P2.
5. **Đóng mặc định:** job lỗi hoặc hết giờ thì câu không hiện; học sinh nhận gợi ý theo thang.

## Hệ quả

- Job mới `POST /v1/kiem-loi-giang` (không trạng thái): đầu vào là câu đã qua lọc, các dòng bảng đã khóa (LaTeX, phát biểu, mã dòng, trích dẫn), các dòng bài làm của học sinh, dữ kiện bảo vệ của bài; đầu ra là câu đã làm sạch và phán quyết từng biểu thức.
- Bảng công thức: lúc khóa, mỗi dòng chạy tầng 1 (SymPy) và tầng 2 (tìm đoạn trong tài liệu được phép). Dòng không qua thì không khóa được bảng. Đây cũng là cách giáo viên «duyệt» công thức cho gia sư.
- Bộ ca lời giảng mới (≥ 100 câu, 5 loại; spec SC-004) vào `services/math/kiemdinh/` qua lab Kiểm định, chạy trong cổng merge của `services/math`.
- ADR 003 (gia sư không đọc lời giải) và ADR 010 (SSE trạng thái) giữ nguyên.

## Điều làm quyết định này sai

- Đo trên bộ ca thật thấy gia sư mất gần hết công thức có ích (trên 20 % câu bị thay bằng gợi ý thang vì bỏ công thức trong bảng) → xem lại bộ nhận dạng, không nới cổng.
- Khách muốn gia sư giải thích ngoài bảng công thức (ví dụ chứng minh một quy tắc) → cần một luồng «giải thích đã duyệt trước» do giáo viên soạn, không nới cổng cho mô hình.
