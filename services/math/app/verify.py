# -*- coding: utf-8 -*-
"""Kiểm định 3 tầng + cổng phát hành (DAC-TA-3-TANG v1.3).

Tầng 1: SymPy (kiem_5_buoc), không LLM.
Tầng 2: tìm trong tài liệu đã nạp (quyền dùng hợp lệ) một ĐOẠN phát biểu đúng phương pháp mà lời giải dùng:
        "dấu y' => đơn điệu" luôn cần. Đoạn phải được trích nguyên văn (tài liệu, phiên bản, vị trí).
        Không có đoạn khớp -> KHONG_KIEM_DUOC. Tài liệu phát biểu NGƯỢC định lí -> SAI (trích đoạn đó).
        Có đoạn đúng -> áp chính quy tắc đó lên bảng dấu của lời giải; kết luận mâu thuẫn -> SAI.
Tầng 3: đối chiếu với TỪNG dòng bảng công thức giáo viên. Nhận dạng quy tắc: lũy thừa, tổng, thương, đơn điệu,
        cực đại/cực tiểu. Thiếu quy tắc đơn điệu -> KHONG_KIEM_DUOC. Áp quy tắc (đúng như bảng ghi) lên bảng dấu
        và kết luận của lời giải, và kiểm quy tắc đạo hàm bằng SymPy; mâu thuẫn -> SAI kèm công thức.
        Không suy từ kết quả Tầng 1.
Cổng: SAI ở bất kỳ tầng -> BI_CHAN; còn KHONG_KIEM_DUOC -> chờ giáo viên duyệt; mọi tầng DAT/GV_DUYET -> phát hành.
Không có kết quả tầng nào -> không phát hành.
"""
import re
import unicodedata

from app.grader import bai_lam_sang_payload, grade, payload_to_bai_lam

QUYEN_HOP_LE = {"tu_soan", "gv_so_huu", "cong_khai", "duoc_phep", "cc_by", "cc_by_sa", "cc0", "public_domain", "mien_phi_giao_duc"}

_DUONG = r"(>\s*0|dương|không âm|≥\s*0|>=\s*0|\\ge(q)?\s*0|lớn hơn 0|lớn hơn không)"
_AM = r"(<\s*0|(?<!không )âm|không dương|≤\s*0|<=\s*0|\\le(q)?\s*0|nhỏ hơn 0|bé hơn 0)"
_DAOHAM = r"(f'|f′|y'|y′|đạo hàm)"
_DB = r"(đồng biến|tăng)"
_NB = r"(nghịch biến|giảm)"


def _nfc(t):
    return unicodedata.normalize("NFC", t or "")


def _menh_de(text):
    """Tách tài liệu thành các mệnh đề (giữ vị trí ký tự để trích dẫn)."""
    out = []
    for m in re.finditer(r"[^.;\n]+", text):
        s = m.group(0).strip()
        if s:
            out.append((m.start() + (len(m.group(0)) - len(m.group(0).lstrip())), s))
    return out


def _huong_don_dieu(clause):
    """+1 nếu mệnh đề nói (y' dương => đồng biến / y' âm => nghịch biến); -1 nếu nói ngược; None nếu không phải
    phát biểu quy tắc đơn điệu."""
    t = _nfc(clause).lower()
    if not re.search(_DAOHAM, t):
        return None
    # tìm cặp (điều kiện dấu) ... (kết luận) theo thứ tự xuất hiện
    dau = None
    md = re.search(_DUONG, t)
    ma = re.search(_AM, t)
    if md and (not ma or md.start() <= ma.start()):
        dau = 1
    elif ma:
        dau = -1
    if dau is None:
        return None
    ket = None
    mdb = re.search(_DB, t)
    mnb = re.search(_NB, t)
    if mdb and (not mnb or mdb.start() <= mnb.start()):
        ket = 1
    elif mnb:
        ket = -1
    if ket is None:
        return None
    if not re.search(r"hàm|\bf\b|\by\b", t):
        return None
    return 1 if dau == ket else -1


