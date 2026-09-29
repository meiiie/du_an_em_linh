# Nhật ký Kiểm định – giai đoạn 1

Giờ ghi theo giờ Việt Nam (UTC+7).

- 26/09 23:50 – Tạo venv `kiemdinh/.venv` (Python 3.13.5), cài sympy 1.14.0 (mpmath 1.3.0), PyYAML (để đọc bộ đề).
- 26/09 23:50–23:51 – Nhận bổ sung từ điều phối: (a) chủ đề mẫu của Sư phạm "Ứng dụng đạo hàm: đơn điệu, cực trị" (Toán 12), thêm ~10 ca lỗi + vài ca đúng, thêm trường `ma_loi_supham`; (b) định dạng JSON: `buoc_sai`, `loai_kiem`, `ma_loi`, `do_tin_cay_ma_loi`, trường truy vết, kiểm theo từng cặp dòng, trạng thái nội dung DA_PHAT_HANH / CHO_GIAO_VIEN_DUYET / BI_CHAN; tên file nộp chính `giai-doan-1-kiem-dinh.md`.
- 26/09 23:55 – Soạn xong bộ đề v1.0 `bo-de-kiem-thu/cac-ca.yaml` (102 ca: 69 sai, 33 đúng), TRƯỚC khi viết bộ kiểm. Sửa một câu chữ ở L37 (trường `khang_dinh_ai` còn sót ghi chú nháp "x - 1 ≥ -6??"), không đổi phần `kiem` hay nhãn. Băm SHA-256 sau khi sửa: `814a329c0f34a00909ccfaf3d39a11ba50c3692f14036ee9d95b6eb79b26cc56`. Từ đây bộ đề được coi là đóng băng; mọi thay đổi sau sẽ ghi dưới đây.
- Lúc soạn, thư mục `supham/` chỉ có `_nguon/`, chưa có `giai-doan-1-su-pham.md` → `ma_loi_supham` để null cho tất cả các ca.
- 27/09 ~00:00 – Lần chạy 1 bộ kiểm Tầng 1 trên bộ đề v1.0 (sha256 814a329c…): 69 ca lỗi → 67 SAI, 0 ĐẠT, 2 KHÔNG KIỂM ĐƯỢC; 33 ca đúng → 31 ĐẠT, **1 báo nhầm (DC7)**, 1 KHÔNG KIỂM ĐƯỢC. Lưu nguyên văn: `ket-qua/ket-qua-tang1-lan1-truoc-sua-loi.json`, `ket-qua/tom-tat-tang1-lan1-truoc-sua-loi.md`.
- Nguyên nhân báo nhầm DC7: LỖI CỦA BỘ KIỂM (không phải của đề): y = -x^4 + 2x^2 có hai điểm cực đại x = ±1 cùng giá trị 1; hàm `_cung_tap` so độ dài danh sách [1, 1] với {1} nên báo khác. Sửa: so như tập hợp (bỏ trùng). Đồng thời thêm `diem_gay()` để bỏ qua điểm gãy của |.| khi so đạo hàm (SymPy cho sign(0) = 0 nên coi f' "xác định" tại điểm gãy) – sửa này chưa ảnh hưởng ca nào của bộ v1.0 nhưng cần cho bộ 5 bước. KHÔNG sửa bộ đề. Lần chạy 2 (sau sửa): 0 báo nhầm. Báo cáo ghi cả hai lần.
- 27/09 00:01 – Thêm bộ 5 bước v1.1 (`cac-ca-5-buoc.yaml`, 12 ca, sha256 92c034aa…) theo yêu cầu ma_buoc/ô bảng; chạy: 9/9 ca lỗi bắt được, đúng ma_buoc + dòng/ô 9/9, 0 báo nhầm. Lưu `ket-qua/ket-qua-5-buoc-v1.1.json`.
- 27/09 ~00:05 – Giao ước nhóm mới cho bảng (k xen kẽ khoảng/điểm; hàng "x" | "dau_y'" | "bien_thien"; luật (a) hàng x → B.DH.NGHIEM, (b) dau_doi_trong_khoang, (c) ô tại điểm 0/||). Nâng bộ 5 bước lên v1.2 (sha256 991063a2…): chuyển nhãn S02 (ô 3→5), S08 (ô 1→1, đổi tên hàng), S11 (ô 2→3, hàng bien_thien), **S12 đổi từ B.DH.XETDAU ô 2 sang B.DH.NGHIEM hàng x** do luật (a) mới (bảng thiếu mốc x=2); đổi "khong_xd" thành "||" ở S10. Thêm S13–S16. Đây là đổi QUY ƯỚC, không phải chỉnh cho khớp kết quả; kết quả v1.1 vẫn giữ.
- Lần chạy 1 bộ v1.2: 12/12 lỗi bắt được, đúng ma_buoc + ô 12/12, nhưng **1 báo nhầm (S10)**: bộ kiểm đòi mốc x = 1 (nghiệm tử số của y' = (x-1)/sqrt(x^2-2x), nằm NGOÀI TXĐ). Lỗi của bộ kiểm; sửa: tập mốc đúng chỉ gồm điểm thuộc TXĐ hoặc biên TXĐ. Lưu nguyên văn `ket-qua/ket-qua-5-buoc-v1.2-lan1-truoc-sua-loi.json`. Lần chạy 2: 0 báo nhầm. Chạy lại bộ 102 ca sau sửa: kết quả không đổi (67/69, 0 báo nhầm).

## 29/09/2026 ~12:10 (giờ VN) — F-01
- Áp bản vá tham chiếu của Kiểm định `kiemdinh/ban-va/nguon/ap_f01.py` lên `kiemdinh/tang1/kiem_tang1.py` và `kiemdinh/loc-lo-dap-an/loc.py`
  (chỉ đổi chữ ký `kiem_5_buoc(bl, bo_qua_txd=False)` cho khớp bản v1.3 trên main 4e4af19; nội dung khối code chép nguyên văn).
- Sao bộ ca khóa `bo-de-kiem-thu/ca-dau-vao-doc-hai.yaml` (1.0-f01, SHA-256 ce50b1737d0287c20f43206deb32cf46d423c21c37aedae09ca2e2eb79d47c76), không sửa;
  port thành `tests/test_dau_vao_doc_hai_kd.py` (kiểm SHA + đủ 288 ca).
- 29/09 ~12:25: áp bản vá gộp của Kiểm định `kiemdinh/ban-va/0002-f01-loc-su-kien-bao-ve-4e4af19.patch` lên main 507874c (git apply --3way).
  Xung đột: `kiem_tang1.py` giữ bản trên main (đã có bộ phân tích an toàn tham chiếu từ ap_f01; bỏ các khối kiem_an_toan/cổng HOC_TOAN_CHO_KIEM_DEM cũ của 0002);
  `grader.py` giữ cổng danh sách trắng và thêm lớp `_chuoi_doc_hai` của 0002 chạy sau cổng. machine.py, leakfilter.py, loc.py và 3 file test áp nguyên văn.
