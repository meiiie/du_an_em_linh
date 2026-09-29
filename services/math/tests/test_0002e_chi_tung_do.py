# -*- coding: utf-8 -*-
"""Bản vá Kiểm định 0002e (Sư phạm chốt 29/09 13:10; áp SAU 0002d -> 0002c).

Ô cực đại / cực tiểu chỉ ghi tung độ không còn bị KHONG_KIEM_DUOC khi đọc được:
- tung độ đúng, thiếu hoành độ (y_{CĐ} = 6, f_{CT} = −26)      -> SAI, SAI_KET_LUAN, ERR.DH.11, câu nhắn hỏi điểm, không nhắc giá trị;
- tung độ sai, có hay không có hoành độ                        -> SAI, SAI_GIA_TRI, ERR.DH.22;
- đủ hoành độ và tung độ đúng (x_{CĐ} = 1, y_{CĐ} = 6 / tại x = 1, y = 6) -> DAT.
Hoành độ đúng, thiếu tung độ: giữ hành vi cũ (DAT). Ngoại lệ "đề chỉ hỏi giá trị cực đại" để sau (khai_bao chưa có).
"""
import sys, os

import pytest

sys.path.insert(0, os.path.dirname(__file__))
from test_0002d_chi_so_sgk import B2, VD01, _khong_bi_chan_code, _payload  # noqa: E402

from app.grader import grade  # noqa: E402

# (bài, ô, chuỗi) — tung độ đúng: B2 CĐ (-1; 6), CT (3; −26); VD01 CĐ (1; 6), CT (3; 2)
Y_DUNG = [
    (B2, "cd", "y_{CĐ} = 6"), (B2, "cd", "y_{CD} = 6"), (B2, "cd", "y_{cđ}=6"), (B2, "cd", "y_CĐ = 6"), (B2, "cd", "y_CD = 6"),
    (B2, "cd", "f_{CĐ} = 6"), (B2, "cd", "f_CD = 6"), (B2, "cd", "y_{\\text{CĐ}} = 6"), (B2, "cd", "y_{\\mathrm{CD}} = 6"),
    (B2, "cd", "y = 6"), (B2, "cd", "giá trị cực đại bằng 6"),
    (B2, "ct", "f_{CT} = −26"), (B2, "ct", "f_{CT} = -26"), (B2, "ct", "y_{CT} = −26"), (B2, "ct", "y_CT = -26"), (B2, "ct", "f_CT = −26"),
    (B2, "ct", "y_{ct} = -26"), (B2, "ct", "y_{\\text{CT}} = −26"), (B2, "ct", "f_{\\mathrm{CT}} = -26"), (B2, "ct", "y = −26"),
    (B2, "ct", "giá trị cực tiểu là −26"),
    (VD01, "cd", "y_{CĐ} = 6"), (VD01, "ct", "y_{CT} = 2"), (VD01, "ct", "f_{\\text{CT}} = 2"),
]
Y_SAI = [
    (B2, "cd", "y_{CĐ} = 5"), (B2, "cd", "y_{CĐ} = −26"), (B2, "cd", "f_{\\text{CĐ}} = -6"), (B2, "ct", "y_{CT} = −25"),
    (B2, "ct", "f_{CT} = 26"), (B2, "ct", "y_{\\mathrm{CT}} = 6"), (VD01, "ct", "y_{CT} = 6"), (VD01, "cd", "y_CD = 2"),
    # có hoành độ đúng, tung độ sai (trước 0002e: SAI_KET_LUAN; nay SAI_GIA_TRI như luật chốt)
    (B2, "ct", "x_{CT} = 3, y_{CT} = −25"), (B2, "cd", "x = -1, y = 0"), (VD01, "cd", "x_{\\text{CĐ}} = 1, y_{\\text{CĐ}} = 2"),
    (B2, "ct", "tại x = 3, y = −20"),
]
DU = [
    (B2, "cd", "x_{CĐ} = -1, y_{CĐ} = 6"), (B2, "cd", "tại x = −1, y = 6"), (B2, "ct", "x_{CT} = 3, y_{CT} = −26"),
    (B2, "ct", "x = 3; f_{CT} = −26"), (B2, "ct", "x_{\\mathrm{CT}} = 3, y_{\\mathrm{CT}} = -26"), (B2, "cd", "f(-1) = 6"),
    (VD01, "cd", "x_{CĐ} = 1, y_{CĐ} = 6"), (VD01, "cd", "tại x = 1, y = 6"), (VD01, "ct", "x_{\\text{CT}} = 3, y_{\\text{CT}} = 2"),
]


