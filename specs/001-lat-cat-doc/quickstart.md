# Quickstart — chạy và kiểm lát cắt P2

## Chạy

```bash
docker compose -f compose.v2.yaml up --build --wait   # core profile dev: tài khoản thử + nhập nội dung chủ đề (research R6)
bash scripts/khoi-v2.sh                               # khói: trang, /api, math
```

Mở http://localhost:4200. Tài khoản tổng hợp: `gv@demo.local` / `giaovien123`, `hs.an@demo.local` / `hocsinh123` (Bình, Chi cùng mật khẩu). Gia sư mặc định chạy chế độ `offline`; nhà thật chỉ khi đặt khóa trong biến môi trường của core và chỉ cho tài khoản tổng hợp (ADR 012).

## Kịch bản tay «một vòng»

1. Giáo viên: «Tài liệu» → tải một PDF có chữ, quyền dùng `tu_soan` → «Công thức» → khóa bảng → «Duyệt» → thấy `DH12-01-TH-01` «Không kiểm được», `DH12-DEMO-CHAN-01` «Sai» → duyệt mục đầu kèm ghi chú.
2. An: «Học» → bài kế → làm 5 bước, nộp đạo hàm sai → thấy ô bị tô → «Cần gợi ý?» → gia sư trả lời theo thang, trích dẫn `[n]` đoạn của PDF vừa nạp → sửa bước, nộp bài.
3. An: «Học» → mức hiểu tăng, bài kế nâng 1 nấc → «Lịch» có lời khuyên mới.
4. Giáo viên: «Mức» → thấy An tăng; bấm đổi 3 mức.

## Kiểm theo tiêu chí (spec §Success Criteria)

| SC | Lệnh |
| --- | --- |
| SC-001, 002, 008, 010 | `pnpm --filter frontend e2e` trên compose (kịch bản `mot-vong`, `duyet`, `toi-vdc`, `tuong-duong-v0`) |
| SC-003, 004 | `cd services/core && ./mvnw -B -ntp verify -Dgroups=bo-ca` (nhà giả `gia-lap` phát lại bộ dụ đáp án, 288 ca ác ý, bộ ca lời giảng) |
| SC-005, 009 | cổng merge `services/math` (`docs/KIEM-THU.md`): bộ AI 70 ca, thang gợi ý qua bộ lọc |
| SC-006 | `./mvnw -B -ntp verify -Dtest=DoiChieuChamV0Test` (chấm toàn ngân hàng, so tệp vàng xuất từ v0) |
| SC-007 | `./mvnw -B -ntp verify -Dtest=ThoiGianGiaSuTest` (p95 trên 50 lượt offline) |

Số đo thật ghi vào PR theo `docs/KIEM-THU.md`: lệnh, kết quả, SHA.
