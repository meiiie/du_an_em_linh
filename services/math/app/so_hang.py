# -*- coding: utf-8 -*-
"""Đối chiếu hạng tử trong sandbox; chỉ xuất chữ học sinh đã gõ, không xuất đáp án."""
import re
from sympy import Add, diff, expand, simplify
from app.dau_vao import ly_do_tu_choi
from app.normalizer import normalize_expr
from app.paths import load_kiem


def tach_so_hang(text):
    """Giữ thứ tự/chữ gốc, tách cộng/trừ ngoài ngoặc, không tách dấu của số mũ."""
    terms, start, depth = [], 0, 0
    for i, char in enumerate(text):
        if char in "({":
            depth += 1
        elif char in ")}":
            depth -= 1
        elif char in "+-−–—" and depth == 0 and text[start:i].strip():
            if text[:i].rstrip()[-1] not in "+-−–—*/^":
                terms.append(text[start:i].strip())
                start = i
        if depth < 0:
            raise ValueError("ngoặc không cân bằng")
    if depth:
        raise ValueError("ngoặc không cân bằng")
    terms.append(text[start:].strip())
    if not all(terms) or len(terms) > 32:
        raise ValueError("không tách được hạng tử")
    return terms


def so_hang(payload):
    try:
        variable = payload.get("variable", "x")
        if not isinstance(variable, str) or not re.fullmatch(r"[a-df-z]", variable):
            raise ValueError("biến chưa hỗ trợ")
        K = load_kiem()

        def read(raw):
            if not isinstance(raw, str) or ly_do_tu_choi(raw):
                raise ValueError("đầu vào không hợp lệ")
            if variable != "x" and re.search(r"(?<![A-Za-z])x(?![A-Za-z])", raw):
                raise ValueError("có biến ngoài biến đang đối chiếu")
            # Dùng cùng normalizer và parser một biến x; không đổi chữ xuất ra cho học sinh.
            norm = normalize_expr(re.sub(r"(?<![A-Za-z])" + variable + r"(?![A-Za-z])", "x", raw))
            if norm is None:
                raise ValueError("không chuẩn hóa được")
            expr = K.phan_tich_an_toan(norm)
            if expr.has(K.oo, K.zoo, K.nan) or expr.free_symbols - {K.x}:
                raise ValueError("biểu thức không hữu hạn")
            return expr

        student = payload["student"]
        # Core chỉ có hàm của đề: cùng đạo hàm CAS như bộ chấm, không đọc solutions.
        expected = read(payload["expected"]) if payload.get("expected") is not None else diff(read(payload["function"]), K.x)
        reference = [term for term in Add.make_args(expand(expected)) if term != 0]
        written = tach_so_hang(student)
        values = [read(term) for term in written]
        # Dòng tương đương nhưng chưa thu gọn (x+x, nhóm ngoặc…) không bị chỉ sai từng mảnh.
        if simplify(Add(*values) - expected) == 0:
            return {"terms": [{"index": i + 1, "text": term, "status": "ok"} for i, term in enumerate(written)],
                    "missing_count": 0}
        statuses = [None] * len(written)
        remaining = list(range(len(reference)))
        # Ghép chính xác trước để hạng tử sai không chiếm chỗ của hạng tử đúng.
        for i, value in enumerate(values):
            match = next((j for j in remaining if simplify(value - reference[j]) == 0), None)
            if match is not None:
                statuses[i] = "ok"
                remaining.remove(match)
            elif value == 0:
                statuses[i] = "ok"
        # Ghép phần cần sửa theo hệ số/phần biến, rồi thứ tự; phần dư là thừa.
        # Chỉ xác định vị trí, không suy diễn nguyên nhân hoặc phán quyết cả dòng.
        for i, value in enumerate(values):
            if statuses[i] is not None:
                continue
            coefficient, basis = value.as_coeff_Mul()
            match = next((j for j in remaining if reference[j].as_coeff_Mul()[0] == coefficient
                          or reference[j].as_coeff_Mul()[1] == basis), None)
            if match is not None:
                statuses[i] = "sai"
                remaining.remove(match)
        for i in range(len(written)):
            if statuses[i] is None:
                statuses[i] = "sai" if remaining else "thua"
                if remaining:
                    remaining.pop(0)
        return {"terms": [{"index": i + 1, "text": term, "status": statuses[i]} for i, term in enumerate(written)],
                "missing_count": len(remaining)}
    except Exception:  # Không để ngoại lệ chứa đầu vào/đáp án đi ra khỏi sandbox.
        return {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "DAU_VAO_KHONG_HOP_LE"}
