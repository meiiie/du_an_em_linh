# ADR 014 — Danh sách trắng theo câu cho lời gia sư

**Trạng thái:** Đề xuất (2026-10-05). Chờ chủ repo duyệt. Phân tích: [`labs/decisions/2026-10-05-cau-quy-tac-bang-loi.md`](../../labs/decisions/2026-10-05-cau-quy-tac-bang-loi.md). Nếu được duyệt, ADR này thay mục 2 (cách nhận ra quy tắc phát biểu bằng lời) và mục 6 (gợi ý thay thế) của ADR 013. Các mục khác của ADR 013 giữ nguyên.

## Bối cảnh

ADR 013 nhận ra câu phát biểu quy tắc bằng lời bằng một bộ từ vựng: câu có thuật ngữ đi cùng từ quan hệ là ứng viên. Ứng viên chỉ được giữ khi khớp bảng công thức đã khóa. Mục «Điều làm quyết định này sai» của ADR 013 đã viết sẵn: lọt lặp lại theo cách từ vựng không theo kịp thì chuyển sang danh sách trắng theo câu.

Tín hiệu đó đã xảy ra (#89, KD-0005):
- bản 1 lọt 23/37 câu dò;
- bản 2 thêm đúng các từ đó;
- hai tập câu dò mới vẫn lọt 26/26 và 61/75.

Từ nối của tiếng Việt không đếm hết được. Thuật ngữ của chủ đề thì có biên.

Cũng theo số đo đó, 72 % câu trong thang gợi ý của v0 không qua cổng của ADR 013. Mục 6 của ADR 013 lại chỉ dùng câu gợi ý đã qua cổng làm câu thay thế.

## Quyết định

1. **Danh sách trắng theo câu.** Một câu do mô hình viết chỉ tới học sinh khi đạt ít nhất một điều sau:
   - **W1:** bỏ các đoạn toán đã `DAT` (công thức của bảng, trích nguyên văn đề hay bài làm theo ADR 013 mục 1) và các dấu dẫn `[n]`, phần chữ còn lại không chứa thuật ngữ toán nào;
   - **W2:** câu trùng nguyên một câu phát biểu của một dòng bảng đã khóa, có hay không có `[n]`, sau chuẩn hóa khoảng trắng, hoa thường, dấu câu;
   - **W3:** câu trùng nguyên một câu gợi ý đã duyệt của bước.

   Câu còn lại bị bỏ cả câu. Mục 4 và 5 của ADR 013 (ghi cho giáo viên, đóng mặc định) giữ nguyên. Không còn danh sách từ quan hệ.
2. **Danh sách thuật ngữ** là dữ liệu của lab Kiểm định (bản vá KD-0005 bản 3), nghiêng về bắt thừa, gồm:
   - thuật ngữ của chủ đề;
   - viết tắt («đh», «TXĐ», «CĐ», «CT»…);
   - từ chỉ hình dạng đồ thị («đỉnh», «đáy», «đi lên»…).

   Trước khi so: chuẩn hóa NFC và vị trí dấu thanh, bỏ ký tự định dạng (loại Unicode Cf). Lượt so không dấu chỉ áp cho từ viết không dấu.
3. **Gợi ý đã duyệt** là thang gợi ý do lab Sư phạm soạn, phát hành qua bản vá có mã và hai lượt rà độc lập (`math-verifier`, `pedagogy-reviewer`). Không bắt chúng qua job `kiem-loi-giang`. Câu thay thế lấy từ đây. Không còn câu gợi ý nào cho bước thì dùng câu cố định không chứa toán như ADR 013.
4. **Prompt** yêu cầu mô hình dẫn `[n]` thay vì phát biểu lại quy tắc, như ADR 013. Prompt vẫn không phải ranh giới an toàn.

## Hệ quả

- KD-0005 bản 3 viết theo W1–W3, kèm tập câu dò giữ riêng (không dùng để chọn từ).
- Job `kiem-loi-giang` (#90) bỏ phần từ quan hệ; các phần khác của hợp đồng giữ nguyên.
- Lab Sư phạm viết lại thang gợi ý của 17 bài v0: dẫn `[n]` thay vì phát biểu lại quy tắc ngoài bảng.
- Gia sư nói ít câu tự do hơn. #90 đo tỉ lệ câu bị thay theo ngưỡng 20 % của ADR 013.

## Điều làm quyết định này sai

- Một câu không chứa thuật ngữ nào vẫn mang được quy tắc sai tới học sinh (ví dụ viết hoàn toàn bằng ẩn dụ) → đổi kiến trúc: mô hình chỉ chọn câu gợi ý đã duyệt và dẫn `[n]` (phương án D của bản phân tích).
- Trên 20 % câu bị thay dù thang gợi ý đã viết lại → như trên.
