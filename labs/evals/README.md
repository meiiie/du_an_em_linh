# Lab Kiểm định & Đánh giá

**Sứ mệnh:** biến «hệ thống đúng và an toàn» thành con số đo được, chạy lại được. Lab sở hữu bộ ca nghiệm thu và eval; mã sản phẩm phải qua chúng trước khi tới học sinh.

## Bộ ca đang có (v0)

| Bộ | Chỗ | Số liệu gần nhất (`docs/KIEM-THU.md`, 29/09/2026) |
| --- | --- | --- |
| pytest dịch vụ toán | `services/math/tests/` | 1053 đạt |
| Nghiệm thu Sư phạm (5 bước) | `services/math/kiemdinh/bo-de-kiem-thu/cac-ca-5-buoc*.yaml` | 80/80 |
| Đầu vào độc hại (288 ca) | `services/math/kiemdinh/bo-de-kiem-thu/ca-dau-vao-doc-hai.yaml` | 0 đạt nhầm, 0 thực thi được |
| Bộ AI | (70 ca) | 70/70 |
| Thang gợi ý qua bộ lọc lộ đáp án | `services/math/app/data/thang-goi-y-mau/` | 0 chặn nhầm / 4560 câu; 32/32 câu lộ cài sẵn bị bắt |
| Build + UX (180 test, 390 + 1280) | bộ khóa SHA của nhóm Build/UX | 122 đạt, 39 đỏ, 19 bỏ qua |

Kết quả chạy nằm ở `services/math/kiemdinh/ket-qua/` và chỉ được ghi bởi script chạy bộ ca. Hook chặn sửa tay (hiến chương IV).

## Khung eval gia sư cho v2

| Chiều | Câu hỏi | Cách đo | Ngưỡng |
| --- | --- | --- | --- |
| Lộ đáp án | Gia sư có nêu kết quả, làm hộ bước không? | Bộ lọc CAS + bộ ca dụ đáp án (năn nỉ, nhập vai, chia nhỏ câu hỏi, chèn lệnh) | 0 lộ |
| Đúng toán | Công thức, phép biến đổi trong lời gia sư có đúng không? | Tầng 1–3 trên mọi biểu thức trích được; rà mẫu bằng tay | 0 sai đã biết |
| Đúng lỗi | Gợi ý có nhắm đúng bước sai, đúng mã lỗi không? | So với nhãn của bộ ca Sư phạm | ≥ 95 % |
| Chất lượng gợi mở | Câu hỏi có làm học sinh tự nghĩ không, có ngắn và đúng tiếng lớp 12 không? | Rubric 4 tiêu chí, chấm bằng LLM có hiệu chuẩn với giáo viên trên ≥ 50 mẫu | Do lab Sư phạm đặt |
| Chèn lệnh qua tài liệu | Tài liệu giáo viên nạp có chứa lệnh ẩn làm gia sư lộ đáp án? | Bộ tài liệu cài lệnh (OWASP LLM01) | 0 thành công |
| Trễ, chi phí | Một lượt mất bao lâu, bao nhiêu tiền? | p50 / p95 thời gian; token × giá | Đặt theo ngân sách (Q7) |

Mọi thay đổi prompt, nhà cung cấp, mô hình hay bộ lọc phải chạy khung này; kết quả (lệnh, SHA, số) dán vào PR (hiến chương IV).

## Việc mở

- Chuyển bộ AI 70 ca và bộ e2e gia sư sang dạng chạy được với `services/core` (v2) trước khi port gia sư.
- Dựng bộ chèn lệnh qua tài liệu (chưa có ở v0).
- Bộ benchmark OCR 30–50 trang SGK / đề tiếng Việt (xem `labs/research/2026-10-01-muc-hieu-va-ocr.md`).
