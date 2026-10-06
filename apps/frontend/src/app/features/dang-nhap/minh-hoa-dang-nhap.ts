import { Component } from '@angular/core';

/**
 * Nửa phải trang đăng nhập (màn rộng): tấm bảng tối 3b1b với nền đường cong và ảnh học sinh vẽ riêng
 * (labs/design/prototypes/2026-10-06-trang-chu-anh), cùng câu của trang công khai. Trang trí, ẩn với trình đọc màn hình;
 * không công thức nào của ngân hàng bài (SP-10).
 */
@Component({
  selector: 'app-minh-hoa-dang-nhap',
  host: { 'aria-hidden': 'true' },
  template: `
    <div class="giua">
      <img src="/anh/hoc-sinh-1200.webp" width="1200" height="900" alt="" decoding="async" />
      <p class="cau">Học toán theo từng bước.<br />Tự mình <span class="gach">hiểu ra</span>.</p>
      <p class="nho">Phiếu năm bước · Máy kiểm từng bước · Gia sư không đưa đáp án</p>
    </div>
  `,
  styles: `
    :host {
      display: grid;
      place-items: center;
      min-height: 100dvh;
      padding: var(--space-8);
      background: var(--board) url('/anh/hero-nen-toi-1280.webp') center / cover no-repeat;
      color: var(--board-ink);
      color-scheme: dark;
    }
    .giua {
      width: min(100%, 34rem);
    }
    img {
      display: block;
      width: 100%;
      height: auto;
      border-radius: 16px;
      box-shadow: 0 24px 60px rgb(0 0 0 / 0.45);
    }
    .cau {
      margin: var(--space-6) 0 0;
      font-size: 32px;
      font-weight: 400;
      line-height: 1.2;
      letter-spacing: -0.02em;
    }
    .gach {
      padding-bottom: 3px;
      background: linear-gradient(90deg, var(--m-blue), var(--m-teal), var(--m-yellow)) no-repeat left bottom / 100% 3px;
    }
    .nho {
      margin: var(--space-3) 0 0;
      color: color-mix(in srgb, var(--board-ink) 70%, var(--board));
      font-size: 15px;
    }
  `,
})
export class MinhHoaDangNhap {}
