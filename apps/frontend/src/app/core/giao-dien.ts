import { computed, DestroyRef, DOCUMENT, effect, inject, Injectable, signal } from '@angular/core';

export type CheDo = 'light' | 'dark';

/** Khóa `localStorage` của lựa chọn; script đầu `index.html` đọc cùng khóa để đặt chế độ trước lần vẽ đầu. */
export const KHOA_GIAO_DIEN = 'mathl-giao-dien';

const TOI = '(prefers-color-scheme: dark)';

/**
 * Chế độ sáng / tối của cả ứng dụng. Chưa chọn thì theo máy (`prefers-color-scheme`, đổi theo khi máy đổi); bấm nút
 * thì nhớ lựa chọn trong `localStorage` (sở thích hiển thị, không phải dữ liệu phiên). Chế độ đang dùng nằm ở
 * `data-theme` của <html>, nơi `styles.css` đổi token.
 */
@Injectable({ providedIn: 'root' })
export class GiaoDien {
  private readonly daChon = signal<CheDo | null>(docLuaChon());
  private readonly cuaMay = signal<CheDo>('light');
  readonly cheDo = computed(() => this.daChon() ?? this.cuaMay());

  constructor() {
    // jsdom (test) không có matchMedia: coi như máy sáng.
    if (typeof matchMedia === 'function') {
      const mq = matchMedia(TOI);
      this.cuaMay.set(mq.matches ? 'dark' : 'light');
      const doi = (e: MediaQueryListEvent) => this.cuaMay.set(e.matches ? 'dark' : 'light');
      mq.addEventListener('change', doi);
      inject(DestroyRef).onDestroy(() => mq.removeEventListener('change', doi));
    }
    const html = inject(DOCUMENT).documentElement;
    effect(() => html.setAttribute('data-theme', this.cheDo()));
  }

  doi(): void {
    const moi: CheDo = this.cheDo() === 'dark' ? 'light' : 'dark';
    this.daChon.set(moi);
    try {
      localStorage.setItem(KHOA_GIAO_DIEN, moi);
    } catch {
      // Trình duyệt chặn bộ nhớ (chế độ riêng tư, cookie bị tắt): vẫn đổi, chỉ không nhớ sang lần sau.
    }
  }
}

function docLuaChon(): CheDo | null {
  try {
    const v = localStorage.getItem(KHOA_GIAO_DIEN);
    return v === 'light' || v === 'dark' ? v : null;
  } catch {
    return null;
  }
}
