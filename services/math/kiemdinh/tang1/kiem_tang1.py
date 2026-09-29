# -*- coding: utf-8 -*-
"""Bộ kiểm Tầng 1 (máy tự kiểm) – thử nghiệm giai đoạn 1, dự án Phần mềm học toán với AI.

Nguyên tắc: chỉ dùng SymPy (tính toán ký hiệu + thay số hữu tỉ chính xác + liệt kê).
Không gọi mô hình AI nào. Kết quả: DAT / SAI / KHONG_KIEM_DUOC.
SAI chỉ được trả khi có bằng chứng cụ thể (phản ví dụ, nghiệm bị mất/thừa được thế lại,
khoảng có điểm chứng kiến đã kiểm trực tiếp). Nếu các công cụ mâu thuẫn -> KHONG_KIEM_DUOC.
"""
import random, itertools
from sympy import (Symbol, Integer, Rational, pi, E, oo, zoo, nan, S, sqrt, log, exp, sin, cos,
                   tan, cot, Abs, sign, Pow, Ne, Eq, simplify, diff, integrate, solveset, solve,
                   Reals, Interval, FiniteSet, Union, Complement, EmptySet, ConditionSet,
                   preorder_traversal, logcombine, periodicity, together, fraction, nsimplify,
                   binomial, factorial, expand, Min, Max)
from sympy.logic.boolalg import BooleanTrue, BooleanFalse
# F-01: không import parse_expr ở cấp module; chỉ bộ phân tích an toàn bên dưới được gọi nó.
from sympy.parsing.sympy_parser import standard_transformations, rationalize

x = Symbol('x', real=True)
a = Symbol('a', real=True)
b = Symbol('b', real=True)
k = Symbol('k', integer=True)
C = Symbol('C')
TRANS = standard_transformations + (rationalize,)
LOC = dict(x=x, a=a, b=b, k=k, C=C, pi=pi, E=E, e=E, oo=oo, Abs=Abs, sqrt=sqrt, log=log, ln=log,
           exp=exp, sin=sin, cos=cos, tan=tan, cot=cot, Ne=Ne, Eq=Eq, binomial=binomial,
           factorial=factorial)
EPS = 1e-35


# --- An toàn đầu vào (F-01, 29/09) -------------------------------------------------------------
# parse_expr dùng eval. Chuỗi tới đây có thể đến từ học sinh, nên TRƯỚC khi gọi parse_expr phải kiểm cây cú pháp:
# chỉ cho số, tên (không bắt đầu bằng "_", không phải tên dựng sẵn của Python), phép toán, và lời gọi tới hàm
# toán trong danh sách trắng. Cấm thuộc tính (a.b), chỉ số (a[b]), lambda, comprehension, chuỗi ký tự, từ khoá
# tham số, lũy thừa hằng quá lớn. Vi phạm -> ValueError (tầng trên trả KHONG_KIEM_DUOC, không bao giờ chấm).
import ast as _ast
import os
import builtins as _builtins

DO_DAI_TOI_DA = 400
HAM_CHO_PHEP = frozenset({
    'sqrt', 'Abs', 'log', 'ln', 'exp', 'sin', 'cos', 'tan', 'cot', 'Ne', 'Eq', 'binomial', 'factorial',
    'Integer', 'Rational', 'Float', 'Symbol', 'root', 'cbrt', 'sign', 'Max', 'Min', 'floor', 'ceiling',
    'asin', 'acos', 'atan', 'Interval', 'Union', 'FiniteSet', 'Mod', 'Piecewise',
})
_TEN_CAM = frozenset(n for n in dir(_builtins) if n not in HAM_CHO_PHEP) | {
    'lambda', 'sympy', 'os', 'sys', 'subprocess', 'importlib', 'globals', 'locals', 'vars', 'getattr',
    'setattr', 'delattr', 'eval', 'exec', 'compile', 'open', 'input', 'breakpoint', 'help', 'exit', 'quit',
    'type', 'object', 'dir', 'memoryview', 'bytearray', 'bytes', 'classmethod', 'staticmethod', 'super',
    'property', 'sympify', 'parse_expr', 'lambdify', 'init_printing', 'var', 'symbols', 'preview', 'plot',
}
_NUT_CHO_PHEP = (
    _ast.Expression, _ast.BinOp, _ast.UnaryOp, _ast.Compare, _ast.BoolOp, _ast.Tuple, _ast.Name, _ast.Load,
    _ast.Constant, _ast.Call,
    _ast.Add, _ast.Sub, _ast.Mult, _ast.Div, _ast.Pow, _ast.Mod, _ast.FloorDiv, _ast.USub, _ast.UAdd,
    _ast.Eq, _ast.NotEq, _ast.Lt, _ast.LtE, _ast.Gt, _ast.GtE, _ast.And, _ast.Or,
)


def _co_ten(nut):
    return any(isinstance(n, _ast.Name) for n in _ast.walk(nut))


def kiem_an_toan(s):
    """Ném ValueError nếu chuỗi không phải biểu thức toán an toàn."""
    s = str(s)
    if len(s) > DO_DAI_TOI_DA:
        raise ValueError('biểu thức quá dài')
    if '__' in s or "'" in s or '"' in s or '\\' in s or '[' in s or ']' in s or ';' in s or '@' in s:
        raise ValueError('ký tự không cho phép')
    try:
        cay = _ast.parse(s.strip(), mode='eval')
    except SyntaxError as e:
        raise ValueError('cú pháp không hợp lệ') from e
    for nut in _ast.walk(cay):
        if not isinstance(nut, _NUT_CHO_PHEP):
            raise ValueError('cấu trúc không cho phép: %s' % type(nut).__name__)
        if isinstance(nut, _ast.Name):
            if nut.id.startswith('_') or nut.id in _TEN_CAM or len(nut.id) > 12:
                raise ValueError('tên không cho phép: %s' % nut.id)
        elif isinstance(nut, _ast.Constant):
            v = nut.value
            if isinstance(v, bool) or not isinstance(v, (int, float)):
                raise ValueError('hằng không cho phép')
            if isinstance(v, int) and abs(v) >= 10 ** 15:
                raise ValueError('số quá lớn')
        elif isinstance(nut, _ast.Call):
            if not isinstance(nut.func, _ast.Name) or nut.func.id not in HAM_CHO_PHEP or nut.keywords:
                raise ValueError('lời gọi không cho phép')
        elif isinstance(nut, _ast.BinOp) and isinstance(nut.op, _ast.Pow):
            mu = nut.right
            if isinstance(mu, _ast.UnaryOp):
                mu = mu.operand
            if isinstance(mu, _ast.Constant) and isinstance(mu.value, (int, float)) and abs(mu.value) > 64:
                raise ValueError('số mũ quá lớn')
            if not _co_ten(nut.right) and any(isinstance(n, _ast.BinOp) and isinstance(n.op, _ast.Pow)
                                              for n in _ast.walk(nut.right)):
                raise ValueError('lũy thừa tầng không cho phép')
    return s


# ======================= BỘ PHÂN TÍCH AN TOÀN (F-01, 29/09/2026) =======================
# Chuỗi đầu vào KHÔNG BAO GIỜ được thực thi như code Python tùy ý:
#  1) giới hạn độ dài; 2) danh sách ký tự cho phép; 3) mọi '.' phải thuộc một số thập phân;
#  4) mọi tên phải nằm trong danh sách cho phép; 5) cây cú pháp (ast.parse, KHÔNG thực thi) chỉ gồm
#     số, tên cho phép, + - * / ** (^), so sánh đơn < > <= >=, lời gọi hàm cho phép (không keyword);
#  6) chặn lũy thừa lớn / tháp lũy thừa (9**9**9); 7) parse_expr với local_dict cố định, global_dict tối thiểu,
#     __builtins__ rỗng, KHÔNG dùng lambda_notation / auto_symbol / factorial_notation.
# Bị từ chối -> ném DauVaoKhongHopLe (ly_do = DAU_VAO_KHONG_HOP_LE).
import ast as _ast
import re as _re_at
import sympy as _sp
from sympy.parsing.sympy_parser import (parse_expr as _parse_expr_goc, auto_number as _auto_number,
                                        rationalize as _rationalize, convert_xor as _convert_xor)

DAU_VAO_KHONG_HOP_LE = 'DAU_VAO_KHONG_HOP_LE'
DO_DAI_TOI_DA = 300          # ký tự (chuỗi dài nhất trong mọi bộ ca hiện có: 47)
SO_MU_TOI_DA = 100           # |số mũ hằng| tối đa
DO_SAU_LUY_THUA_TOI_DA = 2   # số tầng ** lồng nhau ở cơ số ((a**b)**c là tối đa)
SO_CHU_SO_TOI_DA = 15        # chữ số tối đa của một hằng số nguyên
_KY_TU_CHO_PHEP = _re_at.compile(r'^[0-9A-Za-z+\-*/^().,<>= ]*$')
_SO_THAP_PHAN = _re_at.compile(r'\d+\.\d*|\.\d+|\d+')
_TEN = _re_at.compile(r'[A-Za-z][A-Za-z0-9]*')
_HAM_CHO_PHEP = {'sin': _sp.sin, 'cos': _sp.cos, 'tan': _sp.tan, 'cot': _sp.cot, 'exp': _sp.exp, 'log': _sp.log,
                 'ln': _sp.log, 'sqrt': _sp.sqrt, 'abs': _sp.Abs, 'Abs': _sp.Abs, 'Ne': _sp.Ne}
_X_AT = _sp.Symbol('x', real=True)
_HANG_CHO_PHEP = {'x': _X_AT, 'pi': _sp.pi, 'E': _sp.E, 'e': _sp.E, 'oo': _sp.oo,
                  # tên các bộ ca Tầng 1 đang dùng (đồng nhất thức a, b; họ nghiệm k; hằng nguyên hàm C)
                  'a': _sp.Symbol('a', real=True), 'b': _sp.Symbol('b', real=True),
                  'k': _sp.Symbol('k', integer=True), 'C': _sp.Symbol('C')}
_GLOBAL_TOI_THIEU = {'Symbol': _sp.Symbol, 'Integer': _sp.Integer, 'Float': _sp.Float, 'Rational': _sp.Rational}
_BIEN_DOI_AN_TOAN = (_auto_number, _rationalize, _convert_xor)


class DauVaoKhongHopLe(ValueError):
    ly_do = DAU_VAO_KHONG_HOP_LE


def _tu_choi(s, vi_sao):
    raise DauVaoKhongHopLe('%s: %s (%r)' % (DAU_VAO_KHONG_HOP_LE, vi_sao, s[:60] if isinstance(s, str) else type(s).__name__))


def _hang_so(n):
    if isinstance(n, _ast.Constant) and type(n.value) in (int, float):
        return n.value
    if isinstance(n, _ast.UnaryOp) and isinstance(n.op, (_ast.USub, _ast.UAdd)):
        v = _hang_so(n.operand)
        return None if v is None else (-v if isinstance(n.op, _ast.USub) else v)
    return None


def _co_pow(n):
    return any(isinstance(m, _ast.BinOp) and isinstance(m.op, _ast.Pow) for m in _ast.walk(n))


def _do_sau_pow(n):
    if isinstance(n, _ast.BinOp) and isinstance(n.op, _ast.Pow):
        return 1 + _do_sau_pow(n.left)
    if isinstance(n, _ast.BinOp):
        return max(_do_sau_pow(n.left), _do_sau_pow(n.right))
    if isinstance(n, _ast.UnaryOp):
        return _do_sau_pow(n.operand)
    if isinstance(n, _ast.Call):
        return max([_do_sau_pow(a_) for a_ in n.args] or [0])
    return 0


