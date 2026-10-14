# Seed Trường THPT Nguyễn Trãi

Đây là trường **hư cấu**, năm học **2026–2027**. Toàn bộ người dùng, bài làm, lịch và tình huống duyệt là dữ liệu tổng hợp; tên miền email chỉ dùng làm định danh, không gửi thư. Không có thông tin học sinh thật.

## Bật seed

Chạy PostgreSQL và `services/math`, rồi khởi động core từ `services/core` với profile `dev`. Có thể cấp mật khẩu qua biến môi trường **`MATHL_DEV_SEED_PASSWORD`** từ môi trường chạy của bạn; nếu thiếu hoặc chỉ có khoảng trắng, core tự sinh mật khẩu ngẫu nhiên. Không ghi giá trị vào repo, tệp cấu hình hay log.

```bash
pnpm dev:math
# Trong terminal khác, sau khi cấp biến môi trường ở trên:
cd services/core
JAVA_HOME=$HOME/.local/jdk/jdk-25.0.4.1+1 ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

`application-dev.yaml` bật `app.content.import-on-startup` và đọc `../../data`. Compose gắn `./data/dev` chỉ đọc vào `/app/noi-dung/dev`; image production không chứa seed hư cấu. Container dùng `APP_CONTENT_SOURCE=/app/noi-dung`. Nếu muốn tắt nhập nội dung lúc khởi động, đặt `APP_CONTENT_IMPORT_ON_STARTUP=false`.

Core lưu mật khẩu (tự sinh hoặc được cấp) trong `/tmp/mathl-dev-seed-password`, quyền `0600`, không ghi vào log. Đổi đường dẫn bằng `MATHL_DEV_SEED_PASSWORD_FILE`; thư mục cha phải tồn tại và UID chạy core (1001 trong container) phải ghi được. Không gắn tệp này ra host. Khi không cấp mật khẩu, chạy lại dùng tệp đã có. Tệp mất khi tạo lại container; với CSDL đã có tài khoản, hãy cấp lại mật khẩu đã dùng từ môi trường chạy vì seed không đổi mật khẩu tài khoản cũ. Script `bash scripts/khoi-v2.sh` đọc tệp qua `docker compose exec -T core`, không in bí mật, đăng nhập Nguyễn Hoài Nam (`nam.nguyen.hoai@thpt-nguyentrai.edu.vn`, lớp 12A3).

Thiếu hoặc không đọc được `data/dev`: các runner nội dung, giao bài và lịch sử cảnh báo rồi bỏ qua, core vẫn khởi động; script khói báo lỗi nếu seed không hoàn tất. Seed không đổi mật khẩu hoặc nâng quyền của tài khoản đã có. Nếu các tài khoản tổng hợp từ lần chạy trước còn tồn tại, seed có thể bổ sung phần dữ liệu thiếu của các lớp đó.

Mọi runner mới đều có `@Profile("dev")`. Runner nhập snapshot v0 lúc khởi động đã được thay bằng runner trường hư cấu; importer v0 và snapshot vẫn còn để đối chiếu. Profile production không tự nạp ngân hàng này, kể cả khi bật thuộc tính nhập lúc khởi động. Không sửa migration V1–V12; không có migration seed mới.

## Dữ liệu và giới hạn cổng toán

Nguồn ở `data/dev/nguyen-trai/`, dùng các trường của importer hiện có: mã bài, kỹ năng, 4 mức, Bloom, đề, lời giải cấu trúc và sự kiện bảo vệ. `loi_giai` chứa lời giải tự soạn bằng tiếng Việt; `kiem_dinh` chứa dữ kiện để pytest kiểm độc lập. Công thức và tài liệu thuộc nguồn tự soạn của tổ Toán hư cấu, không chép bài SGK.

Có **74 bài**: 68 bài gốc thuộc **13 chủ đề**, cộng 6 bài luyện hiện tại `NT-LUYEN` (2 bài mỗi khối, không sinh lịch sử, gia sư mở trong chế độ ôn tập). Các chủ đề gốc đủ bốn mức. Khối 10 có hàm số bậc hai, mệnh đề/tập hợp, thống kê và xác suất cơ bản; khối 11 có lượng giác, dãy số/cấp số, đạo hàm, hình học không gian và xác suất; khối 12 có đơn điệu/cực trị, tích phân, hình học không gian/Oxyz và xác suất có điều kiện. Danh mục `ky-nang.json` có 29 kỹ năng cụ thể (ví dụ cực trị hàm bậc ba, cấp số cộng, thể tích khối chóp). Mỗi bài gắn mã kỹ năng thật; ngân hàng và bài giao chỉ thuộc đúng khối lớp. Importer chỉ kiểm/phát hành ngân hàng của khối đó.

`de_bai.latex` là preview LaTeX ngắn của từng bài, sinh bằng `sympy.latex` khi có biểu thức; hình học/xác suất dùng câu tiếng Việt trong `\text{...}` kèm số liệu, không dùng tên khóa máy như `good`, `g1` hoặc mảng tọa độ; `ham_so_sympy` và `kiem_dinh.du_kien` giữ dạng máy. Đề nhúng toán trong `$...$`. Pytest kiểm đáp án từng bài và đối chiếu bản hiển thị với SymPy, không cần cài `parse_latex`/ANTLR.

CAS hiện có chấm khung đạo hàm 5 bước. **8 bài hàm số** phát hành sau cổng CAS; **55 bài khác** có quyết định giáo viên hư cấu duyệt qua `VerificationRun.approve`, ghi `content_reviews.reviewer_id` bằng giáo viên tổng hợp của lớp. Không đổi các tầng CAS sang `DAT`. **5 bài toàn trường** giữ chờ rà soát lập luận sư phạm (khối 10: 0, khối 11: 3, khối 12: 2): lượng giác, cấp số nhân, tiếp tuyến, cực tiểu diện tích và phép chiếu. Quyết định duyệt seed căn cứ vào `test_teacher_solution`; pytest kiểm cả các bài vẫn chờ rà soát. Cách này chỉ nằm trong runner dev.

Bảng đạo hàm gồm 6 dòng khóa sau kiểm hai tầng có trích dẫn. Bảng bổ sung 12 dòng giữ `NHAP`. Gia sư giữ `offline`, tắt mở lời giải sau nộp. Các dạng ngoài khung đạo hàm chưa được chấm tự động khi học sinh làm bài mới.

Mục tiêu cho 68 bài gốc trên CSDL mới khi CAS hoạt động đầy đủ (số lượng CSDL chưa được kiểm trong sandbox). Sáu bài luyện hiện tại bổ sung 60 lượt giao; script khói đối chiếu tổng ngân hàng với tệp seed thực tế, học sinh lớp 12 thấy 25 bài (23 bài gốc + 2 bài luyện):

| Dữ liệu | Số lượng |
| --- | ---: |
| Người dùng tổng hợp | 35: 1 quản trị, 4 giáo viên, 30 học sinh |
| Lớp | 3, mỗi lớp 10 học sinh và 2 giáo viên |
| Bài ngân hàng | 68; khối 10: 16 phát hành, khối 11: 24 phát hành + 3 chờ, khối 12: 23 phát hành + 2 chờ |
| Quyết định duyệt nội dung | 55: duyệt ở đúng lớp của khối |
| Tài liệu lớp | 21: 7 ghi chú × 3 lớp |
| Bảng công thức | 3 bảng đạo hàm khóa + 3 bảng chương nháp |
| Lượt giao | 630: khối 10 có 160, khối 11 có 240, khối 12 có 230 |
| Bài đã nộp lịch sử | 390; 240 lượt kiểm tra (8 bài × 30 học sinh) chưa nộp |
| Bài chờ thầy cô xem | 68, có cảnh báo `NHO_GV`; không tính sai |
| Sự kiện mức hiểu | 322; bỏ qua 68 bài chưa kiểm được |
| Trạng thái mức hiểu | 160, tính BKT thật trên kỹ năng đúng khối |
| Lịch học | 120 phiên, 45 phút/phiên |
| Gợi ý chờ duyệt | 1 (lớp 12) |

Dùng `assignments.set_name` (V7), không thêm migration. Lớp 10A1 có **3 bộ** (2 ôn + 1 kiểm tra), 11A2 có **6 bộ** (5 ôn + 1 kiểm tra), 12A3 có **4 bộ** (3 ôn + 1 kiểm tra). Bộ “Kiểm tra hàm số — 45 phút” ở mỗi lớp có **8 bài đúng khối**, đủ NB/TH/VD/VDC, mode `KIEM_TRA`, cùng hạn 12 giờ sau lần tạo lớp. Khối 10 dùng bài hàm bậc hai/tập hợp; khối 11 dùng lượng giác/đạo hàm; khối 12 dùng đơn điệu/cực trị. Cờ `kiem_tra_seed` trong từng bài là nguồn lựa chọn bộ kiểm tra. Lịch sử chỉ sinh từ giao bài `ON_TAP`; kiểm tra chưa nộp. Tên bộ không tạo cơ chế bấm giờ. Bộ ôn gom theo chủ đề, có hạn 3–7 ngày sau lần tạo lớp. Các hạn neo vào lần tạo lớp; chạy lại không dời hạn hoặc sửa giao bài đã có.

`bai-lam-lich-su.json` gồm payload tổng hợp đúng/sai/chưa đọc được. Bài khung 5 bước lấy phán quyết CAS thật; bài chương khác có `giao_vien_cham=true`, đáp án học sinh được pytest đối chiếu với lời giải đã kiểm, lưu căn cứ `GV_DUYET_SEED` và giáo viên chấm trong `grading_results.normalization.reviewer_id`. Đây là lịch sử giáo viên chấm tổng hợp, không phải phán quyết CAS. Các lượt chưa kiểm được không tác động BKT.

Lịch sử ôn tập gồm **227 đạt / 95 chưa đạt / 68 chờ xem**. Phân bố theo học sinh/chương; nhóm mạnh đạt nhiều hơn nhóm cần hỗ trợ. Không gán mức hiểu riêng: dùng BKT domain trên mã kỹ năng của bài, làm tròn PostgreSQL `real` giữa các lượt. Unit phát lại xác nhận 390 lượt, 322 lượt tác động BKT và 160 trạng thái có lịch sử. Adapter đọc seed `WorkspaceNguyenTraiSeedAdapter` chỉ bật profile dev, chỉ áp cho lớp UUID seed qua guard: tổng quan đếm kỹ năng đúng khối, danh sách bài giao dùng tên bộ bài và tổng quan gom hạn theo bộ. Lớp khác dùng adapter hiện hành.

## Hợp đồng API cho lane UI (SEED3/SEED4)

- `GET /api/gv/ngan-hang?lopId=...`: chỉ trả bài có khối chủ đề bằng khối lớp (áp mọi profile), kể cả khi có phát hành nhầm khối. `details.skillCode` giữ mã; thêm `details.skillName: string` lấy từ `skills.name` và `details.grade: number`. Các API kỹ năng/tiến độ vẫn dùng `name` thật từ cùng danh mục.
- `GET /api/hs/bai`: giữ `deBai` là đề đầy đủ; **`deBaiLatex` là trường preview đang được UI hiển thị**, không đổi tên/kiểu. Nguồn `de_bai.latex` đã thay 24 preview hình học/xác suất thành LaTeX dễ đọc. Thêm **`tenBo: string | null`**, lấy trực tiếp từ `Assignment.setName`; bộ cũ không tên trả `null`. `han` tiếp tục là hạn của bộ. Trường mới có trên DTO ở mọi profile.
- `GET /api/gv/giao?lopId=...` trên lớp seed dev: vẫn trả từng lượt giao, giữ `id`, `details.studentId`, `details.problemCode`, `details.mode`, `details.dueAt`. `title` là tên bộ. Thêm `details.tenBo: string`, `details.setKey: string` (UUID ổn định), `details.classId: string`, `details.submitted: boolean` (đã nộp ở phiên bản đề hiện tại, kể cả chưa kiểm được). Lane UI có thể gom bằng `setKey`.
- `GET /api/gv/tong-quan?lopId=...` trên lớp seed dev: **`deadlines` trả một `Item` mỗi bộ**, thay vì từng lượt giao; `Item.id = details.setKey`, `title = tenBo`, `subtitle = "N bài"`, `status = mode`. `details` gồm `tenBo`, `setKey`, `dueAt`, `mode`, `problemCodes: string[]`, `problemCount: number`, `studentCount: number`, `assignmentCount: number`, `submittedCount: number`. `submittedCount` đếm lượt học sinh × bài đã nộp, không phải số học sinh hoàn thành cả bộ. Không có `problemCode` đơn trên dòng bộ; dùng `problemCodes` nếu cần. `assignments` ở tổng quan vẫn đếm lượt giao học sinh × bài (160/240/230), không đổi sang số bộ.

Khóa bộ gồm lớp + tên bộ + hạn + mode, nên hai lần giao cùng tên nhưng khác hạn hoặc mode không bị trộn. Đề, payload máy, lời giải và cổng chấm không đổi vì preview.

## Tính idempotent và dữ liệu đã tồn tại

UUID lớp, bài lịch sử, lịch và cảnh báo được suy từ namespace seed ổn định. Lớp trùng tên/năm nhưng khác UUID, hoặc có thành viên không tổng hợp/vai trò sai, bị bỏ qua. Tài khoản trùng không bị cập nhật; học sinh thuộc lớp khác không được kéo về lớp seed. SQL chỉ chạy trên đúng UUID lớp được guard xác nhận.

Chạy lại giữ id, mật khẩu, hạn, mode, bài đã nộp, lịch hoàn thành, quyết định duyệt và tiến độ đã có. Importer giữ phiên bản khi nội dung không đổi; bảng nháp giáo viên đã sửa không bị ghi đè. Các lịch/hạn neo vào lần tạo/giao đầu, không tự dời để luôn có nhắc mới. Mastery bootstrap chỉ chạy với học sinh chưa có trạng thái mức hiểu; nếu một phần lịch sử thiếu do CAS lỗi nhưng tiến độ đã tồn tại, seed không backfill để tránh ghi đè tiến độ đang học. Bảng nháp seed chưa được ai sửa có thể được importer dùng lại để khóa phiên bản đạo hàm mới khi nguồn đổi; bản giáo viên đã sửa hoặc chép giữ nguyên. Không tự xóa tài khoản/lớp seed cũ trong CSDL đang dùng; muốn thử lại từ đầu, dùng CSDL hoặc schema dev mới. SEED4 không sửa bài đã nộp hoặc hạn/tên bộ đã có từ SEED2/SEED3; để thấy tỷ lệ lịch sử và lịch bộ mới, khởi tạo schema dev mới. Preview là nội dung ngân hàng nên importer nạp phiên bản mới khi LaTeX đổi; lịch sử cũ giữ nguyên phiên bản/căn cứ.

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

Các kiểm tra unit bao phủ sinh/lưu mật khẩu ngẫu nhiên, quyền tệp và dùng lại mật khẩu, trùng tài khoản, đủ roster, profile production và nhập lại ngân hàng. Pytest kiểm từng phép biến đổi, đáp án từ dữ kiện gốc, mẫu bài làm lịch sử, cổng khóa bảng và cổng phát hành thực tế.

### Bằng chứng phiên SEED2, 2026-10-13

SHA nền: `82795de6954e3a2b3116c276e6bef5f06b6c5d5e` (diff chưa commit).

- `/workspace/du_an_em_linh/services/math/.venv/bin/pytest services/math/tests/test_seed_nguyen_trai.py`: **408 passed**, exit 0; kiểm đáp án, LaTeX, lịch sử, cổng bảng và cổng phát hành bằng Python trực tiếp.
- `JAVA_HOME=$HOME/.local/jdk/jdk-25.0.4.1+1 ./mvnw -B -q -o test-compile` từ `services/core`: exit 0.
- Core unit/ArchUnit: **23 tests passed**, exit 0, chọn `PhanBoMucHieuSeedTest,NoiDungNguyenTraiTest,BangChuongSeedTest,SeedProfileTest,CleanArchitectureTest,DddArchitectureTest,NullMarkedPackagesTest` với Maven offline `test`, JDK đã chỉ định và Mockito javaagent. Kiểm BKT phát lại dữ liệu seed bằng domain, đối chiếu phân bố từng khối; test profile xác nhận adapter đọc seed không bật ở production.
- Sandbox không cho mở socket: không kết nối PostgreSQL/CAS HTTP, chưa xác minh số lượng hoặc idempotency trên PostgreSQL. Test tích hợp opt-in có assertion số lượng, khối giao bài, quyết định duyệt, tổng quan và snapshot chạy lại.
- Không sửa migration V1–V12, không commit.

### Bằng chứng phiên SEED3, 2026-10-13

SHA nền: `7d2df1ac2a8c79f5cf0e515fdd943e9ec818b141` (diff chưa commit).

- `/workspace/du_an_em_linh/services/math/.venv/bin/pytest services/math/tests/test_seed_nguyen_trai.py -o addopts='' -q`: **476 passed**, exit 0; gồm 68 kiểm tra preview và các kiểm tra đáp án/cổng toán/lịch sử kế thừa.
- Từ `services/core`: `JAVA_HOME=$HOME/.local/jdk/jdk-25.0.4.1+1 ./mvnw -B -q -o test-compile`: exit 0.
- Core không dùng socket: **261 tests passed**, 0 failure/error/skipped trong 47 lớp test, exit 0. Lệnh thực tế (từ `services/core`, danh sách tên được truyền qua tệp tạm của phiên):

```bash
JAVA_HOME=$HOME/.local/jdk/jdk-25.0.4.1+1 ./mvnw -B -q -o \
  -Dtest=AuthControllerTest,AuthSessionTest,BangChuongSeedTest,BktTest,BoBaiSeedTest,ChiaDoanTest,ClassMembershipServiceTest,ClassSettingsTest,CleanArchitectureTest,ContentReviewTest,DauVanTayTest,DddArchitectureTest,DeBaiHocSinhServiceTest,DocKetQuaChamTest,EmailTest,EscalationTest,FormulaSheetTest,GetBaiCuaHocSinhPreviewTest,JsonKieuJsTest,KienTrucRulesTuKiemTest,Level4Test,LoginAttemptKeyTest,LoginRequestTest,LoginUseCaseTest,LogoutUseCaseTest,LopNguyenTraiSeederTest,NghiDoanMoTest,NoiDungNguyenTraiTest,NullMarkedPackagesTest,OfflineTutorServiceTest,PhanBoMucHieuSeedTest,PracticeModelTest,ProblemReleaseTest,ProblemTest,RefreshSessionUseCaseTest,RefreshTokenTest,SchoolClassTest,SeedProfileTest,SubmissionHistoryTest,TaiKhoanNguyenTraiSeederTest,TepVangDoiChieuTest,UserTest,VerificationRunTest,VietLoiGiaiTest,WorkspaceApiTest,WorkspaceServiceTest,YeuCauChamTest \
  -DargLine=-javaagent:/home/box/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar test
