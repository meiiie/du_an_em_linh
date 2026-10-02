# Feature Specification: P2 — Lát cắt dọc: một vòng của sơ đồ cho «đơn điệu và cực trị»

**Feature Branch**: `001-lat-cat-doc`  
**Created**: 2026-10-02  
**Status**: Draft  
**Input**: Chủ repo duyệt hướng lát cắt dọc (2026-10-02): chạy hết vòng của sơ đồ «Phần mềm học toán với AI» trên v2 cho một chủ đề đã kiểm định ở v0 — Toán 12, đơn điệu và cực trị. Pha P2 (`docs/product/LO-TRINH.md`). Năng lực C1–C10 ở mức tối thiểu cho một chủ đề (`docs/product/MUC-TIEU.md`).

**Phạm vi một câu:** giáo viên chuẩn bị nội dung đã kiểm (bảng công thức, tài liệu, bài qua cổng 3 tầng) → học sinh làm bài theo bước, hỏi gia sư không đưa đáp án, mọi công thức trong lời giảng đã qua cổng 3 tầng → mức hiểu cập nhật → bài kế nâng đúng 1 nấc → lịch tuần; giáo viên duyệt mục không kiểm được và theo dõi lớp.

## User Scenarios & Testing *(mandatory)*

Nhân vật: **An** (học sinh, tài khoản tổng hợp `hs.an@demo.local`), **Chi** (học sinh hay kẹt), **giáo viên thử** (`gv@demo.local`) của lớp «12A1 thử». Mọi dữ liệu là dữ liệu tổng hợp (ADR 006).

### User Story 1 — Học sinh làm một bài theo bước, máy chấm từng bước (Priority: P1)

An mở bài kế tiếp trên trang «Học», làm theo khung 5 bước của chủ đề: tập xác định, đạo hàm, nghiệm của y′ = 0, xét dấu, kết luận. Mỗi lần nộp một bước, máy chấm và nói bước đó hợp lệ hay sai, sai ở đâu, bằng lời của lớp 12, không nêu kết quả đúng. Ô sai được tô. Xong bài, An thấy kết quả bài và mức hiểu mới của kỹ năng.

**Why this priority**: Đây là khối «làm bài tập theo giai đoạn» của sơ đồ; gia sư, mức hiểu và bài kế đều dựa trên kết quả chấm từng bước.

**Independent Test**: Đăng nhập An → mở một bài đã phát hành → nộp bước đạo hàm sai → thấy thông báo nói về đạo hàm và ô bị tô → sửa đúng → thấy «hợp lệ» → tải lại trang vẫn thấy các bước đã nộp.

**Acceptance Scenarios**:

1. **Given** An đã đăng nhập và bài `DH12-03-VD-01` đã phát hành, **When** An nộp bước đạo hàm `3x^{2}-12x` (thiếu hằng số), **Then** máy báo bước đạo hàm sai bằng lời, tô ô sai, và không hiện đạo hàm đúng.
2. **Given** bước đạo hàm vừa sai, **When** An sửa thành `3x^{2}-12x+9` và nộp, **Then** máy báo hợp lệ và mở bước kế.
3. **Given** An đang làm dở, **When** tải lại trang, **Then** thấy lại mọi bước đã nộp và kết quả của từng bước.
4. **Given** bộ chấm lỗi hoặc hết giờ, **When** An nộp một bước, **Then** hệ thống báo «chưa chấm được, thử lại», bước đó **không** được tính là đạt.
5. **Given** lớp để cờ «mở lời giải sau khi nộp» ở mặc định (tắt), **When** An nộp xong cả bài, **Then** không thấy lời giải mẫu.

---

### User Story 2 — Học sinh hỏi gia sư; gia sư giảng mà không đưa đáp án, công thức trong lời giảng đã qua cổng 3 tầng (Priority: P1)

Kẹt ở một bước, An mở gia sư ngay trên phiếu làm bài. Gia sư biết bước nào sai và loại lỗi gì, trả lời theo thang gợi ý ba cấp, không bao giờ làm hộ. Trong lúc chờ, An thấy gia sư đang làm gì (mở kho lớp → hỏi mô hình → kiểm câu trả lời), rồi thấy cả câu đã kiểm. Mỗi công thức tổng quát trong câu đều đã qua ba tầng: máy kiểm, có trong tài liệu của lớp (trích dẫn bấm được), khớp bảng công thức giáo viên đã khóa. Câu nào không qua được thì không tới An.

