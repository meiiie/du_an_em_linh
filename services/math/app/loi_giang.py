# -*- coding: utf-8 -*-
"""Job kiem_loi_giang (ADR 013): cổng thế giới đóng cho câu gia sư, chạy sau /v1/filter, trước khi học sinh thấy câu.

Đơn vị giữ hay bỏ là CÂU (tách như luật 1 của KD-0005). Một câu còn trong `cau_sach` khi mọi đoạn toán của nó DAT và
nó không là ứng viên quy tắc bằng lời, hoặc khi cả câu khớp một dòng bảng đã khóa. Đoạn toán DAT thuộc một trong ba loại:
  CONG_THUC_TONG_QUAT  khớp một dòng bảng dùng được: trùng LaTeX, cùng (E)' = R theo SymPy với u(x), v(x) ký hiệu, trùng
                       vế phải, hoặc bộ đọc định lí của dong_cong_thuc đọc ra cùng mục danh mục với dòng.
  TRICH_DE_BAI         trùng hàm, tử hay mẫu của đề sau khi chuẩn hóa cách viết; không rút gọn, không đổi thứ tự.
  TRICH_BAI_LAM        trùng nguyên một dòng học sinh đã nộp (chỉ bỏ khoảng trắng), đứng ngay sau «em viết …».
Đoạn có dạng quy tắc (vế trái (…)', ký hiệu chung u, v, n…, mệnh đề định lí) chỉ DAT qua bảng, không bao giờ là trích.
Còn lại bị bỏ: KET_QUA_CU_THE, CONG_THUC_TONG_QUAT ngoài bảng (SAI khi máy có phản ví dụ), KHONG_PHAN_TICH_DUOC (LaTeX
hỏng, dấu phân cách không đóng, toán viết trần ngoài $…$).
Ứng viên quy tắc bằng lời là câu có một cụm thuật ngữ và một cụm quan hệ khác nhau. Ứng viên, và câu có toán viết trần,
chỉ được giữ khi trùng nguyên một câu phát biểu của dòng bảng, hoặc bộ đọc định lí đọc trọn câu ra đúng mục của dòng.
Đóng mặc định: lỗi bất kỳ, đầu vào sai kiểu hay quá giới hạn thì không giữ gì (`loi`, `thay_bang_goi_y`).
"""
import re
import unicodedata

from app import dong_cong_thuc as dct

CAU_TOI_DA = 4000
DONG_HS_TOI_DA = 80
DO_DAI_DONG_HS_TOI_DA = 400
HAM_TOI_DA = 200
DOAN_TOAN_TOI_DA = 40

DAT, SAI, KKD = "DAT", "SAI", "KHONG_KIEM_DUOC"
CONG_THUC, QUY_TAC = "CONG_THUC_TONG_QUAT", "QUY_TAC_BANG_LOI"
TRICH_BAI_LAM, TRICH_DE_BAI = "TRICH_BAI_LAM", "TRICH_DE_BAI"
KET_QUA, KHONG_PHAN_TICH = "KET_QUA_CU_THE", "KHONG_PHAN_TICH_DUOC"

# Từ vựng tạm của lab Kiểm định: KD-0005 bản 2 (#116, commit b1232ca, tu-vung-quy-tac.yaml, SHA-256 8fe6bdb6…), chép
# nguyên văn, bỏ mục «nếu … thì» (luật 7). T029b áp bản vá thì đọc từ kiemdinh/loi-giang/ thay cho hai bộ này.
THUAT_NGU = (
    "đạo hàm", "hàm số", "hàm", "tích", "thương", "tổng", "hiệu", "lũy thừa", "căn", "căn bậc hai", "nghiệm", "dấu",
    "giá trị cực đại", "giá trị cực tiểu", "điểm tới hạn", "tập xác định", "khoảng", "đoạn", "hằng số", "tử", "mẫu",
    "phân thức", "đa thức", "bậc", "hệ số", "biến thiên", "bảng biến thiên", "tiếp tuyến", "giới hạn", "liên tục",
    "x mũ n", "tử số", "mẫu số", "nhân tử", "thừa số", "hạng tử", "giá trị lớn nhất", "giá trị nhỏ nhất", "phương trình",
    "tam thức", "biệt thức", "đồ thị", "trục số", "trùng phương", "số mũ", "đồng biến", "nghịch biến", "đơn điệu",
    "cực trị", "cực đại", "cực tiểu", "điểm cực trị", "điểm cực đại", "điểm cực tiểu", "tăng", "giảm", "đi lên",
    "đi xuống", "mũ", "lập phương",
)
TU_QUAN_HE = (
    "bằng", "là", "nhân", "chia", "cộng", "trừ", "bình phương", "lớn hơn", "nhỏ hơn", "lớn hơn hoặc bằng",
    "nhỏ hơn hoặc bằng", "không âm", "không dương", "dương", "âm", "khác", "đổi dấu", "không đổi dấu", "trái dấu",
    "cùng dấu", "suy ra", "kéo theo", "khi và chỉ khi", "khi", "thì", "tương đương", "gồm", "đạt", "cũng vậy", "cũng thế",
    "cũng như", "tương tự", "như vậy", "như thế", "giống", "giống như", "ứng với", "tương ứng", "đồng nghĩa", "nghĩa là",
    "tức", "tức là", "cho ra", "trùng", "trùng với", "cao hơn", "thấp hơn", "lớn nhất", "nhỏ nhất", "vì vậy", "vì thế",
    "do đó", "cho nên", "nên", "dẫn đến", "nếu", "lấy", "các đạo hàm", "luôn", "mọi", "triệt tiêu", "không xác định",
    "đồng biến", "nghịch biến", "đơn điệu", "cực trị", "cực đại", "cực tiểu", "điểm cực trị", "điểm cực đại",
    "điểm cực tiểu", "tăng", "giảm", "đi lên", "đi xuống", "mũ", "lập phương",
)
# Từ nối và trợ từ: câu còn lại chỉ gồm các từ này (cùng dấu dẫn [n]) là câu không còn nội dung (ADR 013 mục 4).
TU_NOI = frozenset((
    "vậy", "thế", "nhé", "nha", "nhá", "ạ", "à", "ừ", "ừm", "vâng", "dạ", "ok", "okay", "rồi", "và", "nên", "thì", "là",
    "do", "đó", "vì", "như", "sau", "khi", "ta", "mình", "có", "ở", "đây", "này", "nào", "với", "của", "cho", "ra",
    "cũng", "còn", "mà", "nhưng", "hay", "hoặc", "tức", "nghĩa", "rằng", "được", "thấy", "tại", "theo", "từ", "đến",
    "tới", "trên", "dưới", "trong", "ngoài", "nữa", "lại", "đã", "sẽ", "đang", "vẫn", "chỉ", "thôi", "đấy", "suy",
))


