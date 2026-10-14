# Khung chấm từng bước của bộ 74 bài

Ngày 2026-10-15, nền `a12b20d945e6f02815a2ae3b495267577d0df25b`, lane grade.
Đối chiếu audit mục 7 và `audit-scope-matrix.md`, bằng chứng E06/E21.

## Số đo và giới hạn tích hợp

| Phạm vi | Trước nối | Sau nối |
| --- | ---: | ---: |
| Bài có khung năm bước và đáp án từng bước được bộ chấm CAS hỗ trợ | 43/74 | 43/74 |
| Bài dùng được phiếu tương tác trên Angular/core | 10/74 | 43/74 |
| Bài có khung CAS mới, còn cần nối frontend/core | 33 | 0 |
| Bài chưa có khung CAS phù hợp | 31 | 31 |

Lane grade đã tăng số khung CAS từ 10 lên 43. Lane nối core/frontend ngày
2026-10-15 đưa cả 43 khung vào phiếu tương tác: 10 B.DH, 17 B.GT và 16 B.XS.
Số đo này là khả năng làm/chấm theo khung khi bài đã được phát hành cho lớp;
không thay cổng kiểm định, trạng thái duyệt hay quyền giao bài.

Importer lưu khung theo `problem_id` và `content_version` trong
`exercise_step_frames` (Flyway V17). Metadata và dữ kiện đề dùng để chấm
tham gia dấu vân tay nội dung: đổi khung tăng phiên bản bài, kiểm lại cổng
phát hành; khung cũ bất biến. Metadata bước trong API học sinh chỉ gồm
`maBuoc`, `ten`, `viec`,
`kieu`. Dữ kiện `bai_toan` được practice đọc từ cổng nội bộ theo đúng bài và
phiên bản, không nhận từ trình duyệt. B.DH giữ nguyên payload `ham` cũ.
Frontend chọn ô theo kiểu nhập và bước kết luận theo vị trí cuối khung;
khôi phục nháp/kết quả chấm và xử lý `KHONG_KIEM_DUOC` cho cả ba họ.

Cờ `san_sang_ui: false` của 33 khung mới trong tệp nguồn ghi trạng thái ở
thời điểm lane grade chưa nối UI. Importer nhận các khung có đủ năm bước và
kiểu nhập được hỗ trợ, thay vì dùng cờ lịch sử đó để khóa phiếu.

`cham-buoc.json` là tệp nội dung bổ sung có phiên bản, không thay schema của
`bai-tap.json` hay khung chủ đề đang hoạt động. Mỗi mục có `ma_bai`, `ho`,
`san_sang_ui`; với họ mới có dữ kiện `bai_toan` và năm mô tả bước cùng
`kieu`/`dap_an`. Với B.DH giữ nguyên payload chuẩn đầy đủ (`payload_dung`),
gồm TXĐ, đạo hàm, nghiệm, các ô bảng và kết luận. Đây là **dữ liệu giáo viên**,
không được gửi nguyên mục có đáp án cho học sinh hoặc gia sư.

Hai bài `NT-DH12-VDC-07` và `NT-DH12-VDC-08` vốn có phần bổ sung biện luận
số nghiệm theo tham số. Khung B.DH kiểm đơn điệu/cực trị, chưa kiểm phần
biện luận đó. Số đếm 10 trước và 43 sau đều đếm hỗ trợ khung, không khẳng
định mọi lập luận tự luận của hai bài này đã được chấm tự động.

## Các họ được hỗ trợ

| Họ | Số bài | Dạng bài và cách kiểm |
| --- | ---: | --- |
| `B.DH.*` | 10 | Giữ nguyên TXĐ → đạo hàm → nghiệm → xét dấu → kết luận; bộ kiểm hiện có. |
| `B.GT.*` | 17 | 4 giá trị nhỏ nhất tam thức bậc hai (hoàn thành bình phương, không dùng đạo hàm lớp 10); 2 phương trình lượng giác trên đoạn; 4 giá trị đạo hàm; 1 tiếp tuyến qua điểm; 1 tối ưu parabol lõm; 3 tích phân; 1 diện tích hàm không đổi dấu; 1 diện tích giữa hai đồ thị. |
| `B.XS.*` | 16 | Đếm tổ hợp; nhị thức; xác suất có điều kiện; Bayes một/nhiều lần rút và chuyển bi; ba màu; nhóm có chức vụ. Các bước kiểm số cách hoặc xác suất thành phần rồi kết hợp, không chấm suy luận văn xuôi. |