def _huong_cuc_tri(clause):
    t = _nfc(clause).lower()
    m = re.search(r"(dương|\+)\s*(sang|\\to|→|->)\s*(âm|-)", t)
    m2 = re.search(r"(âm|-)\s*(sang|\\to|→|->)\s*(dương|\+)", t)
    cd = re.search(r"cực đại", t)
    ct = re.search(r"cực tiểu", t)
    if m and (cd or ct):
        # "+ -> -" gắn với từ đầu tiên đứng sau
        sau = t[m.end():]
        i_cd, i_ct = sau.find("cực đại"), sau.find("cực tiểu")
        if i_cd >= 0 and (i_ct < 0 or i_cd < i_ct):
            return 1
        if i_ct >= 0:
            return -1
    if m2 and (cd or ct):
        sau = t[m2.end():]
        i_cd, i_ct = sau.find("cực đại"), sau.find("cực tiểu")
        if i_ct >= 0 and (i_cd < 0 or i_ct < i_cd):
            return 1
        if i_cd >= 0:
            return -1
    return None


# ------------------------------------------------------------------ áp quy tắc lên lời giải
def _chuan_khoang(s):
    return str(s).replace(" ", "").replace("+oo", "oo").replace("+∞", "oo").replace("∞", "oo")


def _ap_don_dieu(bl, huong):
    """Áp quy tắc đơn điệu (huong=+1 đúng định lí, -1 ngược) lên bảng dấu của lời giải.
    Trả (True, None) nếu khớp kết luận, (False, lý do) nếu mâu thuẫn, (None, lý do) nếu không áp được."""
    bang = (bl or {}).get("bang") or {}
    kl = (bl or {}).get("ket_luan") or {}
    moc = [str(m) for m in bang.get("moc") or []]
    dau = bang.get("dau") or []
    if len(moc) < 2 or len(dau) != len(moc) - 1:
        return None, "Lời giải không có bảng dấu để áp quy tắc."
    ky = {"+": 1, "-": -1}
    chieu_mong = []
    for s in dau:
        if s not in ky:
            return None, "Ô dấu không phải +/−."
        chieu_mong.append(ky[s] * huong)
    if bang.get("chieu"):
        kc = {"tang": 1, "giam": -1}
        for j, c in enumerate(bang["chieu"]):
            if kc.get(c) is not None and kc[c] != chieu_mong[j]:
                return False, "Ô biến thiên thứ %d trái với quy tắc áp lên ô dấu." % j
    if "dong_bien" not in kl and "nghich_bien" not in kl:
        return (True, None) if bang.get("chieu") else (None, "Lời giải không có kết luận đơn điệu.")
    tai = bang.get("dau_tai_diem") or []

    def khoang(huong_muon, gop):
        out, cur = [], None
        for j, h in enumerate(chieu_mong):
            if h == huong_muon:
                if cur is not None and gop and j - 1 < len(tai) and tai[j - 1] not in ("||", "khong_xd"):
                    cur = (cur[0], moc[j + 1])
                else:
                    if cur is not None:
                        out.append(cur)
                    cur = (moc[j], moc[j + 1])
            else:
                if cur is not None:
                    out.append(cur)
                cur = None
        if cur is not None:
            out.append(cur)
        return sorted(_chuan_khoang("(%s;%s)" % p) for p in out)

    for key, h in (("dong_bien", 1), ("nghich_bien", -1)):
        if key not in kl:
            continue
        hs = sorted(_chuan_khoang(v) for v in kl.get(key) or [])
        if hs != khoang(h, False) and hs != khoang(h, True):
            return False, "Kết luận %s không khớp quy tắc áp lên bảng dấu." % key.replace("_", " ")
    return True, None


