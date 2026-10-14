import json
import pytest
from pydantic import ValidationError
from app.routers import kiem_so_hang
from app.schemas import SoHangIn
from app.so_hang import so_hang


def check(student, expected="2*x-6"):
    return so_hang({"expected": expected, "student": student, "variable": "x"})


def test_demo_preserves_written_terms_and_does_not_return_reference():
    result = check("2x - 6x + 4")
    assert result == {"terms": [{"index": 1, "text": "2x", "status": "ok"},
                                {"index": 2, "text": "- 6x", "status": "sai"},
                                {"index": 3, "text": "+ 4", "status": "thua"}], "missing_count": 0}
    assert set(result) == {"terms", "missing_count"}
    assert "2*x-6" not in json.dumps(result)


@pytest.mark.parametrize("student,expected,statuses,missing", [
    ("2x - 6", "2*x-6", ["ok", "ok"], 0), ("-6 + 2x", "2*x-6", ["ok", "ok"], 0),
    ("2x", "2*x-6", ["ok"], 1), ("2x + 2x - 6", "2*x-6", ["ok", "thua", "ok"], 0),
    ("3x - 6", "2*x-6", ["sai", "ok"], 0),
    (r"\frac{4x}{2} - 6", "2*x-6", ["ok", "ok"], 0),
    ("x^(-2) - 6", "x**(-2)-6", ["ok", "ok"], 0),
    ("(x+1) - 6", "x-5", ["ok", "ok"], 0), ("x+x - 6", "2*x-6", ["ok", "ok", "ok"], 0), ("0", "0", ["ok"], 0),
])
def test_matching(student, expected, statuses, missing):
    result = check(student, expected)
    assert [t["status"] for t in result["terms"]] == statuses
    assert result["missing_count"] == missing


@pytest.mark.parametrize("bad", ["__import__('os')", "x.subs(x,1)", "x^9999", "9^9^9", "x+", "(x+1", "1/0", "nan", "x"*301, "x; print(1)"])
def test_unsafe_or_ambiguous_input_has_no_hint(bad):
    assert check(bad) == {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "DAU_VAO_KHONG_HOP_LE"}
    assert check("x", bad)["trang_thai"] == "KHONG_KIEM_DUOC"


def test_endpoint_sandbox_and_derivative_reference():
    # Gọi router và sandbox thật, không cần socket của TestClient.
    result = kiem_so_hang(SoHangIn(function="x**2-6*x+4", student="2x - 6x + 4", variable="x"))
    assert result == check("2x - 6x + 4")
    result = kiem_so_hang(SoHangIn(expected="2*x-6", student="2x-6"))
    assert [t["status"] for t in result["terms"]] == ["ok", "ok"]
    for invalid in [{"student": "x"}, {"student": "x", "expected": "x", "function": "x"},
                    {"student": "x", "expected": "x", "timeout_s": 20},
                    {"student": "x", "expected": "x", "variable": "other"}]:
        with pytest.raises(ValidationError):
            SoHangIn.model_validate(invalid)


def test_single_variable_and_unsafe_variable():
    assert so_hang({"expected": "2t-6", "student": "2t-6t+4", "variable": "t"}) == {
        "terms": [{"index": 1, "text": "2t", "status": "ok"},
                  {"index": 2, "text": "-6t", "status": "sai"},
                  {"index": 3, "text": "+4", "status": "thua"}], "missing_count": 0}
    assert so_hang({"expected": "t+x", "student": "2t", "variable": "t"})["trang_thai"] == "KHONG_KIEM_DUOC"
    for variable in ["e", "__import__", "x+y", 1]:
        assert so_hang({"expected": "x", "student": "x", "variable": variable})["trang_thai"] == "KHONG_KIEM_DUOC"
