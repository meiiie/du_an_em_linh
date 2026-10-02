# -*- coding: utf-8 -*-
"""Job kiem_dong_cong_thuc (ADR 013): kiểm từng dòng bảng công thức của lớp lúc khóa.

Tầng 1 — máy kiểm, không LLM:
  DANG_THUC   dòng dạng `(E)' = R` (lũy thừa, tổng, hiệu, tích, thương, hằng số nhân…). E, R đọc bằng bộ phân tích an
              toàn của tầng 1 (F-01): u, v (cả u(x), v(x)) là hàm ký hiệu; u', v' là đạo hàm; n nguyên dương; k, c hằng
              số. DAT chỉ khi d/dx E − R rút gọn bằng 0 (chứng minh bằng CAS) VÀ mỗi câu lời của dòng là câu đọc đã
              kiểm của chính E (DANH_MUC_CAU_DOC). SAI chỉ khi thế hàm mẫu ra hai vế khác nhau tại một điểm hữu tỉ.
  DINH_LI     thế giới đóng cho định lí: máy đọc từng mệnh đề của dòng (đơn điệu, dấu hiệu cực trị, định nghĩa điểm tới
              hạn). DAT chỉ khi MỌI mệnh đề đọc được trọn và khớp danh mục định lí SGK của chủ đề (DANH_MUC_DINH_LI).
              Đọc trọn nghĩa là: đúng chiều suy ra («A chỉ khi B» là A ⇒ B), hai vế cùng một khoảng («khoảng đó» trỏ
              về khoảng đứng trước), và mỗi vế khớp trọn một mẫu có vị trí: chủ ngữ của «đổi dấu» là y' / đạo hàm, vế
              điều kiện chỉ gồm dấu của y', vế đơn điệu chỉ gồm chủ ngữ hàm số. Bộ hàm mẫu chỉ dùng để tìm phản ví dụ
              cho SAI. Câu máy không đọc trọn (phủ định, lượng từ, điều kiện tại một điểm, từ lạ) → KHONG_KIEM_DUOC,
              không bao giờ DAT.
  KHONG_BIET  loại máy chưa biết → KHONG_KIEM_DUOC (thêm loại mới là việc của lab Kiểm định).
Tầng 2 — tài liệu được phép (quyền dùng hợp lệ như `verify.py`; `chua_ro` không làm căn cứ):
  đẳng thức: có đoạn phát biểu trọn công thức của dòng («nhãn: $công thức$», công thức đóng khung khớp trọn, nhãn không
              phủ định) VÀ mỗi câu của phát biểu trùng trọn một câu của đoạn;
  định lí: MỌI mệnh đề của dòng có đoạn phát biểu cùng mệnh đề (cùng điều kiện, cùng chiều suy ra; đoạn nói về một
              khoảng cụ thể không đỡ định lí tổng quát).
Core chỉ khóa bảng khi mọi dòng DAT ở cả hai tầng (`specs/001-lat-cat-doc/contracts/math-v1.md`).

Quyết định chờ chủ repo (rà math-verifier #101): dòng cực trị viết dạng tóm tắt của bảng công thức («+ → − : cực đại»,
không nêu giả thiết f liên tục tại x0 thuộc khoảng của tập xác định) được nhận như định lí SGK; câu nói ngược giả thiết
(«kể cả khi x0 không thuộc tập xác định») là SAI.
"""
import functools
import re
import unicodedata

from app.verify import QUYEN_HOP_LE

SO_DONG_TOI_DA = 60
SO_TAI_LIEU_TOI_DA = 30
SO_DOAN_TOI_DA = 3000
DO_DAI_LATEX_TOI_DA = 400
DO_DAI_LOI_TOI_DA = 1000
DO_DAI_DOAN_TOI_DA = 4000
# Mệnh đề dài hơn thế này không phải một phát biểu định lí (là văn xuôi): không đem qua các regex của parser.
DO_DAI_MENH_DE_TOI_DA = 400

# Danh mục định lí của chủ đề (SGK Toán 12, ứng dụng đạo hàm), trên một KHOẢNG K của tập xác định. Mệnh đề đơn điệu
# viết (điều kiện, kết luận); D = (dấu, chặt, chỉ bằng 0 tại hữu hạn điểm), M = hướng đơn điệu ngặt.
DANH_MUC_DINH_LI = {
    "DD1": "y' > 0 trên K ⇒ hàm đồng biến trên K (y' < 0 ⇒ nghịch biến)",
    "DD2": "y' ≥ 0 trên K và y' = 0 chỉ tại hữu hạn điểm ⇒ đồng biến trên K (y' ≤ 0 … ⇒ nghịch biến)",
    "DD3": "hàm đồng biến trên K ⇒ y' ≥ 0 trên K (nghịch biến ⇒ y' ≤ 0)",
    "CT1": "y' đổi dấu từ + sang − khi x qua x0 ⇒ x0 là điểm cực đại; từ − sang + ⇒ cực tiểu",
    "CT2": "y'(x0) = 0 mà y' không đổi dấu khi x qua x0 ⇒ x0 không là điểm cực trị",
    "TH": "điểm tới hạn là điểm thuộc tập xác định mà tại đó y' = 0 hoặc y' không xác định",
}
# Câu đọc đã kiểm của công thức (thế giới đóng cho lời của dòng đẳng thức). Máy không đọc nghĩa lời tiếng Việt, nên mỗi
# câu trong phát biểu của dòng phải là một câu đọc đã kiểm của chính biểu thức được lấy đạo hàm (khóa, so bằng CAS).
# Nguồn nguyên văn: bảng khóa của v0 (apps/web/scripts/seed.ts@3bfc584, dòng 612–614) và bản vá sp-tai-lieu-0001 của
# lab Sư phạm (rà math-verifier, #100). Thêm câu mới là việc của lab Kiểm định, kèm lượt rà math-verifier.
DANH_MUC_CAU_DOC = {
    "x^n": ("Đạo hàm của x mũ n là n nhân x mũ n trừ 1.", "Hằng số có đạo hàm bằng 0."),
    "u+v": ("Đạo hàm của tổng bằng tổng các đạo hàm.",),
    "u/v": ("Với thương, tử là u'v trừ uv', mẫu là v bình.",),
}
# Nhãn trước công thức trong tài liệu («nhãn: $công thức$»): khớp trọn một tên đã kiểm của chính quy tắc (khóa E như
# DANH_MUC_CAU_DOC). Nguồn nguyên văn: bản vá sp-tai-lieu-0001 (#100). Nhãn khác («Đạo hàm của tổng khác tổng các đạo
# hàm», «… có u khác 0», tên quy tắc khác) không làm căn cứ; thêm tên là việc của lab Kiểm định.
DANH_MUC_NHAN = {
    "x^n": ("Đạo hàm lũy thừa", "Đạo hàm lũy thừa, với n nguyên dương"),
    "k*u": ("Hằng số nhân với hàm",),
    "u+v": ("Đạo hàm tổng",),
    "u-v": ("Với hiệu cũng vậy", "Đạo hàm hiệu"),
    "u/v": ("Đạo hàm thương", "Đạo hàm thương, tại các điểm có v khác 0"),
}
_DD_TRONG_DANH_MUC = {}
for _h in (1, -1):
    _DD_TRONG_DANH_MUC[(("D", _h, True, False), ("M", _h))] = "DD1"
    _DD_TRONG_DANH_MUC[(("D", _h, False, True), ("M", _h))] = "DD2"
    _DD_TRONG_DANH_MUC[(("M", _h), ("D", _h, False, False))] = "DD3"


class _MayMauThuan(Exception):
    """Hai cách kiểm của máy cho kết quả khác nhau: không kết luận DAT hay SAI."""


# ------------------------------------------------------------------ chuẩn hóa
def _nfc(t):
    t = unicodedata.normalize("NFC", str(t or ""))
    for a, b in (("′", "'"), ("’", "'"), ("−", "-"), ("–", "-"), ("⩾", "≥"), ("⩽", "≤"), ("·", "*")):
        t = t.replace(a, b)
    return t


_LATEX_TU = (
    ("\\Longleftrightarrow", " ⇔ "), ("\\Leftrightarrow", " ⇔ "), ("\\iff", " ⇔ "),
    ("\\Longrightarrow", " ⇒ "), ("\\Rightarrow", " ⇒ "), ("\\implies", " ⇒ "),
    ("\\rightarrow", " → "), ("\\to", " → "),
    ("\\geqslant", "≥"), ("\\leqslant", "≤"), ("\\geq", "≥"), ("\\ge", "≥"), ("\\leq", "≤"), ("\\le", "≤"),
    ("\\neq", "≠"), ("\\ne", "≠"), ("\\infty", "∞"), ("\\in", " ∈ "),
)


_BO_NGOAC = re.compile(r"\\(?:left|right)(?![a-zA-Z])")


