export const THU_TUAN = [
  { ma: "T2", ten: "Thứ Hai" },
  { ma: "T3", ten: "Thứ Ba" },
  { ma: "T4", ten: "Thứ Tư" },
  { ma: "T5", ten: "Thứ Năm" },
  { ma: "T6", ten: "Thứ Sáu" },
  { ma: "T7", ten: "Thứ Bảy" },
  { ma: "CN", ten: "Chủ Nhật" },
] as const;

export type SlotLich = { thu: string; gio: string; viec: string };
export type NhacLich = { id: string; title: string; body: string; sendAt: string };

const EN_THU: Record<string, string> = {
  Mon: "Thứ Hai",
  Tue: "Thứ Ba",
  Wed: "Thứ Tư",
  Thu: "Thứ Năm",
  Fri: "Thứ Sáu",
  Sat: "Thứ Bảy",
  Sun: "Chủ Nhật",
};

export function thuHomNay(now: Date = new Date()) {
  const wd = new Intl.DateTimeFormat("en-US", { weekday: "short", timeZone: "Asia/Ho_Chi_Minh" }).format(now);
  return EN_THU[wd] || "";
}

/** Thứ Hai → Chủ Nhật của tuần đang xem, kèm ngày dương lịch (giờ Việt Nam). */
export function ngayTrongTuanHienTai(now: Date = new Date()) {
  const ymd = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Ho_Chi_Minh",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(now);
  const [y, m, d] = ymd.split("-").map(Number);
  const utcNoon = Date.UTC(y, m - 1, d, 12, 0, 0);
  const wd = new Date(utcNoon).getUTCDay();
  const tuThuHai = wd === 0 ? 6 : wd - 1;
  return THU_TUAN.map((t, i) => {
    const day = new Date(utcNoon);
    day.setUTCDate(day.getUTCDate() - tuThuHai + i);
    return { ...t, ngay: day.getUTCDate(), thang: day.getUTCMonth() + 1 };
  });
}

export function cacGioSlot(slots: { gio: string }[]) {
  return [...new Set(slots.map((s) => s.gio))].sort((a, b) => a.localeCompare(b, "vi"));
}

/** Buổi gần nhất từ hôm nay — vòng lại thứ Hai nếu hết tuần. */
export function thuBuoiTiep(slots: SlotLich[], homNay: string) {
  const idx = THU_TUAN.findIndex((t) => t.ten === homNay);
  const start = idx < 0 ? 0 : idx;
  for (let k = 0; k < 7; k++) {
    const ten = THU_TUAN[(start + k) % 7].ten;
    if (slots.some((s) => s.thu === ten)) return ten;
  }
  return slots[0]?.thu || "";
}

function tenThuTrong(text: string) {
  const hay = text.toLowerCase();
  return THU_TUAN.find((t) => hay.includes(t.ten.toLowerCase()))?.ten || "";
}

function chiGio(sendAt: string) {
  return /^\s*\d{1,2}:\d{2}\s*$/.test(sendAt);
}

function toiNay(title: string) {
  return /tối nay|hom nay|hôm nay/i.test(title);
}

/** Nhắc dính đúng buổi; «tối nay» chỉ dính nếu hôm nay có giờ đó. */
export function ghepNhacVaoSlot(slots: SlotLich[], rems: NhacLich[], homNay: string) {
  const used = new Set<string>();
  const theoSlot = slots.map((s) => {
    const nhac: string[] = [];
    for (const r of rems) {
      if (used.has(r.id)) continue;
      const thuTrong = tenThuTrong(r.sendAt);
      if (thuTrong && thuTrong === s.thu) {
        used.add(r.id);
        if (r.body) nhac.push(r.body);
        continue;
      }
      if (thuTrong) continue;
      if (toiNay(r.title)) {
        if (s.thu === homNay && r.sendAt === s.gio) {
          used.add(r.id);
          if (r.body) nhac.push(r.body);
        }
        continue;
      }
      if (chiGio(r.sendAt) && r.sendAt === s.gio && s.thu === thuBuoiTiep(slots, homNay)) {
        used.add(r.id);
        if (r.body) nhac.push(r.body);
      }
    }
    return { ...s, nhac };
  });
  const roi = rems.filter((r) => !used.has(r.id));
  return { slots: theoSlot, roi };
}
