# Seed Trường THPT Nguyễn Trãi

Đây là trường **hư cấu**, năm học **2026–2027**. Toàn bộ người dùng, bài làm, lịch và tình huống duyệt là dữ liệu tổng hợp; tên miền email chỉ dùng làm định danh, không gửi thư. Không có thông tin học sinh thật.

## Bật seed

Chạy PostgreSQL và `services/math`, rồi khởi động core từ `services/core` với profile `dev`. Cấp mật khẩu qua biến môi trường **`MATHL_DEV_SEED_PASSWORD`** từ môi trường chạy của bạn. Không ghi giá trị vào repo, tệp cấu hình hay log.

```bash
pnpm dev:math
# Trong terminal khác, sau khi cấp biến môi trường ở trên:
cd services/core
JAVA_HOME=$HOME/.local/jdk/jdk-25.0.4.1+1 ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

`application-dev.yaml` bật `app.content.import-on-startup` và đọc `../../data`. Container core đóng gói cả `data/dev`, dùng `APP_CONTENT_SOURCE=/app/noi-dung`. Nếu muốn tắt nhập nội dung lúc khởi động, đặt `APP_CONTENT_IMPORT_ON_STARTUP=false`.

Thiếu biến hoặc giá trị chỉ có khoảng trắng: bỏ qua tạo tài khoản, chỉ cảnh báo tên biến. Seed không đổi mật khẩu hoặc nâng quyền của tài khoản đã có. Nếu các tài khoản tổng hợp từ lần chạy trước còn tồn tại, seed có thể bổ sung phần dữ liệu thiếu của các lớp đó.

Mọi runner mới đều có `@Profile("dev")`. Runner nhập snapshot v0 lúc khởi động đã được thay bằng runner trường hư cấu; importer v0 và snapshot vẫn còn để đối chiếu. Profile production không tự nạp ngân hàng này, kể cả khi bật thuộc tính nhập lúc khởi động. Không sửa migration V1–V12; không có migration seed mới.

## Dữ liệu và giới hạn cổng toán

Nguồn ở `data/dev/nguyen-trai/`, dùng các trường của importer hiện có: mã bài, kỹ năng, 4 mức, Bloom, đề, lời giải cấu trúc và sự kiện bảo vệ. `loi_giai` chứa lời giải tự soạn bằng tiếng Việt; `kiem_dinh` chứa dữ kiện để pytest kiểm độc lập. Công thức và tài liệu thuộc nguồn tự soạn của tổ Toán hư cấu, không chép bài SGK.

Có **44 bài**: 8 bài hàm số và 6 bài cho mỗi chương lượng giác, dãy số/cấp số, đạo hàm, tích phân, hình học không gian, xác suất. Mỗi chương có đủ Nhận biết, Thông hiểu, Vận dụng, Vận dụng cao. Bài nâng cao có phân loại số giao điểm theo tham số, tiếp tuyến qua điểm cho trước, cực tiểu diện tích có dấu đổi, phép chiếu tối ưu và xác suất có điều kiện. Kho dùng chung cho ba lớp để giáo viên khảo sát nội dung các khối.

CAS hiện có chấm khung đạo hàm 5 bước. **8 bài hàm số** chỉ phát hành khi qua đủ ba tầng của CAS; **36 bài chương khác giữ `CHO_GIAO_VIEN_DUYET`**, dù lời giải đã qua kiểm SymPy ngoại tuyến. Seed không tự duyệt hoặc giả trạng thái `DAT`. Bảng đạo hàm gồm 6 dòng chỉ khóa sau kiểm hai tầng có trích dẫn. Bảng bổ sung 12 dòng cho sáu chương giữ `NHAP` để giáo viên kiểm tiếp; tài liệu mỗi chương vẫn nằm trong kho lớp. Cần mở rộng CAS ở lane khác để học sinh làm/chấm tự động các dạng mới. Lời giải không đi vào prompt gia sư; cài lớp giữ `offline`, tắt mở lời giải sau nộp.

Mục tiêu trên CSDL mới khi CAS hoạt động đầy đủ:

| Dữ liệu | Số lượng |
| --- | ---: |
| Người dùng tổng hợp | 35: 1 quản trị, 4 giáo viên, 30 học sinh |
| Lớp | 3, mỗi lớp 10 học sinh và 2 giáo viên |
| Tài liệu lớp | 21: 7 ghi chú × 3 lớp |
| Bảng công thức | 3 bảng đạo hàm khóa + 3 bảng chương nháp |
| Lượt giao | 240: 210 ôn tập + 30 kiểm tra |
| Bài đã nộp lịch sử | 210, trải từ khoảng 20 tới 2 ngày trước lúc tạo lớp |
| Bài chưa kiểm được, chờ giáo viên xem | 6, có cảnh báo `NHO_GV` |
| Sự kiện mức hiểu | 204; bỏ qua 6 bài chưa kiểm được |
| Trạng thái mức hiểu | 30, tính BKT thật, có đủ 4 mức và trường hợp kẹt |
| Lịch học | 120 phiên, 45 phút/phiên |
| Gợi ý chờ duyệt | 3; ngoài ra 36 bài mới/lớp chờ duyệt nội dung |

Bộ ôn gồm củng cố, luyện tập và chuyên đề nâng cao. Bài kiểm tra “Kiểm tra hàm số — 45 phút” có chế độ `KIEM_TRA`, hạn 12 giờ từ lần giao đầu; bài ôn có hạn 7 ngày. Đây là tên bộ bài, không phải cơ chế bấm giờ thi 45 phút. Bài ôn được ghi thời điểm giao trước lịch sử học (21 ngày trước), còn hạn 7 ngày từ lần nạp đầu. Bài kiểm tra chưa có bài nộp lịch sử để nhắc hạn hiện trong app. Lịch gần nhất bắt đầu sau 2 giờ từ lúc tạo lớp, tạo nhắc in-app qua API lịch/nhắc hiện có; không gửi thông báo bên ngoài.

`bai-lam-lich-su.json` gồm các payload học sinh hư cấu đúng/sai/chưa đọc được. Seeder gọi CAS để lấy phán quyết thật, lưu bước/bảng và căn cứ chấm, rồi tính BKT bằng domain hiện có. Không gọi CAS thành công thì không dựng kết quả lịch sử. Mức hiểu của các chương chưa có bài nộp không bị giả tạo.

## Tính idempotent và dữ liệu đã tồn tại

UUID lớp, bài lịch sử, lịch và cảnh báo được suy từ namespace seed ổn định. Lớp trùng tên/năm nhưng khác UUID, hoặc có thành viên không tổng hợp/vai trò sai, bị bỏ qua. Tài khoản trùng không bị cập nhật; học sinh thuộc lớp khác không được kéo về lớp seed. SQL chỉ chạy trên đúng UUID lớp được guard xác nhận.

Chạy lại giữ id, mật khẩu, hạn, mode, bài đã nộp, lịch hoàn thành, quyết định duyệt và tiến độ đã có. Importer giữ phiên bản khi nội dung không đổi; bảng nháp giáo viên đã sửa không bị ghi đè. Các lịch/hạn neo vào lần tạo/giao đầu, không tự dời để luôn có nhắc mới. Mastery bootstrap chỉ chạy với học sinh chưa có trạng thái mức hiểu; nếu một phần lịch sử thiếu do CAS lỗi nhưng tiến độ đã tồn tại, seed không backfill để tránh ghi đè tiến độ đang học. Bảng nháp seed chưa được ai sửa có thể được importer dùng lại để khóa phiên bản đạo hàm mới khi nguồn đổi; bản giáo viên đã sửa hoặc chép giữ nguyên. Không tự xóa tài khoản/lớp seed cũ trong CSDL đang dùng; muốn thử lại từ đầu, dùng CSDL hoặc schema dev mới.

## Danh sách lớp và tài khoản

| Lớp | Khối | Giáo viên chính | Giáo viên cùng dạy |
| --- | ---: | --- | --- |
| 10A1 | 10 | Trần Thu Hương | Hoàng Minh Đức |
| 11A2 | 11 | Lê Văn Duy | Hoàng Minh Đức |
| 12A3 | 12 | Phạm Ngọc Lan | Hoàng Minh Đức |

### Quản trị

| Họ tên | Email | Lớp |
| --- | --- | --- |
| Nguyễn Thanh Tùng | `tung.nguyen.thanh@thpt-nguyentrai.edu.vn` | Toàn trường |

### Giáo viên

| Họ tên | Email | Lớp |
| --- | --- | --- |
| Trần Thu Hương | `huong.tran.thu@thpt-nguyentrai.edu.vn` | 10A1 |
| Lê Văn Duy | `duy.le.van@thpt-nguyentrai.edu.vn` | 11A2 |
| Phạm Ngọc Lan | `lan.pham.ngoc@thpt-nguyentrai.edu.vn` | 12A3 |
| Hoàng Minh Đức | `duc.hoang.minh@thpt-nguyentrai.edu.vn` | 10A1, 11A2, 12A3 |

### Học sinh

| Họ tên | Email | Lớp |
| --- | --- | --- |
| Nguyễn Minh An | `an.nguyen.minh@thpt-nguyentrai.edu.vn` | 10A1 |
| Trần Gia Bảo | `bao.tran.gia@thpt-nguyentrai.edu.vn` | 10A1 |
| Lê Ngọc Châu | `chau.le.ngoc@thpt-nguyentrai.edu.vn` | 10A1 |
| Phạm Anh Dũng | `dung.pham.anh@thpt-nguyentrai.edu.vn` | 10A1 |
| Hoàng Hà Giang | `giang.hoang.ha@thpt-nguyentrai.edu.vn` | 10A1 |
| Đặng Quỳnh Hoa | `hoa.dang.quynh@thpt-nguyentrai.edu.vn` | 10A1 |
| Vũ Đức Huy | `huy.vu.duc@thpt-nguyentrai.edu.vn` | 10A1 |
| Bùi Khánh Linh | `linh.bui.khanh@thpt-nguyentrai.edu.vn` | 10A1 |
| Đỗ Tuấn Minh | `minh.do.tuan@thpt-nguyentrai.edu.vn` | 10A1 |
| Ngô Bảo Ngọc | `ngoc.ngo.bao@thpt-nguyentrai.edu.vn` | 10A1 |
| Nguyễn Hải Anh | `anh.nguyen.hai@thpt-nguyentrai.edu.vn` | 11A2 |
| Trần Minh Bình | `binh.tran.minh@thpt-nguyentrai.edu.vn` | 11A2 |
| Lê Phương Chi | `chi.le.phuong@thpt-nguyentrai.edu.vn` | 11A2 |
| Phạm Quang Đạt | `dat.pham.quang@thpt-nguyentrai.edu.vn` | 11A2 |
| Hoàng Thu Hà | `ha.hoang.thu@thpt-nguyentrai.edu.vn` | 11A2 |
| Đặng Gia Hân | `han.dang.gia@thpt-nguyentrai.edu.vn` | 11A2 |
| Vũ Thành Khang | `khang.vu.thanh@thpt-nguyentrai.edu.vn` | 11A2 |
| Bùi Thảo My | `my.bui.thao@thpt-nguyentrai.edu.vn` | 11A2 |
| Đỗ Minh Quân | `quan.do.minh@thpt-nguyentrai.edu.vn` | 11A2 |
| Ngô Nhật Vy | `vy.ngo.nhat@thpt-nguyentrai.edu.vn` | 11A2 |
| Nguyễn Hoài Nam | `nam.nguyen.hoai@thpt-nguyentrai.edu.vn` | 12A3 |
| Trần Bảo Nhi | `nhi.tran.bao@thpt-nguyentrai.edu.vn` | 12A3 |
| Lê Hồng Phúc | `phuc.le.hong@thpt-nguyentrai.edu.vn` | 12A3 |
| Phạm Khánh Sơn | `son.pham.khanh@thpt-nguyentrai.edu.vn` | 12A3 |
| Hoàng Minh Tâm | `tam.hoang.minh@thpt-nguyentrai.edu.vn` | 12A3 |
| Đặng Ngọc Thảo | `thao.dang.ngoc@thpt-nguyentrai.edu.vn` | 12A3 |
| Vũ Anh Thư | `thu.vu.anh@thpt-nguyentrai.edu.vn` | 12A3 |
| Bùi Quốc Việt | `viet.bui.quoc@thpt-nguyentrai.edu.vn` | 12A3 |
| Đỗ Hải Yến | `yen.do.hai@thpt-nguyentrai.edu.vn` | 12A3 |
| Ngô Tuấn Vũ | `vu.ngo.tuan@thpt-nguyentrai.edu.vn` | 12A3 |

## Kiểm tra

```bash
/workspace/du_an_em_linh/services/math/.venv/bin/pytest services/math/tests/test_seed_nguyen_trai.py
(cd services/core && JAVA_HOME=$HOME/.local/jdk/jdk-25.0.4.1+1 ./mvnw -B -q verify)
# Opt-in khi có quyền socket tới PostgreSQL và CAS:
(cd services/core && MATHL_SEED_INTEGRATION=1 JAVA_HOME=$HOME/.local/jdk/jdk-25.0.4.1+1 ./mvnw -B -q -Dtest=NguyenTraiSeedIntegrationTest test)
```

Test tích hợp dùng schema `seed_test_<uuid>` riêng do Flyway tạo, kiểm các số lượng trên, snapshot trước/sau chạy lại, và guard khi một thành viên thôi là tài khoản tổng hợp. Test dọn schema đó sau khi chạy. Không sửa dữ liệu schema hiện hành.

Các kiểm tra unit bao phủ thiếu mật khẩu, trùng tài khoản, đủ roster, profile production và nhập lại ngân hàng. Pytest kiểm từng phép biến đổi, đáp án từ dữ kiện gốc, mẫu bài làm lịch sử, cổng khóa bảng và cổng phát hành thực tế.

### Bằng chứng phiên 2026-10-13

SHA nền: `c96924064f9e2ff699a95cf1446fa3ab94f6c10e` (diff chưa commit).

- `/workspace/du_an_em_linh/services/math/.venv/bin/pytest -o addopts= services/math/tests/test_seed_nguyen_trai.py -q --tb=short`: **126 passed**, exit 0. Bao gồm cổng CAS thật chạy trực tiếp bằng Python, không HTTP.
- Core `verify` với `JAVA_HOME` JDK đã chỉ định, cấp Mockito javaagent qua `JAVA_TOOL_OPTIONS`, và `-Dtest=*,!MathServiceClientTest`: **433 cases, 257 passed, 176 skipped, 0 failures/errors**, exit 0; build và ArchUnit qua. Các test Testcontainers và test tích hợp opt-in bị skip.
- Lệnh `verify` đầy đủ đã được thử: sandbox chặn cơ chế Mockito tự attach; cấp javaagent giải quyết phần đó nhưng `MathServiceClientTest.start` vẫn lỗi socket `Operation not permitted`. Không coi cổng đầy đủ là đạt.
- `MATHL_SEED_INTEGRATION=1 … -Dtest=NguyenTraiSeedIntegrationTest test`: không khởi động được vì JDBC tới localhost bị sandbox chặn socket. Các số lượng CSDL và idempotency PostgreSQL trong bảng trên **chưa được xác minh ở phiên này**. Test tích hợp đã có để chạy trên môi trường cho phép socket.
- `git diff --check`: exit 0; không thay đổi ngoài lane, không đổi migration V1–V12.
