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
  it('giữ văn xuôi là text, nhận đủ dấu toán trong dòng và khối', async () => {
    const f=TestBed.createComponent(VanBanToan);
    f.componentRef.setInput('text', String.raw`Cho hàm số \(x^2\). Tính \[x+1\] và $y$. Giá \\$5; <script>không chạy</script>`);
    await f.whenStable();
    const el=f.nativeElement as HTMLElement;
    expect(el.querySelectorAll('.katex')).toHaveLength(3);
    expect(el.querySelectorAll('.katex-display')).toHaveLength(1);
    expect([...el.querySelectorAll('annotation')].map(n=>n.textContent)).toEqual(['x^2','x+1','y']);
    expect(el.textContent).toContain('Cho hàm số');
    expect(el.querySelector('script')).toBeNull();
  });

});
