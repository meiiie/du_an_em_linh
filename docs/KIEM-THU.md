# Kết quả kiểm thử lần dựng nguyên mẫu

Chạy trên máy dựng nguyên mẫu. Không suy diễn thêm. CI lặp lại cùng lệnh trên GitHub Actions.

- `pytest` trong `services/math`: **11/11 hàm đạt**
  - Tầng 1: 102 ca trong `kiemdinh/bo-de-kiem-thu/cac-ca.yaml`, lệch rỗng so với `ket-qua-tang1.json`
  - Khung 5 bước: 16 ca trong `cac-ca-5-buoc.yaml`, đúng mã bước và ô
  - Lọc lộ đáp án: 70 tin nhắn, khớp cờ trong `ket-qua-loc-lo-dap-an.json`
  - Chấm payload sản phẩm (`k` từ 0): cùng 16 ca, thêm một ca cặp dòng đạo hàm và bốn hàm máy tự giải
  - Sandbox: 5 hàm (API không import SymPy, quá hạn thì bị giết, chấm qua tiến trình, bộ lọc chặn và bộ lọc cho qua)
- `pnpm test:web`: typecheck đạt; lint không cảnh báo; **18/18** unit (harness + kho)
- Playwright: **10/10** (đăng nhập, phiếu, gia sư, kho, tổng quan, 40/44)
