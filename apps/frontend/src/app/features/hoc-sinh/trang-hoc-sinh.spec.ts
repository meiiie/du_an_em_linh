import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { API_HS, BaiCuaHocSinh } from '../../api/hoc-sinh';
import { TrangHocSinh } from './trang-hoc-sinh';

const BAI: BaiCuaHocSinh[] = [
  {
    maBai: 'DH12-03-NB-01',
    deBai: 'Dựa vào dấu của y′, tìm các khoảng đồng biến, nghịch biến của hàm số.',
    deBaiLatex: "y = x^{3} - 3x^{2} - 9x + 2,\\quad y' = 3(x + 1)(x - 3)",
    kyNang: 'T12.DH.03',
    tenKyNang: 'Tính đơn điệu của hàm số',
    muc4: 'NHAN_BIET',
    han: '2026-10-13T10:00:00Z',
    trangThai: 'DA_NOP',
    soBuocDat: 2,
    soBuoc: 2,
    ketQua: 'DAT',
  },
  {
    maBai: 'GEN-bac_ba-11',
    deBai: 'Tìm các khoảng đồng biến, nghịch biến và cực trị của hàm số y = x^{3} - 6 x^{2} + 1.',
    deBaiLatex: 'x^{3} - 6 x^{2} + 1',
    kyNang: 'T12.DH.03',
    tenKyNang: 'Tính đơn điệu của hàm số',
    muc4: 'VAN_DUNG',
    han: null,
    trangThai: 'DANG_LAM',
    soBuocDat: 3,
    soBuoc: 5,
    ketQua: null,
  },
  {
    maBai: 'GEN-huu_ti-5',
    deBai: 'Tìm các khoảng đồng biến, nghịch biến và cực trị của hàm số y = \\frac{x}{x + 3}.',
    deBaiLatex: '\\frac{x}{x + 3}',
    kyNang: 'T12.DH.03',
    tenKyNang: 'Tính đơn điệu của hàm số',
    muc4: 'VAN_DUNG',
    han: null,
    trangThai: 'CHUA_LAM',
    soBuocDat: 0,
    soBuoc: 5,
    ketQua: null,
  },
];

describe('TrangHocSinh', () => {
  async function mo() {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    const fixture = TestBed.createComponent(TrangHocSinh);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    const el = fixture.nativeElement as HTMLElement;
    return { fixture, http, el, chu: (s: string) => el.querySelector(s)?.textContent?.replace(/\s+/g, ' ').trim() };
  }

  it('gọi GET /api/hs/bai; «Bài tiếp theo» là bài đang làm dở, nút dẫn tới phiếu của bài đó', async () => {
    const t = await mo();
    t.http.expectOne({ method: 'GET', url: API_HS.bai }).flush(BAI);
    await t.fixture.whenStable();
    expect(t.chu('.ke-tiep .cau-hoi')).toBe(BAI[1].deBai);
    expect(t.chu('.tien-do span')).toBe('3/5 bước · Vận dụng');
    expect(t.el.querySelector('[data-testid=lam-buoc-tiep]')!.getAttribute('href')).toBe('/hs/luyen/GEN-bac_ba-11');
    expect([...t.el.querySelectorAll('.tt')].map((e) => e.textContent!.trim())).toEqual(['Đạt', 'Đang làm 3/5', 'Chưa làm']);
    expect(t.chu('[data-testid="bai-DH12-03-NB-01"] .muc')).toMatch(/^Nhận biết · Hạn /);
  });

  it('không có bài đang làm thì «Bài tiếp theo» là bài chưa làm đầu tiên; hết bài thì không có thẻ', async () => {
    const t = await mo();
    t.http.expectOne(API_HS.bai).flush([BAI[0], BAI[2]]);
    await t.fixture.whenStable();
    expect(t.el.querySelector('[data-testid=lam-buoc-tiep]')!.getAttribute('href')).toBe('/hs/luyen/GEN-huu_ti-5');

    t.fixture.componentInstance['ds'].reload();
    TestBed.tick();
    t.http.expectOne(API_HS.bai).flush([BAI[0]]);
    await t.fixture.whenStable();
    expect(t.el.querySelector('.ke-tiep')).toBeNull();
  });

  it('danh sách rỗng: nói rõ đang chờ thầy cô giao bài', async () => {
    const t = await mo();
    t.http.expectOne(API_HS.bai).flush([]);
    await t.fixture.whenStable();
    expect(t.chu('.trong p')).toBe('Chưa có bài. Bài thầy cô giao sẽ hiện ở đây.');
  });

  it('máy chủ lỗi: báo lỗi và «Thử lại» gọi lại', async () => {
    const t = await mo();
    t.http.expectOne(API_HS.bai).flush({ status: 500 }, { status: 500, statusText: 'Lỗi' });
    await t.fixture.whenStable();
    expect(t.el.querySelector('[role=alert]')).not.toBeNull();
    (t.el.querySelector('[role=alert] button') as HTMLButtonElement).click();
    TestBed.tick();
    t.http.expectOne(API_HS.bai).flush(BAI);
    await t.fixture.whenStable();
    expect(t.el.querySelectorAll('app-hang-bai').length).toBe(3);
  });
});
