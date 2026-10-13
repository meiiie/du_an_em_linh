import { BankDetails, Item, StudentProfile } from '../../api/ux';

// Tên từ danh mục dữ liệu hiện hành data/dev/nguyen-trai/danh-muc.json.
const KY_NANG: Record<string, string> = {
  'T12.NT.HS': 'Hàm số: đơn điệu và cực trị', 'T11.NT.LG': 'Lượng giác',
  'T11.NT.DS': 'Dãy số và cấp số', 'T11.NT.DH': 'Đạo hàm',
  'T12.NT.TP': 'Tích phân', 'T12.NT.KG': 'Hình học không gian', 'T10.NT.XS': 'Xác suất',
};
export function tenKyNang(code: string, name?: string): string {
  return name || KY_NANG[code] || 'Kỹ năng toán học';
}
export function tenTrangThai(status: string): string {
  return ({ DAT: 'Đã kiểm', SAI: 'Sai', KHONG_KIEM_DUOC: 'Không kiểm được',
    KHONG_CHAM_DUOC: 'Không chấm được', GV_DUYET: 'Giáo viên đã duyệt',
    CHO_GIAO_VIEN_DUYET: 'Chờ duyệt', DA_PHAT_HANH: 'Đã phát hành', NHAP: 'Bản nháp',
    KHOA: 'Đã khóa', BI_CHAN: 'Bị chặn', CHUA_LAM: 'Chưa làm', DANG_LAM: 'Đang làm',
    DA_NOP: 'Đã nộp', DA_GIAO: 'Đã giao', BIET: 'Biết', HIEU: 'Hiểu', VAN_DUNG: 'Vận dụng',
  } as Record<string, string>)[status] ?? 'Chưa xác định';
}
export function tenBai(b: Item<BankDetails>): string {
  return b.subtitle || (/^[A-Z0-9][A-Z0-9._-]+$/.test(b.title) ? tenKyNang(b.details.skillCode) : b.title);
}
export interface HanBai { id: string; code: string; title: string; dueAt: string; submitted: number; total: number; }
export function gomHan(assignments: Item[], bank: Item<BankDetails>[], histories: Map<string, StudentProfile>): HanBai[] {
  const groups = new Map<string, HanBai & { students: Set<string> }>();
  for (const a of assignments) {
    const dueAt = a.details['dueAt'];
    const studentId = a.details['studentId'];
    if (typeof dueAt !== 'string' || !Number.isFinite(new Date(dueAt).getTime()) || typeof studentId !== 'string') continue;
    const code = String(a.details['problemCode'] ?? a.title);
    const key = `${code}|${dueAt}|${a.status}`;
    let group = groups.get(key);
    if (!group) {
      const b = bank.find(b => (b.details.code || b.title) === code);
      group = { id: key, code, title: b ? tenBai(b) : 'Bài được giao', dueAt, submitted: 0, total: 0, students: new Set() };
      groups.set(key, group);
    }
    if (group.students.has(studentId)) continue;
    group.students.add(studentId);
    group.total++;
    if (histories.get(studentId)?.history.some(h => h.status === 'DA_NOP' && (h.details['problemCode'] ?? h.title) === code)) group.submitted++;
  }
  return [...groups.values()].sort((a,b) => new Date(a.dueAt).getTime() - new Date(b.dueAt).getTime());
}
