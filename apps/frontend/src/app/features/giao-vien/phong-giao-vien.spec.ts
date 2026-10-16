import { KichThuocTrang } from '../../shared/ui/kich-thuoc-trang';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { LopDangChon } from '../../core/lop-dang-chon';
import { PhongGiaoVien } from './phong-giao-vien';

const bankPageUrl=(cl='lop1',page=1,size=20,q='')=>'/api/gv/ngan-hang?lopId='+cl+'&'+new URLSearchParams({page:String(page),size:String(size),q,level4:'',gate:''});

async function setup(subtitle = 'Xét tính đơn điệu', boBai = 0) {
  const lop = { id: signal('lop1'), ds: signal([{ id: 'lop1', title: '12A1' }, { id: 'lop2', title: '12A2' }]), dangTai: signal(false), loi: signal(false), soChoDuyet: signal(0), tai: async () => {} };
  TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), { provide: LopDangChon, useValue: lop }] });
  const fixture = TestBed.createComponent(PhongGiaoVien);
  fixture.componentRef.setInput('trang', 'giao');
  fixture.detectChanges();
  const http = TestBed.inject(HttpTestingController);
  http.expectOne('/api/gv/ngan-hang?lopId=lop1').flush(Array.from({ length: 45 }, (_, i) => ({ id: 'b' + i, title: 'Bài ' + i, subtitle: i === 0 ? subtitle : '', status: i === 44 ? 'NHAP' : 'DA_PHAT_HANH', details: { level4: 'NHAN_BIET', skillCode: 'T12.DH.03', gate: 'DAT', latex: 'x' } })));
  http.expectOne('/api/gv/tien-do?lopId=lop1').flush(Array.from({ length: 30 }, (_, i) => ({ id: 'hs' + i, title: 'Học sinh ' + i, subtitle: '', status: 'STUDENT', details: { skills: [], submitted: 0 } })));
  http.expectOne('/api/gv/giao?lopId=lop1').flush([]);
  http.expectOne('/api/gv/tong-quan?lopId=lop1').flush({students:30,stuck:0,pending:0,assignments:boBai*10,boBai,distribution:{},deadlines:[]});
  await fixture.whenStable();
  const el = fixture.nativeElement as HTMLElement;
  const lists = () => el.querySelectorAll('fieldset');
  const search = async (index: number, value: string) => { const input = lists()[index].querySelector('input[type=search]')! as HTMLInputElement; input.value = value; input.dispatchEvent(new Event('input')); await fixture.whenStable(); };
  return { fixture, el, lists, search, http, lop };
}

