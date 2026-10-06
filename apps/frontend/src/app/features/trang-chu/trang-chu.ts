import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BrandMark } from '../../shared/ui/brand-mark';
import { Button } from '../../shared/ui/button';
import { DoiGiaoDien } from '../../shared/ui/doi-giao-dien';
import { MinhHoaDaoHam } from './minh-hoa-dao-ham';

/** Năm bước của phiếu, tên như v0 (`apps/web/lib/de-hoc-sinh.ts` TEN_TRANG). */
const NAM_BUOC = ['Tập xác định', 'Đạo hàm', 'Nghiệm y′', 'Xét dấu', 'Kết luận'];

/**
 * Trang công khai `/` theo docs/DESIGN.md «Trang công khai», chữ như v0 (`apps/web/app/page.tsx`): chữ trái, hình kéo được
 * phải; ba việc có thật; mục lục năm bước; dải cuối trên nền `board`. `vao-hoc` chỉ ở nút trên header.
 */
@Component({
  selector: 'app-trang-chu',
  imports: [RouterLink, BrandMark, Button, DoiGiaoDien, MinhHoaDaoHam],
  templateUrl: './trang-chu.html',
  styleUrl: './trang-chu.css',
})
export class TrangChu {
  protected readonly namBuoc = NAM_BUOC.map((ten, i) => ({ so: String(i + 1).padStart(2, '0'), ten }));
}
