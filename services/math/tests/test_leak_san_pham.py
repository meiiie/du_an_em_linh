# -*- coding: utf-8 -*-
"""LeakGuard sản phẩm (app/leakfilter.py): fail closed, lỗ tìm thấy ở thử gia sư giai đoạn 2, bộ tin nhắn Kiểm định."""
import os

import pytest
import yaml

import app.leakfilter as LF
from app.leakfilter import loc_ban_nhap
from app.paths import KIEMDINH

BO = yaml.safe_load(open(os.path.join(KIEMDINH, "loc-lo-dap-an", "bo-tin-nhan.yaml"), encoding="utf-8"))

# Bài A của thử gia sư: y = x^3 - 6x^2 + 9x + 2
SK_A = [["DB", "(-oo;1)"], ["DB", "(3;oo)"], ["NB", "(1;3)"], ["DCD", "1"], ["DCT", "3"], ["GTCD", "6"], ["GTCT", "2"],
        ["NGHIEM", "1"], ["NGHIEM", "3"]]
# Bài C: y = x^4 - 8x^2 - 1
SK_C = [["DB", "(-2;0)"], ["DB", "(2;oo)"], ["NB", "(-oo;-2)"], ["NB", "(0;2)"], ["DCD", "0"], ["DCT", "-2"], ["DCT", "2"],
        ["GTCD", "-1"], ["GTCT", "-17"], ["NGHIEM", "-2"], ["NGHIEM", "0"], ["NGHIEM", "2"]]

LO_A = [
    "Hàm nghịch biến trên (1;3).",
    "Hàm nghịch biến trên (1, 3).",
    "Hàm nghịch biến trên khoảng từ một đến ba.",
    "Hàm giảm giữa 1 và 3.",
    "Hàm đi xuống khi x nằm giữa một và ba.",
    "The function is decreasing on (1, 3) and has a local max at x = 1.",
    "Local maximum value is 6.",
    "Điểm cao nhất cục bộ ứng với hoành độ 1, tung độ 6.",
    "y' = 0 khi x bằng một hoặc x bằng ba.",
    "Nghiệm của y' là 1 và 3.",
    "Cực đại tại x = \\frac{2}{2}.",
    "Cực đại tại x = 1,0.",
    "Giá trị cực đại bằng sáu.",
    "Đồng biến khi x > 3 hoặc x < 1.",
    "Hàm tăng trên (3; +\\infty).",
    "Hàm tăng trên (3;+∞) và (−∞;1).",
    "It is increasing when x is greater than three.",
    "The minimum is at three.",
    "Hàm giảm khi x lớn hơn một và nhỏ hơn ba.",
    "It decreases between 1 and 3.",
]
LO_C = [
    "Giá trị cực tiểu là -17.",
    "Giá trị cực tiểu là âm mười bảy.",
    "Giá trị cực tiểu bằng âm 17.",
    "Cực tiểu tại x = ±2.",
    "Cực tiểu tại x bằng cộng trừ hai.",
    "Hàm có ba điểm cực trị.",
    "The minimum value is negative seventeen.",
]
HOP_LE = [
    "Em tính y' trước nhé.",
    "Dùng (x^n)' = n·x^(n-1) và đạo hàm của tổng bằng tổng các đạo hàm.",
    "Khoảng đồng biến là khoảng mà trên đó y' > 0 (y' = 0 chỉ tại hữu hạn điểm).",
    "Em sắp lại các mốc trên hàng x theo thứ tự tăng dần trước, rồi xét dấu từng khoảng.",
    "Em chưa nộp bước nào. Em viết tập xác định rồi bấm Nộp.",
]


@pytest.mark.parametrize("t", LO_A)
def test_chan_lo_bai_a(t):
    assert loc_ban_nhap(t, SK_A)["cho_phep"] is False


@pytest.mark.parametrize("t", LO_C)
def test_chan_lo_bai_c(t):
    assert loc_ban_nhap(t, SK_C)["cho_phep"] is False


@pytest.mark.parametrize("t", HOP_LE)
def test_khong_chan_goi_y_hop_le(t):
    assert loc_ban_nhap(t, SK_A)["cho_phep"] is True


@pytest.mark.parametrize("nhap", ["Đúng rồi, em làm đúng.", "Đúng, hai số em tìm được là chính xác.", "Chưa đúng, em xem lại số thứ hai.",
                                  "Correct!", "Gần đúng rồi em."])
def test_chan_xac_nhan_khi_hs_neu_dap_an(nhap):
    r = loc_ban_nhap(nhap, SK_A, "em ra x = 1 và x = 3 đúng không ạ?")
    assert r["cho_phep"] is False and r["ly_do"] == "XAC_NHAN"
    r = loc_ban_nhap(nhap, SK_C, "cực tiểu có phải là âm mười bảy không?")
    assert r["cho_phep"] is False


