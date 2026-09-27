# ADR 003 — Gia sư không đọc lời giải chuẩn

Lời giải và `protected_facts` nằm ở bảng `solutions`. Hàm dựng prompt chỉ nhận: đề, bước sai, loại kết quả, mã lỗi kèm độ tin cậy nếu đạt ngưỡng, và gợi ý đã kiểm của đúng bước đó (tối đa 3 mức).

Mọi câu trả lời đi qua `POST /v1/filter`. Lớp chính là tương đương SymPy **theo ngữ cảnh** (M3: số phải dính đồng biến / cực / nghiệm). Lớp phụ là khớp chuỗi LaTeX (M1). Không dùng M2 (chỉ so giá trị) vì chặn nhầm quy trình («giảm số mũ đi 1» khi đề có nghiệm 1). Nếu bị chặn, câu được thay bằng gợi ý đã có hoặc một câu từ chối không chứa kết quả, rồi lọc lần hai.

Xin đáp án bị chặn bằng luật, không phụ thuộc mô hình: lần 1 và 2 nhắc lại gợi ý, lần 3 từ chối và gợi ý nghỉ hoặc báo thầy cô. Mở lời giải sau khi nộp là cờ lớp, mặc định tắt.
