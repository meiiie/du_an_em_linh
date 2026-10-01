---
paths:
  - "apps/web/lib/tutor*.ts"
  - "apps/web/lib/gia-su-*.ts"
  - "apps/web/lib/ai-*.ts"
  - "apps/web/lib/llm*.ts"
  - "apps/web/lib/kien-thuc*.ts"
  - "apps/web/lib/kho-lop*.ts"
  - "apps/web/lib/citations*.ts"
  - "apps/web/lib/loi-gia-su*.ts"
  - "apps/web/app/api/hs/gia-su/**"
  - "apps/web/components/tutor-panel.tsx"
  - "apps/web/components/loi-gia-su.tsx"
  - "services/math/app/leakfilter.py"
  - "services/math/app/thang_mau.py"
  - "services/math/app/data/thang-goi-y-mau/**"
  - "services/core/**/tutor/**"
---

# Gia sư AI — vùng an toàn (hiến chương I, II, III)

- Prompt **không** chứa lời giải chuẩn, `protected_facts`, hay đáp án nội bộ. Chỉ: đề, bước sai, loại kết quả, mã lỗi (nếu đủ tin cậy), gợi ý đã kiểm của đúng bước (ADR 003).
- Thứ tự cố định: luật xin đáp án → thang gợi ý 3 cấp (không bottom-out) → kho lớp có trích dẫn → nhà đã chọn tường minh → bộ lọc CAS trên cả câu.
- Không fallback thầm sang nhà khác hay sang câu mẫu; lỗi nhà thì báo lỗi rõ (ADR 007). Không stream token tới học sinh khi bộ lọc cần cả câu (ADR 010).
- Xóa định danh trước khi gửi ra ngoài; khóa không vào log, prompt, trình duyệt.
- Đổi bất kỳ điều gì ở vùng này: dùng skill `tutor-safety`, chạy eval của lab Đánh giá, nhờ subagent `pedagogy-reviewer` và `privacy-reviewer` rà trước khi mở PR.
