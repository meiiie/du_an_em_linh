import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PhienDangNhap } from '../../api/auth';
import { Phien, trangChuCua } from './phien';
import { AN } from './phien.testing';

const phienAn = (accessToken: string): PhienDangNhap => ({ accessToken, accessTokenExpiresAt: '2026-10-02T09:15:00Z', user: AN });
const LOI_401 = { status: 401, statusText: 'Unauthorized' };

describe('Phien', () => {
  let phien: Phien;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    phien = TestBed.inject(Phien);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function dangNhapAn(): Promise<void> {
    const xong = phien.dangNhap('hs.an@demo.local', 'hocsinh123');
    http.expectOne('/api/auth/login').flush(phienAn('a1'));
    await xong;
  }

  it('đăng nhập gửi email + mật khẩu, giữ access token trong bộ nhớ', async () => {
    const xong = phien.dangNhap('hs.an@demo.local', 'hocsinh123');
    const req = http.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'hs.an@demo.local', password: 'hocsinh123' });
    req.flush(phienAn('a1'));

    expect(await xong).toEqual(AN);
    expect(phien.nguoiDung()).toEqual(AN);
    expect(phien.tokenTruyCap()).toBe('a1');
  });

  it('làm mới chạy một luồng: hai lời gọi cùng lúc chỉ gửi một yêu cầu, kèm header chống CSRF, không thân', async () => {
    const a = phien.lamMoi();
    const b = phien.lamMoi();
    const req = http.expectOne('/api/auth/refresh');
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.get('X-Requested-With')).toBe('XMLHttpRequest');
    expect(req.request.body).toBeNull();
    req.flush(phienAn('a2'));

    expect([await a, await b]).toEqual([true, true]);
    expect(phien.tokenTruyCap()).toBe('a2');
    // Xong rồi thì lần sau gửi yêu cầu mới.
    const c = phien.lamMoi();
    http.expectOne('/api/auth/refresh').flush(phienAn('a3'));
    expect(await c).toBe(true);
  });

  it('làm mới thất bại → quên phiên', async () => {
    await dangNhapAn();
    const lamMoi = phien.lamMoi();
    http.expectOne('/api/auth/refresh').flush({ detail: 'Phiên đã hết hạn.' }, LOI_401);

    expect(await lamMoi).toBe(false);
    expect(phien.daDangNhap()).toBe(false);
    expect(phien.tokenTruyCap()).toBeNull();
  });

  it('đăng xuất: máy chủ nhận thì quên phiên', async () => {
    await dangNhapAn();
    const xong = phien.dangXuat();
    const req = http.expectOne('/api/auth/logout');
    expect(req.request.headers.get('X-Requested-With')).toBe('XMLHttpRequest');
    req.flush(null, { status: 204, statusText: 'No Content' });
    await xong;

    expect(phien.daDangNhap()).toBe(false);
    expect(phien.tokenTruyCap()).toBeNull();
  });

  it('đăng xuất lỗi mạng → báo lỗi, giữ phiên (cookie còn sống ở máy chủ)', async () => {
    await dangNhapAn();
    const xong = phien.dangXuat();
    http.expectOne('/api/auth/logout').error(new ProgressEvent('error'));

    await expect(xong).rejects.toBeTruthy();
    expect(phien.nguoiDung()).toEqual(AN);
  });

  it('trang chủ theo vai trò', () => {
    expect(trangChuCua('STUDENT')).toBe('/hs');
    expect(trangChuCua('TEACHER')).toBe('/gv');
    expect(trangChuCua('SCHOOL_ADMIN')).toBe('/gv');
  });
});
