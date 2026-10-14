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
    localStorage.removeItem('mathl-co-phien');
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    phien = TestBed.inject(Phien);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { http.verify(); localStorage.removeItem('mathl-co-phien'); });

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
    localStorage.setItem('mathl-co-phien','1');
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

  it('đăng nhập, làm mới, đăng xuất đều xếp hàng qua cùng một Web Lock', async () => {
    const khoa: string[] = [];
    Object.defineProperty(navigator, 'locks', {
      configurable: true,
      value: { request: (ten: string, viec: () => Promise<unknown>) => (khoa.push(ten), viec()) },
    });
    try {
      await dangNhapAn();
      const lamMoi = phien.lamMoi();
      http.expectOne('/api/auth/refresh').flush(phienAn('a2'));
      await lamMoi;
      const xong = phien.dangXuat();
      http.expectOne('/api/auth/logout').flush(null, { status: 204, statusText: 'No Content' });
      await xong;
      expect(khoa).toEqual(['hta-phien', 'hta-phien', 'hta-phien']);
    } finally {
      delete (navigator as { locks?: unknown }).locks;
    }
  });

  it('khách không có dấu phiên không gửi refresh, kể cả gọi đồng thời', async () => {
    expect(await Promise.all([phien.lamMoi(),phien.lamMoi()])).toEqual([false,false]);
    http.expectNone('/api/auth/refresh');
  });

  it('đăng nhập lưu dấu phiên để tải lại, đăng xuất xóa dấu', async () => {
    await dangNhapAn();
    expect(localStorage.getItem('mathl-co-phien')).toBe('1');
    // Một tab vừa tải lại chỉ có dấu phiên, không có access token trong bộ nhớ.
    const moi=TestBed.runInInjectionContext(()=>new Phien());
    const refresh=moi.lamMoi();http.expectOne('/api/auth/refresh').flush(phienAn('a2'));
    expect(await refresh).toBe(true);
    const logout=moi.dangXuat();http.expectOne('/api/auth/logout').flush(null);await logout;
    expect(localStorage.getItem('mathl-co-phien')).toBeNull();
    expect(await moi.lamMoi()).toBe(false);http.expectNone('/api/auth/refresh');
  });

  it('401 xóa dấu hết hạn; lỗi mạng giữ dấu để thử lại', async () => {
    localStorage.setItem('mathl-co-phien','1');
    const failed=phien.lamMoi();http.expectOne('/api/auth/refresh').error(new ProgressEvent('error'));
    expect(await failed).toBe(false);expect(localStorage.getItem('mathl-co-phien')).toBe('1');
    const expired=phien.lamMoi();http.expectOne('/api/auth/refresh').flush({},LOI_401);
    expect(await expired).toBe(false);expect(localStorage.getItem('mathl-co-phien')).toBeNull();
    expect(await phien.lamMoi()).toBe(false);http.expectNone('/api/auth/refresh');
  });

  it('lưu trữ bị tắt: khách vẫn không gửi refresh', async () => {
    const read=vi.spyOn(Storage.prototype,'getItem').mockImplementation(()=>{throw new Error('Không cho lưu trữ');});
    try{expect(await phien.lamMoi()).toBe(false);http.expectNone('/api/auth/refresh');}finally{read.mockRestore();}
  });

  it('trang chủ theo vai trò', () => {
    expect(trangChuCua('STUDENT')).toBe('/hs');
    expect(trangChuCua('TEACHER')).toBe('/gv');
    expect(trangChuCua('SCHOOL_ADMIN')).toBe('/qt');
  });
});