def _kiem_cay(n, s, ten_ok):
    if isinstance(n, _ast.Expression):
        return _kiem_cay(n.body, s, ten_ok)
    if isinstance(n, _ast.Constant):
        if type(n.value) not in (int, float):
            _tu_choi(s, 'hằng không phải số')
        if type(n.value) is int and len(str(abs(n.value))) > SO_CHU_SO_TOI_DA:
            _tu_choi(s, 'số quá lớn')
        return
    if isinstance(n, _ast.Name):
        if n.id not in ten_ok:
            _tu_choi(s, 'tên không cho phép: %s' % n.id)
        return
    if isinstance(n, _ast.UnaryOp):
        if not isinstance(n.op, (_ast.USub, _ast.UAdd)):
            _tu_choi(s, 'toán tử một ngôi không cho phép')
        return _kiem_cay(n.operand, s, ten_ok)
    if isinstance(n, _ast.BinOp):
        if not isinstance(n.op, (_ast.Add, _ast.Sub, _ast.Mult, _ast.Div, _ast.Pow)):
            _tu_choi(s, 'toán tử không cho phép')
        if isinstance(n.op, _ast.Pow):
            if _co_pow(n.right):
                _tu_choi(s, 'tháp lũy thừa')
            for m in _ast.walk(n.right):
                v = _hang_so(m) if isinstance(m, (_ast.Constant, _ast.UnaryOp)) else None
                if v is not None and abs(v) > SO_MU_TOI_DA:
                    _tu_choi(s, 'số mũ quá lớn')
            if _do_sau_pow(n) > DO_SAU_LUY_THUA_TOI_DA:
                _tu_choi(s, 'lũy thừa lồng quá sâu')
        _kiem_cay(n.left, s, ten_ok)
        return _kiem_cay(n.right, s, ten_ok)
    if isinstance(n, _ast.Compare):
        if len(n.ops) != 1 or not isinstance(n.ops[0], (_ast.Lt, _ast.Gt, _ast.LtE, _ast.GtE)):
            _tu_choi(s, 'so sánh không cho phép')
        _kiem_cay(n.left, s, ten_ok)
        return _kiem_cay(n.comparators[0], s, ten_ok)
    if isinstance(n, _ast.Call):
        if not isinstance(n.func, _ast.Name) or n.func.id not in _HAM_CHO_PHEP or n.keywords or not (1 <= len(n.args) <= 2):
            _tu_choi(s, 'lời gọi hàm không cho phép')
        for a_ in n.args:
            if isinstance(a_, _ast.Starred):
                _tu_choi(s, 'đối số * không cho phép')
            _kiem_cay(a_, s, ten_ok)
        return
    _tu_choi(s, 'cú pháp không cho phép: %s' % type(n).__name__)


def kiem_chuoi_an_toan(s, them_ten=()):
    """Kiểm chuỗi TRƯỚC khi parse. Trả chuỗi đã đổi ^ -> **; ném DauVaoKhongHopLe nếu bị từ chối."""
    if not isinstance(s, str):
        if isinstance(s, (int, float)) and not isinstance(s, bool):
            s = str(s)
        else:
            _tu_choi(s, 'không phải chuỗi')
    if len(s) > DO_DAI_TOI_DA:
        _tu_choi(s, 'chuỗi quá dài (%d ký tự)' % len(s))
    if not _KY_TU_CHO_PHEP.match(s):
        _tu_choi(s, 'ký tự không cho phép')
    if '.' in _SO_THAP_PHAN.sub('0', s):
        _tu_choi(s, "dấu '.' không thuộc số thập phân")
    ten_ok = set(_HAM_CHO_PHEP) | set(_HANG_CHO_PHEP) | set(them_ten)
    for m in _TEN.finditer(s):
        if m.group(0) not in ten_ok:
            _tu_choi(s, 'tên không cho phép: %s' % m.group(0))
    s2 = s.replace('^', '**')
    try:
        cay = _ast.parse(s2.strip(), mode='eval')   # chỉ dựng cây, không thực thi
    except (SyntaxError, ValueError, RecursionError, MemoryError):
        _tu_choi(s, 'không đúng cú pháp biểu thức')
    _kiem_cay(cay, s, ten_ok)
    return s2


def phan_tich_an_toan(s, them=None):
    """Thay cho parse_expr: chỉ parse sau khi kiem_chuoi_an_toan chấp nhận; tên nằm trong local_dict cố định."""
    them = dict(them or {})
    s2 = kiem_chuoi_an_toan(s, them.keys())
    loc_ = dict(_HANG_CHO_PHEP)
    loc_.update(_HAM_CHO_PHEP)
    loc_.update(them)
    glob_ = dict(_GLOBAL_TOI_THIEU)
    glob_['__builtins__'] = {}
    return _parse_expr_goc(s2, local_dict=loc_, global_dict=glob_, transformations=_BIEN_DOI_AN_TOAN)
# ===================== HẾT BỘ PHÂN TÍCH AN TOÀN =====================


# Ghi các lần từ chối trong một lượt kiểm: kiem()/kiem_5_buoc() đổi kết quả thành KHONG_KIEM_DUOC (ly_do DAU_VAO_KHONG_HOP_LE),
# kể cả khi lỗi bị một khối try bên trong nuốt mất. Không bao giờ DAT khi có chuỗi bị từ chối.
_TU_CHOI = []


def P(s, extra=None):
    """Đọc biểu thức bằng bộ phân tích an toàn (F-01). Thay lời gọi parse_expr cũ ở dòng 34."""
    try:
        return phan_tich_an_toan(s, extra)
    except DauVaoKhongHopLe as ex:
        _TU_CHOI.append(str(ex))
        raise


def P_pt(s):
    """'lhs = rhs' -> (lhs, rhs)"""
    l, r = s.split(' = ')
    return P(l), P(r)


def ket_qua(trang_thai, loai_kiem, chi_tiet, buoc_sai=None, phan_chung=None, bang_chung=None,
            canh_bao=None):
    return dict(trang_thai=trang_thai, loai_kiem=loai_kiem, buoc_sai=buoc_sai, chi_tiet=chi_tiet,
                phan_chung=phan_chung, bang_chung=bang_chung, canh_bao=canh_bao or [])


# ------------------------------------------------------------------ điều kiện xác định
def dieu_kien(expr):
    """Liệt kê điều kiện xác định (trên R) của biểu thức: mẫu ≠ 0, căn bậc chẵn ≥ 0,
    log: đối số > 0 (cơ số > 0, ≠ 1 xuất hiện qua log(b) ở mẫu), tan/cot, lũy thừa mũ thực."""
    conds = []
    for sub in preorder_traversal(expr):
        if isinstance(sub, Pow):
            bb, ee = sub.as_base_exp()
            if ee.is_number:
                if ee.is_negative:
                    conds.append(Ne(bb, 0))
                if ee.is_Rational and not ee.is_integer and ee.q % 2 == 0:
                    conds.append(bb >= 0)
                elif ee.is_number and not ee.is_Rational:
                    conds.append(bb > 0)
            else:
                if not (bb.is_number and bb.is_positive):
                    conds.append(bb > 0)
        elif isinstance(sub, log):
            conds.append(sub.args[0] > 0)
        elif isinstance(sub, tan):
            conds.append(Ne(cos(sub.args[0]), 0))
        elif isinstance(sub, cot):
            conds.append(Ne(sin(sub.args[0]), 0))
    out = []
    for c in conds:
        if c is S.true or isinstance(c, BooleanTrue):
            continue
        out.append(c)
    return out


def _so(v):
    """Giá trị số thực (Float 60 chữ số) của biểu thức số, hoặc None nếu không thực/không hữu hạn."""
    if v is None:
        return None
    if v.has(zoo, nan, oo, -oo):
        return None
    try:
        n = v.evalf(60)
    except Exception:
        return None
    if not n.is_number:
        return None
    re_, im_ = n.as_real_imag()
    if abs(im_) > EPS:
        return None
    if re_.has(zoo, nan, oo, -oo):
        return None
    return re_


def thoa_dk(conds, subs):
    for c in conds:
        if c is S.false or isinstance(c, BooleanFalse):
            return False
        v = c.subs(subs)
        if v is S.true or isinstance(v, BooleanTrue):
            continue
        if v is S.false or isinstance(v, BooleanFalse):
            return False
        d = _so(v.lhs - v.rhs)
        if d is None:
            return False
        if isinstance(v, Ne):
            ok = abs(d) > EPS
        elif v.rel_op == '>':
            ok = d > EPS
        elif v.rel_op == '>=':
            ok = d > -EPS
        elif v.rel_op == '<':
            ok = d < -EPS
        elif v.rel_op == '<=':
            ok = d < EPS
        elif v.rel_op == '==':
            ok = abs(d) <= EPS
        else:
            return False
        if not ok:
            return False
    return True


def gia_tri(expr, subs, conds=None):
    if conds is None:
        conds = dieu_kien(expr)
    if not thoa_dk(conds, subs):
        return None
    return _so(expr.subs(subs))


def bang_nhau(v1, v2):
    return abs(v1 - v2) <= EPS * max(1, abs(v1), abs(v2))


def la_khong(v):
    """v là biểu thức số: có bằng 0 không (thử rút gọn chính xác, dự phòng số 60 chữ số)."""
    try:
        s = simplify(v)
        if s == 0:
            return True
    except Exception:
        pass
    n = _so(v)
    return n is not None and abs(n) <= EPS


def mien_xd(conds, var=x):
    D = Reals
    for c in conds:
        if isinstance(c, Ne):
            Z = solveset(c.lhs - c.rhs, var, Reals)
            if isinstance(Z, ConditionSet):
                return None
            D = Complement(D, Z)
        else:
            T = solveset(c, var, Reals)
            if isinstance(T, ConditionSet):
                return None
            D = D.intersect(T)
    return D


# ------------------------------------------------------------------ lấy mẫu điểm
def diem_mau(n=40, seed=12345):
    rnd = random.Random(seed)
    co_dinh = [Integer(0), Integer(1), Integer(-1), Integer(2), Integer(-2), Rational(1, 2),
               Rational(-1, 2), Integer(3), Integer(-3), Rational(1, 3), Rational(5, 2),
               Rational(-7, 3), Integer(10), Integer(-10), Rational(7, 4)]
    ds = list(co_dinh)
    while len(ds) < n:
        q = rnd.randint(1, 9)
        p = rnd.randint(-45, 45)
        ds.append(Rational(p, q))
    return ds


def mau_nhieu_bien(bien, n=40, seed=12345):
    if len(bien) == 1:
        return [{bien[0]: v} for v in diem_mau(n, seed)]
    rnd = random.Random(seed)
    pts = []
    base = diem_mau(n, seed)
    for i in range(n):
        pts.append({v: base[(i * (j + 3) + j * 7 + rnd.randint(0, n - 1)) % n] for j, v in enumerate(bien)})
    return pts


def so_sanh(E1, E2, bien, mien_conds=(), che_do='ca_hai', n=40, bo_diem=()):
    """So sánh E1, E2 tại các điểm mẫu thỏa mien_conds.
    che_do: 'ca_hai'  – tại mọi điểm thuộc miền, cả hai vế phải xác định và bằng nhau;
            'cung_mien' – điểm nào một vế xác định mà vế kia không -> phản ví dụ khác miền;
            'theo_E1'  – nơi E1 xác định thì E2 phải xác định và bằng E1.
    Trả (None|phan_chung, so_diem_hop_le)."""
    d1, d2 = dieu_kien(E1), dieu_kien(E2)
    hop_le = 0
    for pt in mau_nhieu_bien(bien, n):
        if mien_conds and not thoa_dk(list(mien_conds), pt):
            continue
        if bo_diem and len(bien) == 1 and any(la_khong(pt[bien[0]] - q) for q in bo_diem):
            continue
        v1, v2 = gia_tri(E1, pt, d1), gia_tri(E2, pt, d2)
        diem = {str(kk): str(vv) for kk, vv in pt.items()}
        if che_do == 'ca_hai':
            if v1 is None or v2 is None:
                return dict(diem=diem, ly_do='vế %s không xác định tại điểm thuộc miền đã nêu'
                            % ('trái' if v1 is None else 'phải')), hop_le
        elif che_do == 'cung_mien':
            if (v1 is None) != (v2 is None):
                return dict(diem=diem, ly_do='hai vế khác miền xác định (vế %s không xác định)'
                            % ('trái' if v1 is None else 'phải')), hop_le
            if v1 is None:
                continue
        elif che_do == 'theo_E1':
            if v1 is None:
                continue
            if v2 is None:
                return dict(diem=diem, ly_do='biểu thức của AI không xác định tại điểm mà biểu thức đúng xác định'), hop_le
        hop_le += 1
        if not bang_nhau(v1, v2):
            return dict(diem=diem, gia_tri_1=str(v1.evalf(15)), gia_tri_2=str(v2.evalf(15)),
                        ly_do='giá trị khác nhau'), hop_le
    return None, hop_le


def rut_gon_bang_0(e):
    try:
        return simplify(e) == 0
    except Exception:
        return False


# ------------------------------------------------------------------ 1. đồng nhất thức
def kiem_dong_nhat(trai, phai, mien=None):
    L, R = P(trai), P(phai)
    bien = sorted((L.free_symbols | R.free_symbols), key=str)
    mc = [P(m) for m in (mien or [])]
    ky_hieu = rut_gon_bang_0(L - R)
    pc, n = so_sanh(L, R, bien, mc, 'ca_hai' if mc else 'cung_mien')
    if pc:
        return ket_qua('SAI', 'khong_tuong_duong' if 'giá trị' in pc['ly_do'] else 'sai_mien_xac_dinh',
                       'Hai vế không bằng nhau trên miền' + (' đã nêu' if mc else ' (hoặc khác miền xác định)'),
                       buoc_sai=1, phan_chung=pc, bang_chung='thay_so_huu_ti',
                       canh_bao=['rút gọn ký hiệu cho 0 nhưng thay số mâu thuẫn'] if ky_hieu else None)
    if n < 5:
        return ket_qua('KHONG_KIEM_DUOC', 'khong_du_diem', 'Không đủ điểm hợp lệ để thay số (%d)' % n)
    return ket_qua('DAT', 'dong_nhat', 'Rút gọn hiệu = 0' if ky_hieu else 'Thay số %d điểm hữu tỉ đều bằng nhau (không rút gọn được ký hiệu)' % n,
                   bang_chung='ky_hieu+thay_so' if ky_hieu else 'thay_so_huu_ti')


