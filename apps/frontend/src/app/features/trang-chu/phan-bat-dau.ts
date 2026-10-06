import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from '../../shared/ui/button';

/** Năm bước của phiếu, tên như v0 (`apps/web/lib/de-hoc-sinh.ts` TEN_TRANG), kèm việc của từng bước. */
const NAM_BUOC = [
  { ten: 'Tập xác định', viec: 'Viết tập xác định D.' },
  { ten: 'Đạo hàm', viec: 'Tính y′.' },
  { ten: 'Nghiệm y′', viec: 'Giải y′ = 0.' },
  { ten: 'Xét dấu', viec: 'Lập bảng xét dấu y′.' },
  { ten: 'Kết luận', viec: 'Khoảng đơn điệu và cực trị.' },
];

/**
 * «Một bài. Năm bước.» theo phần «Bắt đầu» của trang Wiii (meiiie-design-kit, examples/wiii): chữ trái, thẻ năm bước phải.
 */
@Component({
  selector: 'app-phan-bat-dau',
  imports: [RouterLink, Button],
  template: `
    <section id="nam-buoc" class="bat-dau">
      <div>
        <p class="nho">Bắt đầu một bài</p>
        <h2><span class="dong">Một bài. </span><span class="dong">Năm bước.</span></h2>
        <p class="mo-ta">
          Viết từng bước như trên giấy. Máy kiểm tra mỗi bước và chỉ ra chỗ sai. Đồ thị của bài chỉ mở sau kết luận.
        </p>
        <div class="nut">
          <a appButton variant="vien-dam" routerLink="/dang-nhap">Vào học</a>
          <a appButton variant="vien-kem" href="#xem">Xem cách học</a>
        </div>
      </div>
      <div class="cua-so">
        <p class="cua-so-dau"><span>Phiếu năm bước</span><span>Toán 12</span></p>
        <ol>
          @for (b of namBuoc; track b.ten; let i = $index) {
            <li>
              <span class="so">0{{ i + 1 }}</span>
              <span><strong>{{ b.ten }}</strong>{{ b.viec }}</span>
            </li>
          }
        </ol>
      </div>
    </section>
  `,
  styleUrl: './phan-bat-dau.css',
})
export class PhanBatDau {
  protected readonly namBuoc = NAM_BUOC;
}
