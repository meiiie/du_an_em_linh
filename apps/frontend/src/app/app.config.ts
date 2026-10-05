import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, TitleStrategy, withComponentInputBinding } from '@angular/router';
import { routes } from './app.routes';
import { xacThucInterceptor } from './core/auth/xac-thuc.interceptor';
import { GiaoDien } from './core/giao-dien';
import { TieuDeTrang } from './core/tieu-de-trang';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withFetch(), withInterceptors([xacThucInterceptor])),
    { provide: TitleStrategy, useClass: TieuDeTrang },
    // Tạo sớm để <html data-theme> theo máy cả trên trang không có nút đổi giao diện.
    provideAppInitializer(() => void inject(GiaoDien)),
  ],
};
