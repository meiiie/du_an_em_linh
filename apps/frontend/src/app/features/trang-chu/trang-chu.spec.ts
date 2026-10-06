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
      nutDung: () => [...el.querySelectorAll('button')].find((b) => /chuyển động/.test(b.textContent!))!,
    };
  }

  it('tiêu đề đọc một câu cố định; một nút vao-hoc dẫn tới /dang-nhap', async () => {
    const t = await mo();
    expect(chuDoc(t.el.querySelector('h1')!)).toBe('Học toán theo từng bước. Tự mình hiểu ra.');
    const vaoHoc = t.el.querySelectorAll('[data-testid=vao-hoc]');
    expect(vaoHoc.length).toBe(1);
    expect(vaoHoc[0].getAttribute('href')).toBe('/dang-nhap');
  });

  it('từ gạch chân đổi sau mỗi nhịp; tạm dừng thì đứng yên và dải bảng dừng', async () => {
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
    const t = await mo();
    expect(t.tuDangHien()).toBe('hiểu ra');
    vi.advanceTimersByTime(2800);
    await t.fixture.whenStable();
    expect(t.tuDangHien()).toBe('làm được');

    t.nutDung().click();
    await t.fixture.whenStable();
    expect(t.nutDung().textContent!.trim()).toBe('Tiếp tục chuyển động');
    expect(t.el.querySelector('app-dai-bang')!.classList).toContain('dung');
    vi.advanceTimersByTime(2800 * 3);
    await t.fixture.whenStable();
    expect(t.tuDangHien()).toBe('làm được');
  });

  it('máy đặt giảm chuyển động: từ đứng yên từ đầu, dải bảng dừng', async () => {
    vi.stubGlobal('matchMedia', (q: string) => ({
      matches: q.includes('prefers-reduced-motion'),
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
    }));
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
    const t = await mo();
    vi.advanceTimersByTime(2800 * 2);
    await t.fixture.whenStable();
    expect(t.tuDangHien()).toBe('hiểu ra');
    expect(t.el.querySelector('app-dai-bang')!.classList).toContain('dung');
  });
});
