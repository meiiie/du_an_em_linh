import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { DangNhap } from './dang-nhap';

async function moTrang() {
  await TestBed.configureTestingModule({ imports: [DangNhap], providers: [provideRouter([])] }).compileComponents();
  const fixture = TestBed.createComponent(DangNhap);
  await fixture.whenStable();
  const el = fixture.nativeElement as HTMLElement;
  return {
    el,
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
  it('bước email: tiêu đề, ô email, Tiếp tục khóa khi ô trống', async () => {
    const t = await moTrang();
    expect(t.el.querySelector('h1')?.textContent).toBe('Đăng nhập');
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

  it('email sai dạng → ở lại bước email', async () => {
    const t = await moTrang();
    go(t.o('email')!, 'khong-phai-email');
    await t.on();
    t.gui();
    await t.on();
    expect(t.o('password')).toBeNull();
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
});
