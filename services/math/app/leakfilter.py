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


# 0003: "không" đứng trước động từ / tính từ là phủ định ("hàm số không đạt cực trị", "y' không đổi dấu"),
# không đọc thành số 0 (trước đây "số" + "không" -> "số 0" khớp giá trị bảo vệ 0 -> chặn nhầm câu thang mẫu).
_SAU_PHU_DINH = {"đạt", "có", "phải", "đổi", "tồn", "xác", "đồng", "nghịch", "tăng", "giảm", "chứa", "phụ", "cần",
                 "được", "thể", "đúng", "cùng", "hề", "liên", "thỏa", "thoả", "thuộc", "nằm", "làm", "bị", "đơn", "xảy",
                 "tính", "viết", "ghi", "nối", "dùng", "thay", "chia", "rõ", "biết", "hiểu", "còn", "hợp", "cắt"}


def _khong_phu_dinh(words, end):
    if words[end] != words[end].rstrip(".,;:!?()"):
        return False   # "bằng không." / "bằng không," -> số 0
    k = end + 1
    while k < len(words) and words[k].isspace():
        k += 1
    return k < len(words) and words[k].strip(".,;:!?()").lower() in _SAU_PHU_DINH


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
            if seq == ["không"] and _khong_phu_dinh(words, end):
                v = None   # 0003: "hàm số không đạt cực trị" là phủ định, không phải "hàm số 0 đạt cực trị"
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


# ------------------------------------------------------------------ bản vá 0003 (Kiểm định, 29/09)
# (a) điểm thử nêu số cụ thể mà giữa hai điểm liên tiếp luôn có mốc -> suy ra được mốc ("Thử lần lượt x = -4, -2, 2").
# (b) câu điều kiện có điều kiện ĐÚNG với bài (tính theo sự kiện) = khẳng định kết luận (thang mẫu README §4 quy tắc 5):
#     "Nếu tử là hằng số khác 0 thì y' = 0 vô nghiệm" ở hàm bậc nhất/bậc nhất -> lộ NGHIEM vô nghiệm.
# (c) câu điều kiện chung (nói về "điểm đó", "x₀", "mốc nào … thì") hoặc có điều kiện SAI với bài không phải khẳng định
#     về bài -> không xét luật phủ định ("nếu không đổi dấu thì hàm số không đạt cực trị tại điểm đó").
# Thêm: lộ bằng lời có đối chiếu sự kiện (số nghiệm, nghiệm kép, y' luôn dương/âm, luôn đồng/nghịch biến,
#     TXĐ = ℝ, loại điểm khỏi TXĐ, "còn thiếu điểm lớn hơn").
_SO = r"-?\s?\d+(?:[.,]\d+)?(?:/\d+)?"
_MO_DK = re.compile(r"\b(?:nếu|hễ|giả sử|trường hợp|if|when)\b"
                    r"|\b(?:mốc|điểm|khoảng|nghiệm|giá trị|nhân tử|số|ô|dòng|cột|hệ số|phương trình)\s+nào\b(?=[^?]*\bthì\b)")
_CUC_BO = re.compile(r"(?:điểm|khoảng|mốc|chỗ|nơi|nghiệm)\s+(?:đó|ấy|này)|\btại đó\b|\bở đó\b|x₀|x_\{?0\}?|\bx0\b"
                     r"|\bmột điểm\b|\bmột khoảng\b|\bhai bên\b|\bđi qua\b|\bqua điểm\b|\bk\b")


def _gia_tri_so(s):
    s = s.replace(" ", "").replace(",", ".")
    if "/" in s:
        a, b = s.split("/")
        return float(a) / float(b)
    return float(s)


