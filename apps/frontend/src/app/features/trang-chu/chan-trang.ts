import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BrandMark } from '../../shared/ui/brand-mark';
import { DoiGiaoDien } from '../../shared/ui/doi-giao-dien';

/** Chân trang công khai theo trang Wiii: chữ hiệu, câu giới thiệu, nút đổi giao diện, các cột liên kết, dòng giới hạn. */
@Component({
  selector: 'app-chan-trang',
  imports: [RouterLink, BrandMark, DoiGiaoDien],
  template: `
    <footer class="chan">
      <div class="khung chan-tren">
        <div class="chan-hieu">
          <span class="thuong-hieu"><img src="/icon.svg" width="40" height="40" alt="" /><app-brand-mark size="md" /></span>
          <p>Học toán từng bước, với gia sư không đưa đáp án.</p>
          <app-doi-giao-dien />
        </div>
        <nav class="cot" aria-label="Chân trang">
          <div>
            <p>Học</p>
            <a href="#xem">Cách học</a>
            <a href="#nam-buoc">Năm bước</a>
            <a routerLink="/dang-nhap">Vào học</a>
          </div>
          <div>
            <p>Giáo viên</p>
            <a href="#chon">Chuẩn bị lớp</a>
            <a routerLink="/dang-nhap">Vào lớp</a>
          </div>
          <div>
            <p>Dự án</p>
            <a href="https://github.com/meiiie/du_an_em_linh">GitHub</a>
            <a href="https://github.com/meiiie/du_an_em_linh/blob/main/LICENSE">Giấy phép MIT</a>
          </div>
        </nav>
      </div>
      <div class="khung chan-duoi">
        <p>Các hình trên trang minh họa cách học, không phải bài làm của học sinh.</p>
        <p>© MathL+ · Mã nguồn mở theo giấy phép MIT</p>
      </div>
    </footer>
  `,
  styles: `
    :host {
      display: block;
    }
    .thuong-hieu {
      display: inline-flex;
      align-items: center;
      gap: 10px;
    }
    .khung {
      max-width: 74rem;
      margin: 0 auto;
      padding-inline: max(var(--space-4), env(safe-area-inset-left)) max(var(--space-4), env(safe-area-inset-right));
    }
    .cot a {
      display: inline-flex;
      align-items: center;
      min-height: var(--target);
      color: var(--ink-2);
      text-decoration: none;
    }
    .cot a:hover {
      color: var(--ink);
      text-decoration: underline;
    }
    .chan {
      border-top: 1px solid var(--line);
      background: var(--tc-page);
    }
    .chan-tren {
      display: grid;
      gap: 40px;
      padding-block: 48px 40px;
    }
    .chan-hieu p {
      max-width: 18rem;
      margin: 16px 0 20px;
      color: var(--muted);
      font-size: 15px;
    }
    .cot {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(9rem, 1fr));
      gap: 24px;
      font-size: 14px;
    }
    .cot div {
      display: grid;
      align-content: start;
      gap: 4px;
    }
    .cot p {
      margin: 0 0 8px;
      font-weight: 600;
    }
    .chan-duoi {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      padding-block: 20px calc(24px + env(safe-area-inset-bottom));
      border-top: 1px solid var(--line);
      color: var(--muted);
      font-size: 13px;
    }
    .chan-duoi p {
      max-width: 36rem;
      margin: 0;
    }
    @media (min-width: 40rem) {
      .khung {
        padding-inline: max(var(--space-6), env(safe-area-inset-left)) max(var(--space-6), env(safe-area-inset-right));
      }
    }
    @media (min-width: 64rem) {
      .chan-tren {
        grid-template-columns: 18rem 1fr;
      }
    }
  `,
})
export class ChanTrang {}
