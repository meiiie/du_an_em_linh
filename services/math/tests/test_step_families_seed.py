"""Đáp án từng bước được kiểm độc lập; không dùng build_steps làm oracle."""
import copy
import json
from itertools import combinations, product
from pathlib import Path

import pytest
import sympy as s

from app.grader import K, grade
from app.routers import cham_bai
from app.schemas import JobIn

ROOT = Path(__file__).resolve().parents[3] / "data/dev/nguyen-trai"
EXERCISES = {e["id"]: e for e in json.loads((ROOT / "bai-tap.json").read_text())}
FRAMES = json.loads((ROOT / "cham-buoc.json").read_text())["bai"]
NEW = [frame for frame in FRAMES if frame["ho"] != "B.DH"]
x = s.Symbol("x", real=True)


def expr(raw):
    return s.sympify(str(raw), locals={"x": x})


def equal(actual, expected):
    assert s.simplify(expr(actual) - expected) == 0


def display(spec):
    values = spec["dap_an"]
    if spec["kieu"] == "TAP_HUU_HAN":
        return "{" + "; ".join(values) + "}"
    return "; ".join(values)


def payload(frame, last=4):
    if frame["ho"] == "B.DH":
        value = copy.deepcopy(frame["payload_dung"])
        value["cac_buoc"] = value["cac_buoc"][:last + 1]
    else:
        value = {"bai_toan": frame["bai_toan"], "cac_buoc": [
            {"ma_buoc": step["ma_buoc"], "cac_dong": [{"dong": 0, "latex": display(step)}]}
            for step in frame["cac_buoc"][:last + 1]]}
    value["nop_toi"] = value["cac_buoc"][-1]["ma_buoc"]
    return copy.deepcopy(value)


def draw_probability(good, bad, draw, want):
    # Enumerate equiprobable subsets, independently of the grader's binomial formula.
    samples = list(combinations(range(good + bad), draw))
    return s.Rational(sum(sum(i < good for i in sample) == want for sample in samples), len(samples))


def independent_probability_steps(problem):
    kind, p = problem["loai"], problem["du_kien"]
    if kind == "hypergeom":
        total = len(list(combinations(range(p["good"] + p["bad"]), p["draw"])))
        first = len(list(combinations(range(p["good"]), p["want"])))
        second = len(list(combinations(range(p["bad"]), p["draw"] - p["want"])))
        return [total, first, second, first * second, draw_probability(p["good"], p["bad"], p["draw"], p["want"])]
    if kind == "conditional_group":
        samples = list(combinations(range(p["good"] + p["bad"]), p["draw"]))
        excluded = sum(all(i >= p["good"] for i in sample) for sample in samples)
        favorable = sum(all(i < p["good"] for i in sample) for sample in samples)
        return [len(samples), excluded, len(samples) - excluded, favorable, s.Rational(favorable, len(samples) - excluded)]
    if kind == "binomial":
        n, k, prob = p["n"], p["k"], expr(p["p"])
        events = [outcome for outcome in product((0, 1), repeat=n) if sum(outcome) == k]
        individual = prob ** k * (1 - prob) ** (n - k)
        return [len(events), prob ** k, (1 - prob) ** (n - k), individual,
                sum(s.prod(prob if v else 1 - prob for v in event) for event in events)]
    if kind == "three_colors":
        colors = [color for color, n in enumerate(p["counts"]) for _ in range(n)]
        samples = list(combinations(range(len(colors)), p["draw"]))
        parts = [sum([sum(colors[i] == color for i in sample) for color in range(3)] ==
                     [2 if color == doubled else 1 for color in range(3)] for sample in samples)
                 for doubled in range(3)]
        return [len(samples), *parts, s.Rational(sum(parts), len(samples))]
    if kind == "committee_roles":
        w, m, draw, minimum = p["women"], p["men"], p["draw"], p["min_women"]
        total = favorable = 0
        for captain in range(w + m):
            for secretary in range(w + m):
                if captain == secretary:
                    continue
                others = [i for i in range(w + m) if i not in (captain, secretary)]
                for members in combinations(others, draw - 2):
                    total += 1
                    favorable += captain < w <= secretary and sum(i < w for i in (captain, secretary, *members)) >= minimum
        others = [i for i in range(w + m) if i not in (0, w)]
        remaining = sum(1 + sum(i < w for i in members) >= minimum for members in combinations(others, draw - 2))
        return [total, w, m, remaining, s.Rational(favorable, total)]
    if kind == "bayes_given":
        joint, evidence = expr(p["joint"]), expr(p["evidence"])
        return [evidence, 1 - evidence, joint, evidence - joint, joint / evidence]
    if kind == "bayes_transfer":
        sg, sr = p["source_green"], p["source_red"]
        tg, tr, draw = p["target_green"], p["target_red"], p["draw"]
        # Enumerate each source marble and then each target subset.
        denominators = [0, 0]
        favorable = [0, 0]
        for source in range(sg + sr):
            category = 0 if source < sg else 1
            colors = [True] * tg + [False] * tr + [source < sg]
            for sample in combinations(range(len(colors)), draw):
                denominators[category] += 1
                favorable[category] += all(colors[i] for i in sample)
        prior = s.Rational(sg, sg + sr)
        first, second = [s.Rational(f, d) for f, d in zip(favorable, denominators)]
        evidence = s.Rational(sum(favorable), sum(denominators))
        return [prior, first, second, evidence, s.Rational(favorable[0], sum(favorable))]
    prior = expr(p["prior"])
    draw, want = (p["draw"], p["want"]) if kind == "bayes_sample" else (1, 1)
    first = draw_probability(p["g1"], p["b1"], draw, want)
    second = draw_probability(p["g2"], p["b2"], draw, want)
    evidence = prior * first + (1 - prior) * second
    return [prior, first, second, evidence, prior * first / evidence]


