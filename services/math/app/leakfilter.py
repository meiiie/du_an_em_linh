# -*- coding: utf-8 -*-
"""Bộ lọc đầu ra của gia sư (LeakGuard).

Lớp chính: SymPy theo ngữ cảnh (M3 của Kiểm định, ``loc.py``) — số phải dính đồng biến / cực / nghiệm.
Lớp phụ: so chuỗi LaTeX (M1), ví dụ ``x=1``, ``(1;3)``.
Lớp 0 (29/09, sau thử gia sư giai đoạn 2): chuẩn hóa thêm TRƯỚC khi đưa vào M3/M1:
  - khoảng viết bằng dấu phẩy ``(1, 3)``, "giữa 1 và 3", "between 1 and 3";
  - bất đẳng thức ``x > 3``, ``x < 1``, ``1 < x < 3``, "x lớn hơn / nhỏ hơn một" -> khoảng;
  - số viết bằng chữ tới hàng trăm ("âm mười bảy", "hai mươi mốt", "không phẩy năm"), số tiếng Anh;
  - từ khóa tiếng Anh (increasing, decreasing, local max/min, root...) và cách nói vòng
    ("đi xuống", "điểm cao nhất cục bộ", "hoành độ", "tung độ").
Lớp xác nhận: nếu câu học sinh có số / khoảng mà bản nháp phán "đúng rồi / chính xác / sai rồi" thì chặn
  (bộ lọc cũ không thấy câu học sinh nên xác nhận lọt).
Lớp đếm: "có ba điểm cực trị" khi số lượng đúng bằng số cực trị của bài.

FAIL CLOSED: mọi lỗi bên trong -> cho_phep = False (gia sư dùng câu mẫu an toàn).
Không trả về phần khớp — chỉ cho_phep, để gia sư không nhận lại đáp án.
"""
import re
import unicodedata

from app.dau_vao import DAU_VAO_KHONG_HOP_LE, ly_do_tu_choi
from app.paths import load_loc

_loc = None


def _L():
    global _loc
    if _loc is None:
        _loc = load_loc()
    return _loc


# ------------------------------------------------------------------ số bằng chữ
_VN_DV = {"không": 0, "một": 1, "mốt": 1, "hai": 2, "ba": 3, "bốn": 4, "tư": 4, "năm": 5, "lăm": 5, "sáu": 6,
          "bảy": 7, "bẩy": 7, "tám": 8, "chín": 9}
_VN_TU = set(_VN_DV) | {"mười", "mươi", "trăm", "linh", "lẻ", "phẩy"}
_EN = {"zero": 0, "one": 1, "two": 2, "three": 3, "four": 4, "five": 5, "six": 6, "seven": 7, "eight": 8, "nine": 9,
       "ten": 10, "eleven": 11, "twelve": 12, "thirteen": 13, "fourteen": 14, "fifteen": 15, "sixteen": 16,
       "seventeen": 17, "eighteen": 18, "nineteen": 19, "twenty": 20, "thirty": 30, "forty": 40, "fifty": 50,
       "sixty": 60, "seventy": 70, "eighty": 80, "ninety": 90}
_KICH = {"bằng", "là", "tại", "=", "từ", "đến", "tới", "và", "hoặc", "âm", "trừ", "khoảng", "giữa", "hơn", "độ", "x",
         "có", "phẩy", "số", "is", "at", "equals", "equal", "from", "to", "and", "or", "between", "than", "minus",
         "negative", "of", "value", "roots", "root", "has", "have", "cộng", ">", "<", ">=", "<="}


def _doc_vn(ws):
    """Đọc dãy từ số tiếng Việt -> số (int/float) hoặc None."""
    if "phẩy" in ws:
        i = ws.index("phẩy")
        a, b = _doc_vn(ws[:i]), ws[i + 1:]
        if a is None or not b or any(w not in _VN_DV for w in b):
            return None
        return float("%d.%s" % (a, "".join(str(_VN_DV[w]) for w in b)))
    tong, cur = 0, 0
    i = 0
    while i < len(ws):
        w = ws[i]
        if w == "trăm":
            tong += (cur or 1) * 100
            cur = 0
        elif w == "mười":
            cur += 10
        elif w == "mươi":
            cur = (cur or 1) * 10
        elif w in ("linh", "lẻ"):
            pass
        elif w in _VN_DV:
            cur += _VN_DV[w]
        else:
            return None
        i += 1
    return tong + cur