# ------------------------------------------------------------------ 2. phương trình
def giai_pt(L, R):
    """Tập nghiệm thực (đã lọc ĐKXĐ, đã thế lại) hoặc None nếu không giải được. Trả (tập, phương_pháp)."""
    e = L - R
    conds = dieu_kien(L) + dieu_kien(R)
    thu = [('solveset', e)]
    try:
        thu.append(('solveset+logcombine', logcombine(expand(e * log(2)), force=True)))
    except Exception:
        pass
    for ten, t in thu:
        try:
            Sset = solveset(t, x, Reals)
        except Exception:
            continue
        if isinstance(Sset, FiniteSet):
            return set(r for r in Sset if thoa_dk(conds, {x: r}) and la_khong(e.subs(x, r))), ten
    try:
        ds = solve(e, x)
        return set(r for r in ds if r.is_real is not False and thoa_dk(conds, {x: r}) and la_khong(e.subs(x, r))), 'solve'
    except Exception:
        return None, None


def _tap_str(s):
    return '{' + ', '.join(sorted(str(v) for v in s)) + '}'


def _thuoc(r, tap):
    return any(la_khong(r - t) for t in tap)


def kiem_tap_nghiem_pt(pt, nghiem_ai=None, ho_nghiem_ai=None):
    L, R = P_pt(pt)
    e = L - R
    conds = dieu_kien(L) + dieu_kien(R)
    if ho_nghiem_ai is not None:
        return _kiem_ho_nghiem(e, conds, [P(h) for h in ho_nghiem_ai])
    A = [P(r) for r in nghiem_ai]
    for r in A:
        if not thoa_dk(conds, {x: r}):
            return ket_qua('SAI', 'thua_nghiem_vi_pham_dkxd', 'x = %s vi phạm điều kiện xác định' % r,
                           buoc_sai=1, phan_chung=dict(nghiem=str(r), dieu_kien=[str(c) for c in conds]),
                           bang_chung='the_nghiem_chinh_xac')
        if not la_khong(e.subs(x, r)):
            return ket_qua('SAI', 'thua_nghiem_khong_thoa', 'x = %s không thỏa phương trình' % r,
                           buoc_sai=1, phan_chung=dict(nghiem=str(r), gia_tri_hieu=str(simplify(e.subs(x, r)))),
                           bang_chung='the_nghiem_chinh_xac')
    T, pp = giai_pt(L, R)
    if T is None:
        return ket_qua('KHONG_KIEM_DUOC', 'khong_giai_duoc', 'Các nghiệm AI nêu đều thỏa, nhưng máy không giải được để kiểm đầy đủ nghiệm')
    thieu = [t for t in T if not _thuoc(t, A)]
    canh_bao = ['tính đầy đủ nghiệm dựa trên solve() (không bảo đảm)'] if pp == 'solve' else []
    if thieu:
        return ket_qua('SAI', 'mat_nghiem', 'Máy tìm thấy nghiệm bị bỏ sót: %s' % _tap_str(thieu), buoc_sai=1,
                       phan_chung=dict(nghiem_bi_mat=[str(t) for t in thieu], tap_nghiem_may=_tap_str(T)),
                       bang_chung='giai_lai+the_nguoc (%s)' % pp, canh_bao=canh_bao)
    return ket_qua('DAT', 'tap_nghiem', 'Tập nghiệm khớp %s' % _tap_str(T), bang_chung='giai_lai+the_nguoc (%s)' % pp,
                   canh_bao=canh_bao)


def _kiem_ho_nghiem(e, conds, ho):
    # (a) mọi phần tử của họ (k = -4..4) phải thuộc miền và là nghiệm
    for h in ho:
        for kv in range(-4, 5):
            r = h.subs(k, kv)
            if not thoa_dk(conds, {x: r}):
                return ket_qua('SAI', 'thua_nghiem_vi_pham_dkxd', 'Họ %s chứa x = %s vi phạm ĐKXĐ' % (h, r), buoc_sai=1,
                               phan_chung=dict(ho=str(h), k=kv, x=str(r)), bang_chung='the_nghiem_chinh_xac')
            if not la_khong(e.subs(x, r)):
                return ket_qua('SAI', 'thua_nghiem_khong_thoa', 'Họ %s chứa x = %s không là nghiệm' % (h, r), buoc_sai=1,
                               phan_chung=dict(ho=str(h), k=kv, x=str(r)), bang_chung='the_nghiem_chinh_xac')
    # (b) mọi nghiệm trong cửa sổ [-2π, 2π) phải thuộc một họ
    W = Interval.Ropen(-2 * pi, 2 * pi)
    try:
        Sw = solveset(e, x, W)
    except Exception:
        Sw = None
    if not isinstance(Sw, FiniteSet):
        return ket_qua('KHONG_KIEM_DUOC', 'khong_giai_duoc', 'Các họ nghiệm đều đúng nhưng máy không liệt kê được nghiệm trong một cửa sổ để kiểm đầy đủ')
    T = [r for r in Sw if thoa_dk(conds, {x: r}) and la_khong(e.subs(x, r))]
    thieu = []
    for r in T:
        phu = False
        for h in ho:
            for kk in solve(Eq(h, r), k):
                if simplify(kk).is_integer:
                    phu = True
        if not phu:
            thieu.append(r)
    if thieu:
        return ket_qua('SAI', 'mat_ho_nghiem', 'Nghiệm %s (trong [-2π, 2π)) không thuộc họ nào AI nêu' % _tap_str(thieu), buoc_sai=1,
                       phan_chung=dict(nghiem_bi_mat=[str(t) for t in thieu], nghiem_trong_cua_so=_tap_str(T)),
                       bang_chung='giai_tren_cua_so+the_nguoc')
    return ket_qua('DAT', 'ho_nghiem', 'Họ nghiệm đúng và phủ đủ nghiệm trong [-2π, 2π)', bang_chung='giai_tren_cua_so+the_nguoc',
                   canh_bao=['đầy đủ được kiểm trên cửa sổ [-2π,2π) (giả định chu kỳ ≤ 2π)'])


# ------------------------------------------------------------------ 3. tập hợp / khoảng
_TAP_KY_TU = _re_at.compile(r'^[0-9A-Za-z+\-*/^().,;\[\]{}\\ ∅−]*$')


def parse_tap(s):
    """Đọc tập (TXĐ, tập nghiệm). F-01: kiểm độ dài + ký tự trước; tập sai khuôn -> DauVaoKhongHopLe (KHONG_KIEM_DUOC)."""
    if not isinstance(s, str) or len(s) > DO_DAI_TOI_DA or not _TAP_KY_TU.match(s) or '_' in s:
        _TU_CHOI.append('%s: tập không hợp lệ (%r)' % (DAU_VAO_KHONG_HOP_LE, str(s)[:60]))
        raise DauVaoKhongHopLe(_TU_CHOI[-1])
    try:
        return _parse_tap_loi(s)
    except DauVaoKhongHopLe:
        raise
    except (ValueError, IndexError, TypeError, SyntaxError) as ex:
        _TU_CHOI.append('%s: tập sai khuôn (%r: %s)' % (DAU_VAO_KHONG_HOP_LE, s[:60], type(ex).__name__))
        raise DauVaoKhongHopLe(_TU_CHOI[-1])


def _parse_tap_loi(s):
    s = s.strip().replace('+oo', 'oo').replace('−', '-')
    if s == 'R':
        return Reals
    if s.startswith('R \\'):
        return Complement(Reals, _parse_tap_loi(s[3:].strip()))
    if s in ('{}', '∅'):
        return EmptySet
    phan = []
    for p in s.split(' U '):
        p = p.strip()
        if p.startswith('{'):
            ds = [t for t in p[1:-1].replace(';', ',').split(',') if t.strip()]
            phan.append(FiniteSet(*[P(t) for t in ds]))
        else:
            lo, ro = p[0] == '(', p[-1] == ')'
            u, v = p[1:-1].split(';')
            phan.append(Interval(P(u), P(v), lo, ro))
    return Union(*phan)


def _chung_kien(T):
    """Một điểm thuộc tập T (khác rỗng)."""
    if isinstance(T, FiniteSet):
        return sorted(T, key=lambda v: float(v))[0]
    if isinstance(T, Interval):
        l, r = T.inf, T.sup
        if l == -oo and r == oo:
            return Integer(0)
        if l == -oo:
            return r - 1
        if r == oo:
            return l + 1
        return (l + r) / 2
    if isinstance(T, Union):
        return _chung_kien(T.args[0])
    if isinstance(T, Complement):
        A, B = T.args
        for c in diem_mau(60):
            if A.contains(c) == True and B.contains(c) != True:
                return c
    raise ValueError('không lấy được điểm chứng kiến từ %s' % T)


def _dung_tai(rel, conds, p):
    """Kiểm trực tiếp bất đẳng thức tại p (không qua solveset)."""
    if not thoa_dk(conds, {x: p}):
        return False
    v = rel.subs(x, p)
    if v is S.true or isinstance(v, BooleanTrue):
        return True
    if v is S.false or isinstance(v, BooleanFalse):
        return False
    return thoa_dk([v], {})


def _dau_mut(T):
    pts = set()
    for t in (T.args if isinstance(T, Union) else [T]):
        if isinstance(t, Interval):
            for e_ in (t.inf, t.sup):
                if e_.is_finite:
                    pts.add(e_)
        elif isinstance(t, FiniteSet):
            pts |= set(t)
    return pts


def _so_tap(A, T, dung_tai, ten_ai='AI', loai='tap_nghiem_bpt'):
    """So tập A (AI) với T (máy); dung_tai(p) là kiểm trực tiếp độc lập."""
    hieu = Union(Complement(A, T), Complement(T, A))
    if hieu != EmptySet:
        w = _chung_kien(hieu)
        that = dung_tai(w)
        ai = bool(A.contains(w) == True)
        if that != ai:
            return ket_qua('SAI', 'khac_tap', 'Tập của %s khác tập máy tính; điểm chứng kiến x = %s: %s nói %s, kiểm trực tiếp là %s'
                           % (ten_ai, w, ten_ai, 'thuộc' if ai else 'không thuộc', 'thuộc' if that else 'không thuộc'),
                           buoc_sai=1, phan_chung=dict(x=str(w), tap_ai=str(A), tap_may=str(T)), bang_chung='solveset+kiem_truc_tiep')
        return ket_qua('KHONG_KIEM_DUOC', 'cong_cu_mau_thuan', 'solveset và kiểm trực tiếp mâu thuẫn tại x = %s' % w)
    # đối chứng: thay số + đầu mút
    pts = list(diem_mau(60)) + list(_dau_mut(A)) + list(_dau_mut(T))
    for p in pts:
        if dung_tai(p) != bool(A.contains(p) == True):
            return ket_qua('SAI', 'khac_tap', 'Thay số tại x = %s mâu thuẫn với tập của AI' % p, buoc_sai=1,
                           phan_chung=dict(x=str(p)), bang_chung='kiem_truc_tiep')
    return ket_qua('DAT', loai, 'Tập khớp: %s' % T, bang_chung='solveset+thay_so_%d_diem' % len(pts))


def giai_bpt(rel):
    conds = dieu_kien(rel.lhs) + dieu_kien(rel.rhs)
    D = mien_xd(conds)
    if D is None:
        return None, conds
    try:
        T = solveset(rel, x, Reals)
    except Exception:
        return None, conds
    if isinstance(T, ConditionSet) or T.has(ConditionSet):
        return None, conds
    return T.intersect(D), conds


def kiem_tap_nghiem_bpt(bpt, tap_ai):
    rel = P(bpt)
    A = parse_tap(tap_ai)
    T, conds = giai_bpt(rel)
    if T is None:
        return ket_qua('KHONG_KIEM_DUOC', 'khong_giai_duoc', 'solveset không giải được bất phương trình')
    return _so_tap(A, T, lambda p: _dung_tai(rel, conds, p))


def kiem_tap_xac_dinh(ham, tap_ai):
    f = P(ham)
    conds = dieu_kien(f)
    D = mien_xd(conds)
    if D is None:
        return ket_qua('KHONG_KIEM_DUOC', 'khong_giai_duoc', 'Không tính được tập xác định')
    return _so_tap(parse_tap(tap_ai), D, lambda p: gia_tri(f, {x: p}, conds) is not None, loai='tap_xac_dinh')


