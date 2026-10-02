# -*- coding: utf-8 -*-
"""Job kiem_dong_cong_thuc (ADR 013): kiểm từng dòng bảng công thức của lớp lúc khóa.

Tầng 1 — máy kiểm, không LLM:
  DANG_THUC   dòng dạng `(E)' = R` (lũy thừa, tổng, hiệu, thương, hằng số nhân…). E, R đọc bằng bộ phân tích an toàn
              của tầng 1 (F-01): u, v là hàm ký hiệu u(x), v(x); u', v' là đạo hàm; n nguyên dương; k, c hằng số.
              DAT khi d/dx E − R rút gọn bằng 0. SAI chỉ khi có phản ví dụ cụ thể (thế hàm mẫu, điểm hữu tỉ).
  DINH_LI     loại máy đã biết: đơn điệu (dấu đạo hàm và đồng biến / nghịch biến), cực trị (đổi dấu và cực đại /
              cực tiểu), định nghĩa điểm tới hạn. Mỗi mệnh đề «điều kiện ⇒ kết luận» được so với ngữ nghĩa có sẵn
              của máy rồi tìm phản ví dụ trên bộ hàm mẫu của chủ đề. Không có phản ví dụ → DAT. Đây là kiểm nhất
              quán có giới hạn, không phải chứng minh; căn cứ ghi rõ điều đó.
  KHONG_BIET  loại máy chưa biết → KHONG_KIEM_DUOC (thêm loại mới là việc của lab Kiểm định).
Tầng 2 — tài liệu được phép (quyền dùng hợp lệ như `verify.py`; `chua_ro` không làm căn cứ): có đoạn phát biểu
  quy tắc của dòng. Đẳng thức: đoạn chứa nguyên văn LaTeX hoặc phát biểu của dòng (sau chuẩn hóa). Định lí: đoạn
  có cùng mệnh đề (cùng điều kiện, cùng chiều suy ra).
Core chỉ khóa bảng khi mọi dòng DAT ở cả hai tầng (`specs/001-lat-cat-doc/contracts/math-v1.md`).
"""
import re
import unicodedata

from app.verify import QUYEN_HOP_LE, _huong_cuc_tri, _menh_de

SO_DONG_TOI_DA = 60
SO_TAI_LIEU_TOI_DA = 30
SO_DOAN_TOI_DA = 3000
DO_DAI_LATEX_TOI_DA = 400
DO_DAI_LOI_TOI_DA = 1000
DO_DAI_DOAN_TOI_DA = 4000

GHI_CHU_DINH_LI = "Kiểm nhất quán có giới hạn trên bộ hàm mẫu của chủ đề, không phải chứng minh."


class _MayMauThuan(Exception):
    """Hai cách kiểm của máy cho kết quả khác nhau: không kết luận DAT hay SAI."""


# ------------------------------------------------------------------ chuẩn hóa
def _nfc(t):
    t = unicodedata.normalize("NFC", str(t or ""))
    return t.replace("′", "'").replace("’", "'").replace("−", "-").replace("–", "-")


_LATEX_TU = (
    ("\\Leftrightarrow", " ⇔ "), ("\\iff", " ⇔ "), ("\\Rightarrow", " ⇒ "), ("\\implies", " ⇒ "),
    ("\\rightarrow", " → "), ("\\to", " → "), ("\\geq", "≥"), ("\\ge", "≥"), ("\\leq", "≤"), ("\\le", "≤"),
    ("\\neq", "≠"), ("\\ne", "≠"), ("\\infty", "∞"),
)


def _chu(t):
    """LaTeX lẫn lời → chuỗi chữ thường để nhận mệnh đề."""
    t = _nfc(t).replace("\\left", " ").replace("\\right", " ")
    t = re.sub(r"\\(?:text|mathrm|textrm|mbox)\s*\{([^{}]*)\}", r" \1 ", t)
    for a, b in _LATEX_TU:
        t = t.replace(a, b)
    for a in ("\\,", "\\;", "\\!", "\\ "):
        t = t.replace(a, " ")
    t = t.replace(">=", "≥").replace("<=", "≤").replace("=>", "⇒").replace("->", "→")
    return re.sub(r"\s+", " ", t).strip().lower()


