# -*- coding: utf-8 -*-
"""Bộ lọc đầu ra.

Lớp chính: SymPy theo ngữ cảnh (M3 của Kiểm định) — số phải dính đồng biến / cực / nghiệm.
Không dùng M2 (chỉ so giá trị): «giảm số mũ đi 1» bị chặn nhầm khi đề có nghiệm 1.
Lớp phụ: so chuỗi LaTeX (M1), ví dụ ``x=1``, ``(1;3)``.

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
    bai = {"su_kien": _pairs(su_kien)}
    try:
        sympy_hits = L.m23(ban_nhap, bai, True)
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
