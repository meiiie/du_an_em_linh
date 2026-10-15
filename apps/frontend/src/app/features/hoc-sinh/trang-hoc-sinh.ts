import { nhomBai } from './nhom-bai';
import { VanBanToan } from '../../shared/toan/van-ban-toan';
import { Item, ReminderDetails } from '../../api/ux';
import { ApiLopHoc } from '../../api/lop-hoc';
import { httpResource } from '@angular/common/http';
import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { API_HS, BaiCuaHocSinh, TEN_MUC, TrangHoc } from '../../api/hoc-sinh';
import { KhungCho, LoiTai, TrangRong } from '../../shared/ui/trang-thai';
import { Phien } from '../../core/auth/phien';
import { Katex } from '../../shared/toan/katex';
import { Button } from '../../shared/ui/button';
import { deBang, HangBai, thanDe } from './hang-bai';
import { SoKyNang } from './so-ky-nang';
import { DinhDangSo, ngayViet } from '../../shared/ui/dinh-dang';

/**
 * `/hs`: trang chủ học sinh theo ảnh mô phỏng A (labs/design/prototypes/2026-10-06-wiii-3b1b/A-hoc-1280): «Chào <tên>»
 * (heading như v0), thẻ «Bài tiếp theo» (bài đang làm dở hoặc bài kế do máy chủ chọn) với đề trên bảng 3b1b, và
 * «Bài được giao». Dữ liệu từ `GET /api/hs/bai` của core.
 */
@Component({
  selector: 'app-trang-hoc-sinh',
  imports: [VanBanToan,KhungCho, LoiTai, TrangRong, Button, HangBai, Katex, RouterLink, SoKyNang, DinhDangSo],
  templateUrl: './trang-hoc-sinh.html',
  styleUrl: './trang-hoc-sinh.css',
})
export class TrangHocSinh {
 private readonly api=inject(ApiLopHoc);

  private readonly phien = inject(Phien);
  protected readonly ten = computed(() => this.phien.nguoiDung()?.displayName ?? '');
  protected readonly ds = httpResource<BaiCuaHocSinh[]>(() => API_HS.bai);
  /** Mức hiểu theo kỹ năng (mastery). Lỗi thì chỉ ẩn sổ kỹ năng, danh sách bài vẫn dùng được. */
  protected readonly th = httpResource<TrangHoc>(() => API_HS.trangHoc);
  protected readonly cheDo = this.api.cheDo();
  protected readonly laKiemTra = (ma: string) => this.cheDo.hasValue() && this.cheDo.value().find(m => m.id === ma)?.status === 'KIEM_TRA';
  protected readonly goiBai = this.api.goiBai();
  protected readonly nhac = this.api.nhac();
  protected readonly baiHopLe = computed(() => (this.ds.value() ?? []).filter(b => {
    const mode=(this.cheDo.value() ?? []).find(m=>m.id===b.maBai);
    // Hạn tính theo giờ máy chủ: che-do chỉ trả bài còn làm được, không so với đồng hồ thiết bị.
    return b.trangThai !== 'DA_NOP' && !!mode && mode.details.conHan !== false;
  }));
  protected readonly deXuat = computed(() => (this.goiBai.value() ?? []).filter(r => r.status === 'GUI_THAY_CO'
    || this.baiHopLe().some(b => b.maBai === r.id && !this.laKiemTra(b.maBai))));
  protected tenBaiGoiY(id: string): string { return (this.ds.value() ?? []).find(b => b.maBai === id)?.tenKyNang ?? 'Bài được giao'; }
  protected readonly gio = (s:string)=>ngayViet(s, true);
  protected nhacTen(n: Item<ReminderDetails>): string {
    const code = n.title.startsWith('Sắp đến hạn: ') ? n.title.slice('Sắp đến hạn: '.length) : '';
    const b = (this.ds.value() ?? []).find(b => b.maBai === code);
    return code ? 'Sắp đến hạn: ' + (b?.tenBo || b?.tenKyNang || code) : n.title;
  }
  protected nhacNgay(n: Item<ReminderDetails>): string {
    const dueAt = n.details.dueAt;
    return ngayViet(typeof dueAt === 'string' ? dueAt : n.details.startsAt, true);
  }
  /** Bài ôn chưa nộp trước bài kiểm tra; không dùng gợi bài đã nộp làm lối vào làm lại. */
  protected readonly ke = computed(() => {
    if (!this.cheDo.hasValue()) return undefined;
    const bai = this.baiHopLe();
    const on = bai.filter(b => !this.laKiemTra(b.maBai));
    const recommend = this.goiBai.hasValue() ? this.goiBai.value().filter(r=>r.status!=='GUI_THAY_CO').map(r=>r.id) : [];
    return on.find(b => b.trangThai === 'DANG_LAM') ?? recommend.map(id=>on.find(b=>b.maBai===id)).find(Boolean) ?? bai.find(b=>this.laKiemTra(b.maBai));
  });
  protected readonly kiemTraKe = computed(() => {
    const ke = this.ke();
    if (!ke || !this.laKiemTra(ke.maBai)) return null;
    const nhom = (this.ds.value() ?? []).filter(b => this.laKiemTra(b.maBai) && b.tenBo === ke.tenBo && b.han === ke.han);
    const phut = ke.tenBo?.match(/(\d+)\s*phút/u)?.[1];
    return { ten: 'Bài kiểm tra' + (phut ? ' · ' + phut + ' phút' : ''), soCau: nhom.length,
      han: (this.cheDo.value() ?? []).find(m => m.id === ke.maBai)?.details.dueAt ?? ke.han };
  });
  protected readonly nhacSapToi = computed(() => (this.nhac.value() ?? []).slice(0, 2));
  protected readonly sapToi = computed(() => {
    const ds = this.ds.value() ?? [];
    const ke = this.ke()?.maBai;
    const uuTien = ds.filter(b => b.maBai !== ke && b.ketQua !== 'DAT')
      .sort((a,b) => (a.han ? Date.parse(a.han) : Infinity) - (b.han ? Date.parse(b.han) : Infinity))
      .slice(0, 5);
    const chon = new Set(uuTien.map(b => b.maBai));
    return nhomBai(ds).map(n => ({...n, tong: n.hang.length, hang: n.hang.filter(h => chon.has(h.bai.maBai))})).filter(n => n.hang.length);
  });
  protected readonly ngay = ngayViet;
  protected readonly deBang = deBang;
  protected readonly thanDe = thanDe;
  protected readonly tenMuc = TEN_MUC;
  protected readonly vach = (b: BaiCuaHocSinh) => Array.from({ length: b.soBuoc }, (_, i) => i < b.soBuocDat);
}