def _khoa_cong_thuc(s):
    return re.sub(r"\s+", "", _nfc(s).replace("\\left", "").replace("\\right", ""))


def _khoa_loi(s):
    return re.sub(r"\s+", " ", _nfc(s).lower()).strip().rstrip(" .;:")


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
    s = re.sub(r"([uv])\s*'", r" D\1 ", s).strip()
    if "'" in s or "\\" in s or not s:
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
    s = _nfc(latex).replace("\\left", " ").replace("\\right", " ")
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


def _kiem_dang_thuc(e_str, r_str):
    import sympy as sp
    from app.paths import load_kiem

    kt = load_kiem()
    x = sp.Symbol("x", real=True)
    n = sp.Symbol("n", integer=True, positive=True)
    k = sp.Symbol("k", real=True)
    c = sp.Symbol("c", real=True)
    u, v = sp.Function("u")(x), sp.Function("v")(x)
    them = {"x": x, "n": n, "k": k, "c": c, "u": u, "v": v, "Du": sp.Derivative(u, x), "Dv": sp.Derivative(v, x)}
    try:
        e = kt.phan_tich_an_toan(e_str, them)
        r = kt.phan_tich_an_toan(r_str, them)
    except ValueError as ex:  # DauVaoKhongHopLe kế thừa ValueError
        return {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Công thức không đọc được an toàn (F-01): %s" % str(ex)[:120]}
    if e.has(sp.Derivative):
        return {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Vế trái đã chứa đạo hàm; máy chỉ kiểm dạng (E)' = R."}
    hieu = sp.diff(e, x) - r
    pv = _phan_vi_du_dang_thuc(hieu, x, n, k, c, u, v)
    if pv:
        return {"trang_thai": "SAI", "can_cu": "Thế hàm mẫu vào hai vế: khác nhau.", "phan_vi_du": pv}
    try:
        bang_0 = sp.simplify(hieu) == 0
    except Exception:
        bang_0 = False
    if bang_0:
        return {"trang_thai": "DAT",
                "can_cu": "SymPy: d/dx(%s) − (%s) rút gọn bằng 0 với u(x), v(x) ký hiệu, n nguyên dương." % (e_str, r_str)}
    return {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Không rút gọn được về 0 và không tìm thấy phản ví dụ."}


def _phan_vi_du_dang_thuc(hieu, x, n, k, c, u, v):
    import sympy as sp

    mau = ((x ** 2 + 1, x + 2, 3, 2, 5), (x ** 3 - x, x ** 2 + 1, 2, sp.Rational(-3), 1))
    diem = (sp.Rational(1, 2), sp.Integer(1), sp.Rational(3, 2), sp.Integer(2), sp.Rational(-1, 3))
    for u0, v0, n0, k0, c0 in mau:
        bt = hieu.subs({u: u0, v: v0}).doit().subs({n: n0, k: k0, c: c0})
        for d in diem:
            if v0.subs(x, d) == 0:
                continue
            gt = sp.simplify(bt.subs(x, d))
            if gt.is_number and gt.is_finite and gt != 0:
                return {"u": str(u0), "v": str(v0), "n": int(n0), "k": str(k0), "c": int(c0), "x": str(d),
                        "hieu_hai_ve": str(gt)}
    return None


# ------------------------------------------------------------------ định lí: tách mệnh đề
_DH = r"(?:y\s*'|f\s*'|đạo hàm)"
_KE_THUA = r"theo cùng quy tắc|tương tự|cũng vậy|như trên"


def _tach_suy_ra(cl):
    """Một mệnh đề → danh sách (điều kiện, kết luận) dạng chuỗi."""
    m = re.search(r"(.*?)\s*(?:⇔|khi và chỉ khi)\s*(.*)", cl)
    if m:
        return [(m.group(2), m.group(1)), (m.group(1), m.group(2))]
    m = re.search(r"(.*?)\s*(?:⇒|suy ra)\s*(.*)", cl)
    if m:
        return [(m.group(1), m.group(2))]
    m = re.search(r"\bnếu\b(.*?)\bthì\b(.*)", cl)
    if m:
        return [(m.group(1), m.group(2))]
    m = re.search(r"(.*?)\bkhi\b(.*)", cl)
    if m:
        return [(m.group(2), m.group(1))]
    m = re.search(r"(.*?)\bthì\b(.*)", cl)
    if m:
        return [(m.group(1), m.group(2))]
    return []


def _dau(t):
    """(dấu, chặt) của điều kiện về đạo hàm; dạng phủ định xét trước («không âm» chứa «âm»)."""
    if re.search(r"không âm|≥\s*0|lớn hơn hoặc bằng 0|không nhỏ hơn 0", t):
        return 1, False
    if re.search(r"không dương|≤\s*0|nhỏ hơn hoặc bằng 0|không lớn hơn 0", t):
        return -1, False
    if re.search(r">\s*0|\bdương\b|lớn hơn 0|lớn hơn không", t):
        return 1, True
    if re.search(r"<\s*0|\bâm\b|nhỏ hơn 0|bé hơn 0", t):
        return -1, True
    return None


def _ve(t, truoc=None):
    """Một vế → ('M', hướng) | ('D', dấu, chặt, chỉ bằng 0 tại hữu hạn điểm) | None."""
    db = bool(re.search(r"đồng biến|\btăng\b", t))
    nb = bool(re.search(r"nghịch biến|\bgiảm\b", t))
    d = _dau(t) if re.search(_DH, t) else None
    if (db or nb) and d is None:
        return None if db and nb else ("M", 1 if db else -1)
    if d is not None and not (db or nb):
        huu_han = bool(re.search(r"hữu hạn", t))
        if not huu_han and truoc is not None and re.search(_KE_THUA, t):
            huu_han = truoc[3]
        return ("D", d[0], d[1], huu_han)
    return None


def _menh_de_don_dieu(text):
    out, truoc = [], None
    for _, cl in _menh_de(text):
        for a, c in _tach_suy_ra(cl):
            dk, kl = _ve(a, truoc), _ve(c, truoc)
            if dk and kl and {dk[0], kl[0]} == {"D", "M"}:
                out.append((dk, kl))
                truoc = dk if dk[0] == "D" else kl
    return out


def _menh_de_cuc_tri(text):
    out = []
    for _, cl in _menh_de(text):
        h = _huong_cuc_tri(cl)
        if h is not None:
            out.append(("CT", h))
        elif re.search(r"không đổi dấu", cl) and re.search(r"cực trị", cl):
            out.append(("KD", bool(re.search(r"(chưa|không)\s+(phải|là)\s+(là\s+)?(điểm\s+)?cực trị", cl))))
    return out


def _co_hai_thanh_phan_toi_han(text):
    co_0 = re.search(r"(y\s*'|đạo hàm)\s*(=|bằng)\s*0|nghiệm của (y\s*'|đạo hàm)", text)
    return bool(co_0 and re.search(r"không xác định", text))


def _dinh_nghia_toi_han(text):
    if "điểm tới hạn" not in text:
        return None
    return ("TH", _co_hai_thanh_phan_toi_han(text))


# ------------------------------------------------------------------ định lí: ngữ nghĩa có sẵn và phản ví dụ
class _Mau:
    """Bộ hàm mẫu của chủ đề (đa thức, phân thức bậc nhất trên bậc nhất), tính một lần cho mỗi job."""

    def __init__(self):
        import sympy as sp

        self.sp = sp
        x = self.x = sp.Symbol("x", real=True)
        R, mo, oo = sp.S.Reals, sp.Interval.open, sp.oo
        self.don_dieu = [
            (x ** 3, R), (x, R), (-x, R), (sp.Integer(1), R), (x ** 3 + x, R), (-x ** 3, R), (x ** 5, R),
            (x ** 3 - 3 * x, R), (x ** 3 - 3 * x, mo(1, oo)), (x ** 3 - 3 * x, mo(-1, 1)),
            (x ** 2, R), (x ** 2, mo(0, oo)), (x ** 2, mo(-oo, 0)),
            (x ** 4 - 2 * x ** 2, mo(0, 1)), (x ** 4 - 2 * x ** 2, mo(1, oo)),
            ((x + 1) / (x - 1), mo(1, oo)), ((2 * x - 1) / (x + 1), mo(-1, oo)),
        ]
        self.cuc_tri = [x ** 3 - 3 * x, -x ** 3 + 3 * x, x ** 4 - 2 * x ** 2, x ** 3, x ** 4]
        self._nho = {}

    def dk_dao_ham(self, i, dau, chat, huu_han):
        key = ("D", i, dau, chat, huu_han)
        if key not in self._nho:
            sp, x = self.sp, self.x
            f, I = self.don_dieu[i]
            d = sp.diff(f, x) * dau
            if chat:
                kq = sp.solveset(d <= 0, x, I) == sp.S.EmptySet
            elif sp.solveset(d < 0, x, I) != sp.S.EmptySet:
                kq = False
            elif huu_han:
                z = sp.solveset(sp.Eq(d, 0), x, I)
                kq = z == sp.S.EmptySet or isinstance(z, sp.FiniteSet)
            else:
                kq = True
            self._nho[key] = kq
        return self._nho[key]

    def don_dieu_ngat(self, i, huong):
        """Ngữ nghĩa có sẵn (đúng với đa thức, phân thức liên tục trên khoảng): đơn điệu ngặt theo hướng ⇔ đạo hàm
        theo hướng không âm và chỉ bằng 0 tại hữu hạn điểm. Đối chiếu thêm bằng giá trị hữu tỉ chính xác trên lưới."""
        key = ("M", i, huong)
        if key not in self._nho:
            kq = self.dk_dao_ham(i, huong, False, True)
            if kq and not self._luoi_ngat(i, huong):
                raise _MayMauThuan("lưới giá trị trái với dấu đạo hàm")
            self._nho[key] = kq
        return self._nho[key]

    def _luoi_ngat(self, i, huong):
        sp, x = self.sp, self.x
        f, I = self.don_dieu[i]
        a = I.start if I.start.is_finite else (I.end - 6 if I.end.is_finite else sp.Integer(-3))
        b = I.end if I.end.is_finite else a + 6
        diem = [a + (b - a) * sp.Rational(j, 25) for j in range(1, 25)]
        gt = [f.subs(x, p) for p in diem]
        return all((gt[j + 1] - gt[j]) * huong > 0 for j in range(len(gt) - 1))

    def dung(self, ve, i):
        if ve[0] == "M":
            return self.don_dieu_ngat(i, ve[1])
        return self.dk_dao_ham(i, ve[1], ve[2], ve[3])

    def diem_cuc_tri(self):
        """(hàm, điểm, dấu trái, dấu phải, là cực đại, là cực tiểu) cho mọi nghiệm của y' trong bộ mẫu."""
        if "CT" not in self._nho:
            sp, x = self.sp, self.x
            h = sp.Rational(1, 100)
            out = []
            for f in self.cuc_tri:
                d = sp.diff(f, x)
                for r in sp.solveset(sp.Eq(d, 0), x, sp.S.Reals):
                    trai, phai = sp.sign(d.subs(x, r - h)), sp.sign(d.subs(x, r + h))
                    fr, ft, fp = f.subs(x, r), f.subs(x, r - h), f.subs(x, r + h)
                    out.append((f, r, int(trai), int(phai), bool(ft < fr and fp < fr), bool(ft > fr and fp > fr)))
            self._nho["CT"] = out
        return self._nho["CT"]


def _ten_ve(ve):
    if ve[0] == "M":
        return "đồng biến" if ve[1] == 1 else "nghịch biến"
    ten = {(1, True): "y' > 0", (1, False): "y' ≥ 0", (-1, True): "y' < 0", (-1, False): "y' ≤ 0"}[(ve[1], ve[2])]
    return ten + (", y' = 0 chỉ tại hữu hạn điểm" if ve[3] else "")


def _kiem_don_dieu(dk, kl, mau):
    co_mau = False
    for i, (f, I) in enumerate(mau.don_dieu):
        if not mau.dung(dk, i):
            continue
        co_mau = True
        if not mau.dung(kl, i):
            return {"trang_thai": "SAI",
                    "can_cu": "Phản ví dụ cho «%s ⇒ %s»." % (_ten_ve(dk), _ten_ve(kl)),
                    "phan_vi_du": {"ham": "y = %s" % f, "khoang": str(I)}}
    if not co_mau:
        return {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Không hàm mẫu nào thỏa điều kiện «%s»." % _ten_ve(dk)}
    return {"trang_thai": "DAT", "can_cu": "«%s ⇒ %s»: không có phản ví dụ." % (_ten_ve(dk), _ten_ve(kl))}


def _kiem_cuc_tri(muc, mau):
    for f, r, trai, phai, cd, ct in mau.diem_cuc_tri():
        if muc[0] == "CT" and trai * phai < 0:
            noi_cuc_dai = (trai > 0) == (muc[1] == 1)
            if (cd if noi_cuc_dai else ct) is False:
                return {"trang_thai": "SAI", "can_cu": "Phản ví dụ cho quy tắc đổi dấu và cực trị.",
                        "phan_vi_du": {"ham": "y = %s" % f, "x0": str(r),
                                       "doi_dau": "%s sang %s" % ("+" if trai > 0 else "-", "+" if phai > 0 else "-"),
                                       "thuc_te": "cực đại" if cd else ("cực tiểu" if ct else "không là cực trị")}}
        if muc[0] == "KD" and trai == phai and trai != 0:
            if muc[1] == (cd or ct):
                return {"trang_thai": "SAI", "can_cu": "Phản ví dụ cho «y' bằng 0 mà không đổi dấu».",
                        "phan_vi_du": {"ham": "y = %s" % f, "x0": str(r), "la_cuc_tri": bool(cd or ct)}}
    if muc[0] == "CT":
        return {"trang_thai": "DAT", "can_cu": "Đổi dấu + sang − là cực đại, − sang + là cực tiểu: không có phản ví dụ."}
    return {"trang_thai": "DAT", "can_cu": "y' bằng 0 mà không đổi dấu thì không là cực trị: không có phản ví dụ."}


def _tang_1_dinh_li(dd, ct, th, mau):
    if th is not None and not th[1]:
        return {"trang_thai": "KHONG_KIEM_DUOC",
                "ly_do": "Định nghĩa điểm tới hạn thiếu thành phần (y' = 0 hoặc y' không xác định)."}
    can_cu = []
    try:
        for dk, kl in dd:
            kq = _kiem_don_dieu(dk, kl, mau)
            if kq["trang_thai"] != "DAT":
                return kq
            can_cu.append(kq["can_cu"])
        for muc in ct:
            kq = _kiem_cuc_tri(muc, mau)
            if kq["trang_thai"] != "DAT":
                return kq
            can_cu.append(kq["can_cu"])
    except _MayMauThuan as ex:
        return {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Hai cách kiểm của máy mâu thuẫn: %s." % ex}
    if th is not None:
        can_cu.append("Khớp định nghĩa: điểm thuộc tập xác định có y' = 0 hoặc y' không xác định.")
    return {"trang_thai": "DAT", "can_cu": " ".join(can_cu + [GHI_CHU_DINH_LI])}


# ------------------------------------------------------------------ tầng 2
def _cac_doan(tai_lieu):
    """Đoạn của tài liệu được phép (tài liệu, đoạn, chữ) và danh sách tài liệu bỏ qua."""
    doan, bo_qua = [], []
    for doc in (tai_lieu or [])[:SO_TAI_LIEU_TOI_DA]:
        quyen = str(doc.get("license_status") or doc.get("quyen") or "").strip()
        if quyen not in QUYEN_HOP_LE:
            bo_qua.append({"tai_lieu": doc.get("id"), "ly_do": "quyen_khong_hop_le" if quyen else "khong_khai_quyen"})
            continue
        cac = doc.get("doan")
        if cac is None:
            # Tài liệu chưa chia đoạn: từng mệnh đề (trích ngắn, có vị trí), rồi cả văn bản cho phát biểu dài nhiều câu.
            text = str(doc.get("text") or doc.get("noi_dung") or "")[:DO_DAI_DOAN_TOI_DA]
            cac = [{"id": None, "vi_tri": pos, "text": cl} for pos, cl in _menh_de(text)] + [{"id": None, "vi_tri": 0, "text": text}]
        for d in cac:
            if len(doan) >= SO_DOAN_TOI_DA:
                break
            doan.append((doc.get("id"), d.get("id"), d.get("vi_tri"), str(d.get("text") or "")[:DO_DAI_DOAN_TOI_DA]))
    return doan, bo_qua


def _trich(tai_lieu_id, doan_id, vi_tri, text):
    out = {"tai_lieu": tai_lieu_id, "doan": doan_id, "trich": text[:240]}
    if doan_id is None:
        out["vi_tri"] = vi_tri
    return out


def _tang_2(loai, dong, chi_tiet, doan):
    if loai == "DANG_THUC":
        k_ct, k_loi = _khoa_cong_thuc(dong.get("latex")), _khoa_loi(dong.get("phat_bieu"))
        for tl, dn, vt, text in doan:
            if (k_ct and k_ct in _khoa_cong_thuc(text)) or (k_loi and k_loi in _khoa_loi(text)):
                return {"trang_thai": "DAT", "trich_dan": _trich(tl, dn, vt, text)}
        return {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Không có đoạn tài liệu được phép chứa công thức hay phát biểu của dòng."}
    if loai == "DINH_LI":
        dd, ct, th = chi_tiet
        for tl, dn, vt, text in doan:
            t = _chu(text)
            if (set(dd) & set(_menh_de_don_dieu(t)) or set(ct) & set(_menh_de_cuc_tri(t))
                    or (th is not None and _co_hai_thanh_phan_toi_han(t))):
                return {"trang_thai": "DAT", "trich_dan": _trich(tl, dn, vt, text)}
        return {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Không có đoạn tài liệu được phép phát biểu cùng mệnh đề."}
    return {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Loại dòng máy chưa biết."}


# ------------------------------------------------------------------ job
def _khong_trung(ds):
    """Bỏ mệnh đề trùng (cùng một quy tắc viết ở cả LaTeX lẫn lời), giữ thứ tự."""
    return list(dict.fromkeys(ds))


def _mot_dong(dong, doan, mau):
    latex, loi = str(dong.get("latex") or ""), str(dong.get("phat_bieu") or "")
    if len(latex) > DO_DAI_LATEX_TOI_DA or len(loi) > DO_DAI_LOI_TOI_DA:
        t1 = {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Dòng quá dài."}
        return "KHONG_BIET", t1, {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Dòng quá dài."}
    dt = _tach_dang_thuc(latex) if latex else None
    if dt:
        return "DANG_THUC", _kiem_dang_thuc(*dt), _tang_2("DANG_THUC", dong, None, doan)
    chu = _chu(latex + ". " + loi)
    dd, ct = _khong_trung(_menh_de_don_dieu(chu)), _khong_trung(_menh_de_cuc_tri(chu))
    th = _dinh_nghia_toi_han(chu)
    if dd or ct or th is not None:
        chi_tiet = (dd, ct, th)
        return "DINH_LI", _tang_1_dinh_li(dd, ct, th, mau()), _tang_2("DINH_LI", dong, chi_tiet, doan)
    t1 = {"trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Loại dòng máy chưa biết; thêm loại mới qua lab Kiểm định."}
    return "KHONG_BIET", t1, _tang_2("KHONG_BIET", dong, None, doan)


def kiem_dong_cong_thuc(payload):
    doan, bo_qua = _cac_doan(payload.get("tai_lieu"))
    nho = []

    def mau():
        if not nho:
            nho.append(_Mau())
        return nho[0]

    out = []
    for dong in (payload.get("dong") or [])[:SO_DONG_TOI_DA]:
        loai, t1, t2 = _mot_dong(dong or {}, doan, mau)
        out.append({"id": (dong or {}).get("id"), "loai": loai, "tang1": t1, "tang2": t2})
    return {"dong": out, "bo_qua": bo_qua}
