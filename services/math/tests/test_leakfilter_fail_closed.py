# -*- coding: utf-8 -*-
"""Bộ lọc lộ đáp án phải FAIL-CLOSED và đọc được số có 0 đứng đầu.

Ca E01: "Hẹn em buổi 08 nhé" từng làm parse_expr lỗi; lỗi bị nuốt nên câu lộ "(1;3)" lọt qua.
Sự kiện dùng ở đây: bài y = x^3 - 6x^2 + 9x (nghịch biến trên (1;3), cực đại tại x=1, y=4, cực tiểu tại x=3, y=0).
"""
import pytest

import app.leakfilter as lf
from app.paths import load_loc

SU_KIEN = [["NB", "(1;3)"], ["DCD", "1"], ["GTCD", "4"], ["DCT", "3"], ["GTCT", "0"]]


def test_num_bo_so_0_dau():
    L = load_loc()
    assert L.num("08") == 8
    assert L.num("-02") == -2
    assert L.num("0") == 0
    assert L.num("10") == 10
    assert float(L.num("0.5")) == 0.5


@pytest.mark.parametrize("cau", [
    "Hàm nghịch biến trên khoảng từ 1 đến 3. Hẹn em buổi 08 nhé.",
    "Hàm nghịch biến trên (01; 03) em nhé.",
    "Giá trị cực đại là 04.",
])
def test_cau_lo_co_so_0_dau_bi_chan(cau):
    q = lf.loc_ban_nhap(cau, SU_KIEN)
    assert q["cho_phep"] is False
    assert q["ly_do"] == "LO_DAP_AN"


@pytest.mark.parametrize("cau", [
    "Bài 05: em lập bảng xét dấu y' trước nhé.",
    "Hẹn em 07:30 sáng mai học tiếp phần xét dấu.",
])
def test_cau_hop_le_co_so_0_dau_van_qua(cau):
    q = lf.loc_ban_nhap(cau, SU_KIEN)
    assert q["cho_phep"] is True
    assert q["ly_do"] is None


class _LocHong:
    """Giả lập bộ lọc ném lỗi khi kiểm."""

    def __init__(self, ho_m23=True, ho_m1=False):
        self.ho_m23, self.ho_m1 = ho_m23, ho_m1

    def m23(self, t, b, ctx):
        if self.ho_m23:
            raise SyntaxError("gia lap loi parse")
        return []

    def m1(self, t, b):
        if self.ho_m1:
            raise ValueError("gia lap loi so chuoi")
        return []


@pytest.mark.parametrize("ho_m23,ho_m1", [(True, False), (False, True), (True, True)])
def test_loi_khi_kiem_thi_chan(monkeypatch, ho_m23, ho_m1):
    monkeypatch.setattr(lf, "_L", lambda: _LocHong(ho_m23, ho_m1))
    q = lf.loc_ban_nhap("Em thử lại bước 3 nhé.", SU_KIEN)
    assert q["cho_phep"] is False
    assert q["ly_do"] == "LOI_KIEM_TRA"


def test_nap_bo_loc_loi_thi_chan(monkeypatch):
    def hong():
        raise ImportError("khong nap duoc loc.py")
    monkeypatch.setattr(lf, "_L", hong)
    q = lf.loc_ban_nhap("Em thử lại bước 3 nhé.", SU_KIEN)
    assert q["cho_phep"] is False
    assert q["ly_do"] == "LOI_KIEM_TRA"


def test_su_kien_hong_thi_chan():
    q = lf.loc_ban_nhap("Hàm nghịch biến trên (1;3).", [["NB", "(1;3"]])
    assert q["cho_phep"] is False
    assert q["ly_do"] == "LOI_KIEM_TRA"
