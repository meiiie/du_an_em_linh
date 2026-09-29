# -*- coding: utf-8 -*-
"""Bản vá 0003 (Kiểm định, 29/09) — bộ lọc lộ đáp án (app/leakfilter.py).

(a) Gợi ý nêu điểm thử cụ thể mà giữa hai điểm liên tiếp luôn có mốc -> suy ra được mốc -> chặn
    (27 cặp câu/sự kiện khác nhau của 36 ca "Thử lần lượt x = …" trong ai/doi-chieu-thang-mau/gen-4e4af19.json).
(b) Câu điều kiện có điều kiện đúng với bài tính như khẳng định (thang mẫu README §4 quy tắc 5):
    "Nếu tử là hằng số khác 0 thì y' = 0 vô nghiệm" chặn ở hàm bậc nhất/bậc nhất (8 cặp khác nhau của 12 ca),
    không chặn khi điều kiện sai với bài (bậc ba có hai điểm tới hạn).
(c) Câu điều kiện chung / điều kiện sai với bài không bị luật phủ định chặn nhầm ("không đạt cực trị tại điểm đó",
    "nếu không có điểm tới hạn nào…", "mốc nào không có ở đó … thì bỏ"); "hàm số không …" không đọc thành "hàm số 0".
    Khẳng định lộ thật vẫn chặn.
Thêm: lộ bằng lời đối chiếu sự kiện (số nghiệm, nghiệm kép, y' luôn dương, luôn đồng biến, TXĐ = ℝ, loại điểm, thiếu điểm).
"""
import pytest

from app.leakfilter import loc_ban_nhap

HS = "em chưa hiểu bước này"

# --- sự kiện mẫu (định dạng su_kien_bao_ve của app)
BAC_BA_2CT = [("DB", "(-oo;-3)"), ("DB", "(1;oo)"), ("NB", "(-3;1)"), ("DCD", "-3"), ("DCT", "1"), ("GTCD", "28"),
              ("GTCT", "-4"), ("NGHIEM", "-3"), ("NGHIEM", "1")]                     # x³ + 3x² − 9x + 1
BAC_BA_DON_DIEU = [("DB", "(-oo;oo)"), ("KHONG_CUC_TRI", ""), ("KHONG_NGHICH_BIEN", ""), ("KHONG_NGHIEM", "")]  # x³ + 3x
BAC_BA_KEP = [("DB", "(-oo;1)"), ("DB", "(1;oo)"), ("KHONG_CUC_TRI", ""), ("KHONG_NGHICH_BIEN", ""), ("NGHIEM", "1")]
TRUNG_PHUONG_1CT = [("NB", "(-oo;0)"), ("DB", "(0;oo)"), ("DCT", "0"), ("GTCT", "-2"), ("NGHIEM", "0"),
                    ("KHONG_CUC_DAI", "")]                                               # 3x⁴ + 5x² − 2
TRUNG_PHUONG_3CT = [("DB", "(-1;0)"), ("DB", "(1;oo)"), ("NB", "(-oo;-1)"), ("NB", "(0;1)"), ("DCD", "0"),
                    ("DCT", "-1"), ("DCT", "1"), ("GTCD", "1"), ("GTCT", "0"), ("NGHIEM", "-1"), ("NGHIEM", "0"),
                    ("NGHIEM", "1")]                                                     # x⁴ − 2x² + 1
HUU_TI = [("NB", "(-oo;1)"), ("NB", "(1;oo)"), ("KHONG_CUC_TRI", ""), ("KHONG_DONG_BIEN", ""),
          ("KHONG_NGHIEM", "")]                                                          # (x + 4)/(x − 1)


def _sk_dung(sk):
    """Sự kiện "đúng" kiểu kiem_thang (không có loại KHONG_*)."""
    return [p for p in sk if not p[0].startswith("KHONG_")]


def chan(cau, sk, hs=HS):
    return not loc_ban_nhap(cau, sk, hs)["cho_phep"]


