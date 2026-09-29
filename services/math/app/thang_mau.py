# -*- coding: utf-8 -*-
"""Thang gợi ý mẫu 3 cấp của Sư phạm (supham/thang-goi-y-mau, v0.1, 52 thang) — nạp, chọn, điền tham số.

Quy tắc dùng (README Sư phạm §4–§5):
- Chọn thang theo (ma_buoc, loai_ket_qua); không có thì `chung`. DAU_DOI_TRONG_KHOANG mặc định dùng B.DH.NGHIEM/DIEM_THIEU.
  DAT không cần gợi ý; KHONG_KIEM_DUOC dùng `chung`.
- Chỉ điền {ham}, {tu}, {mau}, NGUYÊN VĂN chuỗi trong đề. Câu còn chỗ điền lạ / thiếu giá trị -> bỏ câu.
- Cấp `null`: không hiện dòng nào, KHÔNG BAO GIỜ trả `ly_do_trong`; trả `hanh_dong` (BAI_TUONG_TU_DE_HON).
- Câu đã điền vẫn đi qua bộ lọc lộ đáp án. Bị chặn -> lùi cấp thấp hơn cùng thang, rồi `chung` cùng cấp; ghi log.
File JSON chép nguyên văn vào app/data/thang-goi-y-mau/ (sha256 ở MANIFEST.txt); không sửa tay.
"""
import json
import logging
import os
import re

log = logging.getLogger("thang_mau")

THU_MUC = os.path.join(os.path.dirname(__file__), "data", "thang-goi-y-mau")
FILE = {"bac_ba": "bac-ba.json", "trung_phuong": "trung-phuong.json", "huu_ti": "huu-ti.json"}
BUOC = ("B.DH.TXD", "B.DH.DAOHAM", "B.DH.NGHIEM", "B.DH.XETDAU", "B.DH.KETLUAN")
THAM_SO = ("ham", "tu", "mau")
_CHO_DIEN = re.compile(r"\{([A-Za-z_][A-Za-z0-9_]*)\}")
_CACHE = {}


def nap(dang):
    if dang not in FILE:
        return None
    if dang not in _CACHE:
        with open(os.path.join(THU_MUC, FILE[dang]), encoding="utf-8") as f:
            _CACHE[dang] = json.load(f)
    return _CACHE[dang]


def dang_cua(ham):
    """bac_ba | trung_phuong | huu_ti | None. Đọc bằng bộ phân tích an toàn của tầng 1 (F-01), không eval."""
    if not ham or not isinstance(ham, str) or len(ham) > 200:
        return None
    try:
        import sympy as sp
        from app.paths import load_kiem
        f = load_kiem().P(ham.replace("^", "**"))
        fs = [v for v in f.free_symbols]
        if len(fs) != 1 or fs[0].name != "x":
            return None
        x = fs[0]
        tu, mau = sp.fraction(sp.together(f))
        if mau.has(x):
            pt, pm = sp.Poly(tu, x), sp.Poly(mau, x)
            if pm.degree() == 1 and pt.degree() <= 1 and sp.simplify(sp.diff(f, x)) != 0:
                return "huu_ti"
            return None
        p = sp.Poly(f, x)
        if p.degree() == 3:
            return "bac_ba"
        if p.degree() == 4 and all(k[0] % 2 == 0 for k in p.monoms()):
            return "trung_phuong"
    except Exception:
        return None
    return None


def _ngoac(s, i):
    """s[i] == '{' -> (nội dung, vị trí sau '}') theo cặp ngoặc; lỗi -> None."""
    if i >= len(s) or s[i] != "{":
        return None
    sau, j = 0, i
    while j < len(s):
        if s[j] == "{":
            sau += 1
        elif s[j] == "}":
            sau -= 1
            if sau == 0:
                return s[i + 1:j], j + 1
        j += 1
    return None