describe('Giao bài với ngân hàng và lớp đầy đủ', () => {


  it('hộp tải tài liệu chỉ có một tiêu đề', async () => {
    const t=await setup();t.fixture.componentRef.setInput('trang','tai-lieu');TestBed.tick();
    t.http.expectOne('/api/gv/kho?lopId=lop1').flush({formulas:[],documents:[]});await t.fixture.whenStable();
    for(const d of t.el.querySelectorAll<HTMLDialogElement>('dialog')){d.showModal=()=>{d.open=true;};d.close=()=>{d.open=false;};}
    [...t.el.querySelectorAll<HTMLButtonElement>('button')].find(b=>b.textContent==='Thêm tài liệu')!.click();await t.fixture.whenStable();
    expect([...t.el.querySelectorAll('dialog[open] h2')].filter(h=>h.textContent==='Thêm tài liệu')).toHaveLength(1);
    t.http.verify();
  });
  it('tên bài giao render toán và phần mô tả công thức bỏ các tầng trống', async () => {
    const t=await setup('Xét tính đơn điệu $x^2$');
    expect(t.lists()[0].querySelector('.katex')).not.toBeNull();
    expect(t.lists()[0].querySelector('annotation')?.textContent).toBe('x^2');
    const component=t.fixture.componentInstance as unknown as {chiTiet(c:unknown):string};
    expect(component.chiTiet({details:{statement:'Góc nhân đôi',tier1:'',tier2:''}})).toBe('Góc nhân đôi');
    t.http.verify();
  });
  it('bộ đếm bộ bài dùng boBai của tổng quan', async () => {
    const t = await setup(undefined, 3);
    expect(t.el.querySelector('.list-summary')?.textContent).toContain('3 bộ bài');
    t.http.verify();
  });
  it('tên truy cập của bài giữ tên gốc cùng đề bài và mức khi hiển thị đề bài', async () => {
    const t = await setup();
    const checkbox = t.lists()[0].querySelector('input[type=checkbox]')!;
    expect(checkbox.getAttribute('aria-label')).toBe('Bài 0 · Xét tính đơn điệu · Nhận biết');
    expect(checkbox.closest('label')?.textContent).toContain('Xét tính đơn điệu · Nhận biết');
    t.http.verify();
  });
  it('chỉ hiện bài phát hành, tìm bài độc lập với 30 học sinh', async () => {
    const t = await setup();
    expect(t.lists()[0].querySelectorAll('[type=checkbox]')).toHaveLength(20);
    expect(t.lists()[1].querySelectorAll('[type=checkbox]')).toHaveLength(20);
    await t.search(0, 'Bài 43');
    expect(t.lists()[0].querySelectorAll('[type=checkbox]')).toHaveLength(1);
    expect(t.lists()[1].querySelectorAll('[type=checkbox]')).toHaveLength(20);
    await t.search(1, 'Học sinh 29');
    expect(t.lists()[1].querySelectorAll('[type=checkbox]')).toHaveLength(1);
    expect(t.lists()[0].textContent).toContain('Bài 43');
    t.http.verify();
  });
  it('phân trang vẫn giữ lựa chọn và tìm được bài cuối ngân hàng', async () => {
    const t = await setup();
    (t.lists()[0].querySelector('[type=checkbox]') as HTMLInputElement).click();
    const next = () => [...t.lists()[0].querySelectorAll('button')].find(b => b.textContent?.includes('Trang sau'))!;
    next().click(); await t.fixture.whenStable();
    expect(t.lists()[0].textContent).toContain('Bài 20');
    next().click(); await t.fixture.whenStable();
    expect(t.lists()[0].querySelectorAll('[type=checkbox]')).toHaveLength(4);
    expect(t.lists()[0].textContent).toContain('Bài 43');
    expect(t.lists()[0].textContent).not.toContain('Bài 44');
    await t.search(0, 'Bài 0');
    expect((t.lists()[0].querySelector('[type=checkbox]') as HTMLInputElement).checked).toBe(true);
    t.http.verify();
  });
  it('giữ lựa chọn khi tìm bài khác, hiển thị hướng dẫn khi không có kết quả', async () => {
    const t = await setup();
    (t.lists()[0].querySelector('[type=checkbox]') as HTMLInputElement).click();
    await t.fixture.whenStable();
    await t.search(0, 'không tồn tại');
    expect(t.lists()[0].textContent).toContain('Chọn bài (1)');
    expect(t.lists()[0].textContent).toContain('Đổi từ khóa để tìm lại.');
    await t.search(0, '');
    expect((t.lists()[0].querySelector('[type=checkbox]') as HTMLInputElement).checked).toBe(true);
    t.http.verify();
  });
  it('giao hạn 21 giờ Việt Nam, tải lại và cập nhật số bộ theo lượt', async () => {
    const t=await setup();
    const name=t.el.querySelector<HTMLInputElement>('form > label input')!;
    name.value='Ôn cực trị';name.dispatchEvent(new Event('input'));
    const due=t.el.querySelector<HTMLInputElement>('#giao-han-nop')!;
    due.value='16/10/2026 21:00';due.dispatchEvent(new Event('input'));
    (t.lists()[0].querySelector('[type=checkbox]') as HTMLInputElement).click();
    await t.fixture.whenStable();
    t.el.querySelector('form')!.dispatchEvent(new Event('submit',{cancelable:true}));
    const request=t.http.expectOne(r=>r.method==='POST'&&r.url==='/api/gv/giao');
    expect(request.request.body.dueAt).toBe('2026-10-16T14:00:00.000Z');
    request.flush({message:'Đã giao',count:2});
    await new Promise(r=>setTimeout(r,0));TestBed.tick();
    t.http.expectOne('/api/gv/giao?lopId=lop1').flush(['hs1','hs2'].map((studentId,i)=>({id:String(i),title:'Ôn cực trị',subtitle:'An',status:'ON_TAP',details:{setKey:'luot-moi',studentId,problemCode:'P1',dueAt:'2026-10-16T14:00:00Z'}})));
    t.http.expectOne('/api/gv/tong-quan?lopId=lop1').flush({students:30,stuck:0,pending:0,assignments:2,boBai:1,distribution:{},deadlines:[]});
    await t.fixture.whenStable();
    expect(t.el.querySelector('.list-summary')?.textContent).toContain('1 bộ bài');
    expect(t.el.querySelector('.row-list')?.textContent).toContain('Ôn cực trị');
    expect(t.el.querySelector('.row-list')?.textContent).toContain('21:00');
    t.http.verify();
  });
  it('Enter trong ô tìm kiếm không gửi bộ bài đã đủ điều kiện giao', async () => {
    const t = await setup();
    const name = t.el.querySelector('form > label input') as HTMLInputElement;
    name.value = 'Ôn tập tuần'; name.dispatchEvent(new Event('input'));
    (t.lists()[0].querySelector('[type=checkbox]') as HTMLInputElement).click();
    await t.fixture.whenStable();
    expect((t.el.querySelector('form button[type=submit]') as HTMLButtonElement).disabled).toBe(false);
    for (const list of t.lists()) {
      const event = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true, cancelable: true });
      list.querySelector('input[type=search]')!.dispatchEvent(event);
      expect(event.defaultPrevented).toBe(true);
    }
    t.http.expectNone(request => request.method === 'POST');
    t.http.verify();
  });
  it('đổi lớp xóa lựa chọn và tìm kiếm, tải dữ liệu đúng lớp mới', async () => {
    const t = await setup();
    (t.lists()[0].querySelector('[type=checkbox]') as HTMLInputElement).click();
    await t.search(0, 'Bài 43');
    const select = t.el.querySelector('[aria-label="Chọn lớp"]') as HTMLSelectElement;
    select.value = 'lop2'; select.dispatchEvent(new Event('change'));
    TestBed.tick();
    t.http.expectOne('/api/gv/ngan-hang?lopId=lop2').flush([]);
    t.http.expectOne('/api/gv/tien-do?lopId=lop2').flush([]);
    t.http.expectOne('/api/gv/giao?lopId=lop2').flush([]);
    t.http.expectOne('/api/gv/tong-quan?lopId=lop2').flush({students:0,stuck:0,pending:0,assignments:0,boBai:0,distribution:{},deadlines:[]});
    await t.fixture.whenStable();
    expect(t.lists()[0].textContent).toContain('Chọn bài (0)');
    expect((t.lists()[0].querySelector('input[type=search]') as HTMLInputElement).value).toBe('');
    expect(t.lists()[0].textContent).toContain('Chưa có bài đã phát hành');
    t.http.verify();
  });
});

