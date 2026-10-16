"""Chỉ so đáp số trong sandbox; phần trình bày dành cho giáo viên."""
from app.dau_vao import ly_do_tu_choi
from app.step_families import read


def grade_free_response(payload):
    from app.grader import _pack, _buoc
    import sympy as s

    code = "B.TD.TRALOI"
    unknown = lambda: _pack("KHONG_KIEM_DUOC", "KHONG_KIEM_DUOC", _buoc(code),
        "can_giao_vien", {code: "KHONG_KIEM_DUOC"}, [], nop_toi=code,
        thong_bao="Thầy cô sẽ xem đáp số và phần trình bày của em.")
    try:
        if payload.get("nop_toi") != code:
            return unknown()
        steps = payload.get("cac_buoc", [])
        if len(steps) != 1 or steps[0].get("ma_buoc") != code or steps[0].get("bang"):
            return unknown()
        lines = steps[0].get("cac_dong", [])
        if len(lines) != 2 or [x.get("dong") for x in lines] != [0, 1]:
            return unknown()
        answer = lines[0].get("latex")
        presentation = lines[1].get("latex")
        if (not isinstance(answer, str) or ly_do_tu_choi(answer)
                or not isinstance(presentation, str) or len(presentation) > 2000):
            return unknown()
        expected = payload["bai_toan"].get("dap_so", "")
        actual = answer.strip()
        if not actual or not expected:
            return unknown()
        # Chỉ nhận đáp số hữu hạn, không biến; văn bản/lập luận không tự phán đúng.
        matches = s.simplify(read(actual, True) - read(expected, True)) == 0
        result = "DAT" if matches else "SAI"
        return _pack(result, result if matches else "SAI_GIA_TRI", None if matches else _buoc(code, 0),
            "dat" if matches else "sai_dap_so", {code: result}, [], nop_toi=code,
            ma_loi_tin=(None, None), thong_bao="Đáp số đúng. Phần trình bày dành cho thầy cô xem." if matches
            else "Đáp số chưa đúng. Em kiểm tra lại cách tính.")
    except Exception:
        return unknown()