# ------------------------------------------------------------------ (a) điểm thử
DIEM_THU_36 = [
    ('x**3 + (-3)*x**2 + (-24)*x + (1)',
     "Thử lần lượt x = -3, -1, 5 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-oo;-2)'), ('DB', '(4;oo)'), ('NB', '(-2;4)'), ('DCD', '-2'), ('DCT', '4'), ('GTCD', '29'), ('GTCT', '-79'), ('NGHIEM', '-2'), ('NGHIEM', '4')]),
    ('x**3 + (-3)*x**2 + (-24)*x + (2)',
     "Thử lần lượt x = -3, -1, 5 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-oo;-2)'), ('DB', '(4;oo)'), ('NB', '(-2;4)'), ('DCD', '-2'), ('DCT', '4'), ('GTCD', '30'), ('GTCT', '-78'), ('NGHIEM', '-2'), ('NGHIEM', '4')]),
    ('x**3 + (-3)*x**2 + (-24)*x + (3)',
     "Thử lần lượt x = -3, -1, 5 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-oo;-2)'), ('DB', '(4;oo)'), ('NB', '(-2;4)'), ('DCD', '-2'), ('DCT', '4'), ('GTCD', '31'), ('GTCT', '-77'), ('NGHIEM', '-2'), ('NGHIEM', '4')]),
    ('x**3 + (-3)*x**2 + (0)*x + (-1)',
     "Thử lần lượt x = -1, 1, 3 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-oo;0)'), ('DB', '(2;oo)'), ('NB', '(0;2)'), ('DCD', '0'), ('DCT', '2'), ('GTCD', '-1'), ('GTCT', '-5'), ('NGHIEM', '0'), ('NGHIEM', '2')]),
    ('x**3 + (-6)*x**2 + (0)*x + (1)',
     "Thử lần lượt x = -1, 1, 5 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-oo;0)'), ('DB', '(4;oo)'), ('NB', '(0;4)'), ('DCD', '0'), ('DCT', '4'), ('GTCD', '1'), ('GTCT', '-31'), ('NGHIEM', '0'), ('NGHIEM', '4')]),
    ('x**3 + (-6)*x**2 + (0)*x + (2)',
     "Thử lần lượt x = -1, 1, 5 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-oo;0)'), ('DB', '(4;oo)'), ('NB', '(0;4)'), ('DCD', '0'), ('DCT', '4'), ('GTCD', '2'), ('GTCT', '-30'), ('NGHIEM', '0'), ('NGHIEM', '4')]),
    ('x**3 + (-6)*x**2 + (9)*x + (3)',
     "Thử lần lượt x = 0, 2, 4 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-oo;1)'), ('DB', '(3;oo)'), ('NB', '(1;3)'), ('DCD', '1'), ('DCT', '3'), ('GTCD', '7'), ('GTCT', '3'), ('NGHIEM', '1'), ('NGHIEM', '3')]),
    ('x**3 + (-9)*x**2 + (24)*x + (-1)',
     "Thử lần lượt x = 1, 3, 5 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-oo;2)'), ('DB', '(4;oo)'), ('NB', '(2;4)'), ('DCD', '2'), ('DCT', '4'), ('GTCD', '19'), ('GTCT', '15'), ('NGHIEM', '2'), ('NGHIEM', '4')]),
    ('x**3 + (0)*x**2 + (-27)*x + (1)',
     "Thử lần lượt x = -4, -2, 4 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-oo;-3)'), ('DB', '(3;oo)'), ('NB', '(-3;3)'), ('DCD', '-3'), ('DCT', '3'), ('GTCD', '55'), ('GTCT', '-53'), ('NGHIEM', '-3'), ('NGHIEM', '3')]),
    ('x**3 + (0)*x**2 + (-3)*x + (-1)',
     "Thử lần lượt x = -2, 0, 2 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-oo;-1)'), ('DB', '(1;oo)'), ('NB', '(-1;1)'), ('DCD', '-1'), ('DCT', '1'), ('GTCD', '1'), ('GTCT', '-3'), ('NGHIEM', '-1'), ('NGHIEM', '1')]),
    ('x**3 + (0)*x**2 + (-3)*x + (1)',
     "Thử lần lượt x = -2, 0, 2 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-oo;-1)'), ('DB', '(1;oo)'), ('NB', '(-1;1)'), ('DCD', '-1'), ('DCT', '1'), ('GTCD', '3'), ('GTCT', '-1'), ('NGHIEM', '-1'), ('NGHIEM', '1')]),
    ('x**3 + (3)*x**2 + (-9)*x + (1)',
     "Thử lần lượt x = -4, -2, 2 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-oo;-3)'), ('DB', '(1;oo)'), ('NB', '(-3;1)'), ('DCD', '-3'), ('DCT', '1'), ('GTCD', '28'), ('GTCT', '-4'), ('NGHIEM', '-3'), ('NGHIEM', '1')]),
    ('(x+(-1))/(x+(1))',
     "Thử lần lượt x = -2, 0 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-oo;-1)'), ('DB', '(-1;oo)'), ('KHONG_CUC_TRI', ''), ('KHONG_NGHICH_BIEN', ''), ('KHONG_NGHIEM', '')]),
    ('(x+(-3))/(x+(-2))',
     "Thử lần lượt x = 1, 3 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-oo;2)'), ('DB', '(2;oo)'), ('KHONG_CUC_TRI', ''), ('KHONG_NGHICH_BIEN', ''), ('KHONG_NGHIEM', '')]),
    ('(x+(-3))/(x+(3))',
     "Thử lần lượt x = -4, -2 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-oo;-3)'), ('DB', '(-3;oo)'), ('KHONG_CUC_TRI', ''), ('KHONG_NGHICH_BIEN', ''), ('KHONG_NGHIEM', '')]),
    ('(x+(0))/(x+(2))',
     "Thử lần lượt x = -3, -1 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-oo;-2)'), ('DB', '(-2;oo)'), ('KHONG_CUC_TRI', ''), ('KHONG_NGHICH_BIEN', ''), ('KHONG_NGHIEM', '')]),
    ('(x+(0))/(x+(3))',
     "Thử lần lượt x = -4, -2 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-oo;-3)'), ('DB', '(-3;oo)'), ('KHONG_CUC_TRI', ''), ('KHONG_NGHICH_BIEN', ''), ('KHONG_NGHIEM', '')]),
    ('(x+(1))/(x+(-1))',
     "Thử lần lượt x = 0, 2 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('NB', '(-oo;1)'), ('NB', '(1;oo)'), ('KHONG_CUC_TRI', ''), ('KHONG_DONG_BIEN', ''), ('KHONG_NGHIEM', '')]),
    ('(x+(4))/(x+(-1))',
     "Thử lần lượt x = 0, 2 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('NB', '(-oo;1)'), ('NB', '(1;oo)'), ('KHONG_CUC_TRI', ''), ('KHONG_DONG_BIEN', ''), ('KHONG_NGHIEM', '')]),
    ('(x+(4))/(x+(2))',
     "Thử lần lượt x = -3, -1 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('NB', '(-oo;-2)'), ('NB', '(-2;oo)'), ('KHONG_CUC_TRI', ''), ('KHONG_DONG_BIEN', ''), ('KHONG_NGHIEM', '')]),
    ('x**4 + (-2)*x**2 + (-2)',
     "Thử lần lượt x = -2, -1/2, 1/2, 2 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-1;0)'), ('DB', '(1;oo)'), ('NB', '(-oo;-1)'), ('NB', '(0;1)'), ('DCD', '0'), ('DCT', '-1'), ('DCT', '1'), ('GTCD', '-2'), ('GTCT', '-3'), ('GTCT', '-3'), ('NGHIEM', '-1'), ('NGHIEM', '0'), ('NGHIEM', '1')]),
    ('x**4 + (-2)*x**2 + (0)',
     "Thử lần lượt x = -2, -1/2, 1/2, 2 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-1;0)'), ('DB', '(1;oo)'), ('NB', '(-oo;-1)'), ('NB', '(0;1)'), ('DCD', '0'), ('DCT', '-1'), ('DCT', '1'), ('GTCD', '0'), ('GTCT', '-1'), ('GTCT', '-1'), ('NGHIEM', '-1'), ('NGHIEM', '0'), ('NGHIEM', '1')]),
    ('x**4 + (-2)*x**2 + (1)',
     "Thử lần lượt x = -2, -1/2, 1/2, 2 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-1;0)'), ('DB', '(1;oo)'), ('NB', '(-oo;-1)'), ('NB', '(0;1)'), ('DCD', '0'), ('DCT', '-1'), ('DCT', '1'), ('GTCD', '1'), ('GTCT', '0'), ('GTCT', '0'), ('NGHIEM', '-1'), ('NGHIEM', '0'), ('NGHIEM', '1')]),
    ('x**4 + (-2)*x**2 + (2)',
     "Thử lần lượt x = -2, -1/2, 1/2, 2 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-1;0)'), ('DB', '(1;oo)'), ('NB', '(-oo;-1)'), ('NB', '(0;1)'), ('DCD', '0'), ('DCT', '-1'), ('DCT', '1'), ('GTCD', '2'), ('GTCT', '1'), ('GTCT', '1'), ('NGHIEM', '-1'), ('NGHIEM', '0'), ('NGHIEM', '1')]),
    ('x**4 + (-8)*x**2 + (-1)',
     "Thử lần lượt x = -3, -1, 1, 3 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-2;0)'), ('DB', '(2;oo)'), ('NB', '(-oo;-2)'), ('NB', '(0;2)'), ('DCD', '0'), ('DCT', '-2'), ('DCT', '2'), ('GTCD', '-1'), ('GTCT', '-17'), ('GTCT', '-17'), ('NGHIEM', '-2'), ('NGHIEM', '0'), ('NGHIEM', '2')]),
    ('x**4 + (-8)*x**2 + (0)',
     "Thử lần lượt x = -3, -1, 1, 3 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-2;0)'), ('DB', '(2;oo)'), ('NB', '(-oo;-2)'), ('NB', '(0;2)'), ('DCD', '0'), ('DCT', '-2'), ('DCT', '2'), ('GTCD', '0'), ('GTCT', '-16'), ('GTCT', '-16'), ('NGHIEM', '-2'), ('NGHIEM', '0'), ('NGHIEM', '2')]),
    ('x**4 + (-8)*x**2 + (2)',
     "Thử lần lượt x = -3, -1, 1, 3 (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, rồi chọn mũi tên tương ứng.",
     [('DB', '(-2;0)'), ('DB', '(2;oo)'), ('NB', '(-oo;-2)'), ('NB', '(0;2)'), ('DCD', '0'), ('DCT', '-2'), ('DCT', '2'), ('GTCD', '2'), ('GTCT', '-14'), ('GTCT', '-14'), ('NGHIEM', '-2'), ('NGHIEM', '0'), ('NGHIEM', '2')]),
]


@pytest.mark.parametrize("ham,cau,sk", DIEM_THU_36, ids=[r[0] for r in DIEM_THU_36])
def test_a_diem_thu_suy_ra_moc_bi_chan(ham, cau, sk):
    r = loc_ban_nhap(cau, sk, HS)
    assert r["cho_phep"] is False and r["ly_do"] == "LO_DAP_AN"


@pytest.mark.parametrize("ham,cau,sk", DIEM_THU_36[:6], ids=[r[0] for r in DIEM_THU_36[:6]])
def test_a_diem_thu_van_chan_khi_su_kien_khong_co_KHONG(ham, cau, sk):
    assert chan(cau, _sk_dung(sk))


@pytest.mark.parametrize("cau", [
    "Trên mỗi khoảng, chọn một giá trị thử nằm trong khoảng đó, thay vào y' (dạng tích nếu có) để lấy dấu.",
    "Em thử thay x = -5 vào y' xem được dấu gì.",               # một điểm thử: không suy ra được mốc
    "Thử lần lượt x = -5, -4 rồi so sánh.",                      # giữa hai điểm không có mốc
    "Mốc nào là phân số hoặc chứa căn thì em ước lượng giá trị gần đúng để so sánh.",
])
def test_a_diem_thu_khong_lo_duoc_qua(cau):
    assert not chan(cau, BAC_BA_2CT)


# ------------------------------------------------------------------ (b) câu điều kiện luôn đúng với bài
HANG_SO_12 = [
    ('(x+(-1))/(x+(1))',
     "Rút gọn y' thành một phân thức, rồi xét riêng tử số và mẫu số. Nếu tử là hằng số khác 0 thì y' = 0 vô nghiệm.",
     [('DB', '(-oo;-1)'), ('DB', '(-1;oo)'), ('KHONG_CUC_TRI', ''), ('KHONG_NGHICH_BIEN', ''), ('KHONG_NGHIEM', '')]),
    ('(x+(-3))/(x+(-2))',
     "Rút gọn y' thành một phân thức, rồi xét riêng tử số và mẫu số. Nếu tử là hằng số khác 0 thì y' = 0 vô nghiệm.",
     [('DB', '(-oo;2)'), ('DB', '(2;oo)'), ('KHONG_CUC_TRI', ''), ('KHONG_NGHICH_BIEN', ''), ('KHONG_NGHIEM', '')]),
    ('(x+(-3))/(x+(3))',
     "Rút gọn y' thành một phân thức, rồi xét riêng tử số và mẫu số. Nếu tử là hằng số khác 0 thì y' = 0 vô nghiệm.",
     [('DB', '(-oo;-3)'), ('DB', '(-3;oo)'), ('KHONG_CUC_TRI', ''), ('KHONG_NGHICH_BIEN', ''), ('KHONG_NGHIEM', '')]),
    ('(x+(0))/(x+(2))',
     "Rút gọn y' thành một phân thức, rồi xét riêng tử số và mẫu số. Nếu tử là hằng số khác 0 thì y' = 0 vô nghiệm.",
     [('DB', '(-oo;-2)'), ('DB', '(-2;oo)'), ('KHONG_CUC_TRI', ''), ('KHONG_NGHICH_BIEN', ''), ('KHONG_NGHIEM', '')]),
    ('(x+(0))/(x+(3))',
     "Rút gọn y' thành một phân thức, rồi xét riêng tử số và mẫu số. Nếu tử là hằng số khác 0 thì y' = 0 vô nghiệm.",
     [('DB', '(-oo;-3)'), ('DB', '(-3;oo)'), ('KHONG_CUC_TRI', ''), ('KHONG_NGHICH_BIEN', ''), ('KHONG_NGHIEM', '')]),
    ('(x+(1))/(x+(-1))',
     "Rút gọn y' thành một phân thức, rồi xét riêng tử số và mẫu số. Nếu tử là hằng số khác 0 thì y' = 0 vô nghiệm.",
     [('NB', '(-oo;1)'), ('NB', '(1;oo)'), ('KHONG_CUC_TRI', ''), ('KHONG_DONG_BIEN', ''), ('KHONG_NGHIEM', '')]),
    ('(x+(4))/(x+(-1))',
     "Rút gọn y' thành một phân thức, rồi xét riêng tử số và mẫu số. Nếu tử là hằng số khác 0 thì y' = 0 vô nghiệm.",
     [('NB', '(-oo;1)'), ('NB', '(1;oo)'), ('KHONG_CUC_TRI', ''), ('KHONG_DONG_BIEN', ''), ('KHONG_NGHIEM', '')]),
    ('(x+(4))/(x+(2))',
     "Rút gọn y' thành một phân thức, rồi xét riêng tử số và mẫu số. Nếu tử là hằng số khác 0 thì y' = 0 vô nghiệm.",
     [('NB', '(-oo;-2)'), ('NB', '(-2;oo)'), ('KHONG_CUC_TRI', ''), ('KHONG_DONG_BIEN', ''), ('KHONG_NGHIEM', '')]),
]


@pytest.mark.parametrize("ham,cau,sk", HANG_SO_12, ids=[r[0] for r in HANG_SO_12])
def test_b_tu_hang_so_vo_nghiem_bi_chan_o_huu_ti(ham, cau, sk):
    r = loc_ban_nhap(cau, sk, HS)
    assert r["cho_phep"] is False and r["ly_do"] == "LO_DAP_AN"
    assert chan(cau, _sk_dung(sk))


CAU_TU_HANG = "Rút gọn y' thành một phân thức, rồi xét riêng tử số và mẫu số. Nếu tử là hằng số khác 0 thì y' = 0 vô nghiệm."


@pytest.mark.parametrize("sk", [BAC_BA_2CT, TRUNG_PHUONG_3CT, TRUNG_PHUONG_1CT], ids=["bac_ba_2ct", "tp_3ct", "tp_1ct"])
def test_b_dieu_kien_sai_voi_bai_khong_chan(sk):
    assert not chan(CAU_TU_HANG, sk)


@pytest.mark.parametrize("cau,sk,bi_chan", [
    ("Tử số là hằng số nên y' = 0 vô nghiệm.", HUU_TI, True),
    ("Nếu y' không đổi dấu thì hàm số không có cực trị.", BAC_BA_DON_DIEU, True),
    ("Nếu y' không đổi dấu thì hàm số không có cực trị.", BAC_BA_KEP, True),
    ("Nếu y' không đổi dấu thì hàm số không có cực trị.", BAC_BA_2CT, False),
    ("Nếu y' có nghiệm kép thì hàm số không có cực trị.", BAC_BA_KEP, True),
    ("Nếu y' có nghiệm kép thì hàm số không có cực trị.", BAC_BA_2CT, False),
    ("Em nhìn vào tử số của y' sau khi rút gọn: tử số đó có chứa x không, và điều đó cho biết gì về phương trình y' = 0?",
     HUU_TI, False),
    ("Nếu em làm đúng thì sẽ thấy hàm số không có cực trị.", HUU_TI, True),   # điều kiện không phải toán: vẫn là khẳng định
])
def test_b_cau_dieu_kien(cau, sk, bi_chan):
    assert chan(cau, sk) is bi_chan


# ------------------------------------------------------------------ (c) phủ định hợp lệ không bị chặn nhầm
KETLUAN_3 = ("Đối chiếu từng dòng kết luận với từng cột của bảng: khoảng có dấu + thì hàm số đồng biến, dấu − thì nghịch "
             "biến. Tại mỗi điểm tới hạn, xem dấu hai bên: nếu + sang − thì đó là điểm cực đại, nếu − sang + thì đó là "
             "điểm cực tiểu, nếu không đổi dấu thì hàm số không đạt cực trị tại điểm đó. Phân biệt điểm cực trị x₀ với "
             "giá trị cực trị y(x₀).")
XETDAU_3 = ("Lập bảng với hàng x gồm các điểm tới hạn em đã tìm, xếp từ nhỏ đến lớn (nếu không có điểm tới hạn nào thì cả "
            "trục số là một khoảng). Trên mỗi khoảng, chọn một giá trị thử nằm trong khoảng đó, thay vào y' để lấy dấu.")
DIEM_THUA_3 = ("Đối chiếu từng mốc trên hàng x với dòng điểm tới hạn em đã viết; mốc nào không có ở đó (và cũng không "
               "phải điểm bị loại khỏi TXĐ) thì bỏ ra khỏi bảng, rồi xem lại ô dấu của khoảng vừa được gộp.")
TAT_CA_SK = [BAC_BA_2CT, BAC_BA_DON_DIEU, BAC_BA_KEP, TRUNG_PHUONG_1CT, TRUNG_PHUONG_3CT, HUU_TI]
TEN_SK = ["bac_ba_2ct", "bac_ba_don_dieu", "bac_ba_kep", "tp_1ct", "tp_3ct", "huu_ti"]


@pytest.mark.parametrize("cau", [KETLUAN_3, XETDAU_3, DIEM_THUA_3], ids=["KETLUAN_3", "XETDAU_3", "DIEM_THUA_3"])
@pytest.mark.parametrize("sk", TAT_CA_SK + [_sk_dung(s) for s in TAT_CA_SK],
                         ids=TEN_SK + [t + "_dung" for t in TEN_SK])
def test_c_cau_thang_mau_khong_bi_chan(cau, sk):
    assert not chan(cau, sk)


@pytest.mark.parametrize("cau", [
    "Nếu y' không đổi dấu khi x đi qua x₀ thì hàm số không đạt cực trị tại x₀.",
    "Nếu y' bằng 0 tại một điểm mà hai bên điểm đó y' cùng dấu thì điểm đó không phải điểm cực trị.",
    "Nếu k < 0 thì phương trình x² = k không có nghiệm thực.",
])
@pytest.mark.parametrize("sk", [BAC_BA_DON_DIEU, BAC_BA_KEP, HUU_TI, TRUNG_PHUONG_1CT], ids=["don_dieu", "kep", "huu_ti", "tp_1ct"])
def test_c_quy_tac_chung_qua(cau, sk):
    assert not chan(cau, sk)


@pytest.mark.parametrize("cau,sk", [
    ("Hàm số không có cực trị.", BAC_BA_DON_DIEU),
    ("Hàm số không có cực trị.", HUU_TI),
    ("Phương trình y' = 0 không có nghiệm.", HUU_TI),
    ("Hàm số không có điểm cực đại.", TRUNG_PHUONG_1CT),
    ("Hàm số đạt cực tiểu tại x bằng không.", TRUNG_PHUONG_1CT),
])
def test_c_khang_dinh_lo_van_chan(cau, sk):
    assert chan(cau, sk)


def test_c_khong_phu_dinh_khong_doc_thanh_so_0():
    # trước 0003: "hàm số không đạt" -> "hàm số 0 đạt" khớp DCT = 0 / GTCT = 0 -> chặn nhầm
    cau = "nếu − sang + thì đó là điểm cực tiểu, nếu không đổi dấu thì hàm số không đạt cực trị tại điểm đó."
    assert not chan(cau, TRUNG_PHUONG_1CT)
    assert not chan(cau, _sk_dung(TRUNG_PHUONG_3CT))


# ------------------------------------------------------------------ lộ bằng lời đối chiếu sự kiện
@pytest.mark.parametrize("cau,sk,ly_do", [
    ("Phương trình có hai nghiệm phân biệt.", BAC_BA_2CT, "LO_DAP_AN"),
    ("Có ba điểm tới hạn.", TRUNG_PHUONG_3CT, "LO_DAP_AN"),
    ("y' có nghiệm kép.", BAC_BA_KEP, "LO_DAP_AN"),
    ("y' luôn dương.", BAC_BA_DON_DIEU, "LO_DAP_AN"),
    ("Vì vậy hàm luôn đồng biến.", BAC_BA_DON_DIEU, "LO_DAP_AN"),
    ("Hàm nghịch biến trên từng khoảng xác định.", HUU_TI, "LO_DAP_AN"),
    ("Hàm đa thức nên tập xác định là ℝ.", BAC_BA_2CT, "LO_DAP_AN"),
    ("Mẫu bằng 0 khi x = 1 nên loại 1.", HUU_TI, "LO_DAP_AN"),
    ("Còn một điểm ở giữa em chưa ghi.", TRUNG_PHUONG_3CT, "LO_DAP_AN"),
    ("Điểm đầu em tìm đúng rồi, chỉ còn thiếu điểm lớn hơn.", BAC_BA_2CT, "XAC_NHAN"),
])
def test_lo_bang_loi_bi_chan(cau, sk, ly_do):
    for s in (sk, _sk_dung(sk)):
        r = loc_ban_nhap(cau, s, None)
        assert r["cho_phep"] is False and r["ly_do"] == ly_do


@pytest.mark.parametrize("cau,sk", [
    ("Phương trình có hai nghiệm phân biệt.", TRUNG_PHUONG_3CT),      # số lượng sai với bài
    ("y' luôn dương.", BAC_BA_2CT),
    ("Hàm nghịch biến trên từng khoảng xác định.", BAC_BA_2CT),
    ("Dòng TXĐ của em chưa khớp với điều kiện mẫu số 2x khác 0. Em kiểm tra lại cả giá trị bị loại lẫn cách viết.", HUU_TI),
    ("Phương trình y' = 0 có mấy nghiệm?", BAC_BA_2CT),                # câu hỏi
    ("Tập xác định của hàm đa thức là gì?", BAC_BA_2CT),
])
def test_lo_bang_loi_khong_chan_nham(cau, sk):
    assert not chan(cau, sk)


# ------------------------------------------------------------------ đường thật của app: /v1/goi-y (thang mẫu, #40)
HAM_THANG = ["x**3+3*x**2-9*x+1", "x**3+3*x", "x**3-3*x**2+3*x", "x**4-2*x**2+1", "3*x**4+5*x**2-2", "(x+4)/(x-1)"]


def _o_thang_mau():
    from app import thang_mau
    out = []
    for dang in ("bac_ba", "trung_phuong", "huu_ti"):
        doc = thang_mau.nap(dang)
        for mb, buoc in (doc.get("thang") or {}).items():
            for loai, t in buoc.items():
                if loai == "_meta" or not isinstance(t, dict):
                    continue
                for cap in (1, 2, 3):
                    if (t.get(str(cap)) or {}).get("noi_dung"):
                        out.append((dang, mb, loai, cap))
    return out


def test_goi_y_thang_mau_moi_cau_qua_loc_khong_lui_cap():
    """Mọi câu thang mẫu đã điền (cấp có nội dung) phải tới học sinh đúng cấp: không bị bộ lọc chặn rồi lùi cấp.
    Trước 0003: KETLUAN/SAI_KET_LUAN/3, XETDAU/chung/3, XETDAU/DIEM_THUA/3 bị chặn nhầm (phủ định trong câu điều kiện,
    "hàm số không đạt" đọc thành "hàm số 0 đạt")."""
    from app import thang_mau
    from app.generator import latex_ham
    from app.machine import bai_lam_may, su_kien_bao_ve
    o = _o_thang_mau()
    assert len(o) > 100
    loi = []
    for ham in HAM_THANG:
        dang = thang_mau.dang_cua(ham)
        sk = [{"loai": a, "gia_tri": b} for a, b in su_kien_bao_ve(bai_lam_may(ham))]
        de = "Tìm các khoảng đồng biến, nghịch biến và cực trị của hàm số $y = %s$." % latex_ham(ham)
        for d, mb, loai, cap in o:
            if d != dang:
                continue
            r = thang_mau.goi_y({"ham": ham, "de_bai": de, "ma_buoc": mb, "loai_ket_qua": loai, "cap": cap, "su_kien": sk})
            chan = [n for n in r["nhat_ky"] if n.get("ly_do") != "KHONG_DIEN_DUOC"]
            if chan:
                loi.append((ham, mb, loai, cap, chan))
    assert loi == []
