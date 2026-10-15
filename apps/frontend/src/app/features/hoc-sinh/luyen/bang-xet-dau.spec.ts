import { TestBed } from '@angular/core/testing';
import { BangXetDau } from './bang-xet-dau';

describe('Bảng xét dấu chỉ đọc', () => {
  it.each(['readonly', 'disabled'])('khóa thêm, xóa, dấu và mũi tên khi %s', async (input) => {
    const f = TestBed.createComponent(BangXetDau);
    f.componentRef.setInput('moc', ['1']);
    f.componentRef.setInput(input, true);
    await f.whenStable();
    const el = f.nativeElement as HTMLElement;
    expect([...el.querySelectorAll('button')].every(button => button.disabled)).toBe(true);
    const o = el.querySelector('input')!;
    expect(o.disabled || o.readOnly).toBe(true);
    el.querySelector<HTMLButtonElement>('[data-testid="dau-0-+"]')!.click();
    el.querySelector<HTMLButtonElement>('.xoa')!.click();
    expect(f.componentInstance.moc()).toEqual(['1']);
    expect(f.componentInstance.dau()).toEqual({});
  });
});