describe('Danh sách giáo viên trên màn hẹp', () => {
  for (const trang of ['ngan-hang','tien-do'] as const) {
    it(trang + ': mỗi trang 10 mục và tìm kiếm đặt lại trang', async () => {
      const lop={id:signal('lop1'),ds:signal([]),dangTai:signal(false),loi:signal(false),soChoDuyet:signal(0),tai:async()=>{}};
      TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([]),{provide:LopDangChon,useValue:lop}]});
      TestBed.inject(KichThuocTrang).soMuc.set(10);
      const f=TestBed.createComponent(PhongGiaoVien);f.componentRef.setInput('trang',trang);f.detectChanges();
      const http=TestBed.inject(HttpTestingController);
      if(trang==='tien-do')http.expectOne('/api/gv/tien-do?lopId=lop1').flush(Array.from({length:21},(_,i)=>({id:'hs'+i,title:'Học sinh '+i,subtitle:'',status:'',details:{skills:[],submitted:0}})));
      if(trang==='ngan-hang')http.expectOne('/api/gv/ky-nang?lopId=lop1').flush([]);if(trang==='ngan-hang')http.expectOne(bankPageUrl('lop1',1,10)).flush(Array.from({length:10},(_,i)=>({id:'b'+i,title:'Bài '+i,subtitle:'',status:'DA_PHAT_HANH',details:{code:'CODE'+i,skillCode:'HS',level4:'NHAN_BIET',gate:'DAT',latex:'x'}})),{headers:{'X-Total-Count':'21'}});
      await f.whenStable();
      const el=f.nativeElement as HTMLElement;
      expect(el.querySelectorAll('tbody tr')).toHaveLength(10);
      expect(el.querySelector('.pagination')?.textContent).toContain('Trang 1 / 3');
      [...el.querySelectorAll<HTMLButtonElement>('button')].find(b=>b.textContent?.trim()==='Trang sau')!.click();TestBed.tick();
      if(trang==='ngan-hang')http.expectOne(bankPageUrl('lop1',2,10)).flush(Array.from({length:10},(_,i)=>({id:'b'+(i+10),title:'Bài '+(i+10),subtitle:'',status:'DA_PHAT_HANH',details:{code:'CODE'+(i+10),skillCode:'HS',level4:'NHAN_BIET',gate:'DAT',latex:'x'}})),{headers:{'X-Total-Count':'21'}});await f.whenStable();
      expect(el.querySelector('tbody tr')?.textContent).toContain(trang==='ngan-hang'?'Bài 10':'Học sinh 10');
      const search=el.querySelector('[type=search]') as HTMLInputElement;search.value=trang==='ngan-hang'?'Bài 20':'Học sinh 20';search.dispatchEvent(new Event('input'));TestBed.tick();
      if(trang==='ngan-hang')http.expectOne(bankPageUrl('lop1',1,10,'Bài 20')).flush([{id:'b20',title:'Bài 20',subtitle:'',status:'DA_PHAT_HANH',details:{code:'CODE20',skillCode:'HS',level4:'NHAN_BIET',gate:'DAT',latex:'x'}}],{headers:{'X-Total-Count':'1'}});await f.whenStable();
      expect(el.querySelectorAll('tbody tr')).toHaveLength(1);
      http.verify();
    });
  }
});