def _pq(loai, trang_thai, **them):
    return dict({"loai": loai, "trang_thai": trang_thai}, **{k: v for k, v in them.items() if v is not None})


def _dong_cua(loi):
    return {"cau_sach": "", "thay_bang_goi_y": True, "bieu_thuc": [], "cac_cau": [], "loi": loi}


# ------------------------------------------------------------------ từ vựng (luật 2–6 của KD-0005)
_TU = re.compile(r"\w+|:")


def _bo_dau(s):
    s = s.replace("đ", "d").replace("Đ", "D")
    return "".join(c for c in unicodedata.normalize("NFD", s) if unicodedata.category(c) != "Mn")


def _theo_tu_dau():
    muc = {}
    for ds, nhan in ((THUAT_NGU, "T"), (TU_QUAN_HE, "Q")):
        for m in ds:
            muc.setdefault(tuple(_TU.findall(m)), set()).add(nhan)
    out = {}
    for tu, nhan in sorted(muc.items(), key=lambda kv: -len(kv[0])):
        out.setdefault(_bo_dau(tu[0]), []).append((tu, frozenset(nhan)))
    return out


_THEO_TU_DAU = _theo_tu_dau()


def _khop_tu(tu_cau, tu_muc):
    """Luật 4: từ có dấu khớp đúng từ; từ toàn ASCII khớp cả dạng bỏ dấu của từ trong mục."""
    return tu_cau == tu_muc or (tu_cau.isascii() and tu_cau == _bo_dau(tu_muc))


# Vần mở viết dấu thanh kiểu mới («luỹ», «hoà») → kiểu của từ vựng («lũy», «hòa»). Vần có phụ âm cuối («khoảng») giữ nguyên.
_DAU_THANH_MOI = {"oà": "òa", "oá": "óa", "oả": "ỏa", "oã": "õa", "oạ": "ọa", "oè": "òe", "oé": "óe", "oẻ": "ỏe",
                  "oẽ": "õe", "oẹ": "ọe", "uỳ": "ùy", "uý": "úy", "uỷ": "ủy", "uỹ": "ũy", "uỵ": "ụy"}
_DAU_THANH = re.compile("(?:%s)(?![^\\W\\d_])" % "|".join(_DAU_THANH_MOI))


def _chu_tu_vung(chu):
    """NFC, chữ thường, bỏ ký tự định dạng (ZWSP, gạch mềm…), dấu thanh kiểu mới quy về kiểu của từ vựng."""
    t = "".join(c for c in unicodedata.normalize("NFC", chu).lower() if unicodedata.category(c) != "Cf")
    return _DAU_THANH.sub(lambda m: _DAU_THANH_MOI[m.group(0)], t)


def _cum(chu):
    """Luật 2, 3, 5: các cụm (vị trí, nhãn) khớp dài nhất, trái sang phải, không chồng nhau."""
    tu = _TU.findall(_chu_tu_vung(chu))
    out, i = [], 0
    while i < len(tu):
        if tu[i] == ":":
            out.append((i, None))
            i += 1
            continue
        hit = next((m for m in _THEO_TU_DAU.get(_bo_dau(tu[i]), ())
                    if len(m[0]) <= len(tu) - i and all(_khop_tu(tu[i + p], m[0][p]) for p in range(len(m[0])))), None)
        if hit:
            out.append((i, hit[1]))
            i += len(hit[0])
        else:
            i += 1
    ket = []
    for vt, nhan in out:
        if nhan is None:
            truoc = any(n and "T" in n for v, n in out if v < vt)
            sau = any(n and "T" in n for v, n in out if v > vt)
            nhan = frozenset("Q") if truoc and sau else frozenset()
        ket.append((vt, nhan))
    return ket


def _la_ung_vien(chu):
    """Luật 6: hai cụm khác nhau, một mang nhãn thuật ngữ, một mang nhãn quan hệ."""
    c = _cum(chu)
    return any(a[0] != b[0] and "T" in a[1] and "Q" in b[1] for a in c for b in c)


# ------------------------------------------------------------------ đoạn toán, câu, toán viết trần
_MO = (("$$", ("$$", "$")), ("\\(", ("\\)",)), ("\\[", ("\\]",)), ("$", ("$",)))
_DOI_KY_TU = str.maketrans({"′": "'", "’": "'", "‘": "'", "ʹ": "'", "−": "-", "‐": "-", "‑": "-", "‒": "-", "–": "-",
                            "—": "-", "―": "-", "⩾": "≥", "⩽": "≤", "·": "*", "×": "*"})
_CHE, _CHE_DAN = "\ue000", "\ue001"
_DAN = re.compile(r"\[\d{1,3}\]")