**Why this priority**: Đây là trái tim của sơ đồ: mũi tên «kiểm tra công thức 3 tầng» đi vào «học cùng AI», và «sửa bài, giảng cho hiểu, không đưa đáp án». v0 mới kiểm 3 tầng cho đề bài, chưa kiểm công thức trong lời giảng.

**Independent Test**: Dùng chế độ offline và một nhà AI giả trả về các câu dựng sẵn (lộ đáp án, công thức sai, công thức đúng nhưng ngoài bảng, công thức trong bảng có trích dẫn, kết quả tính cụ thể của bài) rồi kiểm cái gì tới màn của An.

**Acceptance Scenarios**:

1. **Given** An đang ở bài có bước đạo hàm sai, **When** An gõ «cho em đáp án» ba lần, **Then** hai lần đầu gia sư nhắc lại gợi ý, lần ba từ chối và gợi ý nghỉ hoặc gửi thầy cô; không lần nào có đáp án. Câu «đừng nêu đáp án» không bị coi là xin đáp án.
2. **Given** bước đạo hàm sai, **When** An bấm «Gợi ý» ba lần, **Then** nhận lần lượt cấp 1, 2, 3 của thang đúng (bước, loại lỗi); không cấp nào nêu kết quả của bước.
3. **Given** nhà AI trả về câu chứa kết quả của bài, **When** câu đi qua bộ lọc, **Then** An nhận gợi ý đã kiểm của bước hoặc một câu từ chối không chứa kết quả.
4. **Given** nhà AI trả về một quy tắc đạo hàm sai, **When** câu đi qua cổng, **Then** quy tắc đó không tới An; hệ thống ghi một mục cho giáo viên xem.
5. **Given** nhà AI trả về một công thức đúng nhưng không có trong bảng đã khóa, **When** câu đi qua cổng, **Then** công thức bị rút khỏi câu và không tới An (câu mất nghĩa thì được thay bằng gợi ý đã qua cổng từ trước); giáo viên thấy một mục «không kiểm được».
6. **Given** nhà AI dùng một công thức có trong bảng đã khóa và trong tài liệu của lớp, **When** câu tới An, **Then** công thức hiện kèm số trích dẫn `[n]`, bấm vào mở đúng đoạn trong kho lớp mà không rời phiếu.
7. **Given** nhà AI lỗi hoặc hết giờ, **When** An hỏi, **Then** An nhận lời báo lỗi; hệ thống không tự chuyển sang nhà khác và không tự gửi lại.
8. **Given** một lượt đang chạy, **When** An bấm Dừng, **Then** lượt bị hủy và câu đến muộn không hiện.
9. **Given** lớp chưa có nhà AI nào được bật, **When** An hỏi, **Then** gia sư chạy chế độ offline (thang gợi ý mẫu đã kiểm), An vẫn học được.

---

### User Story 3 — Giáo viên chuẩn bị nội dung đã kiểm (Priority: P1)

Giáo viên xem bảng công thức của chủ đề, sửa nếu cần rồi khóa. Mỗi lần khóa là một phiên bản. Giáo viên nạp tài liệu PDF có chữ (SGK, sách tham khảo) và khai quyền dùng. Ngân hàng bài của chủ đề (nhập từ v0) đi qua cổng 3 tầng: bài đạt cả ba tầng thì phát hành, bài có tầng sai thì bị chặn, bài có tầng không kiểm được thì chờ giáo viên duyệt.

**Why this priority**: Tầng 2 và tầng 3 của cổng cần tài liệu và bảng công thức của chính giáo viên; giáo viên kiểm soát nội dung là bất biến của sản phẩm.

**Independent Test**: Đăng nhập giáo viên → khóa bảng công thức → nạp một tài liệu → chạy kiểm ngân hàng → thấy trạng thái từng bài và căn cứ của từng tầng.

**Acceptance Scenarios**:

