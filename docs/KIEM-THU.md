# Kết quả kiểm thử

Chỉ ghi số chạy thật. Không suy diễn thêm. CI (`.github/workflows/ci.yml`) chạy `test:math`, `test:web` và Playwright e2e trên mọi push/PR.

## Lần bàn giao — 29/09/2026 (giờ Việt Nam)

### Cổng merge cho mọi PR đụng `services/math`

Chạy bằng script cổng trên máy dựng, ở head nhánh đã push. Số dưới là của PR #43 (head `434c79c`, cây dịch vụ toán trùng `main` sau đó):

| Bộ | Kết quả |
| --- | --- |
| `pytest` (`services/math`) | **1053 passed** |
| Nghiệm thu Sư phạm | **80/80** ca đạt |
| Bộ ác ý Kiểm định (288 ca), qua HTTP | TỪ CHỐI 242 · **ĐẠT NHẦM 0** · không từ chối 0 · không áp dụng 46 · **file_co 0** (không ca nào thực thi được) |
| Bộ ác ý Kiểm định, trong tiến trình | TỪ CHỐI 222 · ĐẠT NHẦM 0 · không từ chối 44 · không áp dụng 22 · file_co 0 |
| Bộ AI (70 ca) | **70/70** |
| Thang gợi ý mẫu Sư phạm qua bộ lọc lộ đáp án | bậc ba 0/1560, trùng phương 0/1560, hữu tỉ 0/1440 câu bị chặn nhầm; 32/32 câu lộ cài sẵn vẫn bị bắt |

### Web

- `pnpm test:web`: typecheck đạt, lint không cảnh báo, **85/85** unit.
- Playwright e2e trong repo (`apps/web/tests/e2e`): PLAYWRIGHT_REPO_PLACEHOLDER

### Bộ nghiệm thu Build + UX (180 test, 2 cỡ màn hình 1280/390, bộ khoá SHA của nhóm Build/UX)

Chạy cục bộ, `WORKERS=1`, DB nạp lại trước mỗi lần:

| Lần | SHA | Đỏ | Bỏ qua | Đạt |
| --- | --- | --- | --- | --- |
| 7 | `d8bc163` | 100 | 21 | 59 |
| 8 | `5b950a2` | 83 | 21 | 76 |
BUILD_UX_PLACEHOLDER

## Còn mở

CON_MO_PLACEHOLDER

## Lần dựng nguyên mẫu (lịch sử)

- `pytest`: 11/11 hàm; Playwright 10/10; unit 18/18 (harness + kho).
