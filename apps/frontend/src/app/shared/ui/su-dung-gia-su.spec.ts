import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SuDungGiaSu } from './su-dung-gia-su';

const report={classId:'lop1',requests:3,inputTokens:1200,outputTokens:300,estimatedTokens:400,students:[{studentId:'hs1',requests:3,inputTokens:1200,outputTokens:300,estimatedTokens:400}]};
async function setup(vai:'gv'|'qt'='gv') {
 TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});
 const f=TestBed.createComponent(SuDungGiaSu);f.componentRef.setInput('lopId','lop1');f.componentRef.setInput('vai',vai);f.componentRef.setInput('hocSinh',[{id:'hs1',title:'An'}]);f.detectChanges();
 return {f,http:TestBed.inject(HttpTestingController),el:f.nativeElement as HTMLElement};
}
describe('Báo cáo sử dụng gia sư AI',()=>{
 it.each(['gv','qt'] as const)('gọi API %s theo lớp và hiển thị tổng, từng em, giới hạn chưa có dữ liệu',async vai=>{
  const t=await setup(vai);expect(t.el.querySelector('[role=status]')?.textContent).toContain('Đang tải');
  t.http.expectOne(`/api/${vai}/lop/lop1/gia-su/su-dung`).flush(report);await t.f.whenStable();
  expect(t.el.querySelector('[data-testid=tong-su-dung-ai]')?.textContent).toMatch(/3 lượt gọi · 1[.\s]500 token/);
  const row=t.el.querySelector('tbody tr')!;expect(row.textContent).toContain('An');expect(row.textContent).toContain('Chưa có dữ liệu');
  expect(t.el.textContent).toContain('Token ước tính đã nằm trong tổng');
  expect(t.el.querySelector('caption')?.textContent).toBe('Sử dụng gia sư theo học sinh');
  t.http.verify();
 });
 it('rỗng có tổng bằng 0 và lời giải thích, không hiện bảng giả',async()=>{
  const t=await setup();t.http.expectOne('/api/gv/lop/lop1/gia-su/su-dung').flush({...report,requests:0,inputTokens:0,outputTokens:0,estimatedTokens:0,students:[]});await t.f.whenStable();
  expect(t.el.textContent).toContain('Chưa có lượt sử dụng AI');expect(t.el.querySelector('table')).toBeNull();t.http.verify();
 });
 it('lỗi có Thử lại; đổi lớp hủy yêu cầu cũ và không giữ số liệu lớp cũ',async()=>{
  const t=await setup();t.http.expectOne('/api/gv/lop/lop1/gia-su/su-dung').flush({}, {status:500,statusText:'Error'});await t.f.whenStable();
  expect(t.el.querySelector('[role=alert]')).not.toBeNull();
  t.el.querySelector<HTMLButtonElement>('[role=alert] button')!.click();TestBed.tick();
  const old=t.http.expectOne('/api/gv/lop/lop1/gia-su/su-dung');t.f.componentRef.setInput('lopId','lop2');TestBed.tick();
  expect(old.cancelled).toBe(true);t.http.expectOne('/api/gv/lop/lop2/gia-su/su-dung').flush({...report,classId:'lop2',requests:7});await t.f.whenStable();
  expect(t.el.querySelector('[data-testid=tong-su-dung-ai]')?.textContent).toContain('7 lượt gọi');t.http.verify();
 });
 it('thiếu tên vẫn hiển thị định danh; cập nhật lấy số liệu mới',async()=>{
  const t=await setup();t.f.componentRef.setInput('hocSinh',[]);t.http.expectOne('/api/gv/lop/lop1/gia-su/su-dung').flush(report);await t.f.whenStable();
  expect(t.el.querySelector('tbody th')?.textContent).toBe('Học sinh hs1');
  t.el.querySelector<HTMLButtonElement>('button')!.click();TestBed.tick();t.http.expectOne('/api/gv/lop/lop1/gia-su/su-dung').flush(report);await t.f.whenStable();t.http.verify();
 });
});
