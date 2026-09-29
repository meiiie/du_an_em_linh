# -*- coding: utf-8 -*-
"""Độ phủ sự kiện bảo vệ: mỗi kết luận của đáp án phải có một sự kiện trong su_kien.

Tập kết luận suy ĐỘC LẬP bằng SymPy (không dùng app.machine): khoảng đồng biến, khoảng nghịch biến, điểm và giá trị cực đại,
điểm và giá trị cực tiểu, không có cực trị / không có cực đại / không có cực tiểu, luôn đồng biến / luôn nghịch biến trên từng
khoảng, nghiệm y' = 0 (hoặc không có nghiệm). So với su_kien do job "solve" / "generate" tạo (app.machine.su_kien_bao_ve).
Tên loại theo main 4e4af19: luôn đồng biến = KHONG_NGHICH_BIEN, luôn nghịch biến = KHONG_DONG_BIEN.
Có bài cực trị VÔ TỈ (1 ± √2, ±√2): sự kiện phải giữ nguyên giá trị (trước bản vá: "1  sqrt(2)").
"""
import json
import os

import pytest
import sympy as sp

from app.generator import _DANG
from app.machine import bai_lam_may, su_kien_bao_ve

X = sp.Symbol("x", real=True)
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))


def _cac_bai():
    ds = []
    # bài trong kho Sư phạm mà seed.ts gọi "solve" (bỏ bài có tham số m, như seed.ts)
    p = os.path.join(ROOT, "data", "supham", "03-vi-du-bai-tap.json")
    if os.path.exists(p):
        for e in json.load(open(p, encoding="utf-8")):
            h = e["de_bai"].get("ham_so_sympy")
            if h and "m" not in h:
                ds.append(("kho:" + e["id"], h))
    # bài seed.ts gọi trực tiếp
    ds += [("seed:x**2", "x**2"), ("seed:x**3-3x", "x**3 - 3*x")]
    # bài sinh (generator), mọi dạng, seed 1..12 (seed.ts dùng 11, 7, 5)
    import random
    for dang, fn in sorted(_DANG.items()):
        for seed in range(1, 13):
            ham = fn(random.Random(seed))[0]
            ds.append(("gen:%s:%d" % (dang, seed), ham))
    # bài trong kho Kiểm định (bộ lọc I1–I3, bộ 5 bước) và ca báo lỗi x/(x+3)
    ds += [("kd:I1", "x**3 - 3*x**2 + 2"), ("kd:I2", "x**3 - 6*x**2 + 9*x"), ("kd:I3", "-x**3 + 3*x + 1"),
           ("kd:x/(x+3)", "x/(x+3)"), ("kd:x^3+x", "x**3 + x"), ("kd:(x+1)/(x-1)", "(x+1)/(x-1)"),
           ("kd:-x^4+2x^2", "-x**4 + 2*x**2"), ("kd:x^3-3x^2+3x+1", "x**3 - 3*x**2 + 3*x + 1"), ("kd:x^4-4x^3", "x**4 - 4*x**3"),
           ("kd:(x^2+1)/x", "(x**2 + 1)/x"), ("kd:-x^3", "-x**3"),
           # cực trị vô tỉ (Sư phạm 11:54)
           ("vt:x^3-3x^2-3x+1", "x**3 - 3*x**2 - 3*x + 1"), ("vt:-x^3+3x^2+3x", "-x**3 + 3*x**2 + 3*x"),
           ("vt:x^4-4x^2+1", "x**4 - 4*x**2 + 1"), ("vt:x^3-6x", "x**3 - 6*x"), ("vt:2x^3-3x^2-6x", "2*x**3 - 3*x**2 - 6*x")]
    seen, out = set(), []
    for ten, h in ds:
        k = str(sp.simplify(sp.sympify(h, locals={"x": X})))
        if k not in seen:
            seen.add(k)
            out.append((ten, h))
    return out