describe('Tổng quan với hạn theo bài', () => {
  it('chỉ hiển thị 5 bộ gần nhất và liên kết tới Giao bài', async () => {
    const lop={id:signal('lop1'),ds:signal([]),dangTai:signal(false),loi:signal(false),soChoDuyet:signal(0),tai:async()=>{}};
    TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([]),{provide:LopDangChon,useValue:lop}]});
    const f=TestBed.createComponent(PhongGiaoVien);f.detectChanges();const http=TestBed.inject(HttpTestingController);
    http.expectOne('/api/gv/canh-bao?lopId=lop1').flush([]);
    http.expectOne('/api/gv/tong-quan?lopId=lop1').flush({students:1,stuck:0,pending:0,assignments:80,boBai:8,distribution:{},deadlines:Array.from({length:8},(_,i)=>({id:String(i),title:'Ôn tập '+i,subtitle:'2 bài',status:'ON_TAP',details:{tenBo:'Ôn tập '+i,setKey:String(i),problemCodes:['A','B'],problemCount:2,studentCount:5,assignmentCount:10,submittedCount:3,dueAt:'2026-10-'+(20+i)+'T13:00:00Z'}})).reverse()});
    await f.whenStable();const el=f.nativeElement as HTMLElement;
    expect(el.querySelectorAll('.row-list li')).toHaveLength(5);
    expect(el.querySelector('.row-list strong')?.textContent).toBe('Ôn tập 0');
    expect(el.querySelector('.row-list')?.textContent).toContain('Thứ Ba, 20/10 · 20:00 · 3/10 đã nộp');
    expect(el.querySelector('.row-list')?.textContent).toContain('2 bài · 5 học sinh');
    http.expectNone(r => /\/(giao|ngan-hang|tien-do|hoc-sinh)\b/.test(r.url));
    expect([...el.querySelectorAll('a')].find(a=>a.textContent?.includes('Xem tất cả'))?.getAttribute('href')).toBe('/gv/giao');
    http.verify();
  });
  it('dùng giao bài đầy đủ, đếm người đã nộp và hiển thị tên thay mã', async () => {
    const lop={id:signal('lop1'),ds:signal([{id:'lop1',title:'12A1'}]),dangTai:signal(false),loi:signal(false),soChoDuyet:signal(0),tai:async()=>{}};
    TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([]),{provide:LopDangChon,useValue:lop}]});
    const f=TestBed.createComponent(PhongGiaoVien);f.detectChanges();
    const http=TestBed.inject(HttpTestingController);
    http.expectOne('/api/gv/canh-bao?lopId=lop1').flush([]);
    http.expectOne('/api/gv/tong-quan?lopId=lop1').flush({students:2,stuck:0,pending:0,assignments:2,boBai:1,distribution:{NHAN_BIET:1,THONG_HIEU:1},deadlines:[{id:"old",title:"CODE",details:{dueAt:"2026-10-20T13:00:00Z",studentId:"a",problemCode:"CODE"}}]});
    await new Promise(r=>setTimeout(r,0));
    TestBed.tick();
    await new Promise(r=>setTimeout(r,0));
    http.expectOne('/api/gv/giao?lopId=lop1').flush(['a','b'].map(id=>({id,title:'CODE',subtitle:id,status:'ON_TAP',details:{studentId:id,problemCode:'CODE',dueAt:'2026-10-20T13:00:00Z'}})));
    http.expectOne('/api/gv/ngan-hang?lopId=lop1').flush([{id:'p',title:'CODE',subtitle:'Xét tính đơn điệu',status:'DA_PHAT_HANH',details:{code:'CODE',skillCode:'T12.NT.HS',level4:'NHAN_BIET',gate:'DAT',latex:'x**2'}}]);
    http.expectOne('/api/gv/tien-do?lopId=lop1').flush(['a','b'].map(id=>({id,title:id,subtitle:'',status:'STUDENT',details:{skills:[],submitted:id==='a'?1:0}})));
    await new Promise(r=>setTimeout(r,0));TestBed.tick();
    http.expectOne('/api/gv/hoc-sinh/a?lopId=lop1').flush({student:{id:'a',title:'a',subtitle:'',status:'STUDENT',details:{skills:[],submitted:1}},history:[{id:'s',title:'CODE',subtitle:'DAT',status:'DA_NOP',details:{problemCode:'CODE'}}]});
    await f.whenStable();
    const el=f.nativeElement as HTMLElement;
    expect(el.querySelectorAll('.row-list li')).toHaveLength(1);
    expect(el.querySelector('.row-list strong')?.textContent).toBe('Hàm số: đơn điệu và cực trị');
    expect(el.querySelector('.row-list')?.textContent).toContain('Thứ Ba, 20/10 · 20:00 · 1/2 đã nộp');
    expect(el.querySelector('.distribution-legend')?.textContent).toContain('50%');
    http.expectNone('/api/gv/hoc-sinh/b?lopId=lop1');http.verify();
  });
});

