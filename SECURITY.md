# Bảo mật

Không mở issue công khai nếu nội dung chứa khóa API, cookie phiên, hoặc dữ liệu học sinh.

## Báo cáo

Dùng [GitHub Private vulnerability reporting](https://github.com/meiiie/du_an_em_linh/security/advisories/new) khi repo bật mục này. Nếu chưa bật, gửi cho chủ repo qua kênh riêng — không dán bí mật vào issue.

## Không làm

- Commit `.env`, `.env.local`, `*.pem`, khóa OpenAI / OAuth
- Gọi LLM tới host local không phải loopback (`127.0.0.1`, `localhost`, `::1`)
- Device-OAuth ChatGPT / Codex hay `client_id` không do OpenAI cấp cho ứng dụng này
- Log đề, lời giải, hay tin nhắn học sinh ra dịch vụ ngoài repo

Mẫu biến: `.env.example`. Agent: không đọc tệp bí mật (xem `AGENTS.md`).
