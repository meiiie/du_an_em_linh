# -*- coding: utf-8 -*-
"""Đo: sympy.parsing.latex.parse_latex (backend antlr, KHÔNG có bộ chuẩn hóa) đọc đúng bao nhiêu mẫu trong latex-mau.json."""
import json, os, signal
import sympy
from sympy import *
from sympy.parsing.latex import parse_latex
from sympy.core.relational import Relational
GOC = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
x = Symbol('x', real=True); k = Symbol('k', integer=True)
L = dict(x=x, k=k)


def doi_ten(e):
    try:
        return e.xreplace({s: L.get(s.name, s) for s in e.free_symbols if isinstance(s, Symbol)})
    except Exception:
        return e


def bang(a, b):
    try:
        if isinstance(a, Set) or isinstance(b, Set):
            return a == b
        if isinstance(a, Relational) and isinstance(b, Relational):
            return type(a) == type(b) and simplify((a.lhs - a.rhs) - (b.lhs - b.rhs)) == 0
        if isinstance(a, Integral) or isinstance(b, Integral):
            return simplify(a.doit() - b.doit()) == 0
        return simplify(a - b) == 0
    except Exception:
        return False


def main():
    d = json.load(open(os.path.join(GOC, 'bo-de-kiem-thu', 'latex-mau.json'), encoding='utf-8'))
    ds = []
    for m in d['muc']:
        mong = eval(m['sympy_mong_doi'], {**sympy.__dict__, **L})
        try:
            r = doi_ten(parse_latex(m['latex_goc']))
            ok = bang(r, mong)
            ds.append(dict(id=m['id'], latex=m['latex_goc'], parse_latex=str(r), mong_doi=str(mong), dung=bool(ok)))
        except Exception as ex:
            ds.append(dict(id=m['id'], latex=m['latex_goc'], parse_latex='LỖI: %s' % type(ex).__name__, mong_doi=str(mong), dung=False))
    n = sum(x_['dung'] for x_ in ds)
    out = dict(phien_ban_sympy=sympy.__version__, backend='antlr4-python3-runtime 4.11.1', so_mau=len(ds), dung=n,
               ti_le=round(n / len(ds), 4), chi_tiet=ds)
    json.dump(out, open(os.path.join(GOC, 'ket-qua', 'ket-qua-parse-latex.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
    print(n, '/', len(ds))
    for x_ in ds:
        print(x_['id'], x_['dung'], x_['latex'], '->', x_['parse_latex'])


main()
