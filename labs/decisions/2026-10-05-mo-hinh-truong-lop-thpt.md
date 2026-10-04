# Mô hình trường, năm học, lớp và người dùng cho sản phẩm THPT (lớp 10–12)

| | |
| --- | --- |
| Ngày | 2026-10-05 |
| Người làm | Claude Code, lab Quyết định |
| Trạng thái | Chủ repo giao quyền thiết kế (2026-10-05). Chờ chủ repo xác nhận ADR đi kèm: [015](../../docs/adr/015-mo-hinh-truong-lop-thpt.md) («Đề xuất») |
| Liên quan | `docs/product/MUC-TIEU.md` (Q1, Q4), ADR 006 (dữ liệu tổng hợp), ADR 011 (v2), ADR 012 (quyền riêng tư, «Đề xuất»), #81 (classroom), T058 |

## 1. Câu hỏi quyết định

Từ 2026-10-05, MathL+ là sản phẩm thật cho học sinh THPT lớp 10–12 của đối tác. Giáo viên tự tạo lớp mình quản lý và thêm học sinh. Vậy dữ liệu trường, năm học, lớp, ghi danh, tài khoản và dữ liệu mẫu tổ chức thế nào?

## 2. Dữ kiện

**Chỉ đạo của chủ repo (2026-10-05, nguyên văn):** «từ bây giờ không phải xây dựng một web mà "mẫu thử nghiệm" mà đạt level product thực tế và các seed dữ liệu thực tế nhé, và dữ liệu đối tác có thì họ muốn là người dùng của họ có thể là từ lớp 10 - 12 tức là cấp 3 ở Việt Nam nhé. Người dùng có thể là giáo viên, tạo lớp mà họ quản lý và thêm học sinh ...v.v hay logic thế nào thì tùy có thể tìm hiểu sâu sắc bản LMS mà chúng ta tham khảo để hiểu rõ nhé».

**Hiện trạng v2** (`main` `4d43cb1`):
- `users.role`: `ADMIN`, `SCHOOL_ADMIN`, `TEACHER`, `STUDENT` (V1). Đăng nhập bằng email.
- `classes`: tên, khối 1–12, năm học. `UNIQUE (name, school_year)` là duy nhất trên **toàn hệ thống**, nên hai trường không cùng có «10A1» năm 2026-2027 được. Chưa có khái niệm trường (V3).
- `enrollments`: chỉ mục `enrollments_one_class_per_student_idx` buộc mỗi học sinh **một lớp mãi mãi**. Học sinh không lên lớp năm sau được, không học hai lớp được (V3).
- Lớp chỉ do `LopThuSeeder` tạo («12A1 thử»). Chưa có endpoint tạo lớp hay thêm học sinh (T058 dự kiến).
- Nội dung: một chủ đề Toán 12 (đơn điệu và cực trị). Bộ chấm khung 5 bước chỉ cho chủ đề đó.
- Dữ liệu bài làm, gia sư, mức hiểu đều gắn lớp (data-model).

**LMS tham khảo** (`LMS_hohulili` `34c3f0f2`, chỉ đọc), các mẫu dùng lại được:

| Mẫu | Nơi trong LMS | Dùng cho MathL+ |
| --- | --- | --- |
| Tổ chức (tenant) có loại, bật/tắt | `identity/domain/model/Organization.java`, `OrganizationType` | Trường là tenant: tách dữ liệu đối tác |
| Mã mời 8 ký tự, bảng chữ không có I, O, 0, 1; giới hạn lượt; hạn 1–365 ngày | `identity/domain/model/OrganizationInvite.java` (`createCode`) | Mã lớp và mã mời giáo viên |
| Lời mời email: token ngẫu nhiên lưu băm, dùng một lần, hạn 1–30 ngày | `OrganizationInvite.createEmailInvite`, `AcceptInviteUseCase` | Mời giáo viên vào trường |
| Vai trò `ADMIN`, `ORG_ADMIN`, `TEACHER`, `STUDENT` | `identity/domain/model/Role.java` | Đã có tương đương (`SCHOOL_ADMIN`) |
| Năm học, học kỳ (`academicYear`, `termNumber`) | `academic/domain/model/AcademicTerm.java` | Năm học của trường |
| Thành viên lớp có `ACTIVE`/`INACTIVE`, `joinedAt`, `leftAt` | `academic/domain/model/AcademicClassGroupMembership.java` | Ghi danh có lịch sử, không xóa |
| Lớp `OPEN`/`CLOSED`/`ARCHIVED`/`CANCELLED`, `maxStudents` | `learning_delivery/domain/model/LearningClass.java` | Trạng thái lớp, sĩ số tối đa |
| Giáo viên chính và đồng giảng dạy; không gỡ được giáo viên chính | `learning_delivery/application/usecase/ManageClassTeachersUseCase.java` | Lớp có nhiều giáo viên |
| Thêm học sinh theo email, báo lỗi chung để không lộ email nào có tài khoản (OWASP) | `EnrollStudentByEmailUseCase.java` | Thêm học sinh đã có tài khoản |
| Nhập danh sách Excel: xem trước, đối soát, báo lỗi từng dòng | `fe/.../class-students/add-student-drawer` | Nhập danh sách lớp |

