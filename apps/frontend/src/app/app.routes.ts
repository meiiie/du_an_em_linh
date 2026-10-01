import { Routes } from '@angular/router';

// Đường dẫn giữ như v0 (`/dang-nhap`, sau này `/hs`, `/gv`) để e2e đối chiếu được.
// `/` là trang công khai ở v0; v2 chưa có nên tạm chuyển về đăng nhập.
export const routes: Routes = [
  {
    path: 'dang-nhap',
    title: 'Đăng nhập',
    loadComponent: () => import('./features/dang-nhap/dang-nhap').then((m) => m.DangNhap),
  },
  { path: '', pathMatch: 'full', redirectTo: 'dang-nhap' },
  { path: '**', redirectTo: 'dang-nhap' },
];
