import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * «Chọn cách bắt đầu» theo trang Wiii (meiiie-design-kit, examples/wiii): hai thẻ kem cho học sinh và giáo viên, ảnh minh
 * họa vẽ riêng (labs/design/prototypes/2026-10-06-trang-chu-anh), rồi «Trước khi dùng»: giới hạn của bản thử, hiện sẵn để
 * liên kết ở đầu trang dẫn tới đúng chỗ.
 */
@Component({
  selector: 'app-chon-vai',
  imports: [RouterLink],
  template: `
    <section id="chon" class="chon">
      <h2>Chọn cách bắt đầu.</h2>
      <div class="hai-the">
        <article>
          <img
            class="minh-hoa"
            src="/anh/hoc-sinh-800.webp"
            srcset="/anh/hoc-sinh-800.webp 800w, /anh/hoc-sinh-1200.webp 1200w"
            sizes="(min-width: 64rem) 560px, 100vw"
            width="1200"
            height="900"
            alt="Minh họa: học sinh viết lời giải từng bước vào vở, máy tính bên cạnh hiện đồ thị và tiếp tuyến"
            loading="lazy"
            decoding="async"
          />
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
          <img
            class="minh-hoa"
            src="/anh/giao-vien-800.webp"
            srcset="/anh/giao-vien-800.webp 800w, /anh/giao-vien-1200.webp 1200w"
            sizes="(min-width: 64rem) 560px, 100vw"
            width="1200"
            height="900"
            alt="Minh họa: giáo viên đánh dấu tờ công thức trước giờ dạy, bảng tối phía sau có các đường cong"
            loading="lazy"
            decoding="async"
          />
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
  styleUrl: './chon-vai.css',
})
export class ChonVai {}
