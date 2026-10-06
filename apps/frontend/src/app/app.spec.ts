import { TestBed } from '@angular/core/testing';
import { Meta, Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { appConfig } from './app.config';
import { Phien } from './core/auth/phien';
import { AN, GV, PhienGia } from './core/auth/phien.testing';

describe('định tuyến và tiêu đề tab', () => {
  let gia: PhienGia;
  let harness: RouterTestingHarness | undefined;

  beforeEach(() => {
    gia = new PhienGia();
    harness = undefined;
    TestBed.configureTestingModule({ providers: [...appConfig.providers, { provide: Phien, useValue: gia }] });
  });

  /** Mỗi test chỉ được một harness: tạo lần đầu, các lần sau điều hướng tiếp trên harness đó. */
  async function den(url: string) {
    harness ??= await RouterTestingHarness.create();
    await harness.navigateByUrl(url);
    return { url: TestBed.inject(Router).url, el: harness.routeNativeElement };
  }

  it('/ là trang công khai: tab «Học toán theo từng bước · MathL+», lập chỉ mục được, một nút vao-hoc dẫn tới /dang-nhap', async () => {
    const { url, el } = await den('/');
    expect(url).toBe('/');
    expect(el?.querySelector('h1')).not.toBeNull();
    expect(TestBed.inject(Title).getTitle()).toBe('Học toán theo từng bước · MathL+');
    expect(TestBed.inject(Meta).getTag("name='robots'")).toBeNull();
    const vaoHoc = el?.querySelectorAll('[data-testid=vao-hoc]');
    expect(vaoHoc?.length).toBe(1);
    expect(vaoHoc?.[0].getAttribute('href')).toBe('/dang-nhap');
  });

  it('tab theo mẫu «<trang> · MathL+»', async () => {
    await den('/dang-nhap');
    expect(TestBed.inject(Title).getTitle()).toBe('Đăng nhập · MathL+');
  });

  it('trang đăng nhập không cho lập chỉ mục (như v0)', async () => {
    await den('/dang-nhap');
    expect(TestBed.inject(Meta).getTag("name='robots'")?.content).toBe('noindex, nofollow');
  });

  it('đường lạ chuyển về /dang-nhap', async () => {
    expect((await den('/khong-co')).url).toBe('/dang-nhap');
  });

  it('/hs khi chưa đăng nhập → /dang-nhap kèm returnUrl', async () => {
    expect((await den('/hs')).url).toBe('/dang-nhap?returnUrl=%2Fhs');
  });

  it('học sinh: /hs «Chào An», tab «Học»', async () => {
    gia.dangNhapNhu(AN);
    const { url, el } = await den('/hs');
    expect(url).toBe('/hs');
    expect(el?.querySelector('h1')?.textContent?.trim()).toBe('Chào An');
    expect(TestBed.inject(Title).getTitle()).toBe('Học · MathL+');
  });

  it('giáo viên: /gv «Chưa có lớp», tab «Lớp»; vào /hs bị đưa về /gv', async () => {
    gia.dangNhapNhu(GV);
    const gv = await den('/gv');
    expect(gv.el?.querySelector('h1')?.textContent?.trim()).toBe('Chưa có lớp');
    expect(TestBed.inject(Title).getTitle()).toBe('Lớp · MathL+');
    expect((await den('/hs')).url).toBe('/gv');
  });

  // Bảng phụ lục của specs/001-lat-cat-doc/spec.md: route, heading (h1) và tab của từng màn P2.
  it.each([
    ['/hs/bai', 'Đề bài', 'Đề bài'],
    ['/hs/luyen/B12-01', 'Luyện bài', 'Luyện'],
    ['/hs/lich', 'Lịch học', 'Lịch'],
    ['/hs/kho', 'Công thức và tài liệu', 'Công thức'],
  ])('học sinh: %s có heading «%s», tab «%s»', async (url, h1, tab) => {
    gia.dangNhapNhu(AN);
    const { url: duongDan, el } = await den(url);
    expect(duongDan).toBe(url);
    expect(el?.querySelector('main h1')?.textContent?.trim()).toBe(h1);
    expect(TestBed.inject(Title).getTitle()).toBe(`${tab} · MathL+`);
  });

  it.each([
    ['/gv/duyet', 'Duyệt', 'Duyệt'],
    ['/gv/ngan-hang', 'Đề bài', 'Đề bài'],
    ['/gv/tai-lieu', 'Tài liệu', 'Tài liệu'],
    ['/gv/cong-thuc', 'Công thức', 'Công thức'],
    ['/gv/tien-do', 'Mức lớp', 'Mức'],
    ['/gv/hoc-sinh/00000000-0000-4000-8000-000000000001', 'Học sinh', 'Học sinh'],
    ['/gv/cai-dat', 'Cài đặt lớp', 'Cài lớp'],
    ['/gv/ket-noi-ai', 'Gia sư', 'Gia sư'],
  ])('giáo viên: %s có heading «%s», tab «%s»', async (url, h1, tab) => {
    gia.dangNhapNhu(GV);
    const { url: duongDan, el } = await den(url);
    expect(duongDan).toBe(url);
    expect(el?.querySelector('main h1')?.textContent?.trim()).toBe(h1);
    expect(TestBed.inject(Title).getTitle()).toBe(`${tab} · MathL+`);
  });

  it('trang con cũng chặn sai vai trò: học sinh mở /gv/cai-dat → /hs', async () => {
    gia.dangNhapNhu(AN);
    expect((await den('/gv/cai-dat')).url).toBe('/hs');
  });

  it('đã có phiên (khôi phục từ cookie) mà mở /dang-nhap → về trang chủ', async () => {
    gia.khoiPhucDuoc = AN;
    expect((await den('/dang-nhap')).url).toBe('/hs');
  });
});
