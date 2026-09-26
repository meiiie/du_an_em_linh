# -*- coding: utf-8 -*-
"""Chuẩn hóa cách viết Việt Nam / LaTeX MathLive sang chuỗi mà bộ kiểm Tầng 1 đọc được.

Không đoán khi mơ hồ: trả None (THAT_BAI) để tầng trên ghi KHONG_KIEM_DUOC.
"""
import re
import unicodedata

NORMALIZER_VERSION = "norm-0.1"


def _nfc(s):
    return unicodedata.normalize("NFC", s or "")


def normalize_expr(raw):
    """Biểu thức (đạo hàm, giá trị). Trả chuỗi sympy hoặc None."""
    if raw is None:
        return None
    t = _nfc(str(raw)).strip()
    if not t:
        return None
    t = t.replace("$", "")
    t = t.replace("\\left", "").replace("\\right", "")
    t = t.replace("−", "-").replace("–", "-").replace("—", "-")
    t = re.sub(r"^[yY]\s*'\s*=\s*", "", t)
    t = re.sub(r"^[yY]\s*\\prime\s*=\s*", "", t)
    t = re.sub(r"^f\s*'\s*\(\s*x\s*\)\s*=\s*", "", t)
    t = re.sub(r"^y\s*'\s*\(\s*x\s*\)\s*=\s*", "", t)
    t = t.replace("\\cdot", "*").replace("\\times", "*").replace("\\dfrac", "\\frac")
    for _ in range(6):
        n = re.sub(r"\\frac\s*\{([^{}]+)\}\{([^{}]+)\}", r"(\1)/(\2)", t)
        if n == t:
            break
        t = n
    t = re.sub(r"\\sqrt\s*\{([^{}]+)\}", r"sqrt(\1)", t)
    t = t.replace("^{", "**{").replace("^", "**")
    t = t.replace("{", "").replace("}", "")
    t = t.replace("\\", "")
    t = re.sub(r"(\d)\s*,\s*(\d)", r"\1.\2", t)
    t = re.sub(r"\s+", "", t)
    t = re.sub(r"(\d)([a-zA-Z(])", r"\1*\2", t)
    t = re.sub(r"([a-zA-Z])(\()", r"\1*\2", t)
    for name in ("sqrt", "Abs", "log", "ln", "sin", "cos", "tan", "cot", "exp"):
        t = t.replace(name + "*(", name + "(")
    t = re.sub(r"(\))(\()", r"\1*\2", t)
    t = re.sub(r"(\))([a-zA-Z])", r"\1*\2", t)
    if not t or not re.fullmatch(r"[0-9a-zA-Z+\-*/().,_=<>!| ]+", t):
        # vẫn cho ** và dấu chấm
        if not re.fullmatch(r"[0-9a-zA-Z+\-*/().,_*=<>]+", t):
            return None
    return t


def normalize_domain(raw):
    """Tập xác định -> cú pháp parse_tap ('R', 'R \\ {1}', '(-oo; 0] U [2; +oo)')."""
    if raw is None:
        return None
    t = _nfc(str(raw)).strip()
    if not t:
        return None
    t = t.replace("$", "")
    t = t.replace("\\left", "").replace("\\right", "")
    t = re.sub(r"^[Dd]\s*=\s*", "", t)
    t = t.replace("\\mathbb{R}", "R").replace("\\mathbb R", "R").replace("ℝ", "R")
    t = t.replace("\\setminus", " \\ ")
    t = t.replace("\\cup", " U ").replace("∪", " U ")
    t = t.replace("\\{", "{").replace("\\}", "}")
    t = t.replace("\\infty", "oo").replace("∞", "oo")
    t = t.replace("−", "-").replace("–", "-")
    t = t.replace("+oo", "+oo")
    t = re.sub(r"(\d)\s*,\s*(\d)", r"\1.\2", t)
    t = re.sub(r"\s+", " ", t).strip()
    t = t.replace("R \\", "R \\")
    # khoảng trắng quanh \ 
    t = re.sub(r"R\s*\\\s*", "R \\ ", t)
    t = re.sub(r"\s*U\s*", " U ", t)
    t = re.sub(r"\s+", " ", t).strip()
    if t in ("R", "mathbbR"):
        return "R"
    if t.startswith("R \\") or " U " in t or t[:1] in "([":
        return t
    if re.fullmatch(r"R(\\.*)?", t.replace(" ", "")):
        return t
    return t if t else None


def normalize_number_token(raw):
    t = normalize_expr(raw)
    if t is None:
        return None
    t = t.replace(" ", "")
    if re.fullmatch(r"[+\-]?(?:\d+(?:\.\d+)?|\d+/\d+|\(\d+\)/\(\d+\))", t):
        return t
    return t
