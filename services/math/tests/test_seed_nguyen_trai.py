"""Kiểm nội dung tự soạn; không thay cổng phát hành ba tầng của ứng dụng."""
import json
from pathlib import Path

import pytest
import sympy as s

ROOT = Path(__file__).resolve().parents[3] / 'data/dev/nguyen-trai'
EXERCISES = json.loads((ROOT / 'bai-tap.json').read_text())
x = s.Symbol('x', real=True)
a = s.Symbol('a', real=True)
n = s.Symbol('n', integer=True, positive=True)
k = s.Symbol('k', integer=True)
LOCALS = {'x': x, 'a': a, 'n': n, 'k': k, 'intersection': s.Symbol('intersection')}


def expr(value):
    return s.sympify(str(value), locals=LOCALS).doit()


def equal(left, right):
    assert s.simplify(s.trigsimp(expr(left) - expr(right))) == 0


def test_coverage():
    assert len(EXERCISES) >= 40
    assert len({e['id'] for e in EXERCISES}) == len(EXERCISES)
    assert {e['chu_de'] for e in EXERCISES} == {c['ma'] for c in json.loads((ROOT / 'danh-muc.json').read_text())}
    for chapter in {e['chu_de'] for e in EXERCISES}:
        assert {e['muc_do_4'] for e in EXERCISES if e['chu_de'] == chapter} == {'NB', 'TH', 'VD', 'VDC'}


