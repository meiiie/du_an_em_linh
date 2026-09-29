# -*- coding: utf-8 -*-
"""Chấm bài theo khung 5 bước.

Chỉ số sản phẩm (build §6.4), KHÁC bộ YAML kiểm định (k bắt đầu từ 1):
- dong: chỉ số dòng trong bước, bắt đầu từ 0. Cặp dòng sai thì dong là dòng sau của cặp.
- hang X: k = thứ tự điểm chia theo đúng thứ tự học sinh nhập (app KHÔNG tự sắp), bắt đầu từ 0.
- hang DAU_YPHAY / BIEN_THIEN: k bắt đầu từ 0, xen kẽ khoảng, điểm, khoảng, ...
  k chẵn = ô khoảng, k lẻ = ô điểm. Với 2 điểm chia, dấu trên (p1; +∞) là k = 4.
- Điểm thiếu: buoc_sai.ma_buoc = B.DH.NGHIEM, o = {hang: X, k: null}, loai DIEM_THIEU.
- Đầu ra chỉ dùng tên hàng HOA (X, DAU_YPHAY, BIEN_THIEN); đầu vào nhận thêm tên cũ chữ thường làm bí danh.
- `cac_van_de`: đủ mọi vấn đề, gốc theo thứ tự bước (SAI_TXD, DIEM_THIEU, DIEM_THUA@NGHIEM, SAI_THU_TU_MOC, DIEM_THUA@XETDAU) rồi ô theo k; ô hệ quả có nguyen_nhan = id vấn đề gốc.
"""
import re

from app.dau_vao import DAU_VAO_KHONG_HOP_LE, tu_choi_payload
from app.machine import bai_lam_may
from app.normalizer import NORMALIZER_VERSION, normalize_domain, normalize_expr
from app.paths import load_kiem

K = load_kiem()

ORDER = ["B.DH.TXD", "B.DH.DAOHAM", "B.DH.NGHIEM", "B.DH.XETDAU", "B.DH.KETLUAN"]

# loai_kiem của bộ Kiểm định -> enum sản phẩm. SP-05: dùng đúng ánh xạ của bộ kiểm (SAI_TXD, SAI_DAU, SAI_KET_LUAN,
# SAI_BIEN_DOI) thay vì dồn về SAI_GIA_TRI.
_MAP_LOAI = {
    "dao_ham_sai": "SAI_BIEN_DOI",
    "khong_tuong_duong": "SAI_BIEN_DOI",
    "sai_mien_xac_dinh": "SAI_BIEN_DOI",
    "sai_dau_o_khoang": "SAI_DAU",
    # ma-loi-DH.csv v0.2: ERR.DH.25 (ghi 0 tại điểm y' không xác định, hoặc || tại nghiệm) thuộc loại SAI_DAU
    "sai_o_tai_diem": "SAI_DAU",
    "sai_o_chieu_bien_thien": "SAI_BIEN_DOI",
    "sai_tap_xac_dinh": "SAI_TXD",
    "khac_tap": "SAI_GIA_TRI",
    "cuc_tri_sai": "SAI_KET_LUAN",
    "don_dieu_sai": "SAI_KET_LUAN",
    "ket_luan_sai": "SAI_KET_LUAN",
    "sai_diem_toi_han": "DIEM_THIEU",
    "sai_hang_x_bang": "DIEM_THIEU",
    "dau_doi_trong_khoang": "DAU_DOI_TRONG_KHOANG",
    "sai_thu_tu_moc": "SAI_THU_TU_MOC",
    "thua_nghiem_vi_pham_dkxd": "DIEM_THUA",
    "thua_nghiem_khong_thoa": "DIEM_THUA",
    "mat_nghiem": "DIEM_THIEU",
}

# (mã lỗi, độ tin cậy). SP-07: đoán chỉ theo loại (không có dấu hiệu riêng) để dưới 0,65.
_MA_LOI = {
    "dao_ham_sai": ("ERR.DH.01", 0.8),
    "sai_tap_xac_dinh": ("ERR.DH.02", 0.75),
    "khac_tap": ("ERR.DH.02", 0.6),
    "sai_dau_o_khoang": ("ERR.DH.06", 0.6),
    "sai_o_tai_diem": ("ERR.DH.25", 0.8),
    "sai_o_chieu_bien_thien": ("ERR.DH.26", 0.85),
    "don_dieu_sai": ("ERR.DH.07", 0.6),
    "cuc_tri_sai": ("ERR.DH.12", 0.55),
    "DIEM_THIEU": ("ERR.DH.03", 0.7),
    "DIEM_THUA": ("ERR.DH.21", 0.65),
    "DAU_DOI_TRONG_KHOANG": ("ERR.DH.03", 0.72),
    "SAI_THU_TU_MOC": ("ERR.DH.30", 0.95),
    "SAI_DAU": ("ERR.DH.06", 0.6),
    "SAI_BIEN_DOI": ("ERR.DH.26", 0.85),
    "SAI_TXD": ("ERR.DH.02", 0.75),
    "SAI_GIA_TRI": ("ERR.DH.06", 0.5),
}
_TIN_CAY_LUAT = 0.95  # mã do luật chốt quyết định (ERR.DH.24/30/31)

def _buoc(ma, dong=None, o=None):
    return {"ma_buoc": ma, "dong": dong, "o": o}


def _thong_bao(buoc_sai, loai):
    if not buoc_sai:
        return "Các bước đã nộp hợp lệ."
    ma = buoc_sai["ma_buoc"]
    ten = {
        "B.DH.TXD": "tập xác định",
        "B.DH.DAOHAM": "đạo hàm",
        "B.DH.NGHIEM": "nghiệm và điểm tới hạn",
        "B.DH.XETDAU": "bảng xét dấu",
        "B.DH.KETLUAN": "kết luận",
    }.get(ma, ma)
    if loai == "DAU_DOI_TRONG_KHOANG":
        return "Ở bước nghiệm: trong một khoảng em dựng, y' đổi dấu. Em tìm lại các điểm làm y' bằng 0 hoặc không xác định."
    if loai == "DIEM_THIEU":
        return "Bước nghiệm chưa khớp tập điểm tới hạn. Em kiểm tra lại phương trình y' = 0 và các điểm y' không xác định."
    if loai == "SAI_THU_TU_MOC":
        return "Các mốc trên hàng x chưa theo thứ tự tăng dần. Em sắp lại các mốc từ trái sang phải trước, rồi xét dấu từng khoảng."
    if loai == "DIEM_THUA" and ma == "B.DH.XETDAU":
        # UXT-04-a: lỗi nằm ở bảng (ERR.DH.31), không quy về bước trước.
        return "Hàng x của bảng có một mốc không cần đặt. Em xem lại từng mốc: mốc chỉ đặt tại điểm làm y' bằng 0 hoặc không xác định."
    if loai == "DIEM_THUA":
        return "Bước nghiệm có điểm không phải điểm tới hạn. Em thử thay lại từng điểm vào y'."
    if loai == "KHONG_KIEM_DUOC":
        return "Máy chưa kiểm được bước %s. Thầy cô sẽ xem." % ten
    o = buoc_sai.get("o")
    if o:
        return "Ô ở bước %s cần xem lại. Em kiểm tra lại cả bước rồi nộp." % ten
    dong = buoc_sai.get("dong")
    if dong is None:
        return "Bước %s cần xem lại." % ten
    return "Bước %s, dòng %d cần xem lại." % (ten, dong + 1)


def _pack(trang, loai, buoc_sai, loai_kiem, per, chuan, chua_xong=False, nop_toi=None, cac_van_de=None, ma_loi_tin=None, thong_bao=None):
    ma_loi, tin = None, None
    if trang == "SAI":
        cap = ma_loi_tin or _MA_LOI.get(loai_kiem) or _MA_LOI.get(loai)
        if cap:
            ma_loi, tin = cap
    if trang == "SAI" and not cac_van_de and buoc_sai:
        cac_van_de = [{"id": "VD1", "loai_ket_qua": loai, "buoc_sai": buoc_sai, "ma_loi": ma_loi, "do_tin_cay": tin}]
    return {
        # Danh sách ĐỦ mọi vấn đề (gốc theo thứ tự bước, rồi ô). Màn giáo viên và mô hình thành thạo dùng cả danh sách;
        # màn học sinh chỉ mở vấn đề gốc đầu tiên. Không bao giờ có giá trị điểm (khóa `diem`).
        "cac_van_de": (cac_van_de or []) if trang == "SAI" else [],
        "ket_qua": trang,
        "loai_ket_qua": loai if trang != "DAT" else "DAT",
        "buoc_sai": buoc_sai if trang == "SAI" else None,
        "ma_loi": ma_loi if trang == "SAI" else None,
        "do_tin_cay": tin if trang == "SAI" else None,
        "per_buoc": per,
        "chuan_hoa": chuan,
        "phien_ban_chuan_hoa": NORMALIZER_VERSION,
        "thong_bao": thong_bao or _thong_bao(buoc_sai if trang != "DAT" else None, loai),
        "chua_xong": chua_xong,
        "nop_toi": nop_toi,
    }


def _idx(lines):
    return sorted(lines, key=lambda d: d.get("dong", 0))


def _cells(step):
    bang = step.get("bang") or {}
    return bang.get("cac_o") or []