def _ngu_canh(bai, phu_dinh):
    """Tính các điều kiện theo sự kiện bài (không suy từ chỗ thiếu sự kiện, chỉ từ các khoảng phủ kín trục số)."""
    L = _L()
    diem = {"NGHIEM": set(), "DCD": set(), "DCT": set()}
    kh = []
    for a, b in bai["su_kien"]:
        if a in diem:
            diem[a].add(round(float(L.num(str(b))), 9))
        elif a in ("DB", "NB"):
            u, v = L.parse_khoang(str(b))
            kh.append((float(u) if u.is_finite else float("-inf"), float(v) if v.is_finite else float("inf"), a))
    kh.sort()
    phu = bool(kh) and kh[0][0] == float("-inf") and kh[-1][1] == float("inf") and all(
        abs(kh[i][1] - kh[i + 1][0]) < 1e-9 for i in range(len(kh) - 1))
    dac_biet = diem["NGHIEM"] | diem["DCD"] | diem["DCT"]
    dau_mut = {round(x, 9) for u, v, _a in kh for x in (u, v) if x not in (float("inf"), float("-inf"))}
    lo = sorted(round(kh[i][1], 9) for i in range(len(kh) - 1)
                if phu and not any(abs(kh[i][1] - d) < 1e-9 for d in dac_biet))
    co_ct = bool(diem["DCD"] or diem["DCT"])
    pd = set(phu_dinh)
    if phu and not co_ct:
        pd.add("KHONG_CUC_TRI")
        if not diem["NGHIEM"]:
            pd.add("KHONG_NGHIEM")
    if phu and co_ct and not diem["DCD"]:
        pd.add("KHONG_CUC_DAI")
    if phu and co_ct and not diem["DCT"]:
        pd.add("KHONG_CUC_TIEU")
    loai_kh = {a for _u, _v, a in kh}
    if phu and loai_kh == {"DB"}:
        pd.add("KHONG_NGHICH_BIEN")
    if phu and loai_kh == {"NB"}:
        pd.add("KHONG_DONG_BIEN")
    return {
        "moc": sorted(dac_biet | dau_mut),
        "n_nghiem": len(diem["NGHIEM"]),
        "phu_dinh": [x for x in _PHU_DINH if x in pd] + [x for x in phu_dinh if x not in _PHU_DINH],
        "khong_nghiem": "KHONG_NGHIEM" in pd,
        "khong_cuc_tri": "KHONG_CUC_TRI" in pd,
        "tu_hang": "KHONG_NGHIEM" in pd and bool(lo) and not co_ct,
        "kep": bool(diem["NGHIEM"]) and not co_ct and not lo,
        "lo": lo,
        "txd_r": phu and not lo,
        "chi": (loai_kh.pop() if phu and len(loai_kh) == 1 else None),
    }


def _dk_dung(a, ctx):
    """Điều kiện của câu "nếu A thì B" đúng / sai với bài; None nếu không đánh giá được."""
    if re.search(r"tử(?: số)?(?: của (?:y'|đạo hàm))?(?: chỉ)?(?: là| bằng)?(?: một)? (?:hằng số|hằng|số khác 0|số khác không)"
                 r"|tử(?: số)?(?: của (?:y'|đạo hàm))? không (?:chứa|phụ thuộc)", a):
        return ctx["tu_hang"]
    if "nghiệm kép" in a:
        return ctx["kep"]
    if re.search(r"không đổi dấu", a):
        return ctx["khong_cuc_tri"]
    if re.search(r"vô nghiệm|không có (?:nghiệm|điểm tới hạn)|(?:δ|delta|biệt thức)'?\s*(?:<\s*0|âm)"
                 r"|(?:y'|đạo hàm)\s*(?:luôn|với mọi x)\s*(?:>|<|dương|âm)", a):
        return ctx["khong_nghiem"]
    if re.search(r"(?:δ|delta|biệt thức)'?\s*(?:>\s*0|dương)|(?:hai|2) nghiệm phân biệt", a):
        return ctx["n_nghiem"] == 2
    return None


def _tach_cau(t):
    return [c for c in re.split(r"(?<=[.?!;:])\s+|\n+", t) if c.strip()]


