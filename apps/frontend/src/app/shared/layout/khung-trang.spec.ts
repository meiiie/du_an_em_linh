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
  { path: '', component: NoiDung, title: 'Học' },
  { path: 'bai', component: NoiDung, title: 'Đề bài' },
  { path: 'duyet', component: NoiDung },
  { path: 'hong', loadComponent: () => Promise.reject(new Error('mất mạng')) },
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

  it('màn rộng: ray không phải hộp thoại; thương hiệu là chữ như v0; vùng điều hướng có nhãn', async () => {
    const t = await mo('/hs');
    expect(t.tim('sidebar')?.getAttribute('role')).toBeNull();
    expect(t.tim('sidebar')?.getAttribute('aria-label')).toBeNull();
    expect(t.el.querySelector('.ray-thuong-hieu')?.tagName).toBe('DIV');
    expect(t.tim('sidebar-nav')?.getAttribute('aria-label')).toBe('Menu học sinh');
    expect(document.documentElement.classList).not.toContain('co-thanh-tren');
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
    expect(t.tim('sidebar-nav')?.getAttribute('aria-label')).toBe('Menu giáo viên');
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

  it('chuyển trang → vùng aria-live đọc tiêu đề trang mới; lần nạp đầu không đọc', async () => {
    const t = await mo('/hs');
    expect(t.tim('thong-bao-trang')?.textContent).toBe('');
    await t.harness.navigateByUrl('/hs/bai');
    await t.on();
    expect(t.tim('thong-bao-trang')?.textContent).toBe('Đề bài');
  });

  it('trang con nạp lỗi (mất mạng) → báo, không im lặng', async () => {
    const t = await mo('/hs');
    await TestBed.inject(Router)
      .navigateByUrl('/hs/hong')
      .catch(() => undefined);
    await t.on();
    expect(t.el.querySelector('[role="alert"]')?.textContent).toContain('Chưa mở được trang');
    await t.harness.navigateByUrl('/hs/bai');
    await t.on();
    expect(t.el.querySelector('[role="alert"]')).toBeNull();
  });

  it('màn hẹp: ray là ngăn kéo — đóng thì inert; mở, đóng bằng nút, Esc hay chọn mục', async () => {
    manHinh(false);
    const t = await mo('/hs');
    const ray = t.tim('sidebar')!;
    const nutMo = t.tim('mo-sidebar')!;
    expect(ray.hasAttribute('inert')).toBe(true);
    expect(nutMo.getAttribute('aria-expanded')).toBe('false');
    expect(t.tatCa('dang-xuat')).toHaveLength(1);
    expect(document.documentElement.classList).toContain('co-thanh-tren');

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

  it('màn hẹp: ngăn kéo mở là hộp thoại — phần còn lại inert, khóa cuộn trang', async () => {
    manHinh(false);
    const t = await mo('/hs');
    const ray = t.tim('sidebar')!;
    const ngoai = () => [t.el.querySelector('.skip-link')!, t.el.querySelector('.thanh-tren')!, t.el.querySelector('.vung')!];
    expect(ray.getAttribute('role')).toBe('dialog');
    expect(ray.getAttribute('aria-label')).toBe('Menu');
    expect(ray.getAttribute('aria-modal')).toBeNull();
    expect(ngoai().every((e) => !e.hasAttribute('inert'))).toBe(true);

    t.tim('mo-sidebar')!.click();
    await t.on();
    expect(ray.getAttribute('aria-modal')).toBe('true');
    expect(ngoai().every((e) => e.hasAttribute('inert'))).toBe(true);
    expect(document.documentElement.classList).toContain('khoa-cuon');
    // Vùng aria-live nằm ngoài phần inert, nên vẫn đọc được khi chuyển trang từ ngăn kéo.
    expect(t.tim('thong-bao-trang')?.closest('[inert]')).toBeNull();

    t.tim('dong-sidebar')!.click();
    await t.on();
    expect(ngoai().every((e) => !e.hasAttribute('inert'))).toBe(true);
    expect(document.documentElement.classList).not.toContain('khoa-cuon');
  });

  it('màn hẹp: chọn mục → tiêu điểm vào nội dung; bấm đúng trang đang mở cũng đóng ngăn kéo', async () => {
    manHinh(false);
    const t = await mo('/hs');
    const ray = t.tim('sidebar')!;
    const noiDung = t.el.querySelector<HTMLElement>('#noi-dung')!;

    t.tim('mo-sidebar')!.click();
    await t.on();
    t.tim('nav-hs-bai')!.click();
    await t.on();
    expect(document.activeElement).toBe(noiDung);

    // Đang ở /hs/bai, bấm lại «Đề bài»: Router bỏ qua (không có NavigationEnd) nhưng ngăn kéo vẫn đóng.
    t.tim('mo-sidebar')!.click();
    await t.on();
    expect(ray.classList).toContain('mo');
    t.tim('nav-hs-bai')!.click();
    await t.on();
    expect(ray.classList).not.toContain('mo');
    expect(document.activeElement).toBe(noiDung);
  });
});