def _doan_toan(t):
    """Đoạn toán có dấu phân cách: (đầu, cuối, đầu ruột, cuối ruột, đóng đúng). Dấu mở không có dấu đóng: đoạn chạy tới
    hết câu trả lời; `$$` đóng bằng `$` là dấu phân cách hỏng."""
    out, i = [], 0
    while i < len(t):
        mo = next((m for m in _MO if t.startswith(m[0], i)), None)
        if not mo:
            i += 1
            continue
        a = i + len(mo[0])
        dong = [(t.find(d, a), d) for d in mo[1]]
        dong = [(j, d) for j, d in dong if j >= 0]
        if not dong:
            out.append((i, len(t), a, len(t), False))
            break
        j, d = min(dong, key=lambda jd: (jd[0], jd[1] != mo[1][0]))
        out.append((i, j + len(d), a, j, d == mo[1][0]))
        i = j + len(d)
    return out


def _mot_ky_tu(c):
    """Ký tự tương thích (toàn khổ «＝», «２», «ｘ») về dạng thường khi NFKC cho đúng một ký tự; ký hiệu toán giữ nguyên."""
    if c in _KY_TOAN:
        return c
    k = unicodedata.normalize("NFKC", c)
    return k if len(k) == 1 else c


def _nhin(t, doan):
    """Bản nhìn cùng độ dài của câu trả lời: đoạn toán và dấu dẫn [n] bị che, ký tự tương thích, dấu phẩy trên và dấu trừ
    Unicode quy về ASCII."""
    v = list("".join(_mot_ky_tu(c) for c in t).translate(_DOI_KY_TU))
    for d in doan:
        v[d[0]:d[1]] = _CHE * (d[1] - d[0])
    return _DAN.sub(lambda m: _CHE_DAN * len(m.group(0)), "".join(v))


_RUOT_KHOANG = re.compile(r"\s*[-+]?\s*[\w∞]+(?:[.,]\d+)?\s*[;,]\s*[-+]?\s*[\w∞]+(?:[.,]\d+)?\s*")


def _khoang(s):
    """Các khoảng (đầu, cuối) dạng (a; b): mở bằng một dấu mở ngoặc Unicode hay «]», đóng bằng dấu đóng ngoặc hay «[»
    (cách viết ]a; b[), hai đầu mút cách nhau bởi ; hay ,."""
    out = []
    for i, ch in enumerate(s):
        if ch == "]" or unicodedata.category(ch) == "Ps":
            m = _RUOT_KHOANG.match(s, i + 1)
            if m and m.end() < len(s) and (s[m.end()] == "[" or unicodedata.category(s[m.end()]) == "Pe"):
                out.append((i, m.end() + 1))
    return out


def _tach_cau(v):
    """Luật 1: tách tại . ; ? ! và xuống dòng, trừ trong ngoặc (mọi dấu ngoặc Unicode), trong khoảng (a; b) hay ]a; b[,
    trong đoạn toán (đã che) hay dấu chấm giữa hai chữ số."""
    trong = {i for a, b in _khoang(v) for i in range(a, b)}
    moc, dau, sau = [], 0, 0
    for i, ch in enumerate(v):
        if i in trong:
            continue
        if unicodedata.category(ch) == "Ps":
            sau += 1
        elif unicodedata.category(ch) == "Pe":
            sau = max(0, sau - 1)
        elif ch in ".;?!\n" and sau == 0 and not (ch == "." and 0 < i < len(v) - 1 and v[i - 1].isdigit() and v[i + 1].isdigit()):
            moc.append((dau, i + 1))
            dau = i + 1
    moc.append((dau, len(v)))
    out = []
    for a, b in moc:
        while a < b and v[a].isspace():
            a += 1
        while b > a and v[b - 1].isspace():
            b -= 1
        if a < b:
            out.append((a, b))
    return out


def _cac_cau_chu(text):
    v = _nhin(text, _doan_toan(text))
    return [text[a:b] for a, b in _tach_cau(v)]


_SO_MU = frozenset("⁰¹²³⁴⁵⁶⁷⁸⁹ⁿ⁻⁺₀₁₂₃₄₅₆₇₈₉")
_KY_TOAN = frozenset("=≠≤≥<>⇒⇔→←↔'^_+-*/÷±√∞∈∉⊂∪∩ℝΔ∆()[]{}|") | _SO_MU
_TEN_HAM = frozenset({"sin", "cos", "tan", "cot", "ln", "log", "exp", "sqrt", "lim"})
# Danh sách trắng của chữ ngoài $…$ (ADR 013 mục 2, đóng mặc định): chữ Latin (cả tiếng Việt), chữ số ASCII, khoảng
# trắng, dấu câu của lời văn và các dấu tùy chỗ. Mọi ký tự khác (dấu giống dấu bằng, ngoặc ⟨ ⟩, [ ], emoji, ký tự điều
# khiển…) là dấu hiệu toán. Các dấu gạch đã quy về «-» trong _DOI_KY_TU.
_CAU_VAN = frozenset(",.;:!?«»\"“”…")
_TUY_CHO = frozenset("()+-*/'")
# Dấu tùy chỗ là toán khi: phẩy trên dính sau chữ (lọc khi gom), phép toán giữa hai toán hạng, dấu trước chữ số, chữ
# số kề chữ biến (cả nhân viết cách «3 x»), hàm áp lên đối số, khoảng (a; b).
_DAU_HIEU_TRAN = re.compile(
    r"'|[A-Za-z0-9)]\s*[-+*/]\s*[A-Za-z0-9(]|[-+]\s*\d|\d[A-Za-z]|\d\s+[A-Za-z]\b|[A-Za-z]\d"
    r"|(?<![A-Za-z])[A-Za-z]\s*\(|\(" + _RUOT_KHOANG.pattern + r"\)")


def _chu_latin(c):
    return unicodedata.category(c) in ("Lu", "Ll", "Lt") and unicodedata.name(c, "").startswith("LATIN ")


def _dau_toan(c):
    return not (c in "0123456789" or c.isspace() or c in _CAU_VAN or c in _TUY_CHO or c in (_CHE, _CHE_DAN)
                or _chu_latin(c))


