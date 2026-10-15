import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from '../../shared/ui/button';

/**
 * «Chọn cách bắt đầu»: hai thẻ sáng cho học sinh và giáo viên, ảnh minh
 * họa vẽ riêng (labs/design/prototypes/2026-10-06-trang-chu-anh), rồi «Trước khi dùng»: giới hạn của bản thử, hiện sẵn để
 * liên kết ở đầu trang dẫn tới đúng chỗ.
 */
@Component({
  selector: 'app-chon-vai',
  imports: [RouterLink, Button],
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
            <li>Làm bài theo từng bước phù hợp với chủ đề</li>
            <li>Gia sư AI gợi ý theo bước, không đưa đáp án</li>
            <li>Xem lời giải đầy đủ sau khi nộp</li>
            <li>Nhắc lịch học qua thông báo trình duyệt</li>
          </ul>
          <a appButton routerLink="/dang-nhap">Vào học</a>
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
          <p>Cùng AI soạn nội dung, thầy cô quyết định trước khi giao.</p>
          <ul>
            <li>AI soạn nháp bài tập và lời giải từ tài liệu của thầy cô</li>
            <li>Thầy cô duyệt trước khi giao cho học sinh</li>
            <li>Theo dõi mức hiểu của từng học sinh</li>
            <li>Xem mức dùng AI của lớp</li>
          </ul>
          <a appButton variant="secondary" routerLink="/dang-nhap">Vào lớp</a>
        </article>
      </div>
      <div id="truoc-khi-dung" class="truoc">
        <h3>Trước khi dùng</h3>
        <p>
          Toán THPT lớp 10–12. Bài luyện tập hiển thị theo lớp và chủ đề đã được phát hành.
          Tài khoản do nhà trường cung cấp, không tự đăng ký.
          Nội dung được thầy cô duyệt trước khi học sinh thấy.
          Các hình trên trang minh họa cách học, không phải bài làm của học sinh.
        </p>
      </div>
    </section>
  `,
  styleUrl: './chon-vai.css',
})
export class ChonVai {}
