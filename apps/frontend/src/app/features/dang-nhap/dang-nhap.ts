import { afterNextRender, Component, ElementRef, inject, Injector, signal, viewChild } from '@angular/core';
import { email, form, FormField, maxLength, required } from '@angular/forms/signals';
import { RouterLink } from '@angular/router';
import { TEN_SAN_PHAM } from '../../core/san-pham';
import { BrandMark } from '../../shared/ui/brand-mark';
import { Button } from '../../shared/ui/button';

type Buoc = 'email' | 'mat-khau';

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
  private readonly taiKhoan = signal({ email: '', matKhau: '' });
  protected readonly f = form(this.taiKhoan, (p) => {
    required(p.email);
    email(p.email);
    maxLength(p.email, 120);
    required(p.matKhau);
    maxLength(p.matKhau, 72);
  });

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
    const giaTri = this.f.email().value().trim().toLowerCase();
    if (!giaTri || this.f.email().invalid()) {
      this.oEmail()?.nativeElement.focus();
      return;
    }
    this.chonEmail(giaTri);
  }

  protected chonEmail(giaTri: string): void {
    this.taiKhoan.update((tk) => ({ ...tk, email: giaTri }));
    this.hienMatKhau.set(false);
    this.doiBuoc('mat-khau');
  }

  protected quayLai(): void {
    this.doiBuoc('email');
  }

  protected vaoHoc(event: Event): void {
    event.preventDefault();
    // Gửi tới services/core: issue #57 (port identity từ LMS ở #55).
  }

  private doiBuoc(buoc: Buoc): void {
    this.buoc.set(buoc);
    afterNextRender(() => (buoc === 'email' ? this.oEmail() : this.oMatKhau())?.nativeElement.focus(), {
      injector: this.injector,
    });
  }
}
