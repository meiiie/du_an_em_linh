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

Có **68 bài, 13 chủ đề**, mỗi chủ đề đủ bốn mức. Khối 10 có hàm số bậc hai, mệnh đề/tập hợp, thống kê và xác suất cơ bản; khối 11 có lượng giác, dãy số/cấp số, đạo hàm, hình học không gian và xác suất; khối 12 có đơn điệu/cực trị, tích phân, hình học không gian/Oxyz và xác suất có điều kiện. Ngân hàng vẫn dùng chung để giáo viên khảo sát; bài giao chỉ thuộc đúng khối lớp.

`de_bai.latex` là bản hiển thị LaTeX riêng, sinh bằng `sympy.latex` khi có biểu thức; `ham_so_sympy` và `kiem_dinh.du_kien` giữ dạng máy. Đề nhúng toán trong `$...$`. Pytest kiểm đáp án từng bài và đối chiếu bản hiển thị với SymPy, không cần cài `parse_latex`/ANTLR.

CAS hiện có chấm khung đạo hàm 5 bước. **8 bài hàm số** phát hành sau cổng CAS; **55 bài khác** có quyết định giáo viên hư cấu duyệt qua `VerificationRun.approve`, ghi `content_reviews.reviewer_id` bằng giáo viên tổng hợp của lớp. Không đổi các tầng CAS sang `DAT`. **5 bài/lớp** giữ chờ rà soát lập luận sư phạm: lượng giác, cấp số nhân, tiếp tuyến, cực tiểu diện tích và phép chiếu. Quyết định duyệt seed căn cứ vào `test_teacher_solution`; pytest kiểm cả các bài vẫn chờ rà soát. Cách này chỉ nằm trong runner dev.

Bảng đạo hàm gồm 6 dòng khóa sau kiểm hai tầng có trích dẫn. Bảng bổ sung 12 dòng giữ `NHAP`. Gia sư giữ `offline`, tắt mở lời giải sau nộp. Các dạng ngoài khung đạo hàm chưa được chấm tự động khi học sinh làm bài mới.

Mục tiêu trên CSDL mới khi CAS hoạt động đầy đủ (số lượng CSDL chưa được kiểm trong sandbox):

| Dữ liệu | Số lượng |
| --- | ---: |
| Người dùng tổng hợp | 35: 1 quản trị, 4 giáo viên, 30 học sinh |
| Lớp | 3, mỗi lớp 10 học sinh và 2 giáo viên |
| Bài ngân hàng | 68; mỗi lớp có 63 bài phát hành và 5 bài chờ duyệt |
| Quyết định duyệt nội dung | 165: 55 × 3 lớp |
| Tài liệu lớp | 21: 7 ghi chú × 3 lớp |
| Bảng công thức | 3 bảng đạo hàm khóa + 3 bảng chương nháp |
| Lượt giao | 630: khối 10 có 160, khối 11 có 240, khối 12 có 230 |
| Bài đã nộp lịch sử | 620; 10 lượt kiểm tra hàm số chưa nộp |
| Bài chưa kiểm được | 13, có cảnh báo `NHO_GV` |
| Sự kiện mức hiểu | 607; bỏ qua 13 bài chưa kiểm được |
| Trạng thái mức hiểu | 130, tính BKT thật trên kỹ năng đúng khối |
| Lịch học | 120 phiên, 45 phút/phiên |
| Gợi ý chờ duyệt | 3 |

Tên bộ bài là “Ôn tập đạo hàm – tuần 6”, “Ôn tập tích phân – tuần 6”, v.v. Bài kiểm tra “Kiểm tra hàm số — 45 phút” chỉ giao cho lớp 12, mode `KIEM_TRA`, hạn 12 giờ từ lần giao đầu; tên không tạo cơ chế bấm giờ. Bài ôn có hạn 7 ngày. Thời điểm giao và lịch neo vào lần tạo lớp; chạy lại không dời hạn.

`bai-lam-lich-su.json` gồm payload tổng hợp đúng/sai/chưa đọc được. Bài khung 5 bước lấy phán quyết CAS thật; bài chương khác có `giao_vien_cham=true`, đáp án học sinh được pytest đối chiếu với lời giải đã kiểm, lưu căn cứ `GV_DUYET_SEED` và giáo viên chấm trong `grading_results.normalization.reviewer_id`. Đây là lịch sử giáo viên chấm tổng hợp, không phải phán quyết CAS. Các lượt chưa kiểm được không tác động BKT.

Kế hoạch luyện gồm sai trước rồi đúng ở cuối, dùng BKT domain và làm tròn PostgreSQL `real` giữa các lượt. Kiểm thử domain xác nhận khối 10/12 có **14/12/10/4** trạng thái (35/30/25/10%), khối 11 có **18/15/12/5** (36/30/24/10%). Adapter đọc seed `WorkspaceNguyenTraiSeedAdapter` chỉ bật profile dev, chỉ áp cho lớp UUID seed qua guard: tổng quan đếm kỹ năng đúng khối, danh sách bài giao dùng tên bộ bài. Lớp khác dùng adapter hiện hành.

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

### Bằng chứng phiên SEED2, 2026-10-13

SHA nền: `82795de6954e3a2b3116c276e6bef5f06b6c5d5e` (diff chưa commit).

- `/workspace/du_an_em_linh/services/math/.venv/bin/pytest services/math/tests/test_seed_nguyen_trai.py`: **408 passed**, exit 0; kiểm đáp án, LaTeX, lịch sử, cổng bảng và cổng phát hành bằng Python trực tiếp.
- `JAVA_HOME=$HOME/.local/jdk/jdk-25.0.4.1+1 ./mvnw -B -q -o test-compile` từ `services/core`: exit 0.
- Core unit/ArchUnit: **23 tests passed**, exit 0, chọn `PhanBoMucHieuSeedTest,NoiDungNguyenTraiTest,BangChuongSeedTest,SeedProfileTest,CleanArchitectureTest,DddArchitectureTest,NullMarkedPackagesTest` với Maven offline `test`, JDK đã chỉ định và Mockito javaagent. Kiểm BKT phát lại dữ liệu seed bằng domain, đối chiếu phân bố từng khối; test profile xác nhận adapter đọc seed không bật ở production.
- Sandbox không cho mở socket: không kết nối PostgreSQL/CAS HTTP, chưa xác minh số lượng hoặc idempotency trên PostgreSQL. Test tích hợp opt-in có assertion số lượng, khối giao bài, quyết định duyệt, tổng quan và snapshot chạy lại.
- Không sửa migration V1–V12, không commit.
