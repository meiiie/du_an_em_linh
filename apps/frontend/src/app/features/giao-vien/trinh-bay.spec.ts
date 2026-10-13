import { gomHan, tenTrangThai } from './trinh-bay';
import { BankDetails, Item, StudentProfile } from '../../api/ux';

describe('Hạn bài giao', () => {
  const bank: Item<BankDetails>[] = [{id:'p',title:'CODE',subtitle:'Xét tính đơn điệu',status:'DA_PHAT_HANH',details:{code:'CODE',skillCode:'T12.NT.HS',level4:'NHAN_BIET',gate:'DAT',latex:'x**2'}}];
  const assignment = (studentId: string, dueAt='2026-10-20T13:00:00Z'): Item => ({id:studentId+dueAt,title:'CODE',subtitle:'Học sinh',status:'ON_TAP',details:{studentId,problemCode:'CODE',dueAt}});
  it('gom theo bài và hạn, đếm học sinh duy nhất đã nộp thay vì số lần nộp', () => {
    const history: Item = {id:'s',title:'CODE',subtitle:'DAT',status:'DA_NOP',details:{problemCode:'CODE'}};
    const histories = new Map<string,StudentProfile>([['a',{student:{...assignment('a'),details:{skills:[],submitted:2}},history:[history,history]}]]);
    const groups = gomHan([assignment('a'),assignment('a'),assignment('b'),assignment('a','2026-10-21T13:00:00Z')],bank,histories);
    expect(groups).toHaveLength(2);
    expect(groups[0]).toMatchObject({title:'Xét tính đơn điệu',submitted:1,total:2,dueAt:'2026-10-20T13:00:00Z'});
  });
  it('không tạo hạn giả từ ngày thiếu hoặc hỏng', () => {
    expect(gomHan([assignment('a',''),assignment('b','sai')],bank,new Map())).toEqual([]);
  });
  it('nhãn Việt phân biệt chờ duyệt với không kiểm được', () => {
    expect(tenTrangThai('CHO_GIAO_VIEN_DUYET')).toBe('Chờ duyệt');
    expect(tenTrangThai('KHONG_KIEM_DUOC')).toBe('Không kiểm được');
    expect(tenTrangThai('UNKNOWN')).toBe('Chưa xác định');
  });
});
