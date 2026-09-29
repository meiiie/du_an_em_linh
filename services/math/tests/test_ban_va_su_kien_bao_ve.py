# -*- coding: utf-8 -*-
"""Bản vá Kiểm định cho su_kien_bao_ve và bộ lọc (Sư phạm 11:54, No 11:58):
- cực trị vô tỉ giữ dấu "+" (trước: str(s).replace("+", "") -> "1  sqrt(2)");
- TXĐ hữu tỉ in R \\ {…} (trước: "(-oo; -3) U (-3; +oo)");
- sự kiện không đọc được -> chặn, ly_do SU_KIEN_LOI;
- M3 trích được số vô tỉ bậc hai: câu lộ 1 ± √2 bị chặn, câu an toàn vẫn qua."""
import pytest

import app.leakfilter as lf
from app.machine import bai_lam_may, su_kien_bao_ve

HAM_VT = "x**3 - 3*x**2 - 3*x + 1"   # cực đại tại 1 - √2, cực tiểu tại 1 + √2


def test_cuc_tri_vo_ti_giu_dau_cong():
    sk = su_kien_bao_ve(bai_lam_may(HAM_VT))
    assert ["DCT", "1+sqrt(2)"] in sk and ["DCD", "1-sqrt(2)"] in sk
    assert ["NGHIEM", "1+sqrt(2)"] in sk and ["NB", "(1-sqrt(2);1+sqrt(2))"] in sk
    assert all("  " not in b and "1sqrt" not in b for _, b in sk)


@pytest.mark.parametrize("ham,txd", [("x/(x+3)", "R \\ {-3}"), ("(x**2 + 1)/x", "R \\ {0}"), ("(x+1)/(x-1)", "R \\ {1}"),
                                     ("x**3 - 3*x", "R")])
def test_txd(ham, txd):
    assert bai_lam_may(ham)["TXD"] == txd


def test_su_kien_hong_SU_KIEN_LOI():
    q = lf.loc_ban_nhap("Em thử xét dấu y' xem sao.", [["DCT", "1  sqrt(2)"]])
    assert q["cho_phep"] is False and q["ly_do"] == "SU_KIEN_LOI"
    q = lf.loc_ban_nhap("Em thử xét dấu y' xem sao.", [["LOAI_LA", "1"]])
    assert q["cho_phep"] is False and q["ly_do"] == "SU_KIEN_LOI"


@pytest.mark.parametrize("cau", [
    "Hàm đạt cực tiểu tại x = 1 + sqrt(2).",
    "Cực tiểu tại $x = 1+\\sqrt{2}$.",
    "Cực tiểu tại x = 1+√2",
    "Giá trị cực tiểu là -4 - 4√2",
    "Hàm nghịch biến trên (1-√2; 1+√2)",
    "Hàm nghịch biến trên $(1-\\sqrt{2};1+\\sqrt{2})$",
    "Nghiệm của y'=0 là x = 1 ± √2",
    "x = 1 \\pm \\sqrt{2}",
    "Hàm đồng biến trên (1+√2; +∞)",
])
def test_lo_vo_ti_bi_chan(cau):
    q = lf.loc_ban_nhap(cau, su_kien_bao_ve(bai_lam_may(HAM_VT)))
    assert q["cho_phep"] is False and q["ly_do"] == "LO_DAP_AN"


@pytest.mark.parametrize("cau", [
    "Em thử xét dấu y' xem sao.",
    "Căn 2 xấp xỉ 1,41 em nhé.",
    "Em giải y' = 0 bằng công thức nghiệm, tính Δ' trước nhé.",
    "Hệ số của x là 3, em tính lại đạo hàm nhé.",
])
def test_cau_an_toan_bai_vo_ti_van_qua(cau):
    q = lf.loc_ban_nhap(cau, su_kien_bao_ve(bai_lam_may(HAM_VT)))
    assert q == {"cho_phep": True, "lop_chinh": "sympy", "lop_phu": "chuoi", "loi": False, "ly_do": None}
