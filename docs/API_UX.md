# Hợp đồng API vòng học tập U5–U14

Các đường dưới yêu cầu Bearer JWT; id người thao tác luôn lấy từ token. JSON camelCase. Lỗi dùng problem+json. Giáo viên chỉ đọc/ghi lớp mình dạy; /api/qt dành ADMIN hoặc SCHOOL_ADMIN. ADMIN quản lý toàn hệ thống. SCHOOL_ADMIN chỉ xem người dùng/lớp đã được phân công, bật/tắt tài khoản học sinh/giáo viên trong phạm vi và phân công trong lớp mình; tạo tài khoản/lớp mới dành ADMIN.

DTO dùng chung `Item`: `{id:string,title:string,subtitle:string,status:string,details:object}`. `details` chứa dữ liệu mô tả của từng dòng (bên dưới). `Result`: `{message:string,count:number}`.

- `GET /api/gv/lop` → Item[]; id lớp, title tên, details `{grade,schoolYear}`.
- `GET /api/gv/tong-quan?lopId=UUID` → `{students:number,stuck:number,pending:number,assignments:number,distribution:{NHAN_BIET:number,THONG_HIEU:number,VAN_DUNG:number,VAN_DUNG_CAO:number},deadlines:Item[]}`.
- `GET /api/gv/tien-do?lopId=UUID` → Item[]; id học sinh, title tên; details `{skills:[{code,name,level4,level3,mastery,overridden}],submitted:number}`.
- `GET /api/gv/hoc-sinh/{id}?lopId=UUID` → `{student:Item,history:Item[]}`; history details `{problemCode,result,submittedAt}`.
- `POST /api/gv/muc-hieu` body `{lopId,studentId,skillCode,level4,reason}` → Result. Bốn mã mức NHAN_BIET/THONG_HIEU/VAN_DUNG/VAN_DUNG_CAO; lý do bắt buộc.
- `GET /api/gv/ngan-hang?lopId=UUID` → Item[]; id UUID bài, title mã, subtitle đề; status trạng thái phát hành; details `{code,skillCode,level4,gate,latex}`. FE lọc các trường này.
- `POST /api/gv/ngan-hang` body `{lopId,code,skillCode,level4,statement,latex,function}` → Result. Chỉ chủ đề DH12 hiện tại; máy giải + kiểm 3 tầng rồi lưu trạng thái. Không tự phát hành khi thiếu căn cứ.
- `POST /api/gv/goi-bai` body `{lopId,studentId,problemId,reason}` → Result; giao bài ôn nếu chưa có, ưu tiên bài này; ghi đè hết hiệu lực khi em bắt đầu nộp bước.
- `GET /api/gv/giao?lopId=UUID` → Item[]; details `{studentId,problemCode,mode,dueAt}`.
- `POST /api/gv/giao` body `{lopId,studentIds:UUID[],problemIds:UUID[],mode:"ON_TAP"|"KIEM_TRA",dueAt:ISO|null,setName:string}` → Result. studentIds rỗng là cả lớp. Mỗi bài/học sinh một giao đang hiệu lực; giao lại cập nhật chế độ/hạn.
- `GET /api/hs/che-do` → Item[]; id = mã bài; status = mode; details `{dueAt}`. Khóa gia sư và hạn nộp kiểm ở máy chủ.
- `GET /api/hs/goi-bai` → Item[] (tối đa 1); id = mã bài; title mã, subtitle lý do; details `{level4,skillCode}`. Bài đã giao còn phát hành, ưu tiên kỹ năng yếu và +1 mức; ghi đè giáo viên được xét.
- `GET /api/hs/kho` và `GET /api/gv/kho?lopId=UUID` → `{formulas:Item[],documents:Item[]}`. Công thức: title tên, subtitle LaTeX, status trạng thái bảng; details `{statement,tier1,tier2,version}`. Tài liệu: title tên, subtitle nội dung, status quyền dùng.
- `GET /api/hs/flashcards/export` → `{filename:"cong-thuc.csv",content:string}`. UTF-8 CSV hai cột thuật ngữ, định nghĩa; escape nháy/xuống dòng và chống spreadsheet injection. FE tạo Blob tải về.
- `POST /api/gv/tai-lieu` multipart fields `lopId`, `title`, `licenseStatus` (`tu_soan` mặc định), `file` (.txt/.md/.pdf ≤5MiB) → Result. PDF có text; scan rỗng báo 400.
- `GET /api/gv/cong-thuc?lopId=UUID` → Item[] (kể cả nháp).
- `POST /api/gv/cong-thuc` body `{lopId,title,latex,statement}` → Result; thêm vào bảng NHAP, chưa phát hành cho học sinh.
- `POST /api/gv/cong-thuc/kiem?lopId=UUID` → Result; kiểm dòng nháp qua dịch vụ toán và khóa khi mọi dòng đạt có trích dẫn.
- `GET /api/gv/duyet?lopId=UUID` → Item[]; id lượt kiểm; title mã bài; subtitle lý do; status KHONG_KIEM_DUOC; details `{kind:"PROBLEM",tiers:[{tier,status,reason,citation}]}` hoặc `{kind:"TUTOR",draft}`. Duyệt lời gợi ý chỉ xác nhận giáo viên đã xem; không bỏ qua cổng an toàn khi gia sư chạy.
- `POST /api/gv/duyet/{id}` body `{lopId,approve:boolean,reason:string}` → Result; chỉ duyệt lượt mới nhất còn hiệu lực; SAI không thể duyệt.
- `GET /api/gv/cai-dat?lopId=UUID` → `{lopId,revealSolutionAfterSubmit:boolean,aiProvider:string}`.
- `PUT /api/gv/cai-dat` body `{lopId,revealSolutionAfterSubmit,aiProvider:"offline"}` → Result.
- `POST /api/hs/gia-su` body `{maBai:string,maBuoc:string,cap:1|2|3,cauHoi:string}` → SSE `event: kho`, `goi`, `loc`, `xong`/`loi`; data `{status:string,message:string}`. Chỉ xong mang câu gia sư sau bộ lọc CAS; kiểm tra bị 403 trước SSE. Không stream token. Gia sư offline không đọc lời giải.
- `GET /api/hs/lich` → Item[]; id lịch; title hoạt động; status CHUA_XONG/XONG; details `{startsAt,endsAt}`.
- `POST /api/hs/lich` body `{title,startsAt:ISO,endsAt:ISO}` → Result; tạo phiên học trong khung rảnh.
- `POST /api/hs/lich/{id}/xong` → Result.
- `GET /api/hs/nhac` → Item[] lịch tới hạn trong 24h và bài sắp hết hạn; id/status/title/details như lịch/giao bài.
- `GET /api/qt/nguoi-dung` → Item[]; title tên; subtitle email; status role; details `{enabled,synthetic}`.
- `POST /api/qt/nguoi-dung` body `{email,displayName,password,role,synthetic:true}` → Result.
- `POST /api/qt/nguoi-dung/bulk` body `{ids:UUID[],enabled:boolean}` → Result; không tự khóa mình; thu hồi phiên khi khóa.
- `GET /api/qt/lop` → Item[].
- `POST /api/qt/lop` body `{name,grade:number,schoolYear:"2026-2027"}` → Result.
- `POST /api/qt/lop/phan-cong` body `{lopId,userId,role:"TEACHER"|"STUDENT"}` → Result; vai trò tài khoản phải khớp.
- `GET /api/qt/nhat-ky` → Item[]; title hành động, subtitle lý do; details `{actorId,createdAt}`.

API có sẵn giữ nguyên: GET /api/hs/trang-hoc → `{ten,soKyNang:[{kyNang,tenKyNang,muc4,ket}],hoanThanh:{kyNang,chuDe}}`; GET /api/hs/bai, GET /api/hs/bai/{ma}, POST /buoc, POST /nop.
