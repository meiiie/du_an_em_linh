# -*- coding: utf-8 -*-
"""Chuẩn hóa cách viết Việt Nam / LaTeX MathLive sang chuỗi mà bộ kiểm Tầng 1 đọc được.

Không đoán khi mơ hồ: trả None (THAT_BAI) để tầng trên ghi KHONG_KIEM_DUOC.
"""
import re
import unicodedata

NORMALIZER_VERSION = "norm-0.2"


def _nfc(s):
    return unicodedata.normalize("NFC", s or "")


def _nhom(t, i):
    """t[i] == '{' -> (nội dung trong ngoặc cân bằng, chỉ số sau '}'), hoặc None."""
    if i >= len(t) or t[i] != "{":
        return None
    sau = 0
    for j in range(i, len(t)):
        if t[j] == "{":
            sau += 1
        elif t[j] == "}":
            sau -= 1
            if sau == 0:
                return t[i + 1:j], j + 1
    return None


def _bo_trang(t, i):
    while i < len(t) and t[i] == " ":
        i += 1
    return i


def _mo_lenh(t):
    """\\frac{A}{B} -> ((A)/(B)), \\sqrt{A} -> sqrt(A), ^{A} -> **(A); xử lý ngoặc lồng nhau."""
    out = []
    i = 0
    while i < len(t):
        if t.startswith("\\frac", i):
            j = _bo_trang(t, i + 5)
            a = _nhom(t, j)
            if a:
                k = _bo_trang(t, a[1])
                b = _nhom(t, k)
                if b:
                    out.append("((%s)/(%s))" % (_mo_lenh(a[0]), _mo_lenh(b[0])))
                    i = b[1]
                    continue
            return None
        if t.startswith("\\sqrt", i):
            j = _bo_trang(t, i + 5)
            a = _nhom(t, j)
            if a:
                out.append("sqrt(%s)" % _mo_lenh(a[0]))
                i = a[1]
                continue
            return None
        if t[i] == "^":
            j = _bo_trang(t, i + 1)
            a = _nhom(t, j)
            if a:
                inner = _mo_lenh(a[0])
                if inner is None:
                    return None
                out.append("**(%s)" % inner)
                i = a[1]
                continue
            out.append("**")
            i += 1
            continue
        out.append(t[i])
        i += 1
    r = "".join(out)
    return r if "None" not in r else None


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
    t = _mo_lenh(t)
    if t is None:
        return None
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
    return _chi_bieu_thuc_toan(t)


# F-01: chỉ những tên này được đi tiếp tới SymPy. Mọi chữ ghép khác (vd "frac3" do LaTeX hỏng, "__class__")
# làm chuỗi bị từ chối -> None -> KHONG_KIEM_DUOC; không bao giờ SAI vì lỗi chuẩn hoá, không bao giờ eval.
TEN_TRANG = frozenset({"x", "sqrt", "Abs", "log", "ln", "sin", "cos", "tan", "cot", "exp", "pi", "e", "E", "oo"})


def _chi_bieu_thuc_toan(t):
    if not t or len(t) > 300:
        return None
    if not re.fullmatch(r"[0-9a-zA-Z+\-*/().]+", t):
        return None
    # dấu chấm chỉ được nằm giữa hai chữ số (số thập phân)
    if re.search(r"(?<![0-9])\.|\.(?![0-9])", t):
        return None
    for ten in re.findall(r"[A-Za-z]+", t):
        if ten not in TEN_TRANG:
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
