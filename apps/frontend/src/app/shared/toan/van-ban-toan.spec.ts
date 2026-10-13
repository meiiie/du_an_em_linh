import { TestBed } from '@angular/core/testing';
import { VanBanToan } from './van-ban-toan';

describe('Phát biểu có toán trong dòng', () => {
  it('vẽ $...$ bằng KaTeX, giữ chữ và không chèn HTML từ nội dung', async () => {
    const f=TestBed.createComponent(VanBanToan);
    f.componentRef.setInput('text', 'Cho $x**12$ và $sin(x)$. <img src=x>');
    await f.whenStable();
    const el=f.nativeElement as HTMLElement;
    expect(el.querySelectorAll('.katex')).toHaveLength(2);
    expect(el.querySelectorAll('.katex-display')).toHaveLength(0);
    expect(el.querySelector('annotation')?.textContent).toBe('x^{12}');
    expect(el.textContent).toContain('Cho');
    expect(el.textContent).toContain('<img src=x>');
    expect(el.querySelector('img')).toBeNull();
  });
});