1. **Given** bảng công thức chưa khóa, **When** giáo viên khóa, **Then** bảng có phiên bản mới; các kết quả kiểm dựa trên phiên bản cũ được đánh dấu «cũ».
2. **Given** giáo viên nạp một PDF và khai quyền «chưa rõ», **When** cổng chạy tầng 2, **Then** tài liệu đó không được dùng làm căn cứ.
3. **Given** ngân hàng của chủ đề, **When** cổng chạy, **Then** mỗi bài có trạng thái tổng `DAT`, `SAI` hoặc `KHONG_KIEM_DUOC` kèm căn cứ từng tầng (trích đoạn tài liệu, dòng công thức, kết quả máy).
4. **Given** bài đã phát hành, **When** giáo viên đổi bảng công thức, **Then** bài không bị gỡ tự động nhưng hiện dấu «cần kiểm lại».
5. **Given** giáo viên vừa nạp một tài liệu có quyền dùng hợp lệ, **When** An hỏi gia sư về một quy tắc có trong tài liệu đó, **Then** câu gia sư trích dẫn đúng đoạn của tài liệu vừa nạp, bấm vào mở đúng đoạn đó.

---

### User Story 4 — Giáo viên duyệt mục «không kiểm được» (Priority: P1)

Giáo viên mở hàng đợi duyệt, thấy các bài và các công thức trong lời gia sư mà cổng không kiểm được, kèm lý do. Giáo viên duyệt kèm lý do của mình; hệ thống ghi ai duyệt, lúc nào, vì sao. Mục bị cổng chấm sai thì không duyệt được, chỉ sửa được.

**Why this priority**: Không có bước duyệt thì nội dung `KHONG_KIEM_DUOC` không bao giờ tới học sinh, và cổng 3 tầng thiếu đường cho con người quyết định.

**Independent Test**: Đăng nhập giáo viên → hàng đợi có bài `DH12-01-TH-01` «không kiểm được» và bài `DH12-DEMO-CHAN-01` «sai» → duyệt bài thứ nhất có lý do → bài tới được trang của An; bài thứ hai không có nút duyệt và An không thấy.

**Acceptance Scenarios**:

1. **Given** bài ở trạng thái `KHONG_KIEM_DUOC`, **When** giáo viên duyệt kèm lý do, **Then** trạng thái thành `GV_DUYET`, hệ thống ghi người duyệt, thời điểm, lý do, và bài tới được học sinh.
2. **Given** bài ở trạng thái `SAI`, **When** giáo viên mở hàng đợi, **Then** không có thao tác duyệt; học sinh không thấy bài.
3. **Given** một công thức trong lời gia sư bị giữ lại vì không kiểm được, **When** giáo viên thêm công thức đó vào bảng và khóa, **Then** các lượt gia sư sau được dùng công thức đó (đã qua đủ 3 tầng).

---

### User Story 5 — Mức hiểu và bài kế tiếp «nâng 1 nấc» (Priority: P2)

Sau mỗi bài được chấm, mức hiểu của An theo từng kỹ năng được cập nhật và hiện bằng 4 mức (Nhận biết, Thông hiểu, Vận dụng, Vận dụng cao). Trang «Học» đề xuất bài kế kèm lý do: chữa đúng dạng lỗi vừa mắc, củng cố đúng mức, hoặc nâng đúng một nấc. Chi sai cùng một bước nhiều lần thì được đề xuất bài dễ hơn hoặc gửi thầy cô, và giáo viên thấy cảnh báo.

**Why this priority**: Đây là nhánh «bài tập cho từng học sinh: dạng bài đã sai, mức độ hiểu, nâng lên 1 nấc» và dải «tới khi học tới vận dụng cao». Cần chấm (US1) trước.

**Independent Test**: Cho An đạt một bài Nhận biết của một kỹ năng → bài kế là bài Thông hiểu của kỹ năng đó với lý do «nâng 1 nấc»; cho Chi sai bước đạo hàm ba lần → đề xuất bài dễ hơn hoặc gửi thầy cô, giáo viên thấy «Chi» trong cảnh báo kẹt.

**Acceptance Scenarios**:

1. **Given** An vừa đạt bài Nhận biết của kỹ năng X và mức hiểu của X vượt ngưỡng Thông hiểu, **When** An mở trang «Học», **Then** bài kế là bài Thông hiểu của X kèm lý do «nâng 1 nấc».
2. **Given** An vừa sai bước đạo hàm với mã lỗi đủ tin cậy, **When** An mở trang «Học», **Then** bài kế ưu tiên chữa đúng dạng lỗi đó, lý do nêu dạng lỗi bằng lời.
3. **Given** bất kỳ trạng thái nào, **When** hệ thống đề xuất bài kế, **Then** không bao giờ nhảy quá một mức so với mức hiện tại của kỹ năng.
4. **Given** Chi sai cùng một bước tới ngưỡng kẹt, **When** Chi mở trang «Học», **Then** thấy đề xuất bài dễ hơn hoặc gửi thầy cô; giáo viên thấy cảnh báo kẹt có tên Chi.
5. **Given** An bắt đầu một kỹ năng ở Nhận biết, **When** An làm đạt lần lượt các bài được đề xuất, **Then** mức của kỹ năng đi qua Thông hiểu, Vận dụng tới Vận dụng cao, mỗi lần nâng đúng một nấc, rồi An được báo hoàn thành kỹ năng.
6. **Given** An đạt Vận dụng cao ở mọi kỹ năng của chủ đề, **When** mở trang «Học», **Then** thấy báo đã hoàn thành chủ đề.

