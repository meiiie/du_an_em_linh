import { gomHan, hanTheoBo, tenTrangThai } from './trinh-bay';
import { BankDetails, Item, StudentProfile } from '../../api/ux';

describe('Hạn bài giao', () => {
  const bank: Item<BankDetails>[] = [{id:'p',title:'CODE',subtitle:'Xét tính đơn điệu',status:'DA_PHAT_HANH',details:{code:'CODE',skillCode:'T12.NT.HS',level4:'NHAN_BIET',gate:'DAT',latex:'x**2'}}];
  const assignment = (studentId: string, dueAt='2026-10-20T13:00:00Z'): Item => ({id:studentId+dueAt,title:'CODE',subtitle:'Học sinh',status:'ON_TAP',details:{studentId,problemCode:'CODE',dueAt}});
  it('gom theo bài và hạn, đếm học sinh duy nhất đã nộp thay vì số lần nộp', () => {
    const history: Item = {id:'s',title:'CODE',subtitle:'DAT',status:'DA_NOP',details:{problemCode:'CODE'}};
    const histories = new Map<string,StudentProfile>([['a',{student:{...assignment('a'),details:{skills:[],submitted:2}},history:[history,history]}]]);
    const groups = gomHan([assignment('a'),assignment('a'),assignment('b'),assignment('a','2026-10-21T13:00:00Z')],bank,histories);
    expect(groups).toHaveLength(2);
    expect(groups[0]).toMatchObject({title:'Hàm số: đơn điệu và cực trị',submitted:1,total:2,dueAt:'2026-10-20T13:00:00Z'});
  });
  it('gom nhiều bài trong một bộ, dùng tên bộ và đếm mỗi lượt giao một lần', () => {
    const a = {...assignment('a'), details:{...assignment('a').details, tenBo:'Ôn tuần 1'}};
    const b = {...a,id:'b',title:'CODE2',details:{...a.details,problemCode:'CODE2'}};
    expect(gomHan([a,a,b],bank,new Map())).toMatchObject([{title:'Ôn tuần 1',total:2,submitted:0}]);
  });
  it('dùng title bộ từ API hiện hành khi problemCode đã tách riêng, bỏ đề bài dài', () => {
    const a={...assignment('a'),title:'Ôn tập tuần 2'};
    expect(gomHan([a],bank,new Map())[0].title).toBe('Ôn tập tuần 2');
    expect(gomHan([{...a,title:bank[0].subtitle}],bank,new Map())[0].title).toBe('Hàm số: đơn điệu và cực trị');
  });
  it('chủ đề không có trong danh mục tĩnh lấy tên kỹ năng từ API tiến độ', () => {
    const b={...bank[0],details:{...bank[0].details,skillCode:'T12.NT.XS'}};
    expect(gomHan([assignment('a')],[b],new Map(),new Map([['T12.NT.XS','Xác suất có điều kiện']]))[0].title).toBe('Xác suất có điều kiện');
  });
  it('không tạo hạn giả từ ngày thiếu hoặc hỏng', () => {
    expect(gomHan([assignment('a',''),assignment('b','sai')],bank,new Map())).toEqual([]);
  });
  it('lấy submitted chính thức, dùng setKey và tách lớp', () => {
    const a = {...assignment('a'),details:{...assignment('a').details,tenBo:'Ôn tuần 7',setKey:'set1',classId:'lop1',submitted:true}};
    const b = {...a,id:'b',details:{...a.details,studentId:'b',submitted:false}};
    const other = {...a,id:'c',details:{...a.details,setKey:'set2',classId:'lop2'}};
    expect(gomHan([a,b,other],bank,new Map()).map(g=>[g.title,g.submitted,g.total])).toEqual([['Ôn tuần 7',1,2],['Ôn tuần 7',1,1]]);
  });
  it('đọc dòng bộ không cần problemCode đơn, dùng title dự phòng và bỏ hạn hỏng', () => {
    const row = {id:'set',title:'Ôn tuần 7',subtitle:'2 bài',status:'ON_TAP',details:{dueAt:'2026-10-20T13:00:00Z',problemCodes:['A','B'],problemCount:2,studentCount:5,assignmentCount:10,submittedCount:7}};
    expect(hanTheoBo([row,{...row,id:'bad',details:{...row.details,dueAt:'bad'}}])).toEqual([{id:'set',title:'Ôn tuần 7',dueAt:row.details.dueAt,submitted:7,total:10,problemCount:2,studentCount:5}]);
  });
  it('nhãn Việt phân biệt chờ duyệt với không kiểm được', () => {
    expect(tenTrangThai('CHO_GIAO_VIEN_DUYET')).toBe('Chờ duyệt');
    expect(tenTrangThai('KHONG_KIEM_DUOC')).toBe('Không kiểm được');
    expect(tenTrangThai('UNKNOWN')).toBe('Chưa xác định');
  });
});
