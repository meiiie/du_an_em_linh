import { HttpErrorResponse } from '@angular/common/http';
import { afterNextRender, Component, computed, ElementRef, inject, Injector, input, signal, viewChild } from '@angular/core';
import { email, form, FormField, maxLength, required } from '@angular/forms/signals';
import { Router, RouterLink } from '@angular/router';
import { VaiTro } from '../../api/auth';
import { Phien, trangChuCua } from '../../core/auth/phien';
import { TEN_SAN_PHAM } from '../../core/san-pham';
import { BrandMark } from '../../shared/ui/brand-mark';
import { Button } from '../../shared/ui/button';

type Buoc = 'email' | 'mat-khau';
type LoiDangNhap = 'sai' | 'khoa' | 'may-chu';

// Tài khoản tổng hợp của bản demo (AGENTS.md), không phải học sinh thật.
const TAI_KHOAN_THU = [
  { nhan: 'Học sinh An', email: 'hs.an@demo.local' },
  { nhan: 'Giáo viên', email: 'gv@demo.local' },
] as const;

/** Đăng nhập hai bước như v0 (apps/web/components/login-form.tsx): email → mật khẩu. */
@Component({
  selector: 'app-dang-nhap',
  imports: [FormField, RouterLink, BrandMark, Button],
  templateUrl: './dang-nhap.html',
  styleUrl: './dang-nhap.css',
})
export class DangNhap {
  protected readonly tenSanPham = TEN_SAN_PHAM;
  protected readonly taiKhoanThu = TAI_KHOAN_THU;
  protected readonly buoc = signal<Buoc>('email');
  protected readonly hienMatKhau = signal(false);
  /** Đã bấm «Tiếp tục» ít nhất một lần: từ đó mới báo lỗi email, và lỗi tự tắt khi email đúng. */
  private readonly daGuiEmail = signal(false);
  private readonly taiKhoan = signal({ email: '', matKhau: '' });
  protected readonly f = form(this.taiKhoan, (p) => {
    required(p.email);
    email(p.email);
    maxLength(p.email, 120);
    required(p.matKhau);
    maxLength(p.matKhau, 72);
  });

  protected readonly hienLoiEmail = computed(() => this.daGuiEmail() && this.f.email().invalid());

  /** `?returnUrl=` do guard gắn khi chặn một trang cần đăng nhập. */
  readonly returnUrl = input<string>();
  protected readonly dangGui = signal(false);
  protected readonly loi = signal<LoiDangNhap | null>(null);
  /** Gợi ý mật khẩu thử như v0, chỉ cho tài khoản tổng hợp `@demo.local`. */
  protected readonly laTaiKhoanThu = computed(() => this.f.email().value().endsWith('@demo.local'));

  private readonly phien = inject(Phien);
  private readonly router = inject(Router);

  private readonly injector = inject(Injector);
  private readonly oEmail = viewChild<ElementRef<HTMLInputElement>>('oEmail');
  private readonly oMatKhau = viewChild<ElementRef<HTMLInputElement>>('oMatKhau');

  constructor() {
    // Chỉ tự đặt con trỏ khi có chuột: trên điện thoại, bàn phím ảo bật lên che mất form.
    afterNextRender(() => {
      if (typeof matchMedia === 'function' && matchMedia('(pointer: fine)').matches) this.oEmail()?.nativeElement.focus();
    });
  }

  protected sangMatKhau(event: Event): void {
    event.preventDefault();
    this.daGuiEmail.set(true);
    const giaTri = this.f.email().value().trim().toLowerCase();
    if (!giaTri || this.f.email().invalid()) {
      this.oEmail()?.nativeElement.focus();
      return;
    }
    this.chonEmail(giaTri);
  }

  protected chonEmail(giaTri: string): void {
    // Đang gửi đăng nhập thì không đổi tài khoản: kết quả sẽ thuộc về email cũ (nút đã khóa, đây là lớp chặn thứ hai).
    if (this.dangGui()) return;
    // Mỗi lần vào bước mật khẩu, ô mật khẩu bắt đầu rỗng (như v0): mật khẩu không theo sang tài khoản khác,
    // dù email đổi bằng chip hay gõ tay (ô email gắn thẳng vào model nên không so được với email cũ).
    this.taiKhoan.set({ email: giaTri, matKhau: '' });
    this.daGuiEmail.set(false);
    this.loi.set(null);
    this.hienMatKhau.set(false);
    this.doiBuoc('mat-khau');
  }

  protected quayLai(): void {
    if (this.dangGui()) return;
    this.loi.set(null);
    this.doiBuoc('email');
  }

  protected async vaoHoc(event: Event): Promise<void> {
    event.preventDefault();
    if (this.dangGui()) return;
    if (this.f.matKhau().invalid()) {
      this.oMatKhau()?.nativeElement.focus();
      return;
    }
    this.dangGui.set(true);
    this.loi.set(null);
    try {
      const nguoiDung = await this.phien.dangNhap(this.f.email().value(), this.f.matKhau().value());
      await this.router.navigateByUrl(duongVe(this.returnUrl(), nguoiDung.role));
    } catch (e) {
      this.loi.set(phanLoaiLoi(e));
      this.oMatKhau()?.nativeElement.focus();
    } finally {
      this.dangGui.set(false);
    }
  }

  private doiBuoc(buoc: Buoc): void {
    this.buoc.set(buoc);
    afterNextRender(() => (buoc === 'email' ? this.oEmail() : this.oMatKhau())?.nativeElement.focus(), {
      injector: this.injector,
    });
  }
}

/** Chỉ nhận đường nội bộ (`/…`, không `//` hay `/\` sang miền khác, không quay lại `/dang-nhap`); còn lại về trang chủ. */
function duongVe(returnUrl: string | undefined, vaiTro: VaiTro): string {
  return returnUrl && /^\/(?![/\\])/.test(returnUrl) && !returnUrl.startsWith('/dang-nhap') ? returnUrl : trangChuCua(vaiTro);
}

/**
 * 400 (mật khẩu quá 72 byte) và 401 cùng một câu, như máy chủ: không lộ tài khoản nào có thật. 429: sai quá 5 lần trong
 * 15 phút (F-10, #69), tạm khóa theo email + máy.
 */
function phanLoaiLoi(e: unknown): LoiDangNhap {
  if (!(e instanceof HttpErrorResponse)) return 'may-chu';
  if (e.status === 429) return 'khoa';
  return e.status === 400 || e.status === 401 ? 'sai' : 'may-chu';
}
