import { TestBed } from '@angular/core/testing';
import { DoiGiaoDien } from '../shared/ui/doi-giao-dien';
import { GiaoDien, KHOA_GIAO_DIEN } from './giao-dien';

/** matchMedia giả cho `prefers-color-scheme: dark`; trả hàm đổi chế độ của máy giữa chừng. */
function mayToi(toi: boolean): (moi: boolean) => void {
  const nghe = new Set<(e: { matches: boolean }) => void>();
  vi.stubGlobal('matchMedia', () => ({
    matches: toi,
    addEventListener: (_: string, f: (e: { matches: boolean }) => void) => nghe.add(f),
    removeEventListener: (_: string, f: (e: { matches: boolean }) => void) => nghe.delete(f),
  }));
  return (moi) => nghe.forEach((f) => f({ matches: moi }));
}

const theme = () => document.documentElement.getAttribute('data-theme');

describe('GiaoDien', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => {
    vi.unstubAllGlobals();
    document.documentElement.removeAttribute('data-theme');
  });

  function tao(): GiaoDien {
    const g = TestBed.inject(GiaoDien);
    TestBed.tick();
    return g;
  }

  it('chưa chọn: theo máy, và đổi theo khi máy đổi', () => {
    const doiMay = mayToi(true);
    const g = tao();
    expect(g.cheDo()).toBe('dark');
    expect(theme()).toBe('dark');

    doiMay(false);
    TestBed.tick();
    expect(theme()).toBe('light');
  });

  it('bấm đổi: nhớ lựa chọn, lựa chọn thắng máy cả khi máy đổi sau đó', () => {
    const doiMay = mayToi(false);
    const g = tao();
    g.doi();
    TestBed.tick();
    expect(theme()).toBe('dark');
    expect(localStorage.getItem(KHOA_GIAO_DIEN)).toBe('dark');

    doiMay(false);
    TestBed.tick();
    expect(theme()).toBe('dark');
  });

  it('lựa chọn đã nhớ thắng máy ngay từ đầu', () => {
    localStorage.setItem(KHOA_GIAO_DIEN, 'light');
    mayToi(true);
    tao();
    expect(theme()).toBe('light');
  });

  it('bộ nhớ bị chặn: vẫn đổi được, không vỡ', () => {
    mayToi(false);
    vi.stubGlobal('localStorage', {
      getItem: () => {
        throw new DOMException('chặn', 'SecurityError');
      },
      setItem: () => {
        throw new DOMException('chặn', 'SecurityError');
      },
    });
    const g = tao();
    expect(theme()).toBe('light');
    g.doi();
    TestBed.tick();
    expect(theme()).toBe('dark');
  });
});

describe('DoiGiaoDien', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => {
    vi.unstubAllGlobals();
    document.documentElement.removeAttribute('data-theme');
  });

  it('nút bật tắt có tên cố định; aria-pressed và hình theo chế độ', async () => {
    mayToi(false);
    const fixture = TestBed.createComponent(DoiGiaoDien);
    await fixture.whenStable();
    const nut = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('[data-testid="doi-giao-dien"]')!;
    const matTroi = () => nut.querySelector('circle[r="4"]') !== null;
    expect(nut.getAttribute('aria-label')).toBe('Giao diện tối');
    expect(nut.getAttribute('aria-pressed')).toBe('false');
    expect(matTroi()).toBe(true);

    nut.click();
    await fixture.whenStable();
    expect(nut.getAttribute('aria-pressed')).toBe('true');
    expect(nut.getAttribute('aria-label')).toBe('Giao diện tối');
    expect(matTroi()).toBe(false);
    expect(theme()).toBe('dark');
  });
});