def diem_gay(f):
    """Nghiệm của đối số các |.| – nơi f có thể không khả vi (SymPy cho sign(0)=0 nên phải tách riêng)."""
    out = set()
    for s_ in f.atoms(Abs):
        z = solveset(s_.args[0], x, Reals)
        if isinstance(z, FiniteSet):
            out |= set(z)
    return out


# ------------------------------------------------------------------ 4. đạo hàm, nguyên hàm, tích phân
def kiem_dao_ham(ham, dao_ham_ai):
    f, g = P(ham), P(dao_ham_ai)
    fp = diff(f, x)
    ky_hieu = rut_gon_bang_0(fp - g)
    pc, n = so_sanh(fp, g, [x], dieu_kien(f), 'theo_E1', bo_diem=diem_gay(f))
    if pc:
        return ket_qua('SAI', 'dao_ham_sai', "y' của AI khác đạo hàm máy tính: %s" % fp, buoc_sai=1, phan_chung=pc,
                       bang_chung='diff+thay_so_huu_ti')
    if n < 5:
        return ket_qua('KHONG_KIEM_DUOC', 'khong_du_diem', 'Không đủ điểm')
    return ket_qua('DAT', 'dao_ham', 'Khớp diff(f)', bang_chung='ky_hieu+thay_so' if ky_hieu else 'thay_so_huu_ti')


def kiem_nguyen_ham(ham, nguyen_ham_ai):
    f, F = P(ham), P(nguyen_ham_ai)
    if C not in F.free_symbols:
        return ket_qua('SAI', 'thieu_hang_so_C', 'Họ nguyên hàm thiếu hằng số C (quy ước trình bày)', buoc_sai=1,
                       bang_chung='luat_trinh_bay')
    G = F.subs(C, 0)
    dkf, dkG = dieu_kien(f), dieu_kien(G)
    for pt in mau_nhieu_bien([x], 40):
        if gia_tri(f, pt, dkf) is not None and gia_tri(G, pt, dkG) is None:
            return ket_qua('SAI', 'nguyen_ham_sai_mien', 'F không xác định tại x = %s trong khi f xác định' % pt[x], buoc_sai=1,
                           phan_chung=dict(x=str(pt[x])), bang_chung='thay_so_huu_ti')
    dG = diff(G, x)
    ky_hieu = rut_gon_bang_0(dG - f)
    pc, n = so_sanh(f, dG, [x], dkf, 'theo_E1')
    if pc:
        return ket_qua('SAI', 'nguyen_ham_sai', "F'(x) ≠ f(x) (vi phân ngược)", buoc_sai=1, phan_chung=pc,
                       bang_chung='vi_phan_nguoc+thay_so')
    return ket_qua('DAT', 'nguyen_ham', "F' = f (vi phân ngược)", bang_chung='ky_hieu+thay_so' if ky_hieu else 'thay_so_huu_ti')


def kiem_tich_phan(ham, can_duoi, can_tren, gia_tri_ai):
    f, lo, hi, v = P(ham), P(can_duoi), P(can_tren), P(gia_tri_ai)
    D = mien_xd(dieu_kien(f))
    doan = Interval(Min(lo, hi), Max(lo, hi))
    if D is not None and not doan.is_subset(D):
        return ket_qua('SAI', 'ham_khong_xac_dinh_tren_doan',
                       'f không xác định (không liên tục) tại điểm thuộc đoạn %s; không áp dụng Newton–Leibniz, AI vẫn cho giá trị %s' % (doan, v),
                       buoc_sai=1, phan_chung=dict(diem_ngoai_mien=str(Complement(doan, D))), bang_chung='tap_xac_dinh')
    I = integrate(f, (x, lo, hi))
    if I.has(oo, -oo, zoo, nan):
        return ket_qua('SAI', 'tich_phan_phan_ky', 'Tích phân phân kỳ, AI cho %s' % v, buoc_sai=1, bang_chung='integrate')
    ky = la_khong(I - v)
    nq = _so(__import__('sympy').Integral(f, (x, lo, hi)).evalf(30))
    so_ok = nq is not None and abs(float(nq) - float(v)) < 1e-9
    if ky and so_ok:
        return ket_qua('DAT', 'tich_phan', 'Giá trị %s khớp (ký hiệu + tích phân số)' % I, bang_chung='integrate+quad')
    if (not ky) and (not so_ok):
        return ket_qua('SAI', 'tich_phan_sai', 'Máy tính được %s, AI cho %s' % (I, v), buoc_sai=1,
                       phan_chung=dict(gia_tri_may=str(I), gia_tri_so=str(nq)), bang_chung='integrate+quad')
    return ket_qua('KHONG_KIEM_DUOC', 'cong_cu_mau_thuan', 'integrate (%s) và tích phân số (%s) không nhất quán' % (I, nq))


# ------------------------------------------------------------------ 5. chu kỳ, đơn vị, đếm
def kiem_chu_ky(ham, chu_ky_ai):
    f, T = P(ham), P(chu_ky_ai)
    Tm = periodicity(f, x)
    if Tm is None:
        return ket_qua('KHONG_KIEM_DUOC', 'khong_giai_duoc', 'periodicity không xác định được chu kỳ')
    if la_khong(T - Tm):
        return ket_qua('DAT', 'chu_ky', 'Chu kỳ %s' % Tm, bang_chung='periodicity')
    la_chu_ky = rut_gon_bang_0(f.subs(x, x + T) - f)
    return ket_qua('SAI', 'chu_ky_sai', 'Chu kỳ dương nhỏ nhất là %s; %s %s' % (Tm, T, 'là một chu kỳ nhưng không nhỏ nhất' if la_chu_ky else 'không phải chu kỳ'),
                   buoc_sai=1, phan_chung=dict(chu_ky_may=str(Tm)), bang_chung='periodicity')


def kiem_don_vi(trai, phai):
    from sympy.physics import units as u
    from sympy.physics.units import convert_to, Dimension
    from sympy.physics.units.systems.si import dimsys_SI, SI
    UL = dict(meter=u.meter, kilometer=u.kilometer, centimeter=u.centimeter, hour=u.hour, minute=u.minute,
              second=u.second, liter=u.liter, kilogram=u.kilogram, gram=u.gram, newton=u.newton)
    L, R = P(trai, UL), P(phai, UL)
    dL, dR = SI.get_dimensional_expr(L), SI.get_dimensional_expr(R)
    if not dimsys_SI.equivalent_dims(Dimension(dL), Dimension(dR)):
        return ket_qua('SAI', 'sai_thu_nguyen', 'Thứ nguyên hai vế khác nhau: %s vs %s' % (dL, dR), buoc_sai=1,
                       phan_chung=dict(thu_nguyen_trai=str(dL), thu_nguyen_phai=str(dR)), bang_chung='sympy.physics.units')
    base = [u.meter, u.second, u.kilogram]
    ti_so = simplify(convert_to(L, base) / convert_to(R, base))
    if ti_so == 1:
        return ket_qua('DAT', 'don_vi', 'Cùng thứ nguyên, bằng nhau sau khi đổi về SI', bang_chung='sympy.physics.units')
    return ket_qua('SAI', 'sai_gia_tri_doi_don_vi', 'Cùng thứ nguyên nhưng giá trị khác: trái/phải = %s' % ti_so, buoc_sai=1,
                   phan_chung=dict(trai_SI=str(convert_to(L, base)), phai_SI=str(convert_to(R, base))), bang_chung='sympy.physics.units')


# F-01 (29/09): BỎ eval() ở kiem_dem (trước ở dòng 585 và 591). Hai chuỗi khong_gian / su_kien chỉ đến từ bộ ca do người soạn
# (bo-de-kiem-thu/cac-ca.yaml, kieu dem / xac_suat), KHÔNG đến từ học sinh hay AI; vẫn bỏ eval theo yêu cầu F-01.
# Thay bằng bộ đọc có cấu trúc: ast.parse (chỉ dựng cây) + thông dịch một văn phạm nhỏ, không có builtins, không thực thi code.
import ast as _ast_dem
_DEM_TOI_DA = 10 ** 6   # số phần tử không gian mẫu tối đa được liệt kê


def _dem_so_nguyen(n):
    if isinstance(n, _ast_dem.Constant) and type(n.value) is int:
        return n.value
    if isinstance(n, _ast_dem.UnaryOp) and isinstance(n.op, _ast_dem.USub):
        return -_dem_so_nguyen(n.operand)
    raise DauVaoKhongHopLe('%s: khong_gian chỉ nhận số nguyên' % DAU_VAO_KHONG_HOP_LE)


def _dem_day(n):
    """range(a[, b[, c]]) hoặc [số nguyên, ...] -> list."""
    if isinstance(n, (_ast_dem.List, _ast_dem.Tuple)):
        return [_dem_so_nguyen(e) for e in n.elts]
    if isinstance(n, _ast_dem.Call) and isinstance(n.func, _ast_dem.Name) and n.func.id == 'range' and not n.keywords and 1 <= len(n.args) <= 3:
        return list(range(*[_dem_so_nguyen(a_) for a_ in n.args]))
    raise DauVaoKhongHopLe('%s: khong_gian chỉ nhận range(...) hoặc danh sách số nguyên' % DAU_VAO_KHONG_HOP_LE)


def doc_khong_gian(chuoi):
    """product(...)/permutations(...)/combinations(...) trên range/danh sách số nguyên -> list các bộ."""
    kiem = chuoi if isinstance(chuoi, str) and len(chuoi) <= DO_DAI_TOI_DA else None
    if kiem is None or '_' in kiem:
        raise DauVaoKhongHopLe('%s: khong_gian không hợp lệ' % DAU_VAO_KHONG_HOP_LE)
    try:
        n = _ast_dem.parse(kiem.strip(), mode='eval').body
    except SyntaxError:
        raise DauVaoKhongHopLe('%s: khong_gian sai cú pháp' % DAU_VAO_KHONG_HOP_LE)
    if not (isinstance(n, _ast_dem.Call) and isinstance(n.func, _ast_dem.Name) and n.func.id in ('product', 'permutations', 'combinations')):
        raise DauVaoKhongHopLe('%s: khong_gian phải là product/permutations/combinations' % DAU_VAO_KHONG_HOP_LE)
    ten = n.func.id
    kw = {k_.arg: _dem_so_nguyen(k_.value) for k_ in n.keywords}
    if ten == 'product':
        if set(kw) - {'repeat'}:
            raise DauVaoKhongHopLe('%s: product chỉ nhận repeat=' % DAU_VAO_KHONG_HOP_LE)
        days = [_dem_day(a_) for a_ in n.args]
        so = 1
        for d_ in days:
            so *= len(d_)
        so **= kw.get('repeat', 1)
        if so > _DEM_TOI_DA:
            raise DauVaoKhongHopLe('%s: không gian mẫu quá lớn' % DAU_VAO_KHONG_HOP_LE)
        return list(itertools.product(*days, repeat=kw.get('repeat', 1)))
    if kw or not (1 <= len(n.args) <= 2):
        raise DauVaoKhongHopLe('%s: %s(dãy[, r])' % (DAU_VAO_KHONG_HOP_LE, ten))
    d_ = _dem_day(n.args[0])
    r_ = _dem_so_nguyen(n.args[1]) if len(n.args) == 2 else None
    if ten == 'combinations' and r_ is None:
        raise DauVaoKhongHopLe('%s: combinations cần r' % DAU_VAO_KHONG_HOP_LE)
    if len(d_) > 12:
        raise DauVaoKhongHopLe('%s: không gian mẫu quá lớn' % DAU_VAO_KHONG_HOP_LE)
    return list(getattr(itertools, ten)(d_, r_) if r_ is not None else itertools.permutations(d_))


