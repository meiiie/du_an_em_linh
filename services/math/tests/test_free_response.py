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
