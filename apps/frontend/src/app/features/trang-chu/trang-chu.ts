import { Component, computed, DestroyRef, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BrandMark } from '../../shared/ui/brand-mark';
import { DoiGiaoDien } from '../../shared/ui/doi-giao-dien';
import { CanhLamViec } from './canh-lam-viec';
import { DaiBang } from './dai-bang';
import { PhanBatDau } from './phan-bat-dau';

/** Từ gạch chân đổi lần lượt trong tiêu đề «Tự mình …» (như động từ xoay của trang Wiii). Trình đọc màn hình chỉ nghe từ đầu. */
const DONG_TU = ['hiểu ra', 'làm được', 'sửa sai', 'kiểm tra'];
const NHIP_MS = 2800;

/**
 * Trang công khai `/`, dựng theo trang Wiii trong meiiie-design-kit (examples/wiii): mở đầu căn giữa với từ xoay, lưới thẻ
 * cảnh, phần bắt đầu, dải bảng tối, hai thẻ chọn vai, chân trang có nút tạm dừng chuyển động. Vùng toán là bảng 3b1b.
 */
@Component({
  selector: 'app-trang-chu',
  imports: [RouterLink, BrandMark, DoiGiaoDien, CanhLamViec, DaiBang, PhanBatDau],
  templateUrl: './trang-chu.html',
  styleUrl: './trang-chu.css',
})
export class TrangChu {
  protected readonly dongTu = DONG_TU;
  protected readonly viTri = signal(0);
  /** Người dùng bấm «Tạm dừng chuyển động». */
  protected readonly tamDung = signal(false);
  private readonly giamChuyenDong = signal(
    typeof matchMedia === 'function' && matchMedia('(prefers-reduced-motion: reduce)').matches,
  );
  protected readonly chay = computed(() => !this.tamDung() && !this.giamChuyenDong());

  constructor() {
    if (typeof matchMedia === 'function') {
      const mq = matchMedia('(prefers-reduced-motion: reduce)');
      const nghe = (e: MediaQueryListEvent) => this.giamChuyenDong.set(e.matches);
      mq.addEventListener('change', nghe);
      inject(DestroyRef).onDestroy(() => mq.removeEventListener('change', nghe));
    }
    effect((onCleanup) => {
      if (!this.chay()) return;
      const id = setInterval(() => this.viTri.update((i) => (i + 1) % DONG_TU.length), NHIP_MS);
      onCleanup(() => clearInterval(id));
    });
  }
}
