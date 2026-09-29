# -*- coding: utf-8 -*-
"""Bản vá Kiểm định 0002d (Sư phạm 29/09 12:47, thứ tự áp No chốt 12:49: 0002d trước 0002c).

Lớp dấu hiệu code của PR #34 (`_DAU_HIEU_CODE_DONG`) coi mọi tên có '_' là code, nên ký hiệu SGK x_{CT}, x_{CĐ}, x_CT
bị KHONG_KIEM_DUOC (80 ca Sư phạm: SP03-e, SP03-f hỏng vì ô UI cũ "không cuc_dai"). 0002d: cho qua chỉ số chữ CĐ/CD/CT
(hoa/thường, có/không dấu) hoặc số của x / y / f, có hoặc không có {}, cả dạng MathLive \\text{…} / \\mathrm{…}; cho qua
nhãn ô danh sách trắng của hệ. Mọi '_' khác vẫn là dấu hiệu code.
"""
import pytest

import app.grader as G
from app.dau_vao import DAU_VAO_KHONG_HOP_LE, ly_do_tu_choi
from app.grader import grade

VD01 = dict(ham="x**3 - 6*x**2 + 9*x + 2", dh="3x^{2}-12x+9", m=("1", "3"), db="(-\\infty; 1) và (3; +\\infty)", nb="(1; 3)")
B2 = dict(ham="x**3 - 3*x**2 - 9*x + 1", dh="3x^{2}-6x-9", m=("-1", "3"), db="(-\\infty; -1) và (3; +\\infty)", nb="(-1; 3)")


def _payload(b, cd=None, ct=None, khai_bao=("dong_bien", "nghich_bien", "cuc_dai", "cuc_tieu"), dong_kl=None):
    a, c = b["m"]
    cells = [{"hang": "X", "k": 0, "gia_tri": a}, {"hang": "X", "k": 1, "gia_tri": c},
             {"hang": "DAU_YPHAY", "k": 0, "gia_tri": "+"}, {"hang": "DAU_YPHAY", "k": 1, "gia_tri": "0"},
             {"hang": "DAU_YPHAY", "k": 2, "gia_tri": "-"}, {"hang": "DAU_YPHAY", "k": 3, "gia_tri": "0"},
             {"hang": "DAU_YPHAY", "k": 4, "gia_tri": "+"}]
    if dong_kl is None:
        dong_kl = [{"dong": 0, "latex": b["db"], "loai": "DONG_BIEN"}, {"dong": 1, "latex": b["nb"], "loai": "NGHICH_BIEN"},
                   {"dong": 2, "latex": cd if cd is not None else "x = %s" % a, "loai": "CUC_DAI"},
                   {"dong": 3, "latex": ct if ct is not None else "x = %s" % c, "loai": "CUC_TIEU"}]
    return {"ham": b["ham"], "nop_toi": "B.DH.KETLUAN", "cac_buoc": [
        {"ma_buoc": "B.DH.TXD", "cac_dong": [{"dong": 0, "latex": "\\mathbb{R}"}]},
        {"ma_buoc": "B.DH.DAOHAM", "cac_dong": [{"dong": 0, "latex": b["dh"]}]},
        {"ma_buoc": "B.DH.NGHIEM", "cac_dong": [{"dong": 0, "latex": "x = %s" % a}, {"dong": 1, "latex": "x = %s" % c}]},
        {"ma_buoc": "B.DH.XETDAU", "bang": {"loai_bang": "XET_DAU", "cac_o": cells}},
        {"ma_buoc": "B.DH.KETLUAN", "khai_bao": list(khai_bao), "cac_dong": dong_kl}]}


def _khong_bi_chan_code(r):
    return not any((b or {}).get("ly_do") == DAU_VAO_KHONG_HOP_LE for b in (r.get("buoc_khong_kiem_duoc") or [])) \
        and r.get("ly_do") != DAU_VAO_KHONG_HOP_LE


# ---- 7 dòng Sư phạm nêu nguyên văn + dạng MathLive: không bị cổng danh sách trắng hay lớp dấu hiệu code chặn
SU_PHAM_7 = ["x_{CT} = 3", "x_{CĐ} = 1", "x_CT = 3", "x_{CT} = 3, y_{CT} = −26", "y_{CĐ} = 6", "x_1 = 1", "f_{CT} = −26"]
MATHLIVE = ["x_{CT} = 3", "x_{\\text{CT}} = 3", "x_{\\mathrm{CT}} = 3", "x_{\\text{CĐ}} = -1", "y_{CĐ} = 6", "x_{\\mathrm{CD}} = −1",
            "x_{\\text{CT}} = 3, y_{\\text{CT}} = −26", "x_{\\mathrm{CD}} = -1, y_{\\mathrm{CD}} = 6", "x_{cd} = −1", "x_{Ct}=3",
            "x_{2} = 3", "x_2 = 3", "y_CĐ = 6", "x_{CT}=3; y_{CT}=-26"]


