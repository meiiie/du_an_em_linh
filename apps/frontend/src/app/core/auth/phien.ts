import { HttpClient } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { API_AUTH, HEADER_CHONG_CSRF, NguoiDung, PhienDangNhap, VaiTro } from '../../api/auth';

/** Trang chủ theo vai trò. Quản trị chưa có khu riêng nên dùng khu giáo viên. */
export function trangChuCua(vaiTro: VaiTro): string {
  return vaiTro === 'STUDENT' ? '/hs' : '/gv';
}

/**
 * Phiên đăng nhập của SPA (#57). Access token chỉ nằm trong bộ nhớ, mất khi tải lại trang. Refresh token nằm trong
 * cookie HttpOnly do services/core đặt, nên tải lại trang thì khôi phục phiên bằng `/refresh`.
 *
 * Làm mới chạy một luồng: trong một tab, các lời gọi đồng thời dùng chung một yêu cầu; giữa các tab, Web Locks xếp
 * hàng để mỗi lần làm mới gửi cookie mới nhất. Hai yêu cầu cùng gửi một refresh token cũ thì core coi là token bị lộ
 * và thu hồi mọi phiên của người dùng.
 */
@Injectable({ providedIn: 'root' })
export class Phien {
  private readonly http = inject(HttpClient);
  private readonly hienTai = signal<NguoiDung | null>(null);
  private accessToken: string | null = null;
  private lamMoiDangChay: Promise<boolean> | null = null;

  readonly nguoiDung = this.hienTai.asReadonly();
  readonly daDangNhap = computed(() => this.hienTai() !== null);

  tokenTruyCap(): string | null {
    return this.accessToken;
  }

  async dangNhap(email: string, matKhau: string): Promise<NguoiDung> {
    const phien = await firstValueFrom(this.http.post<PhienDangNhap>(API_AUTH.dangNhap, { email, password: matKhau }));
    this.nhan(phien);
    return phien.user;
  }

  /** Đổi cookie refresh token lấy access token mới; `false` khi phiên đã hết (không cookie, bị thu hồi, hết hạn). */
  lamMoi(): Promise<boolean> {
    this.lamMoiDangChay ??= theoKhoa(() =>
      firstValueFrom(this.http.post<PhienDangNhap>(API_AUTH.lamMoi, null, { headers: HEADER_CHONG_CSRF })),
    )
      .then((phien) => {
        this.nhan(phien);
        return true;
      })
      .catch(() => {
        this.xoa();
        return false;
      })
      .finally(() => {
        this.lamMoiDangChay = null;
      });
    return this.lamMoiDangChay;
  }

  /**
   * Thu hồi phiên ở máy chủ (core xóa luôn cookie) rồi mới quên phiên ở trình duyệt. Lỗi mạng thì ném lỗi và giữ
   * phiên: cookie còn sống, nếu quên phiên thì lần tải trang sau sẽ tự đăng nhập lại.
   */
  async dangXuat(): Promise<void> {
    await theoKhoa(() => firstValueFrom(this.http.post<void>(API_AUTH.dangXuat, null, { headers: HEADER_CHONG_CSRF })));
    this.xoa();
  }

  private nhan(phien: PhienDangNhap): void {
    this.accessToken = phien.accessToken;
    this.hienTai.set(phien.user);
  }

  private xoa(): void {
    this.accessToken = null;
    this.hienTai.set(null);
  }
}

/** Xếp hàng giữa các tab bằng Web Locks khi trình duyệt có; không có (jsdom, trình duyệt cũ) thì chạy thẳng. */
function theoKhoa<T>(viec: () => Promise<T>): Promise<T> {
  const khoa = typeof navigator === 'undefined' ? undefined : navigator.locks;
  return khoa ? khoa.request('hta-phien', viec) : viec();
}