def _tu_to(s):
    """Từ tố của chữ ngoài $…$: (loại, đầu, cuối). 'so', 'lenh' (\\…), 'chu' (từ ASCII), 'cham' (, ; .), 'cach', 'ngat'
    (từ có dấu, dấu câu khác, đoạn đã che), 'ky' (dấu tùy chỗ và mọi dấu hiệu toán)."""
    out, i = [], 0
    while i < len(s):
        ch = s[i]
        if ch.isspace():
            j = i
            while j < len(s) and s[j].isspace():
                j += 1
            out.append(("cach", i, j))
        elif ch == "\\":
            j = i + 1
            while j < len(s) and s[j].isascii() and s[j].isalpha():
                j += 1
            j = max(j, min(i + 2, len(s)))
            out.append(("lenh", i, j))
        elif ch in "0123456789":
            j = i + re.match(r"\d+(?:[.,]\d+)?", s[i:]).end()
            out.append(("so", i, j))
        elif _chu_latin(ch):
            j = i
            while j < len(s) and _chu_latin(s[j]):
                j += 1
            out.append(("chu" if s[i:j].isascii() else "ngat", i, j))
        else:
            j = i + 1
            out.append(("cham" if ch in ",;." else "ngat" if ch in _CAU_VAN or ch in (_CHE, _CHE_DAN) else "ky", i, j))
        i = j
    return out


def _toan_tran(s):
    """Các đoạn toán viết trần trong chữ đã che `s`: [(đầu, cuối)]. Một đoạn là dãy từ tố toán, cách nhau tối đa bởi
    khoảng trắng, có một ký tự ngoài danh sách trắng hay một dấu tùy chỗ đứng ở chỗ toán. Từ ASCII là toán khi chỉ một
    chữ cái, là tên hàm, hay dính vào ký hiệu toán; dấu phẩy trên chỉ là toán khi dính sau chữ, ngoặc đóng hay dấu phẩy
    trên khác; dấu , ; là toán trong ngoặc hay trong khoảng."""
    to = _tu_to(s)
    trong_khoang = {i for a, b in _khoang(s) for i in range(a, b)}

    def dinh(k, l):
        return 0 <= l < len(to) and to[l][0] in ("ky", "so", "lenh") and (to[l][2] == to[k][1] or to[k][2] == to[l][1])

    out, cur, sau = [], [], 0

    def dong():
        doan = s[cur[0]:cur[1]] if cur else ""
        if doan and (any(_dau_toan(c) for c in doan) or _DAU_HIEU_TRAN.search(doan)):
            out.append(tuple(cur))

    for k, (loai, a, b) in enumerate(to):
        if loai == "cach":
            continue
        w = s[a:b]
        if loai == "ky" and w == "'":
            toan = k > 0 and to[k - 1][2] == a and (to[k - 1][0] == "chu" or s[a - 1] in ")}]'")
        elif loai == "chu":
            toan = len(w) == 1 or w in _TEN_HAM or dinh(k, k - 1) or dinh(k, k + 1)
        elif loai == "cham":
            toan = sau > 0 or a in trong_khoang
        else:
            toan = loai in ("ky", "so", "lenh")
        if not toan:
            dong()
            cur, sau = [], 0
            continue
        cur = [cur[0] if cur else a, b]
        sau += sum({"Ps": 1, "Pe": -1}.get(unicodedata.category(c), 0) for c in w)
    dong()
    return out


# ------------------------------------------------------------------ LaTeX hỏng
_LENH_BIET = frozenset((
    "frac dfrac tfrac sqrt left right cdot times div pm mp le leq ge geq leqslant geqslant ne neq lt gt to rightarrow "
    "Rightarrow longrightarrow Longrightarrow leftarrow Leftarrow Leftrightarrow iff implies infty in notin cup cap "
    "setminus subset subseteq mathbb mathrm mathbf text textrm textit mbox operatorname prime sin cos tan cot ln log exp "
    "lim min max Delta delta alpha beta pi quad qquad ldots dots cdots mid vert lvert rvert lbrace rbrace langle rangle "
    "displaystyle forall exists emptyset varnothing approx equiv sim neg land lor wedge vee overline bar hat circ").split())
_DONG_NGOAC = r"(?:[()\[\].|]|\\[{}]|\\[lr]?vert|\\[lr]brace|\\[lr]angle)"
_CUT = re.compile(r"(?:[=+\-*/^_<>≤≥≠,]|\\(?:le|leq|ge|geq|ne|neq|lt|gt|to|rightarrow|Rightarrow|cdot|times|div|in|pm"
                  r"|setminus|cup|cap|frac|dfrac|sqrt|left|right|text))\s*$")
_KHOANG_LATEX = re.compile(r"(?:\\[,;! ]|\\quad|\\qquad|\s)+$")


def _latex_hong(s):
    """Lý do LaTeX hỏng, hoặc None: ngoặc nhọn hay ngoặc tròn không cân, \\left / \\right không đi đôi, lệnh lạ,
    \\frac thiếu nhóm, biểu thức cụt (kết thúc bằng phép toán hay quan hệ), rỗng."""
    t = s.strip()
    if not t:
        return "đoạn toán rỗng"
    nhon = tron = trai = phai = 0
    for m in re.finditer(r"\\([A-Za-z]+|.?)|[{}()\[\]]", t):
        g = m.group(0)
        if g.startswith("\\"):
            ten = m.group(1)
            if not ten:
                return "dấu \\ cụt"
            if ten.isalpha() and ten not in _LENH_BIET:
                return "lệnh \\%s máy không biết" % ten
            if ten in ("left", "right"):
                if not re.match(r"\s*" + _DONG_NGOAC, t[m.end():]):
                    return "\\%s không đi với dấu ngoặc" % ten
                trai, phai = trai + (ten == "left"), phai + (ten == "right")
            continue
        nhon += {"{": 1, "}": -1}.get(g, 0)
        tron += {"(": 1, "[": 1, ")": -1, "]": -1}.get(g, 0)
        if nhon < 0 or tron < 0:
            return "ngoặc không cân"
    if nhon or tron or trai != phai:
        return "ngoặc không cân"
    if "\\frac" in t or "\\dfrac" in t or "\\tfrac" in t:
        if dct._bo_frac(t) is None:
            return "\\frac thiếu tử hay mẫu"
    if _CUT.search(_KHOANG_LATEX.sub("", t)):
        return "biểu thức cụt"
    return None