def tham_so_tu_de(de_bai):
    """Lấy {ham}, {tu}, {mau} NGUYÊN VĂN từ đề: '$y = <biểu thức>$'. Không tính lại, không rút gọn."""
    out = {}
    if not de_bai or not isinstance(de_bai, str):
        return out
    m = re.search(r"\$\s*y\s*=\s*([^$]+?)\s*\$", de_bai)
    tex = bool(m)
    if not m:
        # đề không có $…$: "y = x³ − 6x² + 9x + 2." hoặc "y = x^{3} - 3 x^{2} + 2." (chỉ ký tự của biểu thức)
        m = re.search(r"\by\s*=\s*((?:\\[a-z]+|[0-9x{}^+\-−–*/()·⁰¹²³⁴⁵⁶⁷⁸⁹ ])+?)\s*(?:[.;,:](?:\s|$)|$)", de_bai.strip())
        if not m:
            return out
        tex = bool(re.search(r"[\\^{}]", m.group(1)))
    boc = (lambda v: "$%s$" % v) if tex else (lambda v: v)
    ham = m.group(1).strip()
    out["ham"] = boc(ham)
    if ham.startswith(("\\frac", "\\dfrac")):
        k = ham.index("{")
        a = _ngoac(ham, k)
        b = _ngoac(ham, a[1]) if a else None
        if a and b and b[1] == len(ham):
            out["tu"], out["mau"] = boc(a[0].strip()), boc(b[0].strip())
    else:
        m2 = re.fullmatch(r"\(([^()]+)\)\s*/\s*\(([^()]+)\)", ham)
        if m2:
            out["tu"], out["mau"] = boc(m2.group(1).strip()), boc(m2.group(2).strip())
    return out
    ham = m.group(1).strip()
    out["ham"] = "$%s$" % ham
    fr = ham.startswith(("\\frac", "\\dfrac"))
    if fr:
        k = ham.index("{")
        a = _ngoac(ham, k)
        b = _ngoac(ham, a[1]) if a else None
        if a and b and b[1] == len(ham):
            out["tu"], out["mau"] = "$%s$" % a[0].strip(), "$%s$" % b[0].strip()
    else:
        m2 = re.fullmatch(r"\(([^()]+)\)\s*/\s*\(([^()]+)\)", ham)
        if m2:
            out["tu"], out["mau"] = "$%s$" % m2.group(1).strip(), "$%s$" % m2.group(2).strip()
    return out


def dien(cap, ts):
    """Điền tham số vào một cấp. None nếu cấp rỗng, còn chỗ điền lạ, hoặc thiếu giá trị."""
    nd = cap.get("noi_dung") if isinstance(cap, dict) else None
    if not nd:
        return None
    khai = set(cap.get("tham_so") or [])
    can = set(_CHO_DIEN.findall(nd))
    if can - khai or can - set(THAM_SO) or any(not ts.get(k) for k in can):
        return None
    return _CHO_DIEN.sub(lambda m: ts[m.group(1)], nd)


def chon_thang(doc, ma_buoc, loai):
    """(khóa thang, thang) theo README §5. loai DAT -> (None, None)."""
    if loai == "DAT":
        return None, None
    buoc = (doc.get("thang") or {}).get(ma_buoc)
    if loai == "DAU_DOI_TRONG_KHOANG":
        meta = ((buoc or {}).get("DAU_DOI_TRONG_KHOANG") or {}).get("_meta") or {}
        mac_dinh = meta.get("dung_thang_mac_dinh") or "B.DH.NGHIEM/DIEM_THIEU"
        mb, lk = mac_dinh.split("/", 1)
        t = ((doc.get("thang") or {}).get(mb) or {}).get(lk)
        if t:
            return "%s/%s" % (mb, lk), t
    if not buoc:
        return None, None
    if loai and loai not in ("chung", "_meta", "KHONG_KIEM_DUOC", "CHUNG") and loai in buoc:
        return "%s/%s" % (ma_buoc, loai), buoc[loai]
    return "%s/chung" % ma_buoc, buoc.get("chung")


