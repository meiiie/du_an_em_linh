import { AssignmentDetails, BankDetails, DeadlineDetails, Item, StudentProfile } from '../../api/ux';

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
  return b.subtitle || (/^[A-Z0-9][A-Z0-9._-]+$/.test(b.title) ? tenKyNang(b.details.skillCode, b.details.skillName) : b.title);
}
export interface HanBai { id: string; title: string; dueAt: string; submitted: number; total: number; problemCount?: number; studentCount?: number; }
export function gomHan(assignments: Item<AssignmentDetails>[], bank: Item<BankDetails>[], histories: Map<string, StudentProfile>, skillNames: ReadonlyMap<string, string> = new Map()): HanBai[] {
  const groups = new Map<string, HanBai & { entries: Set<string> }>();
  for (const a of assignments) {
    const dueAt = a.details['dueAt'];
    const studentId = a.details['studentId'];
    if (typeof dueAt !== 'string' || !Number.isFinite(new Date(dueAt).getTime()) || typeof studentId !== 'string') continue;
    const code = String(a.details['problemCode'] ?? a.title);
    const b = bank.find(b => (b.details.code || b.title) === code);
    const topTitle = typeof a.details['problemCode'] === 'string' && a.title !== code && a.title !== b?.subtitle && a.title !== b?.title ? a.title : undefined;
    const setName = [a.details['tenBo'], a.details['setName'], a.details['title'], topTitle].find(v => typeof v === 'string' && v.trim());
    const title = typeof setName === 'string' ? setName.trim() : tenKyNang(b?.details.skillCode ?? '', String(a.details['topicName'] ?? skillNames.get(b?.details.skillCode ?? '') ?? ''));
    const setId = a.details.setKey ?? a.details['setId'] ?? a.details['maBo'];
    const key = JSON.stringify([a.details.classId, setId ?? title, dueAt, a.status]);
    let group = groups.get(key);
    if (!group) {
      group = { id: key, title, dueAt, submitted: 0, total: 0, entries: new Set() };
      groups.set(key, group);
    }
    const entry = JSON.stringify([studentId, code]);
    if (group.entries.has(entry)) continue;
    group.entries.add(entry);
    group.total++;
    if (a.details.submitted ?? histories.get(studentId)?.history.some(h => h.status === 'DA_NOP' && (h.details['problemCode'] ?? h.title) === code)) group.submitted++;
  }
  return [...groups.values()].sort((a,b) => new Date(a.dueAt).getTime() - new Date(b.dueAt).getTime());
}

/** Dùng số lượt nộp của bộ do máy chủ tính, kể cả bài chờ giáo viên xem. */
export function hanTheoBo(deadlines: Item<DeadlineDetails>[]): HanBai[] {
  return deadlines.filter(a => typeof a.details.dueAt === 'string' && Number.isFinite(new Date(a.details.dueAt).getTime()))
    .map(a => ({ id: a.details.setKey || a.id, title: a.details.tenBo?.trim() || a.title || 'Bài được giao',
      dueAt: a.details.dueAt!, submitted: a.details.submittedCount ?? 0, total: a.details.assignmentCount ?? 0, problemCount: a.details.problemCount ?? a.details.problemCodes?.length, studentCount: a.details.studentCount }))
    .sort((a, b) => new Date(a.dueAt).getTime() - new Date(b.dueAt).getTime());
}

/** Một mục cho mỗi lượt giao, kể cả lượt không có hạn. */
export function gomLuotGiao(rows: readonly Item[]): Item[] {
  const groups = new Map<string, Item[]>();
  for (const row of rows) {
    const key = String(row.details['setKey'] ?? JSON.stringify([row.title, row.details['assignedAt'], row.details['dueAt'], row.status]));
    groups.set(key, [...(groups.get(key) ?? []), row]);
  }
  return [...groups].map(([id, entries]) => {
    const first = entries[0];
    const problems = new Set(entries.map(e => e.details['problemCode'] ?? e.id));
    const students = new Set(entries.map(e => e.details['studentId'] ?? e.id));
    return {...first, id, subtitle: `${problems.size} bài · ${students.size} học sinh`};
  });
}