Năm mã của B.GT: `MIEN`, `BIENDOI`, `TRUNGGIAN`, `TINH`, `KETLUAN`.
Năm mã của B.XS: `DUKIEN`, `THANHPHAN1`, `THANHPHAN2`, `KETHOP`, `KETLUAN`.
Tên và việc cần làm lấy **theo bài** từ tệp mới: chẳng hạn B.GT.BIENDOI
là nguyên hàm ở bài tích phân nhưng là đạo hàm ở bài tiếp tuyến. Không ghép
toàn bộ các khung vào `khung-buoc.json` theo chủ đề: một chủ đề có nhiều dạng.

Họ mới nhận biểu thức số/hàm, miền xác định, tập hữu hạn hoặc dãy giá trị
có thứ tự. Dãy dùng dấu `;`; tập dùng `{…; …}`. Nhận phân số LaTeX và các
biểu thức tương đương; tập không yêu cầu thứ tự. Hoàn thành bình phương phải
thực sự chứa bình phương đúng tâm, không nhận việc chép lại đa thức đề bài.
Nguyên hàm được kiểm bằng đạo hàm, nhận cộng hằng số thực cụ thể; hai giá trị
tại cận được kiểm theo **nguyên hàm học sinh đã chọn**.
Kiểm thêm các mẫu số, hàm và lũy thừa trong cú pháp gốc để không nhận phép
rút gọn làm mất điều kiện xác định như `x/x`, `sqrt(x)²` hay `log(x)−log(x)`.

## Hợp đồng nối sang core/frontend

`POST /v1/grade` giữ payload B.DH cũ. Payload mới thêm `bai_toan`:

```json
{
  "bai_toan": {
    "loai": "integral",
    "du_kien": {"ham": "3*x**2", "can_duoi": 0, "can_tren": 2}
  },
  "nop_toi": "B.GT.MIEN",
  "cac_buoc": [
    {"ma_buoc": "B.GT.MIEN", "cac_dong": [{"dong": 0, "latex": "0; 2"}]}
  ]
}
```

Core lấy `bai_toan` từ nội dung của **đúng bài và phiên bản đang làm**,
không nhận đề/đáp án chuẩn do trình duyệt gửi. Bộ chấm tự tính đáp án từ
dữ kiện này, không đọc `dap_an` trong tệp seed. Khi nối cần importer/port
phục vụ khung theo bài, metadata công khai chỉ có mã/tên/việc/kiểu nhập,
adapter practice gửi dữ kiện đề đáng tin cậy, và frontend nhập dòng/tập/dãy
theo từng bước. Đã nối trong lane core/frontend ngày 2026-10-15. Không cần đưa lời giải chuẩn vào ngữ cảnh gia sư.

Phản hồi giữ `ket_qua`, `per_buoc`, `nop_toi`, `chua_xong`, vị trí dòng sai
0-based và các trường hiện có. Mọi bước trước bước nộp phải có mặt đúng thứ
tự, không trùng mã; một bước có một dòng số 0. Không trả đáp án hoặc biểu
thức chuẩn. Không mượn mã lỗi/kỹ năng DH cho họ khác. Dữ kiện không hỗ trợ,
không đọc được hoặc chưa chứng minh được trả `KHONG_KIEM_DUOC`.

## 31 bài còn chưa có khung CAS

Các đáp án số của các bài này vẫn được bộ kiểm seed hiện có đối chiếu với
SymPy. Thiếu khung chấm từng bước, không phải kết luận rằng đáp án sai.