def _translate_o(o):
    """k kiểm định (bắt đầu 1, xen kẽ) -> k sản phẩm (bắt đầu 0)."""
    if not o:
        return None
    hang = o.get("hang")
    k = o.get("k")
    if hang in ("dau_y'", "DAU_YPHAY"):
        return {"hang": "DAU_YPHAY", "k": None if k is None else int(k) - 1}
    if hang in ("bien_thien", "BIEN_THIEN"):
        return {"hang": "BIEN_THIEN", "k": None if k is None else int(k) - 1}
    if hang in ("x", "X"):
        if k is None:
            return {"hang": "X", "k": None}
        return {"hang": "X", "k": int(k) // 2 - 1}
    return {"hang": hang, "k": k}


def _loai_san_pham(r):
    if r["trang_thai"] == "DAT":
        return "DAT", None
    if r["trang_thai"] == "KHONG_KIEM_DUOC":
        return "KHONG_KIEM_DUOC", r.get("loai_kiem")
    vans = r.get("cac_van_de") or []
    if vans:
        # Ưu tiên điểm thiếu, rồi điểm thừa, rồi đổi dấu trong khoảng.
        order = ["DIEM_THIEU", "DIEM_THUA", "DAU_DOI_TRONG_KHOANG"]
        kinds = [v["loai_ket_qua"] for v in vans]
        for name in order:
            if name in kinds:
                return name, r.get("loai_kiem")
        return kinds[0], r.get("loai_kiem")
    lk = r.get("loai_kiem")
    if lk == "sai_diem_toi_han":
        # phân biệt thiếu/thừa qua phan_chung nếu có
        return "DIEM_THIEU", lk
    return _MAP_LOAI.get(lk, "SAI_GIA_TRI"), lk


def _buoc_tu_ket_qua(r, last_nghiem_dong):
    bs = r.get("buoc_sai") or {}
    vans = r.get("cac_van_de") or []
    loai, lk = _loai_san_pham(r)
    if loai == "DAU_DOI_TRONG_KHOANG":
        return _buoc("B.DH.NGHIEM", last_nghiem_dong, None), loai, lk
    if vans and loai in ("DIEM_THIEU", "DIEM_THUA"):
        chosen = next(v for v in vans if v["loai_ket_qua"] == loai)
        src = chosen["buoc_sai"]
        o = _translate_o(src.get("o"))
        # Không có ô bảng: lỗi nằm ở các dòng nghiệm, gán dòng cuối của bước (0-based).
        dong = None if o else last_nghiem_dong
        return _buoc(src["ma_buoc"], dong, o), loai, lk
    if not bs:
        return None, loai, lk
    dong = bs.get("dong")
    # Bộ kiểm ghi dong = 1,2,3,5 theo thứ tự bước khi bước chỉ có một dòng logic.
    if bs.get("o"):
        dong_sp = None
    elif dong in (1, 2, 3, 5):
        dong_sp = 0
    else:
        dong_sp = dong
    if loai == "DAU_DOI_TRONG_KHOANG" or (r.get("loai_kiem") == "dau_doi_trong_khoang"):
        return _buoc("B.DH.NGHIEM", last_nghiem_dong, None), "DAU_DOI_TRONG_KHOANG", lk
    return _buoc(bs.get("ma_buoc"), dong_sp, _translate_o(bs.get("o"))), loai, lk


def _per(stop, err_ma=None, err_trang="SAI"):
    per = {}
    for ma in ORDER:
        if err_ma and ma == err_ma:
            per[ma] = err_trang
            break
        per[ma] = "DAT"
        if ma == stop:
            break
    return per


def _parse_nghiem_lines(lines):
    import re
    roots, kxd = [], []
    chuan = []
    for line in _idx(lines):
        raw = line.get("latex") or ""
        loai_dong = line.get("loai")
        text = raw.lower()
        is_kxd = loai_dong == "KHONG_XD" or ("không xác định" in text) or ("khong xac dinh" in text)
        if re.search(r"không có|khong co|không nghiệm|khong nghiem|∅|emptyset", text) and not re.search(r"\d", text):
            chuan.append({"dong": line.get("dong", 0), "trang_thai_chuan_hoa": "OK", "chuoi_chuan_hoa": "rong"})
            continue
        # SP-04: x \in \{1;3\}, S = \{1; 3\}; bỏ tiền tố "y' không xác định tại"
        raw = re.sub(r"^.*?(không xác định|khong xac dinh)\s*(tại|tai)?\s*", "", raw, flags=re.I) if is_kxd else raw
        raw = raw.replace("\\left", "").replace("\\right", "")
        raw = re.sub(r"(x|S)\s*(\\in|∈|=)\s*\\?\{", "", raw)
        raw = raw.replace("\\{", "").replace("\\}", "").replace("{", "").replace("}", "") if re.search(r"\\\{|\\\}", line.get("latex") or "") else raw
        parts = re.split(r"\\lor|\\vee|\\quad|;| hoặc | hoac |,| và | va ", raw)
        found = []
        for part in parts:
            m = re.search(r"x\s*=\s*(.+)", part.replace("$", ""))
            chunk = m.group(1) if m else part
            token = normalize_expr(chunk.strip(" ."))
            if token is None:
                continue
            # bỏ phần thừa sau số
            if re.fullmatch(r"[+\-]?\d+(?:\.\d+)?(?:/\d+)?", token) or re.fullmatch(r"[+\-]?\(\d+\)/\(\d+\)", token):
                found.append(token)
        if not found and raw.strip():
            return None, None, chuan, line.get("dong", 0)
        chuan.append({"dong": line.get("dong", 0), "trang_thai_chuan_hoa": "OK", "chuoi_chuan_hoa": ",".join(found)})
        if is_kxd:
            kxd.extend(found)
        else:
            roots.extend(found)
    return roots, kxd, chuan, None


def _interval_strings(text):
    import re
    t = text.replace("∞", "oo").replace("\\infty", "oo").replace("−", "-").replace("–", "-")
    t = t.replace("\\left", "").replace("\\right", "")
    out = []
    for m in re.finditer(r"([\(\[])\s*([^;]+?)\s*;\s*([^\)\]]+?)\s*([\)\]])", t):
        a = m.group(2).strip().replace(" ", "")
        b = m.group(3).strip().replace(" ", "")
        a = a.replace("+oo", "+oo")
        if a in ("oo",):
            a = "+oo"
        if b in ("oo",):
            b = "+oo"
        if a.startswith("oo"):
            a = "+" + a
        out.append("%s%s; %s%s" % (m.group(1), a, b, m.group(4)))
    return out


_NHAN_KL = {"DONG_BIEN": "dong_bien", "NGHICH_BIEN": "nghich_bien", "CUC_DAI": "cuc_dai", "CUC_TIEU": "cuc_tieu"}
_SO = r"[+\-−]?\d+(?:[.,]\d+)?(?:/\d+)?"


def _tung_do_chu(t):
    """0002e: tung độ viết bằng lời trong ô / dòng cực trị: "giá trị cực đại bằng 6", "giá trị cực tiểu là −26"."""
    import re
    t = t.replace("−", "-")
    return re.findall(r"giá trị cực (?:đại|tiểu)(?: của hàm số)?\s*(?:là|bằng|=)\s*(%s)" % _SO, t)


def _doc_cuc_tri(raw):
    """Đọc ô cực đại/cực tiểu: "x = 1, y = 6", "x=1; y_{CĐ}=6", "(1; 6)", "1". Trả (các x, các y)."""
    import re
    t = raw.replace("$", "").replace("\\", "").replace("−", "-").lower()
    # 0002d: x_{CT}, x_CT, x_1, x_{2}, và dạng MathLive x_{\text{CT}}, x_{\mathrm{CD}} (dấu \ đã bỏ ở trên)
    t = re.sub(r"_\{?\s*(?:(?:text|mathrm|rm)\s*\{\s*)?c[dđt]\s*\}?\s*\}?", "", t)
    t = re.sub(r"_\{?\s*\d{1,2}\s*\}?(?=\s*=)", "", t)   # chỉ số số chỉ bỏ khi là nhãn: x_1 = 1
    t = re.sub(r"(?<![a-z])f(?=\s*=)", "y", t)   # 0002d: f_{CT} = −26 (sau khi bỏ chỉ số) đọc như y_{CT}
    xs = re.findall(r"x\s*=\s*(%s)" % _SO, t)
    ys = re.findall(r"y\s*=\s*(%s)" % _SO, t)
    # 0002e: "f(-1) = 6", "y_{CĐ} = y(-1) = 6" -> điểm -1, tung độ 6; "giá trị cực đại bằng 6" -> chỉ tung độ
    for a, b in re.findall(r"(?<![a-z])[yf]\s*\(\s*(%s)\s*\)\s*=\s*(%s)" % (_SO, _SO), t):
        xs.append(a)
        ys.append(b)
    ys += _tung_do_chu(t)
    if not xs:
        m = re.fullmatch(r"\s*\(\s*(%s)\s*[;,]\s*(%s)\s*\)\s*" % (_SO, _SO), t)
        if m:
            xs, ys = [m.group(1)], [m.group(2)]
        else:
            m = re.fullmatch(r"\s*(%s)\s*" % _SO, t)
            if m:
                xs = [m.group(1)]
    return [v.replace(",", ".") for v in xs], [v.replace(",", ".") for v in ys]


def _parse_ket_luan(step):
    import re
    lines = _idx(step.get("cac_dong") or [])
    claims = set(step.get("khai_bao") or [])
    kl = {}
    db, nb = [], []
    db_tap = nb_tap = None
    cd, ct, gcd, gct = [], [], [], []
    saw = set()
    chuan = []
    chi_y = {}   # 0002e: ô cực trị chỉ có tung độ {key: {"ys": [...], "dong": i}}
    khai_ds = list(step.get("khai_bao") or [])
    _NHAN_TU_KHAI = {"dong_bien": "DONG_BIEN", "nghich_bien": "NGHICH_BIEN", "cuc_dai": "CUC_DAI", "cuc_tieu": "CUC_TIEU"}
    for vi_tri, line in enumerate(lines):
        raw = line.get("latex") or ""
        t = raw.lower()
        intervals = _interval_strings(raw)
        has_union = ("\\cup" in raw) or ("∪" in raw) or (" U " in raw) or bool(re.search(r"[\)\]]\s*[uU]\s*[\(\[]", raw))
        nhan = (line.get("loai") or "").upper()
        # Client cũ (6eb8b06) không gửi nhãn ô: dòng i ứng khai_bao[i] (4 ô theo thứ tự). Chỉ dùng khi số dòng khớp
        # và dòng không tự nói loại bằng từ khoá.
        m_trong = re.match(r"^\s*(?:không|khong)\s+(dong_bien|nghich_bien|cuc_dai|cuc_tieu)\s*$", t)
        if not nhan and m_trong:
            # ô trống UI cũ gửi "không <khoá>"
            nhan = _NHAN_TU_KHAI[m_trong.group(1)]
        khong_tu_khoa = not re.search(r"đồng biến|dong bien|nghịch biến|nghich bien|cực|cuc (dai|tieu)", t)
        if not nhan and khong_tu_khoa and len(khai_ds) == len(lines):
            nhan = _NHAN_TU_KHAI.get(khai_ds[vi_tri], "")
        elif not nhan and khong_tu_khoa and len(lines) == 4:
            # UI luôn gửi đủ 4 ô theo thứ tự Đồng biến / Nghịch biến / Cực đại / Cực tiểu
            nhan = ("DONG_BIEN", "NGHICH_BIEN", "CUC_DAI", "CUC_TIEU")[vi_tri]
        if nhan in _NHAN_KL:
            # SP-03: ô có nhãn -> hiểu nội dung theo nhãn. Ô trống / "không có" / "Hàm số không có cực trị" = danh sách rỗng.
            key = _NHAN_KL[nhan]
            rong = (not raw.strip()) or (not re.search(r"\d|oo|infty|∞", t) and bool(re.search(r"không|khong|∅|emptyset|varnothing|rỗng|rong|trống|none|^\s*-+\s*$", t)))
            if rong and claims and key not in claims:
                # Ô đề không hỏi, để trống: bỏ qua, không làm hỏng bước
                chuan.append({"dong": line.get("dong", 0), "trang_thai_chuan_hoa": "OK"})
                continue
            saw.add(key)
            if key in ("dong_bien", "nghich_bien"):
                if not rong and not intervals:
                    return None, [{"dong": line.get("dong", 0), "trang_thai_chuan_hoa": "THAT_BAI"}]
                if has_union and intervals:
                    if key == "dong_bien":
                        db_tap = " U ".join(intervals)
                    else:
                        nb_tap = " U ".join(intervals)
                elif not rong:
                    (db if key == "dong_bien" else nb).extend(intervals)
            else:
                if not rong:
                    xs, ys = _doc_cuc_tri(raw)
                    if not xs and ys:
                        # 0002e (Sư phạm 29/09 13:10): ô chỉ ghi tung độ (y_{CĐ} = 6, f_{CT} = −26) vẫn đọc được -> chấm ở
                        # _cham_chi_tung_do (ERR.DH.11 nếu tung độ đúng, ERR.DH.22 nếu sai), không KHONG_KIEM_DUOC.
                        chi_y.setdefault(key, {"ys": [v.replace(",", ".") for v in ys], "dong": line.get("dong", 0)})
                        chuan.append({"dong": line.get("dong", 0), "trang_thai_chuan_hoa": "OK"})
                        continue
                    if not xs:
                        return None, [{"dong": line.get("dong", 0), "trang_thai_chuan_hoa": "THAT_BAI"}]
                    (cd if key == "cuc_dai" else ct).extend(xs)
                    (gcd if key == "cuc_dai" else gct).extend(ys)
            chuan.append({"dong": line.get("dong", 0), "trang_thai_chuan_hoa": "OK"})
            continue
        if re.search(r"không có cực trị|khong co cuc tri|không có điểm cực trị", t):
            saw.update({"cuc_dai", "cuc_tieu"})
            chuan.append({"dong": line.get("dong", 0), "trang_thai_chuan_hoa": "OK"})
            continue
        if re.search(r"đồng biến|dong bien", t):
            saw.add("dong_bien")
            if has_union:
                db_tap = " U ".join(intervals) if intervals else None
            else:
                db.extend(intervals)
        elif re.search(r"nghịch biến|nghich bien", t):
            saw.add("nghich_bien")
            if has_union:
                nb_tap = " U ".join(intervals) if intervals else None
            else:
                nb.extend(intervals)
        elif re.search(r"cực đại|cuc dai", t):
            saw.add("cuc_dai")
            cd.extend(re.findall(r"x\s*=\s*([+\-]?\d+(?:[.,]\d+)?(?:/\d+)?)", t.replace("$", "")))
            ys = re.findall(r"y\s*(?:=|cd|cđ|_{cd}|_{cđ})?\s*=?\s*([+\-]?\d+(?:[.,]\d+)?)", t.replace("_{", "").replace("}", ""))
            # lấy số sau 'y'
            ys = re.findall(r"y\s*(?:_\s*\{?\s*c[dđ]\s*\}?)?\s*=\s*([+\-]?\d+(?:[.,]\d+)?)", raw.lower().replace("\\", ""))
            if not re.search(r"x\s*=", t):
                xs2, ys2 = _doc_cuc_tri(raw)
                if not xs2 and ys2:   # 0002e: "cực đại y_{CĐ} = 6", "giá trị cực đại bằng 6"
                    chi_y.setdefault("cuc_dai", {"ys": ys2, "dong": line.get("dong", 0)})
                    chuan.append({"dong": line.get("dong", 0), "trang_thai_chuan_hoa": "OK"})
                    continue
            gcd.extend(ys)
        elif re.search(r"cực tiểu|cuc tieu", t):
            saw.add("cuc_tieu")
            ct.extend(re.findall(r"x\s*=\s*([+\-]?\d+(?:[.,]\d+)?(?:/\d+)?)", t.replace("$", "")))
            ys = re.findall(r"y\s*(?:_\s*\{?\s*ct\s*\}?)?\s*=\s*([+\-]?\d+(?:[.,]\d+)?)", raw.lower().replace("\\", ""))
            if not re.search(r"x\s*=", t):
                xs2, ys2 = _doc_cuc_tri(raw)
                if not xs2 and ys2:   # 0002e
                    chi_y.setdefault("cuc_tieu", {"ys": ys2, "dong": line.get("dong", 0)})
                    chuan.append({"dong": line.get("dong", 0), "trang_thai_chuan_hoa": "OK"})
                    continue
            gct.extend(ys)
        elif raw.strip():
            return None, [{"dong": line.get("dong", 0), "trang_thai_chuan_hoa": "THAT_BAI"}]
        chuan.append({"dong": line.get("dong", 0), "trang_thai_chuan_hoa": "OK"})
    if "dong_bien" in claims or "dong_bien" in saw:
        if db_tap:
            kl["dong_bien_tren_tap"] = db_tap
        else:
            kl["dong_bien"] = [s.replace(",", ".") for s in db]
    if "nghich_bien" in claims or "nghich_bien" in saw:
        if nb_tap:
            kl["nghich_bien_tren_tap"] = nb_tap
        else:
            kl["nghich_bien"] = [s.replace(",", ".") for s in nb]
    for key in chi_y:
        # ô chỉ tung độ: vị trí/giá trị để _grade_core điền trung tính từ lời giải máy rồi chấm riêng
        if not (cd if key == "cuc_dai" else ct):
            saw.discard(key)
        kl["_chi_tung_do"] = chi_y
    if "cuc_dai" in claims or "cuc_dai" in saw:
        kl["cuc_dai_x"] = [s.replace(",", ".") for s in cd]
        if gcd:
            kl["gia_tri_cuc_dai"] = [s.replace(",", ".") for s in gcd]
    if "cuc_tieu" in claims or "cuc_tieu" in saw:
        kl["cuc_tieu_x"] = [s.replace(",", ".") for s in ct]
        if gct:
            kl["gia_tri_cuc_tieu"] = [s.replace(",", ".") for s in gct]
    if not kl:
        return None, chuan
    return kl, chuan


def _bang_tu_o(cells):
    xs = [c for c in cells if c.get("hang") in ("X", "x")]
    xs = sorted(xs, key=lambda c: c.get("k", 0))
    diem = []
    for c in xs:
        raw = c.get("gia_tri")
        token = normalize_expr(str(raw))
        if token is None:
            if raw not in (None, "") and str(raw).strip():
                # F-01: ô hàng X có nội dung nhưng không đọc được -> KHONG_KIEM_DUOC (không phải SAI "ô trống")
                return None, None
            return None, c.get("k", 0)
        diem.append(token)
    m = len(diem)
    dau_cells = {(c["k"]): c.get("gia_tri") for c in cells if c.get("hang") in ("DAU_YPHAY", "dau_y'")}
    chieu_cells = {(c["k"]): c.get("gia_tri") for c in cells if c.get("hang") in ("BIEN_THIEN", "bien_thien")}
    # k sản phẩm: chẵn = khoảng
    dau = []
    for j in range(m + 1):
        k = 2 * j
        if k not in dau_cells or dau_cells[k] in (None, ""):
            return None, k
        val = str(dau_cells[k]).replace("−", "-").replace("–", "-")
        if val not in ("+", "-", "0", "||"):
            return None, k
        dau.append(val)
    dau_tai = []
    has_point = any((2 * j + 1) in dau_cells for j in range(m))
    if has_point:
        for j in range(m):
            k = 2 * j + 1
            val = dau_cells.get(k)
            if val in (None, ""):
                return None, k
            val = str(val).replace("−", "-")
            if val not in ("+", "-", "0", "||"):
                return None, k
            dau_tai.append(val)
    chieu = []
    has_chieu = any((2 * j) in chieu_cells for j in range(m + 1))
    if has_chieu:
        for j in range(m + 1):
            k = 2 * j
            val = chieu_cells.get(k)
            if val in (None, ""):
                return None, k
            val = str(val).upper()
            chieu.append({"TANG": "tang", "GIAM": "giam", "TANG_LEN": "tang", "GIAM_XUONG": "giam", "||": "khong_xd"}.get(val, val.lower()))
    moc = ["-oo"] + diem + ["+oo"]
    bang = {"moc": moc, "dau": dau}
    if dau_tai:
        bang["dau_tai_diem"] = dau_tai
    if chieu:
        bang["chieu"] = chieu
    return bang, None


def _bu_buoc_truoc(payload):
    """Bài khung ngắn (DAC-TA §3.4(a), 29/09): `buoc_bat_dau` = bước đầu tiên học sinh làm. Các bước TRƯỚC nó do đề cho sẵn,
    không chấm: nếu payload không có thì lấy từ lời giải máy (TXĐ, y', nghiệm) để chấm các bước sau."""
    bd = payload.get("buoc_bat_dau")
    if not bd or bd == ORDER[0]:
        return {}
    may = bai_lam_may(payload.get("ham"))
    if not may:
        return {}
    bd_i = ORDER.index(bd)
    steps = {s.get("ma_buoc") for s in payload.get("cac_buoc") or []}
    bu = {}
    if bd_i >= 1 and "B.DH.TXD" not in steps:
        bu["TXD"] = may["TXD"]
    if bd_i >= 2 and "B.DH.DAOHAM" not in steps:
        bu["dao_ham"] = may["dao_ham"]
    if bd_i >= 3 and "B.DH.NGHIEM" not in steps:
        bu["y_phay_bang_0"] = list(may.get("y_phay_bang_0") or [])
        bu["y_phay_khong_xd"] = list(may.get("y_phay_khong_xd") or [])
    return bu


def payload_to_bai_lam(payload, den):
    """Trả (bai_lam | None, loi_som | None). loi_som là kết quả chấm nếu hỏng trước SymPy."""
    steps = {s["ma_buoc"]: s for s in payload["cac_buoc"]}
    ham = payload["ham"]
    chuan = []
    den_i = ORDER.index(den)
    bu = _bu_buoc_truoc(payload)
    if bu:
        return _payload_to_bai_lam_bu(payload, den, steps, bu)

    txd_lines = _idx((steps.get("B.DH.TXD") or {}).get("cac_dong") or [])
    if not txd_lines:
        return None, _pack("KHONG_KIEM_DUOC", "KHONG_KIEM_DUOC", _buoc("B.DH.TXD", 0, None), "thieu_dong", _per("B.DH.TXD", "B.DH.TXD", "KHONG_KIEM_DUOC"), chuan, nop_toi=den)
    domains = []
    for line in txd_lines:
        dom = normalize_domain(line.get("latex"))
        if not dom:
            return None, _pack("KHONG_KIEM_DUOC", "KHONG_KIEM_DUOC", _buoc("B.DH.TXD", line.get("dong", 0), None), "that_bai_chuan_hoa", _per("B.DH.TXD", "B.DH.TXD", "KHONG_KIEM_DUOC"), chuan, nop_toi=den)
        try:
            K.parse_tap(dom)
        except Exception:
            return None, _pack("KHONG_KIEM_DUOC", "KHONG_KIEM_DUOC", _buoc("B.DH.TXD", line.get("dong", 0), None), "that_bai_chuan_hoa", _per("B.DH.TXD", "B.DH.TXD", "KHONG_KIEM_DUOC"), chuan, nop_toi=den)
        domains.append(dom)
        chuan.append({"ma_buoc": "B.DH.TXD", "dong": line.get("dong", 0), "trang_thai_chuan_hoa": "OK", "chuoi_chuan_hoa": dom})
    for i in range(1, len(domains)):
        if str(K.parse_tap(domains[i])) != str(K.parse_tap(domains[i - 1])):
            return None, _pack("SAI", "SAI_BIEN_DOI", _buoc("B.DH.TXD", txd_lines[i].get("dong", i), None), "khong_tuong_duong", _per("B.DH.TXD", "B.DH.TXD"), chuan, nop_toi=den)

    if den_i < 1:
        bl = bai_lam_may(ham)
        if not bl:
            return None, _pack("KHONG_KIEM_DUOC", "KHONG_KIEM_DUOC", _buoc("B.DH.TXD", 0, None), "khong_giai_duoc", {}, chuan, nop_toi=den)
        bl["TXD"] = domains[-1]
        return bl, None

    dh_lines = _idx((steps.get("B.DH.DAOHAM") or {}).get("cac_dong") or [])
    exprs = []
    for line in dh_lines:
        ex = normalize_expr(line.get("latex"))
        if not ex:
            return None, _pack("KHONG_KIEM_DUOC", "KHONG_KIEM_DUOC", _buoc("B.DH.DAOHAM", line.get("dong", 0), None), "that_bai_chuan_hoa", _per("B.DH.DAOHAM", "B.DH.DAOHAM", "KHONG_KIEM_DUOC"), chuan, nop_toi=den)
        try:
            K.P(ex)
        except Exception:
            return None, _pack("KHONG_KIEM_DUOC", "KHONG_KIEM_DUOC", _buoc("B.DH.DAOHAM", line.get("dong", 0), None), "that_bai_chuan_hoa", _per("B.DH.DAOHAM", "B.DH.DAOHAM", "KHONG_KIEM_DUOC"), chuan, nop_toi=den)
        exprs.append(ex)
        chuan.append({"ma_buoc": "B.DH.DAOHAM", "dong": line.get("dong", 0), "trang_thai_chuan_hoa": "OK", "chuoi_chuan_hoa": ex})
    if not exprs:
        return None, _pack("KHONG_KIEM_DUOC", "KHONG_KIEM_DUOC", _buoc("B.DH.DAOHAM", 0, None), "thieu_dong", _per("B.DH.DAOHAM", "B.DH.DAOHAM", "KHONG_KIEM_DUOC"), chuan, nop_toi=den)
    if len(exprs) > 1:
        pair = K.kiem_bien_doi_bieu_thuc(exprs)
        if pair["trang_thai"] != "DAT":
            # buoc_sai của hàm này là số dòng 1-based của dòng sau
            dong = (pair.get("buoc_sai") or 2) - 1
            trang = "KHONG_KIEM_DUOC" if pair["trang_thai"] != "SAI" else "SAI"
            loai = "KHONG_KIEM_DUOC" if trang != "SAI" else "SAI_BIEN_DOI"
            return None, _pack(trang, loai, _buoc("B.DH.DAOHAM", dong, None), pair.get("loai_kiem"), _per("B.DH.DAOHAM", "B.DH.DAOHAM", trang), chuan, nop_toi=den)

    bl = bai_lam_may(ham)
    if not bl:
        return None, _pack("KHONG_KIEM_DUOC", "KHONG_KIEM_DUOC", None, "khong_giai_duoc", {}, chuan, nop_toi=den)
    bl["TXD"] = domains[-1]
    bl["dao_ham"] = exprs[0]
    bl["_exprs"] = exprs
    bl["_chuan"] = chuan

    if den_i < 2:
        return bl, None

    roots, kxd, ch_n, bad = _parse_nghiem_lines((steps.get("B.DH.NGHIEM") or {}).get("cac_dong") or [])
    if bad is not None or roots is None:
        return None, _pack("KHONG_KIEM_DUOC", "KHONG_KIEM_DUOC", _buoc("B.DH.NGHIEM", bad or 0, None), "that_bai_chuan_hoa", _per("B.DH.NGHIEM", "B.DH.NGHIEM", "KHONG_KIEM_DUOC"), chuan, nop_toi=den)
    for c in ch_n:
        c["ma_buoc"] = "B.DH.NGHIEM"
        chuan.append(c)
    bl["y_phay_bang_0"] = roots
    bl["y_phay_khong_xd"] = kxd
    bl["_last_nghiem"] = (ch_n[-1]["dong"] if ch_n else 0)
    if den_i < 3:
        # giữ bảng máy
        return bl, None

    bang, bad_k = _bang_tu_o(_cells(steps.get("B.DH.XETDAU") or {}))
    if bang is None:
        return None, _pack(
            "KHONG_KIEM_DUOC" if bad_k is None else "SAI",
            "KHONG_KIEM_DUOC" if bad_k is None else "SAI_GIA_TRI",
            _buoc("B.DH.XETDAU", None, {"hang": "DAU_YPHAY", "k": bad_k}),
            "o_trong" if bad_k is not None else "that_bai_chuan_hoa",
            _per("B.DH.XETDAU", "B.DH.XETDAU", "SAI" if bad_k is not None else "KHONG_KIEM_DUOC"),
            chuan,
            nop_toi=den,
        )
    bl["bang"] = bang
    if den_i < 4:
        return bl, None

    kl, ch_k = _parse_ket_luan(steps.get("B.DH.KETLUAN") or {})
    if kl is None:
        dong = 0
        if ch_k and ch_k[0].get("trang_thai_chuan_hoa") == "THAT_BAI":
            dong = ch_k[0].get("dong", 0)
        return None, _pack("KHONG_KIEM_DUOC", "KHONG_KIEM_DUOC", _buoc("B.DH.KETLUAN", dong, None), "that_bai_chuan_hoa", _per("B.DH.KETLUAN", "B.DH.KETLUAN", "KHONG_KIEM_DUOC"), chuan, nop_toi=den)
    bl["ket_luan"] = kl
    return bl, None


def _overlay_may(student_bl, den):
    """Phần sau `den` lấy lời giải máy để chấm đúng phần học sinh đã nộp."""
    ham = student_bl["ham"]
    may = bai_lam_may(ham)
    if may is None:
        return None
    den_i = ORDER.index(den)
    out = dict(may)
    out["ham"] = ham
    if den_i >= 0:
        out["TXD"] = student_bl["TXD"]
    if den_i >= 1:
        out["dao_ham"] = student_bl["dao_ham"]
    if den_i >= 2:
        out["y_phay_bang_0"] = student_bl["y_phay_bang_0"]
        out["y_phay_khong_xd"] = student_bl["y_phay_khong_xd"]
    if den_i >= 3:
        out["bang"] = student_bl["bang"]
    if den_i >= 4:
        out["ket_luan"] = student_bl["ket_luan"]
    return out


_CHUOI_TOI_DA = 2000
# F-01 (bản vá Kiểm định): dấu hiệu code trong chuỗi học sinh nhập, quét TRƯỚC khi chuẩn hóa. Chuỗi không bao giờ được thực thi;
# đây là lớp fail-closed để ô có nội dung lạ không bị bộ trích số bỏ qua rồi chấm như ô trống / ô đúng
# (đo trên 4e4af19: chuỗi code ở ô gia_tri_cuc_dai được chấm DAT 20/22 ca).
_DAU_HIEU_CODE_CHUNG = (r"__|\blambda\b|\bimport\b|\b(?:eval|exec|open|compile|getattr|setattr|globals|locals|vars|input|"
                        r"Symbol|Integer|Float|Rational|Function|Lambda|sympify|parse_expr|lambdify|factorial)\s*\(|"
                        r"\b[A-Za-z_]\w*\s*\(\s*[\'\"]|\(\s*\)\s*\.|\.\s*__|[\"`]|\'\s*\w+\s*\'|"
                        r"(?:\*\*|\^)\s*\{?\s*\d+\s*\}?\s*(?:\*\*|\^)|(?:\*\*|\^)\s*\(?\s*\d{3,}|\d\s*\*\*\s*\d{2,}")
# 0002d (Sư phạm 29/09 12:47): ký hiệu SGK và nhãn ô của hệ KHÔNG phải dấu hiệu code. Trước khi quét, thay:
#  - chỉ số của x / y / f: chữ CĐ, CD, CT (hoa/thường, có/không dấu), có hoặc không có {} (cho phép \text{…} /
#    \mathrm{…} trong {}): x_{CT}, x_CT, y_{CĐ}, f_{ct}; hoặc số 1–2 chữ số ĐỨNG Ở VỊ TRÍ NHÃN (trước = , ; ) và là):
#    x_1 = 1, x_{2} = 3;
#  - nhãn ô danh sách trắng của hệ (UI cũ gửi "không cuc_dai"): dong_bien, nghich_bien, cuc_dai, cuc_tieu, gia_tri_cuc_dai,
#    gia_tri_cuc_tieu, cuc_dai_x, cuc_tieu_x, dong_bien_tren_tap, nghich_bien_tren_tap.
# Phần còn lại: MỌI dấu '_' khác vẫn là dấu hiệu code (tên kiểu __class__, x_y, os_system, g_{1}, x_{CT}_{1} ...).
_CHI_SO_SGK = re.compile(r"(?<![A-Za-z0-9_\\])([xXyYfF])_(?:\{\s*(?:\\(?:text|mathrm|rm)\s*\{\s*)?[cC]\s*[dDđĐtT]\s*\}?\s*\}"
                         r"|[cC][dDđĐtT](?![A-Za-z0-9_{])"
                         # chỉ số số chỉ ở vị trí NHÃN (x_1 = 1, x_{2}; …): "x_1 + 1" (ca M20 bộ độc hại) vẫn bị chặn
                         r"|(?:\{\s*\d{1,2}\s*\}|\d{1,2})(?=\s*(?:=|,|;|\)|và\b|là\b|$)))")
_NHAN_O_HE = re.compile(r"(?<![A-Za-z0-9_])(?:gia_tri_cuc_dai|gia_tri_cuc_tieu|cuc_dai_x|cuc_tieu_x|cuc_dai|cuc_tieu|"
                        r"dong_bien_tren_tap|nghich_bien_tren_tap|dong_bien|nghich_bien)(?![A-Za-z0-9_])")
_DAU_HIEU_CODE_O = re.compile(_DAU_HIEU_CODE_CHUNG + r"|_")
_DAU_HIEU_CODE_DONG = _DAU_HIEU_CODE_O


def _bo_ky_hieu_hop_le(t):
    """Thay chỉ số SGK (giữ chữ cái gốc) và nhãn ô của hệ bằng chữ thường, để phần quét '_' chỉ còn thấy tên lạ."""
    return _NHAN_O_HE.sub("nhan", _CHI_SO_SGK.sub(lambda m: m.group(1), t))


def _chuoi_nguoi_nhap(payload):
    """(chuỗi, là dòng LaTeX?) do học sinh/AI nhập: ham, cac_dong[].latex, bang.cac_o[].gia_tri."""
    yield payload.get("ham"), False
    for b in payload.get("cac_buoc") or []:
        if not isinstance(b, dict):
            continue
        for d in b.get("cac_dong") or []:
            if isinstance(d, dict):
                yield d.get("latex"), True
        for o in ((b.get("bang") or {}).get("cac_o") or []):
            if isinstance(o, dict):
                yield o.get("gia_tri"), False


def _chuoi_doc_hai(payload):
    for t, la_dong in _chuoi_nguoi_nhap(payload):
        if t is None:
            continue
        t = str(t)
        if len(t) > _CHUOI_TOI_DA or (_DAU_HIEU_CODE_DONG if la_dong else _DAU_HIEU_CODE_O).search(_bo_ky_hieu_hop_le(t)):
            return True
    return False


def _kkd_dau_vao(payload, ma, dong, o, chi_tiet):
    """F-01: chuỗi học sinh bị cổng danh sách trắng / bộ phân tích an toàn từ chối -> KHONG_KIEM_DUOC, không bao giờ chấm."""
    den = payload.get("nop_toi") if payload.get("nop_toi") in ORDER else "B.DH.KETLUAN"
    ma = ma if ma in ORDER else None
    r = _pack("KHONG_KIEM_DUOC", "KHONG_KIEM_DUOC", _buoc(ma, dong, o) if ma else None, DAU_VAO_KHONG_HOP_LE,
              _per(den, ma, "KHONG_KIEM_DUOC") if ma else {}, [], nop_toi=den,
              thong_bao=None if ma else "Máy chưa đọc được bài làm này. Thầy cô sẽ xem.")
    r["ly_do"] = DAU_VAO_KHONG_HOP_LE
    r["chi_tiet_tu_choi"] = str(chi_tiet)[:120]
    return r


def _payload_to_bai_lam_bu(payload, den, steps, bu):
    """payload_to_bai_lam cho bài bắt đầu giữa chừng: bước trước buoc_bat_dau lấy từ `bu` (lời giải máy), bước học sinh làm
    đọc như thường bằng chính payload_to_bai_lam (các bước máy được chèn dưới dạng dòng đã chuẩn hóa sẵn)."""
    import copy
    p2 = copy.deepcopy(payload)
    p2.pop("buoc_bat_dau", None)
    them = []
    if "TXD" in bu:
        them.append({"ma_buoc": "B.DH.TXD", "_may": True, "cac_dong": [{"dong": 0, "latex": _txd_latex(bu["TXD"])}]})
    if "dao_ham" in bu:
        them.append({"ma_buoc": "B.DH.DAOHAM", "_may": True, "cac_dong": [{"dong": 0, "latex": _bt_latex(bu["dao_ham"])}]})
    if "y_phay_bang_0" in bu:
        dong = []
        if bu["y_phay_bang_0"]:
            dong.append({"dong": 0, "latex": "x \\in \\{%s\\}" % ";".join(_bt_latex(v) for v in bu["y_phay_bang_0"])})
        else:
            dong.append({"dong": 0, "latex": "không có nghiệm"})
        for i, v in enumerate(bu["y_phay_khong_xd"]):
            dong.append({"dong": i + 1, "latex": "y' không xác định tại %s" % _bt_latex(v), "loai": "KHONG_XD"})
        them.append({"ma_buoc": "B.DH.NGHIEM", "_may": True, "cac_dong": dong})
    p2["cac_buoc"] = them + list(p2.get("cac_buoc") or [])
    bl, som = payload_to_bai_lam(p2, den)
    if bl is not None:
        # giá trị máy dùng đúng như máy tính (không qua bộ chuẩn hóa)
        for k in ("TXD", "dao_ham", "y_phay_bang_0", "y_phay_khong_xd"):
            if k in bu:
                bl[k] = bu[k]
        if "dao_ham" in bu:
            bl["_exprs"] = [bu["dao_ham"]]
        if "y_phay_bang_0" in bu:
            bl["_last_nghiem"] = 0
        bl["_chuan"] = [c for c in bl.get("_chuan") or [] if c.get("ma_buoc") not in {s["ma_buoc"] for s in them}]
    return bl, som


def _txd_latex(t):
    t = str(t).strip()
    if t == "R":
        return "\\mathbb{R}"
    if t.startswith("R \\"):
        return "\\mathbb{R}\\setminus\\{%s\\}" % t[t.index("{") + 1:t.rindex("}")].replace(", ", ";")
    return t


def _bt_latex(e):
    """Biểu thức SymPy của máy -> LaTeX để đi qua bộ chuẩn hóa như dòng học sinh."""
    import sympy as _sp
    return _sp.latex(K.P(str(e)))


def _ve_buoc_bat_dau(payload, r):
    """Vấn đề rơi vào bước do đề cho sẵn (trước buoc_bat_dau) được gắn về bước bắt đầu: vd bài bắt đầu ở B.DH.XETDAU thiếu
    mốc -> DIEM_THIEU ở B.DH.XETDAU, o = {X, null}; thừa mốc -> ERR.DH.31 (DAC-TA §3.4(a))."""
    bd = payload.get("buoc_bat_dau")
    if not bd or bd not in ORDER or bd == ORDER[0] or r.get("ket_qua") != "SAI":
        return
    bd_i = ORDER.index(bd)
    for v in (r.get("cac_van_de") or []) + [r]:
        bs = v.get("buoc_sai") or {}
        if bs.get("ma_buoc") in ORDER and ORDER.index(bs["ma_buoc"]) < bd_i:
            o = bs.get("o")
            if v.get("loai_ket_qua") == "DIEM_THIEU" and bd == "B.DH.XETDAU":
                o = {"hang": "X", "k": None}
            v["buoc_sai"] = _buoc(bd, None if o else bs.get("dong"), o)
            if v.get("loai_ket_qua") == "DIEM_THUA" and v.get("ma_loi") == "ERR.DH.24" and bd == "B.DH.XETDAU":
                v["ma_loi"], v["ky_nang"] = "ERR.DH.31", "T12.DH.03"


def grade(payload):
    if not isinstance(payload, dict) or not isinstance(payload.get("cac_buoc"), list):
        return _kkd_dau_vao(payload if isinstance(payload, dict) else {}, None, None, None, "payload sai khuôn")
    tc = tu_choi_payload(payload)
    if not tc:
        # lớp dấu hiệu code của bản vá Kiểm định 0002 (fail-closed), chạy sau cổng danh sách trắng
        try:
            if _chuoi_doc_hai(payload):
                tc = (None, None, None, "dấu hiệu code (bản vá Kiểm định 0002)")
        except Exception:
            tc = (None, None, None, "không quét được chuỗi")
    if tc:
        return _kkd_dau_vao(payload, *tc)
    if "buoc_bat_dau" in payload and payload.get("buoc_bat_dau") not in ORDER:
        return _kkd_dau_vao(payload, None, None, None, "buoc_bat_dau không hợp lệ")
    del K._TU_CHOI[:]
    r = _grade_core(payload)
    _ve_buoc_bat_dau(payload, r)
    try:
        if r.get("ket_qua") == "DAT":
            r = _diem_ngoai_txd(payload, r) or r
        _hau_xu_ly(payload, r)
        _dau_doi_do_thieu_moc(payload, r)
    except Exception:
        pass  # hậu xử lý chỉ tinh chỉnh vị trí; lỗi ở đây không được đổi kết quả chấm
    if K._TU_CHOI:
        # bộ phân tích an toàn đã từ chối một chuỗi ở đâu đó (kể cả khi lỗi bị nuốt): không bao giờ giữ kết quả chấm
        return _kkd_dau_vao(payload, None, None, None, K._TU_CHOI[0])
    return r


def _diem_ngoai_txd(payload, r):
    """Điểm HS ghi ở bước NGHIEM (nghiệm hoặc điểm y' không xác định) mà nằm NGOÀI tập xác định là mốc thừa
    (ERR.DH.24, DIEM_THUA, B.DH.NGHIEM), vd x/(x+3) ghi x = -3 là điểm y' không xác định."""
    den = payload.get("nop_toi") or "B.DH.KETLUAN"
    if den not in ORDER or ORDER.index(den) < ORDER.index("B.DH.NGHIEM"):
        return None
    f = K.P(payload.get("ham"))
    cf = K.dieu_kien(f)
    _r, _k, chuan, _loi = _parse_nghiem_lines(_buoc_theo_ma(payload, "B.DH.NGHIEM").get("cac_dong") or [])
    for c in chuan or []:
        for v in (c.get("chuoi_chuan_hoa") or "").split(","):
            if not v or v == "rong":
                continue
            if K.gia_tri(f, {K.x: K.P(v)}, cf) is None:
                bs = _buoc("B.DH.NGHIEM", c.get("dong", 0), None)
                vd = [{"id": "VD1", "loai_ket_qua": "DIEM_THUA", "buoc_sai": bs, "ma_loi": "ERR.DH.24", "do_tin_cay": 0.8,
                       "ky_nang": "T12.DH.02"}]
                return _pack("SAI", "DIEM_THUA", bs, "thua_nghiem_vi_pham_dkxd", _per(den, "B.DH.NGHIEM"), r.get("chuan_hoa") or [],
                             nop_toi=den, cac_van_de=vd, ma_loi_tin=("ERR.DH.24", 0.8),
                             thong_bao="Có điểm em ghi ở bước điểm tới hạn không thuộc tập xác định. Em đối chiếu từng điểm với tập xác định.")
    return None


def _buoc_theo_ma(payload, ma):
    for st in payload.get("cac_buoc") or []:
        if st.get("ma_buoc") == ma:
            return st
    return {}


def _dong_chua_moc(payload, gia_tri):
    """Dòng (0-based) ở bước NGHIEM mà HS ghi giá trị này."""
    _r, _k, chuan, _loi = _parse_nghiem_lines(_buoc_theo_ma(payload, "B.DH.NGHIEM").get("cac_dong") or [])
    for c in chuan or []:
        for v in (c.get("chuoi_chuan_hoa") or "").split(","):
            if v and v != "rong" and _bang(v, gia_tri):
                return c.get("dong", 0)
    return None


def _dong_ket_luan_sai(payload):
    """Ô kết luận đầu tiên (theo thứ tự Đồng biến → Nghịch biến → Cực đại → Cực tiểu) có nội dung khác lời giải máy."""
    ds = _cac_o_ket_luan_sai(payload)
    return ds[0][0] if ds else None


def _cac_o_ket_luan_sai(payload):
    """Mọi ô kết luận có nội dung khác lời giải máy, theo thứ tự Đồng biến → Nghịch biến → Cực đại → Cực tiểu: [(dong, key)]."""
    import re
    step = _buoc_theo_ma(payload, "B.DH.KETLUAN")
    kl, _ = _parse_ket_luan(step)
    may = bai_lam_may(payload.get("ham"))
    if kl is None or not may:
        return []
    km = may["ket_luan"]
    chi_y = kl.pop("_chi_tung_do", None) or {}   # 0002e: ô chỉ tung độ
    dong_cua = {}
    ds = _idx(step.get("cac_dong") or [])
    khai_ds = list(step.get("khai_bao") or [])
    for vi_tri, line in enumerate(ds):
        nhan = (line.get("loai") or "").upper()
        key = _NHAN_KL.get(nhan)
        t0 = (line.get("latex") or "").lower()
        if not key and len(khai_ds) == len(ds) and not re.search(r"đồng biến|dong bien|nghịch biến|nghich bien|cực|cuc (dai|tieu)", t0):
            key = khai_ds[vi_tri] if khai_ds[vi_tri] in ("dong_bien", "nghich_bien", "cuc_dai", "cuc_tieu") else None
        if not key:
            t = (line.get("latex") or "").lower()
            key = ("dong_bien" if re.search(r"đồng biến|dong bien", t) else "nghich_bien" if re.search(r"nghịch biến|nghich bien", t)
                   else "cuc_dai" if re.search(r"cực đại|cuc dai", t) else "cuc_tieu" if re.search(r"cực tiểu|cuc tieu", t) else None)
        if key and key not in dong_cua:
            dong_cua[key] = line.get("dong", 0)

    def ds_khoang(d, k):
        if d.get(k + "_tren_tap"):
            return _khoang_chuan(str(d[k + "_tren_tap"]).split(" U "))
        return _khoang_chuan(d.get(k))

    sai = []
    for key in ("dong_bien", "nghich_bien"):
        if key in kl or key + "_tren_tap" in kl:
            if ds_khoang(kl, key) != ds_khoang(km, key) or (bool(kl.get(key + "_tren_tap")) and not km.get(key + "_tren_tap")):
                sai.append(key)
    for key in ("cuc_dai", "cuc_tieu"):
        if key in chi_y:
            if not all(any(_bang(y, v) for v in (km.get("gia_tri_" + key) or [])) for y in chi_y[key]["ys"]):
                sai.append(key)
                dong_cua[key] = chi_y[key]["dong"]
            continue
        if key + "_x" in kl or "gia_tri_" + key in kl:
            if not (_cung_ds(kl.get(key + "_x"), km.get(key + "_x")) and
                    (not kl.get("gia_tri_" + key) or _cung_ds(kl.get("gia_tri_" + key), km.get("gia_tri_" + key)))):
                sai.append(key)
    out = []
    for key in sai:
        if key in dong_cua and all(d != dong_cua[key] for d, _k in out):
            out.append((dong_cua[key], key))
    return out


def _so_thuc(v):
    """Giá trị mốc HS nhập -> float (qua bộ chuẩn hóa an toàn). None nếu không đọc được."""
    import math
    try:
        t = str(v).strip().replace("−", "-")
        if t in ("-oo", "-∞"):
            return -math.inf
        if t in ("+oo", "oo", "+∞", "∞"):
            return math.inf
        n = normalize_expr(t)
        if n is None:
            return None
        return float(K.P(n))
    except Exception:
        return None


def _dau_doi_do_thieu_moc(payload, r):
    """UXT-04-d (DAC-TA §3.4, SP-05): bước Nghiệm thiếu điểm tới hạn (DIEM_THIEU, B.DH.NGHIEM) mà HS đã dựng bảng theo tập
    điểm thiếu: mỗi ô khoảng DAU_YPHAY của bảng HS mà y' thật sự đổi dấu bên trong là HỆ QUẢ (DAU_DOI_TRONG_KHOANG,
    nguyen_nhan = vấn đề gốc). Không nêu điểm bị thiếu; không thêm lỗi gốc mới."""
    den = payload.get("nop_toi")
    if r.get("ket_qua") != "SAI" or den not in ORDER or ORDER.index(den) < ORDER.index("B.DH.XETDAU"):
        return
    ds = r.get("cac_van_de") or []
    goc = next((v for v in ds if v.get("loai_ket_qua") == "DIEM_THIEU" and not v.get("nguyen_nhan")
                and (v.get("buoc_sai") or {}).get("ma_buoc") == "B.DH.NGHIEM"), None)
    if not goc or any(v.get("nguyen_nhan") == goc.get("id") for v in ds):
        return
    xs = sorted(((c.get("k"), c.get("gia_tri")) for c in _cells(_buoc_theo_ma(payload, "B.DH.XETDAU"))
                 if c.get("hang") in ("X", "x") and isinstance(c.get("k"), int)), key=lambda t: t[0])
    vals = [_so_thuc(g) for _k, g in xs]
    if any(v is None for v in vals) or any(b <= a for a, b in zip(vals, vals[1:])):
        return   # hàng X không đọc được hoặc sai thứ tự: để luật thứ tự mốc xử lý
    may = bai_lam_may(payload.get("ham")) or {}
    bang = may.get("bang") or {}
    moc = [_so_thuc(m) for m in bang.get("moc") or []]
    dau = list(bang.get("dau") or [])
    if not dau or len(moc) != len(dau) + 1 or any(m is None for m in moc):
        return
    import math
    bien = [-math.inf] + vals + [math.inf]
    so = len(ds)
    for j in range(len(bien) - 1):
        a, b = bien[j], bien[j + 1]
        cac_dau = {dau[i] for i in range(len(dau)) if moc[i] < b and moc[i + 1] > a and dau[i] in ("+", "-")}
        if len(cac_dau) > 1:
            so += 1
            ds.append({"id": "VD%d" % so, "loai_ket_qua": "DAU_DOI_TRONG_KHOANG",
                       "buoc_sai": _buoc("B.DH.XETDAU", None, {"hang": "DAU_YPHAY", "k": 2 * j}),
                       "nguyen_nhan": goc.get("id"), "ma_loi": _MA_LOI["DAU_DOI_TRONG_KHOANG"][0],
                       "do_tin_cay": _MA_LOI["DAU_DOI_TRONG_KHOANG"][1]})
    r["cac_van_de"] = ds


def _hau_xu_ly(payload, r):
    """Chốt 29/09: (1) mốc thừa có trong nghiệm HS (ERR.DH.24, B.DH.NGHIEM): o = {X, k} của mốc; kèm `dong_lien_quan`
    = dòng nghiệm chứa mốc (0-based).
    (2) lỗi kết luận chỉ ra đúng ô (dong = chỉ số ô) thay cho dòng 0."""
    if r.get("ket_qua") != "SAI":
        return
    cells = {(c.get("hang"), c.get("k")): c.get("gia_tri") for c in _cells(_buoc_theo_ma(payload, "B.DH.XETDAU"))}
    ds = r.get("cac_van_de") or []
    dkl = None
    for v in ds + [r]:
        bs = v.get("buoc_sai") or {}
        o = bs.get("o") or {}
        if v.get("loai_ket_qua") == "DIEM_THUA" and bs.get("ma_buoc") == "B.DH.NGHIEM" and o.get("hang") == "X" and o.get("k") is not None:
            gt = cells.get(("X", o["k"]))
            dong = _dong_chua_moc(payload, gt) if gt is not None else None
            if dong is not None:
                # Chốt No 11:28: vấn đề giữ o = {X, k} của mốc thừa (UI tô đỏ ô đó), bước B.DH.NGHIEM.
                # Dòng nghiệm chứa mốc ghi thêm ở `dong_lien_quan` để UI chỉ chỗ cần sửa; không thành vấn đề thứ hai.
                v["dong_lien_quan"] = dong
        if (v.get("loai_ket_qua") == "SAI_KET_LUAN" or (v.get("loai_ket_qua") == "SAI_GIA_TRI" and v.get("ma_loi") == "ERR.DH.22")) \
                and bs.get("ma_buoc") == "B.DH.KETLUAN" and v.get("ma_loi") != "ERR.DH.11":
            if dkl is None:
                dkl = _dong_ket_luan_sai(payload)
            if dkl is not None:
                v["buoc_sai"] = _buoc("B.DH.KETLUAN", dkl, None)
                if v is r:
                    ten = {"DONG_BIEN": "Đồng biến", "NGHICH_BIEN": "Nghịch biến", "CUC_DAI": "Cực đại", "CUC_TIEU": "Cực tiểu"}
                    nhan = next((str(l.get("loai") or "").upper() for l in _buoc_theo_ma(payload, "B.DH.KETLUAN").get("cac_dong") or []
                                 if l.get("dong") == dkl), "")
                    r["thong_bao"] = ("Bước kết luận: ô %s cần xem lại." % ten[nhan]) if nhan in ten else \
                        "Bước kết luận, dòng %d cần xem lại." % (dkl + 1)
    _them_o_ket_luan_sai(payload, r)


def _them_o_ket_luan_sai(payload, r):
    """UXT-05-b (DAC-TA v1.3 điểm 10: API luôn trả đủ): nhiều ô kết luận cùng sai -> mỗi ô một vấn đề gốc (dong = chỉ số ô).
    Màn học sinh chỉ mở ô đầu; giáo viên thấy đủ. Chỉ thêm khi vấn đề đầu là lỗi kết luận thường (không đổi ERR.DH.11/22, dấu U)."""
    ds = r.get("cac_van_de") or []
    kl_ds = [v for v in ds if (v.get("buoc_sai") or {}).get("ma_buoc") == "B.DH.KETLUAN"]
    if len(kl_ds) != 1 or kl_ds[0].get("loai_ket_qua") != "SAI_KET_LUAN" or kl_ds[0].get("ma_loi") in ("ERR.DH.07", "ERR.DH.11", "ERR.DH.22"):
        return
    if r.get("dau_U") or r.get("toan_dung") is not None:
        return
    dau = kl_ds[0]
    try:
        cac = _cac_o_ket_luan_sai(payload)
    except Exception:
        return
    if len(cac) < 2 or cac[0][0] != (dau.get("buoc_sai") or {}).get("dong"):
        return
    so = len(ds)
    for dong, key in cac[1:]:
        so += 1
        ma, tin = _MA_LOI["cuc_tri_sai" if key.startswith("cuc") else "don_dieu_sai"]
        ds.append({"id": "VD%d" % so, "loai_ket_qua": "SAI_KET_LUAN", "buoc_sai": _buoc("B.DH.KETLUAN", dong, None),
                   "ma_loi": ma, "do_tin_cay": tin})
    r["cac_van_de"] = ds


def _grade_core(payload):
    den = payload.get("nop_toi") or "B.DH.KETLUAN"
    if den not in ORDER:
        den = "B.DH.KETLUAN"
    bl, som = payload_to_bai_lam(payload, den)
    if som:
        return som
    chi_y = (bl.get("ket_luan") or {}).pop("_chi_tung_do", None) if isinstance(bl.get("ket_luan"), dict) else None
    full = _overlay_may(bl, den)
    if full is None:
        return _pack("KHONG_KIEM_DUOC", "KHONG_KIEM_DUOC", None, "khong_giai_duoc", {}, bl.get("_chuan") or [], nop_toi=den)
    if chi_y:
        # 0002e: ô chỉ tung độ -> bộ kiểm thấy ô đó trung tính (vị trí + giá trị của lời giải máy); tung độ HS chấm sau.
        kl_m = (bai_lam_may(bl["ham"]) or {}).get("ket_luan") or {}
        full["ket_luan"] = dict(full.get("ket_luan") or {})
        for key in chi_y:
            full["ket_luan"][key + "_x"] = list(kl_m.get(key + "_x") or [])
            full["ket_luan"]["gia_tri_" + key] = list(kl_m.get("gia_tri_" + key) or [])
            if not full["ket_luan"]["gia_tri_" + key]:
                full["ket_luan"].pop("gia_tri_" + key)
    # bỏ khóa nội bộ
    clean = {k: v for k, v in full.items() if not k.startswith("_")}
    try:
        r = K.kiem_5_buoc(clean)
    except Exception as ex:
        return _pack("KHONG_KIEM_DUOC", "KHONG_KIEM_DUOC", None, "loi_cong_cu", {}, bl.get("_chuan") or [], nop_toi=den)
    last_n = bl.get("_last_nghiem", 0)
    if r["trang_thai"] == "DAT" and chi_y and den == "B.DH.KETLUAN":
        return _cham_chi_tung_do(chi_y, bl["ham"], bl.get("_chuan") or [], den)
    if r["trang_thai"] == "DAT":
        return _pack("DAT", "DAT", None, "dat", _per(den), bl.get("_chuan") or [], chua_xong=den != "B.DH.KETLUAN", nop_toi=den)
    if r.get("cac_van_de"):
        ds, dau_tien = _van_de_san_pham(r, last_n, clean)
        buoc_sai, loai = dau_tien["buoc_sai"], dau_tien["loai_ket_qua"]
        if ORDER.index(buoc_sai["ma_buoc"]) > ORDER.index(den):
            return _pack("DAT", "DAT", None, "dat", _per(den), bl.get("_chuan") or [], chua_xong=True, nop_toi=den)
        ds = [v for v in ds if ORDER.index(v["buoc_sai"]["ma_buoc"]) <= ORDER.index(den)]
        return _pack("SAI", loai, buoc_sai, r.get("loai_kiem"), _per(den, buoc_sai["ma_buoc"]), bl.get("_chuan") or [], nop_toi=den,
                     cac_van_de=ds, ma_loi_tin=(dau_tien["ma_loi"], dau_tien["do_tin_cay"]), thong_bao=dau_tien.get("_thong_bao"))
    buoc_sai, loai, lk = _buoc_tu_ket_qua(r, last_n)
    # Nếu lỗi nằm ở bước sau phần học sinh nộp (không mong đợi vì đã vá bằng lời giải máy) thì coi phần đã nộp là đạt.
    if buoc_sai and ORDER.index(buoc_sai["ma_buoc"]) > ORDER.index(den):
        return _pack("DAT", "DAT", None, "dat", _per(den), bl.get("_chuan") or [], chua_xong=den != "B.DH.KETLUAN", nop_toi=den)
    if r["trang_thai"] == "KHONG_KIEM_DUOC":
        return _pack("KHONG_KIEM_DUOC", "KHONG_KIEM_DUOC", buoc_sai, lk, _per(den, buoc_sai["ma_buoc"] if buoc_sai else den, "KHONG_KIEM_DUOC"), bl.get("_chuan") or [], nop_toi=den)
    # Sai đạo hàm ở dòng 0 khi có nhiều dòng: kiem_5_buoc chỉ thấy dòng đầu.
    if buoc_sai and buoc_sai["ma_buoc"] == "B.DH.DAOHAM":
        buoc_sai = _buoc("B.DH.DAOHAM", 0, None)
    ma_tin, tb = _ma_loi_chi_tiet(r, lk, loai, clean)
    if ma_tin and ma_tin[0] == "ERR.DH.22" and loai == "SAI_KET_LUAN":
        loai = "SAI_GIA_TRI"   # 0002e (Sư phạm 13:10): tung độ sai, có hay không có hoành độ -> SAI_GIA_TRI, ERR.DH.22
    if r.get("dau_U"):
        # Luật dấu U (Sư phạm 29/09 12:13): luôn ERR.DH.07; cờ toan_dung cho gia sư chọn câu nhắn và cho mô hình
        # học sinh (toan_dung = true: lỗi quy ước trình bày, không trừ mức hiểu kỹ năng như ca sai toán).
        ma_tin = ("ERR.DH.07", _TIN_CAY_LUAT)
        out = _pack("SAI", loai, buoc_sai, lk or loai, _per(den, buoc_sai["ma_buoc"] if buoc_sai else den), bl.get("_chuan") or [],
                    nop_toi=den, ma_loi_tin=ma_tin, thong_bao=tb)
        out["toan_dung"] = bool(r.get("toan_dung"))
        for v in out["cac_van_de"]:
            if v.get("loai_ket_qua") == "SAI_KET_LUAN":
                v["toan_dung"] = bool(r.get("toan_dung"))
        return out
    return _pack("SAI", loai, buoc_sai, lk or loai, _per(den, buoc_sai["ma_buoc"] if buoc_sai else den), bl.get("_chuan") or [], nop_toi=den,
                 ma_loi_tin=ma_tin, thong_bao=tb)


_TEN_O_CT = {"cuc_dai": ("Cực đại", "cực đại"), "cuc_tieu": ("Cực tiểu", "cực tiểu")}


def _cham_chi_tung_do(chi_y, ham, chuan, den):
    """0002e (Sư phạm chốt 29/09 13:10). Ô cực đại/cực tiểu chỉ ghi tung độ, mọi phần khác của bài đã đúng:
    - tung độ sai (không phải giá trị cực trị loại đó của hàm) -> SAI_GIA_TRI, ERR.DH.22;
    - tung độ đúng, thiếu hoành độ -> SAI_KET_LUAN, ERR.DH.11 (nhầm điểm cực trị với giá trị cực trị). Câu nhắn hỏi
      "Hàm số đạt cực đại tại điểm nào?", không nhắc lại giá trị.
    Chưa có ngoại lệ "đề chỉ hỏi giá trị cực đại" (khai_bao chỉ có cuc_dai/cuc_tieu): luôn cần hoành độ."""
    kl_m = (bai_lam_may(ham) or {}).get("ket_luan") or {}
    thu_tu = [k for k in ("cuc_dai", "cuc_tieu") if k in chi_y]
    for key in thu_tu:
        dung = kl_m.get("gia_tri_" + key) or []
        if not dung or not all(any(_bang(y, v) for v in dung) for y in chi_y[key]["ys"]):
            if not (kl_m.get(key + "_x") or []):
                # hàm không có cực trị loại này mà HS ghi một giá trị: kết luận sai (có cực trị khi không có)
                bs = _buoc("B.DH.KETLUAN", chi_y[key]["dong"], None)
                return _pack("SAI", "SAI_KET_LUAN", bs, "cuc_tri_sai", _per(den, "B.DH.KETLUAN"), chuan, nop_toi=den,
                             thong_bao="Bước kết luận: ô %s cần xem lại." % _TEN_O_CT[key][0])
            bs = _buoc("B.DH.KETLUAN", chi_y[key]["dong"], None)
            return _pack("SAI", "SAI_GIA_TRI", bs, "cuc_tri_sai", _per(den, "B.DH.KETLUAN"), chuan, nop_toi=den,
                         ma_loi_tin=("ERR.DH.22", 0.8), thong_bao="Bước kết luận: ô %s cần xem lại." % _TEN_O_CT[key][0])
    key = thu_tu[0]
    bs = _buoc("B.DH.KETLUAN", chi_y[key]["dong"], None)
    return _pack("SAI", "SAI_KET_LUAN", bs, "cuc_tri_sai", _per(den, "B.DH.KETLUAN"), chuan, nop_toi=den,
                 ma_loi_tin=("ERR.DH.11", 0.85),
                 thong_bao="Bước kết luận: ô %s mới có giá trị, còn thiếu điểm. Hàm số đạt %s tại điểm nào?" % _TEN_O_CT[key])


def _bang(a, b):
    try:
        return K.la_khong(K.P(str(a)) - K.P(str(b)))
    except Exception:
        return str(a).strip() == str(b).strip()


def _cung_ds(xs, ys):
    xs, ys = list(xs or []), list(ys or [])
    return len(xs) == len(ys) and all(any(_bang(a, b) for b in ys) for a in xs)


def _khoang_chuan(ds):
    return sorted(str(v).replace(" ", "").replace("+oo", "oo") for v in (ds or []))


def _ma_loi_chi_tiet(r, lk, loai, bl):
    """SP-07: mã lỗi cụ thể khi có dấu hiệu riêng; không có thì trả None để dùng mã theo loại (tin cậy < 0,65)."""
    try:
        may = bai_lam_may(bl["ham"])
    except Exception:
        may = None
    if not may:
        return None, None
    kl, kl_m = bl.get("ket_luan") or {}, may["ket_luan"]
    if lk == "don_dieu_sai":
        db, nb = _khoang_chuan(kl.get("dong_bien")), _khoang_chuan(kl.get("nghich_bien"))
        if (db or nb) and db == _khoang_chuan(kl_m.get("nghich_bien")) and nb == _khoang_chuan(kl_m.get("dong_bien")):
            return ("ERR.DH.08", 0.8), None  # đọc ngược chiều đồng biến / nghịch biến
        return None, None
    if lk == "cuc_tri_sai":
        cd, ct = kl.get("cuc_dai_x") or [], kl.get("cuc_tieu_x") or []
        cd_m, ct_m = kl_m.get("cuc_dai_x") or [], kl_m.get("cuc_tieu_x") or []
        if (cd or ct) and _cung_ds(cd, ct_m) and _cung_ds(ct, cd_m):
            return ("ERR.DH.12", 0.85), None
        nghiem0 = may.get("y_phay_bang_0") or []
        for x0 in list(cd) + list(ct):
            la_ct_that = any(_bang(x0, v) for v in list(cd_m) + list(ct_m))
            if not la_ct_that and any(_bang(x0, v) for v in nghiem0):
                return ("ERR.DH.10", 0.85), None  # y'(x0) = 0 nhưng y' không đổi dấu
        if _cung_ds(cd, cd_m) and _cung_ds(ct, ct_m):
            # vị trí đúng, giá trị sai: thử giả thuyết thay vào y'
            try:
                fp = K.P(may["dao_ham"])
                for xs, ys in ((cd, kl.get("gia_tri_cuc_dai") or []), (ct, kl.get("gia_tri_cuc_tieu") or [])):
                    for x0, y0 in zip(xs, ys):
                        if K.la_khong(fp.subs(K.x, K.P(str(x0))) - K.P(str(y0))):
                            return ("ERR.DH.22", 0.85), None
            except Exception:
                pass
            return ("ERR.DH.22", 0.6), None
        return None, None
    if lk == "sai_diem_toi_han":
        D = None
        try:
            f = K.P(bl["ham"])
            cf = K.dieu_kien(f)
            for v in (bl.get("y_phay_bang_0") or []) + (bl.get("y_phay_khong_xd") or []):
                if K.gia_tri(f, {K.x: K.P(str(v))}, cf) is None:
                    return ("ERR.DH.02", 0.8), "Có điểm em ghi không thuộc tập xác định. Em đối chiếu lại từng điểm với tập xác định."
        except Exception:
            pass
        thieu_kxd = [v for v in (may.get("y_phay_khong_xd") or []) if not any(_bang(v, u) for u in (bl.get("y_phay_khong_xd") or []))]
        if thieu_kxd:
            return ("ERR.DH.04", 0.75), None
        thua = [v for v in (bl.get("y_phay_bang_0") or []) if not any(_bang(v, u) for u in (may.get("y_phay_bang_0") or []))]
        thieu = [v for v in (may.get("y_phay_bang_0") or []) if not any(_bang(v, u) for u in (bl.get("y_phay_bang_0") or []))]
        if thua and not thieu:
            return ("ERR.DH.21", 0.7), None
        return None, None
    return None, None


def _van_de_san_pham(r, last_n, bl):
    """Đổi cac_van_de của bộ kiểm sang dạng sản phẩm (k, dong từ 0; hàng HOA). Bỏ giá trị điểm."""
    try:
        f = K.P(bl["ham"])
        cf = K.dieu_kien(f)
    except Exception:
        f = cf = None
    ra = []
    for v in r["cac_van_de"]:
        bs = v.get("buoc_sai") or {}
        o = _translate_o(bs.get("o"))
        dong = bs.get("dong")
        if o:
            dong = None
        elif dong in (1, 2, 3, 5):
            dong = last_n if bs.get("ma_buoc") == "B.DH.NGHIEM" else 0
        loai = v["loai_ket_qua"]
        d = {"id": v["id"], "loai_ket_qua": loai, "buoc_sai": _buoc(bs.get("ma_buoc"), dong, o)}
        if v.get("nguyen_nhan"):
            d["nguyen_nhan"] = v["nguyen_nhan"]
        if v.get("so_diem_thieu") is not None:
            d["so_diem_thieu"] = v["so_diem_thieu"]
        if v.get("ma_loi"):
            d["ma_loi"], d["do_tin_cay"] = v["ma_loi"], _TIN_CAY_LUAT
        else:
            d["ma_loi"], d["do_tin_cay"] = _MA_LOI.get(loai, (None, None))
        if loai == "DIEM_THIEU" and f is not None:
            diem = v.get("diem") or []
            try:
                ngoai = [p for p in diem if K.gia_tri(f, {K.x: K.P(p)}, cf) is None]
            except Exception:
                ngoai = []
            if diem and len(ngoai) == len(diem):
                # SP-07: điểm bị loại khỏi TXĐ không gọi là "điểm tới hạn"
                d["ma_loi"], d["do_tin_cay"] = "ERR.DH.02", 0.75
                d["_thong_bao"] = "Hàng x còn thiếu một mốc: điểm làm hàm số không xác định cũng phải đặt trên hàng x (ô tại đó ghi ||)."
        if v.get("ky_nang"):
            d["ky_nang"] = v["ky_nang"]
        ra.append(d)
    dau = ra[0]
    for d in ra[1:]:
        d.pop("_thong_bao", None)
    ra_sach = [{k: val for k, val in d.items() if not k.startswith("_")} for d in ra]
    return ra_sach, dau


def bai_lam_sang_payload(bl, ham=None):
    """Đổi bài làm kiểu YAML kiểm định sang payload sản phẩm (k từ 0) để chấm lại."""
    ham = ham or bl["ham"]
    cells = []
    moc = bl["bang"]["moc"]
    diem = moc[1:-1]
    for i, p in enumerate(diem):
        cells.append({"hang": "X", "k": i, "gia_tri": str(p)})
    for j, s in enumerate(bl["bang"]["dau"]):
        cells.append({"hang": "DAU_YPHAY", "k": 2 * j, "gia_tri": s})
    for j, s in enumerate(bl["bang"].get("dau_tai_diem") or []):
        cells.append({"hang": "DAU_YPHAY", "k": 2 * j + 1, "gia_tri": s})
    for j, s in enumerate(bl["bang"].get("chieu") or []):
        g = {"tang": "TANG", "giam": "GIAM", "khong_xd": "||"}.get(s, s)
        cells.append({"hang": "BIEN_THIEN", "k": 2 * j, "gia_tri": g})
    nghiem = []
    for i, v in enumerate(bl.get("y_phay_bang_0") or []):
        nghiem.append({"dong": i, "latex": "x = %s" % v, "loai": "NGHIEM"})
    base = len(nghiem)
    for i, v in enumerate(bl.get("y_phay_khong_xd") or []):
        nghiem.append({"dong": base + i, "latex": "y' không xác định tại x = %s" % v, "loai": "KHONG_XD"})
    if not nghiem:
        nghiem.append({"dong": 0, "latex": "không có nghiệm", "loai": "NGHIEM"})
    kl = bl.get("ket_luan") or {}
    dong_kl = []
    n = 0
    khai = []
    if "dong_bien" in kl or "dong_bien_tren_tap" in kl:
        khai.append("dong_bien")
        if kl.get("dong_bien_tren_tap"):
            text = "đồng biến trên %s" % kl["dong_bien_tren_tap"]
        else:
            text = "đồng biến trên " + " và ".join(kl.get("dong_bien") or [])
            if not kl.get("dong_bien"):
                text = "không đồng biến trên khoảng nào"
        dong_kl.append({"dong": n, "latex": text})
        n += 1
    if "nghich_bien" in kl or "nghich_bien_tren_tap" in kl:
        khai.append("nghich_bien")
        if kl.get("nghich_bien_tren_tap"):
            text = "nghịch biến trên %s" % kl["nghich_bien_tren_tap"]
        else:
            text = "nghịch biến trên " + " và ".join(kl.get("nghich_bien") or [])
            if not kl.get("nghich_bien"):
                text = "không nghịch biến trên khoảng nào"
        dong_kl.append({"dong": n, "latex": text})
        n += 1
    if any(k in kl for k in ("cuc_dai_x", "gia_tri_cuc_dai")):
        khai.append("cuc_dai")
        xs = kl.get("cuc_dai_x") or []
        ys = kl.get("gia_tri_cuc_dai") or []
        if not xs:
            dong_kl.append({"dong": n, "latex": "không có cực đại"})
        else:
            bits = []
            for i, x in enumerate(xs):
                bit = "x = %s" % x
                if i < len(ys):
                    bit += ", y = %s" % ys[i]
                bits.append(bit)
            dong_kl.append({"dong": n, "latex": "cực đại tại " + "; ".join(bits)})
        n += 1
    if any(k in kl for k in ("cuc_tieu_x", "gia_tri_cuc_tieu")):
        khai.append("cuc_tieu")
        xs = kl.get("cuc_tieu_x") or []
        ys = kl.get("gia_tri_cuc_tieu") or []
        if not xs:
            dong_kl.append({"dong": n, "latex": "không có cực tiểu"})
        else:
            bits = []
            for i, x in enumerate(xs):
                bit = "x = %s" % x
                if i < len(ys):
                    bit += ", y = %s" % ys[i]
                bits.append(bit)
            dong_kl.append({"dong": n, "latex": "cực tiểu tại " + "; ".join(bits)})
    # Dạng ô của app (v1.3 S32-S37): {db, nb, cd, ct} gửi nguyên chữ HS gõ, kèm nhãn ô — đúng như UI gửi.
    klo = bl.get("ket_luan_o")
    if klo:
        dong_kl, khai = [], []
        for key, loai, ten in (("db", "DONG_BIEN", "dong_bien"), ("nb", "NGHICH_BIEN", "nghich_bien"),
                               ("cd", "CUC_DAI", "cuc_dai"), ("ct", "CUC_TIEU", "cuc_tieu")):
            if key in klo:
                khai.append(ten)
                dong_kl.append({"dong": len(dong_kl), "latex": str(klo.get(key) or ""), "loai": loai})
    return {
        "ham": ham,
        "nop_toi": "B.DH.KETLUAN",
        "cac_buoc": [
            {"ma_buoc": "B.DH.TXD", "cac_dong": [{"dong": 0, "latex": bl["TXD"]}]},
            {"ma_buoc": "B.DH.DAOHAM", "cac_dong": [{"dong": 0, "latex": bl["dao_ham"]}]},
            {"ma_buoc": "B.DH.NGHIEM", "cac_dong": nghiem},
            {"ma_buoc": "B.DH.XETDAU", "bang": {"loai_bang": "XET_DAU", "cac_o": cells}},
            {"ma_buoc": "B.DH.KETLUAN", "khai_bao": khai, "cac_dong": dong_kl},
        ],
    }


def nhan_sang_san_pham(nhan):
    """Nhãn YAML (k từ 1, dong = số thứ tự bước) -> buoc_sai sản phẩm."""
    if nhan is None:
        return None
    o = nhan.get("o")
    if not o:
        return _buoc(nhan["ma_buoc"], 0, None)
    return _buoc(nhan["ma_buoc"], None, _translate_o(o))
