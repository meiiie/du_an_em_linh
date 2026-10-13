import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { LoiTai } from './trang-thai';

describe('LoiTai', () => {
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