def _dem_bieu_thuc(n, t):
    """Thông dịch biểu thức của su_kien trên bộ t: số nguyên, t[i], sum(t), len(t), + - *, so sánh, and/or/not."""
    B = _ast_dem
    if isinstance(n, B.Constant) and type(n.value) is int:
        return n.value
    if isinstance(n, B.Name) and n.id == 't':
        return t
    if isinstance(n, B.Subscript) and isinstance(n.value, B.Name) and n.value.id == 't':
        return t[_dem_so_nguyen(n.slice)]
    if isinstance(n, B.Call) and isinstance(n.func, B.Name) and n.func.id in ('sum', 'len') and not n.keywords and len(n.args) == 1 \
            and isinstance(n.args[0], B.Name) and n.args[0].id == 't':
        return sum(t) if n.func.id == 'sum' else len(t)
    if isinstance(n, B.UnaryOp) and isinstance(n.op, (B.USub, B.Not)):
        v = _dem_bieu_thuc(n.operand, t)
        return -v if isinstance(n.op, B.USub) else (not v)
    if isinstance(n, B.BinOp) and isinstance(n.op, (B.Add, B.Sub, B.Mult, B.Mod)):
        l_, r_ = _dem_bieu_thuc(n.left, t), _dem_bieu_thuc(n.right, t)
        if not (type(l_) is int and type(r_) is int):
            raise DauVaoKhongHopLe('%s: phép tính chỉ trên số nguyên' % DAU_VAO_KHONG_HOP_LE)
        return {B.Add: l_ + r_, B.Sub: l_ - r_, B.Mult: l_ * r_}.get(type(n.op)) if not isinstance(n.op, B.Mod) else l_ % r_
    if isinstance(n, B.BoolOp):
        vs = [_dem_bieu_thuc(v, t) for v in n.values]
        return all(vs) if isinstance(n.op, B.And) else any(vs)
    if isinstance(n, B.Compare):
        OPS = {B.Eq: lambda p, q: p == q, B.NotEq: lambda p, q: p != q, B.Lt: lambda p, q: p < q, B.LtE: lambda p, q: p <= q,
               B.Gt: lambda p, q: p > q, B.GtE: lambda p, q: p >= q}
        trai = _dem_bieu_thuc(n.left, t)
        for op, c_ in zip(n.ops, n.comparators):
            if type(op) not in OPS:
                raise DauVaoKhongHopLe('%s: so sánh không cho phép' % DAU_VAO_KHONG_HOP_LE)
            phai = _dem_bieu_thuc(c_, t)
            if not OPS[type(op)](trai, phai):
                return False
            trai = phai
        return True
    raise DauVaoKhongHopLe('%s: su_kien có cú pháp không cho phép (%s)' % (DAU_VAO_KHONG_HOP_LE, type(n).__name__))


def doc_su_kien(chuoi):
    """'lambda t: <biểu thức>' -> hàm Python thuần thông dịch cây đã kiểm (không eval)."""
    if not isinstance(chuoi, str) or len(chuoi) > DO_DAI_TOI_DA or '_' in chuoi:
        raise DauVaoKhongHopLe('%s: su_kien không hợp lệ' % DAU_VAO_KHONG_HOP_LE)
    try:
        n = _ast_dem.parse(chuoi.strip(), mode='eval').body
    except SyntaxError:
        raise DauVaoKhongHopLe('%s: su_kien sai cú pháp' % DAU_VAO_KHONG_HOP_LE)
    a_ = getattr(n, 'args', None)
    if not (isinstance(n, _ast_dem.Lambda) and len(a_.args) == 1 and a_.args[0].arg == 't' and not a_.vararg and not a_.kwarg
            and not a_.kwonlyargs and not a_.defaults and not a_.posonlyargs):
        raise DauVaoKhongHopLe('%s: su_kien phải có dạng lambda t: ...' % DAU_VAO_KHONG_HOP_LE)
    than = n.body
    _dem_bieu_thuc(than, (0,) * 16)  # kiểm cú pháp sớm trên một bộ giả (không có tác dụng phụ)
    return lambda t: bool(_dem_bieu_thuc(than, tuple(t)))


def kiem_dem(khong_gian, gia_tri_ai, su_kien=None):
    """Liệt kê không gian mẫu (mô hình hóa do người soạn, độc lập với AI)."""
    omega = doc_khong_gian(khong_gian)
    v = P(gia_tri_ai)
    if su_kien is None:
        dung = Integer(len(omega))
        loai = 'dem'
    else:
        f = doc_su_kien(su_kien)
        dung = Rational(sum(1 for t in omega if f(t)), len(omega))
        loai = 'xac_suat'
    if la_khong(dung - v):
        return ket_qua('DAT', loai, 'Liệt kê: %s' % dung, bang_chung='liet_ke_%d_phan_tu' % len(omega))
    return ket_qua('SAI', loai + '_sai', 'Liệt kê cho %s, AI cho %s' % (dung, v), buoc_sai=1,
                   phan_chung=dict(gia_tri_liet_ke=str(dung)), bang_chung='liet_ke_%d_phan_tu' % len(omega),
                   canh_bao=['phụ thuộc mô hình hóa đề (không gian mẫu) do người soạn'])


# ------------------------------------------------------------------ 6. đơn điệu, cực trị
def _nghiem_thuc(expr):
    try:
        Z = solveset(expr, x, Reals)
    except Exception:
        return None
    if isinstance(Z, FiniteSet):
        return set(Z)
    if Z == EmptySet:
        return set()
    num, den = fraction(together(expr))
    try:
        Z = solveset(num, x, Reals)
    except Exception:
        return None
    return set(Z) if isinstance(Z, FiniteSet) else (set() if Z == EmptySet else None)


def phan_tich_dau(f):
    """Chia R thành các khoảng bởi: nghiệm f', điểm f' không xác định, điểm gãy |.|, biên TXĐ.
    Trả dict hoặc None nếu không phân tích được."""
    cf = dieu_kien(f)
    D = mien_xd(cf)
    if D is None:
        return None
    fp = diff(f, x)
    pts = set()
    Z = _nghiem_thuc(fp)
    if Z is None:
        return None
    pts |= Z
    for c in dieu_kien(fp) + cf:
        z = _nghiem_thuc(c.lhs - c.rhs)
        if z is None:
            return None
        pts |= z
    for s_ in list(fp.atoms(sign)) + list(f.atoms(Abs)):
        z = _nghiem_thuc(s_.args[0])
        if z is None:
            return None
        pts |= z
    pts = sorted([p for p in pts if p.is_real], key=lambda v: float(v))
    moc = [-oo] + pts + [oo]
    doan = []
    for l, r in zip(moc[:-1], moc[1:]):
        if l == -oo and r == oo:
            t = Integer(0)
        elif l == -oo:
            t = r - 1
        elif r == oo:
            t = l + 1
        else:
            t = (l + r) / 2
        trong = Interval.open(l, r).is_subset(D)
        if trong is None:
            trong = bool(D.contains(t) == True)
        dau = None
        if trong:
            v = gia_tri(fp, {x: t})
            if v is None:
                return None
            dau = 1 if v > EPS else (-1 if v < -EPS else 0)
        doan.append(dict(l=l, r=r, t=t, trong_D=bool(trong), dau=dau))
    return dict(f=f, fp=fp, D=D, pts=pts, doan=doan, cf=cf)


def cuc_tri_may(pt_):
    f = pt_['f']
    cd, ct = [], []
    ds = pt_['doan']
    for i, p in enumerate(pt_['pts']):
        trai, phai = ds[i], ds[i + 1]
        if not (trai['trong_D'] and phai['trong_D']):
            continue
        if gia_tri(f, {x: p}, pt_['cf']) is None:
            continue
        if trai['dau'] == 1 and phai['dau'] == -1:
            cd.append(p)
        elif trai['dau'] == -1 and phai['dau'] == 1:
            ct.append(p)
    return cd, ct


def khoang_don_dieu_may(pt_):
    f = pt_['f']
    kq = {1: [], -1: []}
    cur = None
    for i, d in enumerate(pt_['doan']):
        if d['trong_D'] and d['dau'] in (1, -1):
            if cur and cur['dau'] == d['dau'] and cur['r'] == d['l'] and gia_tri(f, {x: d['l']}, pt_['cf']) is not None:
                cur['r'] = d['r']
            else:
                if cur:
                    kq[cur['dau']].append((cur['l'], cur['r']))
                cur = dict(l=d['l'], r=d['r'], dau=d['dau'])
        else:
            if cur:
                kq[cur['dau']].append((cur['l'], cur['r']))
            cur = None
    if cur:
        kq[cur['dau']].append((cur['l'], cur['r']))
    return kq


def _tap_so(ds):
    return set(ds)


def _cung_tap(A, B):
    # so như TẬP HỢP (bỏ trùng lặp) – sửa lỗi lần chạy 1: giá trị cực đại {1, 1} bị so độ dài với {1}
    return all(_thuoc(a_, B) for a_ in A) and all(_thuoc(b_, A) for b_ in B)


def kiem_cuc_tri(ham, cuc_dai_x=None, cuc_tieu_x=None, gia_tri_cuc_dai=None, gia_tri_cuc_tieu=None):
    f = P(ham)
    pt_ = phan_tich_dau(f)
    if pt_ is None:
        return ket_qua('KHONG_KIEM_DUOC', 'khong_phan_tich_duoc', 'Không phân tích được dấu f\'')
    cd, ct = cuc_tri_may(pt_)
    vcd = [simplify(f.subs(x, p)) for p in cd]
    vct = [simplify(f.subs(x, p)) for p in ct]
    so = []
    for ten, ai, may in (('điểm cực đại', cuc_dai_x, cd), ('điểm cực tiểu', cuc_tieu_x, ct),
                         ('giá trị cực đại', gia_tri_cuc_dai, vcd), ('giá trị cực tiểu', gia_tri_cuc_tieu, vct)):
        if ai is None:
            continue
        A = [P(v) for v in ai]
        if not _cung_tap(A, may):
            so.append('%s: AI %s, máy %s' % (ten, _tap_str(A), _tap_str(may)))
    tt = dict(diem_cuc_dai=[str(p) for p in cd], diem_cuc_tieu=[str(p) for p in ct],
              gia_tri_cuc_dai=[str(v) for v in vcd], gia_tri_cuc_tieu=[str(v) for v in vct],
              dao_ham=str(pt_['fp']), bang_dau=[dict(khoang='(%s; %s)' % (d['l'], d['r']), trong_D=d['trong_D'], dau=d['dau']) for d in pt_['doan']])
    if so:
        return ket_qua('SAI', 'cuc_tri_sai', '; '.join(so), buoc_sai=1, phan_chung=tt, bang_chung='xet_dau_fprime_tai_diem_huu_ti')
    return ket_qua('DAT', 'cuc_tri', 'Khớp phân tích dấu f\'', phan_chung=None, bang_chung='xet_dau_fprime_tai_diem_huu_ti')


def _khoang_tu_tap(T):
    out = []
    for t in (T.args if isinstance(T, Union) else [T]):
        if isinstance(t, Interval):
            out.append((t.inf, t.sup))
        else:
            return None
    return out


def kiem_don_dieu(ham, dong_bien=None, nghich_bien=None, dong_bien_tren_tap=None, nghich_bien_tren_tap=None):
    f = P(ham)
    pt_ = phan_tich_dau(f)
    if pt_ is None:
        return ket_qua('KHONG_KIEM_DUOC', 'khong_phan_tich_duoc', 'Không phân tích được dấu f\'')
    kq = khoang_don_dieu_may(pt_)
    D = pt_['D']
    fmt = lambda ds: ', '.join('(%s; %s)' % (l, r) for l, r in ds) or '∅'
    tt = dict(dong_bien_may=fmt(kq[1]), nghich_bien_may=fmt(kq[-1]), dao_ham=str(pt_['fp']), TXD=str(D))
    loi = []
    for chieu, ai in ((1, dong_bien), (-1, nghich_bien)):
        if ai is None:
            continue
        A = []
        for s_ in ai:
            I = parse_tap(s_)
            A.append((I.inf, I.sup))
            if not Interval.open(I.inf, I.sup).is_subset(D):
                loi.append('khoảng %s chứa điểm ngoài TXĐ %s' % (s_, D))
        may = kq[chieu]
        if not (len(A) == len(may) and all(any(la_khong(p[0] - q[0]) if p[0].is_finite else p[0] == q[0] for q in may) and
                                           any((la_khong(p[1] - q[1]) if p[1].is_finite else p[1] == q[1]) and
                                               ((la_khong(p[0] - q[0]) if p[0].is_finite else p[0] == q[0])) for q in may) for p in A)):
            loi.append('%s: AI %s, máy %s' % ('đồng biến' if chieu == 1 else 'nghịch biến', fmt(A), fmt(may)))
    for chieu, s_ in ((1, dong_bien_tren_tap), (-1, nghich_bien_tren_tap)):
        if s_ is None:
            continue
        Sx = parse_tap(s_)
        if not Sx.is_subset(D):
            loi.append('tập %s chứa điểm ngoài TXĐ %s' % (s_, D))
            continue
        # tách thành phần liên thông
        if isinstance(Sx, Complement):
            Sx = Sx.simplify() if hasattr(Sx, 'simplify') else Sx
        comps = _khoang_tu_tap(Sx.intersect(Reals)) if not isinstance(Sx, Complement) else None
        if comps is None:
            comps = _khoang_tu_tap(Union(*[Interval.open(d['l'], d['r']) for d in pt_['doan'] if d['trong_D']]).intersect(Sx))
        if comps is None:
            return ket_qua('KHONG_KIEM_DUOC', 'khong_phan_tich_duoc', 'Không tách được tập %s' % s_)
        # mỗi thành phần phải nằm trong một khoảng đơn điệu đúng chiều
        for (l, r) in comps:
            if not any(Interval(l, r).intersect(Reals).is_subset(Interval(q0, q1)) for q0, q1 in kq[chieu]):
                loi.append('thành phần (%s; %s) không nằm trong khoảng %s biến' % (l, r, 'đồng' if chieu == 1 else 'nghịch'))
        # giữa các thành phần: tìm phản ví dụ thứ tự
        if len(comps) > 1 and not loi:
            diem = []
            for (l, r) in comps:
                for e_ in (l, r):
                    if e_.is_finite:
                        for h in (Rational(1, 10), Rational(1, 100), Rational(1, 1000)):
                            for q in (e_ - h, e_ + h):
                                if l < q < r:
                                    diem.append(q)
                diem.append(_chung_kien(Interval.open(l, r)))
            diem = sorted(set(diem), key=float)
            vals = [(q, gia_tri(f, {x: q}, pt_['cf'])) for q in diem]
            vals = [(q, v) for q, v in vals if v is not None]
            pc = None
            for i in range(len(vals)):
                for j in range(i + 1, len(vals)):
                    (q1, v1), (q2, v2) = vals[i], vals[j]
                    if (chieu == 1 and not v1 < v2) or (chieu == -1 and not v1 > v2):
                        pc = dict(x1=str(q1), f_x1=str(v1.evalf(10)), x2=str(q2), f_x2=str(v2.evalf(10)))
                        break
                if pc:
                    break
            if pc:
                loi.append('không %s biến trên cả tập %s: x1 < x2 nhưng f(x1)=%s, f(x2)=%s' % ('đồng' if chieu == 1 else 'nghịch', s_, pc['f_x1'], pc['f_x2']))
                tt['phan_vi_du_thu_tu'] = pc
    if loi:
        return ket_qua('SAI', 'don_dieu_sai', '; '.join(loi), buoc_sai=1, phan_chung=tt, bang_chung='xet_dau_fprime+thay_so')
    return ket_qua('DAT', 'don_dieu', 'Khớp khoảng đơn điệu máy tính', bang_chung='xet_dau_fprime+thay_so')


