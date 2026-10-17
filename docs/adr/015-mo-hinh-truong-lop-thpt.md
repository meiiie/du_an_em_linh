# ADR 015 — Mô hình trường, năm học, lớp và người dùng cho sản phẩm THPT

**Trạng thái:** Đề xuất (2026-10-05). Chủ repo giao quyền thiết kế («logic thế nào thì tùy», 2026-10-05); chờ chủ repo xác nhận. Phân tích: [`labs/decisions/2026-10-05-mo-hinh-truong-lop-thpt.md`](../../labs/decisions/2026-10-05-mo-hinh-truong-lop-thpt.md). Đã qua rà độc lập `privacy-reviewer` (3 mục chặn, đã sửa trong bản này).

## Bối cảnh

Từ 2026-10-05, MathL+ là sản phẩm thật cho học sinh THPT lớp 10–12 của đối tác, không còn là mẫu thử. Giáo viên tự tạo lớp mình quản lý và thêm học sinh. Dữ liệu mẫu phải sát thực tế.

Mô hình P2 không đáp ứng được:
- lớp do quản trị tạo, không có khái niệm trường;
- tên lớp duy nhất trên toàn hệ thống;
- mỗi học sinh một lớp mãi mãi (V3);
- cổng `ClassMembership.lopHoc(hocSinh)` suy ra «lớp duy nhất» của học sinh.

Người học là trẻ vị thành niên (MUC-TIEU §5.4, Hiến chương III). ADR 012 («Đề xuất») đặt điều kiện trước khi có học sinh thật: bản ghi đồng ý kiểm chứng được của cả phụ huynh và học sinh cho mọi học sinh dưới 18 tuổi, hạn lưu, xóa theo yêu cầu.

## Quyết định

1. **Học sinh thật chỉ vào sau ADR 012.** Mọi đường tạo hay thêm học sinh (mục 6) chỉ dùng cho trường thật khi đã có: ADR 012 được chấp nhận, thỏa thuận xử lý dữ liệu với trường, hồ sơ đánh giá tác động. Trước đó chỉ có trường `synthetic`. Học sinh có trạng thái đồng ý theo mục đích (`HOC_TAP`, `GIA_SU_AI`, `NHAC_LICH_NGOAI`). Máy chủ chặn mọi lần ghi dữ liệu học khi chưa có `HOC_TAP`, và chặn mọi lần gọi nhà AI khi chưa có `GIA_SU_AI`. Ngưỡng tuổi theo ADR 012 (hiện là dưới 18).
2. **Trường là tenant.** Người dùng là thành viên của một trường, có một vai trò trong trường. Vai trò tài khoản và vai trò trong trường không được lệch nhau. Dữ liệu tách theo trường ở cả tầng ứng dụng và CSDL: khóa ngoại ghép để không ghi danh được chéo trường, rồi RLS theo ADR 012. Id của trường khác trả 404 như không có. `ADMIN` nền tảng tạo trường cho đối tác, kèm quản trị trường đầu tiên. `ADMIN` không đọc dữ liệu học.
3. **Năm học theo trường.** Lớp gắn (trường, năm học, khối 10/11/12, môn). Tên lớp duy nhất trong (trường, năm học, môn). Sĩ số tối đa mặc định 45 (Thông tư 32/2020/TT-BGDĐT).
4. **Giáo viên tự tạo lớp** trong trường mình và là giáo viên chính. Lớp luôn có đúng một giáo viên chính. Chỉ giáo viên chính hoặc quản trị trường mời, gỡ đồng giảng dạy (giáo viên cùng trường). Đồng giảng dạy không mời tiếp, không đặt lại mật khẩu. Quản trị trường chuyển được giáo viên chính.
5. **Ma trận quyền.**
   - Quản trị trường thấy lớp, sĩ số, giáo viên và số liệu tổng hợp. Muốn xem nội dung học của một học sinh (bài làm, câu hỏi gia sư) thì phải nhập lý do; có nhật ký.
   - Giáo viên chỉ thao tác trên lớp mình là giáo viên chính hoặc đồng giảng dạy.
   - Học sinh chỉ thấy dữ liệu của mình trong các lớp đang học, và thấy danh sách giáo viên của lớp.
   - Mọi kiểm quyền nhận **id lớp tường minh**, chỉ tính ghi danh `ACTIVE` trong lớp cùng trường với người gọi. Lớp `ARCHIVED` chỉ đọc.
