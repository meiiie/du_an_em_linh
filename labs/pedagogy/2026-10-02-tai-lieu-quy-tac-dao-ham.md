# Tài liệu tự soạn cho quy tắc đạo hàm (bản vá `sp-tai-lieu-0001`)

| | |
| --- | --- |
| Trạng thái | chấp nhận (2026-10-02, PR đóng #84, sau hai lượt rà độc lập); thành «đã nâng» khi #85 áp bản vá vào `data/supham/` |
| Người làm | Claude Code, vai lab Sư phạm |
| Câu hỏi | Lấy đâu căn cứ tầng 2 cho ba dòng đẳng thức của bảng công thức v0 (lũy thừa, tổng, thương)? |
| Issue | #84 (epic #79); dùng ở #85 (T011d: áp bản vá, nạp làm tài liệu thứ tư của lớp) và #83 (bảng 6 dòng phải `DAT` cả 6) |

## Vì sao cần

ADR 013 chỉ cho khóa bảng công thức khi mọi dòng `DAT` ở tầng 1 (máy kiểm) và tầng 2 (có đoạn trích trong tài liệu được phép của lớp). Bảng của v0 có 6 dòng (`apps/web/scripts/seed.ts@3bfc584`, dòng 612–617). Ba tài liệu của v0 (dòng 551–599) chỉ phát biểu đơn điệu, bảng xét dấu, cực trị và điểm tới hạn. Không tài liệu nào phát biểu quy tắc lũy thừa, tổng, thương. Thiếu căn cứ thì tầng 2 không đạt và importer không khóa được bảng (Codex, luồng 4165480635 trên #78).

Không sửa tài liệu của v0: chúng được chép nguyên văn ra `data/v0/` (T011b). Lab thêm một tài liệu tự soạn, cùng loại với «Ghi chú tự soạn: đơn điệu và cực trị» của v0.

## Nội dung (nguyên văn trong bản vá, bản 2)

- Mã `sp-tai-lieu-0001`; tiêu đề «Ghi chú tự soạn: quy tắc tính đạo hàm»; `kind` và `licenseStatus` là `tu_soan`; nguồn «lab Sư phạm»; phiên bản 1.
- Khóa JSON giống tài liệu trong `seed.ts` (`title`, `kind`, `source`, `licenseStatus`, `textContent`, `version`), thêm `ma` để importer nhận diện ổn định.

> Ghi chú tự soạn cho lớp 12A1 thử, không chép sách. Dùng khi tính đạo hàm của hàm đa thức và hàm phân thức trong chủ đề đơn điệu và cực trị; u, v là các hàm số có đạo hàm trên khoảng đang xét, k là hằng số. Đạo hàm lũy thừa, với n nguyên dương: $(x^n)' = n x^{n-1}$. Đạo hàm của x mũ n là n nhân x mũ n trừ 1. Hằng số có đạo hàm bằng 0. Hằng số nhân với hàm: $(k u)' = k u'$. Đạo hàm tổng: $(u+v)' = u' + v'$. Đạo hàm của tổng bằng tổng các đạo hàm. Với hiệu cũng vậy: $(u-v)' = u' - v'$. Đạo hàm thương, tại các điểm có v khác 0: $(u/v)' = (u'v - uv') / v^2$. Với thương, tử là u'v trừ uv', mẫu là v bình. Tính xong đạo hàm thì tìm các điểm mà y' bằng 0 hoặc không xác định, rồi xét dấu y' theo ghi chú «đơn điệu và cực trị» của lớp. Ôn lại khi cần: Toán 11, các quy tắc tính đạo hàm.

Cả 6 ô (LaTeX và phát biểu) của 3 dòng bảng là chuỗi con **nguyên văn** của tài liệu, kể cả dấu câu, nên tầng 2 khớp mà không cần chuẩn hóa:

| Dòng bảng v0 | LaTeX của dòng | Phát biểu của dòng | Nguyên văn trong tài liệu |
| --- | --- | --- | --- |
| Đạo hàm lũy thừa | `(x^n)' = n x^{n-1}` | «Đạo hàm của x mũ n là n nhân x mũ n trừ 1. Hằng số có đạo hàm bằng 0.» | có cả hai |
| Đạo hàm tổng | `(u+v)' = u' + v'` | «Đạo hàm của tổng bằng tổng các đạo hàm.» | có cả hai |
| Đạo hàm thương | `(u/v)' = (u'v - uv') / v^2` | «Với thương, tử là u'v trừ uv', mẫu là v bình.» | có cả hai |

Không có ví dụ số, không lời giải: tài liệu hiện cho học sinh trong kho lớp và gia sư trích dẫn được, nên không được chứa kết quả của bài nào. Không mệnh đề nào trong 14 mệnh đề bị `verify.tang_2` của v0 nhận làm đoạn quy tắc, nên thêm tài liệu không đổi kết quả cổng của bài (T013/T014).

## Rà toán (subagent `math-verifier`, độc lập)

Agent mới, không có ngữ cảnh soạn thảo, chạy đúng định nghĩa `.claude/agents/math-verifier.md` trên bản 1 (SymPy 1.14.0, chỉ đọc). Kết quả: **17 đúng · 0 sai · 1 không kiểm được**. Mục không kiểm được là câu chỉ dẫn quy trình «tính xong đạo hàm thì…», không phải mệnh đề toán.

| Mệnh đề | Kết luận và căn cứ chính |
| --- | --- |
| (x^n)' = n x^{n-1}, n nguyên dương | ĐÚNG. Kiểm ký hiệu, m = 1…40, giới hạn tỉ sai phân m = 1…12. Điều kiện chặt hơn mức cần nhưng hợp với đa thức; loại n = 0 là đúng |
| «Đạo hàm của x mũ n là n nhân x mũ n trừ 1» | ĐÚNG nếu đọc «x mũ (n − 1)» như công thức ngay trước. Đọc thành n·xⁿ − 1 thì sai; xem mục «Còn để ngỏ» |
| Hằng số có đạo hàm bằng 0; (k u)' = k u'; (u ± v)' = u' ± v'; tổng hữu hạn | ĐÚNG (`simplify` của hiệu → 0). Giả thiết «u, v có đạo hàm, k là hằng số» đều cần |
| (u/v)' = (u'v − uv')/v², tại v khác 0 | ĐÚNG. Kiểm cả thứ tự phép trừ và bẫy nghiệm tử ngoài tập xác định (u = x² − 1, v = x − 1) |
| 6 ô của dòng bảng v0 612–614 | Cả 6 đúng toán học. Ở bản 1, ô lời của dòng tổng không nguyên văn (tài liệu viết «…đạo hàm;»). Bản 2 đã sửa |

Bản 2 không đổi công thức hay điều kiện nào. Bản 2 chỉ làm 4 việc: đổi «;» thành «.» ở dòng tổng, bọc 5 công thức trong `$…$`, đổi «đa thức và phân thức» thành «hàm đa thức và hàm phân thức», viết lại câu chỉ dẫn quy trình và thêm câu «Ôn lại».

## Rà sư phạm (subagent `pedagogy-reviewer`, độc lập)

Agent mới chạy đúng `.claude/agents/pedagogy-reviewer.md` trên bản 1, chỉ đọc. Kết luận: **ĐẠT**, không có mục CHẶN. Lộ đáp án ĐẠT; mục 2–4 không áp dụng (không phải thang, không phải bài).

| Mức | Phát hiện | Xử lý |
| --- | --- | --- |
| NÊN SỬA | Phát biểu dòng tổng không nguyên văn (dấu «;») | Sửa ở bản 2 |
| NÊN SỬA | Câu cuối dắt từ bước đạo hàm sang thẳng xét dấu, bỏ bước nghiệm. Với phân thức, đó chính là lỗi ERR.DH.04 | Bản 2: «tìm các điểm mà y' bằng 0 hoặc không xác định, rồi xét dấu…». Câu này không ghép dấu đạo hàm với đơn điệu hay cực trị, nên không chạm tầng 2 của v0 |
| NÊN SỬA | Công thức viết trần thì kho lớp không render được | Bản 2 bọc `$…$`; LaTeX của dòng bảng vẫn là chuỗi con. Kho lớp phải render đoạn tài liệu bằng KaTeX (ghi vào #92, T039) |
| NÊN SỬA (ngoài bản vá) | Cửa sổ trích 246 ký tự của kho lớp (`kien-thuc.ts`) neo nhầm và cắt giữa công thức | Ghi vào #91 (T034): neo theo từ khóa của bước, cắt ở ranh giới câu, tài liệu ngắn thì trích cả đoạn |
| GỢI Ý | «n nhân x mũ n trừ 1», «hằng số», «v bình» là khẩu ngữ | Giữ ở bản này vì phải trùng nguyên văn dòng bảng. Sửa cùng lúc dòng bảng và tài liệu ở phiên bản bảng kế tiếp |
| GỢI Ý | «Đa thức và phân thức» là cách gọi rút gọn | Sửa ở bản 2 |
| GỢI Ý | Chỉ chỗ ôn cho học sinh sai lặp lại | Bản 2 thêm «Ôn lại khi cần: Toán 11, các quy tắc tính đạo hàm.». Không ghi tên bài theo một bộ SGK, vì chưa biết khách dùng bộ nào (#61) |
| GỢI Ý | Câu «tự soạn, không chép sách» hiện cho học sinh | Giữ chữ cho đồng bộ với v0. T039 mang sang quy tắc ẩn câu khung của v0 (#92) |
| GỢI Ý | Thêm (ku)', (u−v)' thành dòng bảng; cân nhắc quy tắc tích | Để phiên bản bảng kế tiếp (lab Sư phạm, đo theo ngưỡng 20 % của ADR 013) |
| GỢI Ý | Phát biểu dòng lũy thừa gồm 2 câu, còn tài liệu lại có thể bị chia đoạn theo câu | #83 có thêm ca: tài liệu chia đoạn theo câu thì 3 dòng đẳng thức vẫn đạt tầng 2 nhờ LaTeX |

Lượt tự rà ban đầu của agent soạn có hai lỗi: ghi «7 đúng» trong khi bảng có 6 mệnh đề (7 là số phép kiểm), và bỏ sót lỗi dấu câu ở dòng tổng. Hai lượt rà độc lập bắt được cả hai (Codex, luồng 4165678189 trên #100).

## Bản vá

| | |
| --- | --- |
| Tệp | `labs/pedagogy/ban-va/sp-tai-lieu-0001.patch` (bản 2; 1451 byte, LF) |
| SHA-256 | `60a16b7222d37f9ca51b02607b2ceac511fb9d5e9ae95f92821a0a93775057ab` |
| Tạo ra | `data/supham/tai-lieu/sp-tai-lieu-0001.json` |
| Áp (#85) | Kiểm SHA-256, rồi `git apply labs/pedagogy/ban-va/sp-tai-lieu-0001.patch`; ghi mã `sp-tai-lieu-0001` trong tiêu đề commit; không sửa chữ khi áp |

Đã kiểm `git apply --check` trên `main` `b04fbfd`.

## Còn để ngỏ

- Ba câu khóa nguyên văn từ bảng v0 dùng khẩu ngữ («x mũ n trừ 1» đọc nhầm được thành xⁿ − 1). Công thức ngay trước mỗi câu gỡ được nghĩa. Sửa ở phiên bản bảng kế tiếp, cùng lúc với tài liệu.
- Quy tắc tích `(uv)' = u'v + uv'` chưa có trong bảng lẫn tài liệu. Thêm thì gia sư cũng không được tự nói khi chưa có dòng bảng (ADR 013, thế giới đóng).
