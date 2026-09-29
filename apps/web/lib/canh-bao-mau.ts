/**
 * Cảnh báo kẹt của dữ liệu demo (seed). Dùng chung cho seed và /api/test/reset: đặt lại một học sinh phải trả về đúng trạng
 * thái lúc mới nạp, gồm cả cảnh báo mẫu (UXT-09-a/e cần cảnh báo 'Chi — Điểm tới hạn').
 */
export const CANH_BAO_MAU = [
  {
    email: "hs.chi@demo.local",
    skillCode: "T12.DH.02",
    reason: "Kẹt 3 lượt ở T12.DH.02 (tính đạo hàm). Bản ghi tổng hợp để thấy cảnh báo, không phải học sinh thật.",
  },
] as const;