def _khang_dinh(cau, ctx):
    """Phần khẳng định về bài của một câu: bỏ câu điều kiện chung / điều kiện sai với bài;
    câu điều kiện đúng với bài thì giữ kết luận (quy tắc 5); điều kiện không đánh giá được thì giữ nguyên như cũ."""
    mos = list(_MO_DK.finditer(cau))
    if not mos:
        return cau
    out = [cau[:mos[0].start()]]
    for i, m in enumerate(mos):
        seg = cau[m.start(): mos[i + 1].start() if i + 1 < len(mos) else len(cau)]
        k = re.search(r"\bthì\b|\bthen\b", seg)
        a, c = (seg[:k.start()], seg[k.end():]) if k else (seg, "")
        if c and _CUC_BO.search(c):
            continue
        v = _dk_dung(a, ctx)
        if v is True:
            out.append(" " + c)
        elif v is False or _CUC_BO.search(a) or m.group(0).endswith("nào"):
            continue
        else:
            out.append(" " + seg)
    return "".join(out)


def _vi_pham_phu_dinh(ban_nhap, phu_dinh, cau_hs, ctx=None):
    t = unicodedata.normalize("NFC", ban_nhap or "").lower()
    if ctx is not None:
        phu_dinh = ctx["phu_dinh"]
        cac_cau = [_khang_dinh(c, ctx) for c in _tach_cau(t)]
    else:
        cac_cau = [t]
    for loai in phu_dinh:
        chu = _PHU_DINH.get(loai)
        if not chu:
            continue
        for c in cac_cau:
            if re.search(_KHONG + r"[^.?!;:]{0,40}" + chu, c) or re.search(chu + r"[^.?!;:]{0,30}(không có|không tồn tại|none)", c):
                return "phu_dinh"
            if loai == "KHONG_NGHIEM" and re.search(r"vô nghiệm|\bno (?:real )?(?:roots?|solutions?)\b", c):
                return "phu_dinh"
        # câu trả lời ngắn "không"/"không có" cho câu hỏi có nhắc đúng chủ đề
        if _NGAN.match(t) and cau_hs and re.search(chu, unicodedata.normalize("NFC", cau_hs).lower()):
            return "tra_loi_ngan"
    return None


_GOI_THU = re.compile(r"\b(?:thử|thay|chọn|lấy|test|try|plug|pick|choose|substitut)")


def _vi_pham_diem_thu(ban_nhap, ctx):
    """(a) >= 2 điểm thử cụ thể, không trùng mốc, giữa mỗi cặp liên tiếp đều có mốc -> suy ra được mốc."""
    moc = ctx["moc"]
    if not moc:
        return None
    for c in _tach_cau(chuan_hoa_them(ban_nhap)):
        if not _GOI_THU.search(c):
            continue
        diem = set()
        for m in re.finditer(r"x\s*(?:=|bằng|là)\s*(%s(?:\s*(?:,|;|và|hoặc|rồi|and|or)\s*(?:x\s*(?:=|bằng)\s*)?%s)*)" % (_SO, _SO), c):
            for so in re.findall(_SO, m.group(1)):
                diem.add(round(_gia_tri_so(so), 9))
        diem = sorted(diem)
        if len(diem) < 2 or any(abs(d - x) < 1e-9 for d in diem for x in moc):
            continue
        if all(any(diem[i] < x < diem[i + 1] for x in moc) for i in range(len(diem) - 1)):
            return "diem_thu"
    return None


