import { httpResource } from '@angular/common/http';
import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { API_HS, BaiCuaHocSinh, TEN_MUC } from '../../api/hoc-sinh';
import { Phien } from '../../core/auth/phien';
import { Katex } from '../../shared/toan/katex';
import { Button } from '../../shared/ui/button';
import { HangBai } from './hang-bai';

/**
 * `/hs`: trang chủ học sinh theo ảnh mô phỏng A (labs/design/prototypes/2026-10-06-wiii-3b1b/A-hoc-1280): «Chào <tên>»
 * (heading như v0), thẻ «Bài tiếp theo» (bài đang làm dở, không có thì bài chưa làm đầu tiên) với đề trên bảng 3b1b, và
 * «Bài được giao». Dữ liệu từ `GET /api/hs/bai` của core.
 */
@Component({
  selector: 'app-trang-hoc-sinh',
  imports: [Button, HangBai, Katex, RouterLink],
  templateUrl: './trang-hoc-sinh.html',
  styleUrl: './trang-hoc-sinh.css',
})
export class TrangHocSinh {
  private readonly phien = inject(Phien);
  protected readonly ten = computed(() => this.phien.nguoiDung()?.displayName ?? '');
  protected readonly ds = httpResource<BaiCuaHocSinh[]>(() => API_HS.bai);
  protected readonly ke = computed(() => {
    const bai = this.ds.value() ?? [];
    return bai.find((b) => b.trangThai === 'DANG_LAM') ?? bai.find((b) => b.trangThai === 'CHUA_LAM');
  });
  protected readonly tenMuc = TEN_MUC;
  protected readonly vach = (b: BaiCuaHocSinh) => Array.from({ length: b.soBuoc }, (_, i) => i < b.soBuocDat);
}
