# Hợp đồng API vòng học tập U5–U14

> Hợp đồng dùng cho đặc tả/hiện thực; không là chứng nhận mọi endpoint đã qua nghiệm thu. [Spec 002](../specs/002-hoan-thien-v2/spec.md) và [plan](../specs/002-hoan-thien-v2/plan.md) bổ sung ca quyền, chế độ, hạn và cạnh tranh yêu cầu.

Các đường dưới yêu cầu Bearer JWT; id người thao tác luôn lấy từ token. JSON camelCase. Lỗi dùng problem+json. Giáo viên chỉ đọc/ghi lớp mình dạy; /api/qt dành ADMIN hoặc SCHOOL_ADMIN. ADMIN quản lý toàn hệ thống. SCHOOL_ADMIN chỉ xem người dùng/lớp đã được phân công, bật/tắt tài khoản học sinh/giáo viên trong phạm vi và phân công trong lớp mình; tạo tài khoản/lớp mới dành ADMIN.

DTO dùng chung `Item`: `{id:string,title:string,subtitle:string,status:string,details:object}`. `details` chứa dữ liệu mô tả của từng dòng (bên dưới). `Result`: `{message:string,count:number}`.

- `GET /api/gv/lop` → Item[]; id lớp, title tên, details `{grade,schoolYear}`.
- `GET /api/gv/tong-quan?lopId=UUID` → `{students:number,stuck:number,pending:number,assignments:number,distribution:{NHAN_BIET:number,THONG_HIEU:number,VAN_DUNG:number,VAN_DUNG_CAO:number},deadlines:Item[]}`.
- `GET /api/gv/canh-bao?lopId=UUID` → Item[]; cảnh báo `KET` chưa xử lý của học sinh còn trong lớp, title tên em, subtitle kỹ năng/lý do, details `{studentId}`. Liên kết tới hồ sơ của em.
- `GET /api/gv/tien-do?lopId=UUID` → Item[]; id học sinh, title tên; details `{skills:[{code,name,level4,level3,mastery,overridden}],submitted:number}`.
- `GET /api/gv/hoc-sinh/{id}?lopId=UUID` → `{student:Item,history:Item[]}`; history details `{problemCode,result,submittedAt}`.
- `POST /api/gv/muc-hieu` body `{lopId,studentId,skillCode,level4,reason}` → Result. Bốn mã mức NHAN_BIET/THONG_HIEU/VAN_DUNG/VAN_DUNG_CAO; lý do bắt buộc.
- `GET /api/gv/ngan-hang?lopId=UUID` → Item[]; id UUID bài, title mã, subtitle đề; status trạng thái phát hành; details `{code,skillCode,level4,gate,latex}`. FE lọc các trường này.
- `POST /api/gv/ngan-hang` body `{lopId,code,skillCode,level4,statement,latex,function}` → Result. Chỉ chủ đề DH12 hiện tại; máy giải + kiểm 3 tầng rồi lưu trạng thái. Không tự phát hành khi thiếu căn cứ.
- `POST /api/gv/goi-bai` body `{lopId,studentId,problemId,reason}` → Result; giao bài ôn nếu chưa có, ưu tiên bài này; ghi đè hết hiệu lực khi em bắt đầu nộp bước.
- `GET /api/gv/giao?lopId=UUID` → Item[]; details `{studentId,problemCode,mode,dueAt}`.
- `POST /api/gv/giao` body `{lopId,studentIds:UUID[],problemIds:UUID[],mode:"ON_TAP"|"KIEM_TRA",dueAt:ISO|null,setName:string}` → Result. studentIds rỗng là cả lớp. Mỗi bài/học sinh một giao đang hiệu lực; giao lại cập nhật chế độ/hạn.
- `GET /api/hs/che-do` → Item[]; id = mã bài; status = mode; details `{dueAt}`. Khóa gia sư và hạn nộp kiểm ở máy chủ.
- `GET /api/hs/goi-bai` → Item[] (tối đa 1); id = mã bài; title mã, subtitle lý do; details `{level4,skillCode}`. Chỉ bài ôn đã giao cho chính em, còn phát hành trong lớp, còn hạn và chưa nộp ở phiên bản hiện tại. Status `CHUA_LOI` / `CUNG_CO` / `NANG_1_NAC` / `DE_HON` / `THAY_CO_GIAO`; không vượt mức hiểu hiệu lực +1 kể cả ghi đè. Bài vừa đạt ưu tiên tiếp kỹ năng đó, mức bài kế chỉ hơn bài vừa làm một nấc; nếu mastery vừa tăng thì không tăng lần nữa. Lỗi chỉ dùng tên mã khi đủ độ tin cậy. Sai cùng bước tới ngưỡng BKT thì dễ hơn; thiếu bài dễ hơn trả `GUI_THAY_CO` với id rỗng, chỉ lời khuyên, không liên kết bài.
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
- `GET /api/hs/ke-hoach-tuan?tuan=0` → `{sessions:Item[],loiKhuyen:string}`; tuần tương đối −52…52 theo `Asia/Ho_Chi_Minh`, ba buổi gợi ý 20 phút (thứ hai/tư/bảy, 19:30). Dựa mức hiểu hiệu lực của kỹ năng yếu nhất và chuỗi sai cùng bước; có cả kỹ năng nền đã gặp trong bài lớp này. Chưa có lịch sử dùng lịch/lời khuyên bắt đầu. Sessions status `GOI_Y`, id `goi-y-*`, không lưu/đánh dấu hoàn thành bằng API buổi tự thêm.
- `GET /api/hs/lich` → Item[]; id lịch; title hoạt động; status CHUA_XONG/XONG; details `{startsAt,endsAt}`.
- `POST /api/hs/lich` body `{title,startsAt:ISO,endsAt:ISO}` → Result; tạo phiên học trong khung rảnh.
- `POST /api/hs/lich/{id}/xong` → Result.
- `GET /api/hs/nhac` → Item[]; ưu tiên buổi gợi ý hôm nay theo giờ Việt Nam, sau đó lịch tự thêm trong 24h và bài sắp hết hạn; id/status/title/details như lịch/giao bài.
- `GET /api/qt/nguoi-dung` → Item[]; title tên; subtitle email; status role; details `{enabled,synthetic}`.
- `POST /api/qt/nguoi-dung` body `{email,displayName,password,role,synthetic:true}` → Result.
- `POST /api/qt/nguoi-dung/bulk` body `{ids:UUID[],enabled:boolean}` → Result; không tự khóa mình; thu hồi phiên khi khóa.
- `GET /api/qt/lop` → Item[].
- `POST /api/qt/lop` body `{name,grade:number,schoolYear:"2026-2027"}` → Result.
- `POST /api/qt/lop/phan-cong` body `{lopId,userId,role:"TEACHER"|"STUDENT"}` → Result; vai trò tài khoản phải khớp.
- `GET /api/qt/nhat-ky` → Item[]; title hành động, subtitle lý do; details `{actorId,createdAt}`.

