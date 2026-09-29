# -*- coding: utf-8 -*-
"""Thử nghiệm bộ lọc chặn lộ đáp án: so 3 cách
 M1 – so chuỗi LaTeX chuẩn của đáp án (sau khi bỏ khoảng trắng, $, \\left, \\right);
 M2 – trích xuất (regex tiếng Việt: số, số viết bằng chữ, phân số, thập phân dấu phẩy, khoảng, tập) + so bằng SymPy, CHỈ theo giá trị;
 M3 – như M2 nhưng giá trị phải gắn với NGỮ CẢNH đúng loại sự kiện (đồng biến/nghịch biến/cực đại/cực tiểu/nghiệm).
"""
import re, json, os, time, unicodedata, hashlib, datetime
from collections import OrderedDict
import yaml
from sympy import Rational, oo, nsimplify, latex, S
from sympy.parsing.sympy_parser import standard_transformations, rationalize

TR = standard_transformations + (rationalize,)  # giữ tên cũ; F-01: không còn gọi parse_expr trực tiếp
DIR = os.path.dirname(os.path.abspath(__file__))


_SO_0_DAU = re.compile(r'(?<![\d.])0+(?=\d)')


def bo_so_0_dau(s):
    """'08' -> '8', '-02' -> '-2', '(01)/(02)' -> '(1)/(2)'; giữ nguyên '0', '0.5', '10' (bản vá Kiểm định)."""
    return _SO_0_DAU.sub('', s)


_SO_AN_TOAN = re.compile(r'[0-9+\-*/().]*(sqrt\([0-9+\-*/().]+\)[0-9+\-*/().]*)*')


_SO_0_DAU = re.compile(r'(?<![\d.])0+(?=\d)')


def bo_so_0_dau(s):
    """'08' -> '8', '007' -> '7', '(01)/(02)' -> '(1)/(2)'; giữ '0', '0.5', '10' (sửa lỗi "08" 29/09: Python không cho số nguyên có 0 đứng đầu)."""
    return _SO_0_DAU.sub('', s)


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


def num(s):
    """F-01: đọc số bằng bộ phân tích an toàn (thay lời gọi parse_expr cũ ở dòng 31). Bị từ chối -> DauVaoKhongHopLe."""
    s = bo_so_0_dau(str(s).replace(' ', ''))
    if s in ('oo', '+oo'):
        return oo
    if s == '-oo':
        return -oo
    return nsimplify(phan_tich_an_toan(s), rational=True)


def parse_khoang(s):
    a, b = s.strip()[1:-1].split(';')
    return (num(a), num(b))


# ------------------------------------------------------------ M1
def latex_so(v):
    if v == oo:
        return '+\\infty'
    if v == -oo:
        return '-\\infty'
    return latex(v)


def m1_chuoi(bai):
    out = []
    for loai, v in bai['su_kien']:
        if loai in ('DB', 'NB'):
            a, b = parse_khoang(v)
            out.append('(%s;%s)' % (latex_so(a), latex_so(b)))
        elif loai in ('DCD', 'DCT'):
            out.append('x=%s' % latex_so(num(v)))
        elif loai == 'GTCD':
            out.append('y_{CD}=%s' % latex_so(num(v)))
        elif loai == 'GTCT':
            out.append('y_{CT}=%s' % latex_so(num(v)))
        elif loai == 'NGHIEM':
            out.append('x=%s' % latex_so(num(v)))
    return out


def m1(text, bai):
    t = text.replace('$', '').replace('\\left', '').replace('\\right', '').replace(' ', '')
    return [c for c in m1_chuoi(bai) if c in t]


# ------------------------------------------------------------ chuẩn hóa tiếng Việt + LaTeX
SO_CHU = {'không': '0', 'một': '1', 'hai': '2', 'ba': '3', 'bốn': '4', 'tư': '4', 'năm': '5', 'sáu': '6',
          'bảy': '7', 'tám': '8', 'chín': '9', 'mười': '10'}
W = '|'.join(sorted(SO_CHU, key=len, reverse=True))
KICH = r'(bằng|là|tại|=|từ|đến|tới|và|hoặc|âm|trừ|khoảng)'


def chuan_hoa(text):
    t = unicodedata.normalize('NFC', text).lower()
    for a, b in [('−', '-'), ('–', '-'), ('∞', 'oo'), ('\\infty', 'oo'), ('\\left', ''), ('\\right', ''), ('$', ''),
                 ('\\{', '{'), ('\\}', '}'), ('\\cup', ' u '), ('\\dfrac', '\\frac'), ('+oo', 'oo')]:
        t = t.replace(a, b)
    t = re.sub(r'\\frac\{([^{}]*)\}\{([^{}]*)\}', r'(\1)/(\2)', t)
    t = re.sub(r'(âm|trừ) vô (cùng|cực)', '-oo', t)
    t = re.sub(r'(dương )?vô (cùng|cực)', 'oo', t)
    t = re.sub(r'\bmột nửa\b', '(1)/(2)', t)
    t = re.sub(r'\b(%s|\d+) phần (%s|\d+)\b' % (W, W), lambda m: '(%s)/(%s)' % (SO_CHU.get(m.group(1), m.group(1)), SO_CHU.get(m.group(2), m.group(2))), t)
    for _ in range(2):
        t = re.sub(r'(?<!\w)%s\s+(%s)(?!\w)' % (KICH, W), lambda m: m.group(1) + ' ' + SO_CHU[m.group(2)], t)
    t = re.sub(r'\bâm\s+(\d)', r'-\1', t)
    t = re.sub(r'(\d),(\d)', r'\1.\2', t)
    return t


