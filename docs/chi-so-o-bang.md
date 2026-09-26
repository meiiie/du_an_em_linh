# Chỉ số ô bảng xét dấu

`buoc_sai = { ma_buoc, dong, o }` với `o = { hang, k }`.

`hang` thuộc `X`, `DAU_YPHAY`, `BIEN_THIEN`.

`k` của sản phẩm **bắt đầu từ 0**. Trên hàng dấu và hàng chiều biến thiên, `k` đếm xen kẽ khoảng rồi điểm (tích Descartes của các mốc, gồm hai đầu −∞ và +∞ ở khung bảng, không phải điểm tới hạn):

- `k` chẵn: khoảng giữa hai mốc kề nhau. `k = 0` là khoảng đầu (từ −∞ tới mốc đầu học sinh ghi).
- `k` lẻ: đúng tại mốc. `k = 1` là mốc đầu tiên học sinh tự thêm.

Hàng `X` chỉ ghi các điểm tới hạn học sinh nhập, `k = 0, 1, 2, …` theo thứ tự em thêm, không xen khoảng.

Thiếu điểm tới hạn được gán cho bước `B.DH.NGHIEM`, không tô một ô “còn thiếu” trong lúc gõ. Ứng dụng không tự thêm mốc và không chấm từng ô trước khi nộp cả bước.

Bộ kiểm định YAML đánh `k` từ 1 theo cùng kiểu xen kẽ. Khi đối chiếu, dịch vụ toán trừ 1 (`k_sản_phẩm = k_kiểm_định − 1`) với hàng dấu và chiều; với hàng x thì `k_sản_phẩm = k_kiểm_định / 2 − 1`. `dong` của bước một dòng logic trong bộ kiểm (1, 2, 3, 5) được đưa về dòng 0. Dòng học sinh tự thêm được giữ theo chỉ số 0-based của chính các dòng đó, nên một đường biến đổi khác vẫn chấm được từng cặp dòng liền nhau.
