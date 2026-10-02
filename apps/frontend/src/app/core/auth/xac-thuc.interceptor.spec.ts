import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { Phien } from './phien';
import { AN, PhienGia } from './phien.testing';
import { xacThucInterceptor } from './xac-thuc.interceptor';

const LOI_401 = { status: 401, statusText: 'Unauthorized' };
const choXong = () => new Promise((r) => setTimeout(r, 0));

describe('xacThucInterceptor', () => {
  let gia: PhienGia;
  let client: HttpClient;
  let http: HttpTestingController;

  beforeEach(() => {
    gia = new PhienGia().dangNhapNhu(AN, 'a1');
    TestBed.configureTestingModule({
      providers: [
        { provide: Phien, useValue: gia },
        provideHttpClient(withInterceptors([xacThucInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('gắn Bearer cho /api/ cùng gốc; không gắn cho /api/auth/ hay miền khác', () => {
    client.get('/api/me').subscribe();
    client.post('/api/auth/login', {}).subscribe();
    client.get('https://vi-du.test/api/me').subscribe();

    expect(http.expectOne('/api/me').request.headers.get('Authorization')).toBe('Bearer a1');
    expect(http.expectOne('/api/auth/login').request.headers.has('Authorization')).toBe(false);
    expect(http.expectOne('https://vi-du.test/api/me').request.headers.has('Authorization')).toBe(false);
  });

  it('401 → làm mới một lần rồi gửi lại với token mới', async () => {
    gia.khoiPhucDuoc = AN;
    const ketQua = firstValueFrom(client.get('/api/me'));
    http.expectOne('/api/me').flush(null, LOI_401);
    await choXong();

    const guiLai = http.expectOne('/api/me');
    expect(guiLai.request.headers.get('Authorization')).toBe('Bearer a2');
    guiLai.flush({ id: AN.id });
    expect(await ketQua).toEqual({ id: AN.id });
    expect(gia.soLanLamMoi).toBe(1);
  });

  it('làm mới thất bại → trả lỗi 401 gốc, không gửi lại', async () => {
    // Bắt lỗi ngay khi tạo promise: lỗi đến trong lúc chờ setTimeout, chưa gắn handler thì Vitest báo «unhandled».
    const ketQua = firstValueFrom(client.get('/api/me')).catch((e: unknown) => e);
    http.expectOne('/api/me').flush(null, LOI_401);
    await choXong();

    expect(await ketQua).toBeInstanceOf(HttpErrorResponse);
    expect((await ketQua) as HttpErrorResponse).toMatchObject({ status: 401 });
    http.expectNone('/api/me');
    expect(gia.daDangNhap()).toBe(false);
  });

  it('lỗi khác 401 đi thẳng, không làm mới', async () => {
    const ketQua = firstValueFrom(client.get('/api/me')).catch((e: unknown) => e);
    http.expectOne('/api/me').flush(null, { status: 500, statusText: 'Server Error' });

    expect(await ketQua).toMatchObject({ status: 500 });
    expect(gia.soLanLamMoi).toBe(0);
  });
});
