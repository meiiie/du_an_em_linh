# Quyền riêng tư cho dữ liệu học sinh (trước pilot)

- **Ngày:** 2026-10-02 · **Trạng thái:** đề xuất → [ADR 012](../../docs/adr/012-quyen-rieng-tu-du-lieu-hoc-sinh.md) · **Issue:** [#60](https://github.com/meiiie/du_an_em_linh/issues/60)
- **Người lập:** Claude Code (phiên 2026-10-01/02). Phần pháp lý là đọc hiểu kỹ thuật, **không phải ý kiến luật sư** (§11).

## 1. Câu hỏi quyết định

Trước khi có học sinh thật, sản phẩm xử lý dữ liệu cá nhân của học sinh (đa số chưa thành niên) theo mô hình nào để hợp pháp theo luật Việt Nam năm 2026 mà vẫn giữ được gia sư AI và cá nhân hóa?

## 2. Dữ kiện

### Pháp lý (nguồn ở §12)

| # | Quy định | Nội dung liên quan |
| --- | --- | --- |
| P1 | Luật BVDLCN 91/2025/QH15, hiệu lực 01/01/2026, Điều 24 k1 | Với trẻ em, người đại diện theo pháp luật thay mặt thực hiện quyền của chủ thể dữ liệu (trừ trường hợp không cần đồng ý theo k1 Điều 19). |
| P2 | Như trên, Điều 24 k2–3 | Xử lý dữ liệu của trẻ từ đủ 7 tuổi nhằm công bố, tiết lộ thông tin đời sống riêng tư cần **cả** trẻ và người đại diện đồng ý. Dừng xử lý khi người đã đồng ý rút lại. |
| P3 | Luật Trẻ em 2016, Điều 1 | Trẻ em là người dưới 16 tuổi. Học sinh lớp 10 có em 15 tuổi (trẻ em); lớp 11–12 là người chưa thành niên nhưng không còn là trẻ em. |
| P4 | Luật 91/2025, Điều 20 k2; Điều 21 k1 | Chuyển dữ liệu xuyên biên giới phải có hồ sơ đánh giá tác động (CTIA). Đánh giá tác động xử lý dữ liệu (DPIA) phải lập. Cả hai gửi bản chính cho cơ quan chuyên trách (A05, Bộ Công an) trong 60 ngày. |
| P5 | Luật 91/2025, Điều 38 k2 | Doanh nghiệp nhỏ / khởi nghiệp được chọn không làm Điều 21, 22, k2 Điều 33 trong 5 năm, **trừ** khi kinh doanh dịch vụ xử lý dữ liệu hoặc **trực tiếp xử lý dữ liệu nhạy cảm**. |
| P6 | NĐ 356/2025/NĐ-CP (31/12/2025, hiệu lực 01/01/2026, thay NĐ 13/2023) | Dữ liệu nhạy cảm **mở rộng** sang «dữ liệu theo dõi hành vi, hoạt động sử dụng … các dịch vụ khác trên không gian mạng». Miễn trừ còn loại thêm bên xử lý tích lũy từ 100 nghìn chủ thể. |
| P7 | NĐ 356 | Đồng ý phải rõ ràng, kiểm chứng được (lưu thời điểm và nội dung). Cấm mặc định đồng ý. Đồng ý trong ứng dụng có thiết lập kỹ thuật là hợp lệ. |
| P8 | NĐ 356 | Thời hạn với yêu cầu của chủ thể: rút đồng ý / hạn chế phản hồi 2 ngày làm việc, thực hiện 15 ngày; xem / sửa 10 ngày; **xóa 20 ngày** (30 nếu cần phối hợp bên xử lý). |
| P9 | NĐ 356 | DPIA / CTIA theo Mẫu 09, 10: tiền kiểm, A05 trả lời trong 15 ngày; có sơ đồ luồng dữ liệu; cập nhật 6 tháng một lần hoặc trong 10 ngày khi tổ chức thay đổi. |
| P10 | Luật Trí tuệ nhân tạo 134/2025/QH15, hiệu lực 01/03/2026 | Thông báo cho người dùng biết đang tương tác với AI; gắn nhãn nội dung AI. Hệ thống rủi ro cao: con người giám sát, nhật ký, đánh giá sự phù hợp, báo phân loại cho Bộ KH&CN. Hệ thống giáo dục đang chạy có 18 tháng chuyển tiếp. Danh mục rủi ro cao chưa ban hành. |

### Sản phẩm (mã và ADR hiện có)

- v0 chỉ dùng dữ liệu tổng hợp ([ADR 006](../../docs/adr/006-du-lieu-tong-hop.md)), đã có móc `consent_records`, `audit_logs`; RLS bật nhưng chưa `FORCE`.
- Gia sư gửi tới nhà LLM ([ADR 007](../../docs/adr/007-ai-providers.md), [009](../../docs/adr/009-khoa-lap-trinh-openrouter-zai.md)):
  - nhà đang có: OpenAI (Hoa Kỳ), OpenRouter (Hoa Kỳ, chuyển tiếp tới nhiều nhà), Z.AI (Trung Quốc);
  - nội dung gửi: đề, bước học sinh viết, mã lỗi, gợi ý đã kiểm. Không gửi lời giải ([ADR 003](../../docs/adr/003-gia-su-khong-doc-loi-giai.md)).
  - Ô chữ tự do của học sinh có thể chứa thông tin cá nhân.
- v2 sẽ giữ trong `services/core`:
  - định danh, lớp, bài làm, mức hiểu (BKT), lịch học, nhắc lịch;
  - ảnh bài viết tay để OCR (C1, C4 trong `docs/product/MUC-TIEU.md`).
  - Mức hiểu và lịch học là **theo dõi hành vi học tập trên một dịch vụ trực tuyến**, nên rất có thể là dữ liệu nhạy cảm theo P6.

## 3. Tiêu chí loại

- **L0** Cho phép pilot với học sinh thật trong năm học 2026–2027.
- **L1** Không xử lý dữ liệu học sinh thật khi chưa có cơ sở pháp lý được ghi nhận và kiểm chứng (P7).
- **L2** Không gửi định danh trực tiếp tới nhà LLM: tên, email, số điện thoại, mã học sinh, ảnh khuôn mặt.
- **L3** Rút đồng ý và xóa làm được trong thời hạn P8.
- **L4** Giáo viên xem và ghi đè được mọi phân loại mức, gợi ý bài do máy đưa ra (P10, hiến chương II).

## 4. Tiêu chí chấm (đặt trước khi chấm)

| Tiêu chí | Trọng số |
| --- | --- |
| Rủi ro pháp lý còn lại (thấp = điểm cao) | 30 |
| Giá trị học tập giữ được (gia sư tốt, cá nhân hóa) | 25 |
| Chi phí xây và vận hành (hồ sơ, hạ tầng, nhân sự) | 15 |
| Ma sát khi vào học (học sinh, phụ huynh, nhà trường) | 15 |
| Linh hoạt, đảo ngược được (mở B2C sau, đổi nhà LLM) | 15 |

## 5. Phương án

- **A — Giữ nguyên:** chỉ dữ liệu tổng hợp, chưa pilot.
- **B — Pilot qua trường, LLM nước ngoài sau khi khử định danh:**
  - nhà trường là bên kiểm soát dữ liệu học sinh trong dạy học, sản phẩm là bên xử lý theo hợp đồng có phụ lục xử lý dữ liệu;
  - trường thu đồng ý của phụ huynh (và của học sinh), hệ thống ghi nhận lại;
  - nhà LLM chỉ nhận nội dung toán đã khử định danh;
  - lập DPIA + CTIA.
- **C — Trực tiếp phụ huynh (B2C), LLM nước ngoài sau khi khử định danh:** sản phẩm là bên kiểm soát; xác minh phụ huynh và tuổi trong ứng dụng; DPIA + CTIA + nhân sự bảo vệ dữ liệu ngay từ đầu.
- **D — Pilot qua trường, chỉ LLM trong nước hoặc tự host:** như B nhưng không chuyển dữ liệu xuyên biên giới, nên không cần CTIA. Chất lượng gia sư phụ thuộc mô hình trong nước / mã nguồn mở chạy tại Việt Nam.

A trượt **L0**. B, C, D qua L0–L4 nếu làm đủ quyết định con ở §8.

## 6. Chấm điểm (1–5)

| Tiêu chí (trọng số) | A | B | C | D |
| --- | --- | --- | --- | --- |
| Rủi ro pháp lý (30) | 5 | 4 | 3 | 5 |
| Giá trị học tập (25) | 1 | 5 | 5 | 3 |
| Chi phí (15) | 5 | 3 | 2 | 2 |
| Ma sát vào học (15) | 5 | 4 | 2 | 4 |
| Linh hoạt (15) | 3 | 4 | 4 | 3 |
| **Tổng / 500** | 370 (loại L0) | **410** | 335 | 360 |

Lý do các điểm then chốt:

- **B – pháp lý 4, không phải 5:** vẫn chuyển dữ liệu ra nước ngoài (CTIA, tiền kiểm A05), và còn rủi ro khử định danh sót trong ô chữ tự do.
- **C – pháp lý 3:** sản phẩm tự làm bên kiểm soát dữ liệu của trẻ vị thành niên, phải tự xác minh tuổi và người đại diện.
- **C – ma sát 2:** mỗi phụ huynh phải tự xác minh trước khi con vào học được.
- **D – giá trị 3:** chưa có số đo trên bộ AI 70 ca (`labs/evals`) cho thấy mô hình trong nước đạt mức các nhà hiện tại; điểm này là giả định, cần đo.
- **D – chi phí 2:** GPU tự host hoặc hợp đồng nhà trong nước.

## 7. Độ nhạy

- Pháp lý lên 45, giá trị học tập xuống 10: B = 395, D = 390. D gần bằng B khi rủi ro pháp lý chi phối.
- D thắng nếu mô hình trong nước đạt bộ AI 70 ca (giá trị lên 5 → D = 410, ngang B) **và** không cần CTIA. Kiến trúc nhà tường minh (ADR 007) giữ đường đổi sang D mà không viết lại.
- C chỉ vượt B khi khách chọn B2C (câu hỏi Q4 ở [#61](https://github.com/meiiie/du_an_em_linh/issues/61)); khi đó cần ADR riêng.

## 8. Khuyến nghị: B, kèm quyết định con

1. **Vai trò:** hợp đồng với trường nêu trường là bên kiểm soát, sản phẩm là bên xử lý, kèm phụ lục xử lý dữ liệu: mục đích, loại dữ liệu, thời hạn, xóa khi kết thúc, bên xử lý phụ (nhà LLM, hạ tầng).
2. **Đồng ý tách theo mục đích, không mặc định:**
   - mục đích: `HOC_TAP` (bắt buộc để học), `GIA_SU_AI` (gửi nội dung đã khử định danh ra nước ngoài), `NHAC_LICH_NGOAI` (Zalo, email…);
   - với **mọi học sinh dưới 18 tuổi**, ghi nhận đồng ý của **cả phụ huynh và học sinh**: rộng hơn mức tối thiểu của P1–P3, chờ luật sư thu hẹp;
   - mỗi bản ghi lưu: người đồng ý, vai trò, mục đích, phiên bản văn bản, thời điểm, kênh, bằng chứng (P7).
3. **Không có `GIA_SU_AI` thì gia sư chạy nhà `offline`** (ADR 007): thang gợi ý đã kiểm, không gọi LLM. Học sinh vẫn học được (L1).
4. **Tối thiểu hóa:**
   - định danh chỉ nằm ở `services/core`; `services/math` và nhà LLM chỉ nhận nội dung toán, mã lỗi, mã giả danh theo phiên;
   - bộ khử định danh chạy ở `services/core` **trước** khi gọi LLM, lọc email, số điện thoại, tên trong danh sách lớp, mã học sinh; câu bị lọc ghi vào nhật ký (không ghi nội dung);
   - ảnh bài làm không gửi nhà nước ngoài: OCR trong nước / tự host, hoặc cắt chỉ vùng toán.
5. **Nhà LLM cho học sinh thật:**
   - chỉ nhà có điều khoản không dùng dữ liệu API để huấn luyện và có thỏa thuận xử lý dữ liệu;
   - mỗi nhà một CTIA trước khi bật;
   - Z.AI (Trung Quốc) và OpenRouter (chuyển tiếp nhiều nhà) cần đánh giá riêng; chưa đánh giá thì chỉ dùng cho dữ liệu tổng hợp.
6. **Quyền chủ thể trong ứng dụng:** xem, sửa, tải về, xóa, rút đồng ý. Có hàng đợi yêu cầu kèm hạn theo P8. Xóa thật cả bản sao lưu theo vòng quay đã công bố.
7. **Thời hạn lưu:** mặc định hết năm học cộng một thời hạn do hợp đồng trường quy định (luật sư chốt). Việc xóa theo lịch chạy tự động và có nhật ký.
8. **Minh bạch AI và giám sát (P10):**
   - mọi câu của gia sư gắn nhãn «Gia sư AI»;
   - trang giới thiệu nói rõ phần nào do AI;
   - giáo viên xem và ghi đè mức hiểu, gợi ý bài; mỗi gợi ý có lý do xem được;
   - nhật ký tương tác AI lưu theo thời hạn ở mục 7.
9. **Bảo mật:**
   - `FORCE` RLS trên bảng dữ liệu học sinh; kiểm quyền theo lớp ở use case (chống IDOR);
   - mã hóa khi lưu và khi truyền; nhật ký kiểm toán mọi lần đọc dữ liệu học sinh;
   - kế hoạch ứng phó sự cố: báo A05, báo chủ thể khi luật yêu cầu.
10. **Hồ sơ trước pilot:**
    - DPIA + CTIA (Mẫu 09, 10 NĐ 356), có sơ đồ luồng dữ liệu;
    - chỉ định nhân sự / dịch vụ bảo vệ dữ liệu: miễn trừ P5 khó áp dụng vì P6;
    - phân loại hệ thống AI theo Luật AI khi danh mục rủi ro cao được ban hành.

## 9. Rủi ro và giảm thiểu

| Rủi ro | Giảm thiểu |
| --- | --- |
| Khử định danh sót thông tin trong ô chữ tự do | Danh sách chặn theo lớp + mẫu email / số điện thoại; bộ thử có dữ liệu giả; tỉ lệ lọt đo ở `labs/evals` |
| Nhà trường ngại thủ tục đồng ý | Mẫu đồng ý ngắn, tách mục đích; học không cần `GIA_SU_AI` |
| Tiền kiểm A05 kéo dài (15 ngày + 30 ngày bổ sung) | Nộp hồ sơ sớm, trước ngày pilot ít nhất 2 tháng |
| Nhà LLM đổi điều khoản | Một nhà một CTIA; kiến trúc nhà tường minh cho đổi nhanh; giữ `offline` |
| Danh mục AI rủi ro cao xếp «đánh giá, phân loại người học» vào rủi ro cao | Thiết kế sẵn: giáo viên giám sát, nhật ký, giải thích gợi ý; chuẩn bị hồ sơ đánh giá sự phù hợp |

## 10. Điều làm quyết định này sai

- Luật sư xác nhận dữ liệu học tập **không** thuộc «theo dõi hành vi» nhạy cảm (P6): hồ sơ nhẹ đi, miễn trừ P5 có thể áp dụng.
- Khách chọn B2C ở Q4 ([#61](https://github.com/meiiie/du_an_em_linh/issues/61)): cần ADR cho phương án C.
- Mô hình trong nước / tự host đạt bộ AI 70 ca: chuyển sang D, bỏ CTIA.
- Văn bản hướng dẫn Luật AI xếp gia sư AI cho học sinh vào rủi ro cao: thêm đánh giá sự phù hợp trước khi chạy.

## 11. Câu hỏi cho luật sư

1. Học sinh 16–17 tuổi tự đồng ý được không, hay vẫn cần phụ huynh? Với trẻ dưới 16 tuổi, ngoài phụ huynh có cần đồng ý của chính trẻ không, khi việc xử lý không nhằm công bố đời sống riêng tư (P2)?
2. Mức hiểu, lịch học, nhật ký làm bài có phải «dữ liệu theo dõi hành vi … trên không gian mạng» (P6) không?
3. Trường là bên kiểm soát, sản phẩm là bên xử lý: phân vai này có đứng được không, và ai nộp DPIA / CTIA?
4. Gửi nội dung toán đã khử định danh (không kèm mã nối về người) tới API nước ngoài có còn là chuyển dữ liệu cá nhân xuyên biên giới không?
5. Thời hạn lưu dữ liệu học tập sau khi học sinh rời trường?
6. Gia sư AI cho học sinh có thuộc hệ thống rủi ro trung bình / cao theo Luật AI không, và có phải báo phân loại cho Bộ KH&CN không?

## 12. Nguồn (truy cập 2026-10-01/02)

- Luật 91/2025/QH15 (trích điều): https://luattri.com/library/luat/91-2025-qh15-bao-ve-du-lieu-ca-nhan · Điều 24: https://xaydungchinhsach.chinhphu.vn/quy-dinh-bao-ve-du-lieu-ca-nhan-cua-tre-em-nguoi-bi-mat-hoac-han-che-nang-luc-hanh-vi-dan-su-119250725165056743.htm · https://plo.vn/cong-bo-du-lieu-ca-nhan-cua-tre-tu-7-tuoi-phai-duoc-tre-va-nguoi-dai-dien-dong-y-post888944.html
- NĐ 356/2025/NĐ-CP — EY Law Việt Nam, *Tin nhanh Pháp lý* tháng 3/2026 (độ tin cao, hãng luật): https://www.ey.com/content/dam/ey-unified-site/ey-com/vi-vn/technical/tax/documents/ey-vietnam-legal-alert-march-2026-decree-no356-2025-nd-cp-providing-detailed-guidance-for-implementation-of-personal-data-protection-law-viet.pdf · LuatVietnam: https://luatvietnam.vn/doanh-nghiep/doanh-nghiep-can-biet-gi-ve-bao-ve-du-lieu-ca-nhan-tai-nghi-dinh-356-2025-nd-cp-561-109567-article.html
- Luật Trẻ em 2016, Điều 1: https://vi.wikisource.org/wiki/Lu%E1%BA%ADt_tr%E1%BA%BB_em_n%C6%B0%E1%BB%9Bc_C%E1%BB%99ng_h%C3%B2a_x%C3%A3_h%E1%BB%99i_ch%E1%BB%A7_ngh%C4%A9a_Vi%E1%BB%87t_Nam_2016/Ch%C6%B0%C6%A1ng_I
- Luật 134/2025/QH15 (tóm tắt của hãng luật, độ tin trung bình — đọc toàn văn trước khi dựa vào): https://luatvietan.vn/luat-tri-tue-nhan-tao.html · https://thuvienphapluat.vn/van-ban/Cong-nghe-thong-tin/Luat-Tri-tue-nhan-tao-2025-so-134-2025-QH15-679013.aspx
- Ghi chú trước: [`labs/research/2026-10-01-boi-canh-viet-nam.md`](../research/2026-10-01-boi-canh-viet-nam.md)
