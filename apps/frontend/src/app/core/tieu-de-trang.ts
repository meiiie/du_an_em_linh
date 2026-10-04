import { inject, Injectable } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';
import { ActivatedRouteSnapshot, RouterStateSnapshot, TitleStrategy } from '@angular/router';
import { TEN_SAN_PHAM } from './san-pham';

/**
 * Mỗi lần đổi trang: tab trình duyệt `<tên trang> · MathL+` (docs/DESIGN.md) và thẻ `robots` lấy từ
 * `data.robots` của route (đăng nhập: `noindex, nofollow` như v0). Route không khai thì gỡ thẻ, để trang công khai
 * sau này được lập chỉ mục.
 */
@Injectable({ providedIn: 'root' })
export class TieuDeTrang extends TitleStrategy {
  private readonly title = inject(Title);
  private readonly meta = inject(Meta);

  override updateTitle(snapshot: RouterStateSnapshot): void {
    const trang = this.buildTitle(snapshot);
    this.title.setTitle(trang ? `${trang} · ${TEN_SAN_PHAM}` : TEN_SAN_PHAM);
    const robots = robotsCuaTrang(snapshot.root);
    if (robots) {
      this.meta.updateTag({ name: 'robots', content: robots });
    } else {
      this.meta.removeTag("name='robots'");
    }
  }
}

/** `data.robots` của route sâu nhất có khai. */
function robotsCuaTrang(route: ActivatedRouteSnapshot): string | undefined {
  let ketQua: string | undefined;
  for (let r: ActivatedRouteSnapshot | null = route; r; r = r.firstChild) {
    ketQua = (r.data['robots'] as string | undefined) ?? ketQua;
  }
  return ketQua;
}
