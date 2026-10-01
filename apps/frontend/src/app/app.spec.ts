import { TestBed } from '@angular/core/testing';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { appConfig } from './app.config';

describe('định tuyến và tiêu đề tab', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: appConfig.providers }));

  it('/ chuyển về /dang-nhap', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/');
    expect(TestBed.inject(Router).url).toBe('/dang-nhap');
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe('Đăng nhập');
  });

  it('tab theo mẫu «<trang> · Học toán với AI»', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/dang-nhap');
    expect(TestBed.inject(Title).getTitle()).toBe('Đăng nhập · Học toán với AI');
  });

  it('đường lạ chuyển về /dang-nhap', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/khong-co');
    expect(TestBed.inject(Router).url).toBe('/dang-nhap');
  });
});
