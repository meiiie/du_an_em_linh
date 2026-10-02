import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { VaiTro } from '../../api/auth';
import { Phien, trangChuCua } from './phien';

/**
 * Chỉ cho các vai trò được liệt kê vào route. Chưa có phiên thì thử khôi phục từ cookie; không được thì về
 * `/dang-nhap?returnUrl=…`. Sai vai trò thì về trang chủ của vai trò mình. Mẫu từ
 * `LMS_hohulili@34c3f0f2:fe/src/app/core/guards/role.guard.ts` (MIT), viết lại cho phiên dạng signal.
 */
export function chiVaiTro(...choPhep: VaiTro[]): CanActivateFn {
  return async (_route, state) => {
    const phien = inject(Phien);
    const router = inject(Router);
    if (!phien.daDangNhap()) {
      await phien.lamMoi();
    }
    const nguoiDung = phien.nguoiDung();
    if (!nguoiDung) {
      return router.createUrlTree(['/dang-nhap'], { queryParams: { returnUrl: state.url } });
    }
    return choPhep.includes(nguoiDung.role) ? true : router.createUrlTree([trangChuCua(nguoiDung.role)]);
  };
}

/** Trang đăng nhập: đã có phiên, kể cả vừa khôi phục từ cookie, thì vào thẳng trang chủ của mình. */
export const chuaDangNhap: CanActivateFn = async () => {
  const phien = inject(Phien);
  const router = inject(Router);
  if (!phien.daDangNhap()) {
    await phien.lamMoi();
  }
  const nguoiDung = phien.nguoiDung();
  return nguoiDung ? router.createUrlTree([trangChuCua(nguoiDung.role)]) : true;
};