```

- Unit phát lại 620 lượt, xác nhận **371/161/88**, đủ bốn mức và nhóm mạnh đạt nhiều hơn; chờ xem không tăng số lượt BKT. Kiểm tra gom bộ tách hạn/mode, số bài/học sinh/lượt giao/lượt nộp và khóa ổn định. Test DTO gọi use case thật và kiểm JSON `tenBo`/`deBaiLatex`. Importer unit nhập lần hai không ghi lại nội dung.
- Đã chạy test đỏ cho preview máy, thiếu hàm gom bộ và `tenBo` chưa ánh xạ, rồi kiểm lại xanh sau sửa.
- Chưa chạy test socket PostgreSQL/CAS HTTP trong lane này. Test tích hợp opt-in đã cập nhật các số lượng, phân bố, 4/5/5 bộ và snapshot trước/sau chạy lại; chưa xác nhận idempotency trên PostgreSQL ở phiên này.
- Không sửa frontend/migration V1–V12, không thêm V13 vì đã có `set_name`, không commit.

### Bằng chứng phiên SEED4, 2026-10-13

SHA nền: `b3e678a30f4c494d00915bee8710dd31e0a1f800` (diff chưa commit).

- `/workspace/du_an_em_linh/services/math/.venv/bin/pytest services/math/tests/test_seed_nguyen_trai.py`: **479 passed**, exit 0. Kiểm tên kỹ năng khớp loại hàm; mỗi khối có 8 bài kiểm tra đã duyệt, đủ bốn mức; giữ kiểm toán/cổng của 68 bài.
- Từ `services/core`: `JAVA_HOME=$HOME/.local/jdk/jdk-25.0.4.1+1 ./mvnw -B -q -o test-compile`: exit 0.
- Core không dùng socket: **262 tests passed** trong 48 lớp, 0 failure/error/skipped, exit 0. Dùng lệnh SEED3 ở trên với thêm `BankGradeTest`, cùng JDK, Maven offline và Mockito javaagent. Unit lọc ngân hàng loại bài khối 10/11 khỏi lớp 12; importer nhập lại không ghi lại bài. Unit lịch sử phát lại 390 lượt ôn: **227/95/68**, 322 lượt BKT, 160 trạng thái có lịch sử; bài kiểm tra được loại khỏi lịch sử.
- Không chạy PostgreSQL/CAS HTTP: các số lượng CSDL và idempotency trên PostgreSQL chưa được xác minh trong phiên này. Test tích hợp opt-in đã cập nhật số lượng, bộ kiểm tra, tên kỹ năng/khối và giữ snapshot chạy lại.
- Chỉ sửa phạm vi SEED4; không commit.

## Gia sư AI (GLM)

`POST /api/hs/gia-su` dùng GLM khi core nhận `GLM_API_KEY` không rỗng; không có khóa hoặc khóa chỉ có khoảng trắng thì giữ thang gợi ý `offline`. Chỉ dùng khóa **Z.ai Coding Plan trong môi trường dev và với dữ liệu tổng hợp**. Không dùng khóa này cho học sinh thật hoặc triển khai production. Cấp khóa từ môi trường chạy, không ghi vào repo, frontend, image hay log.

Cấu hình trong `application.yaml` dưới `mathl.gia-su.*`:

| Biến môi trường | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `GLM_API_KEY` | Rỗng | Khóa Bearer phía core; rỗng là offline |
| `GLM_BASE_URL` | `https://api.z.ai/api/coding/paas/v4` | URL gốc; core thêm `/chat/completions`; rỗng dùng mặc định |
| `GLM_MODEL` | `glm-5.3` | Rỗng dùng mặc định |
| `GLM_TIMEOUT` | `45s` | Hạn toàn lời gọi cho mỗi attempt; tối đa hai attempt, tổng kể cả backoff bị chặn ở 110s trong deadline lượt 120s |
| `GLM_FIRST_TOKEN_TIMEOUT` | `20s` | Hạn token nội dung đầu tiên và idle giữa các hoạt động stream sau đó; riêng với hạn toàn attempt |
| `GLM_MAX_TOKENS` | `350` | Trần đầu ra, cho phép giảm xuống 1–350 |
| `GLM_RATE_LIMIT_REQUESTS` | `12` | Lượt tối đa mỗi học sinh trong cửa sổ trượt |
| `GLM_RATE_LIMIT_WINDOW` | `10m` | Độ dài cửa sổ, ví dụ `10m` hoặc `PT10M` |