@pytest.mark.parametrize('exercise', EXERCISES, ids=lambda e: e['id'])
def test_teacher_solution(exercise):
    # Each displayed symbolic step is checked, not only the final number.
    for step in exercise['loi_giai']['cac_buoc']:
        assert step['van_ban']
        if 'bieu_thuc_sympy' in step:
            equal(step['bieu_thuc_sympy'], step['ket_qua_sympy'])
    p = exercise['kiem_dinh']['du_kien']
    kind = exercise['kiem_dinh']['loai']
    answer = exercise['loi_giai']['dap_an']
    if kind == 'quadratic_min':
        f = expr(p['ham'])
        assert s.diff(f, x, 2) > 0
        expected = f.subs(x, s.solve(s.diff(f, x), x)[0])
    elif kind == 'set_intersection':
        assert answer == sorted(set(p['A']) & set(p['B']))
        return
    elif kind == 'mean':
        expected = s.Rational(sum(p['values']), len(p['values']))
    elif kind == 'ham_so':
        extra = answer.get('so_nghiem_theo_m')
        if extra:
            if exercise['id'].endswith('07'):
                assert extra == {'m<3 hoặc m>4': 1, 'm=3 hoặc m=4': 2, '3<m<4': 3}
                samples = [(-1, 1), (3, 2), (s.Rational(7, 2), 3), (4, 2), (5, 1)]
            else:
                assert extra == {'m>57': 0, 'm=57': 2, '-24<m<57': 4, 'm=-24': 3, 'm<-24': 2}
                samples = [(58, 0), (57, 2), (0, 4), (-24, 3), (-25, 2)]
            for height, count in samples:
                assert len(s.polys.polytools.intervals(expr(p['ham'])-height, eps=s.Rational(1,10000))) == count
            answer = answer['ket_luan']
        bl = exercise['bai_lam']
        f = expr(p['ham'])
        fp = s.diff(f, x)
        equal(fp, bl['dao_ham'])
        domain = s.calculus.util.continuous_domain(f, x, s.S.Reals)
        equal_set = lambda values: s.FiniteSet(*(expr(v) for v in values))
        assert s.solveset(fp, x, domain=domain) == equal_set(bl['y_phay_bang_0'])
        derivative_domain = s.calculus.util.continuous_domain(fp, x, s.S.Reals)
        assert domain - derivative_domain == equal_set(bl['y_phay_khong_xd'])
        from app.grader import K
        assert K.parse_tap(bl['TXD']) == domain
        marks = [expr(m.replace('+oo', 'oo')) for m in bl['bang']['moc']]
        signs = []
        for left, right in zip(marks, marks[1:]):
            interval = s.Interval.open(left, right)
            if interval.intersect(domain).is_empty:
                signs.append('||')
                continue
            positive = s.solveset(s.Gt(fp, 0), x, domain=interval)
            negative = s.solveset(s.Lt(fp, 0), x, domain=interval)
            signs.append('+' if positive == interval else '-' if negative == interval else '0')
        assert signs == bl['bang']['dau']
        assert bl['bang']['chieu'] == [{'+': 'tang', '-': 'giam', '0': 'khong_xd', '||': 'khong_xd'}[v] for v in signs]
        for i, point in enumerate(marks[1:-1]):
            assert bl['bang']['dau_tai_diem'][i] == ('0' if point in domain else '||')
        for sign, field in [('+', 'dong_bien'), ('-', 'nghich_bien')]:
            expected = [(marks[i], marks[i + 1]) for i, v in enumerate(signs) if v == sign]
            actual = [tuple(expr(v.strip().replace('+oo', 'oo')) for v in interval[1:-1].split(';')) for interval in answer[field]]
            assert actual == expected
        for before, after, field, values in [('+', '-', 'cuc_dai_x', 'gia_tri_cuc_dai'), ('-', '+', 'cuc_tieu_x', 'gia_tri_cuc_tieu')]:
            extrema = [marks[i + 1] for i in range(len(signs) - 1) if signs[i:i+2] == [before, after] and marks[i+1] in domain]
            assert [expr(v) for v in answer[field]] == extrema
            assert len(answer[values]) == len(extrema)
            for point, value in zip(extrema, answer[values]):
                equal(f.subs(x, point), value)
        # Use the actual runtime grade engine as a second check of the structured five steps.
        from app.grader import bai_lam_sang_payload, grade
        assert grade(bai_lam_sang_payload(bl, p['ham']))['ket_qua'] == 'DAT'
        return
    elif kind == 'trig_value':
        expected = p.get('hang_so', 0) + p.get('he_so', 1) * getattr(s, p['ham'])(expr(p['goc']))
    elif kind == 'trig_roots':
        actual = s.FiniteSet(*(expr(v) for v in answer))
        assert actual == s.solveset(expr(p['ham']), x, domain=s.Interval(expr(p['can_duoi']), expr(p['can_tren'])))
        return
    elif kind == 'ap_term':
        expected = p['u1'] + (p['n'] - 1) * p['d']
    elif kind == 'ap_sum':
        expected = sum(p['u1'] + i * p['d'] for i in range(p['n']))
    elif kind == 'gp_sum':
        expected = sum(p['u1'] * p['q'] ** i for i in range(p['n']))
    elif kind == 'ap_infer':
        u, d = s.symbols('u d')
        inferred = s.solve([u + (p['i']-1)*d-p['ui'], u+(p['j']-1)*d-p['uj']], [u, d])
        expected = sum(inferred[u] + i * inferred[d] for i in range(p['n']))
    elif kind == 'gp_infer':
        q = s.real_root(s.Rational(p['u5'], p['u2']), 3)
        expected = sum(p['u2'] / q * q**i for i in range(p['n']))
    elif kind == 'derivative_value':
        expected = s.diff(expr(p['ham']), x).subs(x, p['tai'])
    elif kind == 'slope_roots':
        assert s.FiniteSet(*(expr(v) for v in answer)) == s.solveset(s.diff(expr(p['ham']), x)-p['slope'], x, domain=s.S.Reals)
        return
    elif kind == 'tangent_through':
        f = expr(p['ham'])
        px, py = p['P']
        condition = f + (px-x)*s.diff(f,x)-py
        assert s.solveset(condition,x,domain=s.S.Reals) == s.FiniteSet(*(expr(v) for v in answer))
        return
    elif kind == 'maximum':
        f = expr(p['ham'])
        roots = s.solveset(s.diff(f, x), x, domain=s.Interval.open(p['can_duoi'], p['can_tren']))
        width = expr(answer['chieu_rong'])
        assert width in roots
        expected = max([f.subs(x, r) for r in roots] + [f.subs(x, p['can_duoi']), f.subs(x, p['can_tren'])])
        equal(f.subs(x,width),expected)
        answer=answer['dien_tich']
    elif kind in ('integral', 'area'):
        f = expr(p['ham'])
        lo, hi = expr(p['can_duoi']), expr(p['can_tren'])
        if kind == 'area':
            roots = sorted(r for r in s.solveset(f, x, domain=s.Interval(lo, hi)) if lo < r < hi)
            bounds = [lo, *roots, hi]
            expected = sum(s.Abs(s.integrate(f, (x, l, r))) for l, r in zip(bounds, bounds[1:]))
        else:
            expected = s.integrate(f, (x, lo, hi))
    elif kind == 'integral_parameter':
        integral = s.integrate(expr(p['ham']), (x, p['can_duoi'], p['can_tren']))
        assert s.solveset(integral-p['target'], a, domain=s.S.Reals) == s.FiniteSet(expr(answer))
        return
    elif kind == 'min_abs_area':
        # Enumerate the sign-change regimes from the actual linear function.
        t=s.Symbol('t', positive=True)
        f=expr(p['ham']).subs(a,-t)
        root=s.solve(f,x)[0]
        changing=s.simplify(s.integrate(f,(x,0,root))-s.integrate(f,(x,root,2)))
        candidates=s.solveset(s.diff(changing,t),t,domain=s.Interval.open(s.Rational(3,2),s.oo))
        assert candidates == s.FiniteSet(3/s.sqrt(2))
        expected=changing.subs(t,3/s.sqrt(2))
        assert expected < 3
        assert s.limit(changing,t,s.oo)==s.oo
    elif kind == 'between_area':
        f, g = expr(p['f']), expr(p['g'])
        bounds = sorted(s.solveset(f-g, x, domain=s.S.Reals))
        expected = s.integrate(s.Abs(f-g), (x, bounds[0], bounds[-1]))
    elif kind == 'pyramid':
        expected = s.Rational(p['a'] * p['b'] * p['h'], 6)
    elif kind == 'distance':
        expected = s.sqrt(sum((b-a)**2 for a, b in zip(p['A'], p['B'])))
    elif kind == 'box_diagonal':
        expected = s.sqrt(sum(a*a for a in p['canh']))
    elif kind == 'plane_projection':
        normal=s.Matrix(p['normal']);point=s.Matrix(p['M'])
        foot=point-(normal.dot(point)+p['d'])/normal.dot(normal)*normal
        assert list(foot)==[expr(v) for v in answer['H']]
        assert normal.dot(foot)+p['d']==0
        assert (point-foot).cross(normal)==s.zeros(3,1)
        expected=(point-foot).norm()
        answer=answer['khoang_cach']
    elif kind == 'plane_distance':
        expected = s.Abs(sum(a*b for a, b in zip(p['M'], p['normal'])) + p['d']) / s.sqrt(sum(a*a for a in p['normal']))
    elif kind == 'tetrahedron':
        origin = s.Matrix(p['O'])
        expected = s.Abs(s.Matrix.hstack(*(s.Matrix(p[key])-origin for key in ('A', 'B', 'C'))).det()) / 6
    elif kind == 'hypergeom':
        expected = s.binomial(p['good'], p['want']) * s.binomial(p['bad'], p['draw']-p['want']) / s.binomial(p['good']+p['bad'], p['draw'])
    elif kind == 'binomial':
        prob = expr(p['p'])
        expected = s.binomial(p['n'], p['k']) * prob**p['k'] * (1-prob)**(p['n']-p['k'])
    elif kind == 'conditional_group':
        expected = s.binomial(p['good'], p['draw']) / (s.binomial(p['good']+p['bad'], p['draw'])-s.binomial(p['bad'], p['draw']))
    elif kind == 'bayes':
        prior = expr(p['prior'])
        green1 = s.Rational(p['g1'], p['g1']+p['b1'])
        green2 = s.Rational(p['g2'], p['g2']+p['b2'])
        expected = prior*green1/(prior*green1+(1-prior)*green2)
    else:
        pytest.fail(f'Không có bộ kiểm cho {kind}')
    equal(expected, answer)


