import { httpResource } from '@angular/common/http';
import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { API_HS, BaiCuaHocSinh, TEN_MUC, TrangHoc } from '../../api/hoc-sinh';
import { Phien } from '../../core/auth/phien';
import { Katex } from '../../shared/toan/katex';
import { Button } from '../../shared/ui/button';
import { deBang, HangBai, thanDe } from './hang-bai';
import { SoKyNang } from './so-ky-nang';

/**
 * `/hs`: trang chủ học sinh theo ảnh mô phỏng A (labs/design/prototypes/2026-10-06-wiii-3b1b/A-hoc-1280): «Chào <tên>»
 * (heading như v0), thẻ «Bài tiếp theo» (bài đang làm dở, không có thì bài chưa làm đầu tiên) với đề trên bảng 3b1b, và
 * «Bài được giao». Dữ liệu từ `GET /api/hs/bai` của core.
 */
@Component({
  selector: 'app-trang-hoc-sinh',
  imports: [Button, HangBai, Katex, RouterLink, SoKyNang],
  templateUrl: './trang-hoc-sinh.html',
  styleUrl: './trang-hoc-sinh.css',
})
export class TrangHocSinh {
  private readonly phien = inject(Phien);
  protected readonly ten = computed(() => this.phien.nguoiDung()?.displayName ?? '');
  protected readonly ds = httpResource<BaiCuaHocSinh[]>(() => API_HS.bai);
  /** Mức hiểu theo kỹ năng (mastery). Lỗi thì chỉ ẩn sổ kỹ năng, danh sách bài vẫn dùng được. */
  protected readonly th = httpResource<TrangHoc>(() => API_HS.trangHoc);
  /** Bài làm được trên phiếu (`soBuoc` 0 là bài trắc nghiệm, em làm ra giấy): đang làm dở trước, rồi chưa làm. */
  protected readonly ke = computed(() => {
    const bai = (this.ds.value() ?? []).filter((b) => b.soBuoc > 0);
    return bai.find((b) => b.trangThai === 'DANG_LAM') ?? bai.find((b) => b.trangThai === 'CHUA_LAM');
  });
  protected readonly deBang = deBang;
  protected readonly thanDe = thanDe;
  protected readonly tenMuc = TEN_MUC;
  protected readonly vach = (b: BaiCuaHocSinh) => Array.from({ length: b.soBuoc }, (_, i) => i < b.soBuocDat);
}
