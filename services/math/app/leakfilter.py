# -*- coding: utf-8 -*-
"""Bộ lọc đầu ra. Lớp chính: tương đương SymPy (M2 của Kiểm định). Lớp phụ: so chuỗi LaTeX (M1).

Không trả về phần khớp — chỉ cho_phep, để gia sư không nhận lại đáp án.
"""
from app.paths import load_loc

_loc = None


def _L():
    global _loc
    if _loc is None:
        _loc = load_loc()
    return _loc


def loc_ban_nhap(ban_nhap, su_kien):
    L = _L()
    bai = {"su_kien": [(a, b) if not isinstance(a, (list, tuple)) else (a[0] if False else a, b) for a, b in _pairs(su_kien)]}
    # chuẩn hoá cặp
    bai = {"su_kien": _pairs(su_kien)}
    try:
        sympy_hits = L.m23(ban_nhap, bai, False)
    except Exception:
        sympy_hits = []
    try:
        chuoi_hits = L.m1(ban_nhap, bai)
    except Exception:
        chuoi_hits = []
    chan = bool(sympy_hits or chuoi_hits)
    return {"cho_phep": (not chan), "lop_chinh": "sympy", "lop_phu": "chuoi"}


def _pairs(su_kien):
    out = []
    for item in su_kien or []:
        if isinstance(item, dict):
            out.append((item["loai"], item["gia_tri"]))
        else:
            out.append((item[0], item[1]))
    return out
