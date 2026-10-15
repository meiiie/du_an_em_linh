import { Component } from '@angular/core';

/**
 * Nửa phải trang đăng nhập (màn rộng): minh họa toán phẳng trên nền xanh nhạt,
 * cùng câu của trang công khai. Trang trí, ẩn với trình đọc màn hình;
 * không công thức nào của ngân hàng bài (SP-10).
 */
@Component({
  selector: 'app-minh-hoa-dang-nhap',
  host: { 'aria-hidden': 'true' },
  template: `
    <div class="giua">
      <svg class="minh-hoa" viewBox="0 0 480 320" fill="none">
        <rect x="16" y="16" width="448" height="288" rx="12" class="giay" />
        <g class="luoi">
          <path d="M64 64H416M64 112H416M64 160H416M64 208H416M64 256H416" />
          <path d="M112 64V256M176 64V256M240 64V256M304 64V256M368 64V256" />
        </g>
        <path d="M64 224H416M240 48V272" class="truc" />
        <path d="M96 80Q240 352 384 80" class="duong" />
        <circle cx="240" cy="216" r="6" class="diem" />
        <g class="chu">
          <text x="400" y="248">x</text><text x="256" y="64">y</text>
          <text x="40" y="48">Học từng bước, hiểu từng ý</text>
        </g>
      </svg>
      <p class="cau">Học toán theo từng bước.<br />Tự mình <span class="gach">hiểu ra</span>.</p>
      <p class="nho">Toán THPT lớp 10–12 · Gia sư AI gợi ý theo bước, không đưa đáp án</p>
    </div>
  `,
  styles: `
    :host {
      display: grid;
      place-items: center;
      min-height: 100dvh;
      padding: var(--space-8);
      background: var(--wash);
      color: var(--ink);
    }
    .giua {
      width: min(100%, 34rem);
    }
    .minh-hoa {
      display: block;
      width: 100%;
      height: auto;
    }
    .giay { fill: var(--raise); stroke: var(--line); }
    .luoi { stroke: var(--line); }
    .truc { stroke: var(--line-strong); stroke-width: 1.5; }
    .duong { stroke: var(--accent); stroke-width: 4; stroke-linecap: round; }
    .diem { fill: var(--accent); }
    .chu { fill: var(--ink-2); font: 14px var(--font-sans); }
    .cau {
      color: var(--ink);
      margin: var(--space-6) 0 0;
      font-size: 32px;
      font-weight: 600;
      line-height: 1.2;
      letter-spacing: -0.02em;
    }
    .gach {
      color: var(--accent);
    }
    .nho {
      margin: var(--space-3) 0 0;
      color: var(--muted);
      font-size: 15px;
      line-height: 24px;
    }
  `,
})
export class MinhHoaDangNhap {}
