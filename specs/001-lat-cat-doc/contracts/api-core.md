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
| GET | `/api/hs/bai` | — | danh sách bài được giao / đã phát hành: `{maBai, tieuDe, muc4, han, trangThai}` | 31 |
| GET | `/api/hs/bai/{maBai}` | — | `{de: {text, latex}, cacBuoc: [{maBuoc, moTa, dangNhap}], baiLam: {cacBuoc: [{maBuoc, dong, latex, ketQua, thongBao, oSai[]}], trangThai}, coTheMoLoiGiai}` | 6–10 |
| POST | `/api/hs/bai/{maBai}/buoc` | `{maBuoc, dong?: [{dong, latex, loai?}], bang?: [{hang, k, giaTri}], suKien?: [{maBuoc, hang?, k?, giaTriCu?, giaTriMoi, luc}]}` (`suKien`: sự kiện nhập mới kể từ lần nộp trước, tối đa 500) | `{ketQua: DAT\|SAI\|KHONG_KIEM_DUOC\|KHONG_CHAM_DUOC, thongBao, oSai: [{maBuoc, dong?, hang?, k?}], maLoi?, buocKe?}` — không có giá trị đúng; yêu cầu chấm dựng từ các bước đã lưu, không từ máy học sinh | 8–10 |
| POST | `/api/hs/bai/{maBai}/nop` | — | `{ketQua: DAT\|SAI\|KHONG_KIEM_DUOC, mucHieu: [{kyNang, muc4Truoc, muc4Sau}], loiGiai?}`. `ketQua` là phán quyết đã ghi của bước kết luận trên nội dung hiện tại của bài làm, ghim làm căn cứ; nộp bài không gọi dịch vụ toán. `loiGiai` chỉ khi lớp bật cờ, bài còn phát hành đúng phiên bản của bài làm, và em không đang làm lại bài (cờ đọc lúc trả lời). Gửi lại sau khi đã nộp trả y hệt. 409 khi bước kết luận chưa có phán quyết cho nội dung hiện tại (chưa nộp, chấm lỗi `KHONG_CHAM_DUOC`, hay đã sửa một bước sau lần chấm đó), hoặc bài làm dở là của đề đã đổi; `detail` nói em phải làm gì. 404 như `…/buoc` | 6, 23 |
| POST | `/api/hs/gia-su` | `{maBai, cauHoi?, chip?: GOI_Y\|SAI_CHO\|GUI_THAY_CO}` | **SSE**, xem dưới | 11–21 |
| GET | `/api/hs/gia-su/{maBai}` | — | lịch sử: `[{vaiTro, noiDung, trichDan[], nhan: "Gia sư AI", luc}]` | 22 |
| GET | `/api/hs/lich` | — | `{tuan: [{thu, gio, viec}], loiKhuyen, nhacHomNay[]}` | 27–28 |
| GET | `/api/hs/kho` | — | `{congThuc: [{id, tieuDe, latex, trichDan}], taiLieu: [{id, tieuDe, doan[]}]}` (đích của `[n]`: `#ct-<id>`, `#tl-<id>`) | 21 |

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