describe('Kết quả chấm trong lịch sử bài nộp', () => {
  it('dùng details.result trước subtitle, dịch cả ba kết quả thay cho DA_NOP', async () => {
    const lop = { id: signal('lop1'), ds: signal([]), dangTai: signal(false), loi: signal(false), soChoDuyet: signal(0), tai: async () => {} };
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), { provide: LopDangChon, useValue: lop }] });
    const fixture = TestBed.createComponent(PhongGiaoVien);
    fixture.componentRef.setInput('trang', 'hoc-sinh');
    fixture.componentRef.setInput('id', 'hs1');
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/gv/ngan-hang?lopId=lop1').flush([]);
    http.expectOne('/api/gv/hoc-sinh/hs1?lopId=lop1').flush({
      student: { id: 'hs1', title: 'Học sinh thử', details: { skills: [] } },
      history: [
        { id: 'dat', title: 'Bài đúng', subtitle: 'SAI', status: 'DA_NOP', details: { result: 'DAT' } },
        { id: 'sai', title: 'Bài sai', subtitle: 'DAT', status: 'DA_NOP', details: { result: 'SAI' } },
        { id: 'unknown', title: 'Bài cần xem', subtitle: 'DAT', status: 'DA_NOP', details: { result: 'KHONG_KIEM_DUOC' } },
        { id: 'fallback', title: 'Bài cũ', subtitle: 'SAI', status: 'DA_NOP', details: {} },
        { id: 'empty', title: 'Chưa có kết quả', subtitle: 'KHONG_KIEM_DUOC', status: 'DA_NOP', details: { result: '' } },
      ],
    });
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    const section = [...el.querySelectorAll('section')].find(s => s.querySelector('h2')?.textContent === 'Bài đã nộp')!;
    expect([...section.querySelectorAll('.badge')].map(b => b.textContent?.trim())).toEqual(['Đạt', 'Sai', 'Không kiểm được', 'Sai', 'Không kiểm được']);
    http.verify();
  });
});