Mô hình học thuật của LMS theo kiểu đại học: khoa, chương trình, khóa, lớp hành chính và lớp học phần (`academic`, `learning_delivery`). Với THPT thì thừa.

**Quy định THPT Việt Nam:**
- Mỗi lớp THPT không quá 45 học sinh. Tỉnh quy định cụ thể theo hướng giảm sĩ số (Thông tư 32/2020/TT-BGDĐT, Điều lệ trường THCS, THPT, hiệu lực 01/11/2020; [văn bản](https://tulieuvankien.dangcongsan.vn/he-thong-van-ban/van-ban-quy-pham-phap-luat/thong-tu-so-322020tt-bgddt-ngay-1592020-cua-bo-giao-duc-va-dao-tao-ban-hanh-dieu-le-truong-trung-hoc-co-so-truong-trung-hoc-6829), truy cập 2026-10-05).
- Luật BVDLCN 91/2025/QH15 (hiệu lực 01/01/2026): xử lý dữ liệu của trẻ em cần đồng ý của người đại diện theo pháp luật; trẻ từ đủ 7 tuổi đồng ý thêm trong một số trường hợp ([Bộ Công an](https://mps.gov.vn/chinh-sach-phap-luat/bai-viet/bao-ve-du-lieu-ca-nhan-trong-mot-so-hoat-dong-1754989261), [LSVN](https://lsvn.vn/bao-ve-du-lieu-ca-nhan-cua-tre-em-theo-luat-bao-ve-du-lieu-ca-nhan-nam-2025-khoang-trong-phap-ly-va-kien-nghi-hoan-thien-a176510.html), truy cập 2026-10-05; độ tin trung bình, chưa đọc nguyên văn điều luật). Học sinh lớp 10 và một phần lớp 11 dưới 16 tuổi. Cách xin đồng ý thuộc ADR 012, đang chờ luật sư.

## 3. Tiêu chí loại

- Dữ liệu học sinh tách theo trường (đối tác): giáo viên trường này không thấy học sinh trường khác.
- Mọi đọc / ghi kiểm quyền theo lớp trong use case (R10). Không lấy «lớp đầu tiên».
- Học sinh không có email vẫn vào được: lớp 10 nhiều em chưa có email riêng.
- Không lưu số định danh cá nhân (CCCD) hay dữ liệu nhạy cảm không cần cho học tập.
- Dữ liệu mẫu không chứa dữ liệu của người thật (ADR 006, AGENTS).

## 4. Tiêu chí chấm (đặt trước khi chấm)

| Tiêu chí | Trọng số |
| --- | --- |
| Đúng thực tế THPT (năm học, khối, lớp, giáo viên) và đủ cho đối tác vận hành | 30 |
| Quyền riêng tư và tách dữ liệu | 25 |
| Giáo viên tự phục vụ được (tạo lớp, thêm học sinh nhanh) | 25 |
| Công sức, rủi ro chuyển đổi từ P2 | 20 |

## 5. Phương án

- **A. Giữ mô hình P2.** Lớp do quản trị tạo; một học sinh một lớp mãi mãi; không có trường.
- **B. Lấy nguyên mô hình đại học của LMS.** Khoa, chương trình, khóa, lớp hành chính, lớp học phần.
- **C. Mô hình THPT tối giản, theo các mẫu của LMS.** Trường (tenant) → năm học → lớp (khối 10/11/12, tên, môn) do giáo viên tạo. Lớp có giáo viên chính và đồng giảng dạy, mã lớp. Ghi danh có lịch sử. Ba cách thêm học sinh (mục 8).
- **D. Lấy giáo viên làm trung tâm, không có trường.** Kiểu lớp học cá nhân: giáo viên nào cũng tạo lớp, học sinh tự đăng ký.

## 6. Chấm điểm (1–5)

| Phương án | Thực tế THPT (30) | Riêng tư (25) | Tự phục vụ (25) | Công sức (20) | Tổng /100 |
| --- | --- | --- | --- | --- | --- |
| A | 1 | 3 | 1 | 5 | 46 |
| B | 3 | 4 | 3 | 1 | 57 |
| C | 5 | 4 | 5 | 3 | 87 |
| D | 2 | 2 | 5 | 4 | 63 |

Lý do các điểm then chốt:
- A: không lên lớp được, không tạo lớp được. Trái chỉ đạo.
- B: khóa, chương trình, lớp học phần không có ở THPT. Công sức lớn cho thứ không dùng.
- C, riêng tư 4: trường tách dữ liệu đối tác. Chưa 5 vì đồng ý của cha mẹ còn chờ ADR 012.
- D, riêng tư 2: không có biên đối tác. Học sinh tự đăng ký đụng ngay chuyện đồng ý của cha mẹ.

## 7. Độ nhạy

Tổng = Σ (trọng số × điểm) / 5. C đứng đầu ở mọi phân bổ thử:
- công sức 40, các tiêu chí khác 20: C 80, D 68;
- riêng tư 40, tự phục vụ 10: C 84, B 60.

## 8. Khuyến nghị: C

**Thực thể** (tên bảng là đề xuất; V8 trở đi):

| Thực thể | Nội dung chính | Ghi chú |
| --- | --- | --- |
| Trường (`schools`) | tên, mã, tỉnh/thành, trạng thái | Tenant. Một đối tác có thể có nhiều trường |
| Thành viên trường (`school_members`) | người dùng, vai trò trong trường (`SCHOOL_ADMIN`, `TEACHER`, `STUDENT`) | Người dùng thuộc một trường |
| Năm học (`school_years`) | nhãn «2026-2027», ngày đầu, ngày cuối | Theo trường |
| Lớp (`classes`, mở rộng) | trường, năm học, khối 10/11/12, tên («10A1»), môn (Toán), trạng thái `OPEN`/`ARCHIVED`, sĩ số tối đa (mặc định 45) | Tên duy nhất trong (trường, năm học, môn) |
| Giáo viên của lớp (`class_teachers`) | `PRIMARY` (người tạo) hay `CO_TEACHER` | Lớp luôn có đúng một giáo viên chính |
| Ghi danh (`enrollments`, mở rộng) | học sinh, `ACTIVE`/`LEFT`, lúc vào, lúc rời, nguồn (`MA_LOP`, `EMAIL`, `DANH_SACH`, `QUAN_TRI`) | Rời lớp giữ dòng; bài làm cũ vẫn thuộc lớp đó |
| Mã lớp | 8 ký tự, bảng chữ không I, O, 0, 1; giáo viên bật / tắt / đổi | Như mã mời của LMS |
| Tài khoản học sinh | tên đăng nhập (khi không có email), mật khẩu tạm, cờ phải đổi mật khẩu, năm sinh (để biết dưới 16 tuổi) | Không lưu CCCD |

**Quyền:**
- `ADMIN` (nền tảng): tạo trường cho đối tác, kèm quản trị trường đầu tiên.
- `SCHOOL_ADMIN`: mời giáo viên, mở năm học, xem mọi lớp của trường, chuyển lớp cho giáo viên khác.
- `TEACHER`: tạo lớp trong trường mình; quản lý lớp mình là giáo viên chính hoặc đồng giảng dạy (thêm, gỡ học sinh; bật mã lớp; mời đồng giảng dạy là giáo viên cùng trường).
- `STUDENT`: vào lớp bằng mã; chỉ thấy dữ liệu của mình, trong các lớp mình đang học.

**Ba cách thêm học sinh:**
1. **Nhập danh sách** (cách chính cho THPT): giáo viên tải tệp CSV / Excel (họ tên, năm sinh, email nếu có). Hệ thống xem trước và báo lỗi từng dòng như LMS, rồi tạo tài khoản: tên đăng nhập sinh theo quy tắc, mật khẩu tạm, phải đổi khi đăng nhập lần đầu. Giáo viên in phiếu đăng nhập phát cho lớp.
2. **Mã lớp:** học sinh đã có tài khoản trong trường nhập mã để vào lớp.
3. **Theo email:** học sinh đã có tài khoản. Báo lỗi chung như LMS, không cho dò email nào có tài khoản.

Học sinh **tự đăng ký** tài khoản bằng mã lớp thì tắt mặc định cho tới khi ADR 012 có cách xin đồng ý của cha mẹ cho học sinh dưới 16 tuổi. Nhà trường tạo tài khoản qua danh sách (cách 1) theo thỏa thuận xử lý dữ liệu với đối tác.

**Năm học mới:** lớp năm cũ chuyển `ARCHIVED`, chỉ đọc. Giáo viên tạo lớp năm mới và ghi danh lại, có lối tắt «lên lớp» chép danh sách từ lớp cũ. Bỏ chỉ mục «một học sinh một lớp»: một học sinh có thể ở nhiều lớp (lớp chính khóa, nhóm bồi dưỡng), mọi dữ liệu học vẫn tính theo lớp.

**Dữ liệu mẫu thực tế (thay «12A1 thử»):**
- Một trường hư cấu, ghi rõ là dữ liệu mẫu: «Trường THPT MathL+ (dữ liệu mẫu)». Năm học 2026-2027.
- 6 lớp: 10A1, 10A2, 11A1, 11A2, 12A1, 12A2. Mỗi lớp 38–42 học sinh (dưới trần 45).
- 1 quản trị trường, 4 giáo viên Toán (mỗi người 1–2 lớp, có đồng giảng dạy).
- Họ tên học sinh sinh tất định từ danh sách họ, tên đệm, tên phổ biến của người Việt, không lấy từ người thật; cờ `synthetic = true`.
- Email ở miền dành riêng `.test` (RFC 2606), không phải miền thật.
- Bài làm, mức hiểu mẫu sinh bằng cách chạy chính luồng chấm trên bài làm mẫu, không ghi thẳng vào bảng.
- Nội dung: danh mục chủ đề và kỹ năng Toán 10, 11, 12 theo Chương trình GDPT 2018, do lab Sư phạm soạn qua bản vá có mã. Bài có chấm từng bước chỉ ở chủ đề đã có bộ chấm.

**Quyết định con cần chủ repo hay đối tác trả lời:**
1. Chủ đề nào làm tiếp sau chủ đề hiện có? Đề xuất: hết chương 1 Toán 12 (giá trị lớn nhất, nhỏ nhất; tiệm cận; khảo sát, đồ thị), rồi đạo hàm Toán 11, rồi hàm số bậc hai Toán 10. Mỗi chủ đề cần khung bước và bộ chấm riêng ở `services/math`.
2. Chủ đề chưa có bộ chấm từng bước thì có dùng câu trắc nghiệm, đúng/sai, trả lời ngắn (chấm theo đáp án, như cấu trúc đề thi tốt nghiệp THPT từ 2025) để phủ đủ chương trình sớm không?
3. Đối tác có danh sách học sinh sẵn: định dạng nào (Excel của phần mềm quản lý trường)? Có cần đồng bộ, hay chỉ nhập một lần mỗi năm?
4. Đăng nhập học sinh: tên đăng nhập + mật khẩu, hay thêm đăng nhập Google hoặc Microsoft của trường?

## 9. Rủi ro và giảm thiểu

| Rủi ro | Giảm thiểu |
| --- | --- |
| Chuyển dữ liệu P2 (lớp «12A1 thử», ràng buộc toàn hệ thống) | Migration mới: thêm trường mặc định cho lớp cũ, đổi ràng buộc duy nhất sang theo trường; giữ nguyên dữ liệu |
| Mật khẩu tạm bị lộ qua phiếu in | Phải đổi khi đăng nhập lần đầu; giáo viên đặt lại được; hết hạn sau 14 ngày nếu chưa dùng |
| Đồng ý của cha mẹ cho học sinh dưới 16 tuổi | Tắt tự đăng ký; ADR 012; lưu năm sinh để biết ai dưới 16 |
| Phạm vi nội dung ba khối rất lớn | Danh mục đủ ba khối trước, bài theo thứ tự ở quyết định con 1 |

## 10. Điều làm quyết định này sai

- Đối tác cần đồng bộ hai chiều với phần mềm quản lý trường → cần cổng tích hợp, xem lại mô hình ghi danh.
- Đối tác bán cho học sinh tự học (B2C), không qua trường → cần tự đăng ký và đồng ý của cha mẹ trong sản phẩm; phương án D trở lại.
