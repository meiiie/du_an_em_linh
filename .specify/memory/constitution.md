<!--
SYNC IMPACT REPORT
Version: TEMPLATE → 1.0.0 (2026-10-01). Bộ chuyển tiếp: nguồn chuẩn là docs/HIEN-CHUONG.md.
Templates: plan-template.md lấy bảng «Constitution Check» bên dưới; không sửa template.
Theo dõi: mỗi lần sửa docs/HIEN-CHUONG.md phải sửa bảng dưới đây trong cùng PR.
-->

# Hiến chương — bản dùng cho Spec Kit

> **Nguồn chuẩn:** [`docs/HIEN-CHUONG.md`](../../docs/HIEN-CHUONG.md) — 8 nguyên tắc, cổng chất lượng, cách sửa đổi. File này chỉ chuyển hiến chương sang dạng `/speckit-plan` và `/speckit-analyze` dùng được. Hai file lệch nhau thì `docs/HIEN-CHUONG.md` thắng. Khi `docs/HIEN-CHUONG.md` chưa có trên nhánh đang làm (nó vào `main` qua PR #52), bảng dưới đây là chuẩn tạm thời.

## Constitution Check

Mỗi `plan.md` phải trả lời **CÓ** cho mọi dòng áp dụng; dòng không áp dụng ghi «Không áp dụng» kèm lý do. Nguyên tắc I–III không thương lượng: một câu KHÔNG là chặn.

| # | Nguyên tắc | Câu kiểm |
| --- | --- | --- |
| I | Không đưa đáp án | Không luồng nào đưa kết quả cuối, lời giải trọn hay làm hộ một bước tới học sinh đang làm bài; mọi câu gia sư đi qua bộ lọc CAS kiểu fail-closed? |
| II | Đúng toán trước hết | Mọi nội dung toán tới học sinh, kể cả công thức trong lời gia sư, qua cổng 3 tầng; đúng / sai do CAS quyết; lỗi gọi dịch vụ toán không bao giờ được coi là đạt? |
| III | Người học là trẻ vị thành niên | Không dữ liệu thật trong repo và test; xóa định danh trước khi gửi nhà cung cấp AI; học sinh biết đang học cùng AI; giáo viên ghi đè được phân loại; khóa chỉ ở biến môi trường? |
| IV | Bằng chứng | Mỗi tiêu chí nghiệm thu có cách đo (lệnh, bộ ca, eval); quyết định khó đảo ngược có ADR? |
| V | Tiếng Việt đúng lứa tuổi | Chữ giao diện tiếng Việt có dấu, đúng giọng người dùng, mỗi chữ một việc? |
| VI | Ranh giới kiến trúc | Đúng dịch vụ và migration chỉ thêm? Theo vùng chạm tới — v0: `apps/web` không gọi SymPy trực tiếp, mọi việc CAS qua `services/math` (sandbox, ADR 002); v2 (ADR 011): `apps/frontend` không logic nghiệp vụ, `services/core` giữ trạng thái và quyền (ArchUnit xanh), Angular signal-first; mọi bản: `services/math` thuần hàm. Vùng không chạm ghi «Không áp dụng». |
| VII | Thay đổi nhỏ | Chia thành nhiều PR một ý; không trừu tượng hóa suy đoán; giữ `data-testid` đã khóa? |
| VIII | Truy cập được | WCAG 2.2 AA; mục tiêu chạm 44 px; kiểm ở 390 px và 1280 px? |

Vi phạm có chủ đích ghi vào mục «Complexity Tracking» của `plan.md`, kèm lý do và người duyệt.

## Quy ước Spec Kit trong repo này

- Dùng cho **epic**: module mới, luồng mới, đổi kiến trúc (`docs/QUY-TRINH.md` §2). Việc nhỏ đi đường issue → PR.
- Nhánh epic `NNN-<slug>` do `/speckit-git-feature` tạo — ngoại lệ có chủ đích với quy ước `<type>/<slug>`; vẫn PR vào `main`, squash merge.
- Spec viết tiếng Việt; tiêu chí nghiệm thu đo được; nêu năng lực `C1`–`C10` (`docs/product/MUC-TIEU.md`) và pha (`docs/product/LO-TRINH.md`).
- Tự commit của phần mở rộng git đang tắt (`.specify/extensions/git/git-config.yml`). Commit tay theo Conventional Commits, có trailer `Co-Authored-By`.

**Version**: 1.0.0 | **Ratified**: khi PR khởi tạo Spec Kit được merge | **Last Amended**: 2026-10-01