---

### User Story 6 — Lịch tuần và lời khuyên (Priority: P3)

An xem thời gian biểu tuần lập từ mức hiểu của mình (kỹ năng yếu nhất, tình trạng kẹt), kèm một lời khuyên ngắn. Hôm nay có việc thì trang «Học» nhắc trong ứng dụng.

**Why this priority**: Nhánh «tư vấn phương pháp học + thời gian biểu, có nhắc lịch» của sơ đồ ở mức tối thiểu; kênh nhắc ngoài để P4.

**Independent Test**: Với mức hiểu dựng sẵn, trang «Lịch» hiện đủ các buổi trong tuần, lời khuyên nêu đúng kỹ năng yếu nhất; ở 390 px không tràn ngang.

**Acceptance Scenarios**:

1. **Given** An chưa làm bài nào, **When** mở «Lịch», **Then** thấy lịch mặc định và lời khuyên bắt đầu.
2. **Given** kỹ năng yếu nhất của An đang kẹt, **When** mở «Lịch», **Then** lời khuyên nói ôn đúng dạng đó, chưa tăng độ khó.
3. **Given** hôm nay có một buổi trong lịch, **When** An mở trang «Học», **Then** thấy lời nhắc việc của hôm nay.

---

### User Story 7 — Giáo viên xem tiến độ lớp và từng học sinh (Priority: P3)

Giáo viên xem bảng lớp theo kỹ năng × mức, đổi sang 3 mức Biết / Hiểu / Vận dụng (CV 7991) khi xem, thấy học sinh đang kẹt, mở trang từng học sinh để xem các bài đã nộp, lỗi từng bước và các lượt gia sư.

**Why this priority**: Nhánh giám sát của giáo viên (luồng E ở mức tối thiểu); cần dữ liệu từ US1, US2, US5.

**Independent Test**: Với dữ liệu tổng hợp của lớp, bảng tiến độ hiện đủ kỹ năng × 4 mức; bấm đổi thì thấy 3 mức; trang của Chi liệt kê đủ bài và lượt gia sư.

**Acceptance Scenarios**:

1. **Given** lớp có An, Bình, Chi, **When** giáo viên mở «Mức», **Then** thấy bảng kỹ năng × 4 mức; bấm đổi thấy 3 mức, không ghi gì thêm vào hồ sơ học sinh.
2. **Given** Chi đang kẹt, **When** giáo viên mở trang lớp, **Then** cảnh báo kẹt có tên Chi và dẫn tới trang của Chi.
3. **Given** giáo viên lớp khác, **When** mở trang của An, **Then** bị từ chối.
4. **Given** giáo viên thấy mức «Vận dụng» của Chi ở một kỹ năng là chưa đúng, **When** giáo viên đặt lại thành «Thông hiểu» kèm lý do, **Then** bài kế của Chi tính theo mức giáo viên đặt, hệ thống ghi ai đặt, lúc nào, vì sao; giáo viên gỡ được lần đặt đó.
5. **Given** giáo viên muốn Chi làm một bài cụ thể, **When** giáo viên chọn bài đó làm bài kế kèm lý do, **Then** trang «Học» của Chi hiện bài đó đầu tiên với lý do «thầy cô giao», thay cho đề xuất của máy.

---

### Edge Cases

