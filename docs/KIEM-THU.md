# Kết quả kiểm thử

Chỉ ghi số chạy thật. Không suy diễn thêm. CI (`.github/workflows/ci.yml`) chạy `test:math`, `test:web` và Playwright e2e trên mọi push/PR.

## Lần bàn giao — 29/09/2026 (giờ Việt Nam)

### Cổng merge cho mọi PR đụng `services/math`

Chạy bằng script cổng trên máy dựng, ở head nhánh đã push. Số dưới là của PR #43 (head `434c79c`). PR #46 (head `51121e7` và `58f6a19`) cho đúng các số này ở cả 5 bộ đầu. Cây `services/math` của `main` hiện tại trùng cây đã đo.

| Bộ | Kết quả |
| --- | --- |
| `pytest` (`services/math`) | **1053 passed** |
| Nghiệm thu Sư phạm | **80/80** ca đạt |
| Bộ ác ý Kiểm định (288 ca), qua HTTP | TỪ CHỐI 242 · **ĐẠT NHẦM 0** · không từ chối 0 · không áp dụng 46 · **file_co 0** (không ca nào thực thi được) |
| Bộ ác ý Kiểm định, trong tiến trình | TỪ CHỐI 222 · ĐẠT NHẦM 0 · không từ chối 44 · không áp dụng 22 · file_co 0 |
| Bộ AI (70 ca) | **70/70** |
| Thang gợi ý mẫu Sư phạm qua bộ lọc lộ đáp án | bậc ba 0/1560, trùng phương 0/1560, hữu tỉ 0/1440 câu bị chặn nhầm; 32/32 câu lộ cài sẵn vẫn bị bắt |

### Web

- `pnpm test:web`: typecheck đạt, lint không cảnh báo, **88/88** unit.
- Playwright e2e trong repo (`apps/web/tests/e2e`), chạy cục bộ trên nhánh bàn giao (main `1b9b8f5` + tài liệu này): **25/25**. CI chạy cùng bộ này trên mọi PR.

### Bộ nghiệm thu Build + UX (180 test, 2 cỡ màn hình 1280/390, bộ khoá SHA của nhóm Build/UX)

Chạy cục bộ, `WORKERS=1`, DB nạp lại trước mỗi lần:

| Lần | SHA | Đỏ | Bỏ qua | Đạt |
| --- | --- | --- | --- | --- |
| 7 | `d8bc163` | 100 | 21 | 59 |
| 8 | `5b950a2` | 83 | 21 | 76 |
| 9 | `575c066` | 65 | 21 | 94 |
| 10 | `3518aeb` (main `1bb2e52` + #44 + #45 + bản đầu #46) | 48 | 19 | 113 |
| 12 | `1b9b8f5` (main sau #47) | **39** | 19 | **122** |

Lần 11 là chạy chọn lọc `-g "UX-07 (|UX-09 (|F-10"` trên cây `730197a` (trùng cây `1b9b8f5`): 38 đạt, 2 bỏ qua, 0 đỏ. Ở lần 12 (toàn bộ), mọi tiêu chí UX-07 (a–l), UX-09 (a–g) và F-10 đều đạt ở cả hai cỡ màn hình có chạy.

Tài khoản: HS = `hs.binh`. Lý do: khung UX đặt lại dữ liệu theo email `hs.X@demo.local` trích từ YAML. Chạy với tài khoản riêng `hs.build@test.local` thì tài khoản đó không được đặt lại, nên chuỗi UX-07 đỏ vì bắt đầu giữa bài. Có một lần chạy dở để làm bằng chứng: `ket-qua/lan10a-hsbuild-dung-giua-3518aeb.txt`.

## Còn mở

Bộ Build+UX lần 12 còn 39 đỏ (đếm cả hai cỡ màn hình):

- **UX-03 b, c**: nộp hàng X ngược thứ tự thì chỉ ô X đỏ; sai thứ tự kèm một ô sai thật.
- **UX-04 a–d**: mốc thừa / bỏ sót mốc trong bảng xét dấu (ERR.DH.31, ERR.DH.24).
- **UX-05 b, c**: kết luận sai hai ô; giáo viên thấy đủ mọi lỗi của một bài nộp.
- **UX-06 d**: trang Học và trang làm bài khớp nhau về bước đang làm.
- **UX-01 f**: dòng «Máy hiểu là…» trước khi chấm (F-04).
- **UX-08 a–e**: giao đề cho một học sinh / cả lớp, phân biệt hai đề cùng hàm, chặn giao đề bị chặn / chưa đủ 3 tầng.
- **UX-10 g** (390): phần tử chạm trên màn làm bài và khung gia sư.
- Spec ngoài UX:
  - `gui-gia-tri-cong-thuc` (ô dự phòng văn bản thường; MathLive a1/a2 ở 1280);
  - `chuoi-doc-hai` (2 ca, 1280);
  - `tu-sap-moc` (1280);
  - `cap-goi-y-luu-db` (1280).
- Hai spec phụ thuộc hành vi cũ:
  - `tu-sap-moc` mở DH12-TH-02 mà không đặt lại dữ liệu và chờ bước TXĐ. Từ khi lưu tiến trình (UX-06), bài mở ở bước đang dở.
  - `cap-goi-y-luu-db` chờ chip «Gợi ý bước này» ở ngữ cảnh mới nâng cấp c1→c1+1. Điều này ngược với UXT-07-a/d: chip nhắc lại cấp đang mở, lên cấp bằng «Gợi ý thêm».
  - Cần nhóm Build/UX chốt.
- Tài khoản Build riêng (`hs.build@test.local`): UX-08 chọn `hs.binh` / «Bình», UX-09 lọc tên /Chi|Bình|An/. Đổi HS sang `hs.build` thì các tiêu chí này lệch, và khung UX không đặt lại tài khoản đó.
- Bản vá Kiểm định 0004 (luật lộ biểu thức y′, ngoại lệ khung ngắn) chưa có trong `kiemdinh/ban-va/`.
- README Kiểm định chưa ghi SHA merge của 0003 (`cb57afd`).
- Deploy Render: secret `RENDER_DEPLOY_HOOK` chưa đặt, nên bản host không tự cập nhật. Chủ repo phải đặt.
- `release-please` đỏ trên main từ trước các PR hôm nay.

## Lần dựng nguyên mẫu (lịch sử)

- `pytest`: 11/11 hàm; Playwright 10/10; unit 18/18 (harness + kho).
