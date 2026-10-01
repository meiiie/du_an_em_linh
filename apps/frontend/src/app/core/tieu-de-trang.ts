import { inject, Injectable } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { TEN_SAN_PHAM } from './san-pham';

/** Tab trình duyệt: `<tên trang> · Học toán với AI` (docs/DESIGN.md). */
@Injectable({ providedIn: 'root' })
export class TieuDeTrang extends TitleStrategy {
  private readonly title = inject(Title);

  override updateTitle(snapshot: RouterStateSnapshot): void {
    const trang = this.buildTitle(snapshot);
    this.title.setTitle(trang ? `${trang} · ${TEN_SAN_PHAM}` : TEN_SAN_PHAM);
  }
}