- Bộ chấm hoặc bộ lọc lỗi, chậm: bước không được tính là đạt; câu gia sư không hiện khi chưa lọc xong (đóng mặc định).
- Lời gia sư chứa LaTeX hỏng hoặc không phân tích được: coi là không kiểm được, không hiện công thức đó.
- Lời gia sư trích lại biểu thức sai của chính An («em viết y′ = …»): được phép khi trùng nguyên văn bài làm của An và được trình bày là lời của An; mọi kết quả tính cụ thể khác không được hiện.
- Giáo viên đổi bảng công thức khi An đang hỏi gia sư: lượt đang chạy dùng phiên bản bảng ở lúc bắt đầu lượt; lượt sau dùng phiên bản mới.
- An nộp cùng một bước từ hai tab: hệ thống ghi một lần, kết quả như nhau.
- Lớp chưa phát hành bài nào hoặc chưa giao bài: trang «Học» có trạng thái rỗng, không lỗi.
- An hỏi ngoài chủ đề hoặc ngoài toán: gia sư đưa về bài đang làm, không trả lời nội dung ngoài lề.
- Nhà AI trả về phần «suy nghĩ» riêng: không bao giờ hiện, không ghi vào lượt.
- Nhà AI được bật cho lớp nhưng chưa có đồng ý `GIA_SU_AI` hoặc ADR 012 chưa được duyệt: chỉ tài khoản tổng hợp được gửi ra ngoài; tài khoản không tổng hợp dùng chế độ offline.

## Requirements *(mandatory)*

### Functional Requirements

**Nội dung và cổng 3 tầng (C1, C2, C3, C5)**

- **FR-001**: Giáo viên MUST xem, sửa và khóa bảng công thức của chủ đề; mỗi lần khóa tạo một phiên bản có dấu vân tay nội dung; đổi bảng làm các kết quả kiểm dựa trên phiên bản cũ thành «cũ».
- **FR-002**: Giáo viên MUST nạp được tài liệu PDF có lớp chữ và khai quyền dùng; tài liệu có quyền «chưa rõ» không được dùng làm căn cứ tầng 2.
- **FR-003**: Hệ thống MUST có ngân hàng bài của chủ đề với đủ thuộc tính của v0: 4 mức, 3 mức CV 7991, mức Bloom, yêu cầu cần đạt (YCCĐ) trích nguyên văn, kỹ năng, khung bước, lời giải ẩn. Nội dung sư phạm của v0 được nhập nguyên văn.
- **FR-004**: Mọi bài MUST qua cổng 3 tầng trước khi tới học sinh: cả ba tầng đạt thì phát hành; một tầng sai thì chặn; còn tầng không kiểm được thì chờ duyệt. Mỗi tầng ghi căn cứ (kết quả máy, trích đoạn tài liệu có vị trí, dòng công thức).
- **FR-005**: Giáo viên MUST duyệt được **bài** ở trạng thái `KHONG_KIEM_DUOC` kèm lý do; hệ thống ghi người duyệt, thời điểm, lý do (`GV_DUYET`). Mục `SAI` không duyệt được. Công thức trong lời gia sư không duyệt riêng được: giáo viên thêm công thức vào bảng rồi khóa phiên bản mới (ADR 013).
- **FR-006**: Lời giải mẫu và dữ kiện bảo vệ của bài MUST NOT tới học sinh khi đang làm; chỉ mở sau khi nộp nếu lớp bật cờ (mặc định tắt).

**Làm bài (C5, C7)**

- **FR-007**: Học sinh MUST làm bài theo khung 5 bước của chủ đề; nhập công thức bằng bàn phím toán, luôn có ô gõ LaTeX dự phòng.
- **FR-008**: Mỗi bước MUST được máy chấm; kết quả nêu loại lỗi bằng lời, mã lỗi khi đủ tin cậy; bước sai được đánh dấu; không hiện kết quả đúng.
- **FR-009**: Lỗi hoặc hết giờ của bộ chấm MUST NOT được coi là đạt.
- **FR-010**: Bài làm dở MUST được lưu sau mỗi lần nộp bước; tải lại trang không mất.

**Gia sư (C5, C7)**

