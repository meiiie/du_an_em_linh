# Lab

Lab là nơi làm ra **tri thức, đặc tả và bộ ca** trước hoặc song song với việc viết mã. Mỗi lab chính thức hóa một «nhóm» đã chạy ở v0 (nhóm Sư phạm, nhóm Kiểm định, nhóm Build/UX, nhóm AI): có thư mục, hợp đồng đầu vào / đầu ra, và đường nâng kết quả lên nguồn chuẩn.

| Lab | Thư mục | Câu hỏi lab trả lời | Kết quả được nâng lên |
| --- | --- | --- | --- |
| Thiết kế (UI/UX) | [`design/`](design/README.md) | Học sinh, giáo viên nhìn và làm gì trên màn hình? | `docs/DESIGN.md`, token, component |
| Sư phạm | [`pedagogy/`](pedagogy/README.md) | Dạy cái gì, theo thứ tự nào, sai thì gợi ý thế nào? | `data/supham/`, thang gợi ý |
| Kiểm định & Đánh giá | [`evals/`](evals/README.md) | Làm sao biết hệ thống đúng và an toàn? | `services/math/kiemdinh/`, job CI, `docs/KIEM-THU.md` |
| Nghiên cứu | [`research/`](research/README.md) | Thế giới (khoa học, luật, công nghệ) đang ở đâu? | Được ADR và spec trích dẫn |
| Quyết định | [`decisions/`](decisions/README.md) | Chọn phương án nào, vì sao, điều gì làm nó sai? | `docs/adr/` |

## Quy tắc chung

1. **Tên tệp** `YYYY-MM-DD-<chủ-đề>.md` (ngày bắt đầu). Thư mục con theo README của từng lab.
2. **Đầu tệp** có: trạng thái (`nháp` · `đang làm` · `đề xuất` · `chấp nhận` · `đã nâng` · `lưu trữ`), người làm, câu hỏi, issue liên quan.
3. **Bằng chứng:** nguồn kèm ngày truy cập; tách dữ kiện khỏi nhận định; ghi độ tin (cao / trung bình / thấp). Số đo kèm lệnh và SHA.
4. **Bàn giao** qua GitHub issue nhãn `lab/<tên>`, trỏ tới tệp trong lab. Lab Sư phạm và Kiểm định bàn giao dữ liệu dưới dạng **bản vá nguyên văn** có mã (ví dụ `sp-sua-thang-0001`, `kd-0004b`); người hiện thực áp đúng nguyên văn.
5. **Nâng lên** khi được chấp nhận: phần cốt lõi chuyển sang nguồn chuẩn ở cột cuối bảng trên; tệp lab đổi trạng thái `đã nâng` kèm đường dẫn đích. Lab không phải nơi chứa nguồn chuẩn lâu dài.
6. **Không** có dữ liệu học sinh thật, khóa, ảnh chụp chứa thông tin cá nhân.

## Mở một phiên lab

Gõ `lab <tên> <chủ đề>` (ví dụ `lab design màn làm bài v2`). Agent đọc README của lab đó, dùng skill tương ứng (`design-study`, `math-pedagogy`, `research-sota`, `decision-record`), viết ghi chú có ngày, kết thúc bằng đề xuất issue. Nghiên cứu dài thì chạy subagent `researcher` ở nền.