def _so_chu(t):
    toks = re.findall(r"\S+|\s+", t)
    out = []
    i = 0
    words = [tk for tk in toks]
    while i < len(words):
        tk = words[i]
        core = tk.strip(".,;:!?()").lower()
        if core in _VN_TU and core not in ("phẩy", "linh", "lẻ"):
            # gom dãy từ số
            j, seq, end = i, [], i
            while j < len(words):
                c = words[j].strip(".,;:!?()").lower() if words[j].strip() else None
                if words[j].isspace():
                    j += 1
                    continue
                if c in _VN_TU and words[j].strip(".,;:!?()").lower() == words[j].lower():
                    seq.append(c)
                    end = j
                    j += 1
                    continue
                if c in _VN_TU:  # có dấu câu dính -> dừng sau từ này
                    seq.append(c)
                    end = j
                break
            truoc = ""
            k = len(out) - 1
            while k >= 0 and out[k].isspace():
                k -= 1
            if k >= 0:
                truoc = out[k].strip(".,;:!?()").lower()
            nhieu_tu = len(seq) > 1 and any(w in ("mười", "mươi", "trăm", "phẩy") for w in seq)
            v = _doc_vn(seq) if (truoc in _KICH or nhieu_tu or seq[0] in ("mười",)) else None
            if v is not None:
                last = words[end]
                tail = last[len(last.rstrip(".,;:!?()")):]
                head = words[i][: len(words[i]) - len(words[i].lstrip("(["))]
                out.append(head + (str(v) if not isinstance(v, float) else repr(v)) + tail)
                i = end + 1
                continue
        if core in _EN:
            j, v, end = i, 0, i
            while j < len(words):
                if words[j].isspace():
                    j += 1
                    continue
                c = words[j].strip(".,;:!?()").lower()
                if c in _EN:
                    v += _EN[c]
                    end = j
                    j += 1
                    continue
                c2 = c.split("-")
                if len(c2) == 2 and all(p in _EN for p in c2):
                    v += _EN[c2[0]] + _EN[c2[1]]
                    end = j
                    j += 1
                    continue
                break
            k = len(out) - 1
            while k >= 0 and out[k].isspace():
                k -= 1
            truoc = out[k].strip(".,;:!?()").lower() if k >= 0 else ""
            if truoc in _KICH or core not in ("one",):
                last = words[end]
                tail = last[len(last.rstrip(".,;:!?()")):]
                out.append(str(v) + tail)
                i = end + 1
                continue
        out.append(tk)
        i += 1
    return "".join(out)


_THAY_CUM = [
    # tiếng Anh -> tiếng Việt (để M3 nhận ngữ cảnh)
    (r"\blocal maximum value\b|\bmaximum value\b|\bmax value\b", "giá trị cực đại"),
    (r"\blocal minimum value\b|\bminimum value\b|\bmin value\b", "giá trị cực tiểu"),
    (r"\b(local |relative )?max(imum)?\s+(point\s+)?(is\s+|occurs\s+)?at\b", "cực đại tại"),
    (r"\b(local |relative )?min(imum)?\s+(point\s+)?(is\s+|occurs\s+)?at\b", "cực tiểu tại"),
    (r"\b(local |relative )?max(imum)?\b", "cực đại"),
    (r"\b(local |relative )?min(imum)?\b", "cực tiểu"),
    (r"\bincreasing\b|\bincreases\b|\bgoes up\b", "đồng biến"),
    (r"\bdecreasing\b|\bdecreases\b|\bgoes down\b", "nghịch biến"),
    (r"\broots?\b|\bsolutions?\b|\bcritical points?\b|\bzeros?\b", "nghiệm"),
    (r"\bnegative infinity\b|\bminus infinity\b", "-oo"),
    (r"\b(positive )?infinity\b", "oo"),
    (r"\bnegative\b|\bminus\b", "âm"),
    (r"\bgreater than\b|\bbigger than\b|\blarger than\b", ">"),
    (r"\bless than\b|\bsmaller than\b", "<"),
    # cách nói vòng tiếng Việt
    (r"đi xuống|đi lên", lambda m: "giảm" if "xuống" in m.group(0) else "tăng"),
    (r"điểm cao nhất cục bộ|đỉnh cục bộ", "cực đại"),
    (r"điểm thấp nhất cục bộ|đáy cục bộ", "cực tiểu"),
    (r"hoành độ", "x ="),
    (r"tung độ", "giá trị"),
    (r"lớn hơn hoặc bằng|không nhỏ hơn", ">="),
    (r"nhỏ hơn hoặc bằng|không lớn hơn|bé hơn hoặc bằng", "<="),
    (r"lớn hơn", ">"),
    (r"nhỏ hơn|bé hơn", "<"),
    (r"cộng trừ|±", "±"),
]

