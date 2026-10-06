import { HttpClient, HttpErrorResponse, httpResource } from '@angular/common/http';
import {
  afterNextRender,
  Component,
  computed,
  ElementRef,
  inject,
  Injector,
  input,
  linkedSignal,
  signal,
  untracked,
  viewChild,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { API_HS, ChiTietBai, KetQuaBuoc, KetQuaCham, KetQuaNop, NopBuoc, TEN_MUC, ViTriSai } from '../../../api/hoc-sinh';
import { Katex } from '../../../shared/toan/katex';
import { OCongThuc } from '../../../shared/toan/o-cong-thuc';
import { Button } from '../../../shared/ui/button';
import { deBang, thanDe } from '../hang-bai';
import { BangXetDau, SuKienO } from './bang-xet-dau';
import { DAO_HAM, dongNghiem, KET_LUAN, Mui, NGHIEM, NhapPhieu, nhapTuBaiLam, oKetLuan, TXD, XET_DAU, yeuCauNop } from './phieu';

interface KetQuaHien {
  readonly ketQua: KetQuaCham;
  readonly thongBao: string;
  readonly oSai: readonly ViTriSai[];
}

type TrangThaiBuoc = 'de-cho' | 'sai' | 'dat' | 'cho' | 'dang-lam' | 'chua-lam';
type SuKien = NonNullable<NopBuoc['suKien']>[number];

const TEN_TRANG_THAI: Record<TrangThaiBuoc, string> = {
  'de-cho': 'đề cho sẵn',
  sai: 'chưa đạt',
  dat: 'đạt',
  cho: 'chờ thầy cô xem',
  'dang-lam': 'đang làm',
  'chua-lam': 'chưa làm',
};

/**
 * `/hs/luyen/:maBai`: phiếu năm bước, như màn làm bài của v0 (`apps/web/components/solve-client.tsx`). Đề và bài làm từ
 * `GET /api/hs/bai/{maBai}`; «Kiểm tra» gửi bước đang mở tới `POST …/buoc` và máy chủ chấm (máy khách không tính đúng /
 * sai); bước kết luận có phán quyết thì «Nộp bài» (`POST …/nop`). Không vẽ đồ thị lúc đang làm: đồ thị có cực trị lộ đáp
 * án (FR-006).
 */
@Component({
  selector: 'app-luyen',
  imports: [BangXetDau, Button, Katex, OCongThuc, RouterLink],
  templateUrl: './luyen.html',
  styleUrl: './luyen.css',
})
export class Luyen {
  readonly maBai = input.required<string>();

  private readonly http = inject(HttpClient);
  private readonly injector = inject(Injector);
  private readonly tieuDe = viewChild<ElementRef<HTMLElement>>('tieuDe');
  private readonly vungPhanHoi = viewChild<ElementRef<HTMLElement>>('vungPhanHoi');

  protected readonly bai = httpResource<ChiTietBai>(() => API_HS.chiTietBai(this.maBai()));
  protected readonly tenMuc = TEN_MUC;
  protected readonly deBang = deBang;
  protected readonly thanDe = thanDe;
  protected readonly ma = { TXD, DAO_HAM, NGHIEM, XET_DAU, KET_LUAN };

  protected readonly khongTimThay = computed(() => (this.bai.error() as HttpErrorResponse | undefined)?.status === 404);

  /** Nháp của năm bước, dựng lại từ bài làm đã lưu mỗi khi tải đề. */
  protected readonly nhap = linkedSignal<ChiTietBai | undefined, NhapPhieu | undefined>({
    source: () => this.bai.value(),
    computation: (b) => (b ? nhapTuBaiLam(b) : undefined),
  });

  /** Kết quả chấm mới nhất của từng bước: từ bài làm đã lưu, rồi từ mỗi lần «Kiểm tra». */
  protected readonly ketQua = linkedSignal<ChiTietBai | undefined, Record<string, KetQuaHien>>({
    source: () => this.bai.value(),
    computation: (b) =>
      Object.fromEntries(
        (b?.baiLam.cacBuoc ?? []).flatMap((s) =>
          s.ketQua ? [[s.maBuoc, { ketQua: s.ketQua, thongBao: s.thongBao ?? '', oSai: s.oSai }]] : [],
        ),
      ),
  });

  /**
   * Thân yêu cầu (không kèm sự kiện) core đang lưu cho từng bước. Lần chấm của một bước chỉ còn là của bước khi mọi bước
   * từ bước bắt đầu tới nó giữ nguyên nội dung (core so băm), nên gửi nội dung mới cho một bước thì bỏ kết quả các bước sau.
   */
  private readonly daLuu = linkedSignal<ChiTietBai | undefined, Record<string, string>>({
    source: () => this.bai.value(),
    computation: (b) => {
      if (!b) return {};
      const n = nhapTuBaiLam(b);
      return Object.fromEntries(b.baiLam.cacBuoc.map((s) => [s.maBuoc, JSON.stringify(yeuCauNop(s.maBuoc, n, b.khaiBaoKetLuan))]));
    },
  });

  private readonly batDau = computed(() => {
    const b = this.bai.value();
    return b?.buocBatDau ? Math.max(0, b.cacBuoc.findIndex((s) => s.maBuoc === b.buocBatDau)) : 0;
  });

  /**
   * Bước đang mở. Lúc tải đề: bước đầu tiên từ bước bắt đầu chưa đạt (đạt hết thì bước cuối). Sau đó chỉ đổi khi em bấm
   * một bước hay vừa đạt bước đang làm, nên đọc `ketQua` không theo dõi.
   */
  protected readonly buoc = linkedSignal<ChiTietBai | undefined, number>({
    source: () => this.bai.value(),
    computation: (b) => {
      if (!b) return 0;
      const kq = untracked(this.ketQua);
      const i = b.cacBuoc.findIndex((s, j) => j >= this.batDau() && kq[s.maBuoc]?.ketQua !== 'DAT');
      return i < 0 ? b.cacBuoc.length - 1 : i;
    },
  });

  protected readonly buocHien = computed(() => this.bai.value()?.cacBuoc[this.buoc()]);
  protected readonly ketQuaHien = computed(() => {
    const ma = this.buocHien()?.maBuoc;
    return ma ? this.ketQua()[ma] : undefined;
  });
  protected readonly cacOKetLuan = computed(() => oKetLuan(this.bai.value()?.khaiBaoKetLuan ?? []));
  protected readonly deChoSan = computed(() =>
    (this.bai.value()?.cacBuoc.slice(0, this.batDau()) ?? []).map((s) => s.ten.toLowerCase()).join(', '),
  );
  protected readonly tenBuocDau = computed(() => this.bai.value()?.cacBuoc[this.batDau()]?.ten.toLowerCase() ?? '');

  /** Lỗi gốc nằm ở một bước trước bước đang mở (vd thiếu nghiệm khi đang xét dấu): nút quay lại bước đó (v0 UXT-04-d). */
  protected readonly buocQuayLai = computed(() => {
    const b = this.bai.value();
    const kq = this.ketQuaHien();
    if (!b || kq?.ketQua !== 'SAI') return undefined;
    const i = b.cacBuoc.findIndex((s) => kq.oSai.some((o) => o.maBuoc === s.maBuoc));
    return i >= this.batDau() && i < this.buoc() ? { i, ...b.cacBuoc[i] } : undefined;
  });

  protected readonly datHet = computed(() => {
    const b = this.bai.value();
    return !!b && b.cacBuoc.slice(this.batDau()).every((s) => this.ketQua()[s.maBuoc]?.ketQua === 'DAT');
  });

  /** Nộp được khi bước kết luận có phán quyết (core trả 409 nếu thiếu bước; `detail` nói em phải làm gì). */
  protected readonly nopDuoc = computed(() => {
    const kq = this.ketQua()[KET_LUAN]?.ketQua;
    return !!kq && kq !== 'KHONG_CHAM_DUOC';
  });

  /** Tên bước vừa đạt, để bước kế báo «Bước … đạt» (v0) cho tới khi em chấm bước này hay đổi bước. */
  protected readonly vuaDat = signal<string | null>(null);
  protected readonly dangGui = signal(false);
  protected readonly loiGui = signal<string | null>(null);
  protected readonly ketQuaNop = signal<KetQuaNop | null>(null);
  protected readonly daNop = computed(() => !!this.ketQuaNop() || this.bai.value()?.baiLam.trangThai === 'DA_NOP');
  protected readonly chuDaNop = computed(() => {
    switch (this.ketQuaNop()?.ketQua ?? this.ketQua()[KET_LUAN]?.ketQua) {
      case 'DAT':
        return 'Đã nộp. Bài đạt.';
      case 'KHONG_KIEM_DUOC':
        return 'Đã nộp. Máy chưa kiểm được một bước, thầy cô sẽ xem.';
      default:
        return 'Đã nộp. Thầy cô sẽ xem các bước chưa đạt.';
    }
  });
  protected readonly loiGiai = computed(() => {
    const l = this.ketQuaNop()?.loiGiai;
    return typeof l === 'string' && l.trim() ? l : null;
  });

  /** Sự kiện nhập ở bảng xét dấu chưa gửi (hợp đồng: tối đa 500 mỗi lần nộp). */
  private suKien: SuKien[] = [];

  protected trangThai(i: number): TrangThaiBuoc {
    const b = this.bai.value();
    if (!b) return 'chua-lam';
    if (i < this.batDau()) return 'de-cho';
    const kq = this.ketQua()[b.cacBuoc[i].maBuoc]?.ketQua;
    if (kq === 'SAI') return 'sai';
    if (kq === 'DAT') return 'dat';
    if (kq === 'KHONG_KIEM_DUOC') return 'cho';
    return i === this.buoc() ? 'dang-lam' : 'chua-lam';
  }

  protected tenTrangThai(i: number): string {
    return TEN_TRANG_THAI[this.trangThai(i)];
  }

  protected moBuoc(i: number): void {
    if (i < this.batDau()) return;
    this.buoc.set(i);
    this.vuaDat.set(null);
    this.loiGui.set(null);
    this.duaTieuDiem();
  }

  protected datTxd(v: string): void {
    this.sua((n) => ({ ...n, txd: v }));
  }

  protected datDh(i: number, v: string): void {
    this.sua((n) => ({ ...n, dh: n.dh.map((x, j) => (j === i ? v : x)) }));
  }

  protected themDongDh(): void {
    this.sua((n) => ({ ...n, dh: [...n.dh, ''] }));
  }

  protected datNghiem(i: number, v: string): void {
    this.sua((n) => ({ ...n, nghiem: n.nghiem.map((x, j) => (j === i ? { ...x, latex: v } : x)) }));
  }

  protected doiLoaiNghiem(i: number): void {
    this.sua((n) => ({
      ...n,
      nghiem: n.nghiem.map((x, j) => (j === i ? { ...x, loai: x.loai === 'NGHIEM' ? 'KHONG_XD' : 'NGHIEM' } : x)),
    }));
  }

  protected themNghiem(): void {
    this.sua((n) => ({ ...n, nghiem: [...n.nghiem, { latex: '', loai: 'NGHIEM' }] }));
  }

  protected datMoc(moc: string[]): void {
    this.sua((n) => ({ ...n, moc }));
  }

  protected datDau(dau: Record<number, string>): void {
    this.sua((n) => ({ ...n, dau }));
  }

  protected datMui(mui: Record<number, Mui>): void {
    this.sua((n) => ({ ...n, mui }));
  }

  protected datKl(ma: string, v: string): void {
    this.sua((n) => ({ ...n, kl: { ...n.kl, [ma]: v } }));
  }

  protected dongSai(maBuoc: string, dong: number): boolean {
    return !!this.ketQua()[maBuoc]?.oSai.some((o) => o.maBuoc === maBuoc && o.dong === dong);
  }

  /** Hàng nghiệm thứ `i` trên màn ứng với dòng nào đã gửi (hàng trống không gửi). */
  protected nghiemSai(n: NhapPhieu, i: number): boolean {
    const dong = dongNghiem(n, i);
    return dong != null && this.dongSai(NGHIEM, dong);
  }

  /** Bước sai mà máy chủ không chỉ dòng hay ô nào: viền cả khối. */
  protected caBuocSai(maBuoc: string): boolean {
    const kq = this.ketQua()[maBuoc];
    return kq?.ketQua === 'SAI' && !kq.oSai.some((o) => o.maBuoc === maBuoc && (o.dong != null || o.k != null));
  }

  protected ghiSuKien(e: SuKienO): void {
    if (this.suKien.length < 500) {
      this.suKien.push({
        maBuoc: XET_DAU,
        hang: e.hang,
        k: e.k,
        giaTriCu: e.giaTriCu ?? undefined,
        giaTriMoi: e.giaTriMoi,
        luc: new Date().toISOString(),
      });
    }
  }

  protected async kiemTra(): Promise<void> {
    const b = this.bai.value();
    const n = this.nhap();
    const buoc = this.buocHien();
    if (!b || !n || !buoc || this.dangGui()) return;
    const i = this.buoc();
    const noiDung = yeuCauNop(buoc.maBuoc, n, b.khaiBaoKetLuan);
    // Chép, không giữ tham chiếu: sự kiện nhập trong lúc chờ phải ở lại cho lần gửi sau.
    const suKien = buoc.maBuoc === XET_DAU ? [...this.suKien] : [];
    this.dangGui.set(true);
    this.loiGui.set(null);
    try {
      const kq = await firstValueFrom(
        this.http.post<KetQuaBuoc>(API_HS.nopBuoc(b.maBai), suKien.length ? { ...noiDung, suKien } : noiDung),
      );
      this.suKien.splice(0, suKien.length);
      const moi = JSON.stringify(noiDung);
      const doi = this.daLuu()[buoc.maBuoc] !== moi;
      this.daLuu.update((m) => ({ ...m, [buoc.maBuoc]: moi }));
      this.ketQua.update((m) => {
        const giu = doi ? Object.fromEntries(Object.entries(m).filter(([ma]) => b.cacBuoc.findIndex((s) => s.maBuoc === ma) < i)) : m;
        return { ...giu, [buoc.maBuoc]: { ketQua: kq.ketQua, thongBao: kq.thongBao, oSai: kq.oSai } };
      });
      this.vuaDat.set(null);
      // Em đã bấm sang bước khác trong lúc chờ thì để em ở đó.
      if (kq.ketQua === 'DAT' && this.buoc() === i && i < b.cacBuoc.length - 1) {
        this.vuaDat.set(buoc.ten);
        this.buoc.set(i + 1);
        this.duaTieuDiem();
      } else {
        this.hienPhanHoi();
      }
    } catch (e) {
      // 400 là thân không hợp lệ: bỏ sự kiện đã gửi, kẻo mọi lần kiểm sau mang lại đúng sự kiện hỏng đó.
      if (e instanceof HttpErrorResponse && e.status === 400) this.suKien.splice(0, suKien.length);
      this.loiGui.set(loiDeDoc(e, 'Chưa gửi được bài làm. Kiểm tra kết nối rồi bấm «Kiểm tra» lại.'));
      this.hienPhanHoi();
    } finally {
      this.dangGui.set(false);
    }
  }

  protected async nopBai(): Promise<void> {
    const b = this.bai.value();
    if (!b || this.dangGui()) return;
    this.dangGui.set(true);
    this.loiGui.set(null);
    try {
      this.ketQuaNop.set(await firstValueFrom(this.http.post<KetQuaNop>(API_HS.nopBai(b.maBai), {})));
    } catch (e) {
      this.loiGui.set(loiDeDoc(e, 'Chưa nộp được bài. Kiểm tra kết nối rồi thử lại.'));
      this.hienPhanHoi();
    } finally {
      this.dangGui.set(false);
    }
  }

  private sua(f: (n: NhapPhieu) => NhapPhieu): void {
    this.nhap.update((n) => (n ? f(n) : n));
  }

  /** Câu chấm nằm dưới các ô: trên điện thoại cuộn tới nó, kẻo thanh «Kiểm tra» dính đáy che mất. */
  private hienPhanHoi(): void {
    afterNextRender(() => this.vungPhanHoi()?.nativeElement.scrollIntoView?.({ block: 'nearest' }), { injector: this.injector });
  }

  /** Đổi bước thì đưa tiêu điểm về tiêu đề bước, để trình đọc màn hình đọc bước mới. */
  private duaTieuDiem(): void {
    afterNextRender(() => this.tieuDe()?.nativeElement.focus(), { injector: this.injector });
  }
}

/** Lỗi đọc được cho học sinh: `detail` của ProblemDetail (409: em phải làm gì) nếu máy chủ trả, không thì câu mặc định. */
function loiDeDoc(e: unknown, macDinh: string): string {
  if (e instanceof HttpErrorResponse) {
    const detail = (e.error as { detail?: unknown } | null)?.detail;
    if (typeof detail === 'string' && detail.trim()) return detail;
    if (e.status === 404) return 'Bài này không còn mở cho lớp em.';
  }
  return macDinh;
}
