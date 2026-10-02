// Kiểu khớp DTO của services/core (module identity). Đổi DTO bên core thì sửa ở đây.

export type VaiTro = 'ADMIN' | 'SCHOOL_ADMIN' | 'TEACHER' | 'STUDENT';

/** `UserDto`. */
export interface NguoiDung {
  readonly id: string;
  readonly email: string;
  readonly displayName: string;
  readonly role: VaiTro;
}

/** `AccessTokenResponse`: thân phản hồi đăng nhập / làm mới. Refresh token nằm trong cookie HttpOnly, JS không thấy. */
export interface PhienDangNhap {
  readonly accessToken: string;
  readonly accessTokenExpiresAt: string;
  readonly user: NguoiDung;
}

export const API_AUTH = {
  dangNhap: '/api/auth/login',
  lamMoi: '/api/auth/refresh',
  dangXuat: '/api/auth/logout',
} as const;

/** `/refresh` và `/logout` đọc cookie nên core đòi header này (chống CSRF); trang lạ không gửi được nó. */
export const HEADER_CHONG_CSRF = { 'X-Requested-With': 'XMLHttpRequest' } as const;
