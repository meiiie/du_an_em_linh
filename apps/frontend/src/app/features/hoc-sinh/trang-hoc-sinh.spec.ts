import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { API_HS, BaiCuaHocSinh, TrangHoc } from '../../api/hoc-sinh';
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
  /** `trangHoc`: thân trả cho `GET /api/hs/trang-hoc` (null: lỗi 500); mặc định chưa có kỹ năng nào. */
  async function mo(trangHoc: TrangHoc | null = { ten: 'An', soKyNang: [], hoanThanh: { kyNang: [], chuDe: [] } }) {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    const fixture = TestBed.createComponent(TrangHocSinh);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    const th = http.expectOne(API_HS.trangHoc);
    if (trangHoc) th.flush(trangHoc);
    else th.flush(null, { status: 500, statusText: 'Lỗi' });
    const el = fixture.nativeElement as HTMLElement;
    return { fixture, http, el, chu: (s: string) => el.querySelector(s)?.textContent?.replace(/\s+/g, ' ').trim() };
  }

  it('gọi GET /api/hs/bai; «Bài tiếp theo» là bài đang làm dở, nút dẫn tới phiếu của bài đó', async () => {
    const t = await mo();
    t.http.expectOne({ method: 'GET', url: API_HS.bai }).flush(BAI);
    await t.fixture.whenStable();
    expect(t.chu('.ke-tiep .cau-hoi')).toBe('Tìm các khoảng đồng biến, nghịch biến và cực trị của hàm số');
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

  it('sổ «Kỹ năng» từ GET /api/hs/trang-hoc: mức bằng lời, ô tô tới mức, nhãn kẹt kèm lời nhắc', async () => {
    const t = await mo({
      ten: 'An',
      soKyNang: [
        { kyNang: 'T12.DH.05', tenKyNang: 'Tìm cực trị của hàm số', muc4: 'NHAN_BIET', ket: true },
        { kyNang: 'T12.DH.03', tenKyNang: 'Tính đơn điệu của hàm số', muc4: 'VAN_DUNG', ket: false },
      ],
      hoanThanh: { kyNang: [], chuDe: [] },
    });
    t.http.expectOne(API_HS.bai).flush(BAI);
    await t.fixture.whenStable();
    expect(t.chu('[data-testid="ky-nang-T12.DH.05"] .muc')).toBe('kẹt Nhận biết');
    expect(t.chu('[data-testid="ky-nang-T12.DH.05"] .nhac')).toBe(
      'Em sai liên tiếp ở kỹ năng này. Thầy cô đã nhận được báo; em xem lại bảng công thức rồi làm lại nhé.',
    );
    expect(t.el.querySelectorAll('[data-testid="ky-nang-T12.DH.03"] .bac i.dat').length).toBe(3);
    expect(t.el.querySelector('[data-testid="ky-nang-T12.DH.03"] .nhac')).toBeNull();
  });

  it('chưa có kỹ năng nào: nói em làm một bài để hiện kỹ năng', async () => {
    const t = await mo();
    t.http.expectOne(API_HS.bai).flush(BAI);
    await t.fixture.whenStable();
    expect(t.chu('[data-testid=so-ky-nang] .trong p')).toBe('Chưa làm bài — làm một bài để hiện kỹ năng.');
  });

  it('trang-hoc lỗi: chỉ ẩn sổ kỹ năng, danh sách bài vẫn hiện', async () => {
    const t = await mo(null);
    t.http.expectOne(API_HS.bai).flush(BAI);
    await t.fixture.whenStable();
    expect(t.el.querySelector('[data-testid=so-ky-nang]')).toBeNull();
    expect(t.el.querySelectorAll('app-hang-bai').length).toBe(3);
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