- **FR-011**: Gia sư MUST chỉ nhận đề, bước đang sai, loại kết quả, mã lỗi (khi đủ tin cậy) và gợi ý đã kiểm của bước đó; MUST NOT nhận lời giải mẫu hay dữ kiện bảo vệ.
- **FR-012**: Gia sư MUST theo thang gợi ý 3 cấp theo (bước, loại lỗi); không cấp nào nêu kết quả của bước (không bottom-out).
- **FR-013**: Xin đáp án MUST bị chặn theo luật, không phụ thuộc mô hình: lần 1 và 2 nhắc gợi ý, lần 3 từ chối và gợi ý nghỉ hoặc gửi thầy cô.
- **FR-014**: Mọi câu gia sư MUST qua bộ lọc lộ đáp án trên cả câu, đóng mặc định; câu bị chặn được thay bằng gợi ý đã kiểm của bước hoặc câu từ chối không chứa kết quả, rồi lọc lại.
- **FR-015**: Mọi công thức tổng quát (quy tắc, định lí) trong câu gia sư MUST qua đủ 3 tầng trước khi hiện: máy kiểm không sai, có trong tài liệu được phép dùng của lớp (trích dẫn được), khớp một dòng của bảng công thức đã khóa. Công thức sai hoặc không kiểm được MUST NOT hiện; hệ thống ghi một mục vào hàng đợi duyệt. Biểu thức trích lại nguyên văn từ đề bài, hoặc từ bài làm của chính học sinh (trình bày là lời của học sinh), được phép; mọi kết quả tính cụ thể khác, và mọi đoạn trông như toán mà hệ thống không phân loại được, MUST NOT hiện. Câu thay thế (khi câu mất nghĩa hoặc khi kiểm lỗi) MUST chỉ lấy từ gợi ý đã qua cùng cổng từ trước.
- **FR-016**: Mỗi lượt gia sư MUST báo trạng thái (mở kho lớp → hỏi mô hình → kiểm câu) và chỉ hiện cả câu sau khi kiểm xong; không hiện từng chữ khi mô hình đang sinh.
- **FR-017**: Học sinh MUST dừng được lượt đang chạy; câu đến muộn bị bỏ.
- **FR-018**: Lỗi của nhà AI MUST NOT dẫn tới tự chuyển nhà hay tự gửi lại; học sinh nhận lời báo lỗi. Chế độ offline (thang gợi ý mẫu đã kiểm) MUST luôn dùng được, không cần khóa.
- **FR-019**: Nhà AI và khóa MUST do máy chủ quản lý; giáo viên chỉ bật / tắt nhà cho lớp; không ai thấy khóa trên giao diện.
- **FR-020**: Trước khi gửi ra nhà AI bên thứ ba, hệ thống MUST xóa định danh (email, số điện thoại, tên trong lớp); chỉ tài khoản tổng hợp được gửi ra ngoài cho tới khi ADR 012 được duyệt và có đồng ý `GIA_SU_AI`.
- **FR-021**: Mỗi câu gia sư MUST mang nhãn «Gia sư AI»; số trích dẫn `[n]` mở được đúng nguồn trong kho lớp mà không rời phiếu.
- **FR-022**: Hội thoại gia sư của từng bài MUST được lưu; tải lại trang thấy lại.

**Mức hiểu, bài kế, vòng lặp (C6, C8, C10)**

- **FR-023**: Sau mỗi bài được chấm, hệ thống MUST cập nhật mức hiểu theo kỹ năng bằng mô hình xác suất và ngưỡng của v0; học sinh thấy 4 mức bằng lời, không thấy thuật ngữ Bloom; hệ thống lưu cả mức Bloom tương ứng.
- **FR-024**: Hệ thống MUST đề xuất bài kế kèm lý do: chữa dạng lỗi đã sai, củng cố đúng mức, hoặc nâng đúng 1 nấc; MUST NOT đề xuất bài cao hơn mức hiện tại của kỹ năng quá một nấc.
- **FR-025**: Học sinh sai cùng một bước tới ngưỡng kẹt MUST được đề xuất bài dễ hơn hoặc gửi thầy cô; giáo viên thấy cảnh báo kẹt.
- **FR-026**: Đạt Vận dụng cao ở một kỹ năng MUST được báo là hoàn thành kỹ năng; đạt ở mọi kỹ năng của chủ đề MUST được báo là hoàn thành chủ đề.

**Lịch (C9)**

- **FR-027**: Hệ thống MUST lập thời gian biểu tuần từ mức hiểu (kỹ năng yếu nhất, tình trạng kẹt) kèm một lời khuyên ngắn có căn cứ.
- **FR-028**: Trang «Học» MUST nhắc việc của hôm nay theo lịch. Kênh nhắc ngoài ứng dụng ngoài phạm vi (P4).

**Giáo viên theo dõi (C4, C6)**

