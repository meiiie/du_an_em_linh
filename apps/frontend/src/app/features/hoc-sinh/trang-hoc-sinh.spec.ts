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
  it('gợi bài NT-DH12-NB-01 kiểm tra không lấn bài luyện chưa nộp dù bài luyện thiếu chế độ', async () => {
    const kt = {...BAI[2], maBai: 'NT-DH12-NB-01', tenBo: 'Kiểm tra hàm số — 45 phút'};
    const t = await mo(undefined, [], [{id:kt.maBai,status:'KIEM_TRA',details:{dueAt:kt.han}}],
      [{id:kt.maBai,title:'',subtitle:'Gợi ý kiểm tra',status:'',details:{}}]);
    t.http.expectOne(API_HS.bai).flush([kt, BAI[0], BAI[2]]);
    await t.fixture.whenStable();
    expect(t.el.querySelector('[data-testid=lam-buoc-tiep]')?.getAttribute('href')).toBe('/hs/luyen/GEN-huu_ti-5');
    expect(t.chu('[data-testid=lam-buoc-tiep]')).toBe('Làm bước tiếp');
    expect(t.chu('.ke-tiep')).not.toContain('Gợi ý kiểm tra');
    t.http.verify();
  });

  it('dùng tên bộ, hạn Việt và đếm bài chờ xem là đã nộp', async () => {
    const t = await mo();
    t.http.expectOne(API_HS.bai).flush(BAI.map((b, i) => ({...b, tenBo:'Ôn tập tuần 7', han:'2026-10-20T13:00:00Z', trangThai:i === 0 ? 'DA_NOP' : b.trangThai, ketQua:i === 0 ? 'KHONG_KIEM_DUOC' : b.ketQua})));
    await t.fixture.whenStable();
    expect(t.chu('.nhom-sap-toi h3')).toBe('Ôn tập tuần 7');
    expect(t.chu('.nhom-sap-toi .list-summary')).toContain('1/3 đã nộp · Hạn: Thứ Ba, 20/10');
    t.http.verify();
  });

  it('ưu tiên bài ôn chưa làm trước bài kiểm tra đang làm', async () => {
    const t = await mo(undefined, [], BAI.map(b => ({id:b.maBai,status:b.maBai === BAI[1].maBai ? 'KIEM_TRA' : 'ON_TAP',details:{dueAt:b.han}})));
    t.http.expectOne(API_HS.bai).flush(BAI);
    await t.fixture.whenStable();
    expect(t.el.querySelector('[data-testid=lam-buoc-tiep]')?.getAttribute('href')).toBe('/hs/luyen/GEN-huu_ti-5');
    expect(t.chu('.ke-tiep')).not.toContain('Bắt đầu kiểm tra');
  });

  it('hết bài ôn thì ghi rõ bài kiểm tra, hạn, số câu và gia sư tạm tắt', async () => {
    const t = await mo(undefined, [], BAI.map(b => ({id:b.maBai,status:'KIEM_TRA',details:{dueAt:'2026-10-20T12:00:00Z'}})));
    t.http.expectOne(API_HS.bai).flush(BAI.map(b => ({...b,tenBo:'Kiểm tra hàm số — 45 phút',han:'2026-10-20T12:00:00Z'})));
    await t.fixture.whenStable();
    expect(t.chu('.ke-tiep')).toContain('Bài kiểm tra · 45 phút');
    expect(t.chu('.ke-tiep')).toContain('3 câu');
    expect(t.chu('.ke-tiep')).toContain('20/10');
    expect(t.chu('[data-testid=lam-buoc-tiep]')).toBe('Bắt đầu kiểm tra');
    expect(t.chu('.ke-tiep')).toContain('Gia sư tạm tắt');
    expect(t.chu('app-hang-bai')).toContain('Kiểm tra');
  });

  /** `trangHoc`: thân trả cho `GET /api/hs/trang-hoc` (null: lỗi 500); mặc định chưa có kỹ năng nào. */
  async function mo(trangHoc: TrangHoc | null = { ten: 'An', soKyNang: [], hoanThanh: { kyNang: [], chuDe: [] } }, nhac: unknown[] = [], modes = BAI.map(b => ({id:b.maBai,status:'ON_TAP',details:{dueAt:b.han}})), recommendations: unknown[] = []) {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    const fixture = TestBed.createComponent(TrangHocSinh);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/hs/che-do').flush(modes);
    http.expectOne('/api/hs/goi-bai').flush(recommendations);
    http.expectOne('/api/hs/nhac').flush(nhac);
    const th = http.expectOne(API_HS.trangHoc);
    if (trangHoc) th.flush(trangHoc);
    else th.flush(null, { status: 500, statusText: 'Lỗi' });
    const el = fixture.nativeElement as HTMLElement;
    return { fixture, http, el, chu: (s: string) => el.querySelector(s)?.textContent?.replace(/\s+/g, ' ').trim() };
  }

  it('nhắc hạn dùng tên bài và dueAt thay mã bài và startsAt trống', async () => {
    const t=await mo(undefined,[{id:'reminder',title:'Sắp đến hạn: DH12-03-NB-01',subtitle:'',status:'ON_TAP',details:{dueAt:'2026-10-20T13:00:00Z'}}]);
    t.http.expectOne(API_HS.bai).flush(BAI);await t.fixture.whenStable();
    expect(t.chu('.hoc-so .row-list strong')).toBe('Sắp đến hạn: Tính đơn điệu của hàm số');
    expect(t.chu('.hoc-so .row-list p')).toBe('Thứ Ba, 20/10 · 20:00');
    t.http.verify();
  });

  it('chỉ hiện hai nhắc hạn và giữ liên kết lịch sau danh sách', async () => {
    const reminders = Array.from({length:4}, (_, i) => ({id:'nhac'+i,title:'Nhắc học '+i,subtitle:'',status:'',details:{startsAt:'2026-10-20T13:00:00Z'}}));
    const t = await mo(undefined, reminders);
    t.http.expectOne(API_HS.bai).flush(BAI);
    await t.fixture.whenStable();
    expect(t.el.querySelectorAll('.nhac-hoc .row-list > li')).toHaveLength(2);
    expect(t.el.querySelector('.nhac-hoc .row-list')?.nextElementSibling?.getAttribute('href')).toBe('/hs/lich');
    expect(t.chu('.nhac-hoc')).not.toContain('Nhắc học 2');
    t.http.verify();
  });

  it('gọi GET /api/hs/bai; «Bài tiếp theo» là bài đang làm dở, nút dẫn tới phiếu của bài đó', async () => {
    const t = await mo();
    t.http.expectOne({ method: 'GET', url: API_HS.bai }).flush(BAI);
    await t.fixture.whenStable();
    expect(t.chu('.ke-tiep .cau-hoi')).toBe('Tìm các khoảng đồng biến, nghịch biến và cực trị của hàm số');
    expect(t.chu('.tien-do span')).toBe('3/5 bước · Vận dụng');
    expect(t.el.querySelector('[data-testid=lam-buoc-tiep]')!.getAttribute('href')).toBe('/hs/luyen/GEN-bac_ba-11');
    expect([...t.el.querySelectorAll('.tt')].map((e) => e.textContent!.trim())).toEqual(['Chưa làm']);
    expect(t.chu('.nhom-sap-toi .list-summary')).toContain('1/3 đã nộp');
    expect(t.el.querySelector('.xem-tat-ca')?.getAttribute('href')).toBe('/hs/bai');
  });

  it('24 bài chỉ hiện tối đa 5 bài sắp tới, nhóm có tiến độ đầy đủ', async () => {
    const t = await mo(undefined, [], Array.from({length:24},(_,i)=>({id:'b'+i,status:'ON_TAP',details:{dueAt:null}})));
    t.http.expectOne(API_HS.bai).flush(Array.from({length:24},(_,i)=>({...BAI[2],maBai:'b'+i})));
    await t.fixture.whenStable();
    expect(t.el.querySelectorAll('app-hang-bai')).toHaveLength(5);
    expect(t.chu('.nhom-sap-toi .list-summary')).toContain('0/24 đã nộp');
    expect(t.el.querySelector('[data-testid=bai-b0]')).toBeNull();
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
        { kyNang: 'T12.DH.03', tenKyNang: 'Tính đơn điệu của hàm số', muc4: 'VAN_DUNG', ket: true },
      ],
      hoanThanh: { kyNang: [], chuDe: [] },
    });
    t.http.expectOne(API_HS.bai).flush(BAI);
    await t.fixture.whenStable();
    expect(t.chu('[data-testid="ky-nang-T12.DH.05"] .muc')).toBe('kẹt Nhận biết');
    expect(t.chu('[data-testid=so-ky-nang] .nhac')).toBe(
      'Kỹ năng đang kẹt: em xem lại công thức hoặc hỏi gia sư để luyện tiếp.',
    );
    expect(t.el.querySelectorAll('[data-testid="ky-nang-T12.DH.03"] .bac i.dat').length).toBe(3);
    expect(t.el.querySelectorAll('[data-testid=so-ky-nang] .nhac')).toHaveLength(1);
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
    expect(t.el.querySelectorAll('app-hang-bai').length).toBe(1);
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
    expect(t.el.querySelectorAll('app-hang-bai').length).toBe(1);
  });
});