def _p(b, o, s):
    return _payload(b, cd=s if o == "cd" else None, ct=s if o == "ct" else None)


@pytest.mark.parametrize("b,o,s", Y_DUNG)
def test_tung_do_dung_thieu_hoanh_do_ERR11(b, o, s):
    r = grade(_p(b, o, s))
    assert _khong_bi_chan_code(r), (s, r)
    assert (r["ket_qua"], r["loai_ket_qua"], r["ma_loi"]) == ("SAI", "SAI_KET_LUAN", "ERR.DH.11"), (s, r)
    assert r["buoc_sai"]["ma_buoc"] == "B.DH.KETLUAN" and r["buoc_sai"]["dong"] == (2 if o == "cd" else 3), (s, r)
    ten = "cực đại" if o == "cd" else "cực tiểu"
    assert "Hàm số đạt %s tại điểm nào?" % ten in r["thong_bao"], r["thong_bao"]
    # không nhắc lại giá trị trong câu nhắn
    assert not any(ch.isdigit() for ch in r["thong_bao"]), r["thong_bao"]
    assert [v["ma_loi"] for v in r["cac_van_de"]] == ["ERR.DH.11"], r


@pytest.mark.parametrize("b,o,s", Y_SAI)
def test_tung_do_sai_ERR22(b, o, s):
    r = grade(_p(b, o, s))
    assert (r["ket_qua"], r["loai_ket_qua"], r["ma_loi"]) == ("SAI", "SAI_GIA_TRI", "ERR.DH.22"), (s, r)
    assert r["buoc_sai"]["ma_buoc"] == "B.DH.KETLUAN" and r["buoc_sai"]["dong"] == (2 if o == "cd" else 3), (s, r)


@pytest.mark.parametrize("b,o,s", DU)
def test_du_hoanh_do_va_tung_do_DAT(b, o, s):
    r = grade(_p(b, o, s))
    assert r["ket_qua"] == "DAT", (s, r)


def test_hoanh_do_dung_thieu_tung_do_giu_hanh_vi_cu_DAT():
    assert grade(_p(B2, "ct", "x_{CT} = 3"))["ket_qua"] == "DAT"
    assert grade(_p(VD01, "cd", "x = 1"))["ket_qua"] == "DAT"


def test_ca_hai_o_chi_tung_do_bao_o_cuc_dai_truoc():
    r = grade(_payload(B2, cd="y_{CĐ} = 6", ct="f_{CT} = −26"))
    assert (r["loai_ket_qua"], r["ma_loi"], r["buoc_sai"]["dong"]) == ("SAI_KET_LUAN", "ERR.DH.11", 2), r
    # tung độ sai được ưu tiên hơn thiếu hoành độ
    r = grade(_payload(B2, cd="y_{CĐ} = 6", ct="f_{CT} = −20"))
    assert (r["loai_ket_qua"], r["ma_loi"], r["buoc_sai"]["dong"]) == ("SAI_GIA_TRI", "ERR.DH.22", 3), r


def test_loi_buoc_truoc_van_uu_tien():
    p = _p(B2, "cd", "y_{CĐ} = 6")
    p["cac_buoc"][1]["cac_dong"][0]["latex"] = "3x^{2}-6x"   # đạo hàm sai
    r = grade(p)
    assert r["ket_qua"] == "SAI" and r["buoc_sai"]["ma_buoc"] == "B.DH.DAOHAM", r


# ---- hàm nhiều cực trị trùng giá trị: y = x⁴ − 2x² + 2, CĐ (0; 2), CT (−1; 1) và (1; 1)
def _tp(cd, ct):
    cells = [{"hang": "X", "k": i, "gia_tri": v} for i, v in enumerate(["-1", "0", "1"])]
    cells += [{"hang": "DAU_YPHAY", "k": k, "gia_tri": v} for k, v in enumerate(["-", "0", "+", "0", "-", "0", "+"])]
    return {"ham": "x**4 - 2*x**2 + 2", "nop_toi": "B.DH.KETLUAN", "cac_buoc": [
        {"ma_buoc": "B.DH.TXD", "cac_dong": [{"dong": 0, "latex": "\\mathbb{R}"}]},
        {"ma_buoc": "B.DH.DAOHAM", "cac_dong": [{"dong": 0, "latex": "4x^{3}-4x"}]},
        {"ma_buoc": "B.DH.NGHIEM", "cac_dong": [{"dong": i, "latex": "x = %s" % v} for i, v in enumerate(["-1", "0", "1"])]},
        {"ma_buoc": "B.DH.XETDAU", "bang": {"loai_bang": "XET_DAU", "cac_o": cells}},
        {"ma_buoc": "B.DH.KETLUAN", "khai_bao": ["dong_bien", "nghich_bien", "cuc_dai", "cuc_tieu"], "cac_dong": [
            {"dong": 0, "latex": "(-1; 0) và (1; +\\infty)", "loai": "DONG_BIEN"},
            {"dong": 1, "latex": "(-\\infty; -1) và (0; 1)", "loai": "NGHICH_BIEN"},
            {"dong": 2, "latex": cd, "loai": "CUC_DAI"}, {"dong": 3, "latex": ct, "loai": "CUC_TIEU"}]}]}