6. **Thêm học sinh** (chỉ học sinh của trường), ba cách:
   - **Nhập danh sách CSV / Excel.** Chỉ nhận ba cột: họ tên, năm sinh, email (tùy chọn); cột khác bị bỏ ngay khi đọc, không hiện ở bước xem trước. Xử lý trong bộ nhớ, không lưu tệp; chặn XXE và zip bomb. Log chỉ ghi số dòng và mã lỗi. Mỗi dòng tạo một tài khoản:
     - tên đăng nhập duy nhất trong trường, không chứa năm sinh;
     - mật khẩu tạm sinh bằng `SecureRandom`, ít nhất 10 ký tự, mỗi em một mật khẩu, chỉ hiện một lần, lưu băm, hết hạn sau 14 ngày;
     - phiên đăng nhập bằng mật khẩu tạm chỉ gọi được đổi mật khẩu.

     Giáo viên in phiếu ngay lúc tạo. Phiếu không có năm sinh, không lưu trên máy chủ. In lại nghĩa là đặt lại mật khẩu.
   - **Mã lớp.** 8 ký tự, bảng chữ không có I, O, 0, 1, sinh bằng `SecureRandom`. Mặc định tắt. Khi bật, mã có hạn (mặc định 7 ngày) và trần lượt bằng số chỗ còn trống. Chỉ học sinh cùng trường dùng được. Mọi lỗi trả cùng một thông điệp; có giới hạn số lần thử theo tài khoản và IP. Giáo viên thấy ai vào bằng mã và gỡ được.
   - **Lời mời theo email** cho học sinh đã có tài khoản cùng trường. Học sinh tự chấp nhận qua token lưu băm, dùng một lần, có hạn. Mọi trường hợp trả cùng một thông điệp, không dò được email nào có tài khoản. Không ghi email vào log.
7. **Không có đường tự đăng ký** tài khoản học sinh. Muốn mở (B2C) thì viết ADR mới, không mở bằng cấu hình.
8. **Tài khoản và đăng nhập.**
   - `email` được để trống (học sinh không có email đăng nhập bằng tên đăng nhập); không bịa email.
   - Giới hạn số lần sai theo cả tài khoản lẫn email + IP.
   - Chỉ giáo viên chính hoặc quản trị trường đặt lại được mật khẩu, với học sinh `ACTIVE` của lớp. Đặt lại thì thu hồi mọi phiên; học sinh được báo lúc đăng nhập.
   - Lưu năm sinh, không lưu số định danh cá nhân. Quy tắc đóng mặc định: coi là dưới N tuổi khi «năm hiện tại − năm sinh ≤ N»; thiếu năm sinh thì coi là dưới 16.
9. **Vòng đời và lưu trữ.** Trong vận hành thường (rời lớp, lên lớp, đóng năm học) không xóa dữ liệu. Khi hết hạn lưu hay có yêu cầu xóa theo ADR 012 thì xóa hoặc giả danh hóa, có nhật ký.
   - Rời lớp (`LEFT`): giáo viên còn xem tới hết năm học, rồi mất quyền.
   - Lớp `ARCHIVED`: bắt đầu tính hạn lưu. Giáo viên không xóa được lớp, chỉ lưu trữ.
   - Rời trường hay nghỉ việc: trong cùng giao dịch, khóa thành viên, thu hồi phiên, gỡ mọi vai trò lớp, ghi nhật ký.
   - Chuyển trường: tạo thành viên mới ở trường mới; dữ liệu cũ ở lại trường cũ theo hạn lưu.
   - Lên lớp năm mới: chỉ chép ghi danh `ACTIVE` còn đồng ý hiệu lực; giáo viên xem trước khi chép.
