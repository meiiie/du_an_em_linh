# ADR 006 — Chỉ dữ liệu tổng hợp

Tài khoản An, Bình, Chi và giáo viên thử đều `is_synthetic`. Không có cổng phụ huynh.

Bảng `consent_records` và `audit_logs` là móc cho pilot sau: học sinh không tổng hợp bị chặn khỏi luyện tập nếu chưa có bản ghi đồng ý mục đích `HOC_TAP` còn hiệu lực. RLS trên `submissions` và `mastery_states` được bật nhưng chưa `FORCE`; nguyên mẫu kiểm vai trò trong mã.
