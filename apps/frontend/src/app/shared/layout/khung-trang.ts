import {
  afterNextRender,
  Component,
  computed,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  Injector,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { Phien } from '../../core/auth/phien';
import { TEN_SAN_PHAM } from '../../core/san-pham';
import { BieuTuong } from '../ui/bieu-tuong';
import { BrandMark } from '../ui/brand-mark';
import { Button } from '../ui/button';
import { DIEU_HUONG, KhuVuc, laTrangChuKhuVuc, TEN_KHU_VUC } from './dieu-huong';

/** Cỡ `lg` của docs/DESIGN.md: từ đây ray mực luôn hiện, dưới đây là ngăn kéo. */
const MAN_HINH_RONG = '(min-width: 64rem)';

/**
 * Khung sau đăng nhập của `/hs` và `/gv` (route cha, các trang con hiện trong `<router-outlet>`), như `AppShell` của
 * v0 (`apps/web/components/app-shell.tsx`) và bố cục ở docs/DESIGN.md. Desktop: ray mực 220 px, không có thanh trên.
 * Điện thoại và máy tính bảng: thanh trên với `mo-sidebar`, ray thành ngăn kéo có `dong-sidebar`. Ngăn kéo đóng thì
 * `inert` (không Tab vào được); Esc, chạm nền hay chọn một mục thì đóng và trả tiêu điểm về nút mở. Nút Đăng xuất chỉ
 * có một trong DOM: ở thanh trên khi màn hẹp, ở chân ray khi màn rộng. Phiên mất thì về `/dang-nhap`.
 */
@Component({
  selector: 'app-khung-trang',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, BieuTuong, BrandMark, Button],
  host: { '(document:keydown.escape)': 'dong(true)' },
  template: `
    <a class="skip-link" href="#noi-dung">Bỏ qua đến nội dung</a>
    @if (!manHinhRong()) {
      <header class="thanh-tren">
        <button
          #nutMo
          type="button"
          class="nut-vuong"
          data-testid="mo-sidebar"
          aria-label="Mở menu"
          aria-controls="ray"
          [attr.aria-expanded]="moNganKeo()"
          (click)="mo()"
        >
          <app-bieu-tuong ten="menu" />
        </button>
        <a class="thuong-hieu" [routerLink]="trangChu()">
          <app-brand-mark />
          <span class="ten-san-pham">{{ tenSanPham }}</span>
        </a>
        <button appButton variant="secondary" type="button" data-testid="dang-xuat" [disabled]="dangThoat()" (click)="thoat()">
          Đăng xuất
        </button>
      </header>
      @if (moNganKeo()) {
        <div class="man-che" aria-hidden="true" (click)="dong(true)"></div>
      }
    }

    <aside
      id="ray"
      class="ray"
      data-testid="sidebar"
      [class.mo]="moNganKeo()"
      [attr.inert]="anRay() ? '' : null"
      [attr.aria-label]="'Điều hướng ' + tenKhuVuc()"
    >
      @if (manHinhRong()) {
        <a class="ray-thuong-hieu" [routerLink]="trangChu()">
          <app-brand-mark dao />
          <span class="ray-ten">
            <span class="ten-san-pham-ray">{{ tenSanPham }}</span>
            <span class="khu-vuc">{{ tenKhuVuc() }}</span>
          </span>
        </a>
      } @else {
        <div class="ray-dau">
          <button #nutDong type="button" class="nut-vuong nut-toi" data-testid="dong-sidebar" aria-label="Đóng menu" (click)="dong(true)">
            <app-bieu-tuong ten="x" />
          </button>
        </div>
      }
      <nav class="ray-nav" data-testid="sidebar-nav" aria-label="Mục chính">
        @for (muc of cacMuc(); track muc.duongDan) {
          <a
            class="muc"
            [routerLink]="muc.duongDan"
            routerLinkActive="dang-mo"
            [routerLinkActiveOptions]="{ exact: laTrangChu(muc.duongDan) }"
            ariaCurrentWhenActive="page"
            [attr.data-testid]="muc.testId"
          >
            <app-bieu-tuong [ten]="muc.bieuTuong" />
            <span class="nhan">{{ muc.nhan }}</span>
          </a>
        }
      </nav>
      <div class="ray-chan">
        <p class="nguoi-dung" data-testid="ten-nguoi-dung">{{ phien.nguoiDung()?.displayName }}</p>
        @if (manHinhRong()) {
          <button type="button" class="ray-thoat" data-testid="dang-xuat" [disabled]="dangThoat()" (click)="thoat()">Đăng xuất</button>
        }
      </div>
    </aside>

    <div class="vung">
      @if (loi()) {
        <p class="loi" role="alert">{{ loi() }}</p>
      }
      <main id="noi-dung" class="noi-dung" tabindex="-1">
        <router-outlet />
      </main>
    </div>
  `,
  styles: `
    :host {
      display: block;
      min-height: 100dvh;
    }

    .thanh-tren {
      position: sticky;
      top: 0;
      z-index: 30;
      display: flex;
      align-items: center;
      gap: var(--space-2);
      min-height: 48px;
      padding: env(safe-area-inset-top) var(--space-2) 0;
      border-bottom: 1px solid var(--line);
      background: var(--canvas);
    }

    .thanh-tren [appButton] {
      margin-left: auto;
    }

    .nut-vuong {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      inline-size: var(--target);
      block-size: var(--target);
      padding: 0;
      border: 0;
      border-radius: var(--radius);
      background: none;
      color: inherit;
      cursor: pointer;
    }

    .nut-vuong:hover {
      background: var(--wash);
    }

    .nut-vuong app-bieu-tuong {
      inline-size: 20px;
      block-size: 20px;
    }

    .thuong-hieu {
      display: inline-flex;
      align-items: center;
      gap: var(--space-2);
      min-width: 0;
      min-height: var(--target);
      color: inherit;
      font-size: 14px;
      font-weight: 500;
      text-decoration: none;
    }

    .ten-san-pham {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .man-che {
      position: fixed;
      inset: 0;
      z-index: 30;
      background: rgb(23 24 28 / 0.4);
    }

    .ray {
      position: fixed;
      top: 48px;
      bottom: 0;
      left: 0;
      z-index: 40;
      display: flex;
      flex-direction: column;
      inline-size: 220px;
      background: var(--ink);
      color: var(--chalk);
      overscroll-behavior: contain;
      transform: translateX(-100%);
      visibility: hidden;
      /* Đóng: trượt ra xong mới ẩn (visibility trễ 200 ms). */
      transition:
        transform 200ms,
        visibility 0s 200ms;
    }

    .ray.mo {
      top: 0;
      transform: none;
      visibility: visible;
      /* Mở: hiện ngay, không chuyển tiếp visibility. Nếu chuyển tiếp, khung đầu vẫn hidden và focus() vào nút đóng
         (afterNextRender) bị trình duyệt bỏ qua. */
      transition: transform 200ms;
    }

    .ray :focus-visible {
      outline-color: var(--chalk);
    }

    .ray-dau {
      display: flex;
      justify-content: flex-end;
      min-height: 48px;
      padding: 0 var(--space-2);
    }

    .nut-toi {
      color: rgb(244 244 245 / 0.6);
    }

    .nut-toi:hover {
      background: rgb(255 255 255 / 0.05);
      color: var(--chalk);
    }

    .ray-thuong-hieu {
      display: flex;
      align-items: center;
      gap: var(--space-2);
      padding: var(--space-5) var(--space-4) var(--space-4);
      color: inherit;
      text-decoration: none;
    }

    .ray-ten {
      display: grid;
      min-width: 0;
    }

    .ten-san-pham-ray,
    .khu-vuc {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .ten-san-pham-ray {
      font-size: 14px;
      font-weight: 500;
    }

    .khu-vuc {
      color: rgb(244 244 245 / 0.55);
      font-size: 12px;
    }

    .ray-nav {
      display: flex;
      flex: 1;
      flex-direction: column;
      gap: var(--space-1);
      overflow-y: auto;
      padding: var(--space-4) var(--space-2) var(--space-3);
    }

    .muc {
      display: flex;
      align-items: center;
      gap: var(--space-3);
      min-height: var(--target);
      padding: 0 var(--space-3);
      border-radius: var(--radius);
      color: rgb(244 244 245 / 0.7);
      font-size: 14px;
      text-decoration: none;
      transition: background-color 150ms;
    }

    .muc:hover {
      background: rgb(255 255 255 / 0.05);
      color: var(--chalk);
    }

    .muc.dang-mo {
      background: rgb(255 255 255 / 0.1);
      color: var(--chalk);
      font-weight: 500;
    }

    .nhan {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .ray-chan {
      margin-top: auto;
      padding: var(--space-4);
      border-top: 1px solid rgb(255 255 255 / 0.1);
    }

    .nguoi-dung {
      margin: 0;
      overflow: hidden;
      font-size: 14px;
      font-weight: 500;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .ray-thoat {
      display: inline-flex;
      align-items: center;
      min-height: var(--target);
      margin-top: var(--space-2);
      padding: 0;
      border: 0;
      background: none;
      color: rgb(244 244 245 / 0.7);
      font: inherit;
      font-size: 14px;
      cursor: pointer;
    }

    .ray-thoat:hover:not(:disabled) {
      color: var(--chalk);
    }

    .ray-thoat:disabled {
      cursor: not-allowed;
      opacity: 0.6;
    }

    .loi {
      margin: 0;
      padding: var(--space-3) var(--space-4);
      background: var(--wash);
      color: var(--mark);
      font-size: 14px;
    }

    .noi-dung {
      max-width: 64rem;
      margin: 0 auto;
      padding: var(--space-5) var(--space-4) var(--space-8);
      outline: none;
    }

    @media (min-width: 40rem) {
      .noi-dung {
        padding-inline: var(--space-5);
      }
    }

    @media (min-width: 64rem) {
      .ray {
        top: 0;
        transform: none;
        visibility: visible;
        transition: none;
      }

      .vung {
        padding-left: 220px;
      }

      .noi-dung {
        padding: var(--space-6) var(--space-6) var(--space-8);
      }
    }
  `,
})
export class KhungTrang {
  /** Khu vực của route cha (`data: { khuVuc }` trong app.routes.ts). */
  readonly khuVuc = input.required<KhuVuc>();