def test_khong_chan_khi_hs_hoi_khai_niem():
    r = loc_ban_nhap("Đúng là câu hỏi hay: khoảng đồng biến là khoảng mà y' dương.", SK_A, "khoảng đồng biến nghĩa là gì ạ?")
    assert r["cho_phep"] is True


def test_fail_closed(monkeypatch):
    def hong(*a, **k):
        raise RuntimeError("hỏng")
    monkeypatch.setattr(LF, "_vi_pham", hong)
    r = loc_ban_nhap("Em tính y' trước nhé.", SK_A)
    assert r["cho_phep"] is False and r["loi"] is True


def test_bo_tin_nhan_kiem_dinh():
    """Mọi tin lộ rõ (M01–M38) bị chặn; chặn nhầm không tăng so với M3+M1 cũ."""
    lot, nham_moi = [], []
    for tn in BO["tin_nhan"]:
        b = BO["bai"][tn["bai"]]
        chan = not loc_ban_nhap(tn["text"], b["su_kien"])["cho_phep"]
        L = LF._L()
        cu = bool(L.m23(tn["text"], b, True) or L.m1(tn["text"], b))
        if tn["lo"] and tn.get("dien_dat") != "vong_vo" and not chan:
            lot.append(tn["id"])
        if not tn["lo"] and chan and not cu:
            nham_moi.append(tn["id"])
    assert lot == []
    assert nham_moi == []


def test_sp11_y_phay_bang_0_khong_bi_chan_nham():
    sk = [["NGHIEM", "0"], ["NGHIEM", "2"], ["DCD", "0"], ["GTCD", "2"]]
    assert loc_ban_nhap("Đặt nhân tử chung để y' thành tích; mỗi nhân tử bằng 0 cho một điểm tới hạn.", sk)["cho_phep"] is True
    assert loc_ban_nhap("Liệt kê nghiệm của y' = 0 mỗi điểm một dòng.", sk)["cho_phep"] is True
    assert loc_ban_nhap("Nghiệm của y' = 0 là x = 0.", sk)["cho_phep"] is False


def test_sp12_loi_va_xac_nhan_tung_phan():
    assert loc_ban_nhap("Hàm đồng biến khi x nhỏ hơn một và khi x lớn hơn ba.", SK_A)["cho_phep"] is False
    r = loc_ban_nhap("Nghiệm đầu tiên em tìm là đúng rồi, em chỉ còn thiếu nghiệm lớn hơn.", SK_A, "em được x = 1")
    assert r["cho_phep"] is False


def test_so_0_dung_dau_khong_lam_lot():
    assert loc_ban_nhap("Giá trị cực đại là 06.", SK_A)["cho_phep"] is False
    assert loc_ban_nhap("Nghịch biến trên (01; 03).", SK_A)["cho_phep"] is False
    r = loc_ban_nhap("Em xem lại bài 08 trong vở nhé.", SK_A)
    assert r["cho_phep"] is True and r["loi"] is False


# --- 29/09: cau_hoc_sinh, THIEU_NGU_CANH, sự kiện phủ định KHONG_CUC_TRI ---------------------------------
SK_HUU_TI = [["DB", "(-oo;-3)"], ["DB", "(-3;oo)"], ["KHONG_CUC_TRI", ""], ["KHONG_NGHICH_BIEN", ""], ["KHONG_NGHIEM", ""]]


def test_thieu_cau_hoc_sinh_tra_loi_ngan_bi_chan():
    r = loc_ban_nhap("Không.", SK_HUU_TI, None)
    assert r["cho_phep"] is False and r["ly_do"] == "THIEU_NGU_CANH"
    r = loc_ban_nhap("có", SK_HUU_TI, None)
    assert r["cho_phep"] is False and r["ly_do"] == "THIEU_NGU_CANH"


def test_khong_cuc_tri_bi_chan():
    for nhap in ("Hàm này không có cực trị đâu em.", "Hàm số không có điểm cực trị.", "This function has no local maximum.",
                 "Cực trị thì không có."):
        assert loc_ban_nhap(nhap, SK_HUU_TI, "cho em hỏi")["cho_phep"] is False, nhap


def test_tra_loi_ngan_dung_chu_de_bi_chan():
    assert loc_ban_nhap("Không có.", SK_HUU_TI, "hàm này có cực trị không ạ?")["cho_phep"] is False


def test_goi_y_hop_le_van_qua():
    assert loc_ban_nhap("Em xét dấu y' trên từng khoảng của tập xác định rồi đọc bảng.", SK_HUU_TI, "em làm tiếp sao ạ")["cho_phep"] is True


def test_su_kien_may_co_khong_cuc_tri():
    from app.machine import bai_lam_may, su_kien_bao_ve
    sk = su_kien_bao_ve(bai_lam_may("x/(x+3)"))
    assert ["KHONG_CUC_TRI", ""] in sk
    sk3 = su_kien_bao_ve(bai_lam_may("x**3-6*x**2+9*x+2"))
    assert not any(a == "KHONG_CUC_TRI" for a, _ in sk3)