def test_coverage_and_scope_are_explicit():
    assert len(EXERCISES) == 77
    assert len(FRAMES) == 43
    assert sum(frame["san_sang_ui"] for frame in FRAMES) == 10
    assert len({frame["ma_bai"] for frame in FRAMES}) == len(FRAMES)
    assert {frame["ma_bai"] for frame in FRAMES if frame["san_sang_ui"]} == {
        e["id"] for e in EXERCISES.values() if e["dang_cau"] == "TU_LUAN_5_BUOC"}
    for frame in NEW:
        assert not frame["san_sang_ui"]
        assert frame["bai_toan"] == EXERCISES[frame["ma_bai"]]["kiem_dinh"]
        assert len(frame["cac_buoc"]) == 5
        assert all(step["ten"] and step["viec"] and step["dap_an"] for step in frame["cac_buoc"])


@pytest.mark.parametrize("frame", [f for f in NEW if f["ho"] == "B.XS"], ids=lambda f: f["ma_bai"])
def test_every_probability_answer_by_independent_enumeration(frame):
    expected = independent_probability_steps(frame["bai_toan"])
    for step, answer in zip(frame["cac_buoc"], expected):
        assert len(step["dap_an"]) == 1
        equal(step["dap_an"][0], answer)
    equal(frame["cac_buoc"][-1]["dap_an"][0], expr(EXERCISES[frame["ma_bai"]]["loi_giai"]["dap_an"]))