  protected readonly phien = inject(Phien);
  protected readonly tenSanPham = TEN_SAN_PHAM;
  protected readonly cacMuc = computed(() => DIEU_HUONG[this.khuVuc()]);
  protected readonly tenKhuVuc = computed(() => TEN_KHU_VUC[this.khuVuc()]);
  protected readonly trangChu = computed(() => (this.khuVuc() === 'HS' ? '/hs' : '/gv'));
  protected readonly laTrangChu = laTrangChuKhuVuc;

  protected readonly manHinhRong = signal(true);
  protected readonly moNganKeo = signal(false);
  /** Ray là ngăn kéo đang đóng: ẩn khỏi bàn phím và trình đọc màn hình. */
  protected readonly anRay = computed(() => !this.manHinhRong() && !this.moNganKeo());
  protected readonly dangThoat = signal(false);
  protected readonly loi = signal('');

  private readonly router = inject(Router);
  private readonly injector = inject(Injector);
  private readonly nutMo = viewChild<ElementRef<HTMLButtonElement>>('nutMo');
  private readonly nutDong = viewChild<ElementRef<HTMLButtonElement>>('nutDong');

  constructor() {
    // Phiên mất giữa chừng (đăng xuất, hoặc làm mới thất bại ở interceptor) → về trang đăng nhập.
    effect(() => {
      if (!this.phien.daDangNhap()) {
        void this.router.navigate(['/dang-nhap']);
      }
    });

    // Chuyển trang (chọn mục, hay lùi / tiến của trình duyệt) thì đóng ngăn kéo.
    this.router.events
      .pipe(
        filter((e) => e instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.moNganKeo.set(false));

    // jsdom (test) không có matchMedia: coi như màn rộng.
    if (typeof matchMedia === 'function') {
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

  protected mo(): void {
    this.moNganKeo.set(true);
    afterNextRender(() => this.nutDong()?.nativeElement.focus(), { injector: this.injector });
  }

  protected dong(traTieuDiem: boolean): void {
    if (!this.moNganKeo()) return;
    this.moNganKeo.set(false);
    if (traTieuDiem) afterNextRender(() => this.nutMo()?.nativeElement.focus(), { injector: this.injector });
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