_N = r"-?\s*(?:oo|\d+(?:[.,]\d+)?(?:/\d+)?)"


def chuan_hoa_them(text):
    t = unicodedata.normalize("NFC", text or "")
    for a, b in (("−", "-"), ("–", "-"), ("∞", "oo"), ("\\infty", "oo"), ("≥", ">="), ("≤", "<="), ("\\geq", ">="),
                 ("\\leq", "<="), ("\\ge", ">="), ("\\le", "<="), ("\\pm", "±"), ("$", "")):
        t = t.replace(a, b)
    t = t.replace("+oo", "oo")
    low = t.lower()
    for pat, rep in _THAY_CUM:
        low = re.sub(pat, rep, low)
    low = re.sub(r"(âm|trừ)\s+vô\s+(cùng|cực)", "-oo", low)
    low = re.sub(r"(dương\s+)?vô\s+(cùng|cực)", "oo", low)
    low = _so_chu(low)
    low = re.sub(r"(?:âm|trừ)\s+(\d)", r"-\1", low)
    # khoảng dấu phẩy: (1, 3) / [1,3] / (1,3)
    low = re.sub(r"([\(\[])\s*(%s)\s*,\s*(%s)\s*([\)\]])" % (_N, _N), lambda m: "%s%s;%s%s" % (m.group(1), m.group(2), m.group(3), m.group(4)), low)
    # giữa a và b / between a and b
    low = re.sub(r"(?:giữa|between)\s+(%s)\s+(?:và|and)\s+(%s)" % (_N, _N), r"(\1;\2)", low)
    # a < x < b
    low = re.sub(r"(%s)\s*<=?\s*x\s*<=?\s*(%s)" % (_N, _N), r"(\1;\2)", low)
    low = re.sub(r"\bx\s+(?:is|là)\s+([<>]=?)", r"x \1", low)
    low = re.sub(r"x\s*>=?\s*(%s)\s*(?:,\s*)?(?:và|and|nhưng|but)\s*(?:x\s*)?<=?\s*(%s)" % (_N, _N), r"(\1;\2)", low)
    low = re.sub(r"x\s*>=?\s*(%s)" % _N, r"(\1;oo)", low)
    low = re.sub(r"x\s*<=?\s*(%s)" % _N, r"(-oo;\1)", low)
    # ±a -> a và -a
    low = re.sub(r"±\s*(\d+(?:[.,]\d+)?)", r"\1 và x = -\1", low)
    return low


_XAC_NHAN = re.compile(
    r"(đúng rồi|đúng vậy|đúng đó|đúng nhé|đúng ạ|đúng là thế|chính xác|em làm đúng|em tìm đúng|em ra đúng|em tính đúng|"
    r"là đúng|đều đúng|đúng hết|hoàn toàn đúng|sai rồi|chưa đúng|chưa chính xác|bị sai|là sai|không đúng|gần đúng|"
    r"\bcorrect\b|that'?s right|\bexactly\b|you'?re right|\bwrong\b|\bincorrect\b|^\s*(đúng|sai|yes|no|right|phải|ừ|vâng)\b)",
    re.I,
)


def _co_so(t):
    t2 = chuan_hoa_them(t)
    return bool(re.search(r"\d|oo", t2))


def _dem_cuc_tri(su_kien):
    return sum(1 for a, _ in su_kien if a in ("DCD", "DCT"))


