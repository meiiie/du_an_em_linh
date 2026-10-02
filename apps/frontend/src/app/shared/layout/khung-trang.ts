import { Component, effect, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Phien } from '../../core/auth/phien';
import { TEN_SAN_PHAM } from '../../core/san-pham';
import { BrandMark } from '../ui/brand-mark';
import { Button } from '../ui/button';

/** Khung trang sau đăng nhập: thanh trên (dấu sản phẩm, tên người dùng, Đăng xuất) và vùng nội dung. */
@Component({
  selector: 'app-khung-trang',
  imports: [BrandMark, Button],
  template: `
    <a class="skip-link" href="#noi-dung">Bỏ qua đến nội dung</a>
    <header class="thanh-tren">
      <span class="thuong-hieu">
        <app-brand-mark />
        <span class="ten-san-pham">{{ tenSanPham }}</span>
      </span>
      <span class="nguoi-dung" data-testid="ten-nguoi-dung">{{ phien.nguoiDung()?.displayName }}</span>
      <button appButton variant="secondary" type="button" data-testid="dang-xuat" [disabled]="dangThoat()" (click)="thoat()">
        Đăng xuất
      </button>
    </header>
    @if (loi()) {
      <p class="loi" role="alert">{{ loi() }}</p>
    }
    <main id="noi-dung" class="noi-dung">
      <ng-content />
    </main>
  `,
  styles: `
    :host {
      display: block;
      min-height: 100dvh;
    }

    .thanh-tren {
      display: flex;
      align-items: center;
      gap: var(--space-3);
      min-height: 56px;
      padding: var(--space-2) var(--space-4);
      border-bottom: 1px solid var(--line);
    }

    .thuong-hieu {
      display: inline-flex;
      align-items: center;
      gap: var(--space-2);
      font-weight: 600;
    }

    .nguoi-dung {
      margin-left: auto;
      min-width: 0;
      overflow: hidden;
      color: var(--muted);
      font-size: 14px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .loi {
      margin: 0;
      padding: var(--space-3) var(--space-4);
      background: var(--wash);
      color: var(--mark);
      font-size: 14px;
    }

    .noi-dung {
      max-width: 72rem;
      margin: 0 auto;
      padding: var(--space-5) var(--space-4) var(--space-8);
    }

    @media (max-width: 479px) {
      .ten-san-pham {
        position: absolute;
        width: 1px;
        height: 1px;
        overflow: hidden;
        clip-path: inset(50%);
        white-space: nowrap;
      }
    }

    @media (min-width: 640px) {
      .noi-dung {
        padding-inline: var(--space-6);
      }

      /* Thanh trên trải hết bề ngang nhưng chữ thẳng hàng với cột nội dung (72rem, đệm 32). */
      .thanh-tren {
        padding-inline: max(var(--space-6), calc((100% - 72rem) / 2 + var(--space-6)));
      }
    }
  `,
})
export class KhungTrang {
  protected readonly phien = inject(Phien);
  protected readonly tenSanPham = TEN_SAN_PHAM;
  protected readonly dangThoat = signal(false);
  protected readonly loi = signal('');
  private readonly router = inject(Router);

  constructor() {
    // Phiên mất giữa chừng (đăng xuất, hoặc làm mới thất bại ở interceptor) → về trang đăng nhập.
    effect(() => {
      if (!this.phien.daDangNhap()) {
        void this.router.navigate(['/dang-nhap']);
      }
    });
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