- **FR-029**: Giáo viên MUST xem bảng tiến độ lớp theo kỹ năng × 4 mức, đổi sang 3 mức CV 7991 chỉ khi hiển thị.
- **FR-030**: Giáo viên MUST xem trang từng học sinh: bài đã nộp, lỗi từng bước, lượt gia sư.
- **FR-031**: Giáo viên MUST giao được bài đã phát hành cho cả lớp hoặc từng học sinh.
- **FR-034**: Giáo viên MUST ghi đè được mức của một học sinh ở một kỹ năng kèm lý do; mức ghi đè dùng cho bài kế cho tới khi giáo viên gỡ; hệ thống ghi người, thời điểm, lý do và vẫn tính mức của máy song song để giáo viên so.
- **FR-035**: Giáo viên MUST ghi đè được bài kế của một học sinh (chọn một bài đã phát hành kèm lý do); bài đó hiện trước đề xuất của máy, lý do «thầy cô giao»; có ghi người, thời điểm, lý do.

**Quyền và tương đương**

- **FR-032**: Giáo viên MUST chỉ thấy lớp mình; học sinh chỉ thấy dữ liệu của mình (như F-08 của v0).
- **FR-033**: Màn tương đương với v0 MUST giữ route, heading và `data-testid` của v0 (bảng ở phụ lục).

### Key Entities *(include if feature involves data)*

- **Chủ đề**: Toán 12 «đơn điệu và cực trị»; chứa kỹ năng, bảng công thức, ngân hàng.
- **Kỹ năng**: mã như `T12.DH.01`, tên ngắn, YCCĐ; là đơn vị của mức hiểu.
- **Mã lỗi**: như `ERR.DH.01`; gắn kỹ năng, bước, loại kết quả, gợi ý sửa.
- **Bài**: đề, 4 mức, 3 mức, Bloom, kỹ năng, khung bước, lời giải ẩn, trạng thái cổng.
- **Bảng công thức**: phiên bản, dấu vân tay, các dòng công thức (LaTeX, phát biểu, nguồn).
- **Tài liệu**: tệp gốc, quyền dùng, văn bản trích, vị trí đoạn (trang, ký tự).
- **Kết quả kiểm**: đối tượng (bài hoặc công thức trong lời gia sư), tầng, trạng thái, căn cứ, phiên bản bảng, cờ «cũ».
- **Lượt duyệt**: mục được duyệt, người, thời điểm, lý do.
- **Bài làm**: học sinh, bài, các bước đã nộp, kết quả từng bước, mã lỗi.
- **Lượt gia sư**: câu hỏi, câu đã kiểm, trạng thái lọc, công thức đã kiểm, trích dẫn, nhà AI, thời gian.
- **Mức hiểu**: học sinh × kỹ năng: xác suất thành thạo, mức 4, mức Bloom, đếm kẹt, lịch sử thay đổi.
- **Ghi đè của giáo viên**: mức đặt lại hoặc bài kế chọn tay, kèm người, thời điểm, lý do, lúc gỡ.
- **Đề xuất bài kế**: bài, lý do (chữa lỗi / củng cố / nâng 1 nấc / dễ hơn).
- **Thời gian biểu tuần**: các buổi (thứ, giờ, việc), lời khuyên, lời nhắc.
- **Giao bài**: bài, người nhận (lớp hoặc học sinh), hạn.
- **Cấu hình nhà AI của lớp**: nhà được bật, chế độ offline mặc định.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Kịch bản «một vòng» chạy hết không lỗi ở cả màn 390 px và 1280 px, không tràn ngang: giáo viên khóa bảng và nạp một tài liệu → An làm bài, hỏi gia sư, gia sư trích dẫn đoạn của tài liệu vừa nạp → mức hiểu tăng → bài kế nâng 1 nấc → lịch tuần cập nhật.
- **SC-002**: Kịch bản duyệt: một mục `KHONG_KIEM_DUOC` thành `GV_DUYET` có đủ người, thời điểm, lý do, rồi mới tới học sinh; trong mọi kịch bản kiểm thử, 0 mục `SAI` tới học sinh.
- **SC-003**: 0 câu gia sư lộ đáp án trên bộ dụ đáp án và bộ ác ý 288 ca của v0 khi chạy qua luồng gia sư mới (đạt nhầm 0).
- **SC-004**: 0 công thức sai hoặc không kiểm được tới học sinh trên bộ ca lời giảng mới, tối thiểu 100 câu chia đủ 7 loại: công thức trong bảng có trích dẫn, công thức đúng ngoài bảng, công thức sai, LaTeX hỏng, kết quả tính cụ thể của bài, toán viết trần ngoài dấu phân cách, trích nguyên văn đề bài; công thức trong bảng có trích dẫn và trích đề bài hiện đúng ít nhất 95 %.
- **SC-005**: Bộ AI 70 ca của v0 đạt 70/70 khi chạy qua luồng mới.
- **SC-006**: Kết quả chấm từng bước trên toàn bộ ngân hàng của chủ đề trùng 100 % với v0.
- **SC-007**: Trạng thái đầu tiên của lượt gia sư hiện trong 1 giây; ở chế độ offline, câu trả lời hiện trong 3 giây (phân vị 95, máy dev).
- **SC-008**: Mọi màn học sinh và giáo viên của v0 cho chủ đề này có màn tương đương trên v2, trừ «Tạo đề» bằng AI (P3); đối chiếu theo bảng phụ lục.
- **SC-009**: Thang gợi ý mẫu qua bộ lọc: 0 câu bị chặn nhầm, 32/32 câu lộ cài sẵn vẫn bị bắt (bằng số của v0).
- **SC-010**: Kịch bản «tới VDC»: một học sinh đi hết 4 mức của một kỹ năng qua ít nhất 4 bài được đề xuất liên tiếp, mỗi lần nâng đúng một nấc, rồi được báo hoàn thành kỹ năng; không bước nào nhảy quá một nấc.

