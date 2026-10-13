import { DestroyRef, Injectable, inject, signal } from '@angular/core';

/** Cùng mốc với CSS thẻ di động. Theo dõi đổi kích thước, gỡ listener khi hủy. */
@Injectable({ providedIn: 'root' })
export class KichThuocTrang {
  private readonly manHep = typeof window !== 'undefined' && typeof window.matchMedia === 'function'
    ? window.matchMedia('(max-width: 600px)') : undefined;
  readonly soMuc = signal(this.manHep?.matches ? 10 : 20);

  constructor() {
    const doi = (e: MediaQueryListEvent) => this.soMuc.set(e.matches ? 10 : 20);
    this.manHep?.addEventListener('change', doi);
    inject(DestroyRef).onDestroy(() => this.manHep?.removeEventListener('change', doi));
  }
}
