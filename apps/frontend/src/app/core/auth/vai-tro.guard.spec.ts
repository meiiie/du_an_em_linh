import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, CanActivateFn, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Phien } from './phien';
import { AN, GV, PhienGia } from './phien.testing';
import { chiVaiTro, chuaDangNhap } from './vai-tro.guard';

describe('guard vai trò', () => {
  let gia: PhienGia;

  beforeEach(() => {
    gia = new PhienGia();
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: Phien, useValue: gia }] });
  });

  async function chay(guard: CanActivateFn, url: string): Promise<string | true> {
    const kq = await TestBed.runInInjectionContext(() => guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot));
    return kq instanceof UrlTree ? TestBed.inject(Router).serializeUrl(kq) : (kq as true);
  }

  it('chưa đăng nhập, không khôi phục được → /dang-nhap kèm returnUrl', async () => {
    expect(await chay(chiVaiTro('STUDENT'), '/hs')).toBe('/dang-nhap?returnUrl=%2Fhs');
    expect(gia.soLanLamMoi).toBe(1);
  });

  it('tải lại trang: khôi phục được phiên từ cookie → cho vào', async () => {
    gia.khoiPhucDuoc = AN;
    expect(await chay(chiVaiTro('STUDENT'), '/hs')).toBe(true);
  });

  it('đã đăng nhập thì không gọi làm mới', async () => {
    gia.dangNhapNhu(AN);
    expect(await chay(chiVaiTro('STUDENT'), '/hs')).toBe(true);
    expect(gia.soLanLamMoi).toBe(0);
  });

  it('sai vai trò → trang chủ của vai trò mình', async () => {
    gia.dangNhapNhu(AN);
    expect(await chay(chiVaiTro('TEACHER'), '/gv')).toBe('/hs');
    gia.dangNhapNhu(GV);
    expect(await chay(chiVaiTro('STUDENT'), '/hs')).toBe('/gv');
  });

  it('trang đăng nhập: có phiên → về trang chủ; không có → ở lại', async () => {
    expect(await chay(chuaDangNhap, '/dang-nhap')).toBe(true);
    gia.khoiPhucDuoc = GV;
    expect(await chay(chuaDangNhap, '/dang-nhap')).toBe('/gv');
  });
});