def _ap_cuc_tri(bl, huong):
    bang = (bl or {}).get("bang") or {}
    kl = (bl or {}).get("ket_luan") or {}
    if not any(k in kl for k in ("cuc_dai_x", "cuc_tieu_x")):
        return True, None
    moc = [str(m) for m in bang.get("moc") or []]
    dau = bang.get("dau") or []
    tai = bang.get("dau_tai_diem") or []
    cd, ct = [], []
    for j in range(1, len(dau)):
        if j - 1 < len(tai) and tai[j - 1] in ("||", "khong_xd"):
            continue
        if dau[j - 1] == "+" and dau[j] == "-":
            (cd if huong == 1 else ct).append(moc[j])
        elif dau[j - 1] == "-" and dau[j] == "+":
            (ct if huong == 1 else cd).append(moc[j])
    try:
        from app.grader import _cung_ds
        ok = _cung_ds(kl.get("cuc_dai_x") or [], cd) and _cung_ds(kl.get("cuc_tieu_x") or [], ct)
    except Exception:
        return None, "Không so được điểm cực trị."
    return (True, None) if ok else (False, "Kết luận cực trị không khớp quy tắc áp lên bảng dấu.")


# ------------------------------------------------------------------ Tầng 2
def tang_2(tai_lieu, bl=None):
    ung_vien, nguoc = [], []
    bo_qua = []
    for doc in tai_lieu or []:
        quyen = (doc.get("license_status") or doc.get("quyen") or "").strip()
        if quyen not in QUYEN_HOP_LE:
            bo_qua.append({"document_id": doc.get("id"), "ly_do": "quyen_khong_hop_le" if quyen else "khong_khai_quyen"})
            continue
        text = _nfc(doc.get("text") or doc.get("noi_dung") or "")
        for pos, cl in _menh_de(text):
            base = {"document_id": doc.get("id"), "ten": doc.get("title") or doc.get("ten"),
                    "phien_ban": doc.get("phien_ban") or doc.get("version") or 1, "vi_tri": pos, "trich": cl}
            h = _huong_don_dieu(cl)
            if h == 1:
                ung_vien.append(dict(base, phat_bieu="dau_dao_ham_suy_ra_don_dieu"))
            elif h == -1:
                nguoc.append(dict(base, phat_bieu="dau_dao_ham_suy_ra_don_dieu_NGUOC"))
            hc = _huong_cuc_tri(cl)
            if hc == 1:
                ung_vien.append(dict(base, phat_bieu="doi_dau_suy_ra_cuc_tri"))
            elif hc == -1:
                nguoc.append(dict(base, phat_bieu="doi_dau_suy_ra_cuc_tri_NGUOC"))
    if nguoc:
        return {"tang": 2, "trang_thai": "SAI", "ly_do": "Tài liệu phát biểu ngược định lí về dấu đạo hàm.", "trich_dan": nguoc[:3],
                "bo_qua": bo_qua}
    don_dieu = [c for c in ung_vien if c["phat_bieu"] == "dau_dao_ham_suy_ra_don_dieu"]
    if not don_dieu:
        return {"tang": 2, "trang_thai": "KHONG_KIEM_DUOC",
                "ly_do": "Không có đoạn tài liệu (được phép dùng) phát biểu quy tắc dấu y' suy ra đơn điệu.",
                "trich_dan": [], "bo_qua": bo_qua}
    if bl:
        ok, why = _ap_don_dieu(bl, 1)
        if ok is False:
            return {"tang": 2, "trang_thai": "SAI", "ly_do": "Áp đoạn trích lên bảng dấu của lời giải: " + why, "trich_dan": don_dieu[:1],
                    "bo_qua": bo_qua}
        cuc = [c for c in ung_vien if c["phat_bieu"] == "doi_dau_suy_ra_cuc_tri"]
        if cuc:
            ok2, why2 = _ap_cuc_tri(bl, 1)
            if ok2 is False:
                return {"tang": 2, "trang_thai": "SAI", "ly_do": "Áp đoạn trích lên bảng dấu của lời giải: " + why2, "trich_dan": cuc[:1],
                        "bo_qua": bo_qua}
        if ok is None:
            return {"tang": 2, "trang_thai": "KHONG_KIEM_DUOC", "ly_do": why, "trich_dan": don_dieu[:1], "bo_qua": bo_qua}
    return {"tang": 2, "trang_thai": "DAT", "ly_do": "Có đoạn tài liệu phát biểu đúng phương pháp và khớp lời giải.",
            "trich_dan": (don_dieu[:1] + [c for c in ung_vien if c["phat_bieu"] != "dau_dao_ham_suy_ra_don_dieu"][:1]), "bo_qua": bo_qua}


