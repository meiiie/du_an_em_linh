# Câu quy tắc bằng lời trong lời gia sư: thêm từ vựng hay chuyển sang danh sách trắng theo câu

| | |
| --- | --- |
| Ngày | 2026-10-05 |
| Người làm | Claude Code, lab Quyết định (từ kết quả lab Kiểm định, #89) |
| Trạng thái | Chờ chủ repo quyết. ADR đi kèm: [014](../../docs/adr/014-danh-sach-trang-theo-cau.md) («Đề xuất») |
| Liên quan | ADR 013 (Chấp nhận), bản vá KD-0005 (#116), job `kiem-loi-giang` (#90), module tutor (#91) |

## 1. Câu hỏi quyết định

Cổng công thức của lời gia sư nhận ra câu phát biểu quy tắc bằng lời bằng một bộ từ vựng (ADR 013 mục 2). Hai lượt rà độc lập cho thấy từ vựng không theo kịp. Vậy tiếp tục thêm từ vựng (KD-0005 bản 3), hay kích hoạt điều khoản dự phòng của chính ADR 013: chuyển sang danh sách trắng theo câu?

## 2. Dữ kiện

ADR 013, mục «Điều làm quyết định này sai», ý 2: «Lọt lặp lại theo cách từ vựng không theo kịp → chuyển sang danh sách trắng theo câu: chỉ hiện câu không có thuật ngữ toán, câu trích dẫn dòng bảng hoặc câu gợi ý đã kiểm trước.»

Số đo (ghi chú lab `labs/evals/2026-10-03-bo-ca-loi-giang.md`; script của hai lượt rà ở scratchpad của phiên, không vào repo):

| Lượt đo | Tập câu | Lọt |
| --- | --- | --- |
| Bản 1, câu dò của `math-verifier` (2026-10-03) | 37 câu (sau tách câu) | 23 |
| Bản 2, cùng tập câu dò đó (tập này đã dùng để chọn từ thêm) | 34 câu sai hay đáp án | 0 |
| Bản 2, câu dò mới của `math-verifier` (2026-10-05) | 26 câu | 26 |
| Bản 2, câu dò mới của lượt rà «đội đỏ» (2026-10-05) | 75 câu | 61 (59 qua cả `/v1/filter`) |

Các họ câu bản 2 vẫn để lọt:
- từ nối thông dụng chưa có: «thành», «ra», «như», «có dạng», «y hệt», «theo kiểu», «chỉ có», «—»;
- câu kế thừa câu trước qua dấu «?»;
- viết tắt: «đh», «hs đb», «CĐ», «CT», «TXĐ», «ycđ»;
- ẩn dụ về đồ thị: «đỉnh», «đáy», «leo dốc», «lên», «xuống»;
- ký tự định dạng chèn giữa từ (ZWSP, gạch mềm);
- mục nhiều từ nuốt từ quan hệ bên trong («giá trị cực đại», «x mũ n»);
- bảng Markdown, gạch đầu dòng sau dấu «:».

Giá của các phương án, đo trên 276 câu gợi ý thật của 17 bài v0 (thang gợi ý trong tệp vàng T013) và trên các ca phải giữ của KD-0005:

| Phương án đo thử | Câu dò lọt | Câu gợi ý v0 bị bắt | Ca phải giữ bị vỡ |
| --- | --- | --- | --- |
| Bản 2 hiện tại | 61/75 | 62 % | 0 |
| Thêm mọi từ tìm được và 4 sửa luật, chỉnh theo đúng tập dò («đội đỏ» S1) | 14/75 | 88 % | 3 |
| Thêm 8 từ nối và «—» (`math-verifier`) | 8/26 | 64 % | 1 |
| Mọi câu có thuật ngữ là ứng viên («đội đỏ» S2, gần danh sách trắng) | 7/75 | 87 % | 11, nếu không có ngoại lệ cho câu chỉ chứa công thức đạt kèm `[n]` |

Câu dò còn lọt ở S2 đều không có thuật ngữ nào: viết tắt và ẩn dụ đồ thị. Thêm các thuật ngữ đó là việc có biên: thuật ngữ của chủ đề đếm được. Từ nối của tiếng Việt thì không đếm hết.

Thang gợi ý v0 cũng không qua cổng hiện tại. 62 % câu là ứng viên quy tắc ngoài bảng (phát biểu DD1, định nghĩa giá trị cực trị…), 48 % có toán viết trần; gộp lại 72 % câu không `DAT`. ADR 013 mục 6 chỉ dùng câu gợi ý `DAT` làm câu thay thế. Nghĩa là với bất kỳ phương án nào, thang gợi ý của v0 phải được lab Sư phạm viết lại, hoặc gần như mọi lần thay câu sẽ rơi về câu cố định không chứa toán.

## 3. Tiêu chí loại

- Đóng mặc định: câu không phân loại được thì không tới học sinh (hiến chương II, ADR 013).
- Đo được bằng bộ ca có mã (KD-0005) và chạy được trong cổng merge của `services/math`.
- Không mở đường «duyệt riêng một công thức» cho mô hình (ADR 013 mục 4).

## 4. Tiêu chí chấm (đặt trước khi chấm)

| Tiêu chí | Trọng số |
| --- | --- |
| An toàn: quy tắc sai hay ngoài bảng không tới học sinh, kể cả cách nói chưa gặp | 40 |
| Giữ được lời gia sư có ích (ngưỡng 20 % câu bị thay của ADR 013) | 25 |
| Tất định, kiểm được bằng bộ ca, đặc tả đủ để #90 hiện thực đúng chữ | 20 |
| Công sức và rủi ro lịch P2 | 15 |

## 5. Phương án

- **A. Giữ thiết kế, KD-0005 bản 3 thêm từ.** Thêm các họ từ đã tìm được, sửa luật (mục nhiều từ mang cả nhãn của mục con, «—» như «:», bỏ ký tự định dạng), thêm ca cho từng họ.
- **B. Danh sách trắng theo câu, đúng chữ ADR 013.** Một câu của gia sư chỉ tới học sinh khi:
  - **W1:** ngoài các đoạn toán đã `DAT` (công thức của bảng, trích nguyên văn đề hay bài làm) và dấu dẫn `[n]`, phần chữ không có thuật ngữ toán nào. Danh sách thuật ngữ của chủ đề gồm cả viết tắt và từ chỉ hình dạng đồ thị;
  - **W2:** câu trùng nguyên một câu phát biểu của dòng bảng đã khóa (có hay không có `[n]`);
  - **W3:** câu trùng nguyên một câu gợi ý đã kiểm trước của bước.

  Không cần từ quan hệ nữa. Từ vựng chỉ còn danh sách thuật ngữ, có biên.
- **C. B, và thang gợi ý do lab Sư phạm soạn được coi là «đã kiểm trước»** khi qua bản vá có mã và hai lượt rà độc lập. Không bắt chúng qua chính job này. Như vậy câu thay thế có nội dung, không chỉ là câu cố định. Cần một dòng mới trong ADR (mục 6 của ADR 013 đang bắt gợi ý qua cùng job).
- **D. Đổi kiến trúc lời gia sư:** mô hình không tự viết câu có toán. Nó chỉ chọn câu gợi ý đã duyệt, dẫn `[n]`, và viết phần động viên hay câu hỏi không có thuật ngữ. Cần ADR mới thay mục 2 của ADR 013.

## 6. Chấm điểm (1–5)

| Phương án | An toàn (40) | Có ích (25) | Tất định (20) | Công sức (15) | Tổng /100 |
| --- | --- | --- | --- | --- | --- |
| A | 2 | 3 | 3 | 4 | 55 |
| B | 4 | 2 | 4 | 4 | 70 |
| C | 4 | 3 | 4 | 3 | 72 |
| D | 5 | 2 | 4 | 2 | 72 |

Lý do các điểm then chốt:
- A an toàn 2: hai tập câu dò mới lọt 26/26 và 61/75. Mỗi bản vá chỉ đóng đúng các câu đã gặp. Đây đúng là tín hiệu mà ADR 013 đặt ra để chuyển hướng.
- B, C an toàn 4: câu dò còn lọt ở S2 đều không chứa thuật ngữ. Danh sách thuật ngữ có biên nên đóng được. Nhưng vẫn là một danh sách, nên không lên 5.
- B có ích 2: không có câu gợi ý nào qua được, nên câu thay thế gần như luôn là câu cố định. C có ích 3: câu thay thế lấy từ thang gợi ý đã duyệt.
- D an toàn 5, công sức 2: mô hình không còn phát biểu toán tự do. Phải thiết kế lại luồng gia sư (#91) trước khi làm.

## 7. Độ nhạy

Tổng = Σ (trọng số × điểm) / 5.
- An toàn 50, có ích 15 (chuyển 10 điểm sang an toàn): D 78, C 74, B 74, A 53.
- An toàn 30, có ích 35 (chuyển 10 điểm sang có ích): C 70, B 66, D 66, A 57.
- A không thắng ở phân bổ nào trong khoảng này: điểm an toàn 2 kéo xuống.

## 8. Khuyến nghị

**C**, trong khuôn khổ điều khoản dự phòng mà ADR 013 đã viết sẵn. Quyết định con:
1. Lab Kiểm định viết KD-0005 bản 3 theo W1–W3: danh sách thuật ngữ (thêm viết tắt, ẩn dụ đồ thị, chuẩn hóa vị trí dấu thanh, bỏ ký tự định dạng); ca cho từng họ câu đã lọt; giữ một phần câu dò làm tập đo riêng, không dùng để chọn từ.
2. Lab Sư phạm viết lại thang gợi ý của 17 bài v0 cho hợp W1–W3 (dẫn `[n]` thay vì phát biểu lại quy tắc), qua bản vá có mã và hai lượt rà độc lập.
3. #90 đo tỉ lệ câu bị thay trên lời gia sư thật theo ngưỡng 20 % của ADR 013. Vượt ngưỡng thì xem lại theo D.
4. Không phụ thuộc quyết định này, làm ngay:
   - vá lỗ của luật kiểm trong KD-0005: job để lại đoạn bị bỏ dưới dạng hiển thị khác chuỗi vẫn qua;
   - mở issue cho `/v1/filter`: 9 câu nói đáp án lọt cả hai lớp lọc.

## 9. Rủi ro và giảm thiểu

| Rủi ro | Giảm thiểu |
| --- | --- |
| Gia sư nói rất ít, học sinh thấy máy móc | Đo tỉ lệ câu bị thay ở #90; thang gợi ý viết lại có nội dung; chọn D nếu vượt 20 % |
| Danh sách thuật ngữ thiếu một từ | Tập câu dò riêng, tăng dần; lọt là P1, bản vá mới |
| Thang gợi ý được miễn job (C) có lỗi toán | Bản vá có mã, `math-verifier` và `pedagogy-reviewer` rà độc lập như dữ liệu lab khác |

## 10. Điều làm quyết định này sai

- Câu dò mới không chứa thuật ngữ nào vẫn mang được một quy tắc sai tới học sinh qua W1 (ví dụ viết hoàn toàn bằng ẩn dụ) → chuyển sang D.
- Đo ở #90 thấy trên 20 % câu bị thay dù thang gợi ý đã viết lại → xem lại cách dẫn `[n]`, hoặc D.