@pytest.mark.parametrize('formula', json.loads((ROOT / 'bang-chuong.json').read_text())['formulas'], ids=lambda f: f['ma'])
def test_chapter_formula(formula):
    check = formula['kiem_dinh']
    equal(check['trai'], check['phai'])


@pytest.mark.parametrize('work', json.loads((ROOT / 'bai-lam-lich-su.json').read_text()), ids=lambda w: w['ma']+'-'+w['loai'])
def test_historical_student_work(work):
    from app.grader import grade
    if work.get('giao_vien_cham'):
        exercise = next(e for e in EXERCISES if e['id'] == work['ma'])
        assert exercise['duyet_seed']['trang_thai'] == 'DA_DUYET'
        correct = work['dap_an_hoc_sinh'] == exercise['loi_giai']['dap_an']
        assert correct == (work['loai'] == 'DUNG')
        assert work['ket_qua'] == {'DUNG': 'DAT', 'SAI': 'SAI', 'CAN_DUYET': 'KHONG_KIEM_DUOC'}[work['loai']]
        return
    expected = {'DUNG': 'DAT', 'SAI': 'SAI', 'CAN_DUYET': 'KHONG_KIEM_DUOC'}[work['loai']]
    assert grade(work['payload'])['ket_qua'] == expected == work['ket_qua']


