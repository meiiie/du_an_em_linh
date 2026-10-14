import { KichThuocTrang } from '../../shared/ui/kich-thuoc-trang';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { QuanTri } from './quan-tri';

describe('Danh sách người dùng', () => {
  it('nhật ký hiện tên hành động và vai trò tiếng Việt thay cho mã', async () => {
    TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});
    const f=TestBed.createComponent(QuanTri);f.componentRef.setInput('trang','nhat-ky');f.detectChanges();
    TestBed.inject(HttpTestingController).expectOne('/api/qt/nhat-ky').flush([{id:'1',title:'TAO_TAI_KHOAN',subtitle:'STUDENT',status:'4ea60e1f-c26c-456c-b85f-ca2fedd7bf86',details:{createdAt:'2026-10-13T19:18:45Z'}}]);
    await f.whenStable();
    const row=(f.nativeElement as HTMLElement).querySelector('tbody tr')!.textContent!;
    expect(row).toContain('Tạo tài khoản');expect(row).toContain('Học sinh');expect(row).toContain('Mã đối tượng 4ea60e1f');
    expect(row).not.toContain('TAO_TAI_KHOAN');
  });

  it('màn hẹp hiện 10 người, đổi trang không mất lựa chọn và đổi chiều rộng đặt lại trang', async () => {
    TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});
    const size=TestBed.inject(KichThuocTrang);size.soMuc.set(10);
    const f=TestBed.createComponent(QuanTri);f.detectChanges();
    const http=TestBed.inject(HttpTestingController);
    http.expectOne('/api/qt/nguoi-dung').flush(Array.from({length:35},(_,i)=>({id:String(i),title:'Người '+i,subtitle:'user'+i+'@example.test',status:'STUDENT',details:{enabled:true}})));
    await f.whenStable();
    const el=f.nativeElement as HTMLElement;
    const next=()=>[...el.querySelectorAll<HTMLButtonElement>('button')].find(b=>b.textContent?.trim()==='Trang sau')!;
    expect(el.querySelectorAll('tbody tr')).toHaveLength(10);
    expect(el.querySelector('.pagination')?.textContent).toContain('Trang 1 / 4');
    (el.querySelector('[type=checkbox]') as HTMLInputElement).click();
    next().click();await f.whenStable();
    expect(el.querySelector('tbody tr')?.textContent).toContain('Người 10');
    size.soMuc.set(20);await f.whenStable();
    expect(el.querySelectorAll('tbody tr')).toHaveLength(20);
    expect(el.querySelector('.pagination')?.textContent).toContain('Trang 1 / 2');
    expect((el.querySelector('[type=checkbox]') as HTMLInputElement).checked).toBe(true);
    http.verify();
  });
  it('mỗi trang 20 người, lọc vai trò và tìm tên đặt lại trang, giữ lựa chọn', async () => {
    TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});
    const f=TestBed.createComponent(QuanTri); f.detectChanges();
    const http=TestBed.inject(HttpTestingController);
    http.expectOne('/api/qt/nguoi-dung').flush(Array.from({length:45},(_,i)=>({id:String(i),title:'Người '+i,subtitle:'user'+i+'@example.test',status:i===44?'TEACHER':'STUDENT',details:{enabled:true,synthetic:true}})));
    await f.whenStable();
    const el=f.nativeElement as HTMLElement;
    const rows=()=>el.querySelectorAll('tbody tr');
    const button=(name:string)=>[...el.querySelectorAll<HTMLButtonElement>('button')].find(b=>b.textContent?.trim()===name)!;
    expect(rows()).toHaveLength(20);
    (el.querySelector('[type=checkbox]') as HTMLInputElement).click();
    button('Trang sau').click(); await f.whenStable();
    expect(rows()[0].textContent).toContain('Người 20');
    button('Giáo viên').click(); await f.whenStable();
    expect(rows()).toHaveLength(1);
    expect(rows()[0].textContent).toContain('Người 44');
    button('Mọi vai trò').click(); await f.whenStable();
    expect((el.querySelector('[type=checkbox]') as HTMLInputElement).checked).toBe(true);
    button('Trang sau').click(); await f.whenStable();
    const search=el.querySelector('[type=search]') as HTMLInputElement;
    search.value='Người 44';search.dispatchEvent(new Event('input'));await f.whenStable();
    expect(rows()).toHaveLength(1);
    expect(rows()[0].textContent).toContain('Người 44');
    http.verify();
  });
});