10. **Nhật ký kiểm toán** (chỉ ghi id):
    - tạo tài khoản hàng loạt, in phiếu, đặt lại mật khẩu;
    - thêm hay gỡ học sinh (`added_by`, `removed_by`), thêm hay gỡ đồng giảng dạy, chuyển giáo viên chính;
    - đổi mã lớp;
    - quản trị trường xem nội dung học.
11. **Nhà AI theo trường.** Trường có cờ `synthetic` và danh sách nhà AI được phép. Kiểm lúc lưu cài lớp và lúc gọi. Z.AI và OpenRouter chỉ cho trường `synthetic` (ADR 012). Tên trường, tên lớp, tên đăng nhập đưa vào bộ khử định danh; prompt gia sư không chứa chúng.
12. **Dữ liệu mẫu thực tế nhưng tổng hợp.**
    - Một trường hư cấu, `synthetic`, năm học 2026-2027, 6 lớp từ 10A1 tới 12A2, mỗi lớp 38–42 học sinh, kèm quản trị trường và giáo viên Toán.
    - Họ tên Việt sinh tất định từ danh sách phổ biến. Email ở miền cố định `truong-mau.test`.
    - Dữ liệu học sinh tạo bằng chính luồng sản phẩm với nhà `offline`.
    - Chỉ chạy ở profile `dev` / `demo`, và không chạy khi CSDL có trường không `synthetic`.
    - Nội dung là danh mục Toán 10–12 theo Chương trình GDPT 2018, qua lab Sư phạm.

## Hệ quả

- Migration mới (V8 trở đi):
  - bảng mới `schools`, `school_members`, `school_years`, `class_teachers`, `class_join_codes`, `consents`, `audit_events`;
  - mở rộng `classes` và `enrollments` (trạng thái, nguồn, người thêm / gỡ), `users` (tên đăng nhập, email được trống, cờ phải đổi mật khẩu, năm sinh);
  - khóa ngoại ghép trường – lớp – thành viên; bỏ chỉ mục «một học sinh một lớp»;
  - chuyển lớp P2 về một trường `synthetic` mặc định.
- **Đổi hợp đồng cổng `ClassMembership`** (không giữ nguyên cách gọi):
  - bỏ `lopHoc(hocSinh)`, `caiDatChoHocSinh(hocSinh)`, `giaoVienDayHocSinh(gv, hs)`; mọi hàm nhận id lớp, chỉ tính `ACTIVE` cùng trường;
  - sửa các nơi đang gọi (`CanhBaoGiaoVienService`; API học sinh của practice đổi sang `/api/hs/lop/{lopId}/…`, #87);
  - test IDOR cho: học sinh ở hai lớp, lớp năm cũ, học sinh `LEFT`, giáo viên đã bị gỡ, id lớp của trường khác.
- Endpoint giáo viên và quản trị trường (T058 mở rộng), màn hình quản lý lớp ở `apps/frontend` (thiết kế qua lab Thiết kế).
- `TaiKhoanThuSeeder`, `LopThuSeeder` thay bằng bộ dữ liệu mẫu mới. Tài khoản thử trong AGENTS đổi theo; e2e v0 giữ nguyên.
- Còn chờ chủ repo hay đối tác (mục 8 của bản phân tích):
  - thứ tự chủ đề lớp 10–12;
  - dạng câu cho chủ đề chưa có bộ chấm từng bước;
  - tiêu đề cột danh sách của đối tác: chỉ nhận tiêu đề kèm dòng giả, không nhận tệp thật;
  - đăng nhập một lần (SSO) của trường.
- Còn chờ luật sư (qua ADR 012):
  - trường tạo tài khoản có cần đồng ý của cha mẹ không;
  - quyền của quản trị trường với dữ liệu học sinh;
  - hạn lưu sau khi rời trường;
  - chuyển dữ liệu giữa hai trường.

## Điều làm quyết định này sai

- Đối tác cần đồng bộ hai chiều với phần mềm quản lý trường → thêm cổng tích hợp, xem lại ghi danh.
- Đối tác bán thẳng cho học sinh (B2C) → cần ADR mới: tự đăng ký và đồng ý của cha mẹ trong sản phẩm.