API có sẵn giữ nguyên: GET /api/hs/trang-hoc → `{ten,soKyNang:[{kyNang,tenKyNang,muc4,ket}],hoanThanh:{kyNang,chuDe}}`; GET /api/hs/bai, GET /api/hs/bai/{ma}, POST /buoc, POST /nop.

Danh sách `GET /api/hs/bai` bổ sung `tenBo: string | null` từ `assignments.set_name`, để gom bài theo bộ đã giao; trường luôn có mặt, `null` khi giao cũ không có tên. Các trường còn lại giữ hợp đồng spec 001. Hết hạn khi ghi bước/nộp trả 403 problem+json với `lyDo: "HET_HAN"` (spec 002 U4, ADR 017); đọc bài làm đã lưu vẫn được.

## Ràng buộc nghiệm thu bổ sung của epic 002

- Mọi yêu cầu tutor và ghi bước/nộp đọc giao đang hiệu lực từ server. `KIEM_TRA` trả 403 trước mở SSE/truy hồi/nhà AI, kể cả mode giả, tab cũ và `dueAt=null`; không có giao hợp lệ thì từ chối. Gia sư kiểm lại quyền ngay trước khi phát câu. Đây là yêu cầu policy theo ADR 017, cần integration test trước khi công bố đạt.
- `now >= dueAt` là hết hạn theo giờ máy chủ: từ chối ghi bước/nộp/tutor bằng 403 problem+json với mã lỗi phân biệt. Đọc bài đã lưu theo quyền vẫn được. Một giao hiệu lực mỗi bài/em; giao lại cập nhật chế độ/hạn và phải nhất quán với yêu cầu đang xử lý.
- Công thức nháp/chặn/stale không xuất cho học sinh hoặc CSV. CSV phải escape dấu phẩy/nháy/xuống dòng, bảo vệ ô có thể thực thi công thức bảng tính; nhập Quizlet thủ công qua bảng tính, không API ghi.
- Nhắc chỉ trong app, lịch đã hoàn thành không nhắc lại; không đồng nghĩa đã gửi push/email/Zalo. Bulk quản trị kiểm toàn tập trước ghi, không cập nhật một phần khi có ID ngoài quyền.