def _vi_pham(ban_nhap, bai, cau_hs):
    L = _L()
    # SP-11: "y' = 0" là tên phương trình, không phải khẳng định nghiệm bằng 0 -> không so số 0 rời rạc
    goc = re.sub(r"(y'|y′|f'\(x\)|f′\(x\))\s*=\s*0(?![\d.,])", "YPHAY_BANG_KHONG", ban_nhap or "")
    # số có số 0 đứng đầu ("08") làm SymPy ném lỗi: bỏ số 0 thừa để so đúng giá trị (lỗi khác vẫn fail closed)
    goc = re.sub(r"(?<![\d.,])0+(\d)", r"\1", goc)
    them = chuan_hoa_them(goc)
    for t in (goc, them):
        if L.m23(t, bai, True) or L.m1(t, bai):
            return "gia_tri"
    n_ct = _dem_cuc_tri(bai["su_kien"])
    if n_ct:
        for m in re.finditer(r"(\d+)\s+(?:điểm\s+)?(?:cực trị|điểm cực trị|extrema|extreme points|turning points)", them):
            if int(m.group(1)) == n_ct:
                return "so_luong"
    if cau_hs and _XAC_NHAN.search(unicodedata.normalize("NFC", goc)) and _co_so(cau_hs):
        return "xac_nhan"
    return None


# Sự kiện phủ định (29/09): "không có cực trị", "không có khoảng nghịch biến"… Không có giá trị số nên xử lý riêng,
# không đưa vào lớp SymPy của loc.py.
_PHU_DINH = {
    "KHONG_CUC_TRI": r"(cực trị|cực đại|cực tiểu|extrem|turning point|local (max|min))",
    "KHONG_CUC_DAI": r"(cực đại|local max)",
    "KHONG_CUC_TIEU": r"(cực tiểu|local min)",
    "KHONG_DONG_BIEN": r"(đồng biến|tăng|increasing)",
    "KHONG_NGHICH_BIEN": r"(nghịch biến|giảm|decreasing)",
    "KHONG_NGHIEM": r"(nghiệm|điểm tới hạn|y' ?= ?0|root)",
}
_KHONG = r"(không có|không tồn tại|chẳng có|không hề có|không đạt|no |none|does not have|doesn't have|has no|there (is|are) no|không\s+\w+\s+(cực|nghiệm))"
_NGAN = re.compile(r"^\s*(không|có|đúng|sai|chưa|rồi|ừ|ừm|vâng|dạ|yes|no|yep|nope|đúng rồi|không có|có ạ|không ạ)[\s.!?…]*$", re.I)


def _vi_pham_phu_dinh(ban_nhap, phu_dinh, cau_hs):
    t = unicodedata.normalize("NFC", ban_nhap or "").lower()
    for loai in phu_dinh:
        chu = _PHU_DINH.get(loai)
        if not chu:
            continue
        if re.search(_KHONG + r"[^.?!]{0,40}" + chu, t) or re.search(chu + r"[^.?!]{0,30}(không có|không tồn tại|none)", t):
            return "phu_dinh"
        # câu trả lời ngắn "không"/"không có" cho câu hỏi có nhắc đúng chủ đề
        if _NGAN.match(t) and cau_hs and re.search(chu, unicodedata.normalize("NFC", cau_hs).lower()):
            return "tra_loi_ngan"
    return None


_BAN_NHAP_TOI_DA = 4000
_DAU_HIEU_CODE = re.compile(r"__|\blambda\b|\bimport\b|\b(?:eval|exec|open|compile|getattr|setattr|globals|locals|vars|input)\s*\("
                            r"|\bsubprocess\b|\bos\.\w|\bsys\.\w|\(\s*\)\s*\."
                            r"|\b[A-Za-z_]\w*\s*\(\s*[\'\"]"
                            r"|\b(?:factorial|Symbol|Integer|Float|Rational|Function|Lambda|sympify|parse_expr|lambdify)\s*\("
                            r"|(?:\*\*|\^)\s*\{?\s*\d+\s*\}?\s*(?:\*\*|\^)"
                            r"|(?:\*\*|\^)\s*\(?\s*\d{3,}|\d\s*\*\*\s*\d{2,}"
                            r"|\b(?![yY]_)\w+_\w+\b|[\"\'`]\s*\w+\s*[\"\'`]")