@pytest.mark.parametrize("cd,ct,ky_vong", [
    ("x = 0, y = 2", "x = -1; x = 1", ("DAT", None)),
    ("x = 0", "x = -1, y = 1; x = 1, y = 1", ("DAT", None)),
    ("x = 0, y = 2", "y_{CT} = 1", ("SAI_KET_LUAN", "ERR.DH.11")),      # hai điểm cực tiểu cùng giá trị 1: ghi một lần là đúng
    ("x = 0, y = 2", "f_{\\text{CT}} = 1", ("SAI_KET_LUAN", "ERR.DH.11")),
    ("y_{CĐ} = 2", "x = -1; x = 1", ("SAI_KET_LUAN", "ERR.DH.11")),
    ("x = 0", "y_{CT} = 0", ("SAI_GIA_TRI", "ERR.DH.22")),
    ("x = 0", "y_{CT} = 2", ("SAI_GIA_TRI", "ERR.DH.22")),               # 2 là giá trị cực ĐẠI, không phải cực tiểu
    ("x = 0", "x = -1, y = 1; x = 1, y = 2", ("SAI_GIA_TRI", "ERR.DH.22")),
])
def test_nhieu_cuc_tri_trung_gia_tri(cd, ct, ky_vong):
    r = grade(_tp(cd, ct))
    if ky_vong[0] == "DAT":
        assert r["ket_qua"] == "DAT", r
    else:
        assert (r["ket_qua"], r["loai_ket_qua"], r["ma_loi"]) == ("SAI",) + ky_vong, (cd, ct, r)


def test_ham_khong_co_cuc_tri_ma_ghi_tung_do_la_sai_ket_luan():
    # y = x/(x+3): không có cực trị; HS ghi y_{CĐ} = 1 -> kết luận sai (có cực đại khi không có), không DAT, không KKD
    p = {"ham": "x/(x+3)", "nop_toi": "B.DH.KETLUAN", "cac_buoc": [
        {"ma_buoc": "B.DH.TXD", "cac_dong": [{"dong": 0, "latex": "\\mathbb{R} \\setminus \\{-3\\}"}]},
        {"ma_buoc": "B.DH.DAOHAM", "cac_dong": [{"dong": 0, "latex": "\\frac{3}{(x+3)^{2}}"}]},
        {"ma_buoc": "B.DH.NGHIEM", "cac_dong": [{"dong": 0, "latex": "y' không xác định tại x = -3", "loai": "KHONG_XD"}]},
        {"ma_buoc": "B.DH.XETDAU", "bang": {"loai_bang": "XET_DAU", "cac_o": [
            {"hang": "X", "k": 0, "gia_tri": "-3"}, {"hang": "DAU_YPHAY", "k": 0, "gia_tri": "+"},
            {"hang": "DAU_YPHAY", "k": 1, "gia_tri": "||"}, {"hang": "DAU_YPHAY", "k": 2, "gia_tri": "+"}]}},
        {"ma_buoc": "B.DH.KETLUAN", "khai_bao": ["dong_bien", "nghich_bien", "cuc_dai", "cuc_tieu"], "cac_dong": [
            {"dong": 0, "latex": "(-\\infty; -3) và (-3; +\\infty)", "loai": "DONG_BIEN"}, {"dong": 1, "latex": "không có", "loai": "NGHICH_BIEN"},
            {"dong": 2, "latex": "y_{CĐ} = 1", "loai": "CUC_DAI"}, {"dong": 3, "latex": "không có", "loai": "CUC_TIEU"}]}]}
    r = grade(p)
    assert r["ket_qua"] == "SAI" and r["loai_ket_qua"] == "SAI_KET_LUAN" and r["buoc_sai"]["ma_buoc"] == "B.DH.KETLUAN", r