# ------------------------------------------------------------------ 7. lời giải nhiều dòng (kiểm từng cặp dòng)
def kiem_bien_doi_bieu_thuc(dong):
    E = [P(d) for d in dong]
    mien = dieu_kien(E[0])
    for kk in range(1, len(E)):
        pc, n = so_sanh(E[kk - 1], E[kk], [x], mien, 'ca_hai')
        if pc:
            return ket_qua('SAI', 'khong_tuong_duong', 'Dòng %d không bằng dòng %d trên miền xác định của dòng 1' % (kk + 1, kk),
                           buoc_sai=kk + 1, phan_chung=pc, bang_chung='thay_so_huu_ti')
        if n < 5:
            return ket_qua('KHONG_KIEM_DUOC', 'khong_du_diem', 'Không đủ điểm tại dòng %d' % (kk + 1), buoc_sai=None)
    return ket_qua('DAT', 'bien_doi_bieu_thuc', 'Mọi cặp dòng liên tiếp tương đương', bang_chung='thay_so_huu_ti')


def kiem_bien_doi_phuong_trinh(dong, ket_luan):
    ptr = [P_pt(d) for d in dong]
    D0 = dieu_kien(ptr[0][0]) + dieu_kien(ptr[0][1])
    S1, pp = giai_pt(*ptr[0])
    if S1 is None:
        return ket_qua('KHONG_KIEM_DUOC', 'khong_giai_duoc', 'Không giải được phương trình gốc')
    canh = []
    for kk in range(1, len(ptr)):
        Sk, _ = giai_pt(*ptr[kk])
        if Sk is None:
            return ket_qua('KHONG_KIEM_DUOC', 'khong_giai_duoc', 'Không giải được dòng %d' % (kk + 1))
        Sk_D = set(r for r in Sk if thoa_dk(D0, {x: r}))
        mat = [r for r in S1 if not _thuoc(r, Sk_D)]
        if mat:
            return ket_qua('SAI', 'mat_nghiem', 'Từ dòng %d sang dòng %d mất nghiệm %s' % (kk, kk + 1, _tap_str(mat)),
                           buoc_sai=kk + 1, phan_chung=dict(nghiem_bi_mat=[str(r) for r in mat]), bang_chung='giai_lai+the_nguoc')
        thua = [r for r in Sk if not _thuoc(r, S1)]
        if thua:
            canh.append('dòng %d là phép biến đổi hệ quả (thêm %s), cần thử lại ở kết luận' % (kk + 1, _tap_str(thua)))
    A = [P(r) for r in ket_luan]
    if not _cung_tap(A, list(S1)):
        return ket_qua('SAI', 'ket_luan_sai', 'Kết luận %s, tập nghiệm đúng (trong ĐKXĐ của đề) %s' % (_tap_str(A), _tap_str(S1)),
                       buoc_sai=len(ptr) + 1, phan_chung=dict(tap_may=_tap_str(S1)), bang_chung='giai_lai+the_nguoc', canh_bao=canh)
    return ket_qua('DAT', 'bien_doi_phuong_trinh', 'Không mất nghiệm ở bước nào; kết luận đúng', bang_chung='giai_lai+the_nguoc', canh_bao=canh)


def kiem_bien_doi_bpt(dong, ket_luan):
    R_ = [P(d) for d in dong]
    D0c = dieu_kien(R_[0].lhs) + dieu_kien(R_[0].rhs)
    D0 = mien_xd(D0c)
    sets = []
    for r in R_:
        T, conds = giai_bpt(r)
        if T is None or D0 is None:
            return ket_qua('KHONG_KIEM_DUOC', 'khong_giai_duoc', 'Không giải được một dòng')
        sets.append((T.intersect(D0), conds))
    for kk in range(1, len(R_)):
        A, B = sets[kk - 1][0], sets[kk][0]
        hieu = Union(Complement(A, B), Complement(B, A))
        if hieu != EmptySet:
            w = _chung_kien(hieu)
            t1 = _dung_tai(R_[kk - 1], D0c + sets[kk - 1][1], w)
            t2 = _dung_tai(R_[kk], D0c + sets[kk][1], w)
            if t1 != t2:
                return ket_qua('SAI', 'khong_tuong_duong', 'Dòng %d không tương đương dòng %d trên miền của đề; tại x = %s dòng %d %s, dòng %d %s'
                               % (kk + 1, kk, w, kk, 'đúng' if t1 else 'sai', kk + 1, 'đúng' if t2 else 'sai'),
                               buoc_sai=kk + 1, phan_chung=dict(x=str(w)), bang_chung='solveset+kiem_truc_tiep')
            return ket_qua('KHONG_KIEM_DUOC', 'cong_cu_mau_thuan', 'Mâu thuẫn công cụ tại dòng %d' % (kk + 1))
    S1 = sets[0][0]
    r = _so_tap(parse_tap(ket_luan), S1, lambda p: _dung_tai(R_[0], D0c, p))
    if r['trang_thai'] == 'SAI':
        r['buoc_sai'] = len(R_) + 1
        r['loai_kiem'] = 'ket_luan_sai'
        r['chi_tiet'] = 'Kết luận sai: ' + r['chi_tiet']
        return r
    if r['trang_thai'] == 'DAT':
        return ket_qua('DAT', 'bien_doi_bpt', 'Mọi cặp dòng tương đương; kết luận đúng', bang_chung=r['bang_chung'])
    return r


def kiem_khong_hinh_thuc(kieu):
    return ket_qua('KHONG_KIEM_DUOC', 'khong_hinh_thuc_hoa', 'Tầng 1 không kiểm được %s; không được coi là ĐẠT' %
                   ('lập luận bằng lời' if kieu == 'loi_van' else 'ảnh viết tay chưa số hóa'))


BO_KIEM = dict(
    dong_nhat=lambda c: kiem_dong_nhat(c['trai'], c['phai'], c.get('mien')),
    tap_nghiem_pt=lambda c: kiem_tap_nghiem_pt(c['pt'], c.get('nghiem_ai'), c.get('ho_nghiem_ai')),
    tap_nghiem_bpt=lambda c: kiem_tap_nghiem_bpt(c['bpt'], c['tap_ai']),
    tap_xac_dinh=lambda c: kiem_tap_xac_dinh(c['ham'], c['tap_ai']),
    dao_ham=lambda c: kiem_dao_ham(c['ham'], c['dao_ham_ai']),
    nguyen_ham=lambda c: kiem_nguyen_ham(c['ham'], c['nguyen_ham_ai']),
    tich_phan=lambda c: kiem_tich_phan(c['ham'], c['can_duoi'], c['can_tren'], c['gia_tri_ai']),
    chu_ky=lambda c: kiem_chu_ky(c['ham'], c['chu_ky_ai']),
    don_vi=lambda c: kiem_don_vi(c['trai'], c['phai']),
    dem=lambda c: kiem_dem(c['khong_gian'], c['gia_tri_ai']),
    xac_suat=lambda c: kiem_dem(c['khong_gian'], c['gia_tri_ai'], c['su_kien']),
    cuc_tri=lambda c: kiem_cuc_tri(c['ham'], c.get('cuc_dai_x'), c.get('cuc_tieu_x'), c.get('gia_tri_cuc_dai'), c.get('gia_tri_cuc_tieu')),
    don_dieu=lambda c: kiem_don_dieu(c['ham'], c.get('dong_bien'), c.get('nghich_bien'), c.get('dong_bien_tren_tap'), c.get('nghich_bien_tren_tap')),
    bien_doi_bieu_thuc=lambda c: kiem_bien_doi_bieu_thuc(c['dong']),
    bien_doi_phuong_trinh=lambda c: kiem_bien_doi_phuong_trinh(c['dong'], c['ket_luan']),
    bien_doi_bpt=lambda c: kiem_bien_doi_bpt(c['dong'], c['ket_luan']),
    loi_van=lambda c: kiem_khong_hinh_thuc('loi_van'),
    anh_viet_tay=lambda c: kiem_khong_hinh_thuc('anh'),
)


def _kkd_dau_vao(chi_tiet):
    r = ket_qua('KHONG_KIEM_DUOC', 'dau_vao_khong_hop_le', chi_tiet)
    r['ly_do'] = DAU_VAO_KHONG_HOP_LE
    return r


def kiem(ca_kiem):
    """F-01: mọi chuỗi bị bộ phân tích an toàn từ chối -> KHONG_KIEM_DUOC, ly_do DAU_VAO_KHONG_HOP_LE (không bao giờ DAT/SAI)."""
    del _TU_CHOI[:]
    try:
        r = BO_KIEM[ca_kiem['kieu']](ca_kiem)
    except DauVaoKhongHopLe as ex:
        return _kkd_dau_vao(str(ex))
    if _TU_CHOI:
        return _kkd_dau_vao(_TU_CHOI[0])
    return r


# ------------------------------------------------------------------ 8. bài làm có cấu trúc 5 bước (đơn điệu / cực trị)
MA_BUOC = ['B.DH.TXD', 'B.DH.DAOHAM', 'B.DH.NGHIEM', 'B.DH.XETDAU', 'B.DH.KETLUAN']


def _bs(ma, dong=None, o=None):
    return dict(ma_buoc=ma, dong=dong, o=o)


def _sai5(ma, dong, o, loai, chi_tiet, phan_chung=None, bang_chung=None):
    r = ket_qua('SAI', loai, chi_tiet, buoc_sai=_bs(ma, dong, o), phan_chung=phan_chung, bang_chung=bang_chung)
    return r


def _van_de_thua(j, p, nghiem_hs):
    """Điểm thừa ở hàng X (chốt 29/09, đã đóng băng): nếu điểm cũng có trong nghiệm học sinh viết ở B.DH.NGHIEM thì
    đúng MỘT vấn đề ERR.DH.24 tại B.DH.NGHIEM (ô X vẫn tô đỏ nhưng không trừ lần hai); chỉ thêm ở bảng thì ERR.DH.31
    tại B.DH.XETDAU."""
    if _thuoc(p, nghiem_hs):
        ma, ml, kn = 'B.DH.NGHIEM', 'ERR.DH.24', 'T12.DH.02'
    else:
        ma, ml, kn = 'B.DH.XETDAU', 'ERR.DH.31', 'T12.DH.03'
    return dict(loai_ket_qua='DIEM_THUA', buoc_sai=_bs(ma, None, dict(hang='x', k=2 * (j + 1))), diem=[str(p)], ma_loi=ml, ky_nang=kn)


def _k_thu_tu(diem_hs):
    """Chỉ số (từ 0) của điểm đầu tiên nhỏ hơn hoặc bằng điểm liền trước; None nếu tăng ngặt."""
    for j in range(1, len(diem_hs)):
        try:
            tang_ngat = bool(S(diem_hs[j] - diem_hs[j - 1]) > 0)
        except Exception:
            tang_ngat = float(diem_hs[j]) > float(diem_hs[j - 1])
        if not tang_ngat:
            return j
    return None