## Assumptions

- Chủ đề: Toán 12 «đơn điệu và cực trị», đúng dữ liệu đã kiểm định của v0 (`data/supham/`, `services/math/kiemdinh/`); dữ liệu sư phạm và kiểm định chỉ sửa bằng bản vá nguyên văn có mã.
- Dữ liệu tổng hợp, một lớp «12A1 thử» với An, Bình, Chi và giáo viên thử (ADR 006).
- Học sinh thấy 4 mức bằng lời, không thấy chữ «Bloom» (Q3 mặc định). Nhập công thức bằng gõ, chưa có ảnh bài viết tay (Q6 mặc định). Điện thoại là thiết bị chính (Q5).
- Nhà AI mặc định là offline. Nhà thật chỉ chạy với tài khoản tổng hợp cho tới khi ADR 012 được duyệt.
- Dịch vụ toán giữ hợp đồng `/v1` hiện có; khả năng mới (kiểm công thức trong lời giảng) được thêm, không đổi hợp đồng cũ.
- Đăng nhập, vai trò, phiên, giới hạn đăng nhập sai đã có từ P1 (#55, #57, #69).
- Ngoài phạm vi: OCR tài liệu ảnh, AI soạn bài, 4 dạng câu và khuôn đề thi, kênh nhắc ngoài, nhiều chủ đề, hiệu chỉnh độ khó bài, staging (#75).

## Phụ lục — màn tương đương với v0

| v0 | Tiêu đề / testid giữ lại | v2 trong P2 |
| --- | --- | --- |
| `/hs` | «Chào <tên>», `nav-hs-lo-trinh`, `mo-sidebar`, `tab-bai-giao` | có |
| `/hs/bai` | «Đề bài», `nav-hs-bai` | có |
| `/hs/luyen/[id]` | `solve-screen`, `nop-buoc`, `cham-thong-bao`, `latex-txd`, `latex-dh`, `mo-gia-su`, `tutor-*`, `chip-goi-y` | có |
| `/hs/lich` | «Lịch học», `lich-tuan`, `nav-hs-lich` | có |
| `/hs/kho` | «Công thức», `nav-hs-kho` | có |
| `/gv` | «Lớp 12A1 thử», `canh-bao-ket`, `san-sang-ai` | có |
| `/gv/duyet` | «Duyệt», `hang-doi`, `duyet-<mã bài>` | có |
| `/gv/ngan-hang` | «Đề bài», `nav-gv-ngan-hang` | có |
| `/gv/tai-lieu` | «Tài liệu» | có |
| `/gv/cong-thuc` | «Công thức» | có |
| `/gv/tien-do` | «Mức», `tien-do`, `toggle-muc` | có |
| `/gv/hoc-sinh/[id]` | trang từng học sinh | có |
| `/gv/cai-dat` | «Cài lớp», `mo-loi-giai`, `ai-provider-offline` | có, bỏ phần dán khóa |
| `/gv/ket-noi-ai` | «Gia sư» | thay bằng trạng thái nhà do máy chủ quản lý |
| `/gv/sinh-bai` | «Tạo đề» | P3 |
