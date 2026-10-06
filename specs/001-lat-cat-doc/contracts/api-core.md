# Hợp đồng `apps/frontend` ↔ `services/core` — P2

- Cùng gốc qua nginx (`/api/`), không CORS. Bearer access token (P1, #73); `/api/auth/*` giữ như #72.
- Lỗi: `application/problem+json`, `detail` tiếng Việt. 401 → frontend làm mới phiên một lần (`xacThucInterceptor`). 403 → không phải thành viên lớp. 404 → không có hoặc không được thấy (không lộ tồn tại).
- Mã trạng thái nội dung giữ như v0: `DAT`, `SAI`, `KHONG_KIEM_DUOC`, `GV_DUYET`; kết quả chấm thêm `KHONG_CHAM_DUOC` (dịch vụ toán lỗi, không bao giờ coi là đạt).
- Kết quả kiểm và trạng thái phát hành tính theo lớp (data-model §content): học sinh chỉ thấy bài đã phát hành cho lớp của mình; giáo viên kiểm, duyệt, giao bài cho lớp mình dạy.
- Không DTO nào của học sinh chứa `solutions.*`, `protected_facts`, `final_answer` khi bài đang làm.

## Học sinh (vai trò `STUDENT`, chỉ dữ liệu của mình)

| Phương thức | Đường dẫn | Vào | Ra | FR |
| --- | --- | --- | --- | --- |
| GET | `/api/hs/trang-hoc` | — | `{ten, viecHomNay[], baiKe: {maBai, tieuDe, lyDo: CHUA_LOI\|CUNG_CO\|NANG_1_NAC\|DE_HON\|THAY_CO_GIAO, kyNang, muc}, soBaiGiao[], soKyNang[{kyNang, muc4, kẹt}], hoanThanh: {kyNang[], chuDe}}` | 23–28 |
| GET | `/api/hs/bai` | — | bài được giao cho em mà đang phát hành ở lớp: `[{maBai, deBai, deBaiLatex, kyNang, tenKyNang, muc4, han, trangThai, soBuocDat, soBuoc, ketQua}]`, xem [Làm bài](#làm-bài-t021) | 31 |
| GET | `/api/hs/bai/{maBai}` | — | `{maBai, de: {text, latex}, kyNang, tenKyNang, muc4, dangTraLoi, buocBatDau, khaiBaoKetLuan[], cacBuoc: [{maBuoc, ten, viec}], baiLam: {trangThai, cacBuoc: [{maBuoc, dong: [{dong, latex, loai?}], bang: [{hang, k, giaTri}], ketQua, thongBao, oSai[]}]}, coTheMoLoiGiai}`, xem [Làm bài](#làm-bài-t021) | 6–10 |
| POST | `/api/hs/bai/{maBai}/buoc` | `{maBuoc, dong?: [{dong, latex, loai?}], bang?: [{hang, k, giaTri}], suKien?: [{maBuoc, hang?, k?, giaTriCu?, giaTriMoi, luc}]}` (`suKien`: sự kiện nhập mới kể từ lần nộp trước, tối đa 500) | `{ketQua: DAT\|SAI\|KHONG_KIEM_DUOC\|KHONG_CHAM_DUOC, thongBao, oSai: [{maBuoc, dong?, hang?, k?}], maLoi?, buocKe?}` — không có giá trị đúng; yêu cầu chấm dựng từ các bước đã lưu, không từ máy học sinh. 400 khi thân sai (xem [Làm bài](#làm-bài-t021)), 404 như bài không có | 8–10 |
| POST | `/api/hs/bai/{maBai}/nop` | — | `{ketQua: DAT\|SAI\|KHONG_KIEM_DUOC, mucHieu: [{kyNang, muc4Truoc, muc4Sau}], loiGiai?}`. `ketQua` là phán quyết đã ghi của bước kết luận trên nội dung hiện tại của bài làm, ghim làm căn cứ; nộp bài không gọi dịch vụ toán. `loiGiai` chỉ khi lớp bật cờ, bài còn phát hành đúng phiên bản của bài làm, và em không đang làm lại bài (cờ đọc lúc trả lời). Gửi lại sau khi đã nộp trả cùng `ketQua` và `mucHieu` khi đề chưa đổi; `loiGiai` theo cờ lúc trả lời. 409 khi bài làm thiếu hay để trống (dòng, ô chỉ có khoảng trắng) một bước từ bước bắt đầu tới bước kết luận (kể cả chưa có bài làm), khi đủ bước mà bước kết luận chưa có phán quyết cho nội dung hiện tại (chấm lỗi `KHONG_CHAM_DUOC`, hay đã sửa một bước sau lần chấm đó), hoặc bài làm dở là của đề đã đổi; `detail` nói em phải làm gì. 404 như `…/buoc` | 6, 23 |
| POST | `/api/hs/gia-su` | `{maBai, cauHoi?, chip?: GOI_Y\|SAI_CHO\|GUI_THAY_CO}` | **SSE**, xem dưới | 11–21 |
| GET | `/api/hs/gia-su/{maBai}` | — | lịch sử: `[{vaiTro, noiDung, trichDan[], nhan: "Gia sư AI", luc}]` | 22 |
| GET | `/api/hs/lich` | — | `{tuan: [{thu, gio, viec}], loiKhuyen, nhacHomNay[]}` | 27–28 |
| GET | `/api/hs/kho` | — | `{congThuc: [{id, tieuDe, latex, trichDan}], taiLieu: [{id, tieuDe, doan[]}]}` (đích của `[n]`: `#ct-<id>`, `#tl-<id>`) | 21 |

### Làm bài (T021)

Có trong core từ T021 (`practice/infrastructure/web/HocSinhBaiController`). Quy tắc chung cho bốn đường `/api/hs/bai…`:

- Người gọi lấy từ access token. Lớp là lớp em đang học (`ClassMembership.lopHoc`). Thân không mang id nào; trường lạ trong thân bị bỏ qua.
- Em chưa ghi danh lớp nào thì danh sách là `[]`, ba đường còn lại trả 404.
- Bài không có, chưa phát hành ở lớp em, bị rút phát hành (đề vừa sửa), hay chỉ phát hành ở lớp khác: cả ba đường theo mã bài trả cùng 404, `detail` là «Bài chưa mở hoặc không chấm được.». Nộp bước, nộp bài cũng trả 404 khi bài phát hành mà không làm theo khung bước (trắc nghiệm, trả lời ngắn); xem đề thì vẫn 200.
- Vai trò khác `STUDENT` nhận 403 trước khi tới controller.
- Trường ghi `?` vắng mặt khi không có. Trường ghi `| null` luôn có mặt, có thể là `null`.
- Không phản hồi nào mang lời giải, dữ kiện bảo vệ hay đáp án cuối, trừ `loiGiai` của nộp bài khi lớp mở.

`GET /api/hs/bai` → 200, theo lúc giao rồi mã bài:

```json
[{"maBai": "DH12-03-TH-01", "deBai": "Xét tính đơn điệu của hàm số y = x³ − 3x² + 2.", "deBaiLatex": "y = x^3 - 3x^2 + 2",
  "kyNang": "T12.DH.03", "tenKyNang": "Xét dấu y', lập bảng biến thiên, kết luận khoảng đơn điệu", "muc4": "THONG_HIEU",
  "han": "2026-10-13T08:00:00Z", "trangThai": "DANG_LAM", "soBuocDat": 2, "soBuoc": 5, "ketQua": null}]
```

- Chỉ lượt giao `DA_GIAO` của chính em ở lớp em mà bài còn `DA_PHAT_HANH` ở lớp đó.
- `muc4`: `NHAN_BIET | THONG_HIEU | VAN_DUNG | VAN_DUNG_CAO`. `han`: ISO-8601 UTC `| null`. `tenKyNang`: tên kỹ năng của bảng `skills`; frontend hiện tên này, không tự đặt tên.
- `trangThai`: `CHUA_LAM | DANG_LAM | DA_NOP`, tính trên bài làm ở phiên bản đề hiện tại. Có bài làm đang làm thì `DANG_LAM` (làm lại sau khi nộp cũng vậy); không có mà đã nộp thì `DA_NOP`; bài làm của đề cũ không tính.
- `soBuoc`: số bước em phải làm, từ bước bắt đầu tới bước kết luận (5 với bài đủ khung, 0 với bài không làm theo khung). `soBuocDat`: số bước mà lần chấm của nội dung hiện tại là `DAT`.
- `ketQua`: `DAT | SAI | KHONG_KIEM_DUOC` khi `DA_NOP`, không thì `null`.

`GET /api/hs/bai/{maBai}` → 200. Bài đang phát hành ở lớp em mà chưa giao cho em vẫn xem được.

```json
{"maBai": "DH12-05-TH-01", "de": {"text": "Tìm cực trị của hàm số …", "latex": "y = …"},
 "kyNang": "T12.DH.05", "tenKyNang": "Tìm cực trị của hàm số cho bởi công thức (dấu hiệu đổi dấu của f')", "muc4": "THONG_HIEU",
 "dangTraLoi": "TU_LUAN_5_BUOC", "buocBatDau": null,
 "khaiBaoKetLuan": ["dong_bien", "nghich_bien", "cuc_dai", "cuc_tieu"],
 "cacBuoc": [{"maBuoc": "B.DH.TXD", "ten": "Tập xác định", "viec": "Viết tập xác định của hàm số"},
             {"maBuoc": "B.DH.DAOHAM", "ten": "Đạo hàm", "viec": "Tính đạo hàm của hàm số"},
             {"maBuoc": "B.DH.NGHIEM", "ten": "Nghiệm y′", "viec": "Tìm nghiệm y′ = 0 và điểm y′ không xác định"},
             {"maBuoc": "B.DH.XETDAU", "ten": "Xét dấu", "viec": "Xét dấu y′ và chiều biến thiên"},
             {"maBuoc": "B.DH.KETLUAN", "ten": "Kết luận", "viec": "Kết luận khoảng đơn điệu và cực trị"}],
 "baiLam": {"trangThai": "DANG_LAM", "cacBuoc": [
   {"maBuoc": "B.DH.TXD", "dong": [{"dong": 0, "latex": "D = \\mathbb{R}"}], "bang": [],
    "ketQua": "DAT", "thongBao": "Đúng rồi.", "oSai": []},
   {"maBuoc": "B.DH.NGHIEM", "dong": [{"dong": 0, "latex": "x = 0", "loai": "NGHIEM"}], "bang": [],
    "ketQua": null, "thongBao": null, "oSai": []},
   {"maBuoc": "B.DH.XETDAU", "dong": [], "bang": [{"hang": "X", "k": 0, "giaTri": "0"}],
    "ketQua": "SAI", "thongBao": "…", "oSai": [{"maBuoc": "B.DH.XETDAU", "hang": "DAU_YPHAY", "k": 2}]}]},
 "coTheMoLoiGiai": false}
```

- `dangTraLoi`: dạng trả lời của bài (`TU_LUAN_5_BUOC`, `TN_NHIEU_LUA_CHON`, `TN_DUNG_SAI`, `TRA_LOI_NGAN`). Khác `TU_LUAN_5_BUOC`, hay bài không có hàm, thì `cacBuoc` và `khaiBaoKetLuan` rỗng và bài không nộp bước được (v0 ghi «Dạng câu này (không theo khung 5 bước) chưa có khung làm bài trên ứng dụng»).
- `buocBatDau`: bước bắt đầu của bài khung ngắn `| null`. `cacBuoc` luôn là cả khung, theo thứ tự; em làm từ `buocBatDau` (hay bước đầu) tới bước cuối. `ten`, `viec` chép nguyên văn `TEN_TRANG`, `LOI_BUOC` của v0 (`apps/web/lib/de-hoc-sinh.ts`).
- `khaiBaoKetLuan`: các ô bước kết luận đề hỏi, suy từ đề như v0 (SP-03). Luôn có `dong_bien`, `nghich_bien`; thêm `cuc_dai`, `cuc_tieu` khi đề hỏi cực trị, cực đại hay cực tiểu. Chỉ nói ô nào có, không mang giá trị nào. Mỗi ô là một dòng của bước `B.DH.KETLUAN` khi nộp, với `loai` là mã viết hoa: `dong_bien` → `DONG_BIEN`, `nghich_bien` → `NGHICH_BIEN`, `cuc_dai` → `CUC_DAI`, `cuc_tieu` → `CUC_TIEU` (như payload của `solve-client.tsx`). Gửi dòng có `loai` của ô đề không hỏi thì 400.
- `baiLam.cacBuoc`: chỉ các bước đã lưu, theo thứ tự khung; `CHUA_LAM` thì `[]`. `dong[].loai` là nhãn em đã gửi (`NGHIEM`, `KHONG_XD`, nhãn ô kết luận…), vắng khi không có, để tải lại điền đúng ô. `bang[].k` 0-based (`docs/chi-so-o-bang.md`).
- `ketQua` (`DAT | SAI | KHONG_KIEM_DUOC | KHONG_CHAM_DUOC | null`), `thongBao` (`| null`), `oSai`: lần chấm của đúng nội dung hiện tại tới bước đó, như phản hồi nộp bước. Sửa một bước từ bước bắt đầu tới bước này sau lần chấm thì lần chấm đó không còn là của bước: `null`, `null`, `[]`. Lần chấm có phán quyết thắng `KHONG_CHAM_DUOC`.
- `coTheMoLoiGiai`: lớp bật «mở lời giải sau khi nộp» (cờ đọc lúc trả lời). Lời giải chỉ đi trong phản hồi nộp bài.

`POST /api/hs/bai/{maBai}/buoc` → 200 như bảng trên. Ví dụ `{"ketQua": "SAI", "thongBao": "…", "oSai": [{"maBuoc": "B.DH.DAOHAM", "dong": 0}], "maLoi": "ERR.DH.03"}`; đạt thì `{"ketQua": "DAT", "thongBao": "Đúng rồi.", "oSai": [], "buocKe": "B.DH.NGHIEM"}`.

- 400 (`title` «Bài làm không hợp lệ»): thân không đọc được; thiếu `maBuoc`; `dong[].latex` thiếu hay dài quá 2000; `dong[].dong` âm; quá 50 dòng, 200 ô, 500 sự kiện; `bang[].giaTri` dài quá 200; bước ngoài khung của bài hay trước bước bắt đầu; bước không có dòng hay ô nào (kể cả `bang: []`); ô kết luận đề không hỏi; sự kiện nhập của bước ngoài khung hay `k` quá 32767. `detail` nói trường hay lỗi nào. Không ghi gì.

`POST /api/hs/bai/{maBai}/nop` (không thân) → 200 `{"ketQua": "DAT", "mucHieu": []}`, thêm `"loiGiai": "…"` khi lớp mở. 409 (`title` «Chưa nộp được bài») kèm `lyDo`:

| `lyDo` | `detail` |
| --- | --- |
| `CHUA_LAM_DU_BUOC` | Em làm đủ các bước, tới bước kết luận, rồi hãy nộp bài. |
| `CHUA_CHAM_BUOC_KET_LUAN` | Em nộp bước kết luận và chờ máy chấm xong rồi hãy nộp bài. |
| `DE_DA_DOI` | Đề bài vừa được cập nhật. Em làm lại theo đề mới nhé. |

Dữ liệu thử (profile `dev`, compose v2): `du-lieu-thu/giao-bai.sql` giao mọi bài đang phát hành ở «12A1 thử» cho An, Bình, Chi, hạn 7 ngày. Đăng nhập `hs.an@demo.local` / `hocsinh123` là có danh sách bài thật.

### SSE `POST /api/hs/gia-su` (ADR 010, research R4)

```text
event: trang_thai
data: {"buoc":"kho"}

event: trang_thai
data: {"buoc":"goi"}

event: trang_thai
data: {"buoc":"loc"}

event: xong
data: {"noiDung":"…câu đã lọc và đã qua cổng…","trichDan":[{"n":1,"loai":"cong_thuc","id":"…","doan":"…"}],"cheDo":"thang_goi_y|mo_hinh|tu_choi","nhan":"Gia sư AI"}
```

- Lỗi: `event: loi`, `data: {"thongBao":"…không chuyển nhà, không gửi lại…"}`. Không có `xong` sau `loi`.
- Dừng: client hủy kết nối; core hủy lượt, không ghi câu muộn.
- Không bao giờ gửi từng phần câu, không gửi phần «suy nghĩ» của nhà.

## Giáo viên (vai trò `TEACHER`, chỉ lớp mình dạy)

| Phương thức | Đường dẫn | Vào | Ra | FR |
| --- | --- | --- | --- | --- |
| GET | `/api/gv/lop` | — | `{tenLop, siSo, canhBaoKet: [{hocSinh, kyNang, loai: KET\|NHO_GV}], sanSangAi: {nha, congThucDaKhoa: bool}}` | 25, 29 |
| GET | `/api/gv/duyet` | — | hàng đợi: `[{runId, loai: BAI\|CONG_THUC_GIA_SU, ma, trangThai: SAI\|KHONG_KIEM_DUOC, canCu: [{tang, trangThai, lyDo, trichDan?}], cu: bool, thaoTac: DUYET\|KIEM_LAI\|SUA_BAI\|THEM_VAO_BANG}]` (mục cũ chỉ có `KIEM_LAI`) | 4, 5 |
| POST | `/api/gv/duyet/{runId}` | `{ghiChu}` (bắt buộc) | chỉ cho `loai = BAI`, `trangThai = KHONG_KIEM_DUOC`: `{trangThai: GV_DUYET, nguoiDuyet, luc}`. 409 nếu mục là `SAI`, hoặc run đã cũ: không phải run mới nhất của bài trong lớp, `stale`, hay `content_hash` / bảng công thức đã đổi (phải kiểm lại bằng `POST /api/gv/ngan-hang/kiem`). 422 nếu mục là `CONG_THUC_GIA_SU`: không duyệt riêng, phải thêm vào bảng công thức rồi khóa phiên bản mới (ADR 013) | 5 |
| GET | `/api/gv/ngan-hang` | — | `[{maBai, muc4, muc3, kyNang, trangThai, cu}]` | 3, 4 |
| POST | `/api/gv/ngan-hang/kiem` | `{maBai?}` | chạy lại cổng; trả trạng thái mới | 4 |
| POST | `/api/gv/giao-bai` | `{maBai, hocSinh?: [id], han?}` (thiếu `hocSinh` = cả lớp) | danh sách giao; 409 nếu bài chưa `DA_PHAT_HANH` cho lớp này | 31 |
| GET | `/api/gv/tai-lieu` | — | `[{id, tieuDe, quyenDung, soDoan, phienBan}]` | 2 |
| POST | `/api/gv/tai-lieu` | `multipart`: tệp PDF ≤ 10 MB, `tieuDe`, `quyenDung` | tài liệu + số đoạn trích được | 2 |
| GET | `/api/gv/cong-thuc` | — | `{phienBan, trangThai: NHAP\|KHOA, cacDong: [{id, tieuDe, latex, phatBieu, tang1, trichDan}]}` | 1 |
| PUT | `/api/gv/cong-thuc` | bản nháp các dòng | bản nháp | 1 |
| POST | `/api/gv/cong-thuc/khoa` | — | phiên bản mới + kết quả tầng 1, 2 từng dòng; 422 nếu dòng nào chưa qua (ADR 013) | 1 |
| GET | `/api/gv/tien-do` | `?muc=4\|3` | ma trận học sinh × kỹ năng → mức (3 mức chỉ đổi khi hiển thị) | 29 |
| GET | `/api/gv/hoc-sinh/{id}` | — | bài đã nộp, lỗi từng bước, các lượt gia sư, mức máy tính và mức ghi đè | 30 |
| PUT | `/api/gv/hoc-sinh/{id}/muc/{kyNang}` | `{muc4, lyDo}` (bắt buộc lý do) | ghi đè đang hiệu lực | 34 |
| DELETE | `/api/gv/hoc-sinh/{id}/muc/{kyNang}` | — | gỡ ghi đè (ghi người, thời điểm) | 34 |
| POST | `/api/gv/hoc-sinh/{id}/bai-ke` | `{maBai, lyDo}` (bài đã phát hành) | bài kế chọn tay | 35 |
| GET | `/api/gv/cai-dat` | — | `{moLoiGiaiSauKhiNop, nhaAi, choPhepMayCucBo, cacNhaDuocBat[]}` | 6, 19 |
| PUT | `/api/gv/cai-dat` | như trên (`nhaAi` ∈ `cacNhaDuocBat`) | cài đặt; 422 khi `choPhepMayCucBo=true` mà máy chủ không bật `app.tutor.allow-local` (chạy trong container: luôn tắt, research R3) | 6, 19 |
| GET | `/api/gv/gia-su` | — | trạng thái từng nhà do máy chủ bật (thử `GET /models`), không có khóa | 19 |