describe('Cảnh báo kẹt cho giáo viên', () => {
  it('cảnh báo có tên, kỹ năng và dẫn tới đúng học sinh', async () => {
    const lop={id:signal('lop1'),ds:signal([]),dangTai:signal(false),loi:signal(false),soChoDuyet:signal(0),tai:async()=>{}};
    TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([]),{provide:LopDangChon,useValue:lop}]});
    const f=TestBed.createComponent(PhongGiaoVien); f.detectChanges();
    const http=TestBed.inject(HttpTestingController);
    http.expectOne('/api/gv/canh-bao?lopId=lop1').flush([{id:'ket1',title:'Chi',subtitle:'Đang kẹt ở Xét dấu đạo hàm.',status:'KET',details:{studentId:'chi'}}]);
    http.expectOne('/api/gv/tong-quan?lopId=lop1').flush({students:1,stuck:1,pending:0,assignments:0,boBai:0,distribution:{},deadlines:[]});
    await f.whenStable();
    const alert=(f.nativeElement as HTMLElement).querySelector('[data-testid=canh-bao-ket]')!;
    expect(alert.textContent).toContain('Chi');
    expect(alert.textContent).toContain('Xét dấu đạo hàm');
    expect(alert.querySelector('a')?.getAttribute('href')).toBe('/gv/hoc-sinh/chi');
    http.verify();
  });
});