def _chu(t):
    """LaTeX lẫn lời → chuỗi chữ thường để nhận mệnh đề."""
    t = _BO_NGOAC.sub(" ", _nfc(t))
    t = re.sub(r"\^\s*\{\s*\\prime\s*\}", "'", t).replace("\\prime", "'")
    t = re.sub(r"\\(?:text|mathrm|textrm|mbox)\s*\{([^{}]*)\}", r" \1 ", t)
    for a, b in _LATEX_TU:
        t = t.replace(a, b)
    for a in ("\\,", "\\;", "\\!", "\\ ", "\\quad", "\\qquad"):
        t = t.replace(a, " ")
    t = t.replace(">=", "≥").replace("<=", "≤").replace("=>", "⇒").replace("->", "→")
    return re.sub(r"\s+", " ", t).strip().lower()


def _khoa_cong_thuc(s):
    return re.sub(r"\s+", "", _BO_NGOAC.sub("", _nfc(s)))


def _khoa_loi(s):
    return re.sub(r"\s+", " ", _nfc(s).lower()).strip().rstrip(" .;:")


def _tach_menh_de(text):
    """Tách theo «.», «;», xuống dòng; không cắt «;» trong ngoặc (khoảng «(a; b)») hay dấu chấm thập phân."""
    out, sau, dau = [], 0, 0
    for i, ch in enumerate(text):
        if ch in "([{":
            sau += 1
        elif ch in ")]}":
            sau = max(0, sau - 1)
        elif ch == "\n" or (ch == ";" and sau == 0) or (ch == "." and not (0 < i < len(text) - 1 and text[i - 1].isdigit() and text[i + 1].isdigit())):
            doan = text[dau:i].strip()
            if doan:
                out.append((dau, doan))
            dau = i + 1
    doan = text[dau:].strip()
    if doan:
        out.append((dau, doan))
    return out


def _cau_cua_loi(s):
    return [c for c in (x.strip() for x in re.split(r"(?<=[.;])\s+", _nfc(s))) if c]


# ------------------------------------------------------------------ đẳng thức (E)' = R
_DANG = re.compile(r"^\s*\((?P<e>.+)\)\s*'\s*=\s*(?P<r>.+?)\s*$")
_TOKEN = re.compile(r"\s*(D[uv]|\d+|[A-Za-z]+|\*\*|[-+*/^()])")
_TEN_DANG_THUC = frozenset("xnkcuv")


def _ngoac(s, i):
    while i < len(s) and s[i] == " ":
        i += 1
    if i >= len(s) or s[i] != "{":
        return None, i
    sau = 0
    for j in range(i, len(s)):
        if s[j] == "{":
            sau += 1
        elif s[j] == "}":
            sau -= 1
            if sau == 0:
                return s[i + 1:j], j + 1
    return None, len(s)


def _bo_frac(s):
    s = s.replace("\\dfrac", "\\frac").replace("\\tfrac", "\\frac")
    for _ in range(10):
        j = s.find("\\frac")
        if j < 0:
            return s
        tu, k = _ngoac(s, j + 5)
        mau, k = _ngoac(s, k) if tu is not None else (None, k)
        if tu is None or mau is None:
            return None
        s = s[:j] + "((" + tu + ")/(" + mau + "))" + s[k:]
    return None


def _toan_hang_cuoi(t):
    return t == ")" or t[0].isdigit() or t[0].isalpha()


def _toan_hang_dau(t):
    return t == "(" or t[0].isdigit() or t[0].isalpha()


def _bieu_thuc(s):
    """Một vế (đã bỏ \\frac, ngoặc nhọn) → chuỗi cho bộ phân tích an toàn; None nếu có tên hay ký hiệu lạ."""
    s = re.sub(r"([uv])\s*'", r" D\1 ", s)
    s = re.sub(r"(?<![A-Za-z])(D[uv]|[uv])\s*\(\s*x\s*\)", r" \1 ", s).strip()  # u(x), u'(x) → u, Du
    if "'" in s or "\\" in s or not s or re.search(r"(?<![A-Za-z])(D[uv]|[uv])\s*\(", s):
        return None
    toks, i = [], 0
    while i < len(s):
        m = _TOKEN.match(s, i)
        if not m:
            return None
        t, i = m.group(1), m.end()
        if t.isalpha() and t not in ("Du", "Dv"):
            if not set(t) <= _TEN_DANG_THUC:
                return None
            toks.extend(t)  # «uv» là u·v
        else:
            toks.append(t)
    out = []
    for t in toks:
        if out and _toan_hang_cuoi(out[-1]) and _toan_hang_dau(t):
            out.append("*")
        out.append(t)
    return "".join(out)


def _tach_dang_thuc(latex):
    s = _BO_NGOAC.sub(" ", _nfc(latex))
    s = re.sub(r"\^\s*\{\s*\\prime\s*\}", "'", s).replace("\\prime", "'")
    s = s.replace("\\cdot", "*").replace("\\times", "*")
    for a in ("\\,", "\\;", "\\!", "\\ "):
        s = s.replace(a, " ")
    s = _bo_frac(s)
    if s is None:
        return None
    m = _DANG.match(s.replace("{", "(").replace("}", ")"))
    if not m:
        return None
    e, r = _bieu_thuc(m.group("e")), _bieu_thuc(m.group("r"))
    return (e, r) if e and r else None