def _kiem_bang_sai_thu_tu(f, cf, bang, moc, diem_hs, k_thu_tu, khong0, khongxd, pt_, thieu, thua, nghiem_hs):
    """Hàng X không tăng dần (luật 3.4 d, chốt 29/09).

    - Gốc theo thứ tự bước: DIEM_THIEU / DIEM_THUA (tập mốc so như tập hợp) rồi SAI_THU_TU_MOC tại k của điểm
      đầu tiên <= điểm liền trước.
    - Ô dấu được kiểm theo cột CHÍNH học sinh dựng (khoảng giữa hai mốc liền kề của học sinh). Ô sai theo cột đó:
      loại theo kết quả kiểm (DAU_DOI_TRONG_KHOANG nếu y' đổi dấu bên trong, SAI_DAU nếu không); nếu khớp đáp án
      cùng k sau khi sắp tăng dần thì là hệ quả, nguyen_nhan trỏ SAI_THU_TU_MOC (hoặc DIEM_THIEU khi khoảng đã sắp
      vẫn chứa điểm thiếu); còn sai sau khi sắp là lỗi riêng.
    - Mũi tên chỉ bị báo khi trái hàng dấu của chính học sinh (SAI_BIEN_DOI).
    """
    ky = {'+': 1, '-': -1, '||': None, 'khong_xd': None, '0': 0}
    kc = {'tang': 1, 'giam': -1, 'khong_xd': None, '||': None}

    def dau_khoang(l, r):
        if not bool(S(r - l) > 0):
            l, r = r, l
        if not bool(S(r - l) > 0):
            return 'rong'
        ph = [d for d in pt_['doan'] if Interval.open(d['l'], d['r']).intersect(Interval.open(l, r)) != EmptySet]
        ds_ = set(d['dau'] if d['trong_D'] else None for d in ph)
        return next(iter(ds_)) if len(ds_) == 1 else 'tron'

    def o_diem(p):
        if gia_tri(f, {x: p}, cf) is None or _thuoc(p, khongxd):
            return '||'
        if _thuoc(p, khong0):
            return '0'
        return '?'

    sap = sorted(diem_hs, key=lambda p: float(p))
    moc_sap = [moc[0]] + sap + [moc[-1]]
    n = len(diem_hs)
    van_de = []
    id_thieu = None
    if thieu:
        id_thieu = 'VD1'
        van_de.append(dict(id=id_thieu, loai_ket_qua='DIEM_THIEU', buoc_sai=_bs('B.DH.NGHIEM', None, dict(hang='x', k=None)),
                           diem=[str(p) for p in thieu], so_diem_thieu=len(thieu)))
    thua_sau = []
    for j, p in thua[:1]:
        v = _van_de_thua(j, p, nghiem_hs)
        if v['buoc_sai']['ma_buoc'] == 'B.DH.NGHIEM':
            v['id'] = 'VD%d' % (len(van_de) + 1)
            van_de.append(v)
        else:
            thua_sau.append(v)  # trong XETDAU, SAI_THU_TU_MOC đứng trước
    id_goc = 'VD%d' % (len(van_de) + 1)
    van_de.append(dict(id=id_goc, loai_ket_qua='SAI_THU_TU_MOC', buoc_sai=_bs('B.DH.XETDAU', None, dict(hang='x', k=2 * (k_thu_tu + 1))),
                       ma_loi='ERR.DH.30', ky_nang='T12.DH.03'))
    for v in thua_sau:
        v['id'] = 'VD%d' % (len(van_de) + 1)
        van_de.append(v)
    o_dau = []  # (k kiểm định, vấn đề)
    dau_ai = bang['dau']
    for j in range(n + 1):
        s_ai = ky.get(dau_ai[j], 'khong_hop_le')
        rieng = dau_khoang(moc[j], moc[j + 1])
        if rieng == 'rong' or s_ai == rieng:
            continue
        loai = 'DAU_DOI_TRONG_KHOANG' if rieng == 'tron' else 'SAI_DAU'
        dung_sap = dau_khoang(moc_sap[j], moc_sap[j + 1])
        v = dict(loai_ket_qua=loai, buoc_sai=_bs('B.DH.XETDAU', None, dict(hang="dau_y'", k=2 * j + 1)))
        if dung_sap == 'tron' and id_thieu:
            v['nguyen_nhan'] = id_thieu
        elif s_ai == dung_sap:
            v['nguyen_nhan'] = id_goc
        o_dau.append((2 * j + 1, v))
    if 'dau_tai_diem' in bang:
        for j in range(n):
            gt = bang['dau_tai_diem'][j]
            if gt == o_diem(diem_hs[j]):
                continue
            v = dict(loai_ket_qua='SAI_GIA_TRI', buoc_sai=_bs('B.DH.XETDAU', None, dict(hang="dau_y'", k=2 * (j + 1))))
            if gt == o_diem(sap[j]):
                v['nguyen_nhan'] = id_goc
            o_dau.append((2 * (j + 1), v))
    o_chieu = []
    if 'chieu' in bang:
        for j in range(n + 1):
            if kc.get(bang['chieu'][j], 'x') != ky.get(dau_ai[j], 'khong_hop_le'):
                o_chieu.append((2 * j + 1, dict(loai_ket_qua='SAI_BIEN_DOI', buoc_sai=_bs('B.DH.XETDAU', None, dict(hang='bien_thien', k=2 * j + 1)))))
    for _, v in sorted(o_dau, key=lambda t: t[0]) + o_chieu:
        v['id'] = 'VD%d' % (len(van_de) + 1)
        van_de.append(v)
    dau_bs = van_de[0]['buoc_sai']
    r = _sai5(dau_bs['ma_buoc'], None, dau_bs['o'], 'sai_thu_tu_moc',
              'Mốc hàng X không tăng dần (điểm thứ %d không lớn hơn điểm liền trước)' % k_thu_tu,
              dict(so_van_de=len(van_de)), 'so_sanh_thu_tu')
    r['cac_van_de'] = van_de
    return r


def kiem_5_buoc(bl, bo_qua_txd=False):
    """F-01: bọc lõi; chuỗi nào bị từ chối (kể cả khi lõi đã nuốt lỗi) -> KHONG_KIEM_DUOC, ly_do DAU_VAO_KHONG_HOP_LE."""
    del _TU_CHOI[:]
    try:
        r = _kiem_5_buoc_loi(bl, bo_qua_txd)
    except DauVaoKhongHopLe as ex:
        r = None
        _TU_CHOI.append(str(ex))
    if _TU_CHOI:
        return _kkd_dau_vao(_TU_CHOI[0])
    return r