async function teacherPage(trang:'ngan-hang'|'ket-noi-ai'|'cai-dat'){
 const lop={id:signal('lop1'),ds:signal([{id:'lop1',title:'12A1'},{id:'lop2',title:'12A2'}]),dangTai:signal(false),loi:signal(false),soChoDuyet:signal(0),tai:async()=>{}};
 TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting(),provideRouter([]),{provide:LopDangChon,useValue:lop}]});
 const f=TestBed.createComponent(PhongGiaoVien);f.componentRef.setInput('trang',trang);f.detectChanges();
 const el=f.nativeElement as HTMLElement;
 for(const d of el.querySelectorAll<HTMLDialogElement>('dialog')){d.showModal=()=>{d.open=true;};d.close=()=>{d.open=false;};}
 const button=(text:string)=>[...el.querySelectorAll<HTMLButtonElement>('button')].find(b=>b.textContent?.trim()===text)!;
 return {f,el,lop,button,http:TestBed.inject(HttpTestingController)};
}
const catalogStudent={id:'hs1',title:'An',subtitle:'',status:'',details:{skills:[{code:'T12.NT.HS',name:'Hàm số',level4:'NHAN_BIET',level3:'Biết',mastery:0,overridden:false}],submitted:0}};
const catalogBank={id:'p1',title:'Câu hỏi',subtitle:'',status:'DA_PHAT_HANH',details:{skillCode:'T12.NT.HS',skillName:'Hàm số',level4:'NHAN_BIET',gate:'DAT',latex:'x'}};
describe('Nhập câu hỏi theo danh mục lớp',()=>{
 it('chọn kỹ năng có thật, không mã mặc định; gửi đúng mã và đổi lớp xóa lựa chọn',async()=>{
  const t=await teacherPage('ngan-hang');t.http.expectNone('/api/gv/tien-do?lopId=lop1');t.http.expectOne(bankPageUrl()).flush([catalogBank]);t.http.expectOne('/api/gv/ky-nang?lopId=lop1').flush([]);await t.f.whenStable();
  t.button('Nhập câu hỏi').click();await t.f.whenStable();
  const form=t.el.querySelector('app-hop-thoai[tieuDe="Nhập câu hỏi"] form') ?? [...t.el.querySelectorAll('form')].find(f=>f.textContent?.includes('Hàm số cần kiểm'))!;
  const select=form.querySelector<HTMLSelectElement>('select')!;expect(select.value).toBe('');expect([...select.options].map(o=>o.value)).toEqual(['','T12.NT.HS']);
  select.value='T12.NT.HS';select.dispatchEvent(new Event('input'));select.dispatchEvent(new Event('change'));
  await t.f.whenStable();
  for(const input of form.querySelectorAll<HTMLInputElement|HTMLTextAreaElement>('input,textarea')){input.value='x';input.dispatchEvent(new Event('input'));}await t.f.whenStable();
  expect(t.f.componentInstance['baiModel']().skillCode).toBe('T12.NT.HS');
  expect(t.f.componentInstance['baiForm']().invalid()).toBe(false);
  form.dispatchEvent(new Event('submit',{cancelable:true}));const r=t.http.expectOne(req=>req.method==='POST');expect(r.request.body.skillCode).toBe('T12.NT.HS');r.flush({message:'Đã nhập',count:1});
  await new Promise(r=>setTimeout(r,0));TestBed.tick();t.http.expectOne(bankPageUrl()).flush([catalogBank]);await t.f.whenStable();
  const cls=t.el.querySelector<HTMLSelectElement>('[aria-label="Chọn lớp"]')!;cls.value='lop2';cls.dispatchEvent(new Event('change'));TestBed.tick();t.http.expectNone('/api/gv/tien-do?lopId=lop2');t.http.expectOne(bankPageUrl('lop2')).flush([]);t.http.expectOne('/api/gv/ky-nang?lopId=lop2').flush([]);await t.f.whenStable();
  expect(select.value).toBe('');expect(select.options).toHaveLength(1);expect(form.textContent).toContain('Lớp chưa có danh mục kỹ năng');expect(form.querySelector<HTMLButtonElement>('[type=submit]')?.disabled).toBe(true);t.http.verify();
 });
 it('lớp mới chưa có học sinh hay bài vẫn lấy kỹ năng từ danh mục lớp, không cho nhập mã tùy ý',async()=>{
  const t=await teacherPage('ngan-hang');t.http.expectNone('/api/gv/tien-do?lopId=lop1');t.http.expectOne(bankPageUrl()).flush([]);t.http.expectOne('/api/gv/ky-nang?lopId=lop1').flush([{id:'T12.NT.HS',title:'Hàm số',subtitle:'',status:'KY_NANG',details:{}}]);await t.f.whenStable();
  const select=t.el.querySelector<HTMLSelectElement>('app-hop-thoai select[aria-label]') ?? [...t.el.querySelectorAll<HTMLSelectElement>('app-hop-thoai select')].find(s=>s.options[0]?.text==='Chọn kỹ năng')!;
  expect([...select.options].map(o=>o.value)).toEqual(['','T12.NT.HS']);t.http.verify();
 });
});
describe('Chữ về gia sư đúng hiện trạng',()=>{
 it.each(['cai-dat','ket-noi-ai'] as const)('%s giải thích mô hình và mở lời giải sau nộp',async trang=>{
  const t=await teacherPage(trang);t.http.expectOne('/api/gv/cai-dat?lopId=lop1').flush({lopId:'lop1',revealSolutionAfterSubmit:true,aiProvider:'glm'});
  if(trang==='ket-noi-ai'){t.http.expectOne('/api/gv/tien-do?lopId=lop1').flush([catalogStudent]);t.http.expectOne('/api/gv/lop/lop1/gia-su/su-dung').flush({classId:'lop1',requests:0,inputTokens:0,outputTokens:0,estimatedTokens:0,students:[]});}
  await t.f.whenStable();expect(t.el.textContent).toContain('Chỉ dẫn hệ thống yêu cầu gia sư');expect(t.el.textContent).toContain('xem lời giải sau khi nộp bài');expect(t.el.textContent).not.toContain('cổng kiểm công thức');expect(t.el.textContent).not.toContain('gợi ý có sẵn');t.http.verify();
 });
});

