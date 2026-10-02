import { computed, signal } from '@angular/core';
import { NguoiDung } from '../../api/auth';

export const AN: NguoiDung = { id: '00000000-0000-4000-8000-000000000001', email: 'hs.an@demo.local', displayName: 'An', role: 'STUDENT' };
export const GV: NguoiDung = {
  id: '00000000-0000-4000-8000-000000000002',
  email: 'gv@demo.local',
  displayName: 'Giáo viên thử',
  role: 'TEACHER',
};

/**
 * `Phien` giả cho test (dùng qua `{ provide: Phien, useValue: new PhienGia() }`): không gọi mạng; chỉnh được người
 * đang đăng nhập, người khôi phục được từ cookie, kết quả đăng nhập / đăng xuất.
 */
export class PhienGia {
  readonly hienTai = signal<NguoiDung | null>(null);
  readonly nguoiDung = this.hienTai.asReadonly();
  readonly daDangNhap = computed(() => this.hienTai() !== null);
  token: string | null = null;
  /** Người mà `lamMoi()` khôi phục được (cookie còn sống); `null` = không có phiên. */
  khoiPhucDuoc: NguoiDung | null = null;
  soLanLamMoi = 0;
  ketQuaDangNhap: () => Promise<NguoiDung> = () => Promise.reject(new Error('chưa đặt ketQuaDangNhap'));
  loiDangXuat: unknown = null;
  daGoiDangXuat = 0;

  dangNhapNhu(nguoiDung: NguoiDung, token = 'a1'): this {
    this.hienTai.set(nguoiDung);
    this.token = token;
    return this;
  }

  tokenTruyCap(): string | null {
    return this.token;
  }

  async dangNhap(): Promise<NguoiDung> {
    const nguoiDung = await this.ketQuaDangNhap();
    this.dangNhapNhu(nguoiDung);
    return nguoiDung;
  }

  async lamMoi(): Promise<boolean> {
    this.soLanLamMoi++;
    this.hienTai.set(this.khoiPhucDuoc);
    this.token = this.khoiPhucDuoc ? `a${this.soLanLamMoi + 1}` : null;
    return this.khoiPhucDuoc !== null;
  }

  async dangXuat(): Promise<void> {
    this.daGoiDangXuat++;
    if (this.loiDangXuat) throw this.loiDangXuat;
    this.hienTai.set(null);
    this.token = null;
  }
}
