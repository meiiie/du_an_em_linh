# -*- coding: utf-8 -*-
"""Bản vá dư 0002b trên main 507874c (Kiểm định, 29/09): phần PR #32 chưa có.
- su_kien_bao_ve giữ dấu "+" (trước: str(s).replace("+", "") -> "1  sqrt(2)"), chuẩn hóa bằng SymPy qua bộ đọc an toàn K.P;
- fmt_domain: TXĐ hữu tỉ máy in "R \\ {…}" (trước: "(-oo; -3) U (-3; +oo)"); Sư phạm: máy không nối khoảng bằng U;
- M3 của loc.py trích được số vô tỉ bậc hai (sqrt, \\sqrt{}, √, căn, ±, \\pm): câu lộ 1 ± √2 bị chặn, câu an toàn vẫn qua;
- TXĐ HỌC SINH viết bằng U / ∪ / \\cup vẫn DAT (luật dấu U chỉ áp cho kết luận đơn điệu ở B.DH.KETLUAN; Sư phạm 12:26).
Giữ kiến trúc PR #32 (app/dau_vao.py): sự kiện hỏng vẫn bị chặn theo lý do của PR #32."""
import pytest

import app.leakfilter as lf
from app.grader import grade
from app.machine import bai_lam_may, su_kien_bao_ve

HAM_VT = "x**3 - 3*x**2 - 3*x + 1"   # cực đại tại 1 - √2, cực tiểu tại 1 + √2


def test_cuc_tri_vo_ti_giu_dau_cong():
    sk = su_kien_bao_ve(bai_lam_may(HAM_VT))
    assert ["DCT", "1+sqrt(2)"] in sk and ["DCD", "1-sqrt(2)"] in sk
    assert ["NGHIEM", "1+sqrt(2)"] in sk and ["NB", "(1-sqrt(2);1+sqrt(2))"] in sk
    assert ["GTCD", "-4+4*sqrt(2)"] in sk and ["GTCT", "-4*sqrt(2)-4"] in sk
    assert all("  " not in b and "1sqrt" not in b for _, b in sk)


def test_su_kien_huu_ti_khong_doi():
    sk = su_kien_bao_ve(bai_lam_may("x**3 - 3*x + 1"))
    for p in (["DB", "(-oo;-1)"], ["DB", "(1;oo)"], ["NB", "(-1;1)"], ["DCD", "-1"], ["DCT", "1"], ["GTCD", "3"], ["GTCT", "-1"]):
        assert p in sk, (p, sk)


@pytest.mark.parametrize("ham,txd", [("x/(x+3)", "R \\ {-3}"), ("(x**2 + 1)/x", "R \\ {0}"), ("(x+1)/(x-1)", "R \\ {1}"),
                                     ("(x**2 - 3*x + 6)/(x - 1)", "R \\ {1}"), ("x**3 - 3*x", "R")])
def test_txd_may_in_R_tru(ham, txd):
    assert bai_lam_may(ham)["TXD"] == txd
    assert " U " not in bai_lam_may(ham)["TXD"]


def test_su_kien_hong_van_bi_chan():
    # (loại sự kiện lạ, vd LOAI_LA, trên 507874c vẫn được bỏ qua — ngoài phạm vi 0002b, ghi ở README)
    for sk in ([["DCT", "1  sqrt(2)"]], [["DCD", "__import__('os')"]]):
        q = lf.loc_ban_nhap("Em thử xét dấu y' xem sao.", sk)
        assert q["cho_phep"] is False and q["ly_do"] in ("DAU_VAO_KHONG_HOP_LE", "LOI_KIEM_TRA", "SU_KIEN_LOI"), (sk, q)


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
    q = lf.loc_ban_nhap(cau, su_kien_bao_ve(bai_lam_may(HAM_VT)), "em không biết làm tiếp")
    assert q["cho_phep"] is False and q["ly_do"] == "LO_DAP_AN", q


@pytest.mark.parametrize("cau", [
    "Em thử xét dấu y' xem sao.",
    "Căn 2 xấp xỉ 1,41 em nhé.",
    "Em giải y' = 0 bằng công thức nghiệm, tính Δ' trước nhé.",
    "Hệ số của x là 3, em tính lại đạo hàm nhé.",
])
def test_cau_an_toan_bai_vo_ti_van_qua(cau):
    q = lf.loc_ban_nhap(cau, su_kien_bao_ve(bai_lam_may(HAM_VT)), "em không biết làm tiếp")
    assert q["cho_phep"] is True, q


def _txd_payload(ham, txd, dh, nop):
    return {"ham": ham, "nop_toi": nop, "cac_buoc": [
        {"ma_buoc": "B.DH.TXD", "cac_dong": [{"dong": 0, "latex": txd}]},
        {"ma_buoc": "B.DH.DAOHAM", "cac_dong": [{"dong": 0, "latex": dh}]}]}


@pytest.mark.parametrize("txd", ["(-\\infty;-3)\\cup(-3;+\\infty)", "(−∞; −3) ∪ (−3; +∞)", "(-oo;-3) U (-3;+oo)",
                                 "(-\\infty; -3) U (-3; +\\infty)", "\\mathbb{R}\\setminus\\{-3\\}", "R \\ {-3}"])
@pytest.mark.parametrize("nop", ["B.DH.TXD", "B.DH.DAOHAM"])
def test_txd_hoc_sinh_viet_bang_U_van_DAT(txd, nop):
    r = grade(_txd_payload("x/(x+3)", txd, "\\frac{3}{(x+3)^{2}}", nop))
    assert r["ket_qua"] == "DAT", r
    assert "ERR.DH.07" not in str(r)


def test_txd_hoc_sinh_sai_van_SAI():
    r = grade(_txd_payload("x/(x+3)", "(-oo;-3) U (-2;+oo)", "\\frac{3}{(x+3)^{2}}", "B.DH.TXD"))
    assert r["ket_qua"] == "SAI" and r["buoc_sai"]["ma_buoc"] == "B.DH.TXD"