# ------------------------------------------------------------------ trích đề: chuẩn hóa cách viết, không rút gọn
_MU_THUONG = str.maketrans("⁰¹²³⁴⁵⁶⁷⁸⁹", "0123456789")


def _khop_ngoac(t, i):
    sau = 0
    for j in range(i, len(t)):
        sau += {"(": 1, ")": -1}.get(t[j], 0)
        if sau == 0:
            return j
    return -1


def _bo_ngoac_thua(t):
    """(x) → x, ((A)) → (A), bỏ cặp ngoặc bọc cả biểu thức. Không đổi thứ tự hay dấu của hạng tử nào."""
    while True:
        cu = t
        t = re.sub(r"\((x|\d+(?:\.\d+)?)\)", r"\1", t)
        i = t.find("((")
        while i >= 0:
            j = _khop_ngoac(t, i + 1)
            if j >= 0 and j + 1 < len(t) and t[j + 1] == ")" and _khop_ngoac(t, i) == j + 1:
                t = t[:i] + t[i + 1:j + 1] + t[j + 2:]
                break
            i = t.find("((", i + 1)
        if t.startswith("(") and _khop_ngoac(t, 0) == len(t) - 1:
            t = t[1:-1]
        if t == cu:
            return t


def _dang_viet(s):
    """Chuẩn hóa cách viết của biểu thức một biến x (KD-0005, ADR 013 mục 1): khoảng trắng, ^ ≡ **, nhân ẩn ≡ *, ngoặc
    nhọn của số mũ, \\frac{a}{b} ≡ (a)/(b), bỏ «y =» hay «f(x) =» đứng đầu. None khi không phải biểu thức đa thức hay
    phân thức của x."""
    t = unicodedata.normalize("NFC", str(s)).translate(_DOI_KY_TU)
    t = re.sub(r"[⁰¹²³⁴⁵⁶⁷⁸⁹]+", lambda m: "^" + m.group(0).translate(_MU_THUONG), t)
    t = dct._BO_NGOAC.sub("", t).replace("\\cdot", "*").replace("\\times", "*")
    for a in ("\\,", "\\;", "\\!", "\\ "):
        t = t.replace(a, " ")
    t = dct._bo_frac(t)
    if t is None:
        return None
    t = re.sub(r"\s+", "", t.replace("**", "^").replace("{", "(").replace("}", ")"))
    t = re.sub(r"^(?:y|f\(x\))=", "", t)
    if not re.fullmatch(r"[0-9x+\-*/^().]+", t):
        return None
    t = re.sub(r"(?<=[0-9x)])(?=[x(])", "*", t)
    return _bo_ngoac_thua(t)


def _bieu_thuc_de(ham):
    """Hàm của đề và, với phân thức, tử và mẫu của nó, ở dạng đã chuẩn hóa cách viết. Chỉ giữ biểu thức có x."""
    h = _dang_viet(ham) if ham else None
    if not h:
        return frozenset()
    out = {h}
    cap, sau = [], 0
    for i, ch in enumerate(h):
        sau += {"(": 1, ")": -1}.get(ch, 0)
        if sau == 0 and ch in "+-*/^" and i > 0:
            cap.append((i, ch))
    if len(cap) == 1 and cap[0][1] == "/":
        out |= {_bo_ngoac_thua(h[:cap[0][0]]), _bo_ngoac_thua(h[cap[0][0] + 1:])}
    return frozenset(b for b in out if "x" in b)


# ------------------------------------------------------------------ trích bài làm
_KHUNG_HS = re.compile(
    r"(?:\b(?:em|bạn)\s+(?:(?:đã|vừa|có)\s+)?(?:viết|ghi|nộp|chép|tính ra|tìm ra|tính được|tìm được|ra)"
    r"|\bdòng\s+(?:(?:em|bạn)\s+(?:(?:đã|vừa)\s+)?(?:viết|ghi|nộp)|của\s+(?:em|bạn)))"
    r"(?:\s+(?:là|rằng|được))?\s*[:,]?\s*[«\"“]?\s*$")
_NOI_TRICH = re.compile(r"\s*(?:,|và|hoặc|rồi)\s*")


def _khoa_hs(s):
    return re.sub(r"\s+", "", unicodedata.normalize("NFC", str(s)).strip().strip("$"))


# ------------------------------------------------------------------ dòng bảng
_CHUNG = re.compile(r"\b(?:Du|Dv|u|v|n)\b")
_VE_TRAI_DAO_HAM = re.compile(r"^\s*\(.*\)\s*(?:'|\^\s*\{?\s*\\prime\s*\}?)\s*=")
_KY_HIEU_RIENG = re.compile(r"[A-Za-z](?:_\{?[A-Za-z0-9]+\}?)?'*(?:\([A-Za-z0-9_{}]*\))?")
_MENH_DE = re.compile(r"\\(?:text|textrm|mbox|Rightarrow|Longrightarrow|implies|iff|Leftrightarrow|to|rightarrow)(?![A-Za-z])|[⇒⇔→]")
_KY_HIEU_DE = frozenset("xyfD")
_NHOM_CHU = re.compile(r"\\(?:text|textrm|textit|mbox|mathbb|mathrm|mathbf|operatorname)\s*\{[^{}]*\}")