# ------------------------------------------------------------------ Tầng 3
def _loai_cong_thuc(ct):
    latex = _nfc(ct.get("latex") or "")
    blob = _nfc(" ".join([latex, ct.get("noi_dung") or "", ct.get("ten") or ""])).lower()
    ds = []
    if re.search(r"x\^\{?n\}?\)?\s*'|x\^n\)'|lũy thừa|luy thua", blob):
        ds.append("luy_thua")
    if re.search(r"\(u\s*\+\s*v\)\s*'", blob) or "đạo hàm tổng" in blob or "đạo hàm của tổng" in blob:
        ds.append("tong")
    if re.search(r"\(u\s*/\s*v\)\s*'|\\frac\{u\}\{v\}|thương", blob):
        ds.append("thuong")
    h = _huong_don_dieu(blob.replace("\\text", " ").replace("\\rightarrow", " thì ").replace("\\rightarrow", " thì "))
    if h is not None:
        ds.append("don_dieu")
    hc = _huong_cuc_tri(blob.replace("\\text", " "))
    if hc is not None or re.search(r"cực đại|cực tiểu", blob) and re.search(r"đổi dấu|sang|\\to", blob):
        if "cuc_dai" not in ds:
            ds.append("cuc_dai")
        if "cực tiểu" in blob:
            ds.append("cuc_tieu")
    return ds, h, hc


def _kiem_luy_thua(ct):
    """So vế phải của (x^n)' = ... với n x^{n-1} bằng SymPy. True đúng / False sai / None không đọc được."""
    from sympy import symbols, simplify
    from sympy.parsing.sympy_parser import parse_expr, standard_transformations, implicit_multiplication_application
    latex = _nfc(ct.get("latex") or "")
    m = re.search(r"=\s*(.+)$", latex)
    if not m:
        return None
    rhs = m.group(1)
    rhs = rhs.replace("{", "(").replace("}", ")").replace("\\cdot", "*").replace("^", "**").replace(" ", "")
    try:
        n, x = symbols("n x")
        e = parse_expr(rhs, local_dict={"n": n, "x": x}, transformations=standard_transformations + (implicit_multiplication_application,))
        return bool(simplify(e - n * x ** (n - 1)) == 0)
    except Exception:
        return None