def _vi_pham_loi(ban_nhap, ctx):
    """Lộ bằng lời (không có số của đáp án) đối chiếu với sự kiện bài; bỏ qua câu hỏi và câu điều kiện chung."""
    for goc in _tach_cau(unicodedata.normalize("NFC", ban_nhap or "").lower()):
        if goc.rstrip().endswith("?"):
            continue
        c = _khang_dinh(goc, ctx)
        s = chuan_hoa_them(c)
        n = ctx["n_nghiem"]
        for m in re.finditer(r"(?:có|gồm|được|ra|thấy)\s+(\d+)\s+(?:nghiệm|điểm tới hạn)", s):
            if n and int(m.group(1)) == n:
                return "so_luong"
        if ctx["kep"] and re.search(r"(?:có|là|được)\s+(?:1\s+|một\s+)?nghiệm kép", c):
            return "so_luong"
        chi = ctx["chi"]
        if chi:
            dau = r"(?:dương|>\s*0|lớn hơn 0)" if chi == "DB" else r"(?:âm|<\s*0|nhỏ hơn 0)"
            if re.search(r"(?:y'|y′|đạo hàm)[^.;,]{0,25}(?:luôn|lúc nào cũng|với mọi x)\s*(?:luôn\s*)?" + dau, c):
                return "loi"
            bien = r"(?:đồng biến|tăng)" if chi == "DB" else r"(?:nghịch biến|giảm)"
            if re.search(r"(?:luôn|lúc nào cũng)\s+" + bien + r"|" + bien +
                         r"\s+trên\s+(?:(?:từng|mỗi)\s+khoảng|(?:cả\s+|toàn\s+bộ\s+)?(?:ℝ|\\mathbb\{r\}|r\b|tập xác định|trục số))", c):
                return "loi"
        if ctx["txd_r"] and re.search(r"(?:tập xác định|txđ|\bd\b)\s*(?:là|=|bằng)\s*(?:ℝ|\\mathbb\{r\}|r\b|mọi số thực|tất cả (?:các )?số thực"
                                      r"|toàn bộ trục số|cả trục số)", c):
            return "loi"
        for m in re.finditer(r"x\s*(?:≠|!=|\\neq|khác)\s*(%s)|(?:loại|bỏ|trừ)\s+(?:đi\s+)?(?:điểm\s+)?(?:x\s*=\s*)?(%s)"
                             r"|\\?\{\s*(%s)\s*\\?\}|mẫu[^.;]{0,25}(?:=|bằng)\s*0[^.;]{0,15}x\s*=\s*(%s)" % (_SO, _SO, _SO, _SO), s):
            so = next(g for g in m.groups() if g)
            if m.group(1) and ("mẫu" in s[max(0, m.start() - 25):m.start()] or re.search(r"[\w)]$", s[:m.start()])):
                continue   # "điều kiện mẫu số 2x khác 0" chỉ nhắc lại điều kiện của đề, không phải giá trị bị loại
            if any(abs(_gia_tri_so(so) - x) < 1e-9 for x in ctx["lo"]):
                return "loi"
        if re.search(r"thiếu\s+(?:\d+\s+|một\s+)?(?:điểm|nghiệm|mốc)\s+(?:lớn|nhỏ|ở giữa|âm|dương|bên)"
                     r"|còn\s+(?:\d+|một|hai|ba)\s+(?:điểm|nghiệm|mốc)[^.?!]{0,30}(?:chưa|thiếu|quên|sót)", c):
            return "xac_nhan" if re.search(r"đúng rồi|đúng", c) else "loi"
    return None


# ------------------------------------------------------------------ bản vá 0004 (Kiểm định, 29/09)
# Câu gợi ý không được viết ra biểu thức của y' (hay tử số của y', dạng rút gọn / phân tích nhân tử) BẰNG đạo hàm thật
# của hàm (thiet-ke-ai-v0 mục 24). So bằng SymPy với sự kiện YPHAY (đạo hàm thật, do su_kien_bao_ve tính từ hàm):
#   - biểu thức có x, hoặc biểu thức sau "y' =", "đạo hàm là": cancel(bt - y') == 0 -> chặn;
#   - biểu thức sau "tử số (của y') là / =": bt / tử(y') là hằng số DƯƠNG -> chặn ("Tử số của y' là −2").
# Biểu thức SAI (khác đạo hàm thật) không bị luật này chặn: không lộ đáp án, chấm sai là việc của bộ chấm (quyết định 0004).
# Ngoại lệ (Sư phạm + AI, mục 24): bài khung ngắn có buoc_bat_dau là B.DH.NGHIEM / B.DH.XETDAU thì y' là dữ kiện đề cho;
# sự kiện YPHAY_DE giữ NGUYÊN VĂN dòng y' của đề. Câu nhắc lại đúng nguyên văn (hoặc một phần nguyên văn, vd tử số như đề
# viết) thì qua; viết dạng đã rút gọn / phân tích / khai triển khác đề thì vẫn chặn. So nguyên văn = cùng cây biểu thức
# khi đọc KHÔNG rút gọn (evaluate=False): bỏ khoảng trắng / dấu ngoặc thừa, không bỏ thứ tự hạng tử hay nhân tử.
_SU_KIEN_BT = ("YPHAY", "YPHAY_DE")
_BUOC_Y_PHAY_DE = ("B.DH.NGHIEM", "B.DH.XETDAU")
_MU = {"⁰": "0", "¹": "1", "²": "2", "³": "3", "⁴": "4", "⁵": "5", "⁶": "6", "⁷": "7", "⁸": "8", "⁹": "9"}
_BT_KY_TU = r"[0-9x+\-*/^().·×{}§ ]"
_BT_TOI_DA = 160
_BT_SO_TOI_DA = 12