def _dang_quy_tac(inner):
    """Đoạn có dạng quy tắc: vế trái (…)', mệnh đề (chữ trong \\text, mũi tên suy ra), hay ký hiệu chung ngoài x, y, f,
    D. Dạng này chỉ DAT qua bảng."""
    t = dct._BO_NGOAC.sub("", dct._nfc(inner))
    if _VE_TRAI_DAO_HAM.match(t) or _MENH_DE.search(t):
        return True
    chu = re.sub(r"\\[A-Za-z]+", " ", _NHOM_CHU.sub(" ", t))
    return bool(set(re.findall(r"[A-Za-z]", chu)) - _KY_HIEU_DE)


def _ve_phai(inner):
    """Biểu thức không có dấu bằng mà có ký hiệu chung u, v, u', v', n (vế phải của một quy tắc), dạng của bộ đọc tầng 1."""
    s = dct._BO_NGOAC.sub(" ", dct._nfc(inner))
    s = re.sub(r"\^\s*\{\s*\\prime\s*\}", "'", s).replace("\\prime", "'").replace("\\cdot", "*").replace("\\times", "*")
    for a in ("\\,", "\\;", "\\!", "\\ "):
        s = s.replace(a, " ")
    s = dct._bo_frac(s)
    r = dct._bieu_thuc(s.replace("{", "(").replace("}", ")")) if s else None
    return r if r and _CHUNG.search(r) else None


def _chuan_bang(s):
    """Luật 8: NFC, chữ thường, bỏ . , ; : ! ? và dấu dẫn [n], gộp khoảng trắng."""
    return re.sub(r"[\s.,;:!?]+", " ", _DAN.sub(" ", unicodedata.normalize("NFC", s).lower())).strip()


def _phu(m, cua_dong):
    """Mệnh đề m có trong các mệnh đề của dòng; với đơn điệu, hai khoảng tổng quát (K, «một khoảng») là một."""
    return any(p == m or (p[0] == m[0] == "DD" and p[1:3] == m[1:3] and dct._khoang_chung(p[3]) and dct._khoang_chung(m[3]))
               for p in cua_dong)


class _Dong:
    """Một dòng bảng đã khóa. Dùng được khi có trích dẫn và tầng 1 tính lại ra DAT như lúc khóa (dong_cong_thuc)."""

    def __init__(self, d):
        self.id = d.get("id")
        self.latex, self.phat_bieu = str(d.get("latex") or ""), str(d.get("phat_bieu") or "")
        self.tieu_de = str(d.get("tieu_de") or "")
        td = d.get("trich_dan")
        tai_lieu = td.get("tai_lieu") if isinstance(td, dict) else None
        self.co_trich_dan = isinstance(tai_lieu, str) and bool(tai_lieu.strip())
        self.qua_dai = len(self.latex) > dct.DO_DAI_LATEX_TOI_DA or len(self.phat_bieu) > dct.DO_DAI_LOI_TOI_DA
        self.khoa = dct._khoa_cong_thuc(self.latex) if self.latex and not self.qua_dai else None
        self.dang_thuc = dct._tach_dang_thuc(self.latex) if self.khoa else None
        self.menh, self.khong_doc = [], []
        if not self.qua_dai and not self.dang_thuc:
            self.menh, self.khong_doc = dct._doc_dong(self.latex + ". " + self.phat_bieu, self.tieu_de)
        self.cau = frozenset(_chuan_bang(c) for c in _cac_cau_chu(self.phat_bieu)) if not self.qua_dai else frozenset()
        self._tang1 = None

    def tang1(self, ngu_canh):
        if self._tang1 is None:
            if self.qua_dai or not self.id:
                self._tang1 = {"trang_thai": KKD}
            elif self.dang_thuc:
                self._tang1 = dct._kiem_dang_thuc(self.dang_thuc[0], self.dang_thuc[1], self.phat_bieu)
            elif self.menh:
                self._tang1 = dct._tang_1_dinh_li(self.menh, self.khong_doc, ngu_canh.mau())
            else:
                self._tang1 = {"trang_thai": KKD}
        return self._tang1


class _NguCanh:
    def __init__(self, bang, bai_lam, ham):
        self.dong = [_Dong(d) for d in bang if isinstance(d, dict)]
        self.bai_lam = frozenset(k for k in (_khoa_hs(l) for l in bai_lam) if k)
        self.de = _bieu_thuc_de(ham)
        self._mau = None
        self._ky_hieu = None
        self._da_doc = {}

    def mau(self):
        if self._mau is None:
            self._mau = dct._Mau()
        return self._mau

    def _doc(self, s):
        """Chuỗi của bộ đọc tầng 1 → biểu thức SymPy (bộ phân tích an toàn F-01), hoặc None."""
        if s not in self._da_doc:
            import sympy as sp
            from app.paths import load_kiem

            if self._ky_hieu is None:
                x = sp.Symbol("x", real=True)
                u, v = sp.Function("u")(x), sp.Function("v")(x)
                self._ky_hieu = {"x": x, "n": sp.Symbol("n", integer=True, positive=True), "k": sp.Symbol("k", real=True),
                                 "c": sp.Symbol("c", real=True), "u": u, "v": v,
                                 "Du": sp.Derivative(u, x), "Dv": sp.Derivative(v, x)}
            try:
                self._da_doc[s] = load_kiem().phan_tich_an_toan(s, self._ky_hieu)
            except ValueError:
                self._da_doc[s] = None
        return self._da_doc[s]

    def _bang_nhau(self, a, b):
        import sympy as sp

        ea, eb = self._doc(a), self._doc(b)
        return ea is not None and eb is not None and sp.simplify(ea - eb) == 0

    def _chon(self, dong):
        """Dòng đầu tiên dùng được trong các dòng khớp; không có thì dòng khớp đầu tiên (để báo lý do)."""
        return next((d for d in dong if self.dung_duoc(d)), dong[0] if dong else None)

    def dung_duoc(self, d):
        return d.co_trich_dan and d.tang1(self)["trang_thai"] == DAT

    def khop_doan(self, inner, dt, vp, menh, khong_doc):
        khoa = dct._khoa_cong_thuc(inner)
        dong = [d for d in self.dong if d.khoa and d.khoa == khoa]
        if not dong and dt:
            dong = [d for d in self.dong if d.dang_thuc and self._bang_nhau(dt[0], d.dang_thuc[0])
                    and self._bang_nhau(dt[1], d.dang_thuc[1])]
        if not dong and vp:
            dong = [d for d in self.dong if d.dang_thuc and _CHUNG.search(d.dang_thuc[1]) and self._bang_nhau(vp, d.dang_thuc[1])]
        if not dong and menh and not khong_doc:
            dong = self.khop_menh(menh)
        return self._chon(dong)

    def khop_menh(self, menh):
        return [d for d in self.dong if d.menh and not d.khong_doc and all(_phu(m, d.menh) for m in menh)]

    def khop_cau(self, cau):
        k = _chuan_bang(cau)
        return self._chon([d for d in self.dong if k in d.cau])

    def phan_quyet_dong(self, d, loai):
        if self.dung_duoc(d):
            return _pq(loai, DAT, dong_bang=d.id, tang={"1": DAT, "2": DAT, "3": DAT})
        t1 = d.tang1(self)["trang_thai"]
        ly_do = "Khớp dòng %s, nhưng dòng %s." % (d.id, "chưa có trích dẫn tài liệu" if t1 == DAT else "không đạt tầng 1 khi máy kiểm lại")
        return _pq(loai, SAI if t1 == SAI else KKD, dong_bang=d.id, ly_do=ly_do)