def _kiem_dang_thuc(e_str, r_str, loi=""):
    import sympy as sp
    from app.paths import load_kiem

    kt = load_kiem()
    x = sp.Symbol("x", real=True)
    n = sp.Symbol("n", integer=True, positive=True)
    k = sp.Symbol("k", real=True)
    c = sp.Symbol("c", real=True)
    u, v = sp.Function("u")(x), sp.Function("v")(x)
    them = {"x": x, "n": n, "k": k, "c": c, "u": u, "v": v, "Du": sp.Derivative(u, x), "Dv": sp.Derivative(v, x)}
    doc_duoc = {"E": e_str, "R": r_str}
    try:
        e = kt.phan_tich_an_toan(e_str, them)
        r = kt.phan_tich_an_toan(r_str, them)
    except ValueError as ex:  # DauVaoKhongHopLe kế thừa ValueError
        return {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Công thức không đọc được an toàn (F-01): %s" % str(ex)[:120]}
    if e.has(sp.Derivative):
        return {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Vế trái đã chứa đạo hàm; máy chỉ kiểm dạng (E)' = R."}
    if _khong_xac_dinh(sp, e) or _khong_xac_dinh(sp, r):
        # (1/0)' = 0: SymPy đọc 1/0 thành zoo và đạo hàm của hằng ra 0 — không được để phép lấy đạo hàm xóa chỗ vô nghĩa.
        return {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Biểu thức không xác định ở đâu cả (chia cho 0, vô cực).", "may_doc": doc_duoc}
    hieu = sp.diff(e, x) - r
    pv = _phan_vi_du_dang_thuc(hieu, x, n, k, c, u, v)
    if pv:
        return {"trang_thai": "SAI", "can_cu": "Thế hàm mẫu vào hai vế: khác nhau.", "phan_vi_du": pv, "may_doc": doc_duoc}
    try:
        bang_0 = sp.simplify(hieu) == 0
    except Exception:
        bang_0 = False
    if not bang_0:
        return {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Không rút gọn được về 0 và không tìm thấy phản ví dụ.", "may_doc": doc_duoc}
    duoc_phep = set()
    for khoa in _khop_danh_muc(sp, kt, them, e):
        duoc_phep.update(_khoa_loi(c) for c in DANH_MUC_CAU_DOC.get(khoa, ()))
    la = [c for c in _cau_cua_loi(loi) if _khoa_loi(c) not in duoc_phep]
    if la:
        return {"trang_thai": "KHONG_KIEM_DUOC",
                "ly_do": "Công thức đúng nhưng câu «%s» của phát biểu chưa có trong danh mục câu đọc đã kiểm của công thức này; "
                         "máy không đọc nghĩa lời (thêm câu qua lab Kiểm định)." % la[0][:120]}
    return {"trang_thai": "DAT", "muc_bang_chung": "CAS",
            "can_cu": "SymPy: d/dx(%s) − (%s) rút gọn bằng 0 với u(x), v(x) ký hiệu, n nguyên dương%s." % (
                e_str, r_str, "; lời của dòng khớp danh mục câu đọc đã kiểm" if loi.strip() else "")}


def _khop_danh_muc(sp, kt, them, e):
    """Khóa của DANH_MUC_CAU_DOC / DANH_MUC_NHAN có biểu thức bằng E (so bằng CAS, nên u(x)+v(x) và u+v cùng khóa)."""
    out = []
    for khoa in sorted(set(DANH_MUC_CAU_DOC) | set(DANH_MUC_NHAN)):
        try:
            if sp.simplify(e - kt.phan_tich_an_toan(_bieu_thuc(khoa), them)) == 0:
                out.append(khoa)
        except ValueError:
            continue
    return out


def nhan_cua_cong_thuc(latex):
    """Tên đã kiểm (DANH_MUC_NHAN) của quy tắc mà dòng LaTeX phát biểu; rỗng nếu dòng không phải quy tắc trong danh mục."""
    dt = _tach_dang_thuc(latex) if latex else None
    if not dt:
        return frozenset()
    import sympy as sp
    from app.paths import load_kiem

    kt = load_kiem()
    x = sp.Symbol("x", real=True)
    u, v = sp.Function("u")(x), sp.Function("v")(x)
    them = {"x": x, "n": sp.Symbol("n", integer=True, positive=True), "k": sp.Symbol("k", real=True),
            "c": sp.Symbol("c", real=True), "u": u, "v": v, "Du": sp.Derivative(u, x), "Dv": sp.Derivative(v, x)}
    try:
        e = kt.phan_tich_an_toan(dt[0], them)
    except ValueError:
        return frozenset()
    return frozenset(_khoa_nhan(t) for khoa in _khop_danh_muc(sp, kt, them, e) for t in DANH_MUC_NHAN.get(khoa, ()))


def _khoa_nhan(s):
    return re.sub(r"\s+", " ", _nfc(s).lower()).strip().rstrip(":").strip()


def _khong_xac_dinh(sp, bt):
    """Biểu thức có zoo / nan / ∞, hoặc mẫu (sau khi quy đồng) đồng nhất bằng 0: không xác định tại điểm nào."""
    if bt.has(sp.zoo, sp.nan, sp.oo, -sp.oo):
        return True
    try:
        return sp.simplify(sp.fraction(sp.together(bt))[1]) == 0
    except Exception:
        return True


def _phan_vi_du_dang_thuc(hieu, x, n, k, c, u, v):
    import sympy as sp

    mau = (
        (x ** 3 + 2 * x + 5, x ** 2 + x + 3, 3, sp.Rational(7, 3), -2),
        (x ** 2 + 1, x + 2, 4, 2, 5),
        (x ** 3 - x, x ** 2 + 1, 2, sp.Rational(-3), 1),
    )
    diem = (sp.Rational(1, 2), sp.Integer(1), sp.Rational(3, 2), sp.Integer(2), sp.Rational(-1, 3))
    for u0, v0, n0, k0, c0 in mau:
        bt = hieu.subs({u: u0, v: v0}).doit().subs({n: n0, k: k0, c: c0})
        for d in diem:
            if v0.subs(x, d) == 0:
                continue
            gt = sp.simplify(bt.subs(x, d))
            if gt.is_number and gt.is_finite and gt.is_real and gt != 0:
                return {"u": str(u0), "v": str(v0), "n": int(n0), "k": str(k0), "c": int(c0), "x": str(d),
                        "hieu_hai_ve": str(gt)}
    return None


# ------------------------------------------------------------------ định lí: thuật ngữ và cụm cố định
_THUAT_NGU = re.compile(
    r"đạo hàm|y\s*'|f\s*'|cực|đồng biến|nghịch biến|\btăng\b|\bgiảm\b|tới hạn|giá trị|gtln|gtnn|tiệm cận|nghiệm|"
    r"\bmọi\b|\bluôn\b|tất cả|tồn tại|xác định|liên tục|đổi dấu|\bdấu\b|dương|\bâm\b|[≥≤><=⇒⇔→∈]|\d")
_KE_THUA = r"theo cùng quy tắc|tương tự|cũng vậy|như trên"
_PHU_DINH_DD = re.compile(r"(?:không|chưa|chẳng)\s+(?:chắc\s+)?(?:phải\s+)?(?:là\s+)?(?:hàm\s+(?:số\s+)?)?(?:đồng biến|nghịch biến|tăng|giảm)")
_DIEM = re.compile(r"(?:y|f)\s*'\s*\(\s*(?!x\s*\))|\btại\s+(?:x\s*=|x0\b|x_0|x\s*_\s*0|điểm\b)")
_MIEN = re.compile(r"tập xác định|miền xác định|\btxđ\b|\btrên\s+d\b|\\mathbb\s*\{\s*r\s*\}\s*\\setminus|ℝ\s*\\|\br\s*\\\s*\{")
_DAU_MUT = r"[-+]?\s*(?:\d+(?:[.,]\d+)?|∞|[a-z](?:_?\d)?)"
# Khoảng tường minh chỉ có dạng «(a; b)». Ngoặc chứa thứ khác («(ℝ\{0})», «(x ≠ 0)», «(không xác định)») không phải khoảng.
_KHOANG_TM = r"\(\s*" + _DAU_MUT + r"\s*;\s*" + _DAU_MUT + r"\s*\)"
_KHOANG = re.compile(
    r"trên\s+(?:từng|mỗi)\s+khoảng(?:\s+xác định|\s+của\s+(?:tập xác định|txđ|d\b)|\s+(?:đó|này)\b)?|"
    r"với\s+mọi\s+x\s+(?:thuộc|∈)\s+(?:khoảng\s+)?(?:k\b|" + _KHOANG_TM + r"|đó\b|này\b)|"
    r"trên\s+(?:một\s+)?(?:khoảng|đoạn|nửa khoảng)(?:\s+(?:đó|này|k\b|" + _KHOANG_TM + r"))?|trên\s+k\b|"
    r"trên\s+" + _KHOANG_TM + r"|trên\s+(?:r|ℝ)\b|trên\s+\\mathbb\s*\{\s*r\s*\}")
_DK_KY_HIEU = re.compile(
    r"(?:y|f)\s*'(?:\s*\(\s*x\s*\))?\s*(≥|>|≤|<)\s*0(?P<hoac>\s*(?:hoặc|hay)\s*(?:(?:y|f)\s*'(?:\s*\(\s*x\s*\))?\s*)?(?:=|bằng)\s*0)?")
_DK_LOI = re.compile(
    r"(?:đạo hàm(?:\s+của\s+hàm(?:\s+số)?)?(?:\s+(?:f|y))?|(?:y|f)\s*'(?:\s*\(\s*x\s*\))?)\s+"
    r"(không âm|không dương|lớn hơn hoặc bằng (?:0|không)|nhỏ hơn hoặc bằng (?:0|không)|bé hơn hoặc bằng (?:0|không)|"
    r"dương|âm|lớn hơn (?:0|không)|nhỏ hơn (?:0|không)|bé hơn (?:0|không))"
    r"(?P<hoac>\s*(?:hoặc|hay)\s*(?:bằng\s*0|=\s*0))?")
# «y' = 0 chỉ tại hữu hạn điểm»: phải có phần «= 0 / bằng 0». «y' ≥ 0 tại hữu hạn điểm» là câu khác (sai), không đọc.
_HUU_HAN = re.compile(
    r"(?:,\s*)?(?:và\s+)?(?:chỉ\s+)?(?:(?:y|f)\s*'(?:\s*\(\s*x\s*\))?\s*(?:=|bằng)\s*0|(?:đạo hàm\s+)?bằng\s+0)\s+"
    r"(?:chỉ\s+)?tại\s+(?:một\s+số\s+)?hữu hạn\s+điểm(?:\s+(?:của|thuộc)\s+(?:khoảng\s+)?k\b)?")
_HUU_HAN_PHU_DINH = re.compile(r"(?:không|chưa)\s+(?:chỉ\s+)?(?:tại\s+)?hữu hạn|vô\s+(?:hạn|số)")
_DON_DIEU = re.compile(r"đồng biến|nghịch biến|\btăng\b|\bgiảm\b")


_PHU_DINH = re.compile(r"\b(?:không|chưa|chẳng)\b")

# Đọc theo mẫu có vị trí, không theo túi từ. Vế điều kiện chỉ gồm cụm dấu của y' (và khoảng, «y' = 0 chỉ tại hữu hạn
# điểm»): sau khi bỏ các cụm đó chỉ được còn «trên» (của «trên tập xác định»). Vế đơn điệu chỉ gồm chủ ngữ hàm số và
# «đồng biến / nghịch biến». Thừa số khác («f(x)f'(x) > 0», «hàm số x f(x)») làm vế không đọc được.
_DU_DIEU_KIEN = frozenset({"trên"})
_CHU_NGU_DD = re.compile(r"(?:(?:hàm(?: số)?(?: (?:f|y))?(?: ?\( ?x ?\))?|f ?\( ?x ?\)|f|y|nó)(?: sẽ)?(?: là)?)?(?: trên)?")


def _con_thuat_ngu(t):
    """Phần còn lại sau khi bỏ các cụm đã nhận diện còn thuật ngữ toán hay từ phủ định (vd «không suy ra»)."""
    t = re.sub(r"[\s,.:;()]+", " ", t)
    return bool(_THUAT_NGU.search(t) or _PHU_DINH.search(t))


def _tu_la(t, dem):
    """Còn từ ngoài danh sách từ đệm `dem`: máy không đọc trọn mệnh đề."""
    return any(w not in dem for w in re.sub(r"[\s,.:;()]+", " ", t).split())


def _khoa_mot(s):
    """Một cụm khoảng → khóa: 'TUNG' (từng khoảng xác định), 'DO' (khoảng đó), khoảng tường minh, 'K', 'R', 'CHUNG:…'."""
    if re.search(r"(?:từng|mỗi)\s+khoảng", s):
        return "TUNG"
    if re.search(r"\b(?:đó|này)\s*$", s):
        m = re.search(r"nửa khoảng|đoạn", s)
        return "DO:" + m.group(0) if m else "DO"
    m = re.search(r"\(([^()]*)\)", s)
    if m:
        return "(%s)" % re.sub(r"\s+", "", m.group(1))
    if re.search(r"\bk\s*$", s):
        return "K"
    if re.search(r"(?:\br|ℝ|\\mathbb\s*\{\s*r\s*\})\s*$", s):
        return "R"
    m = re.search(r"nửa khoảng|khoảng|đoạn", s)
    return "CHUNG:" + m.group(0) if m else "?" + s


def _khoang_chung(k):
    """Khoảng tổng quát của định lí (K, «một khoảng», không nêu): đoạn tài liệu nêu khoảng này đỡ được mọi khoảng."""
    return k is None or k == "K" or k.startswith("CHUNG:")


def _khoa_khoang(t):
    """Bỏ các cụm khoảng / tập xác định của một vế → (khóa, phần còn lại). Khóa None khi vế không nêu khoảng, 'MIEN'
    cho tập xác định (có thể không liên thông), False khi một vế nêu hai khoảng khác nhau."""
    khoa = set()

    def lay(m):
        khoa.add(_khoa_mot(m.group(0)))
        return " "

    def lay_mien(_):
        khoa.add("MIEN")
        return " "

    t = _MIEN.sub(lay_mien, _KHOANG.sub(lay, t))
    do = {k for k in khoa if k.startswith("DO")}
    co = khoa - do
    if len(co) > 1:
        return False, t
    return (next(iter(co)) if co else (next(iter(do)) if do else None)), t


def _loai_khoang(k):
    """Danh từ của khóa khoảng mà «… đó» trỏ về được: «khoảng», «đoạn», «nửa khoảng»; tập xác định thì không có."""
    if k.startswith("CHUNG:"):
        return k[len("CHUNG:"):]
    return None if k == "MIEN" else "khoảng"


_TIEN_DE = re.compile(r"(?:(?:với|cho)\s+hàm(?:\s+số)?(?:\s+(?:y|f))?\s+(?:xác định|có đạo hàm)\s+)?(?P<k>.+)")


def _khoa_tien_de(t):
    """Phần đứng trước «nếu / khi … thì»: rỗng, hoặc chỉ nêu khoảng chung cho hai vế («với hàm số xác định trên một
    khoảng:»). Trả khóa khoảng; None khi rỗng; False khi còn nội dung khác (phủ định, giả thiết lạ…)."""
    t = re.sub(r"[\s,:]+", " ", t).strip()
    if not t:
        return None
    m = _TIEN_DE.fullmatch(t)
    k = m.group("k") if m else t
    return _khoa_mot(k) if _KHOANG.fullmatch(k) else False


def _ve(t, truoc=None):
    """Một vế → (('M', hướng) | ('D', dấu, chặt, hữu hạn), khóa khoảng) hoặc None nếu không đọc trọn."""
    t = t.strip()
    if _PHU_DINH_DD.search(t) or _DIEM.search(t) or _HUU_HAN_PHU_DINH.search(t):
        return None
    khoa, t = _khoa_khoang(t)
    if khoa is False:
        return None
    dk = []

    def lay_ky_hieu(m):
        dk.append((1 if m.group(1) in "≥>" else -1, m.group(1) in "><" and not m.group("hoac")))
        return " "

    def lay_loi(m):
        tu = m.group(1)
        dau = 1 if tu in ("không âm", "dương") or tu.startswith("lớn") else -1
        dk.append((dau, tu not in ("không âm", "không dương") and "hoặc bằng" not in tu and not m.group("hoac")))
        return " "

    t = _DK_KY_HIEU.sub(lay_ky_hieu, t)
    t = _DK_LOI.sub(lay_loi, t)
    dd = _DON_DIEU.findall(t)
    if dk and dd:
        return None
    if dk:
        if len(set(dk)) != 1:
            return None
        dau, chat = dk[0]
        huu_han = bool(_HUU_HAN.search(t))
        t = _HUU_HAN.sub(" ", t)
        if re.search(_KE_THUA, t):
            if truoc is None or truoc[0] != "D":
                return None
            huu_han = truoc[3]
            t = re.sub(_KE_THUA, " ", t)
        if _tu_la(t, _DU_DIEU_KIEN):
            return None
        return ("D", dau, chat, False if chat else huu_han), khoa
    if len(dd) == 1:
        con = re.sub(r"[\s,.:;]+", " ", _DON_DIEU.sub(" ", t)).strip()
        if not _CHU_NGU_DD.fullmatch(con):
            return None
        return ("M", 1 if dd[0] in ("đồng biến", "tăng") else -1), khoa
    return None


_CHI_KHI = re.compile(r"\bchỉ\s+(?:khi|nếu)\b")


def _tach_suy_ra(cl):
    """Một mệnh đề → danh sách (điều kiện, kết luận, tiền đề, điều kiện đứng trước?); rỗng khi không rõ chiều suy ra.
    «A chỉ khi B» là A ⇒ B (điều kiện cần), ngược chiều với «A khi B». Tiền đề là phần đứng trước «nếu / khi … thì»,
    phải được đọc trọn. Cờ cuối cho biết thứ tự trong câu, để «khoảng đó» chỉ trỏ về khoảng đứng trước nó."""
    m = re.fullmatch(r"(.*?)\s*(?:⇔|khi và chỉ khi|nếu và chỉ nếu)\s*(.*)", cl)
    if m:
        return [(m.group(2), m.group(1), "", False), (m.group(1), m.group(2), "", True)]
    if _CHI_KHI.search(cl):
        m = re.fullmatch(r"(.+?)\s*\bchỉ\s+(?:khi|nếu)\b\s*(.+)", cl)
        return [(m.group(1), m.group(2), "", True)] if m and not _CHI_KHI.search(m.group(2)) else []
    m = re.fullmatch(r"(.*?)\s*(?:⇒|suy ra)\s*(.*)", cl)
    if m:
        return [(m.group(1), m.group(2), "", True)]
    m = re.fullmatch(r"(.*?)\b(?:nếu|khi)\b(.*?)\bthì\b(.*)", cl)
    if m:
        return [(m.group(2), m.group(3), m.group(1), True)]
    m = re.fullmatch(r"(.*?)\bkhi\b(.*)", cl)
    if m:
        return [(m.group(2), m.group(1), "", False)]
    m = re.fullmatch(r"(.+?)\bnếu\b(.+)", cl)
    if m:
        return [(m.group(2), m.group(1), "", False)]
    m = re.fullmatch(r"(.*?)\bthì\b(.*)", cl)
    if m:
        return [(m.group(1), m.group(2), "", True)]
    return []


def _don_dieu_cua_menh_de(cl, truoc):
    """Mọi cặp suy ra của mệnh đề phải đọc trọn; trả danh sách (điều kiện, kết luận, khóa khoảng) hoặc None.
    Hai vế (và tiền đề) phải nói về cùng một khoảng: nêu khoảng khác nhau thì không chứng minh được cùng miền. «Khoảng
    đó» phải trỏ về một khoảng đứng TRƯỚC nó trong câu («y' > 0 trên khoảng đó thì đồng biến trên ℝ» không đọc)."""
    cap = _tach_suy_ra(cl)
    if not cap:
        return None
    out = []
    for a, c, tien, dk_truoc in cap:
        kt = _khoa_tien_de(tien)
        va, vc = _ve(a, truoc), _ve(c, truoc)
        if kt is False or not va or not vc or {va[0][0], vc[0][0]} != {"D", "M"}:
            return None
        khoa, truoc_k = set(), None
        for k in (kt, va[1], vc[1]) if dk_truoc else (kt, vc[1], va[1]):
            if k is None:
                continue
            if k.startswith("DO"):
                # «… đó» trỏ về khoảng đứng trước, cùng danh từ («đoạn đó» không trỏ về một khoảng)
                if truoc_k is None or _loai_khoang(truoc_k) != (k[len("DO:"):] or "khoảng"):
                    return None
                continue
            khoa.add(k)
            truoc_k = k
        if len(khoa) > 1:
            return None
        out.append((va[0], vc[0], next(iter(khoa)) if khoa else None))
        truoc = va[0] if va[0][0] == "D" else vc[0]
    return out


_DUONG_AM = r"(?:dương|\+)\s*(?:sang|→)\s*(?:âm|-)"
_AM_DUONG = r"(?:âm|-)\s*(?:sang|→)\s*(?:dương|\+)"
_DOI_DAU = _DUONG_AM + "|" + _AM_DUONG
# Câu nói ngược giả thiết của dấu hiệu cực trị: kết luận «vẫn đúng» cả khi x0 nằm ngoài tập xác định (SAI, phản ví dụ 1/x²).
_NGUOC_GIA_THIET = re.compile(
    r"kể cả\s+(?:khi\s+)?(?:(?:x0|x_0|điểm(?:\s+đó)?)\s+)?(?:không thuộc|nằm ngoài|ngoài)\s+(?:tập xác định|txđ|d\b)")
# «khi x qua x0», «khi đi qua một điểm trong»: thuộc mẫu đổi dấu, không phải từ nối «khi».
_QUA = re.compile(
    r"\bkhi\s+(?:x\s+)?(?:đi\s+)?qua(?:\s+(?:một\s+)?điểm(?:\s+trong)?)?(?:\s+(?:x0|x_0))?|(?:\bx\s+)?(?:đi\s+)?\bqua\s+(?:x0|x_0)")
# Chủ ngữ của «đổi dấu / không đổi dấu» chỉ là đạo hàm: y', f'(x), «đạo hàm (của hàm số f)». «Hàm số đổi dấu», «y đổi
# dấu», «đạo hàm của đạo hàm đổi dấu» là câu khác (sai), không đọc.
_DAO_HAM = r"(?:y\s*'(?:\s*\(\s*x\s*\))?|f\s*'(?:\s*\(\s*x\s*\))?|đạo hàm(?:\s+của\s+hàm(?:\s+số)?(?:\s+(?:f|y))?)?)"
_X0 = r"(?:x0|x_0)"
_CT_DK = re.compile(
    r"(?:" + _DAO_HAM + r"\s+)?(?:đổi\s+dấu\s+|đổi\s+)?(?:từ\s+)?(?P<mau>" + _DOI_DAU + r")(?:\s+tại\s+(?:" + _X0 + r"|điểm\s+đó))?")
_CT_KL = re.compile(
    r"(?:(?:" + _X0 + r"|đó|nó)\s+)?(?:(?:hàm(?:\s+số)?(?:\s+(?:f|y))?|f|y)\s+)?(?:đạt\s+|có\s+)?(?:là\s+)?(?:một\s+)?"
    r"(?:điểm\s+)?(?P<kl>cực đại|cực tiểu)(?:\s+(?:tại\s+(?:" + _X0 + r"|điểm\s+đó|đó)|của\s+hàm(?:\s+số)?(?:\s+(?:f|y))?))?")
_KD_DK = re.compile(
    r"(?:(?:y|f)\s*'(?:\s*\(\s*" + _X0 + r"\s*\))?|đạo hàm(?:\s+tại\s+" + _X0 + r")?)\s*(?:=|bằng)\s*0(?:\s+tại\s+" + _X0 + r")?"
    r"\s+(?:mà|và)\s+(?:" + _DAO_HAM + r"\s+)?không\s+đổi\s+dấu(?:\s+tại\s+" + _X0 + r")?")
# Kết luận của CT2, mang nghĩa tại x0: «(x0) không là (điểm) cực trị», «chưa phải cực trị», «hàm không đạt cực trị tại
# đó», «không có cực trị (tại x0)». «Hàm số không có cực trị» (nói cả hàm) và «chưa chắc / có thể là cực trị» không đọc.
_TAI_X0 = r"(?:\s+tại\s+(?:" + _X0 + r"|đó|điểm\s+đó))"
_KD_KL_DUNG = re.compile(
    r"(?:" + _X0 + r"\s+)?(?:không|chưa)\s+(?:phải\s+)?(?:là\s+)?(?:điểm\s+)?cực trị(?:\s+của\s+hàm(?:\s+số)?)?" + _TAI_X0 + r"?"
    r"|(?:hàm(?:\s+số)?\s+)?không\s+(?:đạt|có)\s+cực trị" + _TAI_X0 + r"|không\s+có\s+cực trị")
_KD_KL_SAI = re.compile(
    r"(?:" + _X0 + r"\s+)?(?:vẫn|luôn|cũng)\s+(?:là\s+)?(?:điểm\s+)?cực trị" + _TAI_X0 + r"?"
    r"|(?:hàm(?:\s+số)?\s+)?(?:vẫn|luôn|cũng)\s+(?:đạt|có)\s+cực trị" + _TAI_X0 + r"?")


def _gon(t):
    return re.sub(r"\s+", " ", t).strip(" ,")


def _mot_chieu(t):
    """(điều kiện, kết luận) của một mảnh cực trị; thêm «A : B», «A là B» của dạng tóm tắt. None khi không rõ chiều."""
    cap = _tach_suy_ra(t)
    if not cap:
        m = re.fullmatch(r"(.*?)\s*(?::|\blà\b)\s*(.*)", t)
        cap = [(m.group(1), m.group(2), "", True)] if m else []
    if len(cap) != 1 or cap[0][2].strip():
        return None
    return _gon(cap[0][0]), _gon(cap[0][1])


def _qua_diem_khac_x0(t):
    """«khi x qua một điểm» mà câu lại nói về x0: hai điểm không gắn với nhau, không đọc."""
    return bool(re.search(r"qua\s+(?:một\s+)?điểm", t) and re.search(r"\bx_?0\b", t))


def _mot_manh_cuc_tri(manh):
    """CT1 đúng chiều và đúng mẫu: «(y' / đạo hàm) đổi dấu từ + sang − ⇒ x0 là (điểm) cực đại». ('CT', mẫu, kết luận)."""
    if _qua_diem_khac_x0(manh):
        return None
    chieu = _mot_chieu(_QUA.sub(" ", manh))
    if not chieu:
        return None
    m_dk, m_kl = _CT_DK.fullmatch(chieu[0]), _CT_KL.fullmatch(chieu[1])
    if not m_dk or not m_kl:
        return None
    return ("CT", 1 if re.fullmatch(_DUONG_AM, m_dk.group("mau")) else -1, 1 if m_kl.group("kl") == "cực đại" else -1)


def _khong_doi_dau(cl):
    """CT2 đúng chiều và đúng mẫu: «y'(x0) = 0 mà y' không đổi dấu ⇒ x0 không là cực trị» ([('KD', True)]), hoặc câu
    nói ngược «… thì vẫn là cực trị» ([('KD', False)]). Điều kiện phải nêu y' = 0 (hàm có đạo hàm tại x0)."""
    if _qua_diem_khac_x0(cl):
        return None
    chieu = _mot_chieu(_QUA.sub(" ", cl))
    if not chieu or not _KD_DK.fullmatch(chieu[0]):
        return None
    if _KD_KL_DUNG.fullmatch(chieu[1]):
        return [("KD", True)]
    if _KD_KL_SAI.fullmatch(chieu[1]):
        return [("KD", False)]
    return None


def _cuc_tri_cua_menh_de(cl):
    """Dấu hiệu cực trị: mỗi mảnh là một suy ra đúng chiều (CT1), hoặc cả mệnh đề là CT2.
    Trả danh sách ('CT', mẫu, kết luận) / ('KD', đúng?) / ('NGUOC_GIA_THIET',) hoặc None nếu không đọc trọn."""
    if re.search(r"⇔|khi và chỉ khi|nếu và chỉ nếu", cl):
        return None
    cl = re.sub(r"\s*,\s*(thì|suy ra|⇒)", r" \1", cl)
    if re.search(r"không đổi dấu", cl):
        return _khong_doi_dau(cl)
    out = []
    for manh in re.split(r",|\bvà\b|\bcòn\b|\bnhưng\b", cl):
        if not manh.strip():
            continue
        if _NGUOC_GIA_THIET.fullmatch(manh.strip()):
            out.append(("NGUOC_GIA_THIET",))
            continue
        ct = _mot_manh_cuc_tri(manh)
        if ct is None:
            return None
        out.append(ct)
    return out or None


_Y0_TH = r"(?:y\s*'|đạo hàm)\s*(?:=|bằng)\s*0"
_KXD_TH = r"(?:y\s*'|đạo hàm)\s+không\s+(?:xác định|tồn tại)"
_MIEN_TH = r"thuộc\s+(?:tập xác định|txđ|d)"
_DAU_TH = r"điểm tới hạn(?:\s+của\s+hàm(?:\s+số)?)?\s+"
_CAC = r"(?:(?:các|những)\s+)?"
_TAI_DO = r"(?:mà\s+)?(?:tại\s+(?:đó|điểm đó)\s+)?"
# Định nghĩa điểm tới hạn đọc trọn theo hai dạng (thế giới đóng): «là điểm thuộc TXĐ mà tại đó y' = 0 hoặc y' không xác
# định» và «gồm nghiệm của y' (= 0) và điểm thuộc TXĐ mà y' không xác định». Dạng khác (thiếu, «và» thay «hoặc») không đọc.
_TH_DINH_NGHIA = (
    re.compile(_DAU_TH + r"là\s+" + _CAC + r"điểm\s+" + _MIEN_TH + r"\s+" + _TAI_DO + _Y0_TH + r"\s+(?:hoặc|hay)\s+" + _KXD_TH),
    re.compile(_DAU_TH + r"gồm\s+" + _CAC + r"nghiệm\s+của\s+(?:y\s*'|đạo hàm)(?:\s*(?:=|bằng)\s*0)?\s+và\s+" + _CAC
               + r"điểm\s+" + _MIEN_TH + r"\s+" + _TAI_DO + _KXD_TH),
)
_TH_TOM_TAT = re.compile(_Y0_TH + r"\s+(?:hoặc|hay)\s+" + _KXD_TH)
# Định nghĩa nói ngược điều kiện «thuộc tập xác định»: chỉ xét câu có dạng định nghĩa («điểm tới hạn là / gồm …»), để câu
# đúng như «khi tìm điểm tới hạn, loại các điểm không thuộc tập xác định» không bị xếp SAI.
_TH_DAU_DINH_NGHIA = re.compile(_DAU_TH + r"(?:là|gồm|bao gồm|có thể là)\b")
_TH_SAI = re.compile(r"không thuộc\s+(?:tập xác định|txđ|d\b)|ngoài\s+(?:tập xác định|txđ)|hàm số không xác định")


def _toi_han_cua_menh_de(cl, co_tieu_de):
    """('TH',) khi mệnh đề là định nghĩa điểm tới hạn đọc trọn; ('TH_TOM_TAT',) cho dạng ký hiệu «y' = 0 hoặc y' không
    xác định» của dòng có tiêu đề điểm tới hạn; ('TH_SAI',) khi định nghĩa nói điểm ngoài tập xác định (hay nơi hàm
    không xác định) là điểm tới hạn."""
    t = cl.strip(" .;:,")
    if _TH_DAU_DINH_NGHIA.match(t) and _TH_SAI.search(t):
        if re.search(r"loại\s+(?:trừ|bỏ|ra)|\btrừ\b|bị\s+loại|không\s+(?:tính|kể)", t):
            return None  # nói điều kiện bằng cách loại trừ: máy không đọc trọn, không xếp SAI
        con = re.sub(_KXD_TH, " ", _TH_SAI.sub(" ", t))
        return [("TH_SAI",)] if not _PHU_DINH.search(con) else None  # «không phải là điểm không thuộc…»: không đọc
    if "điểm tới hạn" in t:
        return [("TH",)] if any(g.fullmatch(t) for g in _TH_DINH_NGHIA) else None
    if co_tieu_de and _TH_TOM_TAT.fullmatch(t):
        return [("TH_TOM_TAT",)]
    return None


def _doc_menh_de(cl, truoc, co_tieu_de):
    """Một mệnh đề → (danh sách mệnh đề đọc được hoặc None, mệnh đề điều kiện để «theo cùng quy tắc» trỏ về)."""
    if len(cl) > DO_DAI_MENH_DE_TOI_DA:
        return None, truoc
    dd = _don_dieu_cua_menh_de(cl, truoc)
    if dd:
        for dk, kl, _ in dd:
            truoc = dk if dk[0] == "D" else kl
        return [("DD", dk, kl, khoang) for dk, kl, khoang in dd], truoc
    return (_cuc_tri_cua_menh_de(cl) or _toi_han_cua_menh_de(cl, co_tieu_de)), truoc


def _doc_dong(text, tieu_de=""):
    """Đọc mọi mệnh đề của dòng. Trả (danh sách mệnh đề, danh sách mệnh đề không đọc được)."""
    co_tieu_de = "tới hạn" in _chu(tieu_de)
    menh, khong_doc, truoc = [], [], None
    for _, cl in _tach_menh_de(_chu(text)):
        ms, truoc = _doc_menh_de(cl, truoc, co_tieu_de)
        if ms:
            menh.extend(ms)
        else:
            # Thế giới đóng: mệnh đề không nhận ra đều là «chưa đọc trọn», kể cả khi không có từ nào trong _THUAT_NGU
            # («Hàm số này là hàm chẵn.»). Đoạn tài liệu bỏ qua danh sách này; dòng bảng thì không DAT được.
            khong_doc.append(cl)
    return list(dict.fromkeys(menh)), khong_doc


# ------------------------------------------------------------------ bộ hàm mẫu (chỉ để tìm phản ví dụ)
class _Mau:
    """Hàm mẫu của chủ đề, tính một lần cho mỗi job. Chỉ dùng để sinh phản ví dụ cho SAI, không bao giờ để cho DAT."""

    def __init__(self):
        import sympy as sp

        self.sp = sp
        x = self.x = sp.Symbol("x", real=True)
        R, mo, oo = sp.S.Reals, sp.Interval.open, sp.oo
        self.khoang = [
            (x ** 3, R), (x, R), (-x, R), (sp.Integer(1), R), (x ** 3 + x, R), (-x ** 3, R), (x ** 5, R),
            (x - sp.sin(x), R), (x ** 3 - 3 * x, R), (x ** 3 - 3 * x, mo(1, oo)), (x ** 3 - 3 * x, mo(-1, 1)),
            (x ** 2, R), (x ** 2, mo(0, oo)), (x ** 2, mo(-oo, 0)),
            (x ** 4 - 2 * x ** 2, mo(0, 1)), (x ** 4 - 2 * x ** 2, mo(1, oo)),
            ((x + 1) / (x - 1), mo(1, oo)), ((2 * x - 1) / (x + 1), mo(-1, oo)),
        ]
        # Tập xác định không liên thông: (hàm, điểm gián đoạn, cặp điểm hai bên để chứng tỏ không đơn điệu trên cả tập)
        self.mien_roi = [((x + 1) / (x - 1), 1, (0, 2)), ((2 * x - 1) / (x + 1), -1, (-2, 0))]
        self.cuc_tri = [x ** 3 - 3 * x, -x ** 3 + 3 * x, x ** 4 - 2 * x ** 2, x ** 3, x ** 4]
        self._nho = {}

    def _d(self, f):
        return self.sp.diff(f, self.x)

    def dk(self, f, I, ve):
        sp, x = self.sp, self.x
        _, dau, chat, huu_han = ve
        d = self._d(f) * dau
        if chat:
            return sp.solveset(d <= 0, x, I) == sp.S.EmptySet
        if sp.solveset(d < 0, x, I) != sp.S.EmptySet:
            return False
        if huu_han:
            z = sp.solveset(sp.Eq(d, 0), x, I)
            return z == sp.S.EmptySet or isinstance(z, sp.FiniteSet)
        return True

    def don_dieu(self, f, I, huong):
        """Đơn điệu ngặt trên KHOẢNG I (hàm giải tích): đạo hàm theo hướng không âm và không đồng nhất bằng 0.
        Đối chiếu hai chiều với lưới giá trị hữu tỉ chính xác; lệch nhau thì máy mâu thuẫn."""
        sp, x = self.sp, self.x
        d = self._d(f) * huong
        kq = sp.solveset(d < 0, x, I) == sp.S.EmptySet and sp.simplify(d) != 0
        a = I.start if I.start.is_finite else (I.end - 6 if I.end.is_finite else sp.Integer(-3))
        b = I.end if I.end.is_finite else a + 6
        gt = [f.subs(x, a + (b - a) * sp.Rational(j, 25)) for j in range(1, 25)]
        luoi = all(bool((gt[j + 1] - gt[j]) * huong > 0) for j in range(len(gt) - 1))
        if kq and not luoi:
            raise _MayMauThuan("lưới giá trị trái với dấu đạo hàm của %s" % f)
        if luoi and not kq and not isinstance(f, sp.Integer):
            raise _MayMauThuan("lưới giá trị đơn điệu mà dấu đạo hàm nói không, ở %s" % f)
        return kq

    def dung(self, ve, f, I):
        key = (ve, str(f), str(I))
        if key not in self._nho:
            self._nho[key] = self.don_dieu(f, I, ve[1]) if ve[0] == "M" else self.dk(f, I, ve)
        return self._nho[key]

    def diem_cuc_tri(self):
        """(hàm, điểm, dấu trái, dấu phải, là cực đại, là cực tiểu); dấu lấy tại trung điểm giữa các nghiệm liên tiếp."""
        if "CT" not in self._nho:
            sp, x = self.sp, self.x
            out = []
            for f in self.cuc_tri:
                d = self._d(f)
                ng = sorted(sp.solveset(sp.Eq(d, 0), x, sp.S.Reals))
                for i, r in enumerate(ng):
                    trai = (ng[i - 1] + r) / 2 if i > 0 else r - 1
                    phai = (r + ng[i + 1]) / 2 if i + 1 < len(ng) else r + 1
                    st, sph = int(sp.sign(d.subs(x, trai))), int(sp.sign(d.subs(x, phai)))
                    ft, fr, fp = f.subs(x, trai), f.subs(x, r), f.subs(x, phai)
                    # các mẫu đơn điệu giữa hai nghiệm liên tiếp: so với trung điểm là đủ
                    out.append((f, r, st, sph, bool(ft < fr and fp < fr), bool(ft > fr and fp > fr)))
            self._nho["CT"] = out
        return self._nho["CT"]


def _ten_ve(ve):
    if ve[0] == "M":
        return "đồng biến" if ve[1] == 1 else "nghịch biến"
    ten = {(1, True): "y' > 0", (1, False): "y' ≥ 0", (-1, True): "y' < 0", (-1, False): "y' ≤ 0"}[(ve[1], ve[2])]
    return ten + (", y' = 0 chỉ tại hữu hạn điểm" if ve[3] else "")


def _phan_vi_du_don_dieu(dk, kl, khoang, mau):
    """Phản ví dụ đúng mệnh đề đã viết: khoảng tổng quát thì mọi khoảng của bộ mẫu; ℝ thì chỉ mẫu trên ℝ; tập xác
    định thì mẫu có tập xác định rời; khoảng cụ thể hay «từng khoảng xác định» thì không tìm (bộ mẫu không bám)."""
    sp, x = mau.sp, mau.x
    if khoang == "MIEN":
        if dk[0] == "D" and kl[0] == "M":
            for f, gian_doan, (a, b) in mau.mien_roi:
                D = sp.Union(sp.Interval.open(-sp.oo, gian_doan), sp.Interval.open(gian_doan, sp.oo))
                fa, fb = f.subs(x, a), f.subs(x, b)
                if mau.dk(f, D, dk) and bool((fb - fa) * kl[1] <= 0):
                    return {"ham": "y = %s" % f, "mien": "ℝ \\ {%s}" % gian_doan,
                            "cap_diem": [str(a), str(b)], "gia_tri": [str(fa), str(fb)]}
        return None
    if not _khoang_chung(khoang) and khoang != "R":
        return None
    for f, I in mau.khoang:
        if khoang == "R" and I != sp.S.Reals:
            continue
        if mau.dung(dk, f, I) and not mau.dung(kl, f, I):
            return {"ham": "y = %s" % f, "khoang": str(I)}
    return None


def _phan_vi_du_cuc_tri(muc, mau):
    if muc[0] == "NGUOC_GIA_THIET":
        return {"ham": "y = 1/x**2", "x0": "0", "doi_dau": "+ sang -",
                "thuc_te": "x0 = 0 không thuộc tập xác định nên không là điểm cực trị"}
    for f, r, trai, phai, cd, ct in mau.diem_cuc_tri():
        if muc[0] == "CT" and trai * phai < 0 and (1 if trai > 0 else -1) == muc[1]:
            noi = cd if muc[2] == 1 else ct
            if not noi:
                return {"ham": "y = %s" % f, "x0": str(r), "doi_dau": "%s sang %s" % ("+" if trai > 0 else "-", "+" if phai > 0 else "-"),
                        "thuc_te": "cực đại" if cd else ("cực tiểu" if ct else "không là cực trị")}
        if muc[0] == "KD" and trai == phai and trai != 0 and not muc[1] and not (cd or ct):
            return {"ham": "y = %s" % f, "x0": str(r), "la_cuc_tri": False}
    return None


def _tang_1_dinh_li(menh, khong_doc, mau):
    if khong_doc:
        return {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Có mệnh đề máy chưa đọc trọn, nên không kiểm cả dòng: «%s»." % khong_doc[0][:160]}
    co_th = any(m[0] == "TH" for m in menh)
    can_cu = []
    try:
        for m in menh:
            if m[0] == "DD":
                _, dk, kl, khoang = m
                mien = khoang == "MIEN"
                ma = None if mien else _DD_TRONG_DANH_MUC.get((dk, kl))
                if ma:
                    can_cu.append("«%s ⇒ %s» khớp %s." % (_ten_ve(dk), _ten_ve(kl), ma))
                    continue
                pv = _phan_vi_du_don_dieu(dk, kl, khoang, mau)
                if pv:
                    return {"trang_thai": "SAI", "can_cu": "Phản ví dụ cho «%s ⇒ %s»%s." % (_ten_ve(dk), _ten_ve(kl), " trên tập xác định" if mien else ""),
                            "phan_vi_du": pv}
                return {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "«%s ⇒ %s»%s không có trong danh mục định lí." % (_ten_ve(dk), _ten_ve(kl), " trên tập xác định" if mien else "")}
            if m[0] in ("CT", "KD", "NGUOC_GIA_THIET"):
                if (m[0] == "CT" and m[1] == m[2]) or (m[0] == "KD" and m[1]):
                    can_cu.append("Dấu hiệu cực trị khớp %s." % ("CT1" if m[0] == "CT" else "CT2"))
                    continue
                pv = _phan_vi_du_cuc_tri(m, mau)
                if pv:
                    return {"trang_thai": "SAI", "can_cu": "Phản ví dụ cho dấu hiệu cực trị đã viết.", "phan_vi_du": pv}
                return {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Dấu hiệu cực trị không có trong danh mục định lí."}
            if m[0] == "TH_SAI":
                return {"trang_thai": "SAI", "can_cu": "Định nghĩa điểm tới hạn nói ngược điều kiện «thuộc tập xác định».",
                        "phan_vi_du": {"ham": "y = (x + 1)/(x - 1)", "x0": "1",
                                       "thuc_te": "y' không xác định tại 1 nhưng 1 không thuộc tập xác định"}}
            if m[0] == "TH":
                can_cu.append("Định nghĩa điểm tới hạn đủ ba thành phần, khớp TH.")
            if m[0] == "TH_TOM_TAT":
                if not co_th:
                    return {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Dạng ký hiệu của điểm tới hạn cần định nghĩa đầy đủ trong phát biểu."}
                can_cu.append("Dạng ký hiệu tóm tắt định nghĩa TH.")
    except _MayMauThuan as ex:
        return {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Hai cách kiểm của máy mâu thuẫn: %s." % ex}
    return {"trang_thai": "DAT", "muc_bang_chung": "DANH_MUC", "can_cu": " ".join(list(dict.fromkeys(can_cu)))}


# ------------------------------------------------------------------ tầng 2
# Câu nói một điều là sai (khác phủ định thông thường như «không đổi dấu»): «Mệnh đề trên là sai.», «Học sinh hay nhầm…».
_PHU_NHAN = re.compile(r"\b(?:sai|nhầm|ngộ nhận)\b|\b(?:không|chưa)\s+(?:đúng|chính xác)\b")


def _co_cau_phu_nhan(text):
    """Đoạn có mệnh đề máy không đọc thành định lí mà nói điều gì đó sai: phân cực của đoạn không rõ."""
    _, khong_doc = _doc_dong(text)
    return any(_PHU_NHAN.search(c) for c in khong_doc)


def _cac_doan(tai_lieu):
    """Đoạn của tài liệu được phép (tài liệu, đoạn, vị trí, chữ) và danh sách tài liệu bỏ qua."""
    doan, bo_qua = [], []
    for doc in (tai_lieu or [])[:SO_TAI_LIEU_TOI_DA]:
        quyen = str(doc.get("license_status") or doc.get("quyen") or "").strip()
        if quyen not in QUYEN_HOP_LE:
            bo_qua.append({"tai_lieu": doc.get("id"), "ly_do": "quyen_khong_hop_le" if quyen else "khong_khai_quyen"})
            continue
        cac = doc.get("doan")
        if cac is None:
            # Tài liệu chưa chia đoạn: từng mệnh đề (trích ngắn, có vị trí), rồi cả văn bản cho câu dài nhiều mệnh đề.
            text = str(doc.get("text") or doc.get("noi_dung") or "")[:DO_DAI_DOAN_TOI_DA]
            cac = [{"id": None, "vi_tri": pos, "text": cl} for pos, cl in _tach_menh_de(text)] + [{"id": None, "vi_tri": 0, "text": text}]
        if any(_co_cau_phu_nhan(str(d.get("text") or "")[:DO_DAI_DOAN_TOI_DA]) for d in cac):
            # «Mệnh đề trên là sai.» có thể nói về câu bất kỳ trong tài liệu: không dùng tài liệu này làm căn cứ.
            bo_qua.append({"tai_lieu": doc.get("id"), "ly_do": "co_cau_phu_nhan"})
            continue
        for d in cac:
            if len(doan) >= SO_DOAN_TOI_DA:
                break
            doan.append((doc.get("id"), d.get("id"), d.get("vi_tri"), str(d.get("text") or "")[:DO_DAI_DOAN_TOI_DA]))
    return doan, bo_qua


def _trich(tl, dn, vt, text):
    out = {"tai_lieu": tl, "doan": dn, "trich": text[:240]}
    if dn is None:
        out["vi_tri"] = vt
    return out


def _dung_danh_muc(m):
    """Mệnh đề đọc được và đúng theo danh mục (DD1–DD3 trên khoảng, CT1, CT2, TH)."""
    if m[0] == "DD":
        return m[3] != "MIEN" and (m[1], m[2]) in _DD_TRONG_DANH_MUC
    if m[0] == "CT":
        return m[1] == m[2]
    if m[0] == "KD":
        return m[1]
    return m[0] == "TH"


@functools.lru_cache(maxsize=4096)
def _menh_de_doan(text):
    """Mệnh đề làm căn cứ của một đoạn tài liệu: chỉ lấy từ mệnh đề mà MỌI phần đều đúng theo danh mục. «Hàm đồng biến
    ⇔ y' > 0» sai một chiều, nên chiều đúng của nó cũng không làm căn cứ; dạng tóm tắt và mệnh đề sai không tính."""
    out, truoc = set(), None
    for _, cl in _tach_menh_de(_chu(text)):
        ms, truoc_moi = _doc_menh_de(cl, truoc, False)
        if ms and all(_dung_danh_muc(m) for m in ms):
            out.update(ms)
            truoc = truoc_moi
        else:
            truoc = None
    # Lưu theo chữ của đoạn (mỗi job một tiến trình con): mỗi dòng định lí dùng lại, không phân tích lại cả kho đoạn.
    return frozenset(out)


def _ho_tro(p, m):
    """Mệnh đề p của đoạn tài liệu làm căn cứ cho mệnh đề m của dòng: cùng mệnh đề; với đơn điệu, đoạn nêu khoảng
    tổng quát (K, «một khoảng») hoặc đúng khoảng của dòng — đoạn nói về một khoảng cụ thể không đỡ định lí tổng quát."""
    if p[0] != "DD" or m[0] != "DD":
        return p == m
    return p[1:3] == m[1:3] and m[3] != "MIEN" and (p[3] == m[3] or _khoang_chung(p[3]))


_TOAN = re.compile(r"\$\$(.+?)\$\$|\$(.+?)\$|\\\((.+?)\\\)|\\\[(.+?)\\\]")


def _phat_bieu_cong_thuc(text, khoa, nhan_duoc=frozenset()):
    """Đoạn phát biểu công thức có khóa `khoa`: một mệnh đề của đoạn có dạng «nhãn: $công thức$» hay «$công thức$» —
    công thức đóng khung, khớp trọn (không là phần của công thức dài hơn), đứng cuối mệnh đề; nhãn (nếu có) khớp trọn một
    tên đã kiểm của chính quy tắc đó (`nhan_duoc`, từ DANH_MUC_NHAN)."""
    for _, cl in _tach_menh_de(text):
        for m in _TOAN.finditer(cl):
            if _khoa_cong_thuc(next(g for g in m.groups() if g is not None)) != khoa or cl[m.end():].strip(" .;:"):
                continue
            nhan = cl[:m.start()].strip()
            if nhan and (not nhan.endswith(":") or _khoa_nhan(nhan) not in nhan_duoc):
                continue
            return True
    return False


def _co_cau(text, khoa):
    """Đoạn có một câu trùng trọn câu `khoa` của phát biểu (không phải chuỗi con của một câu dài hơn)."""
    return any(_khoa_loi(c) == khoa for c in _cau_cua_loi(text))


def _tang_2(loai, dong, menh, doan):
    if loai == "DANG_THUC":
        can = [("cong_thuc", _khoa_cong_thuc(dong.get("latex")))] + [("loi", _khoa_loi(c)) for c in _cau_cua_loi(dong.get("phat_bieu"))]
        nhan_duoc = nhan_cua_cong_thuc(str(dong.get("latex") or ""))
        trich = []
        for kieu, khoa in can:
            if not khoa:
                continue
            if kieu == "cong_thuc":
                hit = next((d for d in doan if _phat_bieu_cong_thuc(d[3], khoa, nhan_duoc)), None)
            else:
                hit = next((d for d in doan if _co_cau(d[3], khoa)), None)
            if hit is None:
                return {"trang_thai": "KHONG_KIEM_DUOC",
                        "ly_do": "Không có đoạn tài liệu được phép phát biểu trọn %s của dòng." % (
                            "công thức (đóng khung, nhãn là tên đã kiểm của quy tắc)" if kieu == "cong_thuc" else "câu «%s»" % khoa[:80])}
            trich.append(_trich(*hit))
        if not trich:
            return {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Dòng không có công thức hay phát biểu để đối chiếu."}
        return _ket_qua_tang_2(trich)
    if loai == "DINH_LI":
        can = [m for m in menh if m[0] in ("DD", "CT", "KD", "TH")]
        trich = []
        cua_doan = [(d, _menh_de_doan(d[3])) for d in doan]
        for m in can:
            hit = next((d for d, ms in cua_doan if any(_ho_tro(p, m) for p in ms)), None)
            if hit is None:
                return {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Không có đoạn tài liệu được phép phát biểu mệnh đề %s của dòng." % _ten_menh_de(m)}
            trich.append(_trich(*hit))
        if not trich:
            return {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Không có mệnh đề để đối chiếu."}
        return _ket_qua_tang_2(trich)
    return {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Loại dòng máy chưa biết."}


def _ten_menh_de(m):
    if m[0] == "DD":
        khoang = "" if _khoang_chung(m[3]) else " trên %s" % {"MIEN": "tập xác định", "TUNG": "từng khoảng xác định", "R": "ℝ"}.get(m[3], m[3])
        return "«%s ⇒ %s»%s" % (_ten_ve(m[1]), _ten_ve(m[2]), khoang)
    if m[0] == "CT":
        return "«%s ⇒ %s»" % ("+ sang −" if m[1] == 1 else "− sang +", "cực đại" if m[2] == 1 else "cực tiểu")
    if m[0] == "KD":
        return "«y' không đổi dấu ⇒ không là cực trị»"
    return "định nghĩa điểm tới hạn"


def _ket_qua_tang_2(trich):
    duy_nhat = list({(t["tai_lieu"], t["doan"], t.get("vi_tri"), t["trich"]): t for t in trich}.values())
    out = {"trang_thai": "DAT", "trich_dan": duy_nhat[0]}
    if len(duy_nhat) > 1:
        out["trich_dan_them"] = duy_nhat[1:]
    return out


# ------------------------------------------------------------------ job
def _mot_dong(dong, doan, mau):
    latex, loi = str(dong.get("latex") or ""), str(dong.get("phat_bieu") or "")
    if len(latex) > DO_DAI_LATEX_TOI_DA or len(loi) > DO_DAI_LOI_TOI_DA:
        t = {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Dòng quá dài."}
        return "KHONG_BIET", t, dict(t)
    dt = _tach_dang_thuc(latex) if latex else None
    if dt:
        return "DANG_THUC", _kiem_dang_thuc(dt[0], dt[1], loi), _tang_2("DANG_THUC", dong, None, doan)
    menh, khong_doc = _doc_dong(latex + ". " + loi, str(dong.get("tieu_de") or ""))
    if menh:
        return "DINH_LI", _tang_1_dinh_li(menh, khong_doc, mau()), _tang_2("DINH_LI", dong, menh, doan)
    toan = [c for c in khong_doc if _con_thuat_ngu(c)]
    ly_do = ("Máy chưa đọc trọn mệnh đề «%s»: máy chỉ kiểm câu đúng mẫu danh mục (đúng chiều suy ra, hai vế cùng một "
             "khoảng; không phủ định, lượng từ, điều kiện tại một điểm hay từ máy không biết)." % toan[0][:160]
             if toan else "Loại dòng máy chưa biết; thêm loại mới qua lab Kiểm định.")
    t1 = {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": ly_do}
    return "KHONG_BIET", t1, _tang_2("KHONG_BIET", dong, None, doan)


def kiem_dong_cong_thuc(payload):
    doan, bo_qua = _cac_doan(payload.get("tai_lieu"))
    nho = []

    def mau():
        if not nho:
            nho.append(_Mau())
        return nho[0]

    cac_dong = payload.get("dong") or []
    if len(cac_dong) > SO_DONG_TOI_DA:
        # Không cắt im lặng: core khóa bảng khi mọi dòng trả về DAT, nên dòng không kiểm cũng phải có kết quả.
        ly_do = "Bảng có %d dòng, quá giới hạn %d dòng mỗi lần kiểm; không dòng nào được kiểm." % (len(cac_dong), SO_DONG_TOI_DA)
        t = {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": ly_do}
        return {"dong": [{"id": (d or {}).get("id") if isinstance(d, dict) else None, "loai": "KHONG_BIET",
                          "tang1": dict(t), "tang2": dict(t)} for d in cac_dong],
                "bo_qua": bo_qua, "loi": "QUA_NHIEU_DONG"}
    out = []
    for dong in cac_dong:
        loai, t1, t2 = _mot_dong(dong or {}, doan, mau)
        out.append({"id": (dong or {}).get("id"), "loai": loai, "tang1": t1, "tang2": t2})
    return {"dong": out, "bo_qua": bo_qua}
