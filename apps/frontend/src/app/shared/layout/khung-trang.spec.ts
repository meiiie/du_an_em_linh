import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Phien } from '../../core/auth/phien';
import { AN, PhienGia } from '../../core/auth/phien.testing';
import { KhungTrang } from './khung-trang';

@Component({ imports: [KhungTrang], template: `<app-khung-trang><h1>Nội dung</h1></app-khung-trang>` })
class Trang {}

describe('KhungTrang', () => {
  async function mo() {
    const gia = new PhienGia().dangNhapNhu(AN);
    TestBed.configureTestingModule({ imports: [Trang], providers: [provideRouter([]), { provide: Phien, useValue: gia }] });
    const dieuHuong = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(Trang);
    await fixture.whenStable();
    const el = fixture.nativeElement as HTMLElement;
    return { gia, dieuHuong, el, on: () => fixture.whenStable(), nut: () => el.querySelector<HTMLButtonElement>('[data-testid="dang-xuat"]')! };
  }

  it('hiện tên người dùng, nội dung trong <main>, nút Đăng xuất', async () => {
    const t = await mo();
    expect(t.el.querySelector('[data-testid="ten-nguoi-dung"]')?.textContent).toBe('An');
    expect(t.el.querySelector('main h1')?.textContent).toBe('Nội dung');
    expect(t.nut().textContent?.trim()).toBe('Đăng xuất');
    expect(t.dieuHuong).not.toHaveBeenCalled();
  });

  it('Đăng xuất → về /dang-nhap', async () => {
    const t = await mo();
    t.nut().click();
    await t.on();
    expect(t.gia.daGoiDangXuat).toBe(1);
    expect(t.dieuHuong).toHaveBeenCalledWith(['/dang-nhap']);
  });

  it('đăng xuất lỗi mạng → báo lỗi, ở lại trang', async () => {
    const t = await mo();
    t.gia.loiDangXuat = new HttpErrorResponse({ status: 0 });
    t.nut().click();
    await t.on();
    expect(t.el.querySelector('[role="alert"]')?.textContent).toContain('Chưa đăng xuất được');
    expect(t.dieuHuong).not.toHaveBeenCalled();
  });
});
