import { Component, computed, input } from '@angular/core';

export type TenBieuTuong =
  | 'home'
  | 'book'
  | 'calendar'
  | 'inbox'
  | 'bank'
  | 'file'
  | 'sigma'
  | 'chart'
  | 'settings'
  | 'kho'
  | 'plug'
  | 'menu'
  | 'x';

/** Một nét SVG; trường không dùng của mỗi loại để trống (template không cần thu hẹp kiểu). */
interface Net {
  readonly kieu: 'path' | 'rect' | 'circle' | 'polyline';
  readonly d?: string;
  readonly x?: number;
  readonly y?: number;
  readonly w?: number;
  readonly h?: number;
  readonly rx?: number;
  readonly cx?: number;
  readonly cy?: number;
  readonly r?: number;
  readonly points?: string;
}

const p = (d: string): Net => ({ kieu: 'path', d });

/**
 * Nét vẽ chép nguyên văn từ Lucide 0.544.0 (ISC, https://lucide.dev), đúng bộ biểu tượng ray của v0
 * (`apps/web/components/app-shell.tsx`): House, BookOpen, CalendarDays, Inbox, Library, FileText, Sigma,
 * LayoutDashboard, Settings, BookMarked, Plug, Menu, X. Chép thẳng để khỏi thêm thư viện cho mười ba hình.
 */
const NET: Record<TenBieuTuong, readonly Net[]> = {
  home: [
    p('M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8'),
    p('M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z'),
  ],
  book: [
    p('M12 7v14'),
    p('M3 18a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h5a4 4 0 0 1 4 4 4 4 0 0 1 4-4h5a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1h-6a3 3 0 0 0-3 3 3 3 0 0 0-3-3z'),
  ],
  calendar: [
    p('M8 2v4'),
    p('M16 2v4'),
    { kieu: 'rect', x: 3, y: 4, w: 18, h: 18, rx: 2 },
    p('M3 10h18'),
    p('M8 14h.01'),
    p('M12 14h.01'),
    p('M16 14h.01'),
    p('M8 18h.01'),
    p('M12 18h.01'),
    p('M16 18h.01'),
  ],
  inbox: [
    { kieu: 'polyline', points: '22 12 16 12 14 15 10 15 8 12 2 12' },
    p('M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z'),
  ],
  bank: [p('m16 6 4 14'), p('M12 6v14'), p('M8 8v12'), p('M4 4v16')],
  file: [
    p('M15 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7Z'),
    p('M14 2v4a2 2 0 0 0 2 2h4'),
    p('M10 9H8'),
    p('M16 13H8'),
    p('M16 17H8'),
  ],
  sigma: [p('M18 7V5a1 1 0 0 0-1-1H6.5a.5.5 0 0 0-.4.8l4.5 6a2 2 0 0 1 0 2.4l-4.5 6a.5.5 0 0 0 .4.8H17a1 1 0 0 0 1-1v-2')],
  chart: [
    { kieu: 'rect', x: 3, y: 3, w: 7, h: 9, rx: 1 },
    { kieu: 'rect', x: 14, y: 3, w: 7, h: 5, rx: 1 },
    { kieu: 'rect', x: 14, y: 12, w: 7, h: 9, rx: 1 },
    { kieu: 'rect', x: 3, y: 16, w: 7, h: 5, rx: 1 },
  ],
  settings: [
    p(
      'M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 ' +
        '3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 ' +
        '2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915',
    ),
    { kieu: 'circle', cx: 12, cy: 12, r: 3 },
  ],
  kho: [p('M10 2v8l3-3 3 3V2'), p('M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H19a1 1 0 0 1 1 1v18a1 1 0 0 1-1 1H6.5a1 1 0 0 1 0-5H20')],
  plug: [p('M12 22v-5'), p('M9 8V2'), p('M15 8V2'), p('M18 8v5a4 4 0 0 1-4 4h-4a4 4 0 0 1-4-4V8Z')],
  menu: [p('M4 5h16'), p('M4 12h16'), p('M4 19h16')],
  x: [p('M18 6 6 18'), p('m6 6 12 12')],
};

/** Biểu tượng nét 24×24, màu theo chữ (`currentColor`), chỉ để trang trí: luôn đi kèm chữ hay `aria-label` của nút. */
@Component({
  selector: 'app-bieu-tuong',
  host: { 'aria-hidden': 'true', class: 'bieu-tuong' },
  template: `
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      @for (n of net(); track $index) {
        @switch (n.kieu) {
          @case ('path') { <path [attr.d]="n.d" /> }
          @case ('rect') { <rect [attr.x]="n.x" [attr.y]="n.y" [attr.width]="n.w" [attr.height]="n.h" [attr.rx]="n.rx" /> }
          @case ('circle') { <circle [attr.cx]="n.cx" [attr.cy]="n.cy" [attr.r]="n.r" /> }
          @case ('polyline') { <polyline [attr.points]="n.points" /> }
        }
      }
    </svg>
  `,
  styles: `
    :host {
      display: inline-flex;
      flex-shrink: 0;
      width: 1rem;
      height: 1rem;
    }

    svg {
      width: 100%;
      height: 100%;
    }
  `,
})
export class BieuTuong {
  readonly ten = input.required<TenBieuTuong>();
  protected readonly net = computed(() => NET[this.ten()]);
}