async function userForm(password: string) {
  TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
  const fixture = TestBed.createComponent(QuanTri);
  fixture.detectChanges();
  const http = TestBed.inject(HttpTestingController);
  http.expectOne('/api/qt/nguoi-dung').flush([]);
  await fixture.whenStable();
  const el = fixture.nativeElement as HTMLElement;
  for (const dialog of el.querySelectorAll<HTMLDialogElement>('dialog')) {
    dialog.showModal = () => { dialog.open = true; };
    dialog.close = () => { dialog.open = false; };
  }
  [...el.querySelectorAll<HTMLButtonElement>('button')].find(b => b.textContent?.trim() === 'Thêm người dùng')!.click();
  await fixture.whenStable();
  const inputs = el.querySelectorAll<HTMLInputElement>('app-hop-thoai form input');
  for (const [index, value] of ['Người dùng thử', 'demo@example.test', password].entries()) {
    inputs[index].value = value;
    inputs[index].dispatchEvent(new Event('input'));
  }
  await fixture.whenStable();
  const form = inputs[0].closest('form')!;
  return { fixture, http, el, form, submit: () => form.dispatchEvent(new Event('submit', { cancelable: true })) };
}

describe('Tạo người dùng theo ràng buộc máy chủ', () => {
  it('mật khẩu 11 ký tự không thể gửi, hướng dẫn ghi tối thiểu 12', async () => {
    const t = await userForm('abcdefghijk');
    expect(t.form.textContent).toContain('ít nhất 12 ký tự');
    expect(t.form.querySelector<HTMLButtonElement>('button[type=submit]')!.disabled).toBe(true);
    t.submit();
    t.http.expectNone(r => r.method === 'POST');
    t.http.verify();
  });

  it('mật khẩu 12 ký tự được gửi và hiện detail tiếng Việt của máy chủ', async () => {
    const t = await userForm('abcdefghijkl');
    expect(t.form.querySelector<HTMLButtonElement>('button[type=submit]')!.disabled).toBe(false);
    t.submit();
    const request = t.http.expectOne(r => r.url === '/api/qt/nguoi-dung' && r.method === 'POST');
    expect(request.request.body.password).toBe('abcdefghijkl');
    request.flush({ detail: 'Email đã được sử dụng.' }, { status: 409, statusText: 'Conflict' });
    await t.fixture.whenStable();
    expect(t.form.querySelector('[role=alert]')?.textContent).toBe('Email đã được sử dụng.');
    t.http.verify();
  });

  it('lỗi không có detail vẫn hiện hướng dẫn thử lại', async () => {
    const t = await userForm('abcdefghijkl');
    t.submit();
    t.http.expectOne(r => r.method === 'POST').flush({}, { status: 500, statusText: 'Server Error' });
    await t.fixture.whenStable();
    expect(t.form.querySelector('[role=alert]')?.textContent).toBe('Chưa lưu được. Kiểm tra lựa chọn và thử lại.');
    t.http.verify();
  });
});

