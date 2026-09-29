# -*- coding: utf-8 -*-
"""F-01: cổng danh sách trắng cho MỌI chuỗi học sinh gửi tới bộ chấm (và giá trị sự kiện gửi tới bộ lọc).

Chuỗi không bao giờ được thực thi (bộ đọc biểu thức là bộ phân tích an toàn của Kiểm định, không eval). Cổng này
quyết định PHÂN LOẠI: chuỗi không giống bài làm toán -> KHONG_KIEM_DUOC với ly_do DAU_VAO_KHONG_HOP_LE, thay vì
để các regex đọc ô bỏ qua phần lạ rồi chấm SAI hoặc (tệ hơn) DAT.

Danh sách trắng:
- ký tự: chữ cái (mọi bảng chữ, gồm tiếng Việt), chữ số, khoảng trắng và dấu toán/LaTeX liệt kê ở _DAU;
- từ ASCII: âm tiết tiếng Việt không dấu hợp lệ, hoặc tên toán/LaTeX trong _TU_TOAN; từ có chữ không phải ASCII
  (tiếng Việt có dấu) được nhận;
- '_' chỉ trong y_{CĐ}, y_{CT}, x_{1}, x_1 = … (theo sau là '{', cđ/ct hoa-thường, hoặc 1–2 chữ số rồi = , ; )); nháy đơn chỉ là dấu phẩy đạo hàm (y', f'(x));
- không có '.' nối tên (x.subs), không có tháp lũy thừa, số mũ >= 1000, số quá 15 chữ số; tối đa 300 ký tự/ô.
"""
import re
import unicodedata

DAU_VAO_KHONG_HOP_LE = "DAU_VAO_KHONG_HOP_LE"
DO_DAI_TOI_DA = 300

_DAU = set("+-−–—*/^()[]{};,.=<>≤≥≠|\\$'′∞∪∩ℝ∅±√∈∉·×:!?%… ")
_TU_TOAN = frozenset("""
x y f g e pi oo r d s u v sqrt abs log ln sin cos tan cot exp frac dfrac tfrac cdot times left right infty
mathbb mathbf mathrm text textrm operatorname setminus cup cap in notin emptyset varnothing lor vee land wedge quad
qquad le ge leq geq lt gt ne neq pm mp prime rightarrow leftrightarrow implies iff to lim cd ct none null delta
approx ldots cdots dots circ mid begin end array cases dx
""".split())
_AM_TIET = re.compile(
    r"^(?:ngh|ng|nh|ch|gh|gi|kh|ph|qu|th|tr|[bcdghklmnprstvx])?"
    r"(?:uye|uya|uyu|uoi|uou|ieu|yeu|oai|oay|oao|oeo|uay|uai|ai|ao|au|ay|eo|eu|ia|ie|iu|oa|oe|oi|oo|ua|ue|ui|uo|uu|uy|ya|ye|[aeiouy])"
    r"(?:ch|ng|nh|[cmnpt])?$"
)
# 0002d: thêm chỉ số chữ hoa/thường lẫn lộn (Ct, cT) và chỉ số số không có {} ở vị trí nhãn (x_1 = 1); "x_1 + 1" vẫn bị từ chối
_GACH_DUOI_OK = re.compile(r"_(?=\s*\{|\s*[cC]\s*[dDđĐtT](?![A-Za-z0-9_])|\s*\d{1,2}\s*(?:=|,|;|\)|$))")
_THAP = re.compile(r"(?:\*\*|\^)\s*[\(\{]?\s*[\w.+\-]+\s*[\)\}]?\s*(?:\*\*|\^)")
_MU_LON = re.compile(r"(?:\*\*|\^)\s*[\(\{]?\s*[+\-]?\s*\d{4,}")
_SO_DAI = re.compile(r"\d{16,}")
_O_TRONG_UI_CU = re.compile(r"^\s*(?:không|khong)\s+(?:dong_bien|nghich_bien|cuc_dai|cuc_tieu)\s*$", re.I)
_THUOC_TINH = re.compile(r"[A-Za-z0-9)\]]\.[A-Za-z_]{2,}|\.\s*[A-Za-z_]{2,}\s*\(")


def ly_do_tu_choi(s, do_dai_toi_da=DO_DAI_TOI_DA):
    """None nếu chuỗi được nhận; ngược lại chuỗi mô tả lý do (không chứa lại nội dung chuỗi)."""
    if s is None:
        return None
    if isinstance(s, bool) or not isinstance(s, (str, int, float)):
        return "không phải chuỗi"
    t = unicodedata.normalize("NFC", str(s))
    if len(t) > do_dai_toi_da:
        return "quá dài (%d ký tự)" % len(t)
    if _O_TRONG_UI_CU.match(t):
        return None  # ô trống của UI cũ (6eb8b06) gửi "không <khoá>", vd "không cuc_dai"
    for i, ch in enumerate(t):
        if ch.isalnum() or ch.isspace() or ch in _DAU:
            if ch == "'" and not (i > 0 and (t[i - 1].isalpha() or t[i - 1] in "'′)}")):
                return "dấu nháy không phải dấu đạo hàm"
            continue
        if ch == "_" and _GACH_DUOI_OK.match(t, i):
            continue
        return "ký tự không cho phép: U+%04X" % ord(ch)
    if "__" in t:
        return "ký tự không cho phép"
    if _THAP.search(t):
        return "tháp lũy thừa"
    if _MU_LON.search(t):
        return "số mũ quá lớn"
    if _SO_DAI.search(t):
        return "số quá lớn"
    if _THUOC_TINH.search(t):
        return "truy cập thuộc tính"
    for w in re.findall(r"[^\W\d_]+", t):
        if not w.isascii():
            continue
        lw = w.lower()
        if len(lw) == 1 or lw in _TU_TOAN or _AM_TIET.match(lw):
            continue
        return "từ không thuộc danh sách trắng: %s" % w[:20]
    return None


# Giá trị enum của ô dấu/biến thiên: kiểm riêng ở bộ chấm, không qua cổng từ.
_ENUM_O = frozenset({"+", "-", "−", "–", "0", "||", "TANG", "GIAM", "TANG_LEN", "GIAM_XUONG", "KHONG_XD", ""})


def chuoi_hs_trong_payload(payload):
    """Liệt kê (ma_buoc, dong, o, chuoi) cho mọi chuỗi học sinh trong payload chấm."""
    out = [(None, None, None, payload.get("ham"))]
    for st in payload.get("cac_buoc") or []:
        if not isinstance(st, dict):
            out.append((None, None, None, st))
            continue
        ma = st.get("ma_buoc")
        for ln in st.get("cac_dong") or []:
            if isinstance(ln, dict):
                out.append((ma, ln.get("dong"), None, ln.get("latex")))
            else:
                out.append((ma, None, None, ln))
        for c in ((st.get("bang") or {}).get("cac_o") or []):
            if not isinstance(c, dict):
                out.append((ma, None, None, c))
                continue
            v = c.get("gia_tri")
            if c.get("hang") not in ("X", "x") and str(v).strip().upper() in _ENUM_O:
                continue
            out.append((ma, None, {"hang": c.get("hang"), "k": c.get("k")}, v))
    return out


def tu_choi_payload(payload):
    """(ma_buoc, dong, o, ly_do) của chuỗi bị từ chối đầu tiên, hoặc None."""
    for ma, dong, o, s in chuoi_hs_trong_payload(payload):
        ld = ly_do_tu_choi(s)
        if ld:
            return ma, dong, o, ld
    return None
