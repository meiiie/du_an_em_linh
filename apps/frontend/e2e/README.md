# Nghiệm thu frontend U0–U15

Từ gốc repo (Node theo `apps/frontend/package.json`):

```bash
pnpm --filter frontend build
pnpm --filter frontend e2e
```

Playwright phục vụ `apps/frontend/dist/frontend/browser` bằng server SPA nhỏ ở
`127.0.0.1:4300`. Mọi `/api/**` được chặn bằng `page.route` trước khi điều hướng.
API chưa có mock làm test thất bại; không cần core, math, CSDL hay tài khoản thật.
Hai project Chromium: **1440 × 1000** và **390 × 844**.

- `E2E_DIST_DIR`: thư mục chứa `index.html` của bản build khác.
- `E2E_PORT`: đổi cổng server (mặc định 4300).
- `E2E_BASE_URL`: dùng frontend đã dựng ở URL khác, không khởi động server test.
- `E2E_CHROMIUM_EXECUTABLE`: đường dẫn Chromium có sẵn nếu cần.
- `E2E_OUTPUT_DIR`: đổi nơi lưu kết quả, trace và ảnh.
- `E2E_BACKEND=1`: chạy ba spec integration cũ với compose, cổng 4200.

`acceptance/ux.spec.ts` kiểm tra phần giao diện của các lát:

| Lát | Kiểm tra |
| --- | --- |
| U0–U1 | Computed box-shadow của phần tử hiện và pseudo-element bằng `none`; giảm chuyển động |
| U2 | Skeleton, rỗng, lỗi 500/501, thử lại; dialog giữ/trả focus; axe nếu có dependency |
| U3 | Nav học sinh/giáo viên/quản trị, guard vai trò, bottom nav 390, không tràn ngang |
| U4 | Gọi trang Học, kỹ năng bốn bậc và bài kế |
| U5–U6 | Kho, KaTeX, tài liệu, lật thẻ qua Enter; tải CSV đúng tên/BOM/nội dung/escaping |
| U7–U8 | SSE mock, ba cấp gợi ý, câu từ chối xin đáp án, aria-live, cột desktop/sheet mobile |
| U9 | Payload giao bài, chế độ/hạn; ẩn gia sư khi kiểm tra, xử lý 403 |
| U10 | KPI drill-down, tiến độ 3 mức, chi tiết và payload ghi đè có lý do |
| U11–U12 | Lọc mức/cổng, tài liệu/công thức; duyệt/chặn cần lý do |
| U13 | Lịch tuần và tạo buổi học bằng bàn phím |
| U14 | Admin và school admin, lọc/bulk người dùng, tạo/phân công lớp |
| U15 | Giao bài → hỏi gia sư → chấm/nộp → mức tăng → bài kế → lịch; ảnh sáng/tối cho cả hai project |

CSV và SSE là dữ liệu tổng hợp. U15 khôi phục phần bài làm đã kiểm bằng mock rồi
nhập/chấm bước kết luận và nộp qua UI. Không coi mock này là kiểm định toán,
kiểm tra luật gia sư ở core hoặc bằng chứng server thực sự chặn chat: mock 403 chỉ
kiểm tra phản ứng của UI. U0 ở đây là cổng computed style; cổng source/CI vẫn là
`node scripts/check-flat.mjs`. Upload, nhật ký thật và luật mastery của core cần
kiểm thử tại lane tương ứng.

Không thêm dependency trong lane này. Nếu `@axe-core/playwright` không tồn tại,
bốn ca axe ở mỗi project được đánh dấu skip kèm lý do; không báo đạt a11y.
Ảnh U15 ghi vào output của từng test, trace và ảnh lỗi giữ theo config.
