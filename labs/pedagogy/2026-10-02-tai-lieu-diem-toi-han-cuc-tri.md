# Tài liệu tự soạn cho điểm tới hạn và dấu hiệu cực trị (bản vá `sp-tai-lieu-0002`)

| | |
| --- | --- |
| Trạng thái | đã nâng (2026-10-02, #85 T011d): áp nguyên văn vào [`data/supham/tai-lieu/sp-tai-lieu-0002.json`](../../data/supham/tai-lieu/sp-tai-lieu-0002.json). Chấp nhận ở PR đóng #103, bản 2 sau hai lượt rà độc lập |
| Người làm | Claude Code, vai lab Sư phạm |
| Câu hỏi | Lấy đâu căn cứ tầng 2 cho hai mệnh đề của bảng v0 mà tài liệu hiện có không phát biểu? |
| Issue | #103 (epic #79); dùng ở #85 (T011d áp bản vá) và #83 (bảng 6 dòng phải `DAT` cả 6) |

## Vì sao cần

Lượt rà `math-verifier` độc lập trên #101 (job `kiem-dong-cong-thuc`) chỉ ra rằng tầng 2 chỉ đòi một mệnh đề của dòng có đoạn trích. Phát biểu của dòng đã khóa là danh sách trắng cho gia sư (ADR 013), nên mệnh đề không có căn cứ vẫn tới học sinh. Từ #101, tầng 2 đòi **mọi** mệnh đề của dòng định lí có đoạn trích. Với 4 tài liệu hiện có (3 của v0 và `sp-tai-lieu-0001`), hai dòng của bảng v0 (`apps/web/scripts/seed.ts@3bfc584`) thiếu căn cứ:

- **d-5 «Cực trị»**, câu «Đạo hàm bằng 0 mà không đổi dấu thì chưa phải cực trị.»: không tài liệu nào phát biểu.
- **d-6 «Điểm tới hạn»**: không tài liệu nào định nghĩa. Câu «Lập bảng xét dấu …» của v0 không phải định nghĩa và thiếu điều kiện «thuộc tập xác định».

## Nội dung (nguyên văn trong bản vá, bản 2)

- Mã `sp-tai-lieu-0002`; tiêu đề «Ghi chú tự soạn: điểm tới hạn và dấu hiệu cực trị»; `kind` và `licenseStatus` là `tu_soan`; nguồn «lab Sư phạm»; phiên bản 1; khóa JSON như `sp-tai-lieu-0001`.

> Ghi chú tự soạn cho lớp 12A1 thử, không chép sách. Điểm tới hạn gồm nghiệm của đạo hàm bằng 0 và điểm thuộc tập xác định mà đạo hàm không xác định. Bảng xét dấu của y' có hàng x gồm các điểm tới hạn và các điểm bị loại khỏi tập xác định, xếp từ nhỏ đến lớn. Đạo hàm bằng 0 mà không đổi dấu thì chưa phải cực trị. Ôn lại khi cần: Toán 12, cực trị của hàm số; Toán 10, tập xác định của hàm số.

Điểm tới hạn **khác** mốc của bảng xét dấu: điểm bị loại khỏi tập xác định không phải điểm tới hạn nhưng vẫn là mốc (ERR.DH.07, ERR.DH.31; thang `huu-ti.json`). Bản 1 lẫn hai khái niệm này.

Đối chiếu với bảng v0 (nguyên văn, kể cả dấu câu):

| Dòng bảng v0 | Câu | Có trong tài liệu |
| --- | --- | --- |
| d-5 «Cực trị» (dòng 616, câu 3) | «Đạo hàm bằng 0 mà không đổi dấu thì chưa phải cực trị.» | có, nguyên văn |
| d-6 «Điểm tới hạn» (dòng 617) | «Điểm tới hạn gồm nghiệm của đạo hàm bằng 0 và điểm thuộc tập xác định mà đạo hàm không xác định.» | có, nguyên văn |

## Kiểm máy

- Job `kiem-dong-cong-thuc` (bản làm lại ở #101) với 5 tài liệu của lớp: bảng 6 dòng của v0 `DAT` cả 6 ở hai tầng. d-5 lấy CT1 từ ghi chú đơn điệu của v0, CT2 từ tài liệu này; d-6 lấy định nghĩa từ tài liệu này.
- Không mệnh đề nào của tài liệu bị `verify.tang_2` của v0 nhận làm đoạn quy tắc (`_huong_don_dieu`, `_huong_cuc_tri` đều `None`), nên kết quả cổng của bài không đổi (T013/T014).
- `git apply --check` trên `main`: áp được.

## Rà độc lập (trên bản 1)

Hai agent mới, không có ngữ cảnh soạn thảo, chạy đúng `.claude/agents/math-verifier.md` và `.claude/agents/pedagogy-reviewer.md`.

**math-verifier: 8 đúng · 1 sai · 1 không kiểm được.**
- Hai câu khóa đúng toán và trùng nguyên văn từng byte.
  - Câu định nghĩa đúng khi hiểu nghiệm của y' là nghiệm nằm trong tập xác định, vì y' chỉ xác định ở đó.
  - Câu «không đổi dấu» đúng theo định nghĩa cực trị chặt của SGK. y'(x0) = 0 kéo theo f khả vi, nên liên tục, tại x0.
- **SAI:** câu thủ tục của bản 1 («ghi các điểm tới hạn lên trục số rồi xét dấu trên từng khoảng giữa chúng») bỏ sót điểm bị loại khỏi tập xác định. Với (x + 1)/(x − 1), hàm không có điểm tới hạn nào, nên làm theo câu đó sẽ coi ℝ là một khoảng.
- Không kiểm được: câu «Ôn lại», vì không phải mệnh đề toán.

**pedagogy-reviewer: CHƯA ĐẠT (bản 1) → đã sửa ở bản 2.**

| Mức | Phát hiện | Xử lý ở bản 2 |
| --- | --- | --- |
| CHẶN | Câu thủ tục sai khi tập xác định khác ℝ: làm theo thì ra đúng lỗi ERR.DH.07 trên DH12-03-TH-02, DH12-03-NB-02 | Thay bằng «Bảng xét dấu của y' có hàng x gồm các điểm tới hạn và các điểm bị loại khỏi tập xác định, xếp từ nhỏ đến lớn.» |
| NÊN SỬA | Ví dụ $y = x^3$: hôm nay không lộ bài nào. Nhưng importer nạp tài liệu vào mọi lớp mà không đối chiếu với dữ kiện bảo vệ của từng bài, nên khi ngân hàng thêm bài bậc ba có nghiệm kép, câu này thành lời giải mẫu | Bỏ ví dụ. Đưa lại bằng bản vá riêng sau khi đoạn trích tài liệu được lọc |
| NÊN SỬA (ngoài bản vá) | Đoạn trích tài liệu vào prompt và chip «Đã đọc» không qua `/v1/filter` | Ghi vào #91 (T034): lọc từng đoạn trích theo dữ kiện bảo vệ của bài; bộ lọc lỗi thì không trích |
| GỢI Ý | «Ôn lại» chỉ trỏ về bài đang học; lỗi tập xác định có kỹ năng tiên quyết T10.HS.01 | Thêm «Toán 10, tập xác định của hàm số» |
| GỢI Ý | Câu khóa «chưa phải cực trị» thiếu chữ «điểm»; «nghiệm của đạo hàm bằng 0» là khẩu ngữ | Giữ nguyên vì phải trùng nguyên văn bảng v0; sửa cùng lúc dòng bảng và tài liệu ở phiên bản bảng kế tiếp |

Với bản 2, máy kiểm lại:
- Hai câu khóa vẫn trùng nguyên văn.
- Bộ nhận dạng của v0 không nhận mệnh đề nào làm quy tắc.
- Job `kiem-dong-cong-thuc` (#101) với 5 tài liệu: bảng 6 dòng của v0 `DAT` cả 6 ở hai tầng.

## Bản vá

| | |
| --- | --- |
| Tệp | `labs/pedagogy/ban-va/sp-tai-lieu-0002.patch` (bản 2; 1006 byte, LF) |
| SHA-256 | `128d9d777ea8537660bfd277e17cd4c50e5bc66859f63525122fb7652f33779e` |
| Tạo ra | `data/supham/tai-lieu/sp-tai-lieu-0002.json` |
| Áp (#85) | Kiểm SHA-256, rồi `git apply labs/pedagogy/ban-va/sp-tai-lieu-0002.patch`; ghi mã trong tiêu đề commit; không sửa chữ khi áp |
