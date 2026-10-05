import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Phien } from '../../core/auth/phien';
import { AN, GV, PhienGia } from '../../core/auth/phien.testing';
import { DangNhap } from './dang-nhap';

async function moTrang(returnUrl?: string) {
  const gia = new PhienGia();
  await TestBed.configureTestingModule({
    imports: [DangNhap],
    providers: [provideRouter([]), { provide: Phien, useValue: gia }],
  }).compileComponents();
  const dieuHuong = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  const fixture = TestBed.createComponent(DangNhap);
  if (returnUrl !== undefined) fixture.componentRef.setInput('returnUrl', returnUrl);
  await fixture.whenStable();
  const el = fixture.nativeElement as HTMLElement;
  return {
    el,
    gia,
    dieuHuong,
    on: () => fixture.whenStable(),
    o: (testid: string) => el.querySelector<HTMLInputElement>(`[data-testid="${testid}"]`),
    nut: (chu: string) => [...el.querySelectorAll('button')].find((b) => b.textContent?.trim() === chu) ?? null,
    gui: () => el.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true })),
  };
}

function go(o: HTMLInputElement, giaTri: string): void {
  o.value = giaTri;
  o.dispatchEvent(new Event('input'));
}

describe('DangNhap', () => {
  it('bước email: tiêu đề, ô email, Tiếp tục khóa khi ô trống, có nút đổi giao diện', async () => {
    const t = await moTrang();
    expect(t.el.querySelector('h1')?.textContent).toBe('Đăng nhập');
    expect(t.el.querySelector('[data-testid="doi-giao-dien"]')?.getAttribute('aria-pressed')).toBe('false');
    expect(t.o('email')).not.toBeNull();
    expect(t.o('password')).toBeNull();
    expect(t.nut('Tiếp tục')?.disabled).toBe(true);
  });

  it('email hợp lệ → bước mật khẩu, email chỉ đọc và viết thường', async () => {
    const t = await moTrang();
    go(t.o('email')!, 'HS.An@demo.local');
    await t.on();
    expect(t.nut('Tiếp tục')?.disabled).toBe(false);
    t.gui();
    await t.on();
    expect(t.o('email')?.readOnly).toBe(true);
    expect(t.o('email')?.value).toBe('hs.an@demo.local');
    expect(t.o('password')?.type).toBe('password');
  });

  it('email sai dạng → ở lại bước email, báo lỗi ngay dưới ô; sửa đúng thì lỗi tắt', async () => {
    const t = await moTrang();
    go(t.o('email')!, 'a..b@example.com');
    await t.on();
    expect(t.el.querySelector('#loi-email')).toBeNull();
    t.gui();
    await t.on();
    expect(t.o('password')).toBeNull();
    expect(t.el.querySelector('#loi-email')?.textContent).toBe('Email chưa đúng dạng.');
    expect(t.o('email')?.getAttribute('aria-invalid')).toBe('true');
    expect(t.o('email')?.getAttribute('aria-describedby')).toBe('loi-email');
    go(t.o('email')!, 'hs.an@demo.local');
    await t.on();
    expect(t.el.querySelector('#loi-email')).toBeNull();
    expect(t.o('email')?.hasAttribute('aria-invalid')).toBe(false);
  });

  it('tài khoản thử điền email và sang bước mật khẩu', async () => {
    const t = await moTrang();
    t.nut('Giáo viên')!.click();
    await t.on();
    expect(t.o('email')?.value).toBe('gv@demo.local');
    expect(t.o('password')).not.toBeNull();
  });

  it('Hiện / Ẩn mật khẩu đổi kiểu ô và aria-pressed', async () => {
    const t = await moTrang();
    t.nut('Học sinh An')!.click();
    await t.on();
    const nutMat = t.el.querySelector<HTMLButtonElement>('[aria-controls="o-mat-khau"]')!;
    expect(nutMat.getAttribute('aria-label')).toBe('Hiện mật khẩu');
    nutMat.click();
    await t.on();
    expect(t.o('password')?.type).toBe('text');
    expect(nutMat.getAttribute('aria-pressed')).toBe('true');
    expect(nutMat.getAttribute('aria-label')).toBe('Ẩn mật khẩu');
  });

  it('mật khẩu đã gõ không theo sang tài khoản khác (gõ email khác, chip khác, chọn lại)', async () => {
    const t = await moTrang();
    const goMatKhauChoAn = async () => {
      t.nut('Học sinh An')!.click();
      await t.on();
      go(t.o('password')!, 'bi-mat');
      await t.on();
    };

    await goMatKhauChoAn();
    t.nut('Quay lại')!.click();
    await t.on();
    go(t.o('email')!, 'hs.binh@demo.local');
    await t.on();
    t.gui();
    await t.on();
    expect(t.o('email')?.value).toBe('hs.binh@demo.local');
    expect(t.o('password')?.value).toBe('');

    await goMatKhauChoAn();
    t.nut('Giáo viên')!.click();
    await t.on();
    expect(t.o('email')?.value).toBe('gv@demo.local');
    expect(t.o('password')?.value).toBe('');

    await goMatKhauChoAn();
    t.nut('Quay lại')!.click();
    await t.on();
    t.nut('Học sinh An')!.click();
    await t.on();
    expect(t.o('password')?.value).toBe('');
  });

  it('Quay lại về bước email, giữ email đã nhập', async () => {
    const t = await moTrang();
    t.nut('Học sinh An')!.click();
    await t.on();
    t.nut('Quay lại')!.click();
    await t.on();
    expect(t.o('password')).toBeNull();
    expect(t.o('email')?.readOnly).toBe(false);
    expect(t.o('email')?.value).toBe('hs.an@demo.local');
  });

  describe('gửi đăng nhập', () => {
    async function vaoBuocMatKhau(returnUrl?: string, email = 'hs.an@demo.local') {
      const t = await moTrang(returnUrl);
      go(t.o('email')!, email);
      await t.on();
      t.gui();
      await t.on();
      go(t.o('password')!, 'hocsinh123');
      await t.on();
      return t;
    }

    it('đúng → vào trang chủ theo vai trò', async () => {
      const t = await vaoBuocMatKhau();
      t.gia.ketQuaDangNhap = () => Promise.resolve(AN);
      t.gui();
      await t.on();
      expect(t.dieuHuong).toHaveBeenCalledWith('/hs');
      expect(t.el.querySelector('[data-testid="loi-dang-nhap"]')).toBeNull();
    });

    it('returnUrl nội bộ được dùng; returnUrl sang miền khác bị bỏ', async () => {
      const noiBo = await vaoBuocMatKhau('/gv', 'gv@demo.local');
      noiBo.gia.ketQuaDangNhap = () => Promise.resolve(GV);
      noiBo.gui();
      await noiBo.on();
      expect(noiBo.dieuHuong).toHaveBeenCalledWith('/gv');

      TestBed.resetTestingModule();
      const la = await vaoBuocMatKhau('//vi-du.test/lua');
      la.gia.ketQuaDangNhap = () => Promise.resolve(AN);
      la.gui();
      await la.on();
      expect(la.dieuHuong).toHaveBeenCalledWith('/hs');
    });

    it('sai mật khẩu → báo lỗi kèm mật khẩu thử như v0, ô mật khẩu aria-invalid', async () => {
      const t = await vaoBuocMatKhau();
      t.gia.ketQuaDangNhap = () => Promise.reject(new HttpErrorResponse({ status: 401 }));
      t.gui();
      await t.on();
      const loi = t.el.querySelector('[data-testid="loi-dang-nhap"]');
      expect(loi?.getAttribute('role')).toBe('alert');
      expect(loi?.textContent).toContain('hocsinh123');
      expect(loi?.textContent).toContain('giaovien123');
      expect(t.o('password')?.getAttribute('aria-invalid')).toBe('true');
      expect(t.o('password')?.getAttribute('aria-describedby')).toBe('loi-dang-nhap');
      expect(t.dieuHuong).not.toHaveBeenCalled();
    });

    it('tài khoản không phải tài khoản thử → không lộ mật khẩu thử', async () => {
      const t = await vaoBuocMatKhau(undefined, 'co.giao@truong.edu.vn');
      t.gia.ketQuaDangNhap = () => Promise.reject(new HttpErrorResponse({ status: 401 }));
      t.gui();
      await t.on();
      const loi = t.el.querySelector('[data-testid="loi-dang-nhap"]')?.textContent ?? '';
      expect(loi).toContain('Chưa vào được');
      expect(loi).not.toContain('hocsinh123');
    });

    it('sai quá 5 lần (429) → báo tạm khóa như v0, không đổ cho mật khẩu', async () => {
      const t = await vaoBuocMatKhau();
      t.gia.ketQuaDangNhap = () => Promise.reject(new HttpErrorResponse({ status: 429 }));
      t.gui();
      await t.on();
      expect(t.el.querySelector('[data-testid="khoa-dang-nhap"]')?.textContent).toContain('tạm khóa 15 phút');
      expect(t.el.querySelector('[data-testid="loi-dang-nhap"]')).toBeNull();
      expect(t.o('password')?.hasAttribute('aria-invalid')).toBe(false);
      expect(t.o('password')?.getAttribute('aria-describedby')).toBe('loi-dang-nhap');
    });

    it('máy chủ lỗi → câu riêng, không đổ cho mật khẩu', async () => {
      const t = await vaoBuocMatKhau();
      t.gia.ketQuaDangNhap = () => Promise.reject(new HttpErrorResponse({ status: 0 }));
      t.gui();
      await t.on();
      expect(t.el.querySelector('[data-testid="loi-dang-nhap"]')?.textContent).toContain('Chưa kết nối được máy chủ');
      expect(t.o('password')?.hasAttribute('aria-invalid')).toBe(false);
    });

    it('đang gửi thì khóa Vào học và mọi nút đổi tài khoản', async () => {
      const t = await vaoBuocMatKhau();
      let xong!: (n: typeof AN) => void;
      t.gia.ketQuaDangNhap = () => new Promise((r) => (xong = r));
      t.gui();
      await t.on();
      expect(t.nut('Vào học')?.disabled).toBe(true);
      expect(t.nut('Vào học')?.getAttribute('aria-busy')).toBe('true');
      for (const chu of ['Quay lại', 'Học sinh An', 'Giáo viên']) {
        expect(t.nut(chu)?.disabled, chu).toBe(true);
      }
      expect(t.el.querySelector<HTMLButtonElement>('[aria-label="Sửa email"]')?.disabled).toBe(true);
      xong(AN);
      // Chuỗi await (đăng nhập → điều hướng → finally) chạy qua vài microtask mà Angular không theo dõi.
      await new Promise((r) => setTimeout(r, 0));
      await t.on();
      expect(t.nut('Vào học')?.disabled).toBe(false);
    });
  });
});
