# -*- coding: utf-8 -*-
"""SP-03 (ô kết luận theo nhãn), SP-04 (tập nghiệm, tiền tố không xác định, \\frac lồng)."""
from app.grader import grade


def _payload(ham, txd, dh, nghiem, cells, kl, nop="B.DH.KETLUAN"):
    return {"ham": ham, "nop_toi": nop, "cac_buoc": [
        {"ma_buoc": "B.DH.TXD", "cac_dong": [{"dong": 0, "latex": txd}]},
        {"ma_buoc": "B.DH.DAOHAM", "cac_dong": [{"dong": 0, "latex": dh}]},
        {"ma_buoc": "B.DH.NGHIEM", "cac_dong": nghiem},
        {"ma_buoc": "B.DH.XETDAU", "bang": {"loai_bang": "XET_DAU", "cac_o": cells}},
        {"ma_buoc": "B.DH.KETLUAN", "cac_dong": kl},
    ]}


CELLS_A = [{"hang": "X", "k": 0, "gia_tri": "1"}, {"hang": "X", "k": 1, "gia_tri": "3"},
           {"hang": "DAU_YPHAY", "k": 0, "gia_tri": "+"}, {"hang": "DAU_YPHAY", "k": 2, "gia_tri": "-"}, {"hang": "DAU_YPHAY", "k": 4, "gia_tri": "+"}]


def test_sp03_placeholder_dung_nhan():
    kl = [{"dong": 0, "latex": "(-\\infty;1) và (3;+\\infty)", "loai": "DONG_BIEN"},
          {"dong": 1, "latex": "(1;3)", "loai": "NGHICH_BIEN"},
          {"dong": 2, "latex": "x = 1, y = 6", "loai": "CUC_DAI"},
          {"dong": 3, "latex": "x = 3, y = 2", "loai": "CUC_TIEU"}]
    r = grade(_payload("x**3-6*x**2+9*x+2", "\\mathbb{R}", "3x^{2}-12x+9", [{"dong": 0, "latex": "x\\in\\{1;3\\}"}], CELLS_A, kl))
    assert r["ket_qua"] == "DAT", r


def test_sp03_chi_hoi_don_dieu_khong_can_o_cuc_tri():
    kl = [{"dong": 0, "latex": "(-\\infty;1) và (3;+\\infty)", "loai": "DONG_BIEN"},
          {"dong": 1, "latex": "(1;3)", "loai": "NGHICH_BIEN"}]
    r = grade(_payload("x**3-6*x**2+9*x+2", "\\mathbb{R}", "3x^{2}-12x+9", [{"dong": 0, "latex": "x = 1"}, {"dong": 1, "latex": "x = 3"}], CELLS_A, kl))
    assert r["ket_qua"] == "DAT", r


def test_sp03_huu_ti_khong_co_cuc_tri():
    cells = [{"hang": "X", "k": 0, "gia_tri": "-3"}, {"hang": "DAU_YPHAY", "k": 0, "gia_tri": "+"},
             {"hang": "DAU_YPHAY", "k": 1, "gia_tri": "||"}, {"hang": "DAU_YPHAY", "k": 2, "gia_tri": "+"}]
    for cd in ("Hàm số không có cực trị", "không có", ""):
        kl = [{"dong": 0, "latex": "(-\\infty;-3) và (-3;+\\infty)", "loai": "DONG_BIEN"},
              {"dong": 1, "latex": "không có", "loai": "NGHICH_BIEN"},
              {"dong": 2, "latex": cd, "loai": "CUC_DAI"},
              {"dong": 3, "latex": cd, "loai": "CUC_TIEU"}]
        r = grade(_payload("x/(x+3)", "\\mathbb{R}\\setminus\\{-3\\}", "\\frac{3}{(x+3)^{2}}", [{"dong": 0, "latex": "không có nghiệm"}], cells, kl))
        assert r["ket_qua"] == "DAT", (cd, r)


def test_sp04_frac_long_nhau_dung():
    for dh in ("\\frac{3}{(x+3)^{2}}", "\\frac{3}{\\left(x+3\\right)^{2}}", "\\dfrac{3}{x^{2}+6x+9}"):
        r = grade(_payload("x/(x+3)", "\\mathbb{R}\\setminus\\{-3\\}", dh, [], [], [], nop="B.DH.DAOHAM"))
        assert r["ket_qua"] == "DAT", (dh, r)


def test_sp04_khong_xd_khong_can_x_bang():
    r = grade({"ham": "abs(x)", "nop_toi": "B.DH.NGHIEM", "cac_buoc": [
        {"ma_buoc": "B.DH.TXD", "cac_dong": [{"dong": 0, "latex": "\\mathbb{R}"}]},
        {"ma_buoc": "B.DH.DAOHAM", "cac_dong": [{"dong": 0, "latex": "\\frac{x}{|x|}"}]},
        {"ma_buoc": "B.DH.NGHIEM", "cac_dong": [{"dong": 0, "latex": "y' không xác định tại 0", "loai": "KHONG_XD"}]},
    ]})
    assert r["ket_qua"] in ("DAT", "KHONG_KIEM_DUOC")
    assert not (r["ket_qua"] == "SAI" and r["buoc_sai"]["ma_buoc"] == "B.DH.NGHIEM")