async function adminPage(trang:'lop'|'nguoi-dung', rows:unknown[]){
 TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});
 const f=TestBed.createComponent(QuanTri);f.componentRef.setInput('trang',trang);f.detectChanges();const http=TestBed.inject(HttpTestingController);http.expectOne('/api/qt/'+trang).flush(rows);await f.whenStable();const el=f.nativeElement as HTMLElement;
 for(const d of el.querySelectorAll<HTMLDialogElement>('dialog')){d.showModal=()=>{d.open=true;};d.close=()=>{d.open=false;};}
 const button=(name:string)=>[...el.querySelectorAll<HTMLButtonElement>('button')].find(b=>b.textContent?.trim()===name)!;
 return {f,http,el,button};
}
const cls=(status='MO')=>({id:'lop1',title:'12A1',subtitle:'2026-2027',status,details:{enabled:status==='MO'}});
describe('Khóa lớp và xác nhận thao tác quản trị',()=>{
 it.each([true,false])('khóa=%s chỉ gửi PUT sau xác nhận và tải lại trạng thái',async locked=>{
  const t=await adminPage('lop',[cls(locked?'MO':'KHOA')]);t.button(locked?'Khóa lớp':'Mở khóa lớp').click();await t.f.whenStable();
  expect(t.el.querySelector('dialog[open]')?.textContent).toContain('lớp 12A1?');t.http.expectNone(r=>r.method==='PUT');
  t.button('Hủy').click();await t.f.whenStable();t.http.expectNone(r=>r.method==='PUT');
  t.button(locked?'Khóa lớp':'Mở khóa lớp').click();await t.f.whenStable();t.button('Xác nhận').click();
  const r=t.http.expectOne('/api/qt/lop/lop1/khoa');expect(r.request.method).toBe('PUT');expect(r.request.body).toEqual({locked});
  // Nhấn hai lần không gửi thêm.
  t.f.componentInstance['thucHien']();t.http.expectNone(r=>r.method==='PUT');
  r.flush({message:'Đã lưu',count:1});await new Promise(r=>setTimeout(r,0));TestBed.tick();t.http.expectOne('/api/qt/lop').flush([cls(locked?'KHOA':'MO')]);await t.f.whenStable();
  expect(t.el.querySelector('dialog[open]')).toBeNull();expect(t.el.querySelector('tbody')?.textContent).toContain(locked?'Đã khóa':'Đang mở');t.http.verify();
 });
 it('lỗi khóa giữ hộp thoại và cho thử lại, không báo thành công',async()=>{
  const t=await adminPage('lop',[cls()]);t.button('Khóa lớp').click();await t.f.whenStable();t.button('Xác nhận').click();t.http.expectOne('/api/qt/lop/lop1/khoa').flush({detail:'Không có quyền quản lý lớp.'},{status:403,statusText:'Forbidden'});await t.f.whenStable();
  expect(t.el.querySelector('dialog[open] [role=alert]')?.textContent).toBe('Không có quyền quản lý lớp.');expect(t.button('Xác nhận').disabled).toBe(false);t.http.expectNone('/api/qt/lop');t.http.verify();
 });
 it.each([true,false])('bulk enabled=%s cần xác nhận; hủy giữ lựa chọn và không gửi',async enabled=>{
  const t=await adminPage('nguoi-dung',[{id:'hs1',title:'An',subtitle:'',status:'STUDENT',details:{enabled:true}}]);t.el.querySelector<HTMLInputElement>('[type=checkbox]')!.click();await t.f.whenStable();
  const action=enabled?'Mở tài khoản':'Khóa tài khoản';t.button(action).click();await t.f.whenStable();t.http.expectNone(r=>r.method==='POST');
  t.button('Hủy').click();await t.f.whenStable();expect(t.el.querySelector<HTMLInputElement>('[type=checkbox]')!.checked).toBe(true);
  t.button(action).click();await t.f.whenStable();t.button('Xác nhận').click();const r=t.http.expectOne('/api/qt/nguoi-dung/bulk');expect(r.request.body).toEqual({ids:['hs1'],enabled});r.flush({message:'Đã lưu',count:1});await new Promise(r=>setTimeout(r,0));TestBed.tick();t.http.expectOne('/api/qt/nguoi-dung').flush([]);await t.f.whenStable();expect(t.el.querySelector('dialog[open]')).toBeNull();t.http.verify();
 });
 it('báo cáo AI chỉ tải khi chọn lớp, đúng endpoint quản trị và tên học sinh',async()=>{
  const t=await adminPage('lop',[cls()]);t.http.expectNone(r=>r.url.includes('su-dung'));t.button('Sử dụng AI').click();TestBed.tick();
  t.http.expectOne('/api/qt/nguoi-dung').flush([{id:'hs1',title:'An',subtitle:'',status:'STUDENT',details:{enabled:true}}]);
  t.http.expectOne('/api/qt/lop/lop1/gia-su/su-dung').flush({classId:'lop1',requests:1,inputTokens:10,outputTokens:20,estimatedTokens:0,students:[{studentId:'hs1',requests:1,inputTokens:10,outputTokens:20,estimatedTokens:0}]});await t.f.whenStable();
  expect(t.el.querySelector('app-su-dung-gia-su tbody th')?.textContent).toBe('An');t.http.verify();
 });
});
