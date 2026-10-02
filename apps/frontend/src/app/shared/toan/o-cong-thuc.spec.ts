import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { form, FormField } from '@angular/forms/signals';
import { NAP_MATHLIVE, OCongThuc } from './o-cong-thuc';

@Component({
  imports: [OCongThuc, FormField],
  template: `<app-o-cong-thuc [formField]="f.latex" nhan="Đạo hàm" testId="latex-dh" />`,
})
class Vo {
  readonly bai = signal({ latex: 'x^2' });
  readonly f = form(this.bai);
}

describe('OCongThuc', () => {
  async function mo(nap: () => Promise<boolean>) {
    TestBed.configureTestingModule({ providers: [{ provide: NAP_MATHLIVE, useValue: nap }] });
    const fixture = TestBed.createComponent(Vo);
    await fixture.whenStable();
    await new Promise((r) => setTimeout(r, 0));
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    return { fixture, el, o: () => el.querySelector<HTMLElement & { value: string }>('[data-testid="latex-dh"]')! };
  }

  it('MathLive không nạp được → ô LaTeX dự phòng, giá trị hai chiều với Signal Forms', async () => {
    const t = await mo(() => Promise.resolve(false));
    const o = t.o() as unknown as HTMLInputElement;
    expect(o.tagName).toBe('INPUT');
    expect(o.getAttribute('aria-label')).toBe('Đạo hàm');
    expect(o.value).toBe('x^2');
    expect(t.el.querySelector('.doi-cach')).toBeNull();

    o.value = '3x^2';
    o.dispatchEvent(new Event('input'));
    await t.fixture.whenStable();
    expect(t.fixture.componentInstance.bai().latex).toBe('3x^2');

    t.fixture.componentInstance.bai.set({ latex: '6x' });
    await t.fixture.whenStable();
    expect(o.value).toBe('6x');
  });

  it('có MathLive → <math-field> nhận giá trị; «Gõ LaTeX» chuyển sang ô chữ', async () => {
    const t = await mo(() => Promise.resolve(true));
    const o = t.o();
    expect(o.tagName).toBe('MATH-FIELD');
    expect(o.value).toBe('x^2');

    o.value = '3x^2';
    o.dispatchEvent(new Event('input'));
    await t.fixture.whenStable();
    expect(t.fixture.componentInstance.bai().latex).toBe('3x^2');

    t.el.querySelector<HTMLButtonElement>('.doi-cach')!.click();
    await t.fixture.whenStable();
    expect(t.o().tagName).toBe('INPUT');
    expect((t.o() as unknown as HTMLInputElement).value).toBe('3x^2');
  });
});
