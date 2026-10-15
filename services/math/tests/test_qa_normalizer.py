import pytest
from app.dau_vao import ly_do_tu_choi
from app.normalizer import normalize_expr, normalize_domain


@pytest.mark.parametrize("raw,expected", [("+inf", "+oo"), ("-inf", "-oo"), ("∞", "oo"), ("+∞", "+oo")])
def test_infinity_aliases(raw, expected):
    assert ly_do_tu_choi(raw) is None
    assert normalize_expr(raw) == expected
    assert normalize_domain(f"(-inf; {raw})") == f"(-oo; {expected})"


@pytest.mark.parametrize("raw", [r"R \ {3}", r"ℝ\{3}", r"\mathbb{R}\setminus\{3\}"])
def test_real_set_difference(raw):
    assert ly_do_tu_choi(raw) is None
    assert normalize_domain(raw) == r"R \ {3}"


def test_unknown_names_still_rejected():
    assert normalize_expr("infinite") is None
    assert ly_do_tu_choi("x.__class__") is not None


def test_domain_aliases_reach_safe_cas_parser():
    from sympy import S, FiniteSet
    from app.paths import load_kiem
    for raw in [r"R \ {3}", r"ℝ\{3}"]:
        assert load_kiem().parse_tap(normalize_domain(raw)) == S.Reals - FiniteSet(3)
    assert load_kiem().parse_tap(normalize_domain("(-inf; +∞)")) == S.Reals
