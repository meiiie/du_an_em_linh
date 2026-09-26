# -*- coding: utf-8 -*-
"""Dựng lời giải chuẩn cấu trúc 5 bước từ hàm số, bằng chính các hàm của bộ kiểm Tầng 1.

Lời giải này chỉ nằm trong dịch vụ chấm / sinh bài. Gia sư không được đọc.
"""
from sympy import EmptySet, Interval, oo, simplify, latex

from app.paths import load_kiem

K = load_kiem()


def _num(v):
    if v == oo:
        return "+oo"
    if v == -oo:
        return "-oo"
    s = str(simplify(v))
    return s


def _fmt_interval(I):
    lo = "(" if I.left_open else "["
    ro = ")" if I.right_open else "]"
    return "%s%s; %s%s" % (lo, _num(I.start), _num(I.end), ro)


def fmt_domain(D):
    from sympy import Reals, Complement, Union, Interval, FiniteSet
    if D == Reals:
        return "R"
    if isinstance(D, Complement):
        base, removed = D.args
        if base == Reals and isinstance(removed, FiniteSet):
            inside = ", ".join(_num(p) for p in sorted(removed, key=lambda v: float(v)))
            return "R \\ {%s}" % inside
        if base == Reals:
            return "R \\ %s" % fmt_domain(removed)
    parts = D.args if isinstance(D, Union) else (D,)
    out = []
    for p in parts:
        if isinstance(p, Interval):
            out.append(_fmt_interval(p))
        elif isinstance(p, FiniteSet):
            out.append("{%s}" % ", ".join(_num(v) for v in p))
        else:
            return None
    return " U ".join(out)


def _open_pair(l, r):
    return "(%s; %s)" % (_num(l), _num(r))


def phan_loai_diem(ham):
    """Trả (f, D, fp, khong0, khongxd, dung_x) theo đúng luật kiem_5_buoc, hoặc None."""
    from sympy import limit
    f = K.P(ham)
    cf = K.dieu_kien(f)
    D = K.mien_xd(cf)
    if D is None:
        return None
    pt = K.phan_tich_dau(f)
    if pt is None:
        return None
    fp = pt["fp"]
    gay = K.diem_gay(f)
    khong0, khongxd = [], []
    for p in pt["pts"]:
        if K.gia_tri(f, {K.x: p}, cf) is None:
            continue
        if any(K.la_khong(p - q) for q in gay):
            try:
                l_, r_ = limit(fp, K.x, p, "-"), limit(fp, K.x, p, "+")
            except Exception:
                return None
            if not K.la_khong(l_ - r_):
                khongxd.append(p)
                continue
            if K.la_khong(l_):
                khong0.append(p)
            continue
        v = K.gia_tri(fp, {K.x: p})
        if v is None:
            khongxd.append(p)
        elif abs(v) <= K.EPS:
            khong0.append(p)
    from sympy import FiniteSet
    bd = D.boundary
    dung_x = [p for p in pt["pts"] if K.gia_tri(f, {K.x: p}, cf) is not None or (isinstance(bd, FiniteSet) and K._thuoc(p, list(bd)))]
    dung_x = sorted(dung_x, key=lambda v: float(v))
    return dict(f=f, D=D, cf=cf, pt=pt, fp=fp, khong0=khong0, khongxd=khongxd, dung_x=dung_x)


def _dau_khoang(pt, l, r):
    """Dấu y' trên (l; r) nếu không đổi; None nếu trộn."""
    hop = Interval.open(l, r)
    ph = [d for d in pt["doan"] if Interval.open(d["l"], d["r"]).intersect(hop) != EmptySet]
    if not ph:
        return None
    ds = set(d["dau"] if d["trong_D"] else "||" for d in ph)
    if len(ds) != 1:
        return None
    d0 = next(iter(ds))
    if d0 == 1:
        return "+"
    if d0 == -1:
        return "-"
    if d0 in ("||", None):
        return "||"
    if d0 == 0:
        return "0"
    return None


def bai_lam_may(ham):
    info = phan_loai_diem(ham)
    if info is None:
        return None
    txd = fmt_domain(info["D"])
    if not txd:
        return None
    fp = info["fp"]
    dao = str(simplify(fp))
    moc_pts = info["dung_x"]
    moc = [-oo] + moc_pts + [oo]
    dau = []
    chieu = []
    for l, r in zip(moc[:-1], moc[1:]):
        d = _dau_khoang(info["pt"], l, r)
        if d is None:
            return None
        dau.append(d)
        chieu.append({"+": "tang", "-": "giam", "||": "khong_xd", "0": "khong_xd"}[d])
    dau_tai = []
    for p in moc_pts:
        if K.gia_tri(info["f"], {K.x: p}, info["cf"]) is None or K._thuoc(p, info["khongxd"]):
            dau_tai.append("||")
        elif K._thuoc(p, info["khong0"]):
            dau_tai.append("0")
        else:
            dau_tai.append("||")
    kq = K.khoang_don_dieu_may(info["pt"])
    cd, ct = K.cuc_tri_may(info["pt"])
    f = info["f"]
    kl = {
        "dong_bien": [_open_pair(l, r) for l, r in kq[1]],
        "nghich_bien": [_open_pair(l, r) for l, r in kq[-1]],
        "cuc_dai_x": [_num(p) for p in cd],
        "cuc_tieu_x": [_num(p) for p in ct],
        "gia_tri_cuc_dai": [_num(simplify(f.subs(K.x, p))) for p in cd],
        "gia_tri_cuc_tieu": [_num(simplify(f.subs(K.x, p))) for p in ct],
    }
    bang = {
        "moc": [_num(m) if m not in (oo, -oo) else ("+oo" if m == oo else "-oo") for m in moc],
        "dau": dau,
        "dau_tai_diem": dau_tai,
        "chieu": chieu,
    }
    return {
        "ham": ham,
        "TXD": txd,
        "dao_ham": dao,
        "y_phay_bang_0": [_num(p) for p in info["khong0"]],
        "y_phay_khong_xd": [_num(p) for p in info["khongxd"]],
        "bang": bang,
        "ket_luan": kl,
    }


def latex_ham(ham):
    try:
        return latex(K.P(ham))
    except Exception:
        return ham


def su_kien_bao_ve(bl):
    """Sự kiện để bộ lọc so SymPy. Định dạng khớp loc.py: (loai, chuỗi)."""
    sk = []
    kl = bl.get("ket_luan") or {}
    for s in kl.get("dong_bien") or []:
        sk.append(["DB", _khoang_loc(s)])
    for s in kl.get("nghich_bien") or []:
        sk.append(["NB", _khoang_loc(s)])
    for s in kl.get("cuc_dai_x") or []:
        sk.append(["DCD", str(s).replace("+", "")])
    for s in kl.get("cuc_tieu_x") or []:
        sk.append(["DCT", str(s).replace("+", "")])
    for s in kl.get("gia_tri_cuc_dai") or []:
        sk.append(["GTCD", str(s)])
    for s in kl.get("gia_tri_cuc_tieu") or []:
        sk.append(["GTCT", str(s)])
    for s in bl.get("y_phay_bang_0") or []:
        sk.append(["NGHIEM", str(s)])
    for s in bl.get("y_phay_khong_xd") or []:
        sk.append(["NGHIEM", str(s)])
    return sk


def _khoang_loc(s):
    t = str(s).replace(" ", "")
    # loc.parse_khoang cần dạng (a;b) không khoảng trắng, +oo được num() hiểu
    t = t.replace("+oo", "oo")
    return t