def loc_ban_nhap(ban_nhap, su_kien, cau_hs=None):
    try:
        # F-01: bản nháp có dấu hiệu code / quá dài, hoặc giá trị sự kiện không qua cổng danh sách trắng -> chặn.
        # Không bao giờ thực thi; chặn để câu lạ không tới học sinh và sự kiện hỏng không làm bộ so "không khớp".
        if not isinstance(ban_nhap, str) or _L().kiem_ban_nhap(ban_nhap):
            return {"cho_phep": False, "lop_chinh": "dau_vao", "lop_phu": "chuoi", "loi": False, "ly_do": DAU_VAO_KHONG_HOP_LE}
        cac = _pairs(su_kien)
        if any(not re.fullmatch(r"[A-Z][A-Z_]{0,30}", str(a)) or ly_do_tu_choi(b) for a, b in cac):
            return {"cho_phep": False, "lop_chinh": "dau_vao", "lop_phu": "chuoi", "loi": False, "ly_do": DAU_VAO_KHONG_HOP_LE}
        phu_dinh = [a for a, _b in cac if str(a).startswith("KHONG_")]
        bai = {"su_kien": [(a, b) for a, b in cac if not str(a).startswith("KHONG_")]}
        # Sự kiện bảo vệ phải đọc được TRƯỚC khi so (Sư phạm 11:54): hỏng -> chặn, ly_do SU_KIEN_LOI
        # (vd "1  sqrt(2)" do replace("+", "") cũ; không để lẫn với lỗi kiểm bản nháp).
        if not _su_kien_doc_duoc(bai["su_kien"]):
            return {"cho_phep": False, "lop_chinh": "sympy", "lop_phu": "chuoi", "loi": True, "ly_do": "SU_KIEN_LOI"}
        # F-01 (bản vá Kiểm định): bản nháp có dấu hiệu code / quá dài -> chặn (không bao giờ thực thi; câu lạ không tới HS).
        if not isinstance(ban_nhap, str) or len(ban_nhap) > _BAN_NHAP_TOI_DA or _DAU_HIEU_CODE.search(ban_nhap):
            return {"cho_phep": False, "lop_chinh": "sympy", "lop_phu": "chuoi", "loi": True, "ly_do": "DAU_VAO_KHONG_HOP_LE"}
        # Thiếu câu HS: câu nháp chỉ là "không"/"có" không đánh giá được -> chặn
        if cau_hs is None and _NGAN.match(unicodedata.normalize("NFC", ban_nhap or "")):
            return {"cho_phep": False, "lop_chinh": "ngu_canh", "lop_phu": "chuoi", "loi": False, "ly_do": "THIEU_NGU_CANH"}
        ly_do = _vi_pham_phu_dinh(ban_nhap, phu_dinh, cau_hs) or _vi_pham(ban_nhap, bai, cau_hs)
        # ly_do chỉ là LOẠI quyết định (theo bản vá Kiểm định): LO_DAP_AN / XAC_NHAN / THIEU_NGU_CANH / SU_KIEN_LOI / DAU_VAO_KHONG_HOP_LE / LOI_KIEM_TRA / None
        return {"cho_phep": ly_do is None, "lop_chinh": "sympy", "lop_phu": "chuoi", "loi": False,
                "ly_do": None if ly_do is None else ("XAC_NHAN" if ly_do == "xac_nhan" else "LO_DAP_AN")}
    except Exception:
        # FAIL CLOSED: không kiểm được (nạp bộ lọc, đọc sự kiện, parse số, so SymPy) thì CHẶN
        return {"cho_phep": False, "lop_chinh": "sympy", "lop_phu": "chuoi", "loi": True, "ly_do": "LOI_KIEM_TRA"}


def _su_kien_doc_duoc(cap):
    # Lỗi nạp bộ lọc (không có loc.py / thiếu hàm) KHÔNG phải lỗi sự kiện: để nó bay ra -> LOI_KIEM_TRA.
    L = _L()
    doc_khoang, doc_so = L.parse_khoang, L.num
    for a, b in cap:
        try:
            if a in ("DB", "NB"):
                doc_khoang(str(b))
            elif a in ("DCD", "DCT", "GTCD", "GTCT", "NGHIEM"):
                doc_so(str(b))
            else:
                return False   # loại sự kiện lạ
        except Exception:
            return False
    return True


def _pairs(su_kien):
    out = []
    for item in su_kien or []:
        if isinstance(item, dict):
            out.append((item["loai"], item["gia_tri"]))
        else:
            out.append((item[0], item[1]))
    return out
