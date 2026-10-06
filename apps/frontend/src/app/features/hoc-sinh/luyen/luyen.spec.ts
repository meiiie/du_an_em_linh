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
  async function mo(bai: ChiTietBai | { loi: number }) {
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
    await fixture.whenStable();
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

  it('đề cho sẵn bước đầu: bước đó khóa, nói rõ em bắt đầu từ đâu', async () => {
    const t = await mo(chiTiet({}, { buocBatDau: 'B.DH.NGHIEM' }));
    expect(t.chu('[data-testid=de-cho-san]')).toBe('Đề đã cho sẵn: tập xác định, đạo hàm. Em bắt đầu từ bước nghiệm.');
    expect(t.$<HTMLButtonElement>('[data-testid="step-B.DH.TXD"]')!.disabled).toBe(true);
    expect(t.chu('.tieu-de')).toBe('Bước 3/5 Nghiệm');
  });

  it('bài không còn mở (404): nói rõ và dẫn về danh sách', async () => {
    const t = await mo({ loi: 404 });
    expect(t.chu('[role=alert] p')).toBe('Bài này không còn mở cho lớp em.');
    expect(t.$('[data-testid=solve-screen]')).toBeNull();
  });
});