def tang_3(cong_thuc, bl=None):
    nhan = []
    for ct in cong_thuc or []:
        ds, h, hc = _loai_cong_thuc(ct)
        if ds:
            nhan.append((ct, ds, h, hc))
    ref = lambda ct: {"formula_id": ct.get("id"), "ten": ct.get("ten"), "noi_dung": ct.get("noi_dung"), "latex": ct.get("latex")}
    # quy tắc đạo hàm: kiểm bằng SymPy
    for ct, ds, h, hc in nhan:
        if "luy_thua" in ds:
            ok = _kiem_luy_thua(ct)
            if ok is False:
                return {"tang": 3, "trang_thai": "SAI", "ly_do": "Công thức đạo hàm lũy thừa trong bảng mâu thuẫn với đạo hàm SymPy của lời giải.",
                        "cong_thuc": [ref(ct)]}
    don_dieu = [(ct, h) for ct, ds, h, hc in nhan if "don_dieu" in ds]
    if not don_dieu:
        return {"tang": 3, "trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Bảng công thức chưa có quy tắc đơn điệu để đối chiếu.",
                "cong_thuc": [ref(ct) for ct, *_ in nhan][:5]}
    if not bl:
        return {"tang": 3, "trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Chưa có lời giải cấu trúc để áp quy tắc.", "cong_thuc": [ref(ct) for ct, _ in don_dieu]}
    for ct, h in don_dieu:
        ok, why = _ap_don_dieu(bl, h)
        if ok is False:
            return {"tang": 3, "trang_thai": "SAI", "ly_do": "Áp «%s» lên bảng dấu: %s" % (ct.get("ten"), why), "cong_thuc": [ref(ct)]}
        if ok is None:
            return {"tang": 3, "trang_thai": "KHONG_KIEM_DUOC", "ly_do": why, "cong_thuc": [ref(ct)]}
    for ct, ds, h, hc in nhan:
        if hc is not None:
            ok, why = _ap_cuc_tri(bl, hc)
            if ok is False:
                return {"tang": 3, "trang_thai": "SAI", "ly_do": "Áp «%s» lên bảng dấu: %s" % (ct.get("ten"), why), "cong_thuc": [ref(ct)]}
    return {"tang": 3, "trang_thai": "DAT", "ly_do": "Áp từng quy tắc trong bảng lên lời giải: khớp.",
            "cong_thuc": [ref(ct) for ct, *_ in nhan][:5]}


def cong_phat_hanh(tang):
    states = [t["trang_thai"] for t in tang]
    if not states:
        return "CHO_GIAO_VIEN_DUYET", "KHONG_KIEM_DUOC"
    if any(s == "SAI" for s in states):
        return "BI_CHAN", "SAI"
    if any(s == "KHONG_KIEM_DUOC" for s in states):
        return "CHO_GIAO_VIEN_DUYET", "KHONG_KIEM_DUOC"
    if all(s in ("DAT", "GV_DUYET") for s in states):
        return "DA_PHAT_HANH", "DAT"
    return "CHO_GIAO_VIEN_DUYET", "KHONG_KIEM_DUOC"


def verify(payload):
    bl = payload.get("bai_lam")
    if payload.get("payload_cham"):
        gp = payload["payload_cham"]
        if not bl:
            try:
                bl, _ = payload_to_bai_lam(gp, "B.DH.KETLUAN")
            except Exception:
                bl = None
    elif bl:
        gp = bai_lam_sang_payload(bl, payload.get("ham"))
    else:
        t1 = {"tang": 1, "trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Chưa có lời giải cấu trúc 5 bước để máy tự kiểm."}
        t2 = tang_2(payload.get("tai_lieu") or [])
        if t2["trang_thai"] == "DAT":
            t2 = dict(t2, trang_thai="KHONG_KIEM_DUOC", ly_do="Có đoạn tài liệu nhưng chưa có lời giải để áp.")
        t3 = tang_3(payload.get("cong_thuc") or [], None)
        tang = [t1, t2, t3]
        phat_hanh, tong = cong_phat_hanh(tang)
        return {"trang_thai_tong": tong, "trang_thai_phat_hanh": phat_hanh, "tang": tang, "phien_ban_chuan_hoa": "norm-0.2"}
    g = grade(gp)
    t1 = {
        "tang": 1,
        "trang_thai": g["ket_qua"],
        "loai_ket_qua": g.get("loai_ket_qua"),
        "buoc_sai": g.get("buoc_sai"),
        "cac_van_de": g.get("cac_van_de"),
        "ma_loi": g.get("ma_loi"),
        "do_tin_cay": g.get("do_tin_cay"),
        "ly_do": g.get("thong_bao"),
    }
    t2 = tang_2(payload.get("tai_lieu") or [], bl)
    t3 = tang_3(payload.get("cong_thuc") or [], bl)
    tang = [t1, t2, t3]
    phat_hanh, tong = cong_phat_hanh(tang)
    return {
        "trang_thai_tong": tong,
        "trang_thai_phat_hanh": phat_hanh,
        "tang": tang,
        "phien_ban_chuan_hoa": g.get("phien_ban_chuan_hoa"),
    }