Các thay đổi của lane quyết định khách chỉ thêm Flyway V16 trở lên; V14/V15 dành cho lane khác, migration đã có bất biến. Không nới cổng ADR 013 khi giáo viên xác nhận đã xem lời gia sư.


## Quyết định khách xác nhận ngày 2026-10-14

- `POST /api/hs/bai/{maBai}/loi-giai` là thao tác xác nhận xem toàn bộ, trả `{loiGiai, nguon: "GIAO_VIEN" | "MO_HINH"}`. Cần bài đang phát hành đúng phiên bản, ghi danh, lần nộp đủ đã commit và không đang làm lại cùng phiên bản. Kiểm tra còn hạn hoặc không có hạn trả 403; chưa nộp trả 409. `POST …/nop` không còn trả nội dung lời giải. `solution_reveals` ghi lần xem đầu tiên theo bài làm; `details.reveals` ở tiến độ và `details.revealedAt` ở lịch sử giúp giáo viên phân biệt học có xem lời giải. BKT không được cập nhật lần hai khi xem.
- `POST /api/gv/soan-nhap` nhận `{lopId, documentId}`. Tài liệu phải thuộc lớp giáo viên quản lý và có quyền dùng. AI không nhận dữ liệu học sinh. Đề/lời giải chưa dùng được cho học sinh cho đến khi giáo viên duyệt trong `GET /api/gv/duyet`, loại `CONTENT_DRAFT`, qua `POST /api/gv/duyet/{id}` với lý do. Duyệt tạo bài qua bộ kiểm hiện có; kết quả sai vẫn bị chặn. Token nhà trả về được lưu trong `ai_usage`.
- `GET /api/hs/chu-de`, `GET /api/hs/tu-hoc?chuDe=…` chỉ trả nội dung đã phát hành thuộc khối của lớp. `POST /api/hs/tu-hoc/{code}` thêm lượt ôn tập nếu chưa giao; không ghi đè chế độ/hạn của giáo viên. Gợi ý cá nhân tiếp tục qua `/api/hs/goi-bai`.
- `GET /api/hs/nhac/push` chỉ trả `{available, publicKey}`. POST đăng ký, DELETE hủy trên thiết bị với `{endpoint, keys: {p256dh, auth}}`; người nhận lấy từ phiên. Không đưa khóa riêng VAPID ra frontend. Nhắc lịch trong ứng dụng vẫn hoạt động khi không cấu hình hoặc thiết bị từ chối.

Sau cổng xác nhận, nhánh giải thích tái sử dụng `HintModel` bằng đề công khai, không đưa lời giải chuẩn hay định danh vào prompt. Chỉ JSON đủ năm bước và toàn văn được cổng công thức giữ nguyên mới hiển thị với nguồn `MO_HINH`; lỗi, thiếu bước, `offline` hoặc chưa xác minh được dùng lời giải đã lưu với nguồn `GIAO_VIEN`. Gia sư đang làm bài không thay đổi. `ai_usage` đếm lượt gọi giải thích; token là NULL khi `HintModel` không cung cấp số đo, không giả định bằng 0.
