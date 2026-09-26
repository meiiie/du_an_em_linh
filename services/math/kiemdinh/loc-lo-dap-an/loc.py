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
from sympy.parsing.sympy_parser import parse_expr, standard_transformations, rationalize

TR = standard_transformations + (rationalize,)
DIR = os.path.dirname(os.path.abspath(__file__))


def num(s):
    s = s.replace(' ', '')
    if s in ('oo', '+oo'):
        return oo
    if s == '-oo':
        return -oo
    return nsimplify(parse_expr(s, transformations=TR), rational=True)


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