def thang_chung(dang, de_bai):
    """Thang `chung` 3 cấp cho 5 bước (dạng khối của generator). Cấp null -> noi_dung None + hanh_dong."""
    doc = nap(dang)
    if not doc:
        return None
    ts = tham_so_tu_de(de_bai)
    out = []
    for mb in BUOC:
        _k, t = chon_thang(doc, mb, "chung")
        cac = []
        for c in (1, 2, 3):
            cap = (t or {}).get(str(c)) or {}
            nd = dien(cap, ts)
            if nd:
                cac.append({"cap": c, "noi_dung": nd, "nguon": "thang-mau/%s/%s/chung/%d" % (dang, mb, c)})
            else:
                o = {"cap": c, "noi_dung": None, "ly_do_trong": "Thang mẫu Sư phạm: cấp để trống." if cap.get("noi_dung") is None
                     else "Không điền được tham số từ đề."}
                if cap.get("hanh_dong"):
                    o["hanh_dong"] = cap["hanh_dong"]
                cac.append(o)
        out.append({"ma_buoc": mb, "cac_cap": cac})
    return out


def goi_y(payload):
    """Một câu gợi ý theo (ma_buoc, loai_ket_qua, cap). Trả:
    {dang, thang, cap_yeu_cau, cap, noi_dung | None, hanh_dong | None, rieng: bool, nhat_ky: [...]}.
    `rieng` = thang riêng theo loại kết quả (không phải `chung`). Không bao giờ trả ly_do_trong."""
    from app.leakfilter import loc_ban_nhap
    ham, de_bai = payload.get("ham"), payload.get("de_bai")
    mb, loai = payload.get("ma_buoc"), payload.get("loai_ket_qua") or "chung"
    try:
        cap = int(payload.get("cap") or 0)
    except (TypeError, ValueError):
        cap = 0
    su_kien = payload.get("su_kien") or []
    dang = payload.get("dang") if payload.get("dang") in FILE else dang_cua(ham)
    kq = {"dang": dang, "thang": None, "cap_yeu_cau": cap, "cap": None, "noi_dung": None, "hanh_dong": None,
          "rieng": False, "nhat_ky": []}
    if not dang or mb not in BUOC or cap not in (1, 2, 3):
        return kq
    doc = nap(dang)
    khoa, t = chon_thang(doc, mb, loai)
    if not t:
        return kq
    kq["thang"], kq["rieng"] = khoa, not khoa.endswith("/chung")
    lv = t.get(str(cap)) or {}
    if lv.get("noi_dung") is None:
        # Cấp rỗng: không hiện dòng nào, không lộ ly_do_trong; làm hanh_dong
        kq["hanh_dong"] = lv.get("hanh_dong") or "BAI_TUONG_TU_DE_HON"
        kq["cap"] = cap
        return kq
    ts = tham_so_tu_de(de_bai)
    _kc, chung = chon_thang(doc, mb, "chung")
    thu = [(khoa, t, c) for c in range(cap, 0, -1)]
    if kq["rieng"] and chung:
        thu.append(("%s/chung" % mb, chung, cap))
    for k, th, c in thu:
        nd = dien(th.get(str(c)) or {}, ts)
        if not nd:
            if (th.get(str(c)) or {}).get("noi_dung"):
                kq["nhat_ky"].append({"thang": k, "cap": c, "ly_do": "KHONG_DIEN_DUOC"})
            continue
        q = loc_ban_nhap(nd, su_kien, "")
        if q.get("cho_phep"):
            kq.update(cap=c, noi_dung=nd, thang=k)
            break
        kq["nhat_ky"].append({"thang": k, "cap": c, "ly_do": q.get("ly_do")})
        log.warning("thang_mau: bộ lọc chặn %s/%s cấp %s (%s) — Sư phạm xem lại câu", dang, k, c, q.get("ly_do"))
    return kq