@pytest.mark.parametrize("s", SU_PHAM_7 + MATHLIVE)
def test_ky_hieu_sgk_khong_la_dau_hieu_code(s):
    assert ly_do_tu_choi(s) is None, s
    assert not G._chuoi_doc_hai({"ham": "x**3", "cac_buoc": [{"cac_dong": [{"latex": s}]}]}), s


@pytest.mark.parametrize("s", SU_PHAM_7 + MATHLIVE)
def test_ky_hieu_sgk_toi_duoc_bo_cham(s):
    r = grade(_payload(B2, ct=s))
    assert _khong_bi_chan_code(r), (s, r)


# ---- kỳ vọng DAT (giá trị khớp bài)
@pytest.mark.parametrize("b,cd,ct", [
    (VD01, "x_{CĐ} = 1", "x_{CT} = 3"), (VD01, "x_1 = 1", "x_CT = 3"), (VD01, "x = 1, y_{CĐ} = 6", "x = 3, y_{CT} = 2"),
    (VD01, "x_{\\text{CĐ}} = 1", "x_{\\text{CT}} = 3"), (VD01, "x_{\\mathrm{CD}} = 1", "x_{\\mathrm{CT}} = 3"),
    (VD01, "x_{CĐ} = 1, y_{CĐ} = 6", "x_{CT} = 3, y_{CT} = 2"), (VD01, "x_{cđ} = 1", "x_{ct} = 3"),
    (B2, "x_{CĐ} = −1", "x_{CT} = 3, y_{CT} = −26"), (B2, "x_{CĐ} = -1, y_{CĐ} = 6", "x_{CT} = 3, y_{CT} = -26"),
    (B2, "x_{\\mathrm{CD}} = −1", "x = 3; f_{CT} = −26"), (B2, "x_{\\text{CĐ}} = -1", "x_{\\text{CT}} = 3, y_{\\text{CT}} = −26"),
    (B2, "x_{\\mathrm{CD}} = -1, y_{\\mathrm{CD}} = 6", "x_{CT}=3; y_{CT}=-26"), (B2, "x_1 = −1", "x_2 = 3"),
])
def test_chi_so_sgk_DAT(b, cd, ct):
    r = grade(_payload(b, cd=cd, ct=ct))
    assert r["ket_qua"] == "DAT", (cd, ct, r)


def test_gia_tri_sai_van_SAI():
    r = grade(_payload(VD01, ct="x_{CT} = 2"))
    assert r["ket_qua"] == "SAI", r
    r = grade(_payload(B2, ct="x_{CT} = 3, y_{CT} = −25"))
    assert r["ket_qua"] == "SAI", r


# ---- dạng SP03-e / SP03-f: đề chỉ hỏi đơn điệu, UI cũ gửi 2 ô cực trị trống "không cuc_dai", "không cuc_tieu"
@pytest.mark.parametrize("db,nb", [("đồng biến trên (-\\infty; 1) và (3; +\\infty)", "nghịch biến trên (1; 3)"),
                                   ("(-\\infty; 1) và (3; +\\infty)", "(1; 3)")])
def test_SP03_ef_o_trong_ui_cu_DAT(db, nb):
    dong = [{"dong": 0, "latex": db}, {"dong": 1, "latex": nb}, {"dong": 2, "latex": "không cuc_dai"}, {"dong": 3, "latex": "không cuc_tieu"}]
    r = grade(_payload(VD01, khai_bao=("dong_bien", "nghich_bien"), dong_kl=dong))
    assert r["ket_qua"] == "DAT", r


# ---- mọi '_' khác vẫn là dấu hiệu code -> KHONG_KIEM_DUOC, DAU_VAO_KHONG_HOP_LE
@pytest.mark.parametrize("s", ["x_y = 3", "__import__('os')", "os_system = 3", "x_{CT}_{1} = 3", "g_{1} = 3", "cuc_dai_x.subs(x, 3)",
                               "x__1 = 3", "x_123 = 3", "a_b_c = 3", "x_{CT} = 3, lambda_ = 1", "y_{CĐ} = __class__", "cuc_dai_hack = 3"])
def test_gach_duoi_la_van_bi_chan(s):
    r = grade(_payload(B2, ct=s))
    assert r["ket_qua"] == "KHONG_KIEM_DUOC", (s, r)
    assert not _khong_bi_chan_code(r), (s, r)


# ---- chỉ số số chỉ được nhận ở vị trí nhãn; "x_1 + 1" (ca M20 bộ độc hại khóa) vẫn bị chặn ở mọi ô
@pytest.mark.parametrize("s", ["x_1 + 1", "x = x_1 + 1", "x_{1} + 1", "x_2*x", "x = x_{2}"])
def test_chi_so_so_ngoai_vi_tri_nhan_bi_chan(s):
    r = grade(_payload(B2, ct=s))
    assert r["ket_qua"] == "KHONG_KIEM_DUOC", (s, r)
