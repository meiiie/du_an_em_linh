# Tài liệu tự soạn cho quy tắc đạo hàm (bản vá `sp-tai-lieu-0001`)

| | |
| --- | --- |
| Trạng thái | chấp nhận (2026-10-02, PR đóng #84); thành «đã nâng» khi #85 áp bản vá vào `data/supham/` |
| Người làm | Claude Code, vai lab Sư phạm |
| Câu hỏi | Lấy đâu căn cứ tầng 2 cho ba dòng đẳng thức của bảng công thức v0 (lũy thừa, tổng, thương)? |
| Issue | #84 (epic #79); dùng ở #85 (T011d: áp bản vá, nạp làm tài liệu thứ tư của lớp) và #83 (bảng 6 dòng phải `DAT` cả 6) |

## Vì sao cần

ADR 013 chỉ cho khóa bảng công thức khi mọi dòng `DAT` ở tầng 1 (máy kiểm) và tầng 2 (có đoạn trích trong tài liệu được phép của lớp). Bảng của v0 có 6 dòng (`apps/web/scripts/seed.ts@3bfc584`, dòng 612–617). Ba tài liệu của v0 (dòng 551–599) chỉ phát biểu đơn điệu, bảng xét dấu, cực trị và điểm tới hạn; không tài liệu nào phát biểu quy tắc lũy thừa, tổng, thương. Thiếu căn cứ thì tầng 2 không đạt, importer không khóa được bảng (Codex, luồng 4165480635 trên #78).

Không sửa tài liệu của v0: chúng được chép nguyên văn ra `data/v0/` (T011b). Lab thêm một tài liệu tự soạn, cùng loại với «Ghi chú tự soạn: đơn điệu và cực trị» của v0.

## Nội dung (nguyên văn trong bản vá)

- Mã `sp-tai-lieu-0001`; tiêu đề «Ghi chú tự soạn: quy tắc tính đạo hàm»; `kind` và `licenseStatus` là `tu_soan`; nguồn «lab Sư phạm»; phiên bản 1.
- Khóa JSON giống tài liệu trong `seed.ts` (`title`, `kind`, `source`, `licenseStatus`, `textContent`, `version`), thêm `ma` để importer nhận diện ổn định.

> Ghi chú tự soạn cho lớp 12A1 thử, không chép sách. Dùng khi tính đạo hàm của đa thức và phân thức trong chủ đề đơn điệu và cực trị; u, v là các hàm số có đạo hàm trên khoảng đang xét, k là hằng số. Đạo hàm lũy thừa, với n nguyên dương: (x^n)' = n x^{n-1}. Đạo hàm của x mũ n là n nhân x mũ n trừ 1. Hằng số có đạo hàm bằng 0. Hằng số nhân với hàm: (k u)' = k u'. Đạo hàm tổng: (u+v)' = u' + v'. Đạo hàm của tổng bằng tổng các đạo hàm; với hiệu cũng vậy: (u-v)' = u' - v'. Đạo hàm thương, tại các điểm có v khác 0: (u/v)' = (u'v - uv') / v^2. Với thương, tử là u'v trừ uv', mẫu là v bình. Tính xong đạo hàm thì xét dấu y' theo ghi chú «đơn điệu và cực trị» của lớp.

Mỗi dòng bảng có đoạn chứa **nguyên văn** cả LaTeX lẫn phát biểu của dòng đó, để tầng 2 khớp được mà không cần diễn giải:

| Dòng bảng v0 | LaTeX của dòng | Phát biểu của dòng | Có nguyên văn trong tài liệu |
| --- | --- | --- | --- |
| Đạo hàm lũy thừa | `(x^n)' = n x^{n-1}` | «Đạo hàm của x mũ n là n nhân x mũ n trừ 1. Hằng số có đạo hàm bằng 0.» | có cả hai |
| Đạo hàm tổng | `(u+v)' = u' + v'` | «Đạo hàm của tổng bằng tổng các đạo hàm.» | có cả hai |
| Đạo hàm thương | `(u/v)' = (u'v - uv') / v^2` | «Với thương, tử là u'v trừ uv', mẫu là v bình.» | có cả hai |

Không có ví dụ số, không lời giải: tài liệu hiện cho học sinh trong kho lớp và gia sư trích dẫn được, nên không được chứa kết quả của bài nào.

## Rà toán (quy trình `math-verifier`)

SymPy 1.14.0 (python hệ thống, `services/math` chưa có venv), chạy tại chỗ ngày 2026-10-02; `u`, `v` là hàm ký hiệu của `x`, `n` nguyên dương.

| Mệnh đề | Kết luận | Biểu thức kiểm |
| --- | --- | --- |
| (x^n)' = n x^{n-1}, n nguyên dương | ĐÚNG | `simplify(diff(x**n, x) - n*x**(n-1)) == 0`; thêm n = 1…12 cụ thể |
| Hằng số có đạo hàm bằng 0 | ĐÚNG | `diff(c, x) == 0` |
| (k u)' = k u' | ĐÚNG | `simplify(diff(k*u, x) - k*diff(u, x)) == 0` |
| (u+v)' = u' + v' | ĐÚNG | `simplify(diff(u + v, x) - (diff(u, x) + diff(v, x))) == 0` |
| (u-v)' = u' - v' | ĐÚNG | `simplify(diff(u - v, x) - (diff(u, x) - diff(v, x))) == 0` |
| (u/v)' = (u'v - uv')/v^2, tại v khác 0 | ĐÚNG | `simplify(diff(u/v, x) - (diff(u, x)*v - u*diff(v, x))/v**2) == 0` |

7 đúng · 0 sai · 0 không kiểm được. Điều kiện «n nguyên dương» và «v khác 0» được giữ trong câu: với n = 0, n x^{n-1} không xác định tại x = 0; thương chỉ có nghĩa khi v khác 0.

## Rà sư phạm (checklist `pedagogy-reviewer`)

| Mục | Kết quả |
| --- | --- |
| 1. Lộ đáp án | Không có kết quả của bài nào, không ví dụ số, không lời giải: ĐẠT |
| 2. Thang gợi ý | Không áp dụng (tài liệu tham khảo, không phải thang) |
| 3. Mức độ | Không áp dụng (không phải bài) |
| 4. Câu hỏi gợi mở | Không áp dụng |
| 5. Ngôn ngữ | Thuật ngữ «đạo hàm», «hằng số», «tử», «mẫu», «đa thức», «phân thức» theo SGK; có dấu; giọng ghi chú của lớp: ĐẠT. GỢI Ý: «v bình» là cách nói khẩu ngữ; giữ vì trùng nguyên văn phát biểu dòng thương của bảng v0, để tầng 2 khớp |
| 6. Dữ liệu | Không YCCĐ; không học sinh thật; lớp «12A1 thử» là dữ liệu tổng hợp: ĐẠT |

Kết luận: ĐẠT.

Hai lượt rà trên do chính agent soạn tài liệu làm, theo đúng quy trình trong `.claude/agents/math-verifier.md` và `.claude/agents/pedagogy-reviewer.md`. Phiên làm việc mở ở thư mục cha nên subagent của repo không được nạp. Đây **không** phải rà độc lập; người duyệt PR nên đọc lại phần «Nội dung».

## Bản vá

| | |
| --- | --- |
| Tệp | `labs/pedagogy/ban-va/sp-tai-lieu-0001.patch` (1298 byte, LF) |
| SHA-256 | `10dd6b2a646c9e6f7540a1d5f37a319faa59b147f5d8a8c70e6a22f45f492380` |
| Tạo ra | `data/supham/tai-lieu/sp-tai-lieu-0001.json` |
| Áp (#85) | `git apply labs/pedagogy/ban-va/sp-tai-lieu-0001.patch`; ghi mã `sp-tai-lieu-0001` trong tiêu đề commit; kiểm SHA-256 trước khi áp |

Đã kiểm `git apply --check` trên `main` `3bfc584`.

## Ngoài phạm vi

- Quy tắc tích `(uv)' = u'v + uv'`: bảng v0 không có dòng này. Thêm dòng vào bảng là việc mở rộng nội dung (lab Sư phạm, xem `2026-10-01-ke-hoach-mo-rong-noi-dung.md`).
- Câu «hằng số nhân với hàm» và «hiệu» có trong tài liệu để học sinh tra cứu. Chúng không phải dòng bảng, nên gia sư vẫn không được tự phát biểu chúng (ADR 013, thế giới đóng).