# ------------------------------------------------------------------ phán quyết
def _ngoai_bang(ctx, loai, dt, menh, khong_doc):
    """Công thức hay quy tắc không khớp dòng bảng nào: SAI khi máy có phản ví dụ, còn lại KHONG_KIEM_DUOC."""
    kq = None
    if dt:
        kq = dct._kiem_dang_thuc(dt[0], dt[1])
    elif menh:
        kq = dct._tang_1_dinh_li(menh, khong_doc, ctx.mau())
    if kq and kq["trang_thai"] == SAI:
        return _pq(loai, SAI, ly_do="Máy kiểm ra sai: %s" % (kq.get("can_cu") or "có phản ví dụ."), phan_vi_du=kq.get("phan_vi_du"))
    if kq and kq["trang_thai"] == DAT:
        return _pq(loai, KKD, ly_do="Đúng theo máy nhưng không khớp dòng bảng đã khóa nào.")
    if loai == QUY_TAC:
        return _pq(loai, KKD, ly_do="Ứng viên quy tắc bằng lời không khớp phát biểu dòng bảng nào; bỏ cả câu.")
    return _pq(loai, KKD, ly_do="Không khớp dòng bảng đã khóa nào.")


def _xep_doan(ctx, inner, dong_dung, khung_hs):
    """Phán quyết một đoạn toán có dấu phân cách (ruột `inner`)."""
    if not dong_dung:
        return _pq(KHONG_PHAN_TICH, KKD, ly_do="Dấu phân cách toán không đóng.")
    hong = _latex_hong(inner)
    if hong:
        return _pq(KHONG_PHAN_TICH, KKD, ly_do="LaTeX hỏng: %s." % hong)
    dt = dct._tach_dang_thuc(inner)
    vp = _ve_phai(inner) if not dt and "=" not in inner else None
    menh, khong_doc = dct._doc_dong(inner) if not dt and not vp else ([], [])
    dong = ctx.khop_doan(inner, dt, vp, menh, khong_doc)
    if dong:
        return ctx.phan_quyet_dong(dong, CONG_THUC)
    if dt or vp or menh or _dang_quy_tac(inner):
        return _ngoai_bang(ctx, CONG_THUC, dt, menh, khong_doc)
    if _dang_viet(inner) in ctx.de:
        return _pq(TRICH_DE_BAI, DAT)
    if khung_hs and _khoa_hs(inner) in ctx.bai_lam:
        return _pq(TRICH_BAI_LAM, DAT)
    if _KY_HIEU_RIENG.fullmatch(re.sub(r"\s+", "", dct._BO_NGOAC.sub("", dct._nfc(inner)))):
        return _pq(KHONG_PHAN_TICH, KKD, ly_do="Ký hiệu đứng riêng: không thuộc loại biểu thức nào của ADR 013.")
    return _pq(KET_QUA, KKD, ly_do="Kết quả tính cụ thể: không trùng nguyên văn đề hay dòng bài làm được dẫn là lời của học sinh.")


# Ký hiệu chung đứng riêng: y', f'(x) là thuật ngữ «đạo hàm»; x₀, y(x₀) là ký hiệu điểm. Chúng không mang giá trị nào,
# nên câu chứa chúng được xét từ vựng như khi viết bằng lời, không phải như toán không phân loại được.
_DAO_HAM_RIENG = re.compile(r"[yf]'(?:\(x\))?")
_DIEM_RIENG = re.compile(r"x_0|[yf]\(x_0\)")


def _ky_hieu_chung(s):
    """Chữ thay cho `s` khi xét từ vựng: «đạo hàm» với y', f'(x); "" với x₀, y(x₀); None khi `s` không phải ký hiệu
    chung đứng riêng."""
    t = dct._BO_NGOAC.sub("", unicodedata.normalize("NFC", s).translate(_DOI_KY_TU))
    t = re.sub(r"[\s{}]+", "", re.sub(r"\^\s*\{?\s*\\prime\s*\}?", "'", t).replace("₀", "_0"))
    if _DAO_HAM_RIENG.fullmatch(t):
        return "đạo hàm"
    return "" if _DIEM_RIENG.fullmatch(t) else None