@pytest.mark.parametrize("frame", [f for f in NEW if f["ho"] == "B.GT"], ids=lambda f: f["ma_bai"])
def test_every_calculus_answer_from_definition(frame):
    kind, p = frame["bai_toan"]["loai"], frame["bai_toan"]["du_kien"]
    steps = frame["cac_buoc"]
    values = [step["dap_an"] for step in steps]
    answer = EXERCISES[frame["ma_bai"]]["loi_giai"]["dap_an"]
    if kind == "quadratic_min":
        f = expr(p["ham"])
        polynomial = s.Poly(f, x)
        a, b, _ = polynomial.all_coeffs()
        vertex = -b / (2 * a)
        equal(values[0][0], a)
        assert a > 0
        equal(values[1][0], vertex)
        equal(values[2][0], f.subs(x, vertex))
        equal(values[3][0], f)
        assert s.expand(expr(values[3][0]) - expr(values[2][0])) == s.expand(a * (x - vertex) ** 2)
        assert s.solveset(f < expr(values[4][0]), x, domain=s.S.Reals) == s.S.EmptySet
    elif kind == "trig_roots":
        f = expr(p["ham"])
        lo, hi = expr(p["can_duoi"]), expr(p["can_tren"])
        assert list(map(expr, values[0])) == [lo, hi]
        equal(values[1][0], s.trigsimp(f))
        u = s.Symbol("u", real=True)
        transformed = expr(values[1][0]).subs(s.sin(x), u)
        levels = s.solveset(transformed, u, domain=s.Interval(-1, 1))
        assert s.FiniteSet(*map(expr, values[2])) == levels
        assert s.FiniteSet(*map(expr, values[3])) == s.solveset(s.sin(x) - min(levels), x, domain=s.Interval(lo, hi))
        assert s.FiniteSet(*map(expr, values[4])) == s.solveset(f, x, domain=s.Interval(lo, hi))
        assert s.FiniteSet(*map(expr, values[4])) == s.FiniteSet(*map(expr, answer))
        return
    elif kind == "between_area":
        f, g = expr(p["f"]), expr(p["g"])
        roots = s.solveset(f - g, x, domain=s.S.Reals)
        assert s.FiniteSet(*map(expr, values[0])) == roots
        equal(values[1][0], g - f)
        equal(s.diff(expr(values[2][0]), x), g - f)
        lo, hi = sorted(roots)
        equal(values[3][0], s.integrate(g - f, (x, lo, hi)))
        equal(values[4][0], s.integrate(s.Abs(g - f), (x, lo, hi)))
    else:
        f, fp = expr(p["ham"]), s.diff(expr(p["ham"]), x)
        if kind in ("integral", "area"):
            lo, hi = expr(p["can_duoi"]), expr(p["can_tren"])
            assert list(map(expr, values[0])) == [lo, hi]
            F = expr(values[1][0])
            equal(s.diff(F, x), f)
            equal(values[2][0], F.subs(x, lo))
            equal(values[3][0], F.subs(x, hi))
            if kind == "area":
                # Establish the sign on the entire interval before integrating.
                assert s.solveset(f < 0, x, domain=s.Interval.open(lo, hi)) == s.Interval.open(lo, hi)
                equal(values[4][0], s.integrate(-f, (x, lo, hi)))
            else:
                equal(values[4][0], s.integrate(f, (x, lo, hi)))
        elif kind == "derivative_value":
            assert K.parse_tap(values[0][0]) == s.calculus.util.continuous_domain(f, x, s.S.Reals)
            if f.is_polynomial(x):
                leading = max(s.Add.make_args(s.expand(f)), key=lambda term: s.degree(term, x))
                partial = s.diff(leading, x)
            elif f.func == s.sin:
                partial = s.diff(f.args[0], x)
            else:
                # Quotient numerator independently recovered from the derivative.
                partial = s.cancel(fp * s.denom(f) ** 2)
            equal(values[1][0], partial)
            equal(values[2][0], fp)
            equal(values[3][0], f.subs(x, p["tai"]))
            equal(values[4][0], fp.subs(x, p["tai"]))
        elif kind == "tangent_through":
            assert K.parse_tap(values[0][0]) == s.S.Reals
            equal(values[1][0], fp)
            px, py = p["P"]
            equal(values[2][0], f + (px - x) * fp - py)
            roots = s.solveset(expr(values[2][0]), x, domain=s.S.Reals)
            assert s.FiniteSet(*map(expr, values[4])) == roots
            assert s.FiniteSet(*map(expr, values[3])) == s.FiniteSet(*(fp.subs(x, r) for r in roots))
            assert s.FiniteSet(*map(expr, values[4])) == s.FiniteSet(*map(expr, answer))
            return
        elif kind == "maximum":
            lo, hi = p["can_duoi"], p["can_tren"]
            assert list(map(expr, values[0])) == [lo, hi]
            equal(values[1][0], fp)
            roots = s.solveset(fp, x, domain=s.Interval.open(lo, hi))
            assert s.FiniteSet(*map(expr, values[2])) == roots
            point = next(iter(roots))
            assert list(map(expr, values[3])) == [f.subs(x, v) for v in (lo, point, hi)]
            equal(values[4][0], expr(answer["chieu_rong"]))
            equal(values[4][1], expr(answer["dien_tich"]))
            # Concavity proves global maximum, not just a comparison at chosen points.
            assert s.diff(f, x, 2) < 0
            return
    equal(values[4][0], expr(answer))