def _tien_xu_ly_bt(t, che_chu=True):
    """Chuẩn hóa ký hiệu để tìm biểu thức: dấu trừ, lũy thừa chữ nhỏ, lệnh LaTeX, chữ có 'x' (xét, x-quang) -> che."""
    t = unicodedata.normalize("NFC", t or "")
    for a, b in (("−", "-"), ("–", "-"), ("—", "-"), ("′", "'"), ("$", " "), ("\\left", ""), ("\\right", ""),
                 ("\\cdot", "*"), ("\\times", "*"), ("\\,", " "), ("\\;", " "), ("\\ ", " ")):
        t = t.replace(a, b)
    t = re.sub(r"\\[dt]?frac", "§", t)
    t = re.sub(r"[⁰¹²³⁴⁵⁶⁷⁸⁹]+", lambda m: "^" + "".join(_MU[c] for c in m.group(0)) + " ", t)
    t = re.sub(r"(?<=\d),(?=\d)", ".", t)          # 0,5 -> 0.5
    if not che_chu:
        return t
    # chữ chứa x (xét, xác, max) không phải biến x
    t = re.sub(r"[^\W\d_]+", lambda m: m.group(0) if m.group(0) == "x" else "¤" * len(m.group(0)), t)
    return t


def _doi_bt(s):
    """Chuỗi biểu thức (đã tiền xử lý) -> cú pháp SymPy tường minh, hoặc None."""
    s = s.strip()
    # \frac{A}{B} -> ((A)/(B)), xử lý lồng nhau từ trong ra
    for _ in range(8):
        m = re.search(r"§\s*\{([^{}§]*)\}\s*\{([^{}§]*)\}", s)
        if not m:
            break
        s = s[:m.start()] + "((%s)/(%s))" % (m.group(1), m.group(2)) + s[m.end():]
    if "§" in s:
        return None
    s = s.replace("{", "(").replace("}", ")").replace("·", "*").replace("×", "*").replace("^", "**")
    s = re.sub(r"\s+", "", s)
    s = re.sub(r"(?<=[\dx)])(?=[x(])", "*", s)      # 3x, 3(x, x(, )(, )x
    s = re.sub(r"(?<=[x)])(?=\d)", "*", s)          # x2 hiếm gặp, )2
    s = s.replace("***", "**")
    if not s or s.count("(") != s.count(")") or not re.search(r"[\dx]", s):
        return None
    return s


def _cac_bt(t):
    """[(vị trí bắt đầu, chuỗi SymPy)] các đoạn biểu thức trong câu đã tiền xử lý (tách ở dấu '=' ',' ';' và chữ)."""
    out = []
    for m in re.finditer(r"%s+" % _BT_KY_TU, t):
        doan = m.group(0)
        st = m.start() + len(doan) - len(doan.lstrip(" "))
        doan = doan.strip(" ")
        doan = re.sub(r"[.\s]+$", "", doan)
        if not doan or len(doan) > _BT_TOI_DA:
            continue
        s = _doi_bt(doan)
        if s:
            out.append((st, s))
        if len(out) >= _BT_SO_TOI_DA:
            break
    return out