def ket_luan_sympy(ham):
    """Tập kết luận (loai, giá trị SymPy | None) suy trực tiếp bằng SymPy."""
    f = sp.sympify(ham, locals={"x": X})
    fp = sp.together(sp.diff(f, X))
    num, den = sp.fraction(fp)
    ngoai = sorted({r for r in sp.solve(sp.fraction(sp.together(f))[1], X) if r.is_real})
    ng = sorted({r for r in sp.solve(num, X) if r.is_real and r not in ngoai})
    moc = sorted(set(ng) | set(ngoai))
    diem = [-sp.oo] + moc + [sp.oo]
    dau = []
    for a, b in zip(diem[:-1], diem[1:]):
        t = (a + b) / 2 if a.is_finite and b.is_finite else (b - 1 if a == -sp.oo and b.is_finite else (a + 1 if a.is_finite else 0))
        dau.append(sp.sign(fp.subs(X, t)))
    # gộp hai khoảng cùng dấu nếu điểm giữa thuộc TXĐ (y' = 0 không đổi dấu)
    khoang = []
    for i, d in enumerate(dau):
        a, b = diem[i], diem[i + 1]
        if khoang and khoang[-1][2] == d and a not in ngoai:
            khoang[-1] = (khoang[-1][0], b, d)
        else:
            khoang.append((a, b, d))
    kl = set()
    for a, b, d in khoang:
        kl.add(("DB" if d > 0 else "NB", (a, b)))
    cd, ct = [], []
    for i, p in enumerate(moc):
        if p in ngoai:
            continue
        if dau[i] > 0 > dau[i + 1]:
            cd.append(p)
        elif dau[i] < 0 < dau[i + 1]:
            ct.append(p)
    for p in cd:
        kl |= {("DCD", p), ("GTCD", sp.simplify(f.subs(X, p)))}
    for p in ct:
        kl |= {("DCT", p), ("GTCT", sp.simplify(f.subs(X, p)))}
    if not cd and not ct:
        kl.add(("KHONG_CUC_TRI", None))
    elif not cd:
        kl.add(("KHONG_CUC_DAI", None))
    elif not ct:
        kl.add(("KHONG_CUC_TIEU", None))
    loai = {d for _, _, d in khoang}
    if loai == {1}:
        kl.add(("KHONG_NGHICH_BIEN", None))
    if loai == {-1}:
        kl.add(("KHONG_DONG_BIEN", None))
    for p in ng:
        kl.add(("NGHIEM", p))
    if not ng:
        kl.add(("KHONG_NGHIEM", None))
    return kl


def _so(t):
    t = t.strip()
    if t in ("oo", "+oo"):
        return sp.oo
    if t == "-oo":
        return -sp.oo
    try:
        return sp.nsimplify(sp.sympify(t, locals={"x": X}))   # hiểu sqrt: "1+sqrt(2)"
    except Exception:
        return "KHONG_DOC_DUOC:" + t   # sự kiện hỏng (vd "1  sqrt(2)") -> tính là thiếu bảo vệ, không làm test ngã


def _gia_tri(loai, v):
    if loai in ("DB", "NB"):
        a, b = str(v).strip()[1:-1].split(";")
        return (_so(a), _so(b))
    if v in (None, ""):
        return None
    return _so(str(v))


def thieu_bao_ve(ham):
    bl = bai_lam_may(ham)
    if bl is None:
        return None
    sk = {(a, _gia_tri(a, b)) for a, b in su_kien_bao_ve(bl)}
    return sorted([k for k in ket_luan_sympy(ham) if k not in sk], key=str)


BAI = _cac_bai()


def test_do_phu_tong_hop():
    so_bai, so_kl, thieu, bo_qua = 0, 0, [], []
    for ten, h in BAI:
        t = thieu_bao_ve(h)
        if t is None:
            bo_qua.append(ten)
            continue
        so_bai += 1
        so_kl += len(ket_luan_sympy(h))
        thieu += [(ten, k) for k in t]
    print("DO_PHU so_bai=%d so_ket_luan=%d thieu=%d bo_qua=%d" % (so_bai, so_kl, len(thieu), len(bo_qua)))
    for x in thieu:
        print("THIEU", x)
    assert so_bai > 0
    assert thieu == []


@pytest.mark.parametrize("ten,ham", BAI, ids=[t for t, _ in BAI])
def test_do_phu_tung_bai(ten, ham):
    t = thieu_bao_ve(ham)
    if t is None:
        pytest.skip("bai_lam_may không dựng được lời giải")
    assert t == [], t
