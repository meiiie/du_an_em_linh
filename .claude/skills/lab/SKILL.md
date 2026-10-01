---
name: lab
description: Mở một phiên lab (design, pedagogy, evals, research, decisions) theo labs/<tên>/README.md và viết ghi chú có ngày. Dùng khi chủ repo gõ «lab <tên> <chủ đề>» hoặc giao một việc nghiên cứu, thiết kế, sư phạm, kiểm định, quyết định.
argument-hint: "<design|pedagogy|evals|research|decisions> <chủ đề>"
---

# Phiên lab

Lab: **$0**. Yêu cầu đầy đủ: $ARGUMENTS

1. Đọc `labs/README.md` và `labs/$0/README.md`. Tên lab không có → liệt kê 5 lab và hỏi lại.
2. Nạp skill của lab:

   | Lab | Skill |
   | --- | --- |
   | design | `design-study` |
   | pedagogy | `math-pedagogy` |
   | evals | `tutor-safety`, `math-engine` |
   | research | `research-sota` |
   | decisions | `decision-record` |

3. Tạo `labs/$0/[thư-mục-con-theo-README/]YYYY-MM-DD-<chủ-đề>.md`, đầu tệp có trạng thái, người làm, câu hỏi, issue liên quan.
4. Làm việc theo README của lab. Đọc nhiều nguồn → giao subagent `researcher` chạy nền để giữ ngữ cảnh chính gọn.
5. Kết thúc: tóm tắt phát hiện, cập nhật chỉ mục của lab, đề xuất issue (tiêu đề + tiêu chí nghiệm thu) cho chủ repo. Chỉ mở issue khi được bảo.
