import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { TrangChu } from './trang-chu';

/** Chữ mà trình đọc màn hình nghe: bỏ phần `aria-hidden`. */
function chuDoc(el: Element): string {
  const ban = el.cloneNode(true) as Element;
  ban.querySelectorAll('[aria-hidden=true]').forEach((n) => n.remove());
  return ban.textContent!.replace(/\s+/g, ' ').trim();
}

describe('TrangChu', () => {
  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  async function mo() {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(TrangChu);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    return {
      fixture,
      el,
      tuDangHien: () => el.querySelector('.xoay .hien .gach')!.textContent!.trim(),
      buoc: async (ms: number) => {
        vi.advanceTimersByTime(ms);
        await fixture.whenStable();
      },
    };
  }

  it('tiêu đề đọc một câu cố định; một nút vao-hoc dẫn tới /dang-nhap; không nút tạm dừng', async () => {
    const t = await mo();
    expect(chuDoc(t.el.querySelector('h1')!)).toBe('Học toán theo từng bước. Tự mình hiểu ra.');
    const vaoHoc = t.el.querySelectorAll('[data-testid=vao-hoc]');
    expect(vaoHoc.length).toBe(1);
    expect(vaoHoc[0].getAttribute('href')).toBe('/dang-nhap');
    expect([...t.el.querySelectorAll('button')].some((b) => /chuyển động/.test(b.textContent!))).toBe(false);
  });

  it('từ gạch chân chạy đúng một vòng (4,8 s) rồi dừng ở «hiểu ra»', async () => {
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
    const t = await mo();
    expect(t.tuDangHien()).toBe('hiểu ra');
    await t.buoc(1200);
    expect(t.tuDangHien()).toBe('làm được');
    await t.buoc(1200);
    expect(t.tuDangHien()).toBe('sửa sai');
    await t.buoc(2400);
    expect(t.tuDangHien()).toBe('hiểu ra');
    await t.buoc(1200 * 5);
    expect(t.tuDangHien()).toBe('hiểu ra');
  });

  it('máy đặt giảm chuyển động: từ đứng yên từ đầu', async () => {
    vi.stubGlobal('matchMedia', (q: string) => ({
      matches: q.includes('prefers-reduced-motion'),
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
    }));
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
    const t = await mo();
    await t.buoc(1200 * 2);
    expect(t.tuDangHien()).toBe('hiểu ra');
  });

  it('ảnh minh họa có chữ thay thế, kích thước khai sẵn (không xô bố cục) và tải lười', async () => {
    const t = await mo();
    const anh = [...t.el.querySelectorAll<HTMLImageElement>('img.minh-hoa')];
    expect(anh.map((a) => a.getAttribute('src'))).toEqual(['/anh/hoc-sinh-800.webp', '/anh/giao-vien-800.webp']);
    for (const a of anh) {
      expect(a.alt.startsWith('Minh họa:')).toBe(true);
      expect([a.getAttribute('width'), a.getAttribute('height'), a.getAttribute('loading')]).toEqual(['1200', '900', 'lazy']);
    }
  });
});