NUM = r'-?\s*(?:oo|\(\d+\)/\(\d+\)|\d+(?:\.\d+)?(?:/\d+)?)'


def trich_xuat(t):
    """Trả danh sách (vị_trí, loại 'khoang'|'so', giá trị)."""
    items, dung = [], []
    for m in re.finditer(r'[\(\[]\s*(%s)\s*;\s*(%s)\s*[\)\]]' % (NUM, NUM), t):
        items.append((m.start(), 'khoang', (num(m.group(1)), num(m.group(2)))))
        dung.append(m.span())
    for m in re.finditer(r'(?:từ|khoảng)\s+(%s)\s+(?:đến|tới)\s+(%s)' % (NUM, NUM), t):
        items.append((m.start(), 'khoang', (num(m.group(1)), num(m.group(2)))))
        dung.append(m.span())
    for m in re.finditer(r'\{([^{}]*)\}', t):
        for p in re.split(r'[;,]', m.group(1)):
            if re.fullmatch(r'\s*%s\s*' % NUM, p):
                items.append((m.start(), 'so', num(p)))
        dung.append(m.span())
    for m in re.finditer(r'(?<![\w.^\)])(%s)(?![\w.]*\^)' % NUM, t):
        if any(a <= m.start() < b for a, b in dung):
            continue
        if m.start() > 0 and t[m.start() - 1] == '^':
            continue
        if 'oo' in m.group(1):
            continue
        # bỏ hệ số dính biến (3x, 12x) và số mũ
        end = m.end()
        if end < len(t) and t[end].isalpha():
            continue
        items.append((m.start(), 'so', num(m.group(1))))
    return items


KW = [('GTCD', ['giá trị cực đại', 'y_{cd}', 'y_cd', 'y_{cđ}', 'y_cđ']), ('GTCT', ['giá trị cực tiểu', 'y_{ct}', 'y_ct']),
      ('DCD', ['cực đại tại', 'điểm cực đại']), ('DCT', ['cực tiểu tại', 'điểm cực tiểu']),
      ('CD_MO', ['cực đại']), ('CT_MO', ['cực tiểu']), ('DB', ['đồng biến', 'tăng']), ('NB', ['nghịch biến', 'giảm'])]
KW_NGHIEM = ['nghiệm', 'x =', 'x=', 'x bằng', 's =', 's=']


def ngu_canh(t, pos):
    # đầu câu: dấu . ! ? không nằm giữa hai chữ số
    bd = 0
    for m in re.finditer(r'[!?\n]|\.(?!\d)', t[:pos]):
        bd = m.end()
    tien = t[bd:pos]
    best = None
    for loai, kws in KW:
        for kw in kws:
            i = tien.rfind(kw)
            if i >= 0 and (best is None or i > best[0] or (i == best[0] and len(kw) > best[2])):
                best = (i, loai, len(kw))
    if best:
        return best[1]
    for kw in KW_NGHIEM:
        if kw in tien:
            return 'NGHIEM'
    return None


def khop_loai(ctx, loai):
    if ctx == loai:
        return True
    return (ctx == 'CD_MO' and loai in ('DCD', 'GTCD')) or (ctx == 'CT_MO' and loai in ('DCT', 'GTCT'))


def m23(text, bai, dung_ngu_canh):
    t = chuan_hoa(text)
    hits = []
    sk = []
    for loai, v in bai['su_kien']:
        sk.append((loai, parse_khoang(v) if loai in ('DB', 'NB') else num(v)))
    for pos, kind, val in trich_xuat(t):
        ctx = ngu_canh(t, pos) if dung_ngu_canh else None
        for loai, v in sk:
            if kind == 'khoang' and loai in ('DB', 'NB'):
                ok = (val[0] - v[0] == 0 or val[0] == v[0]) and (val[1] - v[1] == 0 or val[1] == v[1])
            elif kind == 'so' and loai not in ('DB', 'NB'):
                ok = (val - v) == 0
            else:
                ok = False
            if ok and (not dung_ngu_canh or (ctx is not None and khop_loai(ctx, loai))):
                hits.append('%s=%s' % (loai, v))
    return sorted(set(hits))