Compose truyền khóa, URL, model, timeout và giới hạn từ môi trường host. Sau khi thay cấu hình, tạo lại core. Giới hạn dùng bộ nhớ của một tiến trình, đặt lại khi core khởi động; mỗi replica có bộ đếm riêng. Vượt giới hạn trả HTTP 429 bằng tiếng Việt **trước SSE**. Kiểm tra quyền/bài kiểm tra/hết hạn trước khi tiêu thụ lượt.

Mô hình nhận đề, bước, cấp 1–3, câu hỏi, dòng em đã nộp, bảng công thức đã khóa và gợi ý thang; không nhận lời giải chuẩn hoặc dữ kiện bảo vệ. Tắt thinking và không stream token. Bản nháp đi qua `/v1/filter` rồi cổng `kiem_loi_giang`. Lỗi nhà, hết giờ hoặc bản nháp bị chặn thì lấy thang dự phòng qua cùng cổng; cổng vẫn lỗi thì chỉ hiện câu cố định không chứa toán. Xin đáp án hoặc đổi vai được từ chối bằng luật, không gọi mô hình. Chỉ gửi lại tối đa một lần với 429/500/502/503/504, tôn trọng `Retry-After`; không đổi nhà.

Eval cần Node 22, core/CAS đang chạy và học sinh tổng hợp có bài ôn đạo hàm lẫn bài kiểm tra còn hạn. Cấp `CORE_URL`, `MATHL_EVAL_EMAIL`, `MATHL_EVAL_PASSWORD`, `GLM_API_KEY` từ môi trường, rồi chạy:

```bash
node scripts/eval-gia-su.mjs --out /tmp/mathl-gia-su-glm.json
node --test scripts/eval-gia-su.test.mjs
```

Không có khóa: in `bỏ qua: thiếu GLM_API_KEY`, thoát 0. Bộ 24 ca kiểm HTTP, thứ tự SSE, lời kết thúc, lộ đáp án theo dữ liệu seed/heuristic và độ trễ; JSON chỉ chứa tên ca và số đo, không chứa chat/tài khoản/khóa. Giãn mặc định 51 giây/lượt để không chạm 12 lượt/10 phút, mất khoảng 20 phút; script gia hạn phiên đăng nhập. Dùng tài khoản riêng, không đồng thời hỏi trên UI. Muốn chạy nhanh trong dev, nâng `GLM_RATE_LIMIT_REQUESTS` hoặc giảm `GLM_RATE_LIMIT_WINDOW` **giống nhau ở core và script**, rồi khởi động lại core. Thiếu bài phù hợp hoặc SSE `loi` làm ca trượt; kiểm lộ là phép dò cơ học, không thay eval sư phạm hay CAS. Chưa có số đo GLM thật trong sandbox của phiên hiện thực này.

> Gia sư GLM chỉ chạy khi **lớp chọn nhà `glm`** (giáo viên: Cài đặt → Gia sư → «Gia sư AI (GLM)») **và** máy chủ có `GLM_API_KEY`. Lớp mới mặc định `offline`. Seed dev trường Nguyễn Trãi đặt sẵn `glm` cho ba lớp (dữ liệu hư cấu). Câu hỏi nghi có thông tin cá nhân (kể cả trong `$…$`/`\text{}`) không được gửi ra ngoài.