def _doc_ten(s, ten):
    """Chữ của câu để xét từ vựng và nội dung: mỗi ký hiệu chung đứng riêng thay bằng chữ của nó."""
    out, k = [], 0
    for x0, x1, thay in sorted(ten):
        out += [s[k:x0], " %s " % thay]
        k = x1
    return "".join(out) + s[k:]


def _bo_phan_cach(cau):
    t = _DAN.sub(" ", cau)
    for d in ("$$", "$", "\\(", "\\)", "\\[", "\\]"):
        t = t.replace(d, " ")
    return t


def _xet_cau(ctx, t, v, a, b, doan):
    """(giữ câu?, phán quyết các biểu thức của câu, chữ của câu để xét từ vựng và nội dung)."""
    pq, ke, trich_truoc, ten = [], a, False, []
    for d0, d1, r0, r1, dung in doan:
        thay = _ky_hieu_chung(t[r0:r1]) if dung else None
        if thay is not None:
            ten.append((d0 - a, d1 - a, thay))
            trich_truoc = False
        else:
            truoc = t[ke:d0]
            khung = bool(_KHUNG_HS.search(truoc.lower())) or (trich_truoc and bool(_NOI_TRICH.fullmatch(truoc.lower())))
            p = dict({"doan": t[r0:r1].strip()}, **_xep_doan(ctx, t[r0:r1], dung, khung))
            trich_truoc = p["loai"] == TRICH_BAI_LAM
            pq.append((d0, p))
        ke = d1
    for x0, x1 in _toan_tran(v[a:b]):
        thay = _ky_hieu_chung(v[a + x0:a + x1])
        if thay is not None:
            ten.append((x0, x1, thay))
            continue
        pq.append((a + x0, dict({"doan": t[a + x0:a + x1]}, **_pq(KHONG_PHAN_TICH, KKD,
                                                                  ly_do="Toán viết trần ngoài $…$, không phân loại được."))))
    pq = [p for _, p in sorted(pq, key=lambda z: z[0])]
    chu = _doc_ten(v[a:b], ten)
    ung_vien = _la_ung_vien(chu)
    if not ung_vien and all(p["trang_thai"] == DAT for p in pq):
        return True, pq, chu
    cau = t[a:b]
    doan_cau = re.sub(r"[.;?!\s]+$", "", cau)
    dong = ctx.khop_cau(cau)
    menh, khong_doc = dct._doc_dong(_bo_phan_cach(cau)) if not dong else ([], [])
    if not dong and menh and not khong_doc:
        dong = ctx._chon(ctx.khop_menh(menh))
    if dong:
        q = dict({"doan": doan_cau}, **ctx.phan_quyet_dong(dong, QUY_TAC))
        if q["trang_thai"] == DAT:
            return True, [q], chu
        return False, [q] + pq, chu
    if ung_vien:
        pq.insert(0, dict({"doan": doan_cau}, **_ngoai_bang(ctx, QUY_TAC, None, menh if not khong_doc else [], [])))
    return False, pq, chu


def _co_noi_dung(chu, pq):
    """Câu giữ lại còn mệnh đề: có đoạn toán DAT, hoặc có từ ngoài danh sách từ nối và trợ từ."""
    if any(p["trang_thai"] == DAT for p in pq):
        return True
    return any(w not in TU_NOI for w in re.findall(r"[^\W\d_]+", chu.lower()))


def _loi_dau_vao(payload):
    cau, bang = payload.get("cau"), payload.get("bang_cong_thuc", [])
    hs, ham = payload.get("bai_lam_hoc_sinh", []), payload.get("ham", "")
    if (not isinstance(cau, str) or not isinstance(bang, list) or not isinstance(hs, list) or not isinstance(ham, str)
            or any(not isinstance(d, str) for d in hs)):
        return "DAU_VAO_KHONG_HOP_LE"
    if (len(cau) > CAU_TOI_DA or len(bang) > dct.SO_DONG_TOI_DA or len(hs) > DONG_HS_TOI_DA
            or any(len(d) > DO_DAI_DONG_HS_TOI_DA for d in hs) or len(ham) > HAM_TOI_DA):
        return "QUA_GIOI_HAN"
    return None


def _kiem(payload):
    loi = _loi_dau_vao(payload)
    if loi:
        return _dong_cua(loi)
    t = unicodedata.normalize("NFC", payload["cau"])
    doan = _doan_toan(t)
    if len(doan) > DOAN_TOAN_TOI_DA:
        return _dong_cua("QUA_GIOI_HAN")
    ctx = _NguCanh(payload.get("bang_cong_thuc", []), payload.get("bai_lam_hoc_sinh", []), payload.get("ham", ""))
    v = _nhin(t, doan)
    cac = _tach_cau(v)
    bieu_thuc, cac_cau, giu, noi_dung = [], [], [], False
    for i, (a, b) in enumerate(cac):
        ok, pq, chu = _xet_cau(ctx, t, v, a, b, [d for d in doan if a <= d[0] < b])
        bieu_thuc.extend(dict(p, cau=i) for p in pq)
        cac_cau.append({"cau": t[a:b], "giu": ok})
        if ok:
            giu.append(i)
            noi_dung = noi_dung or _co_noi_dung(chu, pq)
    cau_sach = "".join(t[cac[i][0]:cac[i + 1][0] if i + 1 < len(cac) else len(t)] for i in giu).strip()
    return {"cau_sach": cau_sach if noi_dung else "", "thay_bang_goi_y": not noi_dung, "bieu_thuc": bieu_thuc,
            "cac_cau": cac_cau}


def kiem_loi_giang(payload):
    """POST /v1/kiem-loi-giang (specs/001-lat-cat-doc/contracts/math-v1.md). Đóng mặc định với mọi lỗi."""
    try:
        return _kiem(payload if isinstance(payload, dict) else {})
    except Exception:
        return _dong_cua("LOI_KIEM_TRA")
