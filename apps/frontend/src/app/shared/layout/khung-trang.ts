import {
  afterNextRender,
  Component,
  computed,
  DestroyRef,
  DOCUMENT,
  effect,
  ElementRef,
  inject,
  Injector,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  NavigationEnd,
  NavigationError,
  NavigationStart,
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from '@angular/router';
import { HangChoDuyet } from '../../core/hang-cho';
import { Phien } from '../../core/auth/phien';
import { ChuyenTrang } from '../ui/chuyen-trang';
import { BieuTuong } from '../ui/bieu-tuong';
import { BrandMark } from '../ui/brand-mark';
import { Button } from '../ui/button';
import { DoiGiaoDien } from '../ui/doi-giao-dien';
import { DIEU_HUONG, KhuVuc, laTrangChuKhuVuc, TEN_KHU_VUC } from './dieu-huong';

/** Cỡ `lg` của docs/DESIGN.md: từ đây thanh bên luôn hiện, dưới đây là ngăn kéo. */
const MAN_HINH_RONG = '(min-width: 1024px)';

/**
 * Khung sau đăng nhập của `/hs` và `/gv` (route cha, các trang con hiện trong `<router-outlet>`), như `AppShell` của
 * v0 (`apps/web/components/app-shell.tsx`), dáng thanh bên của Wiii. Desktop: thanh bên 260 px, nút đổi giao diện ở góc
 * phải trên. Điện thoại và máy tính bảng: thanh trên với `mo-sidebar` và nút đổi giao diện, thanh bên thành ngăn kéo
 * có `dong-sidebar`.
 *
 * Ngăn kéo là hộp thoại (`role="dialog"`, `aria-modal`): đóng thì `inert`; mở thì phần còn lại của trang `inert`, nên
 * Tab không ra được chỗ bị ray che (WCAG 2.4.11), và trang sau không cuộn. Esc, chạm nền hay nút đóng thì đóng và trả
 * tiêu điểm về nút mở. Chọn một mục thì đóng ngay (cả khi bấm đúng trang đang mở) và đưa tiêu điểm vào nội dung.
 *
 * Mỗi lần chuyển trang, vùng `aria-live` đọc tiêu đề trang mới (v0 có route announcer của Next.js). Nút Đăng xuất chỉ
 * có một trong DOM: ở thanh trên khi màn hẹp, ở chân ray khi màn rộng. Phiên mất thì về `/dang-nhap`.
 */
@Component({
  selector: 'app-khung-trang',
  imports: [ChuyenTrang, RouterLink, RouterLinkActive, RouterOutlet, BieuTuong, BrandMark, Button, DoiGiaoDien],
  host: { '(document:keydown.escape)': 'dong(true)' },
  template: `
    <a class="skip-link" href="#noi-dung" [attr.inert]="nganKeoMo() ? '' : null">Bỏ qua đến nội dung</a>
    @if (!manHinhRong()) {
      <header class="thanh-tren" [attr.inert]="nganKeoMo() ? '' : null">
        @if (!dienThoai()) {<button
          #nutMo
          type="button"
          class="nut-icon"
          data-testid="mo-sidebar"
          aria-label="Mở menu"
          aria-controls="ray"
          [attr.aria-expanded]="moNganKeo()"
          (click)="mo($event)"
        >
          <app-bieu-tuong ten="menu" />
        </button>}
        <a class="thuong-hieu" [routerLink]="trangChu()">
          <app-brand-mark />
        </a>
        @if(dienThoai()){<span class="top-page-title">{{tieuDeHien()}}</span>}@else{<app-doi-giao-dien />
        <button appButton variant="secondary" type="button" data-testid="dang-xuat" [disabled]="dangThoat()" (click)="thoat()">
          Đăng xuất
        </button>}
      </header>
      <div class="man-che" [class.mo]="moNganKeo()" aria-hidden="true" (click)="dong(true)"></div>
    }

    <aside
      id="ray"
      class="ray"
      data-testid="sidebar"
      [class.mo]="moNganKeo()"
      [attr.inert]="anRay() ? '' : null"
      [attr.role]="manHinhRong() ? null : 'dialog'"
      [attr.aria-modal]="nganKeoMo() ? 'true' : null"
      [attr.aria-label]="manHinhRong() ? null : 'Menu'"
    >
      <div class="ray-dau">
        <!-- Như v0: khối thương hiệu trên ray là chữ, không phải link (mục «Học» / «Lớp» đã dẫn về trang chủ). -->
        <div class="ray-thuong-hieu">
          <app-brand-mark size="md" />
          <span class="nhan-muc">{{ tenKhuVuc() }}</span>
        </div>
        @if (!manHinhRong()) {
          <button #nutDong type="button" class="nut-icon" data-testid="dong-sidebar" aria-label="Đóng menu" (click)="dong(true)">
            <app-bieu-tuong ten="x" />
          </button>
        }
      </div>
      @if (laQuanTri()) {
        <nav class="chuyen-khu-vuc" aria-label="Khu vực làm việc">
          <a routerLink="/gv" [attr.aria-current]="khuVuc() === 'GV' ? 'page' : null" (click)="chonMuc()">Giáo viên</a>
          @if(khuVuc() !== 'QT'){<a routerLink="/qt" data-testid="chuyen-quan-tri" [attr.aria-current]="khuVuc() === 'QT' ? 'page' : null" (click)="chonMuc()">Quản trị</a>}
        </nav>
      }
      <nav class="ray-nav" data-testid="sidebar-nav" [attr.aria-label]="nhanMenu()">
        @for (muc of cacMuc(); track muc.duongDan; let i = $index) {
          @if (muc.nhom && (i === 0 || cacMuc()[i - 1].nhom !== muc.nhom)) {
            <p class="nhom-nav">{{muc.nhom}}</p>
          }
          <a
            class="muc"
            [routerLink]="muc.duongDan"
            routerLinkActive="dang-mo"
            [routerLinkActiveOptions]="{ exact: laTrangChu(muc.duongDan) }"
            ariaCurrentWhenActive="page"
            [attr.data-testid]="muc.testId"
            (click)="chonMuc()"
          >
            <app-bieu-tuong [ten]="muc.bieuTuong" />
            <span class="nhan">{{ muc.nhan }}</span>@if(muc.duongDan==='/gv/duyet' && choDuyet.so()){<span class="badge" aria-label="Bài chờ duyệt">{{choDuyet.so()}}</span>}
          </a>
        }
      </nav>
      <div class="ray-chan">
        <span class="chu-dau" aria-hidden="true">{{ chuDau() }}</span>
        <p class="nguoi-dung" data-testid="ten-nguoi-dung">{{ phien.nguoiDung()?.displayName }}</p>
        @if(dienThoai()){<app-doi-giao-dien />}
        @if (manHinhRong() || dienThoai()) {
          <button appButton variant="secondary" block type="button" data-testid="dang-xuat" [disabled]="dangThoat()" (click)="thoat()">
            Đăng xuất
          </button>
        }
      </div>
    </aside>

    <div class="vung" [attr.inert]="nganKeoMo() ? '' : null">
      @if (manHinhRong()) {
        <div class="goc"><app-doi-giao-dien /></div>
      }
      @if (thongBaoLoi()) {
        <p class="loi" role="alert">{{ thongBaoLoi() }}</p>
      }
      <main appChuyenTrang #noiDung id="noi-dung" class="noi-dung" tabindex="-1">
        <router-outlet />
      </main>
    </div>
    @if (!manHinhRong()) {
      <nav class="bottom-nav" aria-label="Điều hướng nhanh" [attr.inert]="nganKeoMo() ? '' : null">
        @for (m of mucDuoi(); track m.duongDan) {
          <a [routerLink]="m.duongDan" routerLinkActive="dang-mo" [routerLinkActiveOptions]="{exact:laTrangChu(m.duongDan)}" ariaCurrentWhenActive="page"><app-bieu-tuong [ten]="m.bieuTuong" /><span>{{m.nhan}}</span></a>
        }
        <button type="button" [attr.data-testid]="dienThoai() ? 'mo-sidebar' : 'mo-tai-khoan'" [attr.aria-label]="dienThoai() ? 'Mở menu' : 'Mở thêm điều hướng'" aria-controls="ray" [attr.aria-expanded]="moNganKeo()" (click)="mo($event)"><app-bieu-tuong ten="menu" /><span>{{dienThoai() || khuVuc() === 'HS' ? 'Tôi' : 'Thêm'}}</span></button>
      </nav>
    }
    <p class="sr-only" aria-live="polite" data-testid="thong-bao-trang">{{ thongBaoTrang() }}</p>
  `,
  styleUrl: './khung-trang.css',})
export class KhungTrang {
  /** Khu vực của route cha (`data: { khuVuc }` trong app.routes.ts). */
  readonly khuVuc = input.required<KhuVuc>();

  protected readonly phien = inject(Phien);
  protected readonly laQuanTri = computed(() => ['ADMIN', 'SCHOOL_ADMIN'].includes(this.phien.nguoiDung()?.role ?? ''));
  protected readonly choDuyet = inject(HangChoDuyet);
  protected readonly cacMuc = computed(() => DIEU_HUONG[this.khuVuc()]);
  protected readonly mucDuoi = computed(() => this.khuVuc() === 'GV' ? this.cacMuc().filter(m=>['/gv','/gv/tien-do','/gv/giao','/gv/duyet'].includes(m.duongDan)) : this.cacMuc());
  protected readonly tenKhuVuc = computed(() => TEN_KHU_VUC[this.khuVuc()]);
  /** «Menu học sinh» / «Menu giáo viên». */
  protected readonly nhanMenu = computed(() => 'Menu ' + this.tenKhuVuc().toLocaleLowerCase('vi'));
  protected readonly trangChu = computed(() => (this.khuVuc() === 'HS' ? '/hs' : this.khuVuc() === 'GV' ? '/gv' : '/qt'));
  protected readonly laTrangChu = laTrangChuKhuVuc;
  /** Chữ đầu của tên gọi (từ cuối: «Nguyễn Văn An» → «A») cho ô tròn ở chân thanh bên. */
  protected readonly chuDau = computed(() =>
    (this.phien.nguoiDung()?.displayName.trim().split(/\s+/).at(-1) ?? '').charAt(0).toLocaleUpperCase('vi'),
  );

  protected readonly manHinhRong = signal(true);
  protected readonly dienThoai = signal(false);
  private readonly duongHien = signal('');
  protected readonly tieuDeHien = computed(() => this.duongHien().includes('/luyen/') ? 'Luyện bài' : this.cacMuc().find(m => m.duongDan === this.duongHien())?.nhan ?? this.tenKhuVuc());
  protected readonly moNganKeo = signal(false);
  /** Ngăn kéo đang mở (chỉ có ở màn hẹp): phần còn lại của trang `inert`, như sau một hộp thoại. */
  protected readonly nganKeoMo = computed(() => !this.manHinhRong() && this.moNganKeo());
  /** Ray là ngăn kéo đang đóng: ẩn khỏi bàn phím và trình đọc màn hình. */
  protected readonly anRay = computed(() => !this.manHinhRong() && !this.moNganKeo());
  protected readonly dangThoat = signal(false);
  protected readonly loi = signal('');
  private readonly loiDieuHuong = signal('');
  protected readonly thongBaoLoi = computed(() => this.loi() || this.loiDieuHuong());
  /** Tiêu đề trang mới, cho vùng `aria-live`. */
  protected readonly thongBaoTrang = signal('');

  private readonly router = inject(Router);
  private readonly injector = inject(Injector);
  private readonly document = inject(DOCUMENT);
  private readonly nutMo = viewChild<ElementRef<HTMLButtonElement>>('nutMo');
  private readonly nutDong = viewChild<ElementRef<HTMLButtonElement>>('nutDong');
  private readonly noiDung = viewChild.required<ElementRef<HTMLElement>>('noiDung');

  constructor() {
    // Phiên mất giữa chừng (đăng xuất, hoặc làm mới thất bại ở interceptor) → về trang đăng nhập.
    effect(() => {
      if (!this.phien.daDangNhap()) {
        void this.router.navigate(['/dang-nhap']);
      }
    });

    this.router.events.pipe(takeUntilDestroyed()).subscribe((e) => {
      if (e instanceof NavigationStart) {
        this.loiDieuHuong.set('');
      } else if (e instanceof NavigationEnd) {
        this.duongHien.set(e.urlAfterRedirects.split('?')[0]);
        // Chuyển trang (chọn mục, hay lùi / tiến của trình duyệt) thì đóng ngăn kéo.
        this.moNganKeo.set(false);
        // Lần nạp đầu trình đọc màn hình tự đọc tiêu đề. Router đặt tiêu đề ngay sau NavigationEnd (cùng lượt chạy),
        // nên đọc ở vi tác vụ kế.
        if (e.id > 1) queueMicrotask(() => this.thongBaoTrang.set(this.document.title));
      } else if (e instanceof NavigationError) {
        this.loiDieuHuong.set('Chưa mở được trang. Kiểm tra kết nối rồi thử lại.');
      }
    });

    // Lớp trên <html>: màn hẹp có thanh trên dính (styles.css chừa scroll-padding), ngăn kéo mở thì khóa cuộn trang.
    const html = this.document.documentElement;
    effect(() => {
      html.classList.toggle('co-thanh-tren', !this.manHinhRong());
      html.classList.toggle('khoa-cuon', this.nganKeoMo());
    });
    inject(DestroyRef).onDestroy(() => html.classList.remove('co-thanh-tren', 'khoa-cuon'));

    // jsdom (test) không có matchMedia: coi như màn rộng.
    if (typeof matchMedia === 'function') {
      const phone = matchMedia('(max-width: 600px)');
      this.dienThoai.set(phone.matches);
      const phoneChange = (e: MediaQueryListEvent) => this.dienThoai.set(e.matches);
      phone.addEventListener('change', phoneChange);
      inject(DestroyRef).onDestroy(() => phone.removeEventListener('change', phoneChange));
      const mq = matchMedia(MAN_HINH_RONG);
      this.manHinhRong.set(mq.matches);
      const doi = (e: MediaQueryListEvent) => {
        this.manHinhRong.set(e.matches);
        if (e.matches) this.moNganKeo.set(false);
      };
      mq.addEventListener('change', doi);
      inject(DestroyRef).onDestroy(() => mq.removeEventListener('change', doi));
    }
  }

  private nutDaMo: HTMLElement | null = null;

  protected mo(e?: Event): void {
    this.nutDaMo = e?.currentTarget as HTMLElement | null;
    this.moNganKeo.set(true);
    afterNextRender(() => this.nutDong()?.nativeElement.focus(), { injector: this.injector });
  }

  protected dong(traTieuDiem: boolean): void {
    if (!this.moNganKeo()) return;
    this.moNganKeo.set(false);
    if (traTieuDiem) afterNextRender(() => (this.nutDaMo ?? this.nutMo()?.nativeElement)?.focus(), { injector: this.injector });
  }

  /**
   * Chọn một mục trên ngăn kéo: đóng ngay, không chờ NavigationEnd (bấm đúng trang đang mở thì Router bỏ qua, không có
   * NavigationEnd; chunk lười tải chậm thì ngăn kéo đứng yên). Mục vừa bấm thành `inert`, nên đưa tiêu điểm vào nội dung.
   */
  protected chonMuc(): void {
    if (!this.nganKeoMo()) return;
    this.moNganKeo.set(false);
    afterNextRender(() => this.noiDung().nativeElement.focus({ preventScroll: true }), { injector: this.injector });
  }

  protected async thoat(): Promise<void> {
    this.dangThoat.set(true);
    this.loi.set('');
    try {
      await this.phien.dangXuat();
    } catch {
      this.loi.set('Chưa đăng xuất được. Kiểm tra kết nối rồi thử lại.');
    } finally {
      this.dangThoat.set(false);
    }
  }
}