| Dạng | Các mã bài | Lý do giữ tự luận |
| --- | --- | --- |
| Giá trị lượng giác (6) | `NT-LG11-NB-01`, `NT-LG11-NB-02`, `NT-LG11-TH-03`, `NT-LG11-VD-04`, `NT-LUYEN11-NB-01`, `NT-LUYEN11-NB-02` | Một đến ba thao tác; ép thành năm bước sẽ lặp dữ kiện/đáp án. Cần khung ngắn đánh giá góc/quy tắc dấu. |
| Giao tập hợp (4) | `NT-MT10-NB-01`, `NT-MT10-TH-02`, `NT-MT10-VD-03`, `NT-MT10-VDC-04` | Cần khung tập hợp và cách kiểm phần tử; bài hiện quá ngắn cho năm bước. |
| Trung bình (4) | `NT-TK10-NB-01`, `NT-TK10-TH-02`, `NT-TK10-VD-03`, `NT-TK10-VDC-04` | Đếm số phần tử, tổng, chia; nên dùng khung ngắn, không kéo dài thành năm bước. |
| Dãy số (6) | `NT-DS11-NB-01`, `NT-DS11-TH-02`, `NT-DS11-TH-03`, `NT-DS11-VD-04`, `NT-DS11-VDC-05`, `NT-DS11-VDC-06` | Cần họ cấp số cộng/nhân riêng, kiểm chỉ số và hệ phương trình suy ra công sai/công bội. Không ghép vào khung đạo hàm/tích phân. |
| Đường chéo hộp (5) | `NT-KG12-VD-04`, `NT-KG11-NB-01`, `NT-KG11-TH-02`, `NT-KG11-VD-03`, `NT-KG11-VDC-04` | Kết quả số kiểm được nhưng lời giải ngắn; cần khung hình học/Pythagore phù hợp. |
| Khoảng cách điểm (1) | `NT-KG12-TH-03` | Cần khung tọa độ/vectơ; không ép công thức khoảng cách vào năm bước giải tích. |
| Thể tích chóp (2) | `NT-KG12-NB-01`, `NT-KG12-TH-02` | Cần kiểm cách chọn diện tích đáy/chiều cao và đơn vị, ngoài hai họ mới. |
| Hình chiếu lên mặt phẳng (1) | `NT-KG12-VDC-05` | Cần kiểu vectơ/điểm, điều kiện vuông góc và thuộc mặt phẳng, không chỉ giá trị khoảng cách. |
| Thể tích tứ diện (1) | `NT-KG12-VDC-06` | Cần khung vectơ/định thức và giá trị tuyệt đối, ngoài hai họ mới. |
| Diện tích chứa tham số (1) | `NT-TP12-VDC-05` | Phải chia trường hợp vị trí nghiệm, dấu hàm và chứng minh tối ưu toàn cục trên các miền tham số. Bộ chấm diện tích mới chủ động từ chối hàm đổi dấu trong đoạn, không dùng sai công thức \|∫f\|. |

## Kiểm thử

### Lane nối core/frontend, 2026-10-15

Nền `e9f9c1a9f761e8cc237c723796013d1cbc117f49`, thay đổi chưa commit.
`KhungChamWiringTest` đọc đủ 74 đề, đếm **43** phiếu chấm được, so metadata
và dữ kiện máy chủ theo từng bài; kiểm không mượn khung từ phiên bản khác.
`KhungChamUseCaseUnitTest` kiểm đường nộp B.GT/B.XS, giữ
`KHONG_KIEM_DUOC`, dữ kiện bất biến và băm ổn định theo thứ tự khóa.
`KhungChamPracticeIntegrationTest` có hai ca tham số B.GT/B.XS, chạy importer,
cổng nội dung, nộp đủ năm bước, đọc lại và nộp bài trên PostgreSQL thật;
chỉ máy CAS ở biên được giả lập. Hai ca đã biên dịch, chưa chạy vì thiếu Docker.

- Core, từ `services/core`, `JAVA_HOME=$HOME/.local/jdk/jdk-25.0.4.1+1`:
  `./mvnw -q '-DargLine=-javaagent:/home/box/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar' '-Dtest=!MathServiceClientTest' -Dsurefire.reportsDirectory=/tmp/wire-core-surefire test`:
  **438 đạt, 0 lỗi, 203 bỏ qua** (Docker/opt-in), gồm ArchUnit và 5 ca unit mới.
  Lệnh chuẩn đã thử nhưng Mockito không tự attach được trong sandbox.
  Thêm javaagent khắc phục attach; chạy đầy đủ vẫn lỗi tại test cũ
  `MathServiceClientTest.start` vì mở socket bị `Operation not permitted`.
  Lần đạt loại riêng test đó; không sửa hoặc bỏ qua assertion trong mã test.