def _kiem_5_buoc_loi(bl, bo_qua_txd=False):
    from sympy import limit
    f = P(bl['ham'])
    cf = dieu_kien(f)
    D = mien_xd(cf)
    if D is None:
        return ket_qua('KHONG_KIEM_DUOC', 'khong_giai_duoc', 'Không tính được TXĐ')
    # Bước 1 – TXĐ
    A = parse_tap(bl['TXD'])
    r1 = _so_tap(A, D, lambda p: gia_tri(f, {x: p}, cf) is not None, ten_ai='bài làm', loai='tap_xac_dinh')
    if r1['trang_thai'] != 'DAT' and not bo_qua_txd:
        if r1['trang_thai'] == 'SAI':
            r = _sai5('B.DH.TXD', 1, None, 'sai_tap_xac_dinh', r1['chi_tiet'], r1['phan_chung'], r1['bang_chung'])
            # API trả đủ mọi lỗi gốc theo thứ tự bước (S31): chấm tiếp các bước sau như thể TXĐ đúng
            try:
                sau = kiem_5_buoc(bl, bo_qua_txd=True)
            except Exception:
                sau = None
            van_de = [dict(id='VD1', loai_ket_qua='SAI_TXD', buoc_sai=r['buoc_sai'])]
            if sau and sau['trang_thai'] == 'SAI':
                ds = sau.get('cac_van_de') or [dict(id='VD1', loai_ket_qua=loai_ket_qua(sau)[0], buoc_sai=sau['buoc_sai'])]
                doi = {v['id']: 'VD%d' % (i + 2) for i, v in enumerate(ds)}
                for v in ds:
                    v = dict(v, id=doi[v['id']])
                    if v.get('nguyen_nhan'):
                        v['nguyen_nhan'] = doi.get(v['nguyen_nhan'], v['nguyen_nhan'])
                    van_de.append(v)
            if len(van_de) > 1:
                r['cac_van_de'] = van_de
            return r
        return r1
    # Bước 2 – đạo hàm
    r2 = kiem_dao_ham(bl['ham'], bl['dao_ham'])
    if r2['trang_thai'] != 'DAT':
        if r2['trang_thai'] == 'SAI':
            return _sai5('B.DH.DAOHAM', 2, None, 'dao_ham_sai', r2['chi_tiet'], r2['phan_chung'], r2['bang_chung'])
        return r2
    # Bước 3 – nghiệm y' = 0 và điểm y' không xác định (thuộc TXĐ)
    pt_ = phan_tich_dau(f)
    if pt_ is None:
        return ket_qua('KHONG_KIEM_DUOC', 'khong_phan_tich_duoc', "Không phân tích được dấu y'")
    fp = pt_['fp']
    gay = diem_gay(f)
    khong0, khongxd = [], []
    for p in pt_['pts']:
        if gia_tri(f, {x: p}, cf) is None:
            continue  # ngoài TXĐ
        if any(la_khong(p - q) for q in gay):
            try:
                l_, r_ = limit(fp, x, p, '-'), limit(fp, x, p, '+')
            except Exception:
                return ket_qua('KHONG_KIEM_DUOC', 'khong_phan_tich_duoc', 'Không tính được giới hạn một phía của y\' tại %s' % p)
            if not la_khong(l_ - r_):
                khongxd.append(p)
                continue
            if la_khong(l_):
                khong0.append(p)
            continue
        v = gia_tri(fp, {x: p})
        if v is None:
            khongxd.append(p)
        elif abs(v) <= EPS:
            khong0.append(p)
    A0 = [P(v) for v in bl.get('y_phay_bang_0', [])]
    Ax = [P(v) for v in bl.get('y_phay_khong_xd', [])]
    loi3 = []
    if not _cung_tap(A0, khong0):
        loi3.append("y' = 0: bài làm %s, máy %s" % (_tap_str(A0), _tap_str(khong0)))
    if not _cung_tap(Ax, khongxd):
        loi3.append("y' không xác định (trong TXĐ): bài làm %s, máy %s" % (_tap_str(Ax), _tap_str(khongxd)))
    _bang3 = bl.get('bang')
    _sai_thu_tu_bang = bool(_bang3) and _k_thu_tu([P(m) for m in _bang3['moc'][1:-1]]) is not None
    _thua_trong_bang = False
    if loi3 and _bang3:
        _x_bang = [P(m) for m in _bang3['moc'][1:-1]]
        _th3 = [p for p in khong0 if not _thuoc(p, A0)] + [p for p in khongxd if not _thuoc(p, Ax)]
        _tu3 = [p for p in A0 if not _thuoc(p, khong0)] + [p for p in Ax if not _thuoc(p, khongxd)]
        _thua_trong_bang = not _th3 and bool(_tu3) and all(_thuoc(p, _x_bang) for p in _tu3)
    if loi3 and not _sai_thu_tu_bang and not _thua_trong_bang:
        # v1.3 (S21): vừa thiếu nghiệm vừa sai thứ tự mốc -> chấm theo bảng để báo đủ các gốc
        thieu3 = [p for p in khong0 if not _thuoc(p, A0)] + [p for p in khongxd if not _thuoc(p, Ax)]
        thua3 = [p for p in A0 if not _thuoc(p, khong0)] + [p for p in Ax if not _thuoc(p, khongxd)]
        r = _sai5('B.DH.NGHIEM', 3, None, 'sai_diem_toi_han', '; '.join(loi3),
                  dict(y_phay_bang_0=[str(p) for p in khong0], y_phay_khong_xd=[str(p) for p in khongxd]), 'solveset+gioi_han_mot_phia')
        r['cac_van_de'] = ([dict(id='VD1', loai_ket_qua='DIEM_THIEU', buoc_sai=_bs('B.DH.NGHIEM', 3), diem=[str(p) for p in thieu3])] if thieu3 else []) + \
                          ([dict(id='VD%d' % (2 if thieu3 else 1), loai_ket_qua='DIEM_THUA', buoc_sai=_bs('B.DH.NGHIEM', 3), diem=[str(p) for p in thua3])] if thua3 else [])
        return r
    if False:
        return _sai5('B.DH.NGHIEM', 3, None, 'sai_diem_toi_han', '; '.join(loi3),
                     dict(y_phay_bang_0=[str(p) for p in khong0], y_phay_khong_xd=[str(p) for p in khongxd]), 'solveset+gioi_han_mot_phia')
    # Bước 4 – bảng xét dấu / biến thiên (chấm cả bước khi nộp).
    # Chỉ số ô k đếm từ trái sang, xen kẽ khoảng và điểm do CHÍNH học sinh dựng:
    # moc [-oo, p1, ..., pn, oo] -> k=1 (-oo;p1), k=2 điểm p1, k=3 (p1;p2), ..., k=2n+1 (pn;+oo).
    bang = bl['bang']
    moc = [P(m.replace('+oo', 'oo')) for m in bang['moc']]
    diem_hs = moc[1:-1]
    # (a) hàng x: tập điểm của HS so với nghiệm y' ∪ điểm y' không xác định ∪ biên/điểm loại khỏi TXĐ
    # sửa lỗi lần 1 (S10): chỉ giữ điểm thuộc TXĐ hoặc là biên TXĐ; nghiệm tử số của y' nằm ngoài TXĐ (x=1 với sqrt(x^2-2x)) không phải mốc
    bd = D.boundary
    dung_x = [p for p in pt_['pts'] if gia_tri(f, {x: p}, cf) is not None or (isinstance(bd, FiniteSet) and _thuoc(p, list(bd)))]
    thieu = [p for p in dung_x if not _thuoc(p, diem_hs)]
    thua = [(j, p) for j, p in enumerate(diem_hs) if not _thuoc(p, dung_x)]
    # (b) chẩn đoán phụ: ô khoảng mà y' đổi dấu bên trong (do HS bỏ sót điểm)
    def _phan(l, r):
        return [d for d in pt_['doan'] if Interval.open(d['l'], d['r']).intersect(Interval.open(l, r)) != EmptySet]
    o_khoang = []
    for j in range(1, len(moc)):
        l, r = moc[j - 1], moc[j]
        ph = _phan(l, r)
        ds_ = set(d['dau'] if d['trong_D'] else None for d in ph)
        o_khoang.append(dict(k=2 * j - 1, khoang='(%s; %s)' % (l, r), dau=(next(iter(ds_)) if len(ds_) == 1 else 'tron'), phan=ph))
    doi_trong = [dict(k=o['k'], khoang=o['khoang']) for o in o_khoang if o['dau'] == 'tron']
    # (d) thứ tự mốc (chốt 29/09): app không tự sắp.
    k_thu_tu = _k_thu_tu(diem_hs)
    nghiem_hs = [P(v) for v in (bl.get('y_phay_bang_0') or [])] + [P(v) for v in (bl.get('y_phay_khong_xd') or [])]
    if k_thu_tu is not None:
        return _kiem_bang_sai_thu_tu(f, cf, bang, moc, diem_hs, k_thu_tu, khong0, khongxd, pt_, thieu, thua, nghiem_hs)
    if thieu or thua:
        van_de = []
        if thieu:
            van_de.append(dict(id='VD1', loai_ket_qua='DIEM_THIEU', buoc_sai=_bs('B.DH.NGHIEM', None, dict(hang='x', k=None)),
                               diem=[str(p) for p in thieu], so_diem_thieu=len(thieu)))
        for j, p in thua[:1]:
            v = _van_de_thua(j, p, nghiem_hs)
            v['id'] = 'VD%d' % (len(van_de) + 1)
            van_de.append(v)
        for dt in doi_trong:
            van_de.append(dict(id='VD%d' % (len(van_de) + 1), loai_ket_qua='DAU_DOI_TRONG_KHOANG', nguyen_nhan='VD1' if thieu else None,
                               buoc_sai=_bs('B.DH.XETDAU', None, dict(hang="dau_y'", k=dt['k'])), khoang=dt['khoang']))
        if not thieu:
            # chỉ có điểm thừa: ô khoảng hai bên chỉ bị báo khi dấu thật sự sai (S30)
            _ky = {'+': 1, '-': -1, '||': None, 'khong_xd': None, '0': 0}
            for o in o_khoang:
                if o['dau'] != 'tron' and _ky.get(bang['dau'][(o['k'] - 1) // 2], 'khong_hop_le') != o['dau']:
                    van_de.append(dict(id='VD%d' % (len(van_de) + 1), loai_ket_qua='SAI_DAU',
                                       buoc_sai=_bs('B.DH.XETDAU', None, dict(hang="dau_y'", k=o['k'])), khoang=o['khoang']))
        dau_bs = van_de[0]['buoc_sai']
        r = _sai5(dau_bs['ma_buoc'], None, dau_bs['o'], 'sai_hang_x_bang',
                  'Hàng x của bảng: thiếu %s, thừa %s' % (_tap_str(thieu), _tap_str([p for _, p in thua])),
                  dict(tap_diem_dung=_tap_str(dung_x), dau_doi_trong_khoang=doi_trong), 'solveset+gioi_han_mot_phia')
        r['cac_van_de'] = van_de
        return r
    ky = {'+': 1, '-': -1, '||': None, 'khong_xd': None, '0': 0}
    kc = {'tang': 1, 'giam': -1, 'khong_xd': None, '||': None}
    ky = {'+': 1, '-': -1, '||': None, 'khong_xd': None, '0': 0}
    dau_ai = bang['dau']
    for o in o_khoang:
        s_ai = ky.get(dau_ai[(o['k'] - 1) // 2], 'khong_hop_le')
        if o['dau'] == 'tron':  # chỉ xảy ra nếu hàng x đã đúng mà vẫn trộn dấu (không mong đợi)
            return _sai5('B.DH.XETDAU', None, dict(hang="dau_y'", k=o['k'], khoang=o['khoang']), 'dau_doi_trong_khoang',
                         "Ô %d %s: y' đổi dấu (hoặc có phần ngoài TXĐ) bên trong khoảng HS dựng" % (o['k'], o['khoang']),
                         [dict(diem_thu=str(d['t']), dau=d['dau'] if d['trong_D'] else 'ngoài TXĐ') for d in o['phan']], 'diem_thu_huu_ti+dau_chinh_xac')
        if s_ai != o['dau']:
            return _sai5('B.DH.XETDAU', None, dict(hang="dau_y'", k=o['k'], khoang=o['khoang']), 'sai_dau_o_khoang',
                         "Ô %d %s: bài làm ghi '%s', dấu y' tại điểm thử %s là %s" % (o['k'], o['khoang'], dau_ai[(o['k'] - 1) // 2], o['phan'][0]['t'], o['dau']),
                         dict(diem_thu=str(o['phan'][0]['t'])), 'diem_thu_huu_ti+dau_chinh_xac')
    # (c) ô tại điểm: '0' nếu y' = 0, '||' nếu y' không xác định hoặc điểm ngoài TXĐ
    if 'dau_tai_diem' in bang:
        for j, p in enumerate(diem_hs):
            if gia_tri(f, {x: p}, cf) is None or _thuoc(p, khongxd):
                dung_o = '||'
            elif _thuoc(p, khong0):
                dung_o = '0'
            else:
                dung_o = '?'
            if bang['dau_tai_diem'][j] != dung_o:
                return _sai5('B.DH.XETDAU', None, dict(hang="dau_y'", k=2 * (j + 1), diem=str(p)), 'sai_o_tai_diem',
                             "Ô %d (x = %s): bài làm ghi '%s', đúng là '%s'" % (2 * (j + 1), p, bang['dau_tai_diem'][j], dung_o), None, 'the_diem_chinh_xac')
    if 'chieu' in bang:
        for o in o_khoang:
            if kc.get(bang['chieu'][(o['k'] - 1) // 2], 'x') != ky.get(dau_ai[(o['k'] - 1) // 2], 'khong_hop_le'):
                return _sai5('B.DH.XETDAU', None, dict(hang='bien_thien', k=o['k'], khoang=o['khoang']), 'sai_o_chieu_bien_thien',
                             "Ô %d %s: chiều '%s' không khớp dấu y' %s" % (o['k'], o['khoang'], bang['chieu'][(o['k'] - 1) // 2], o['dau']),
                             None, 'diem_thu_huu_ti+dau_chinh_xac')
    # Bước 5 – kết luận
    kl = bl['ket_luan']
    r_dd = kiem_don_dieu(bl['ham'], kl.get('dong_bien'), kl.get('nghich_bien'), kl.get('dong_bien_tren_tap'), kl.get('nghich_bien_tren_tap')) \
        if any(t in kl for t in ('dong_bien', 'nghich_bien', 'dong_bien_tren_tap', 'nghich_bien_tren_tap')) else None
    r_ct = kiem_cuc_tri(bl['ham'], kl.get('cuc_dai_x'), kl.get('cuc_tieu_x'), kl.get('gia_tri_cuc_dai'), kl.get('gia_tri_cuc_tieu')) \
        if any(t in kl for t in ('cuc_dai_x', 'cuc_tieu_x', 'gia_tri_cuc_dai', 'gia_tri_cuc_tieu')) else None
    for r in (r_dd, r_ct):
        if r and r['trang_thai'] == 'SAI':
            return _sai5('B.DH.KETLUAN', 5, None, r['loai_kiem'], r['chi_tiet'], r['phan_chung'], r['bang_chung'])
    for r in (r_dd, r_ct):
        if r and r['trang_thai'] != 'DAT':
            return r
    return ket_qua('DAT', 'bai_lam_5_buoc', 'Cả 5 bước hợp lệ', bang_chung='nhieu_phuong_phap')


# ------------------------------------------------------------------ ánh xạ sang loai_ket_qua (giao ước nhóm 27/09)
_MAP = dict(khong_tuong_duong='SAI_BIEN_DOI', sai_mien_xac_dinh='SAI_BIEN_DOI', sai_o_chieu_bien_thien='SAI_BIEN_DOI',
            thua_nghiem_vi_pham_dkxd='DIEM_THUA', thua_nghiem_khong_thoa='DIEM_THUA',
            mat_nghiem='DIEM_THIEU', mat_ho_nghiem='DIEM_THIEU',
            sai_tap_xac_dinh='SAI_TXD', sai_dau_o_khoang='SAI_DAU', dau_doi_trong_khoang='DAU_DOI_TRONG_KHOANG', sai_thu_tu_moc='SAI_THU_TU_MOC',
            cuc_tri_sai='SAI_KET_LUAN', don_dieu_sai='SAI_KET_LUAN', ket_luan_sai='SAI_KET_LUAN')


THU_TU_LOAI = ['SAI_TXD', 'DIEM_THIEU', 'DIEM_THUA', 'SAI_THU_TU_MOC', 'SAI_DAU', 'SAI_GIA_TRI', 'SAI_BIEN_DOI',
               'DAU_DOI_TRONG_KHOANG', 'SAI_KET_LUAN']


def loai_ket_qua(r, kieu=None):
    if r['trang_thai'] == 'DAT':
        return ['DAT']
    if r['trang_thai'] == 'KHONG_KIEM_DUOC':
        return ['KHONG_KIEM_DUOC']
    lk = r['loai_kiem']
    if 'cac_van_de' in r:
        # v1.3: liệt kê mọi loại (kể cả ô hệ quả) theo thứ tự xuất hiện trong cac_van_de (gốc theo bước rồi ô theo k)
        loai = []
        for v in r['cac_van_de']:
            if v['loai_ket_qua'] not in loai:
                loai.append(v['loai_ket_qua'])
        return loai
    if lk == 'khac_tap':
        return ['SAI_TXD'] if kieu == 'tap_xac_dinh' else ['SAI_GIA_TRI']
    if lk == 'sai_diem_toi_han':
        return ['DIEM_THIEU_HOAC_THUA']
    return [_MAP.get(lk, 'SAI_GIA_TRI')]


# ------------------------------------------------------------------ chỉ số ô ra ngoài (giao ước 29/09)
_HANG_RA = {'x': 'X', "dau_y'": 'DAU_YPHAY', 'bien_thien': 'BIEN_THIEN'}
_HANG_VAO = {'x': 'X', 'X': 'X', "dau_y'": 'DAU_YPHAY', 'dau_yphay': 'DAU_YPHAY', 'DAU_YPHAY': 'DAU_YPHAY',
             'bien_thien': 'BIEN_THIEN', 'BIEN_THIEN': 'BIEN_THIEN'}


def o_ra_ngoai(o):
    """Đổi ô nội bộ (k từ 1, xen kẽ khoảng/điểm, hàng chữ thường) sang ô giao ước 29/09:
    hang viết HOA (X / DAU_YPHAY / BIEN_THIEN), k đếm từ 0.
    - X: k là chỉ số điểm trong hàng X (0 = điểm đầu tiên sau -oo).
    - DAU_YPHAY, BIEN_THIEN: k xen kẽ khoảng/điểm, 0 = khoảng đầu tiên, 1 = điểm đầu tiên, ...
    Nhận cả tên cũ chữ thường làm bí danh; đầu ra chỉ dùng tên HOA."""
    if not o or 'hang' not in o:
        return o
    hang = _HANG_VAO.get(o['hang'], o['hang'])
    k = o.get('k')
    if k is not None and hang == 'X':
        k = k // 2 - 1
    elif k is not None:
        k = k - 1
    return dict(hang=hang, k=k)