def _doc_bt(s, rut_gon=True):
    """Đọc chuỗi SymPy bằng bộ phân tích an toàn của loc.py. Không đọc được (chuỗi không phải biểu thức) -> None."""
    L = _L()
    from sympy import Symbol
    x = Symbol("x")
    try:
        e = L.phan_tich_an_toan(s, {"x": x})
    except L.DauVaoKhongHopLe:
        return None
    if rut_gon:
        return e
    # cây nguyên văn: chuỗi đã qua kiem_chuoi_an_toan ở trên, đọc lại không rút gọn với cùng từ điển tên
    loc_ = dict(L._HANG_CHO_PHEP)
    loc_.update(L._HAM_CHO_PHEP)
    loc_["x"] = x
    from sympy import Add, Mul, Pow
    glob_ = dict(L._GLOBAL_TOI_THIEU)
    glob_.update(Add=Add, Mul=Mul, Pow=Pow)   # evaluate=False sinh lời gọi Add/Mul/Pow tường minh
    glob_["__builtins__"] = {}
    s2 = L.kiem_chuoi_an_toan(s, ["x"])
    return L._parse_expr_goc(s2, local_dict=loc_, global_dict=glob_, transformations=L._BIEN_DOI_AN_TOAN, evaluate=False)


def _doc_bt_cau(s, rut_gon=True):
    """Đọc một đoạn của CÂU NHÁP. Đoạn chữ lẫn ký hiệu không phải biểu thức (lỗi cú pháp) -> None, không chặn cả câu;
    sự kiện YPHAY / YPHAY_DE hỏng thì vẫn fail closed (SU_KIEN_LOI) ở loc_ban_nhap."""
    try:
        return _doc_bt(s, rut_gon)
    except Exception:
        return None


def _y_phay_su_kien(su_kien_bt):
    yp, de = None, []
    for a, b in su_kien_bt:
        if a == "YPHAY":
            s = _doi_bt(_tien_xu_ly_bt(str(b)))
            e = _doc_bt(s) if s else None
            if e is None:
                raise ValueError("YPHAY")
            yp = e
        elif a == "YPHAY_DE":
            s = _doi_bt(_tien_xu_ly_bt(str(b)))
            e = _doc_bt(s, rut_gon=False) if s else None
            if e is None:
                raise ValueError("YPHAY_DE")
            de.append(e)
    return yp, de


def _nguyen_van_de(s, de):
    if not de:
        return False
    e = _doc_bt_cau(s, rut_gon=False)
    if e is None:
        return False
    from sympy import preorder_traversal
    return any(e == n for d in de for n in preorder_traversal(d))


_TRUOC_Y_PHAY = re.compile(r"(?:y\s*'|f\s*'\s*\(\s*x\s*\)|đạo hàm(?:\s+(?:y\s*'|của hàm số|của hàm|bằng))?)\s*(?:=|là|bằng|:)\s*$")
_TRUOC_TU = re.compile(r"tử(?:\s*(?:số|thức))?(?:\s+(?:của\s+)?(?:y\s*'|f\s*'\s*\(\s*x\s*\)|đạo hàm))?\s*(?:=|là|bằng|:)\s*$")


def _vi_pham_y_phay(ban_nhap, su_kien_bt):
    if not any(a == "YPHAY" for a, _b in su_kien_bt):
        return None
    yp, de = _y_phay_su_kien(su_kien_bt)
    from sympy import Symbol, cancel, fraction, together
    tu_yp = fraction(cancel(together(yp)))[0]
    goc = unicodedata.normalize("NFC", ban_nhap or "").lower()
    t = _tien_xu_ly_bt(goc)
    t_chu = _tien_xu_ly_bt(goc, che_chu=False)   # cùng độ dài với t, giữ chữ để đọc cụm đứng trước biểu thức
    for st, s in _cac_bt(t):
        truoc = t_chu[max(0, st - 40):st]
        la_tu = bool(_TRUOC_TU.search(truoc))
        la_yp = not la_tu and bool(_TRUOC_Y_PHAY.search(truoc))
        if "x" not in s and not (la_tu or la_yp):
            continue
        e = _doc_bt_cau(s)
        if e is None or e.free_symbols - {Symbol("x")}:
            continue
        lo = False
        if not la_tu and cancel(together(e - yp)) == 0:
            lo = True
        elif la_tu and e != 0:
            r = cancel(together(e / tu_yp))
            lo = bool(r.is_number and r.is_positive)
        if lo and not _nguyen_van_de(s, de):
            return "y_phay"
    return None


