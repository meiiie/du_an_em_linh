"""Ngân hàng data/dev thật: không lấy đáp số từ trình duyệt."""
import copy
import json
from pathlib import Path
import pytest
from app.grader import grade

ROOT = Path(__file__).resolve().parents[3]
EXERCISES = json.loads((ROOT / "data/dev/nguyen-trai/bai-tap.json").read_text())


def payload(code="NT-KG12-NB-01", answer="72", presentation="Diện tích đáy bằng 6 nhân 8 chia 2."):
    ex = next(e for e in EXERCISES if e["id"] == code)
    return {"bai_toan": {"loai": "tra_loi_tu_do", "dap_so": ex["loi_giai"]["dap_an"]},
            "nop_toi": "B.TD.TRALOI", "cac_buoc": [{"ma_buoc": "B.TD.TRALOI", "cac_dong": [
                {"dong": 0, "latex": answer}, {"dong": 1, "latex": presentation}]}]}


def test_real_geometry_seed_correct_equivalent_and_wrong_answer():
    assert grade(payload())["ket_qua"] == "DAT"
    assert grade(payload(answer="144/2"))["ket_qua"] == "DAT"
    result = grade(payload(answer="73"))
    assert result["ket_qua"] == "SAI"
    assert "72" not in result["thong_bao"]


@pytest.mark.parametrize("answer", ["", "x", "__import__('os').getcwd()", "1/0", "oo", "NaN"])
def test_unreadable_answer_goes_to_teacher(answer):
    assert grade(payload(answer=answer))["ket_qua"] == "KHONG_KIEM_DUOC"


def test_missing_reference_goes_to_teacher_and_presentation_is_not_evaluated():
    p = payload(presentation="Em chưa biết làm tiếp, xin thầy cô xem.")
    p["bai_toan"]["dap_so"] = ""
    assert grade(p)["ket_qua"] == "KHONG_KIEM_DUOC"
    assert grade(payload(presentation="Em đã áp dụng công thức thể tích."))["ket_qua"] == "DAT"


def test_rejects_missing_reordered_and_duplicate_lines():
    for lines in [[], [{"dong": 0, "latex": "72"}], [{"dong": 1, "latex": "72"}, {"dong": 0, "latex": "72"}]]:
        p = payload()
        p["cac_buoc"][0]["cac_dong"] = lines
        assert grade(p)["ket_qua"] == "KHONG_KIEM_DUOC"


@pytest.mark.parametrize("presentation", [
    "", "a" * 2000,
    'Em dùng Pythagoras: BC = √(AB² + AC²). Kết quả: 72 cm³. ✅',
    "__import__('os').getcwd() <script>alert('text')</script>",
])
def test_presentation_is_free_text(presentation):
    assert grade(payload(presentation=presentation))["ket_qua"] == "DAT"


@pytest.mark.parametrize("presentation", ["a" * 2001, None, 72, [], {}])
def test_presentation_must_be_text_at_most_2000_characters(presentation):
    assert grade(payload(presentation=presentation))["ket_qua"] == "KHONG_KIEM_DUOC"


def test_answer_still_passes_expression_whitelist():
    assert grade(payload(answer="7" * 301))["ket_qua"] == "KHONG_KIEM_DUOC"


@pytest.mark.parametrize("code,answer", [
    ("NT-MT10-NB-01", "[4,3]"),
    ("NT-KG12-VDC-05", '{"H":["2/9","-1/9","20/9"],"khoang_cach":"8/3"}'),
])
def test_structured_reference_omitted_by_core_goes_to_teacher(code, answer):
    p = payload(code=code, answer=answer)
    # Core giữ bước nhập nhưng không cung cấp đáp số cho phép so vô hướng.
    del p["bai_toan"]["dap_so"]
    result = grade(p)
    assert result["ket_qua"] == "KHONG_KIEM_DUOC"
    assert result["per_buoc"] == {"B.TD.TRALOI": "KHONG_KIEM_DUOC"}
    assert result["loai_ket_qua"] == "KHONG_KIEM_DUOC"
    assert answer not in result["thong_bao"]
