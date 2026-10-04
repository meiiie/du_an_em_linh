# ADR 015 — Mô hình trường, năm học, lớp và người dùng cho sản phẩm THPT

**Trạng thái:** Đề xuất (2026-10-05). Chủ repo giao quyền thiết kế («logic thế nào thì tùy», 2026-10-05); chờ chủ repo xác nhận. Phân tích: [`labs/decisions/2026-10-05-mo-hinh-truong-lop-thpt.md`](../../labs/decisions/2026-10-05-mo-hinh-truong-lop-thpt.md).

## Bối cảnh

Từ 2026-10-05, MathL+ là sản phẩm thật cho học sinh THPT lớp 10–12 của đối tác, không còn là mẫu thử. Giáo viên tự tạo lớp mình quản lý và thêm học sinh. Dữ liệu mẫu phải sát thực tế.

Mô hình P2 không đáp ứng được:
- lớp do quản trị tạo, không có khái niệm trường;
- tên lớp duy nhất trên toàn hệ thống;
- mỗi học sinh một lớp mãi mãi (V3).

LMS tham khảo có sẵn các mẫu: tổ chức làm tenant, mã mời, giáo viên chính và đồng giảng dạy, ghi danh có lịch sử, nhập danh sách có xem trước. Nhưng mô hình học thuật của nó theo kiểu đại học.

## Quyết định

1. **Trường là tenant.** Người dùng thuộc một trường. Dữ liệu tách theo trường. `ADMIN` nền tảng tạo trường cho đối tác, kèm quản trị trường đầu tiên (`SCHOOL_ADMIN`).
2. **Năm học theo trường.** Lớp gắn (trường, năm học, khối 10/11/12, môn). Tên lớp duy nhất trong (trường, năm học, môn). Sĩ số tối đa mặc định 45 (Thông tư 32/2020/TT-BGDĐT).
3. **Giáo viên tự tạo lớp** trong trường mình và là giáo viên chính. Mời giáo viên cùng trường làm đồng giảng dạy. Lớp luôn có đúng một giáo viên chính. Quản trị trường xem mọi lớp và chuyển được giáo viên chính.
4. **Ghi danh có lịch sử.** Học sinh `ACTIVE` hay `LEFT`, kèm lúc vào, lúc rời và nguồn. Không xóa dòng ghi danh. Một học sinh có thể ở nhiều lớp. Mọi dữ liệu học (bài làm, gia sư, mức hiểu) vẫn tính theo lớp.
5. **Thêm học sinh bằng ba cách:**
   - nhập danh sách (CSV / Excel): xem trước, báo lỗi từng dòng, tạo tài khoản có tên đăng nhập và mật khẩu tạm, phải đổi khi đăng nhập lần đầu, có phiếu in;
   - mã lớp 8 ký tự (bảng chữ không I, O, 0, 1), giáo viên bật, tắt, đổi;
   - email của tài khoản đã có, báo lỗi chung để không dò được email.
6. **Không tự đăng ký** tài khoản học sinh cho tới khi ADR 012 có cách xin đồng ý của cha mẹ cho học sinh dưới 16 tuổi (Luật BVDLCN 91/2025/QH15). Lưu năm sinh, không lưu số định danh cá nhân.
7. **Năm học mới:** lớp cũ `ARCHIVED`, chỉ đọc. Có lối tắt chép danh sách sang lớp năm mới.
8. **Dữ liệu mẫu thực tế nhưng tổng hợp:**
   - một trường hư cấu ghi rõ là dữ liệu mẫu, năm học 2026-2027, 6 lớp từ 10A1 tới 12A2, mỗi lớp 38–42 học sinh;
   - quản trị trường và giáo viên Toán;
   - họ tên Việt sinh tất định từ danh sách phổ biến, cờ `synthetic`, email ở miền `.test`;
   - dữ liệu học sinh tạo bằng chính luồng sản phẩm, không ghi thẳng vào bảng;
   - nội dung là danh mục Toán 10–12 theo Chương trình GDPT 2018, qua lab Sư phạm.

## Hệ quả

- Migration mới (V8 trở đi): `schools`, `school_members`, `school_years`, `class_teachers`; mở rộng `classes`, `enrollments`; bỏ chỉ mục «một học sinh một lớp»; chuyển lớp P2 về một trường mặc định.
- Cổng `ClassMembership` (classroom) thêm kiểm theo trường; mọi module khác giữ nguyên cách gọi.
- Endpoint giáo viên và quản trị trường (T058 mở rộng), màn hình quản lý lớp ở `apps/frontend`.
- `TaiKhoanThuSeeder`, `LopThuSeeder` thay bằng bộ dữ liệu mẫu mới. Tài khoản thử của AGENTS đổi theo; e2e v0 giữ nguyên.
- Thứ tự chủ đề lớp 10–12 và dạng câu cho chủ đề chưa có bộ chấm từng bước: chờ chủ repo hay đối tác (mục 8 của bản phân tích).

## Điều làm quyết định này sai

- Đối tác cần đồng bộ hai chiều với phần mềm quản lý trường → thêm cổng tích hợp, xem lại ghi danh.
- Đối tác bán thẳng cho học sinh (B2C) → cần tự đăng ký và đồng ý của cha mẹ trong sản phẩm.