def class_library():
    return [dict(id=d['ma'], ten=d['title'], text=d['textContent'], license_status=d['licenseStatus'], phien_ban=d['version'])
            for d in json.loads((ROOT / 'tai-lieu.json').read_text())]


def derivative_formulas():
    return [dict(id=f['ma'], latex=f['latex'], noi_dung=f['noiDung'], ten=f['title'])
            for f in json.loads((ROOT / 'bang-dao-ham.json').read_text())['formulas']]


def test_derivative_sheet_passes_actual_formula_gate():
    import re
    from app.dong_cong_thuc import kiem_dong_cong_thuc
    docs = []
    for document in class_library():
        passages = [dict(id=f"{document['id']}-{i}", trang=None, text=text.strip())
                    for i, text in enumerate(re.split(r'(?<=[.;])\s+', document['text'])) if text.strip()]
        docs.append(dict(id=document['id'], ten=document['ten'], doan=passages, license_status=document['license_status']))
    formulas = [dict(id=f['id'], tieu_de=f['ten'], latex=f['latex'], phat_bieu=f['noi_dung']) for f in derivative_formulas()]
    rows = kiem_dong_cong_thuc(dict(dong=formulas, tai_lieu=docs))['dong']
    assert len(rows) == 6
    for row in rows:
        assert row['tang1']['trang_thai'] == row['tang2']['trang_thai'] == 'DAT', row
        assert row['tang2']['trich_dan']['doan']


@pytest.mark.parametrize('exercise', EXERCISES, ids=lambda e: e['id'])
def test_runtime_publication_gate(exercise):
    from app.verify import verify
    result = verify(dict(ham=exercise['de_bai']['ham_so_sympy'], bai_lam=exercise['bai_lam'],
                         de_bai=exercise['de_bai']['van_ban'], tai_lieu=class_library(), cong_thuc=derivative_formulas()))
    expected = 'DA_PHAT_HANH' if exercise['bai_lam'] else 'CHO_GIAO_VIEN_DUYET'
    assert result['trang_thai_phat_hanh'] == expected
    assert len(result['tang']) == 3


@pytest.mark.parametrize('exercise', EXERCISES, ids=lambda e: e['id'])
def test_display_math_is_separate(exercise):
    display = exercise['de_bai']['latex']
    statement = exercise['de_bai']['van_ban']
    assert display.strip() and '**' not in display and '*' not in display
    assert statement.count('$') >= 2 and statement.count('$') % 2 == 0
    assert '**' not in statement and '·' not in statement
    machine = exercise['de_bai']['ham_so_sympy']
    if machine:
        # Deterministic SymPy rendering, without an optional ANTLR dependency.
        expected = s.latex(s.sympify(machine, locals=LOCALS, evaluate=False) if exercise['kiem_dinh']['loai'] == 'trig_value' else expr(machine)).replace(r'\frac', r'\dfrac')
        assert expected in display


def test_review_queue_and_grade_coverage():
    assert sum(e['duyet_seed']['trang_thai'] == 'CHO_DUYET' for e in EXERCISES) == 5
    catalog = json.loads((ROOT / 'danh-muc.json').read_text())
    assert {c['ma'] for c in catalog if c['lop'] == 10} == {'BH10', 'MT10', 'TK10', 'XS10'}
    assert {c['ma'] for c in catalog if c['lop'] == 11} == {'LG11', 'DS11', 'DH11', 'KG11', 'XS11'}
    assert {c['ma'] for c in catalog if c['lop'] == 12} == {'DH12', 'TP12', 'KG12', 'XS12'}
    for c in catalog:
        assert any(e['chu_de'] == c['ma'] and e['duyet_seed']['trang_thai'] == 'DA_DUYET' for e in EXERCISES)


@pytest.mark.parametrize('exercise', EXERCISES, ids=lambda e: e['id'])
def test_list_preview_is_human(exercise):
    preview = exercise['de_bai']['latex']
    assert len(preview) <= 180
    assert not any(token in preview for token in ('Dữ kiện', 'good=', 'bad=', 'draw=', 'want=', 'g1=', 'normal=', r'\left['))
    if exercise['chu_de'].startswith(('KG', 'XS')):
        assert r'\text{' in preview
        assert any(word in preview.lower() for word in ('chóp', 'hộp', 'thẻ', 'bút', 'xu', 'bi ', 'nhóm', 'đoạn', 'diện', 'chiếu'))
