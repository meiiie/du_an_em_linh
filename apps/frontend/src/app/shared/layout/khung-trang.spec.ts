import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, withComponentInputBinding } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { Phien } from '../../core/auth/phien';
import { AN, GV, PhienGia } from '../../core/auth/phien.testing';
import { KhungTrang } from './khung-trang';

@Component({ template: `<h1>Nội dung</h1>` })
class NoiDung {}

const con = [
  { path: '', component: NoiDung },
  { path: 'bai', component: NoiDung },
  { path: 'duyet', component: NoiDung },
];

/** matchMedia giả: jsdom không có; `rong = false` là điện thoại / máy tính bảng (dưới `lg`). */
function manHinh(rong: boolean) {
  vi.stubGlobal('matchMedia', () => ({ matches: rong, addEventListener: () => undefined, removeEventListener: () => undefined }));
}

describe('KhungTrang', () => {
  afterEach(() => vi.unstubAllGlobals());

  async function mo(url: string, nguoiDung = AN) {
    const gia = new PhienGia().dangNhapNhu(nguoiDung);
    TestBed.configureTestingModule({
      providers: [
        provideRouter(
          [
            { path: 'hs', component: KhungTrang, data: { khuVuc: 'HS' }, children: con },
            { path: 'gv', component: KhungTrang, data: { khuVuc: 'GV' }, children: con },
            { path: 'dang-nhap', component: NoiDung },
          ],
          withComponentInputBinding(),
        ),
        { provide: Phien, useValue: gia },
      ],
    });
    const harness = await RouterTestingHarness.create(url);
    const el = harness.fixture.nativeElement as HTMLElement;
    const tim = (testId: string) => el.querySelector<HTMLElement>(`[data-testid="${testId}"]`);
    const tatCa = (testId: string) => el.querySelectorAll(`[data-testid="${testId}"]`);
    return { gia, harness, el, tim, tatCa, on: () => harness.fixture.whenStable() };
  }

  it('học sinh: ray bốn mục của v0, tên người dùng, nội dung trong <main>', async () => {
    const t = await mo('/hs');
    const muc = [...t.el.querySelectorAll<HTMLAnchorElement>('[data-testid="sidebar-nav"] a')];
    expect(muc.map((a) => a.dataset['testid'])).toEqual(['nav-hs-lo-trinh', 'nav-hs-bai', 'nav-hs-lich', 'nav-hs-kho']);
    expect(muc.map((a) => a.textContent?.trim())).toEqual(['Học', 'Đề bài', 'Lịch', 'Công thức']);
    expect(t.tim('ten-nguoi-dung')?.textContent).toBe('An');
    expect(t.el.querySelector('main h1')?.textContent).toBe('Nội dung');
    expect(t.tim('sidebar')?.textContent).toContain('Học sinh');
    expect(t.tim('mo-sidebar')).toBeNull();
  });

  it('giáo viên: tám mục, chưa có «Tạo đề» (P3); mục đang mở có aria-current', async () => {
    const t = await mo('/gv/duyet', GV);
    const muc = [...t.el.querySelectorAll<HTMLAnchorElement>('[data-testid="sidebar-nav"] a')];
    expect(muc.map((a) => a.dataset['testid'])).toEqual([
      'nav-gv-tong-quan',
      'nav-gv-duyet',
      'nav-gv-ngan-hang',
      'nav-gv-tai-lieu',
      'nav-gv-cong-thuc',
      'nav-gv-tien-do',
      'nav-gv-ket-noi-ai',
      'nav-gv-cai-dat',
    ]);
    expect(t.tim('nav-gv-duyet')?.getAttribute('aria-current')).toBe('page');
    // «Lớp» là trang chủ khu vực: chỉ sáng khi đứng đúng /gv, không sáng ở trang con
    expect(t.tim('nav-gv-tong-quan')?.getAttribute('aria-current')).toBeNull();
  });

  it('màn rộng: một nút Đăng xuất ở chân ray; bấm → về /dang-nhap', async () => {
    const t = await mo('/hs');
    expect(t.tatCa('dang-xuat')).toHaveLength(1);
    t.tim('dang-xuat')!.click();
    await t.on();
    expect(t.gia.daGoiDangXuat).toBe(1);
    expect(TestBed.inject(Router).url).toBe('/dang-nhap');
  });

  it('đăng xuất lỗi mạng → báo lỗi, ở lại trang', async () => {
    const t = await mo('/hs');
    t.gia.loiDangXuat = new HttpErrorResponse({ status: 0 });
    t.tim('dang-xuat')!.click();
    await t.on();
    expect(t.el.querySelector('[role="alert"]')?.textContent).toContain('Chưa đăng xuất được');
    expect(TestBed.inject(Router).url).toBe('/hs');
  });

  it('màn hẹp: ray là ngăn kéo — đóng thì inert; mở, đóng bằng nút, Esc hay chọn mục', async () => {
    manHinh(false);
    const t = await mo('/hs');
    const ray = t.tim('sidebar')!;
    const nutMo = t.tim('mo-sidebar')!;
    expect(ray.hasAttribute('inert')).toBe(true);
    expect(nutMo.getAttribute('aria-expanded')).toBe('false');
    expect(t.tatCa('dang-xuat')).toHaveLength(1);

    nutMo.click();
    await t.on();
    expect(ray.hasAttribute('inert')).toBe(false);
    expect(ray.classList).toContain('mo');
    expect(nutMo.getAttribute('aria-expanded')).toBe('true');
    t.tim('dong-sidebar')!.click();
    await t.on();
    expect(ray.hasAttribute('inert')).toBe(true);

    nutMo.click();
    await t.on();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    await t.on();
    expect(ray.classList).not.toContain('mo');

    nutMo.click();
    await t.on();
    t.tim('nav-hs-bai')!.click();
    await t.on();
    expect(TestBed.inject(Router).url).toBe('/hs/bai');
    expect(ray.classList).not.toContain('mo');
  });
});
