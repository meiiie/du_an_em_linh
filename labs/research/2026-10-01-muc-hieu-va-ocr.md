# Mô hình mức hiểu, lặp cách quãng, OCR đề toán (2026-10-01)

Câu hỏi: dùng kỹ thuật nào cho C6 (mức hiểu), C8/C9 (chọn bài, lịch ôn), C1 (nạp tài liệu có công thức)?

## Mô hình mức hiểu (knowledge tracing)

| Họ mô hình | Ưu | Nhược | Dùng khi |
| --- | --- | --- | --- |
| BKT (Corbett & Anderson 1994; `pyBKT`) | Giải thích được, rẻ, chạy với ít dữ liệu | Coi kỹ năng độc lập, nhị phân | **Ngay bây giờ** — v0 đã có |
| IRT (1PL/2PL) | Hiệu chỉnh độ khó bài; chọn bài «nâng 1 nấc» có cơ sở | Cần dữ liệu trả lời đủ lớn | Khi mỗi bài có ≳ 100–200 lượt trả lời |
| DKT / SAKT / AKT, simpleKT | Chính xác hơn trên bộ chuẩn (ASSISTments) | Khó giải thích, cần nhiều dữ liệu | Khi có dữ liệu thật ở quy mô trường |
| LLM-KT (LLaMA tinh chỉnh, L-HAKT AAAI 2026) | Dùng ngữ nghĩa đề bài | Đắt; nghiên cứu 03/2026 cho thấy mô hình KT chuyên dụng **nhanh hơn, rẻ hơn, chính xác hơn** LLM | Không dùng để ước lượng mức |

Lặp cách quãng: FSRS là chuẩn mở hiện hành cho lịch ôn; LECTOR (2025) thêm LLM, chỉ hơn baseline tốt nhất khoảng 2 % tương đối. Hệ quả: dùng FSRS (hoặc tương đương) cho lịch ôn trong C9, LLM chỉ để diễn đạt lời khuyên.

Nguyên lý học tập có bằng chứng mạnh cho «tư vấn phương pháp» (C9): luyện truy hồi và học cách quãng (độ hữu ích cao), luyện xen kẽ cho toán (Dunlosky et al. 2013; Rohrer & Taylor 2007).

## OCR đề và sách có công thức (C1)

| Hệ | Ghi chú (OmniDocBench và benchmark 2026) |
| --- | --- |
| PaddleOCR-VL 1.6 (0,9B) | Tổng 96,34; công thức CDM 97,53 — dẫn đầu, tự host được |
| MinerU 2.5-Pro (1,2B) | Tổng 95,75; công thức CDM 97,45 |
| Mathpix | Thương mại; benchmark trích công thức PDF xếp nhóm đầu |
| olmOCR 2, DeepSeek-OCR, LightOnOCR (1B) | Mã mở, tốt cho trang chữ; cần đo với tiếng Việt có dấu |

Hệ quả: lab Đánh giá dựng bộ 30–50 trang SGK / đề mẫu tiếng Việt (có công thức, bảng biến thiên, hình) để đo trước khi chọn. Kết quả OCR **không** vào kho trực tiếp: giáo viên xác nhận, rồi qua cổng 3 tầng như mọi nội dung khác.

## Nguồn (truy cập 2026-10-01)

- https://arxiv.org/pdf/2603.02830 (*Faster, Cheaper, More Accurate: Specialised KT Models Outperform LLMs*)
- https://stanford.edu/~cpiech/bio/papers/deepKnowledgeTracing.pdf · https://en.papernotes.org/AAAI2026/self_supervised/towards_llm-empowered_knowledge_tracing_via_llm-student_hierarchical_behavior_al/
- https://arxiv.org/pdf/2508.03275 (LECTOR)
- https://arxiv.org/pdf/2512.09874 (benchmark trích công thức từ PDF) · https://github.com/opendatalab/OmniDocBench
- https://arxiv.org/pdf/2606.03264 (PaddleOCR-VL 1.6) · https://arxiv.org/pdf/2604.04771 (MinerU 2.5-Pro) · https://arxiv.org/pdf/2510.19817 (olmOCR 2) · https://arxiv.org/pdf/2601.14251 (LightOnOCR)
- Dunlosky J. et al. (2013). *Improving Students' Learning With Effective Learning Techniques.* Psychological Science in the Public Interest, 14(1).