BAN_NHAP_TOI_DA = 4000   # ký tự; dài hơn -> chặn (DAU_VAO_KHONG_HOP_LE)
# Dấu hiệu code trong bản nháp gia sư (không bao giờ là lời gợi ý toán hợp lệ) -> chặn. Bản nháp KHÔNG bao giờ được thực thi;
# chặn ở đây là fail-closed để câu lạ không tới học sinh.
_DAU_HIEU_CODE = re.compile(r'__|\blambda\b|\bimport\b|\b(?:eval|exec|open|compile|getattr|setattr|globals|locals|vars|input)\s*\('
                            r'|\bsubprocess\b|\bos\.\w|\bsys\.\w|\(\s*\)\s*\.'
                            r'|\b[A-Za-z_]\w*\s*\(\s*[\'"]'                                   # gọi hàm với đối số chuỗi: f('...')
                            r'|\b(?:factorial|Symbol|Integer|Float|Rational|Function|Lambda|sympify|parse_expr|lambdify)\s*\('
                            r'|(?:\*\*|\^)\s*\{?\s*\d+\s*\}?\s*(?:\*\*|\^)'                   # tháp lũy thừa 9**9**9, 10^10^10
                            r'|(?:\*\*|\^)\s*\(?\s*\d{3,}|\d\s*\*\*\s*\d{2,}'                # số mũ lớn
                            r'|\b(?![yY]_)\w+_\w+\b|["\'`]\s*\w+\s*["\'`]')                                # tên có '_' (x_1, su_kien), chuỗi trong nháy


def kiem_ban_nhap(text):
    """Trả None nếu bản nháp được phép đưa vào bộ so; hoặc lý do từ chối (chuỗi)."""
    if not isinstance(text, str):
        return 'ban_nhap không phải chuỗi'
    if len(text) > BAN_NHAP_TOI_DA:
        return 'ban_nhap quá dài (%d ký tự)' % len(text)
    m = _DAU_HIEU_CODE.search(text)
    if m:
        return 'ban_nhap có dấu hiệu code: %r' % m.group(0)
    return None


def main():
    raw = open(os.path.join(DIR, 'bo-tin-nhan.yaml'), 'rb').read()
    bo = yaml.safe_load(raw.decode())
    ppl = OrderedDict(M1_chuoi_latex=lambda t, b: m1(t, b), M2_trich_xuat_sympy_gia_tri=lambda t, b: m23(t, b, False),
                      M3_trich_xuat_sympy_ngu_canh=lambda t, b: m23(t, b, True))
    ds = []
    t0 = time.perf_counter()
    for tn in bo['tin_nhan']:
        b = bo['bai'][tn['bai']]
        r = OrderedDict(id=tn['id'], bai=tn['bai'], lo=tn['lo'], dien_dat=tn.get('dien_dat'), text=tn['text'], chuan_hoa=chuan_hoa(tn['text']))
        for ten, fn in ppl.items():
            h = fn(tn['text'], b)
            r[ten] = dict(chan=bool(h), khop=h)
        ds.append(r)
    tt = time.perf_counter() - t0
    tong = OrderedDict()
    ro = [d for d in ds if d['lo'] and d['dien_dat'] != 'vong_vo']
    vv = [d for d in ds if d['dien_dat'] == 'vong_vo']
    hl = [d for d in ds if not d['lo']]
    for ten in ppl:
        tong[ten] = OrderedDict(
            recall_lo_ro=('%d/%d' % (sum(d[ten]['chan'] for d in ro), len(ro)), round(sum(d[ten]['chan'] for d in ro) / len(ro), 4)),
            recall_vong_vo=('%d/%d' % (sum(d[ten]['chan'] for d in vv), len(vv)), round(sum(d[ten]['chan'] for d in vv) / len(vv), 4)),
            recall_tat_ca_ca_lo=('%d/%d' % (sum(d[ten]['chan'] for d in ro + vv), len(ro + vv)), round(sum(d[ten]['chan'] for d in ro + vv) / len(ro + vv), 4)),
            chan_nham=('%d/%d' % (sum(d[ten]['chan'] for d in hl), len(hl)), round(sum(d[ten]['chan'] for d in hl) / len(hl), 4)),
            ca_lo_bi_lot=[d['id'] for d in ro + vv if not d[ten]['chan']],
            ca_chan_nham=[d['id'] for d in hl if d[ten]['chan']])
    out = OrderedDict(truy_vet=dict(bo_tin_nhan='loc-lo-dap-an/bo-tin-nhan.yaml', sha256=hashlib.sha256(raw).hexdigest(),
                                    thoi_diem=datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=7))).isoformat(timespec='seconds'),
                                    thoi_gian_s=round(tt, 3)),
                      tong_hop=tong, tung_tin_nhan=ds)
    json.dump(out, open(os.path.join(DIR, '..', 'ket-qua', 'ket-qua-loc-lo-dap-an.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=1, default=str)
    print(json.dumps(tong, ensure_ascii=False, indent=1))


if __name__ == '__main__':
    main()
