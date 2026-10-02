import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { Phien } from './phien';

/**
 * Gắn access token cho lời gọi `/api/` cùng gốc (trừ `/api/auth/`, vốn dựa vào cookie). Gặp 401 thì làm mới phiên một
 * lần rồi gửi lại; làm mới thất bại thì trả lỗi gốc, phiên đã bị xóa nên khung trang đưa về đăng nhập.
 */
export const xacThucInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api/') || req.url.startsWith('/api/auth/')) {
    return next(req);
  }
  const phien = inject(Phien);
  const ganToken = (r: HttpRequest<unknown>) => {
    const token = phien.tokenTruyCap();
    return token ? r.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : r;
  };
  return next(ganToken(req)).pipe(
    catchError((loi: unknown) => {
      if (!(loi instanceof HttpErrorResponse) || loi.status !== 401) {
        return throwError(() => loi);
      }
      return from(phien.lamMoi()).pipe(switchMap((ok) => (ok ? next(ganToken(req)) : throwError(() => loi))));
    }),
  );
};
