import loiGiaiDayDu from '../../../../../../services/core/src/main/resources/seed-nguyen-trai/loi-giai-day-du.json';
import taiLieu from '../../../../../../data/dev/nguyen-trai/tai-lieu.json';
import lichSu from '../../../../../../data/dev/nguyen-trai/bai-lam-lich-su.json';
import taiLieuV0 from '../../../../../../data/v0/tai-lieu.json';
import viDu from '../../../../../../data/supham/03-vi-du-bai-tap.json';
import khungNgan from '../../../../../../data/supham/bai-khung-ngan.seed-v01.json';
import taiLieuSuPham1 from '../../../../../../data/supham/tai-lieu/sp-tai-lieu-0001.json';
import taiLieuSuPham2 from '../../../../../../data/supham/tai-lieu/sp-tai-lieu-0002.json';
import baiTap from '../../../../../../data/dev/nguyen-trai/bai-tap.json';
import congThuc from '../../../../../../data/dev/nguyen-trai/bang-chuong.json';
import daoHam from '../../../../../../data/dev/nguyen-trai/bang-dao-ham.json';
import congThucV0 from '../../../../../../data/v0/bang-cong-thuc.json';
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


describe('LaTeX chưa có dấu phân cách', () => {
  it.each([
    String.raw`\text{Chóp đáy tam giác vuông, hai cạnh }6,8\text{ cm; cao }9\text{ cm}`,
    String.raw`y'\ge 0,\ y'=0\text{ chỉ tại hữu hạn điểm}\Rightarrow\text{đồng biến}`,
    String.raw`y'=0\text{ hoặc }y'\text{ không xác định}`,
    'S_n=n(2u_1+(n−1)d)/2',
  ])('vẽ công thức nguyên vẹn: %s', async (text) => {
    const f = TestBed.createComponent(VanBanToan);
    f.componentRef.setInput('text', text);
    await f.whenStable();
    const el = f.nativeElement as HTMLElement;
    expect(el.querySelector('.katex')).not.toBeNull();
    expect(el.querySelector('.katex-error')).toBeNull();
    el.querySelectorAll('annotation').forEach(n => n.remove());
    expect(el.textContent).not.toMatch(/\\(?:text|d?frac)|\$/);
  });

  it('giữ văn xuôi, chỉ vẽ công thức trong câu chưa có dấu phân cách', async () => {
    const f = TestBed.createComponent(VanBanToan);
    f.componentRef.setInput('text', String.raw`Tổng là S_n=\frac{n(2u_1+(n-1)d)}2. Xét tiếp S_n=u_1(q^n−1)/(q−1).`);
    await f.whenStable();
    const el = f.nativeElement as HTMLElement;
    expect(el.querySelectorAll('.katex')).toHaveLength(2);
    expect(el.querySelector('.katex-error')).toBeNull();
    expect(el.textContent).toContain('Tổng là');
    expect(el.textContent).toContain('Xét tiếp');
  });
});


describe('Toàn bộ nội dung toán của seed', () => {
  function chuoi(value: unknown, path = ''): { path: string; text: string }[] {
    if (!value || typeof value !== 'object') return [];
    return Object.entries(value).flatMap(([key, v]) => {
      const next = `${path}.${key}`;
      if (typeof v === 'string' && /^(latex|van_ban|statement|solution|noiDung|cong_thuc|textContent|text)$/.test(key)) return [{ path: next, text: v }];
      return chuoi(v, next);
    });
  }
  it('vẽ phát biểu, LaTeX, công thức và từng bước lời giải không lộ mã nguồn', async () => {
    const f = TestBed.createComponent(VanBanToan);
    const fields = chuoi({ loiGiaiDayDu, baiTap, congThuc, daoHam, congThucV0, taiLieu, lichSu, taiLieuV0, viDu, khungNgan, taiLieuSuPham1, taiLieuSuPham2 });
    expect(fields.length).toBeGreaterThan(500);
    // Nhiều seed lặp lại cùng phát biểu ở bài tập, lịch sử và lời giải.
    // Quét mọi đường dẫn, nhưng chỉ dựng DOM một lần cho mỗi nội dung.
    const byText = new Map<string, string[]>();
    for (const { path, text } of fields) {
      const paths = byText.get(text) ?? [];
      paths.push(path);
      byText.set(text, paths);
    }
    const errors: string[] = [];
    for (const [text, paths] of byText) {
      const path = paths.join(', ');
      f.componentRef.setInput('text', text);
      await f.whenStable();
      const el = (f.nativeElement as HTMLElement).cloneNode(true) as HTMLElement;
      if (el.querySelector('.katex-error')) errors.push(path + ': lỗi KaTeX: ' + text);
      // MathML giữ nguồn cho công cụ hỗ trợ; chỉ quét chữ có thể thấy trên màn hình.
      el.querySelectorAll('.katex-mathml').forEach(n => n.remove());
      if (/\\(?:text|d?frac)\b|\$/.test(el.textContent ?? '')) errors.push(path + ': còn mã nguồn: ' + text);
    }
    expect(errors).toEqual([]);
  });
});


it('giữ văn xuôi xen lệnh text có khoảng trắng và ngoặc lồng nhau', async () => {
  const f = TestBed.createComponent(VanBanToan);
  f.componentRef.setInput('text', String.raw`Cho \text{hình {chóp}} với cạnh \frac{2 + 1}{3}. Tính thể tích.`);
  await f.whenStable();
  const el = f.nativeElement as HTMLElement;
  expect(el.querySelectorAll('.katex')).toHaveLength(2);
  expect(el.querySelector('.katex-error')).toBeNull();
  const visible = el.cloneNode(true) as HTMLElement;
  visible.querySelectorAll('.katex-mathml').forEach(n => n.remove());
  expect(visible.textContent).toContain('Cho');
  expect(visible.textContent).not.toMatch(/\\(?:text|frac)|[{}]/);
});
