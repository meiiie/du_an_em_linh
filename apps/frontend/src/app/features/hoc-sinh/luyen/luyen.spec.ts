import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { API_HS, ChiTietBai } from '../../../api/hoc-sinh';
import { NAP_MATHLIVE } from '../../../shared/toan/o-cong-thuc';
import { Luyen } from './luyen';

const MA = 'GEN-bac_ba-11';
const CAC_BUOC = [
  { maBuoc: 'B.DH.TXD', ten: 'Tập xác định', viec: 'Tìm tập xác định D của hàm số.' },
  { maBuoc: 'B.DH.DAOHAM', ten: 'Đạo hàm', viec: 'Tính y′.' },
  { maBuoc: 'B.DH.NGHIEM', ten: 'Nghiệm', viec: 'Giải y′ = 0 và tìm điểm y′ không xác định.' },
  { maBuoc: 'B.DH.XETDAU', ten: 'Xét dấu', viec: 'Lập bảng xét dấu y′ và bảng biến thiên.' },
  { maBuoc: 'B.DH.KETLUAN', ten: 'Kết luận', viec: 'Kết luận khoảng đơn điệu và cực trị.' },
];

function chiTiet(baiLam: Partial<ChiTietBai['baiLam']> = {}, khac: Partial<ChiTietBai> = {}): ChiTietBai {
  return {
    maBai: MA,
    de: { text: 'Tìm các khoảng đồng biến, nghịch biến và cực trị của hàm số.', latex: 'y = x^{3} - 6x^{2} + 1' },
    kyNang: 'T12.DH.03',
    tenKyNang: 'Tính đơn điệu của hàm số',
    muc4: 'VAN_DUNG',
    dangTraLoi: 'TU_LUAN_5_BUOC',
    buocBatDau: null,
    khaiBaoKetLuan: ['dong_bien', 'nghich_bien', 'cuc_dai', 'cuc_tieu'],
    cacBuoc: CAC_BUOC,
    baiLam: { trangThai: 'DANG_LAM', cacBuoc: [], ...baiLam },
    coTheMoLoiGiai: false,
    ...khac,
  };
}

const daDat = (maBuoc: string, dong = [{ dong: 0, latex: '\\mathbb{R}' }]) => ({
  maBuoc,
  dong,
  bang: [],
  ketQua: 'DAT' as const,
  thongBao: 'Đúng.',
  oSai: [],
});

