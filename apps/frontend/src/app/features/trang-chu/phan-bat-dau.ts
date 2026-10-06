import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Năm bước của phiếu, tên như v0 (`apps/web/lib/de-hoc-sinh.ts` TEN_TRANG), kèm việc của từng bước. */
const NAM_BUOC = [
  { ten: 'Tập xác định', viec: 'Viết tập xác định D.' },
  { ten: 'Đạo hàm', viec: 'Tính y′.' },
  { ten: 'Nghiệm y′', viec: 'Giải y′ = 0.' },
  { ten: 'Xét dấu', viec: 'Lập bảng xét dấu y′.' },
  { ten: 'Kết luận', viec: 'Khoảng đơn điệu và cực trị.' },
];

/**
 * Nửa dưới trang công khai theo trang Wiii (meiiie-design-kit, examples/wiii): «Bắt đầu» (chữ trái, thẻ năm bước phải),
 * «Chọn cách bắt đầu» (hai thẻ kem cho học sinh và giáo viên), «Trước khi dùng» (giới hạn của bản thử, hiện sẵn để liên kết ở đầu trang dẫn tới đúng chỗ).
 */
@Component({
  selector: 'app-phan-bat-dau',
  imports: [RouterLink],
  template: `
    <section id="nam-buoc" class="bat-dau">
      <div>
        <p class="nho">Bắt đầu một bài</p>
        <h2><span class="dong">Một bài. </span><span class="dong">Năm bước.</span></h2>
        <p class="mo-ta">
          Viết từng bước như trên giấy. Máy kiểm tra mỗi bước và chỉ ra chỗ sai. Đồ thị của bài chỉ mở sau kết luận.
        </p>
        <div class="nut">
          <a class="vien vien-dam" routerLink="/dang-nhap">Vào học</a>
          <a class="vien vien-kem" href="#xem">Xem cách học</a>
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

    <section id="chon" class="chon">
      <h2>Chọn cách bắt đầu.</h2>
      <div class="hai-the">
        <article>
          <h3>Học sinh.</h3>
          <p>Làm bài theo từng bước, hỏi gia sư khi kẹt.</p>
          <ul>
            <li>Phiếu năm bước cho mỗi bài</li>
            <li>Gợi ý ba cấp, không đưa đáp án</li>
            <li>Mức hiểu theo từng kỹ năng</li>
            <li>Lịch học trong tuần</li>
          </ul>
          <a class="vien vien-dam" routerLink="/dang-nhap">Vào học</a>
        </article>
        <article>
          <h3>Giáo viên.</h3>
          <p>Chuẩn bị nội dung của lớp và theo dõi từng em.</p>
          <ul>
            <li>Tài liệu và bảng công thức của lớp</li>
            <li>Công thức được kiểm trước khi dùng</li>
            <li>Hàng duyệt cho phần máy không kiểm được</li>
            <li>Mức của từng học sinh</li>
          </ul>
          <a class="vien vien-vien" routerLink="/dang-nhap">Vào lớp</a>
        </article>
      </div>
      <div id="truoc-khi-dung" class="truoc">
        <h3>Trước khi dùng</h3>
        <p>
          MathL+ đang ở bản thử nghiệm. Hiện có một chủ đề: đơn điệu và cực trị, Toán 12. Chưa tự đăng ký được; tài
          khoản thử do nhóm phát triển cấp. Các hình trên trang minh họa cách học, không phải bài làm của học sinh.
        </p>
      </div>
    </section>
  `,
  styleUrl: './phan-bat-dau.css',
})
export class PhanBatDau {
  protected readonly namBuoc = NAM_BUOC;
}
