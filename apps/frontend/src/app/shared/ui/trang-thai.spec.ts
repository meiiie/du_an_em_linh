import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { KhungCho, LoiTai } from './trang-thai';

describe('LoiTai', () => {
  it('lỗi tải có thông báo và nút thử lại trong cùng vùng cảnh báo', async () => {
    const fixture = TestBed.createComponent(LoiTai);
    fixture.componentRef.setInput('loi', new HttpErrorResponse({ status: 500 }));
    await fixture.whenStable();
    const alert = fixture.nativeElement.querySelector('[role="alert"]');
    expect(alert.textContent).toContain('Chưa tải được');
    expect(alert.textContent).toContain('Kiểm tra kết nối rồi thử lại.');
    expect(alert.querySelector('button').textContent).toBe('Thử lại');
  });
  it('API chưa có: giải thích trạng thái, vẫn cho thử lại khi dịch vụ sẵn sàng', async () => {
    const fixture = TestBed.createComponent(LoiTai);
    fixture.componentRef.setInput('loi', new HttpErrorResponse({ status: 501 }));
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Tính năng chưa sẵn sàng');
    const retry = vi.fn();
    fixture.componentInstance.thuLai.subscribe(retry);
    fixture.nativeElement.querySelector('button').click();
    expect(retry).toHaveBeenCalledOnce();
  });
});


describe('Khung chờ',()=>{
 it('xung chỉ chạy 3 lượt (4,2 giây), giữ thông báo đang tải sau khi dừng',async()=>{
  const f=TestBed.createComponent(KhungCho);f.componentRef.setInput('nhan','Đang tải bài');await f.whenStable();
  const el=f.nativeElement as HTMLElement;
  expect(getComputedStyle(el.querySelector('i')!).animation).toMatch(/cho-mo 1\.4s ease-in-out 3 alternate$/);
  expect(el.querySelector('[role=status]')?.getAttribute('aria-busy')).toBe('true');
  expect(el.querySelector('[role=status]')?.textContent).toContain('Đang tải bài');
 });
});
