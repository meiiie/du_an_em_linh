import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { form, FormField, required } from '@angular/forms/signals';
import { NAP_MATHLIVE, OCongThuc } from './o-cong-thuc';

@Component({
  imports: [OCongThuc, FormField],
  template: `<app-o-cong-thuc [formField]="f.latex" nhan="Đạo hàm" testId="latex-dh" />`,
})
class Vo {
  readonly bai = signal({ latex: 'x^2' });
  readonly f = form(this.bai, (p) => {
    required(p.latex);
  });
}

type MathFieldGia = HTMLElement & { value: string; menuItems?: unknown[] };

describe('OCongThuc', () => {
  async function mo(nap: () => Promise<boolean>) {
    TestBed.configureTestingModule({ providers: [{ provide: NAP_MATHLIVE, useValue: nap }] });
    const fixture = TestBed.createComponent(Vo);
    await fixture.whenStable();
    await new Promise((r) => setTimeout(r, 0));
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    const tim = <T extends HTMLElement>(testId: string) => el.querySelector<T>(`[data-testid="${testId}"]`);
    return {
      fixture,
      el,
      oGo: () => tim<HTMLInputElement>('latex-dh')!,
      oToan: () => el.querySelector<MathFieldGia>('math-field')!,
      xemTruoc: () => tim('hieu-latex-dh'),
      on: () => fixture.whenStable(),
    };
  }

  it('nhãn thấy được; ô gõ bằng bàn phím có testId của v0 và tên chứa nhãn', async () => {
    const t = await mo(() => Promise.resolve(false));
    expect(t.el.querySelector('.nhan')?.textContent).toBe('Đạo hàm');
    expect(t.el.querySelector('label')).toBeNull();
    expect(t.oGo().getAttribute('aria-label')).toBe('Đạo hàm — gõ bằng bàn phím');
    expect(t.oGo().placeholder).toBe('Gõ bằng bàn phím, ví dụ 3x^2-12x+9');
  });

  it('MathLive không nạp được → ô math-field ẩn, có câu báo; ô gõ hai chiều với Signal Forms', async () => {
    const t = await mo(() => Promise.resolve(false));
    expect(t.oToan().hidden).toBe(true);
    expect(t.el.textContent).toContain('Chưa tải được ô gõ công thức trực quan');
    expect(t.oGo().value).toBe('x^2');

    t.oGo().value = '3x^2';
    t.oGo().dispatchEvent(new Event('input'));
    await t.on();
    expect(t.fixture.componentInstance.bai().latex).toBe('3x^2');

    t.fixture.componentInstance.bai.set({ latex: '6x' });
    await t.on();
    expect(t.oGo().value).toBe('6x');
  });

  it('có MathLive → math-field hiện (mf-<testId>), tắt menu; hai ô cùng một giá trị', async () => {
    const t = await mo(() => Promise.resolve(true));
    const o = t.oToan();
    expect(o.hidden).toBe(false);
    expect(o.dataset['testid']).toBe('mf-latex-dh');
    expect(o.getAttribute('aria-label')).toBe('Đạo hàm');
    expect(o.menuItems).toEqual([]);
    expect(o.value).toBe('x^2');
    expect(t.el.textContent).not.toContain('Chưa tải được');

    o.value = '3x^2';
    o.dispatchEvent(new Event('input'));
    await t.on();
    expect(t.fixture.componentInstance.bai().latex).toBe('3x^2');
    expect(t.oGo().value).toBe('3x^2');

    t.oGo().value = '3x^2-12x';
    t.oGo().dispatchEvent(new Event('input'));
    await t.on();
    expect(o.value).toBe('3x^2-12x');
  });

  it('MathLive chưa nạp xong → không gán value vào math-field (thuộc tính riêng sẽ che getter của MathLive)', async () => {
    const t = await mo(() => new Promise<boolean>(() => undefined));
    expect(Object.prototype.hasOwnProperty.call(t.oToan(), 'value')).toBe(false);
  });

  it('dòng «Máy hiểu là» vẽ giá trị bằng KaTeX; ô trống thì không có', async () => {
    const t = await mo(() => Promise.resolve(false));
    expect(t.xemTruoc()?.textContent).toContain('Máy hiểu là');
    expect(t.xemTruoc()?.querySelector('.katex')).not.toBeNull();

    t.fixture.componentInstance.bai.set({ latex: '  ' });
    await t.on();
    expect(t.xemTruoc()).toBeNull();
  });

  it('Signal Forms báo không hợp lệ → aria-invalid trên cả hai ô', async () => {
    const t = await mo(() => Promise.resolve(true));
    expect(t.oGo().getAttribute('aria-invalid')).toBeNull();

    t.fixture.componentInstance.bai.set({ latex: '' });
    await t.on();
    expect(t.oGo().getAttribute('aria-invalid')).toBe('true');
    expect(t.oToan().getAttribute('aria-invalid')).toBe('true');
  });
});
