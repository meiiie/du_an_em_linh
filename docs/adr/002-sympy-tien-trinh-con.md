# ADR 002 — SymPy trong tiến trình con

Chuỗi học sinh và lời giải máy đều đi qua `app/sandbox.py`: sinh tiến trình `job_runner.py`, giới hạn thời gian, giết tiến trình nếu quá hạn. `job_runner` là chỗ duy nhất import SymPy.

Chuẩn hóa ký hiệu Việt (`(1;3)`, `0,5`, `\mathbb{R}`, `\cup`) nằm trong `normalizer.py` và chạy trong tiến trình đó. Bộ kiểm Tầng 1 và 16 ca 5 bước của Kiểm định được gọi lại, không viết lại luật chấm.
