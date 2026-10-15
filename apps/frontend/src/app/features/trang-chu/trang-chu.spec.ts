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

  it('từ gạch chân chạy đúng một vòng (đổi lần cuối ở 4 s) rồi dừng ở «hiểu ra»', async () => {
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
    const t = await mo();
    expect(t.tuDangHien()).toBe('hiểu ra');
    await t.buoc(1000);
    expect(t.tuDangHien()).toBe('làm được');
    await t.buoc(1000);
    expect(t.tuDangHien()).toBe('sửa sai');
    await t.buoc(2000);
    expect(t.tuDangHien()).toBe('hiểu ra');
    await t.buoc(1000 * 5);
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
    await t.buoc(1000 * 2);
    expect(t.tuDangHien()).toBe('hiểu ra');
  });

  it('giới thiệu đúng phạm vi THPT và nội dung do giáo viên duyệt', async () => {
    const t = await mo();
    const chu = t.el.textContent!;
    expect(chu).toContain('Toán THPT');
    expect(chu).toContain('Lớp 10–12');
    expect(chu).toContain('Bài luyện tập theo lớp và chủ đề');
    expect(chu).toContain('AI soạn nháp bài tập và lời giải từ tài liệu của thầy cô');
    expect(chu).toContain('Thầy cô duyệt trước khi giao cho học sinh');
    expect(chu).toContain('Theo dõi mức hiểu của từng học sinh');
    expect(chu).toContain('Xem mức dùng AI của lớp');
    expect(chu).toContain('Xem lời giải đầy đủ sau khi nộp');
    expect(chu).toContain('Nhắc lịch học qua thông báo trình duyệt');
    expect(chu).not.toMatch(/Một chủ đề|Công thức trong bài tập được kiểm|Hàng duyệt cho phần máy|MIT|Nghị định/);
    expect(t.el.querySelector('footer')?.textContent).toContain('© MathL+');
    expect(t.el.querySelector('footer a[href*="github.com"]')).toBeNull();
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