describe('Luyen', () => {
  async function mo(bai: ChiTietBai | { loi: number }, choOn = true) {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: NAP_MATHLIVE, useValue: () => Promise.resolve(false) },
      ],
    });
    const fixture = TestBed.createComponent(Luyen);
    fixture.componentRef.setInput('maBai', MA);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    const yc = http.expectOne({ method: 'GET', url: API_HS.chiTietBai(MA) });
    if ('loi' in bai) yc.flush({ detail: 'x' }, { status: bai.loi, statusText: 'Lỗi' });
    else yc.flush(bai);
    if (choOn) await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    const $ = <T extends HTMLElement>(s: string) => el.querySelector<T>(s);
    const chu = (s: string) => $(s)?.textContent?.replace(/\s+/g, ' ').trim();
    const go = async (testId: string, v: string) => {
      const o = $<HTMLInputElement>(`input[data-testid="${testId}"]`)!;
      o.value = v;
      o.dispatchEvent(new Event('input'));
      await fixture.whenStable();
    };
    const bam = async (s: string) => {
      $<HTMLButtonElement>(s)!.click();
      await fixture.whenStable();
    };
    /** `kiemTra` / `nopBai` tự `await` ngoài Angular: chờ thêm một vòng macrotask (AGENTS.md của frontend). */
    const xong = async () => {
      await new Promise((r) => setTimeout(r, 0));
      await fixture.whenStable();
    };
    return { fixture, http, el, $, chu, go, bam, xong };
  }

  it('vào lại bài mở bước đầu chưa đạt; «Kiểm tra» gửi đúng bước, đạt thì sang bước kế và báo bước vừa đạt', async () => {
    const t = await mo(chiTiet({ cacBuoc: [daDat('B.DH.TXD')] }));
    expect(t.chu('.tieu-de')).toBe('Bước 2/5 Đạo hàm');
    expect(t.$('[data-testid="step-B.DH.TXD"]')!.getAttribute('data-tt')).toBe('dat');
    expect(t.$('[data-testid="step-B.DH.DAOHAM"]')!.getAttribute('aria-current')).toBe('step');

    await t.go('latex-dh', '3x^2-12x');
    await t.bam('[data-testid=nop-buoc]');
    const yc = t.http.expectOne({ method: 'POST', url: API_HS.nopBuoc(MA) });
    expect(yc.request.body).toEqual({ maBuoc: 'B.DH.DAOHAM', dong: [{ dong: 0, latex: '3x^2-12x' }] });
    yc.flush({ ketQua: 'DAT', thongBao: 'Đúng.', oSai: [] });
    await t.xong();

    expect(t.chu('.tieu-de')).toBe('Bước 3/5 Nghiệm');
    expect(t.chu('[data-testid=cham-thong-bao]')).toBe('Bước Đạo hàm đạt. Em làm tiếp bước này.');
    expect(t.chu('[data-testid=nop-buoc]')).toBe('Kiểm tra');
  });

  it('sai ở bước trước (thiếu nghiệm khi xét dấu): tô dòng, hiện câu máy chủ và nút quay lại bước đó', async () => {
    const t = await mo(chiTiet({ cacBuoc: [daDat('B.DH.TXD'), daDat('B.DH.DAOHAM'), daDat('B.DH.NGHIEM', [{ dong: 0, latex: '0' }])] }));
    expect(t.chu('.tieu-de')).toBe('Bước 4/5 Xét dấu');
    await t.bam('[data-testid=nop-buoc]');
    t.http
      .expectOne(API_HS.nopBuoc(MA))
      .flush({ ketQua: 'SAI', thongBao: 'Bảng thiếu một mốc. Xem lại nghiệm của y′.', oSai: [{ maBuoc: 'B.DH.NGHIEM', dong: 0 }] });
    await t.xong();

    expect(t.chu('[data-testid=cham-thong-bao]')).toBe('Bảng thiếu một mốc. Xem lại nghiệm của y′.');
    expect(t.$('[data-testid="step-B.DH.XETDAU"]')!.getAttribute('data-tt')).toBe('sai');
    expect(t.chu('[data-testid=nop-buoc]')).toBe('Kiểm tra lại');
    await t.bam('[data-testid=quay-lai-buoc]');
    expect(t.chu('.tieu-de')).toBe('Bước 3/5 Nghiệm');
    expect(t.$<HTMLInputElement>('input[data-testid=latex-nghiem]')!.value).toBe('0');
  });

  it('kết luận có phán quyết thì nộp được; 409 hiện `detail`, nộp xong hiện kết quả và mức hiểu', async () => {
    const t = await mo(chiTiet({ cacBuoc: CAC_BUOC.slice(0, 4).map((s) => daDat(s.maBuoc)) }));
    expect(t.$('[data-testid=nop-bai]')).toBeNull();
    expect(t.$('[data-testid=latex-cd]')).not.toBeNull();
    await t.go('latex-db', '(4;+\\infty)');
    await t.bam('[data-testid=nop-buoc]');
    const yc = t.http.expectOne(API_HS.nopBuoc(MA));
    expect(yc.request.body.dong).toEqual([
      { dong: 0, latex: '(4;+\\infty)', loai: 'DONG_BIEN' },
      { dong: 1, latex: '', loai: 'NGHICH_BIEN' },
      { dong: 2, latex: 'không có cực đại', loai: 'CUC_DAI' },
      { dong: 3, latex: 'không có cực tiểu', loai: 'CUC_TIEU' },
    ]);
    yc.flush({ ketQua: 'SAI', thongBao: 'Còn thiếu khoảng đồng biến.', oSai: [{ maBuoc: 'B.DH.KETLUAN', dong: 0 }] });
    await t.xong();
    expect(t.$('[data-testid=o-kl-db]')!.classList).toContain('sai');
    expect(t.chu('.canh-bao')).toBe('Còn bước chưa đạt. Nộp bây giờ thì thầy cô thấy bài như hiện tại.');

    await t.bam('[data-testid=nop-bai]');
    t.http
      .expectOne({ method: 'POST', url: API_HS.nopBai(MA) })
      .flush({ detail: 'Em sửa bước Kết luận sau lần chấm. Bấm «Kiểm tra» lại rồi nộp.' }, { status: 409, statusText: 'Conflict' });
    await t.xong();
    expect(t.chu('[role=alert]')).toBe('Em sửa bước Kết luận sau lần chấm. Bấm «Kiểm tra» lại rồi nộp.');

    await t.bam('[data-testid=nop-bai]');
    t.http.expectOne(API_HS.nopBai(MA)).flush({ ketQua: 'SAI', mucHieu: [{ kyNang: 'T12.DH.03', muc4Truoc: 'VAN_DUNG', muc4Sau: 'THONG_HIEU' }] });
    await t.xong();
    expect(t.chu('[data-testid=da-nop] h2')).toBe('Đã nộp. Thầy cô sẽ xem các bước chưa đạt.');
    expect(t.chu('[data-testid=da-nop] .phu')).toBe('Mức hiểu Tính đơn điệu của hàm số: Vận dụng → Thông hiểu');
    expect(t.$('[data-testid=thanh-nop]')).toBeNull();
    expect(t.$('[data-testid=solve-screen]')!.getAttribute('data-da-nop')).toBe('true');
  });

  it('đề cho sẵn bước đầu: bước đó khóa, nói rõ em bắt đầu từ đâu, kể cả khi vào lại ở bước sau', async () => {
    const t = await mo(chiTiet({ cacBuoc: [daDat('B.DH.NGHIEM')] }, { buocBatDau: 'B.DH.NGHIEM' }));
    expect(t.chu('[data-testid=de-cho-san]')).toBe('Đề đã cho sẵn: tập xác định, đạo hàm. Em bắt đầu từ bước nghiệm.');
    expect(t.$<HTMLButtonElement>('[data-testid="step-B.DH.TXD"]')!.disabled).toBe(true);
    expect(t.chu('.tieu-de')).toBe('Bước 4/5 Xét dấu');
  });

  it('sửa một bước đã đạt rồi kiểm lại: dấu đạt của các bước sau mất (core không còn coi là của bài), không cho nộp', async () => {
    const t = await mo(chiTiet({ cacBuoc: CAC_BUOC.map((s) => daDat(s.maBuoc)) }));
    expect(t.$('[data-testid=nop-bai]')).not.toBeNull();
    await t.bam('[data-testid="step-B.DH.TXD"]');
    await t.go('latex-txd', '\\mathbb{R}\\setminus\\{0\\}');
    await t.bam('[data-testid=nop-buoc]');
    t.http.expectOne(API_HS.nopBuoc(MA)).flush({ ketQua: 'SAI', thongBao: 'Xem lại tập xác định.', oSai: [{ maBuoc: 'B.DH.TXD', dong: 0 }] });
    await t.xong();
    expect(CAC_BUOC.map((s) => t.$(`[data-testid="step-${s.maBuoc}"]`)!.getAttribute('data-tt'))).toEqual([
      'sai',
      'chua-lam',
      'chua-lam',
      'chua-lam',
      'chua-lam',
    ]);
    expect(t.$('[data-testid=nop-bai]')).toBeNull();
  });

  it('kiểm lại một bước không sửa gì: các bước sau giữ dấu đạt', async () => {
    const t = await mo(chiTiet({ cacBuoc: CAC_BUOC.map((s) => daDat(s.maBuoc)) }));
    await t.bam('[data-testid="step-B.DH.TXD"]');
    await t.bam('[data-testid=nop-buoc]');
    const yc = t.http.expectOne(API_HS.nopBuoc(MA));
    expect(yc.request.body).toEqual({ maBuoc: 'B.DH.TXD', dong: [{ dong: 0, latex: '\\mathbb{R}' }] });
    yc.flush({ ketQua: 'DAT', thongBao: 'Đúng.', oSai: [] });
    await t.xong();
    expect(CAC_BUOC.map((s) => t.$(`[data-testid="step-${s.maBuoc}"]`)!.getAttribute('data-tt'))).toEqual(['dat', 'dat', 'dat', 'dat', 'dat']);
    expect(t.$('[data-testid=nop-bai]')).not.toBeNull();
  });

  it('vào lại bài có hàng đạo hàm trống: gửi lại đúng số dòng core đã lưu, nên các bước sau vẫn giữ dấu đạt', async () => {
    const dh = { ...daDat('B.DH.DAOHAM'), dong: [{ dong: 1, latex: '3x^2-12x' }] };
    const t = await mo(chiTiet({ cacBuoc: CAC_BUOC.map((s) => (s.maBuoc === 'B.DH.DAOHAM' ? dh : daDat(s.maBuoc))) }));
    await t.bam('[data-testid="step-B.DH.DAOHAM"]');
    expect(t.$<HTMLInputElement>('input[data-testid=latex-dh]')!.value).toBe('');
    expect(t.$<HTMLInputElement>('input[data-testid=latex-dh-1]')!.value).toBe('3x^2-12x');
    await t.bam('[data-testid=nop-buoc]');
    const yc = t.http.expectOne(API_HS.nopBuoc(MA));
    expect(yc.request.body).toEqual({ maBuoc: 'B.DH.DAOHAM', dong: [{ dong: 1, latex: '3x^2-12x' }] });
    yc.flush({ ketQua: 'DAT', thongBao: 'Đúng.', oSai: [] });
    await t.xong();
    expect(CAC_BUOC.map((s) => t.$(`[data-testid="step-${s.maBuoc}"]`)!.getAttribute('data-tt'))).toEqual(['dat', 'dat', 'dat', 'dat', 'dat']);
  });

  it('mất phản hồi sau khi gửi nội dung mới: không giữ dấu đạt của các bước sau (core có thể đã lưu)', async () => {
    const t = await mo(chiTiet({ cacBuoc: CAC_BUOC.map((s) => daDat(s.maBuoc)) }));
    await t.bam('[data-testid="step-B.DH.TXD"]');
    await t.go('latex-txd', '\\mathbb{R}\\setminus\\{0\\}');
    await t.bam('[data-testid=nop-buoc]');
    t.http.expectOne(API_HS.nopBuoc(MA)).flush(null, { status: 502, statusText: 'Bad Gateway' });
    await t.xong();
    expect(t.chu('[role=alert]')).toBe('Chưa gửi được bài làm. Kiểm tra kết nối rồi bấm «Kiểm tra» lại.');
    expect(CAC_BUOC.map((s) => t.$(`[data-testid="step-${s.maBuoc}"]`)!.getAttribute('data-tt'))).toEqual([
      'dang-lam',
      'chua-lam',
      'chua-lam',
      'chua-lam',
      'chua-lam',
    ]);
    expect(t.$('[data-testid=nop-bai]')).toBeNull();
  });

  it('sửa nháp sau khi đã có kết luận: bước đó thôi hiện đạt, chặn nộp và nhắc kiểm tra lại', async () => {
    const t = await mo(chiTiet({ cacBuoc: CAC_BUOC.map((s) => daDat(s.maBuoc)) }));
    expect(t.chu('.tieu-de')).toBe('Bước 5/5 Kết luận');
    await t.go('latex-db', '(4;+\\infty)');
    expect(t.$('[data-testid="step-B.DH.KETLUAN"]')!.getAttribute('data-tt')).toBe('da-sua');
    expect(t.$('[data-testid=nop-bai]')).toBeNull();
    expect(t.chu('.canh-bao')).toBe('Em vừa sửa bước Kết luận. Bấm «Kiểm tra» ở bước đó rồi hãy nộp.');
  });

  it('em sửa bước trong lúc chờ chấm: kết quả về không kéo em sang bước kế, bước đó hiện đã sửa', async () => {
    const t = await mo(chiTiet());
    await t.go('latex-txd', '\\mathbb{R}');
    await t.bam('[data-testid=nop-buoc]');
    const yc = t.http.expectOne(API_HS.nopBuoc(MA));
    await t.go('latex-txd', '\\mathbb{R}\\setminus\\{1\\}');
    yc.flush({ ketQua: 'DAT', thongBao: 'Đúng.', oSai: [] });
    await t.xong();
    expect(t.chu('.tieu-de')).toBe('Bước 1/5 Tập xác định');
    expect(t.$('[data-testid="step-B.DH.TXD"]')!.getAttribute('data-tt')).toBe('da-sua');
  });

  it('mở lại bài đã nộp: gửi lại POST …/nop để core phát lại kết quả, mức hiểu và lời giải', async () => {
    // Không chờ ổn định: yêu cầu POST đang mở là việc dở của trang cho tới khi có phản hồi.
    const t = await mo(chiTiet({ trangThai: 'DA_NOP', cacBuoc: CAC_BUOC.map((s) => daDat(s.maBuoc)) }), false);
    TestBed.tick();
    const yc = t.http.expectOne({ method: 'POST', url: API_HS.nopBai(MA) });
    yc.flush({
      ketQua: 'DAT',
      mucHieu: [{ kyNang: 'T12.DH.03', muc4Truoc: 'THONG_HIEU', muc4Sau: 'VAN_DUNG' }],
      loiGiai: 'Hàm số đồng biến trên (−∞; 0) và (4; +∞).',
    });
    await t.xong();
    expect(t.chu('[data-testid=da-nop] h2')).toBe('Đã nộp. Bài đạt.');
    expect(t.chu('[data-testid=da-nop] .phu')).toBe('Mức hiểu Tính đơn điệu của hàm số: Thông hiểu → Vận dụng');
    expect(t.chu('[data-testid=loi-giai-sau-nop] p:last-child')).toBe('Hàm số đồng biến trên (−∞; 0) và (4; +∞).');
  });

  it('phát lại bài đã nộp hỏng (500): vẫn báo đã nộp theo kết luận đã lưu, trang không kẹt, xem lại được từng bước', async () => {
    const t = await mo(chiTiet({ trangThai: 'DA_NOP', cacBuoc: CAC_BUOC.map((s) => daDat(s.maBuoc)) }), false);
    TestBed.tick();
    t.http.expectOne({ method: 'POST', url: API_HS.nopBai(MA) }).flush(null, { status: 500, statusText: 'Lỗi' });
    await t.xong();
    expect(t.chu('[data-testid=da-nop] h2')).toBe('Đã nộp. Bài đạt.');
    await t.bam('[data-testid="step-B.DH.TXD"]');
    expect(t.chu('.tieu-de')).toBe('Bước 1/5 Tập xác định');
    expect(t.$('[data-testid="step-B.DH.TXD"]')!.getAttribute('aria-current')).toBe('step');
  });

  it('phát lại hỏng thì báo và «Thử lại» gọi lại, có kết quả thì hiện mức hiểu', async () => {
    const t = await mo(chiTiet({ trangThai: 'DA_NOP', cacBuoc: CAC_BUOC.map((s) => daDat(s.maBuoc)) }), false);
    TestBed.tick();
    t.http.expectOne(API_HS.nopBai(MA)).flush(null, { status: 503, statusText: 'Lỗi' });
    await t.xong();
    expect(t.chu('[data-testid=da-nop] [role=alert]')).toBe('Chưa tải được kết quả và lời giải của lần nộp.');
    t.$<HTMLButtonElement>('[data-testid=tai-lai-ket-qua]')!.click();
    TestBed.tick();
    t.http.expectOne(API_HS.nopBai(MA)).flush({ ketQua: 'DAT', mucHieu: [{ kyNang: 'T12.DH.03', muc4Truoc: 'VAN_DUNG', muc4Sau: 'VAN_DUNG_CAO' }] });
    await t.xong();
    expect(t.$('[data-testid=da-nop] [role=alert]')).toBeNull();
    expect(t.chu('[data-testid=da-nop] .phu')).toBe('Mức hiểu Tính đơn điệu của hàm số: Vận dụng → Vận dụng cao');
  });

  it('chấm xét dấu chỉ ra hàng nghiệm sai: quay lại bước nghiệm thì hàng đó được viền', async () => {
    const nghiem = { ...daDat('B.DH.NGHIEM'), dong: [{ dong: 0, latex: '0' }, { dong: 1, latex: '3' }] };
    const t = await mo(chiTiet({ cacBuoc: [daDat('B.DH.TXD'), daDat('B.DH.DAOHAM'), nghiem] }));
    await t.bam('[data-testid=nop-buoc]');
    t.http
      .expectOne(API_HS.nopBuoc(MA))
      .flush({ ketQua: 'SAI', thongBao: 'Mốc 3 không phải nghiệm của y′.', oSai: [{ maBuoc: 'B.DH.NGHIEM', dong: 1 }] });
    await t.xong();
    await t.bam('[data-testid=quay-lai-buoc]');
    expect(t.chu('.tieu-de')).toBe('Bước 3/5 Nghiệm');
    expect(t.$('[data-testid=dong-nghiem-1]')!.classList).toContain('sai');
    expect(t.$('[data-testid=dong-nghiem-0]')!.classList).not.toContain('sai');
  });

  it('đang nộp bài thì phiếu khóa, sửa lúc đó không lọt vào bài đã nộp', async () => {
    const t = await mo(chiTiet({ cacBuoc: CAC_BUOC.map((s) => daDat(s.maBuoc)) }));
    await t.bam('[data-testid=nop-bai]');
    const yc = t.http.expectOne({ method: 'POST', url: API_HS.nopBai(MA) });
    expect(t.$('input[data-testid=latex-db]')!.matches(':disabled')).toBe(true);
    yc.flush({ ketQua: 'DAT', mucHieu: [] });
    await t.xong();
    expect(t.$('input[data-testid=latex-db]')!.matches(':disabled')).toBe(true);
    expect(t.chu('[data-testid=da-nop] h2')).toBe('Đã nộp. Bài đạt.');
  });

  it('kết luận đã chấm mà một bước chưa gửi lần nào: chặn nộp, nói bước đó chưa kiểm tra (không nói «vừa sửa»)', async () => {
    const t = await mo(
      chiTiet({ cacBuoc: [...CAC_BUOC.slice(1, 4).map((s) => daDat(s.maBuoc)), { ...daDat('B.DH.KETLUAN'), ketQua: 'SAI' as const }] }),
    );
    expect(t.$('[data-testid=nop-bai]')).toBeNull();
    expect(t.chu('.canh-bao')).toBe('Bước Tập xác định chưa kiểm tra. Bấm «Kiểm tra» ở bước đó rồi hãy nộp.');
  });

  it('xóa mốc sau khi nạp lại: ô khoảng core lưu rỗng không sinh sự kiện', async () => {
    const xetDau = {
      ...daDat('B.DH.XETDAU'),
      dong: [],
      bang: [
        { hang: 'X', k: 0, giaTri: '0' },
        { hang: 'DAU_YPHAY', k: 0, giaTri: '' },
        { hang: 'DAU_YPHAY', k: 2, giaTri: '+' },
      ],
      ketQua: 'SAI' as const,
    };
    const t = await mo(chiTiet({ cacBuoc: [...CAC_BUOC.slice(0, 3).map((s) => daDat(s.maBuoc)), xetDau] }));
    await t.bam('[aria-label="Xóa mốc 0"]');
    await t.go('moc-nhap', '4');
    await t.bam('[data-testid=moc-them]');
    await t.bam('[data-testid=nop-buoc]');
    const yc = t.http.expectOne(API_HS.nopBuoc(MA));
    expect(
      yc.request.body.suKien.map((e: { hang: string; k: number; giaTriCu?: string; giaTriMoi: string }) => [e.hang, e.k, e.giaTriCu ?? null, e.giaTriMoi]),
    ).toEqual([
      ['X', 0, '0', ''],
      ['DAU_YPHAY', 2, '+', ''],
      ['X', 0, null, '4'],
    ]);
  });

  it('xóa mốc ghi sự kiện cho mốc và từng ô bị xóa theo (core đếm khi nghi đoán mò)', async () => {
    const t = await mo(chiTiet({ cacBuoc: CAC_BUOC.slice(0, 3).map((s) => daDat(s.maBuoc)) }));
    await t.go('moc-nhap', '0');
    await t.bam('[data-testid=moc-them]');
    await t.bam('[data-testid="dau-0-+"]');
    await t.bam('[aria-label="Xóa mốc 0"]');
    await t.bam('[data-testid=nop-buoc]');
    const yc = t.http.expectOne(API_HS.nopBuoc(MA));
    expect(
      yc.request.body.suKien.map((e: { hang: string; k: number; giaTriCu?: string; giaTriMoi: string }) => [e.hang, e.k, e.giaTriCu ?? null, e.giaTriMoi]),
    ).toEqual([
      ['X', 0, null, '0'],
      ['DAU_YPHAY', 0, null, '+'],
      ['X', 0, '0', ''],
      ['DAU_YPHAY', 0, '+', ''],
    ]);
  });

  it('em bấm sang bước khác trong lúc chờ chấm: đạt thì không kéo em sang bước kế, không bỏ qua bước nào', async () => {
    const t = await mo(chiTiet());
    await t.go('latex-txd', '\\mathbb{R}');
    await t.bam('[data-testid=nop-buoc]');
    const yc = t.http.expectOne(API_HS.nopBuoc(MA));
    await t.bam('[data-testid="step-B.DH.NGHIEM"]');
    yc.flush({ ketQua: 'DAT', thongBao: 'Đúng.', oSai: [], buocKe: 'B.DH.DAOHAM' });
    await t.xong();
    expect(t.chu('.tieu-de')).toBe('Bước 3/5 Nghiệm');
    expect(t.$('[data-testid="step-B.DH.TXD"]')!.getAttribute('data-tt')).toBe('dat');
    expect(t.$('[data-testid="step-B.DH.DAOHAM"]')!.getAttribute('data-tt')).toBe('chua-lam');
  });

  it('sự kiện bảng xét dấu nhập trong lúc chờ chấm được gửi ở lần kiểm sau, không mất', async () => {
    const t = await mo(chiTiet({ cacBuoc: CAC_BUOC.slice(0, 3).map((s) => daDat(s.maBuoc)) }));
    await t.go('moc-nhap', '0');
    await t.bam('[data-testid=moc-them]');
    await t.bam('[data-testid=nop-buoc]');
    const lan1 = t.http.expectOne(API_HS.nopBuoc(MA));
    expect(lan1.request.body.suKien.map((e: { hang: string; k: number; giaTriMoi: string }) => [e.hang, e.k, e.giaTriMoi])).toEqual([
      ['X', 0, '0'],
    ]);
    await t.bam('[data-testid="dau-0-+"]');
    lan1.flush({ ketQua: 'SAI', thongBao: 'Bảng chưa đủ dấu.', oSai: [] });
    await t.xong();

    await t.bam('[data-testid=nop-buoc]');
    const lan2 = t.http.expectOne(API_HS.nopBuoc(MA));
    expect(lan2.request.body.suKien.map((e: { hang: string; k: number; giaTriMoi: string }) => [e.hang, e.k, e.giaTriMoi])).toEqual([
      ['DAU_YPHAY', 0, '+'],
    ]);
  });

  it('bài không còn mở (404): nói rõ và dẫn về danh sách', async () => {
    const t = await mo({ loi: 404 });
    expect(t.chu('[role=alert] p')).toBe('Bài này không còn mở cho lớp em.');
    expect(t.$('[data-testid=solve-screen]')).toBeNull();
  });
});