@pytest.mark.parametrize("last", range(5))
@pytest.mark.parametrize("frame", FRAMES, ids=lambda f: f["ma_bai"])
def test_correct_prefixes_match_core_result_contract(frame, last):
    request = payload(frame, last)
    result = grade(request)
    assert result["ket_qua"] == "DAT", result
    assert result["per_buoc"] == {step["ma_buoc"]: "DAT" for step in request["cac_buoc"]}
    assert result["nop_toi"] == request["nop_toi"]
    assert result["chua_xong"] == (last < 4)
    assert result["cac_van_de"] == []


@pytest.mark.parametrize("index", range(5))
@pytest.mark.parametrize("frame", NEW, ids=lambda f: f["ma_bai"])
def test_wrong_step_is_caught_at_its_position_without_answer_leak(frame, index):
    request = payload(frame)
    kind = frame["cac_buoc"][index]["kieu"]
    wrong = {"MIEN": "R \\ {987}", "TAP_HUU_HAN": "{987}", "DAY_GIA_TRI": "987; 988"}.get(kind, "987*x" if kind == "NGUYEN_HAM" else "987")
    request["cac_buoc"][index]["cac_dong"][0]["latex"] = wrong
    result = grade(request)
    assert result["ket_qua"] == "SAI", result
    assert result["buoc_sai"] == {"ma_buoc": request["cac_buoc"][index]["ma_buoc"], "dong": 0, "o": None}
    assert result["ma_loi"] is None  # No borrowed DH error/skill code for these families.
    assert result["per_buoc"][request["cac_buoc"][index]["ma_buoc"]] == "SAI"
    assert result["chuan_hoa"] == []
    assert "dap_an" not in json.dumps(result)
    assert "987" not in json.dumps(result)


def find(kind):
    return next(f for f in NEW if f["bai_toan"]["loai"] == kind)


def test_primitive_constant_and_endpoint_values_are_consistent():
    frame = find("integral")
    request = payload(frame)
    for index in (1, 2, 3):
        request["cac_buoc"][index]["cac_dong"][0]["latex"] += "+7"
    assert grade(request)["ket_qua"] == "DAT"
    request["cac_buoc"][2]["cac_dong"][0]["latex"] = frame["cac_buoc"][2]["dap_an"][0]
    assert grade(request)["ket_qua"] == "SAI"


@pytest.mark.parametrize("written", ["x^3+x/x-1", "x^3+sqrt(x)^2-x", "x^3+log(x)-log(x)"])
def test_primitive_cancellation_cannot_hide_domain_restrictions(written):
    request = payload(find("integral"))
    request["cac_buoc"][1]["cac_dong"][0]["latex"] = written
    assert grade(request)["ket_qua"] == "KHONG_KIEM_DUOC"


def test_square_cancellation_cannot_hide_a_hole_at_the_vertex():
    request = payload(find("quadratic_min"))
    request["cac_buoc"][3]["cac_dong"][0]["latex"] = "(x-2)^4/(x-2)^2-1"
    assert grade(request)["ket_qua"] == "KHONG_KIEM_DUOC"


def test_equivalent_latex_and_sets_in_any_order():
    request = payload(find("bayes_given"))
    request["cac_buoc"][-1]["cac_dong"][0]["latex"] = r"\dfrac{2}{5}"
    assert grade(request)["ket_qua"] == "DAT"
    request = payload(find("tangent_through"))
    request["cac_buoc"][-1]["cac_dong"][0]["latex"] = r"\{\frac{3}{2}; 0\}"
    assert grade(request)["ket_qua"] == "DAT"


