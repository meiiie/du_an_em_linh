import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Phien } from '../../core/auth/phien';
import { Katex } from '../../shared/toan/katex';
import { Button } from '../../shared/ui/button';
import { BaiHocSinh, NAM_BUOC, TEN_MUC } from './bai-mau';
import { HangBai } from './hang-bai';

/**
 * `/hs`: trang chủ học sinh theo ảnh mô phỏng A (labs/design/prototypes/2026-10-06-wiii-3b1b/A-hoc-1280): «Chào <tên>»
 * (heading như v0), thẻ «Bài tiếp theo» với đề trên bảng 3b1b và năm bước, «Bài được giao», «Mức hiểu» theo kỹ năng.
 */
@Component({
  selector: 'app-trang-hoc-sinh',
  imports: [Button, HangBai, Katex, RouterLink],
  templateUrl: './trang-hoc-sinh.html',
  styleUrl: './trang-hoc-sinh.css',
})
export class TrangHocSinh {
  private readonly phien = inject(Phien);
  protected readonly du = inject(BaiHocSinh);
  protected readonly ten = computed(() => this.phien.nguoiDung()?.displayName ?? '');
  protected readonly ke = this.du.baiKeTiep();
  protected readonly namBuoc = NAM_BUOC;
  protected readonly tenMuc = TEN_MUC;
  protected readonly mucSo = { NHAN_BIET: 1, THONG_HIEU: 2, VAN_DUNG: 3, VAN_DUNG_CAO: 4 } as const;
  protected readonly buocDat = this.ke?.trangThai.loai === 'dang-lam' ? this.ke.trangThai.buocDat : 0;
}