describe('Lưu trữ nội dung lớp',()=>{
 it('gửi khóa bộ giao thay vì danh sách mã sau xác nhận',async()=>{
  const t=await setup();
  for(const d of t.el.querySelectorAll<HTMLDialogElement>('dialog')){d.showModal=()=>{d.open=true;};d.close=()=>{d.open=false;};}
  t.fixture.componentInstance['ds'].reload();TestBed.tick();
  t.http.expectOne('/api/gv/giao?lopId=lop1').flush(['a1','a2'].map((id,i)=>({id,title:'Bộ hình học',subtitle:'An',status:'ON_TAP',details:{setKey:'bo1',studentId:'nam',problemCode:'KG'+i}})));await t.fixture.whenStable();
  const button=(name:string)=>[...t.el.querySelectorAll<HTMLButtonElement>('button')].find(b=>b.textContent?.trim()===name)!;
  button('Lưu trữ bộ bài').click();await t.fixture.whenStable();t.http.expectNone('/api/gv/giao/luu-tru');
  expect(t.el.querySelector('dialog[open]')?.textContent).toContain('Bộ hình học');
  button('Xác nhận').click();const req=t.http.expectOne('/api/gv/giao/luu-tru');expect(req.request.body).toEqual({lopId:'lop1',setKey:'bo1'});
  req.flush({message:'Đã lưu trữ',count:2});await new Promise(r=>setTimeout(r,0));TestBed.tick();
  t.http.expectOne('/api/gv/giao?lopId=lop1').flush([]);t.http.expectOne('/api/gv/tong-quan?lopId=lop1').flush({students:1,stuck:0,pending:0,assignments:0,boBai:0,distribution:{},deadlines:[]});await t.fixture.whenStable();t.http.verify();
 });
 it('ẩn tài liệu gửi đúng tài liệu và lớp sau xác nhận',async()=>{
  const t=await setup();t.fixture.componentRef.setInput('trang','tai-lieu');TestBed.tick();
  t.http.expectOne('/api/gv/kho?lopId=lop1').flush({formulas:[],documents:[{id:'doc1',title:'Hình học',subtitle:'Nội dung',status:'tu_soan',details:{}}]});await t.fixture.whenStable();
  for(const d of t.el.querySelectorAll<HTMLDialogElement>('dialog')){d.showModal=()=>{d.open=true;};d.close=()=>{d.open=false;};}
  const button=(name:string)=>[...t.el.querySelectorAll<HTMLButtonElement>('button')].find(b=>b.textContent?.trim()===name)!;
  button('Ẩn tài liệu').click();await t.fixture.whenStable();t.http.expectNone(r=>r.method==='DELETE');button('Xác nhận').click();
  const req=t.http.expectOne('/api/gv/tai-lieu/doc1?lopId=lop1');expect(req.request.method).toBe('DELETE');req.flush({message:'Đã ẩn',count:1});
  await new Promise(r=>setTimeout(r,0));TestBed.tick();t.http.expectOne('/api/gv/kho?lopId=lop1').flush({formulas:[],documents:[]});await t.fixture.whenStable();t.http.verify();
 });
});