def test_completing_square_requires_the_square_and_accepts_reversed_base():
    frame = find("quadratic_min")
    request = payload(frame)
    request["cac_buoc"][3]["cac_dong"][0]["latex"] = frame["bai_toan"]["du_kien"]["ham"]
    assert grade(request)["ket_qua"] == "SAI"
    request["cac_buoc"][3]["cac_dong"][0]["latex"] = "(2-x)^2-1"
    assert grade(request)["ket_qua"] == "DAT"


@pytest.mark.parametrize("mutation", ["missing", "duplicate", "order", "unknown", "mixed", "empty", "two_lines", "bad_index", "table", "last", "start", "forged_answer"])
def test_malformed_frames_fail_closed(mutation):
    request = payload(find("integral"))
    steps = request["cac_buoc"]
    if mutation == "missing":
        del steps[1]
    elif mutation == "duplicate":
        steps[1] = copy.deepcopy(steps[0])
    elif mutation == "order":
        steps.reverse()
    elif mutation == "unknown":
        steps[0]["ma_buoc"] = "B.NOTHING"
    elif mutation == "mixed":
        steps[0]["ma_buoc"] = "B.DH.TXD"
    elif mutation == "empty":
        steps[0]["cac_dong"] = []
    elif mutation == "two_lines":
        steps[0]["cac_dong"] *= 2
    elif mutation == "bad_index":
        steps[0]["cac_dong"][0]["dong"] = 1
    elif mutation == "table":
        steps[0]["bang"] = {"cac_o": []}
    elif mutation == "last":
        request["nop_toi"] = "B.DH.KETLUAN"
    elif mutation == "start":
        request["buoc_bat_dau"] = steps[-1]["ma_buoc"]
    elif mutation == "forged_answer":
        request["bai_toan"]["dap_an"] = 987
    assert grade(request)["ket_qua"] == "KHONG_KIEM_DUOC"


@pytest.mark.parametrize("bad", ["__import__('os')", "x.subs(x,1)", "9**9**9", "x + unknown", "sqrt(-1)", "1/0", "", "x" * 301])
def test_unsafe_or_nonreal_inputs_are_never_accepted(bad):
    request = payload(find("integral"))
    request["cac_buoc"][-1]["cac_dong"][0]["latex"] = bad
    assert grade(request)["ket_qua"] != "DAT"


@pytest.mark.parametrize("problem", [
    {"loai": "integral", "du_kien": {"ham": "1/x", "can_duoi": -1, "can_tren": 1}},
    {"loai": "area", "du_kien": {"ham": "x", "can_duoi": -1, "can_tren": 1}},
    {"loai": "hypergeom", "du_kien": {"good": 3, "bad": 4, "draw": 1000000, "want": 1}},
    {"loai": "hypergeom", "du_kien": {"good": True, "bad": 4, "draw": 1, "want": 1}},
    {"loai": "binomial", "du_kien": {"n": 2, "k": 3, "p": "1/2"}},
    {"loai": "bayes_given", "du_kien": {"joint": "1/2", "evidence": 0}},
    {"loai": "bayes_given", "du_kien": {"joint": "1/2", "evidence": "1/3"}},
    {"loai": "bayes_given", "du_kien": {"joint": "__import__('os')", "evidence": "1/2"}},
    {"loai": "min_abs_area", "du_kien": {}},
])
def test_unverifiable_models_fail_closed(problem):
    assert grade({"bai_toan": problem, "cac_buoc": []})["ket_qua"] == "KHONG_KIEM_DUOC"


@pytest.mark.parametrize("kind", ["integral", "bayes", "tangent_through"])
def test_grade_router_runs_new_families_in_sandbox(kind):
    result = cham_bai(JobIn(**payload(find(kind))))
    assert result["ket_qua"] == "DAT", result
    assert result["chuan_hoa"] == []