def su_kien_de_cho(de_bai, buoc_bat_dau):
    """Sự kiện YPHAY_DE (NGUYÊN VĂN dòng y' của đề) cho bài khung ngắn bắt đầu ở B.DH.NGHIEM / B.DH.XETDAU; ngược lại []."""
    if buoc_bat_dau not in _BUOC_Y_PHAY_DE or not isinstance(de_bai, str):
        return []
    t = unicodedata.normalize("NFC", de_bai)
    m = re.search(r"(?:y\s*['′]|f\s*['′]\s*\(\s*x\s*\))\s*=\s*([0-9x+\-−–*/^().·× ⁰¹²³⁴⁵⁶⁷⁸⁹]+)", t)
    if not m:
        return []
    bt = re.sub(r"[.\s]+$", "", m.group(1)).strip()
    if not bt or ly_do_tu_choi(bt) or _doi_bt(_tien_xu_ly_bt(bt)) is None:
        return []
    return [["YPHAY_DE", bt]]


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
        # 0004: sự kiện biểu thức (YPHAY, YPHAY_DE) tách riêng, không đưa vào lớp số / phủ định
        bt = [(a, b) for a, b in cac if a in _SU_KIEN_BT]
        cac = [(a, b) for a, b in cac if a not in _SU_KIEN_BT]
        phu_dinh = [a for a, _b in cac if str(a).startswith("KHONG_")]
        bai = {"su_kien": [(a, b) for a, b in cac if not str(a).startswith("KHONG_")]}
        # Sự kiện bảo vệ phải đọc được TRƯỚC khi so (Sư phạm 11:54): hỏng -> chặn, ly_do SU_KIEN_LOI
        # (vd "1  sqrt(2)" do replace("+", "") cũ; không để lẫn với lỗi kiểm bản nháp).
        if not _su_kien_doc_duoc(bai["su_kien"]) or not _bt_doc_duoc(bt):
            return {"cho_phep": False, "lop_chinh": "sympy", "lop_phu": "chuoi", "loi": True, "ly_do": "SU_KIEN_LOI"}
        # F-01 (bản vá Kiểm định): bản nháp có dấu hiệu code / quá dài -> chặn (không bao giờ thực thi; câu lạ không tới HS).
        if not isinstance(ban_nhap, str) or len(ban_nhap) > _BAN_NHAP_TOI_DA or _DAU_HIEU_CODE.search(ban_nhap):
            return {"cho_phep": False, "lop_chinh": "sympy", "lop_phu": "chuoi", "loi": True, "ly_do": "DAU_VAO_KHONG_HOP_LE"}
        # Thiếu câu HS: câu nháp chỉ là "không"/"có" không đánh giá được -> chặn
        if cau_hs is None and _NGAN.match(unicodedata.normalize("NFC", ban_nhap or "")):
            return {"cho_phep": False, "lop_chinh": "ngu_canh", "lop_phu": "chuoi", "loi": False, "ly_do": "THIEU_NGU_CANH"}
        ctx = _ngu_canh(bai, phu_dinh)
        ly_do = (_vi_pham_phu_dinh(ban_nhap, phu_dinh, cau_hs, ctx) or _vi_pham(ban_nhap, bai, cau_hs)
                 or _vi_pham_diem_thu(ban_nhap, ctx) or _vi_pham_loi(ban_nhap, ctx) or _vi_pham_y_phay(ban_nhap, bt))
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


def _bt_doc_duoc(bt):
    # 0004: YPHAY / YPHAY_DE phải đọc được bằng bộ phân tích an toàn; hỏng -> SU_KIEN_LOI (lỗi nạp bộ lọc vẫn bay ra)
    L = _L()
    try:
        _y_phay_su_kien(bt)
    except (ValueError, L.DauVaoKhongHopLe):
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
