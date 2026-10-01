# Gia sư AI môn toán — bằng chứng tới 2026-10-01

Câu hỏi: gia sư LLM giúp hay hại việc học toán, và điều kiện nào làm nó giúp? Phục vụ C7, C9 và bất biến «không đưa đáp án».

## Bằng chứng chính

| Nghiên cứu | Thiết kế | Kết quả | Độ tin |
| --- | --- | --- | --- |
| Bastani et al., *PNAS* 2025 — *Generative AI without guardrails can harm learning* | Thử nghiệm thực địa, gần 1.000 HS THPT môn toán; GPT-4 «Base» vs «Tutor» có rào chắn | Khi có AI: điểm luyện tập +48 % (Base), +127 % (Tutor). Khi bỏ AI: nhóm Base **−17 %** so với nhóm không dùng, và tự đánh giá cao hơn thực lực; nhóm Tutor gần như không bị hại | Cao (RCT, tạp chí đầu ngành) |
| Kestin et al., *Scientific Reports* 06/2025 (Harvard) | RCT, vật lý đại học: gia sư AI thiết kế theo sư phạm vs lớp học tích cực | Trung vị mức tiến bộ của nhóm AI gấp hơn 2 lần | Cao cho bối cảnh đại học; khái quát cho THPT cần thận trọng |
| Google — báo cáo kỹ thuật Guided Learning / LearnLM, 05/2026 | HS dùng ≥ 12 giờ trong 8 tuần | Môn toán: từ phân vị 50 lên 64 | Trung bình (báo cáo của nhà cung cấp) |
| RCT khám phá ở lớp học Anh, arXiv 2512.23633 | Gia sư AI trong lớp | An toàn và hiệu quả ở quy mô thử | Trung bình (preprint) |
| Daheim et al., arXiv 2407.09136 — *Stepwise verification and remediation* | So sánh «kiểm trước rồi sinh» với sinh trực tiếp | Tách phát hiện lỗi khỏi sinh phản hồi cho phản hồi đúng trọng tâm hơn | Trung bình–cao |
| Lightman et al. 2023 (*Let's Verify Step by Step*) và các khảo sát PRM 2025–2026 | Mô hình thưởng theo bước | PRM khó bắt lỗi tinh trong bước; gần với đoán ngẫu nhiên ở một số bộ | Trung bình |
| EACL 2026 — *Feedback generation on problem-solving processes* | So phản hồi LLM với giáo viên | Phản hồi LLM dài dòng, ít nhắm vào hiểu lầm gốc | Trung bình |

Thị trường 2025–2026: ChatGPT Study Mode (07/2025), Claude Learning Mode, Gemini Guided Learning đều chuyển sang hỏi gợi mở thay vì đưa đáp án. Cạnh tranh giữa các phòng lab đã chuyển từ năng lực sang sư phạm.

## Hệ quả thiết kế

1. **Rào chắn là điều kiện cần, không phải tính năng.** Giữ luật xin đáp án, thang gợi ý không bottom-out (Aleven; VanLehn) và bộ lọc lộ đáp án **xác định bằng CAS**, chạy trên mọi câu, fail-closed (v0 đã làm đúng).
2. **Kiểm trước rồi mới sinh.** Bộ chấm bước (CAS) xác định bước sai và mã lỗi. LLM chỉ diễn đạt câu hỏi gợi mở cho đúng lỗi đó. Không để LLM tự phán đúng/sai. Kiến trúc v0 trùng hướng này; v2 giữ nguyên.
3. **Liều lượng quyết định hiệu quả** (≥ 12 giờ / 8 tuần trong báo cáo LearnLM). C9 (lịch + nhắc) là đòn bẩy học tập, không chỉ là tiện ích.
4. **Tự tin ảo là rủi ro đo được** (Bastani). Cần chỉ số «dự đoán của HS vs kết quả thật» trong lab Đánh giá.
5. **Đo bằng eval, không bằng cảm nhận.** Mọi thay đổi prompt hoặc nhà cung cấp phải chạy bộ ca của lab Đánh giá (lộ đáp án, đúng toán, chất lượng gợi mở) trước khi tới học sinh.

## Việc mở

- Đọc bản đầy đủ Kestin 2025 và báo cáo LearnLM 05/2026; trích phần thiết kế prompt và đo lường.
- Tìm RCT gia sư AI ở Việt Nam hoặc Đông Nam Á (chưa thấy trong lượt tìm này).

## Nguồn (truy cập 2026-10-01)

- https://www.pnas.org/doi/10.1073/pnas.2518204122 · https://www.semanticscholar.org/paper/60570ad3268871882c65f6ed4ead35db4b220528
- https://impactaieducation.substack.com/p/a-harvard-randomized-controlled-trial
- https://www.techlearning.com/how-to/geminis-guided-learning-mode-from-google-ai-what-educators-need-to-know · https://cloud.google.com/solutions/learnlm
- https://arxiv.org/abs/2512.23633
- https://www.alphaxiv.org/abs/2407.09136
- https://cdn.openai.com/improving-mathematical-reasoning-with-process-supervision/Lets_Verify_Step_by_Step.pdf
- https://aclanthology.org/2026.eacl-long.132.pdf
- https://glasp.co/articles/ai-study-modes-compared
