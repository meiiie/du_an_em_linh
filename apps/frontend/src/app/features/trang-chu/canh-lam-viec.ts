import { Component } from '@angular/core';
import { Katex } from '../../shared/toan/katex';
import { MinhHoaDaoHam } from './minh-hoa-dao-ham';

const MUC = ['Nhận biết', 'Thông hiểu', 'Vận dụng', 'Vận dụng cao'];

/**
 * «Xem MathL+ làm việc»: lưới thẻ cảnh theo trang Wiii (meiiie-design-kit, examples/wiii), mỗi thẻ một việc có thật của
 * sản phẩm. Cảnh là minh họa (ghi ở đầu lưới), không phải ảnh chụp bài làm; hàm là y = 2x/(x² + 1), không thuộc ngân
 * hàng bài (SP-10).
 */
@Component({
  selector: 'app-canh-lam-viec',
  imports: [Katex, MinhHoaDaoHam],
  template: `
    <div class="tieu-de">
      <h2>Xem MathL+ làm việc</h2>
      <span>Minh họa</span>
    </div>
    <div class="luoi">
      <article class="the phieu-the">
        <p class="dau">Phiếu · Bài 3</p>
        <div class="than phieu">
          <ol class="muc-luc" aria-label="Năm bước">
            <li class="xong"><span>01</span>Tập xác định</li>
            <li class="dang"><span>02</span>Đạo hàm</li>
            <li><span>03</span>Nghiệm y′</li>
            <li><span>04</span>Xét dấu</li>
            <li><span>05</span>Kết luận</li>
          </ol>
          <div class="trang">
            <p class="buoc">02 / Đạo hàm</p>
            <p>Tính y′.</p>
            <app-katex latex="y' = \\dfrac{2(1 - x^{2})}{(x^{2} + 1)^{2}}" />
            <p class="dat">Đúng. Sang bước 3.</p>
          </div>
        </div>
        <p class="chan"><strong>Phiếu năm bước</strong><span>Kiểm từng bước</span></p>
      </article>

      <article class="the toi gia-su-the">
        <p class="dau">Gia sư</p>
        <div class="than gia-su">
          <p class="hoi">Em chưa biết xét dấu y′.</p>
          <p>Tử số 2(1 − x²) bằng 0 ở đâu? Lấy một số trong mỗi khoảng rồi thử dấu.</p>
          <p class="the-nho"><span>Gợi ý cấp 1 / 3</span><span>Không đưa đáp án</span></p>
        </div>
        <p class="chan"><strong>Gia sư</strong><span>Hỏi khi cần</span></p>
      </article>

      <article class="the toi bang-the">
        <p class="dau">Bảng toán</p>
        <div class="than"><app-minh-hoa-dao-ham /></div>
        <p class="chan"><strong>Đồ thị</strong><span>Kéo điểm để xem</span></p>
      </article>

      <article class="the gv-the">
        <p class="dau">Bảng công thức · Lớp 12A1 thử</p>
        <ul class="than cong-thuc">
          <li><app-katex latex="\\left(\\dfrac{u}{v}\\right)' = \\dfrac{u'v - uv'}{v^{2}}" /><span class="dat">Đạt</span></li>
          <li><app-katex latex="(x^{n})' = n\\,x^{n-1}" /><span class="dat">Đạt</span></li>
          <li><span>Ghi chú: điểm tới hạn</span><span class="cho">Chờ duyệt</span></li>
        </ul>
        <p class="chan"><strong>Giáo viên</strong><span>Kiểm công thức trước khi dùng</span></p>
      </article>

      <article class="the muc-the">
        <p class="dau">Mức hiểu · An</p>
        <div class="than muc">
          <ul>
            @for (k of kyNang; track k.ten) {
              <li>
                <span>{{ k.ten }}</span>
                <span class="thang" aria-hidden="true">
                  @for (m of muc; track m; let i = $index) {
                    <i [class.co]="i <= k.muc"></i>
                  }
                </span>
                <span class="chu-muc">{{ muc[k.muc] }}</span>
              </li>
            }
          </ul>
          <p class="ke"><strong>Bài kế tiếp:</strong> xét dấu y′, vì bước 4 sai hai lần.</p>
        </div>
        <p class="chan"><strong>Mức hiểu</strong><span>Bài kế tiếp có lý do</span></p>
      </article>
    </div>
  `,
  styleUrl: './canh-lam-viec.css',
})
export class CanhLamViec {
  protected readonly muc = MUC;
  protected readonly kyNang = [
    { ten: 'Tính đạo hàm', muc: 1 },
    { ten: 'Xét dấu y′', muc: 0 },
    { ten: 'Kết luận cực trị', muc: 2 },
  ];
}