- Frontend, từ `apps/frontend`, đặt
  `PATH=$HOME/.local/node/node-v22.23.3-linux-x64/bin:$PATH` và
  `npm_config_cache=/tmp/mathl-ui3-npm`: `npx --offline -y pnpm@10.33.3 test`
  **263 đạt, 0 lỗi**; `npx --offline -y pnpm@10.33.3 exec ng build`: đạt.
  Lệnh npx chuẩn bị DNS chặn tới npm registry, nên dùng bản pnpm trong cache.
- `node scripts/check-flat.mjs`, `node --test scripts/check-flat.test.mjs`
  và `git diff --check`: đạt.
- `npx --offline -y pnpm@10.33.3 exec playwright test e2e/acceptance/step-frames.spec.ts`:
  chưa chạy được vì web server bị `EPERM` khi listen `127.0.0.1:4300`.
  `--list` nhận đủ 4 ca cho B.GT/B.XS; ca desktop đặt viewport 1280 px,
  ca mobile 390 px. MockApi đã có metadata kiểu nhập; không khẳng định
  đã kiểm hình ảnh hoặc chạy e2e trong sandbox này.

### Lane grade trước khi nối

`test_step_families_seed.py` kiểm từng đáp án: giải tích đối chiếu định nghĩa
bằng SymPy; xác suất liệt kê các kết quả đồng khả năng độc lập với công thức
tổ hợp của grader. Kiểm tất cả tiền tố đúng của cả 43 bài, cố ý sửa từng
bước của 33 bài mới, trường hợp thiếu/trùng/đảo bước, payload ác ý, tính nhất
quán nguyên hàm/cận và đường router qua sandbox. Tệp seed cũ giữ nguyên,
các kiểm thử lời giải/cổng phát hành/lịch sử cũ vẫn được chạy.

Tại SHA nền nói trên, không commit/push:

- Bộ mới: **453 ca đạt, 0 lỗi, 0 bỏ qua**, 8,113 giây; từ `services/math`:
  `PYTHONPATH=. /workspace/du_an_em_linh/services/math/.venv/bin/pytest -p no:warnings -q tests/test_step_families_seed.py --junitxml=/tmp/wt-grade-family-tests.xml`.
- Importer hiện có: **3 ca đạt, 0 lỗi, 0 bỏ qua**; từ `services/core`, đặt
  `JAVA_HOME=$HOME/.local/jdk/jdk-25.0.4.1+1`, chạy
  `./mvnw -q '-DargLine=-javaagent:/home/box/.m2/repository/org/mockito/mockito-core/5.23.0/mockito-core-5.23.0.jar' -Dtest=NoiDungNguyenTraiTest,BaiLuyenNguyenTraiSeedTest test`.
  Tham số javaagent khắc phục Mockito không tự attach được trong sandbox;
  không sửa mã/config core. Không chạy hay khẳng định đã chạy toàn bộ Maven.
- `git diff --check`: đạt.

Lệnh pytest chuẩn toàn dịch vụ bị treo ở TestClient của ca hiện có
`test_tran_timeout_20s`. Kiểm tra tối thiểu độc lập xác nhận
`loop.call_soon_threadsafe(event.set)` không đánh thức vòng lặp trong sandbox
này. Plugin tạm `/tmp/wt-grade-test-env/wt_grade_asyncio_poll.py` đặt một
timer 10 ms trong `BaseEventLoop.run_forever` để giới hạn thời gian selector
chờ; không sửa ứng dụng, dữ liệu, phản hồi hoặc assertion, không bỏ qua test.
Chỉ nạp plugin ở môi trường kiểm thử này; lệnh toàn dịch vụ:

```bash
PYTHONPATH=.:/tmp/wt-grade-test-env /workspace/du_an_em_linh/services/math/.venv/bin/pytest -p wt_grade_asyncio_poll -p no:warnings -q --junitxml=/tmp/wt-grade-math-tests.xml
```

Kết quả toàn dịch vụ của bản cuối: **3.334 ca đạt, 0 lỗi, 0 bỏ qua**,
164,419 giây. Kết quả XML: `/tmp/wt-grade-math-tests.xml`; bản này gồm
453 ca mới nêu trên. Đây là kết quả có plugin môi trường tạm, không khẳng
định lệnh chuẩn không plugin chạy được trong sandbox hiện tại.
