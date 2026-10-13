# Quyết định ngày 2026-10-13 — FLAT, gỡ v0 và kiểm tra

**Trạng thái:** FLAT/gỡ v0 đã được chủ repo chỉ đạo; chi tiết kiểm tra đề xuất để rà.  
**Nguồn:** yêu cầu trực tiếp ngày 2026-10-13; `/workspace/du_an_em_linh-notes/UX_REFERENCE_PLAN.md` U0–U15 và `REQUIREMENTS_ANALYSIS.md` (chỉ đọc). Không dùng báo cáo hiện trạng cũ làm bằng chứng test mới.

## Phương án và lý do

| Vấn đề | Phương án | Đánh giá |
| --- | --- | --- |
| Phân cấp UI | Giữ bóng/vạch trái của nguyên mẫu | Trái FLAT tuyệt đối, loại |
| Phân cấp UI | Nền/chữ/khoảng trắng/viền đều, outline focus | Theo chỉ đạo, giữ token/a11y, chọn |
| Chuyển kiến trúc | Giữ v0 tới đủ màn | Bị chỉ đạo gỡ ngay thay thế |
| Chuyển kiến trúc | V2 duy nhất, snapshot import | Theo chỉ đạo, chọn; vẫn phải đo nghiệm thu v2 |
| Kiểm tra | Ẩn tutor ở frontend | Gọi trực tiếp API/tab cũ vượt được, loại |
| Kiểm tra | Kiểm controller rời rạc | Dễ bỏ sót đường tutor, không chọn làm ranh giới duy nhất |
| Kiểm tra | Policy application dùng chung, đọc mode/hạn server, kiểm trước phát | Đề xuất; kiểm được bằng API và ca cạnh tranh tất định |

Không chấm điểm số khi chưa có phép đo. Một giao hiệu lực mỗi bài/em khớp API_UX; đổi mode/hạn giữa lượt đòi kiểm lại và tuần tự hóa. Nếu khách cần thi nhiều lượt độc lập, ID giao/lượt phải được thiết kế lại trước khi mở rộng.

## Bàn giao

[ADR 014](../../docs/adr/014-giao-dien-phang.md), [ADR 017](../../docs/adr/017-che-do-kiem-tra-phia-server.md), [ADR 016](../../docs/adr/016-go-v0.md); [spec 002](../../specs/002-hoan-thien-v2/spec.md) và task T100–T133. Mặc định khách ở [ASSUMPTIONS](../../docs/ASSUMPTIONS.md). Không sửa mã, dữ liệu sư phạm, migration phát hành hay workflow trong lane DOCS.
