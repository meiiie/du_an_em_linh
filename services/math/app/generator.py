# -*- coding: utf-8 -*-
"""Sinh biến thể tham số hóa. Lời giải do SymPy tính, không lấy từ mô hình ngôn ngữ.

Họ hàm: bậc ba, trùng phương (bậc bốn chẵn), phân thức bậc nhất.
"""
import random

from app.grader import bai_lam_sang_payload, grade
from app.leakfilter import loc_ban_nhap
from app.machine import bai_lam_may, latex_ham, su_kien_bao_ve

GOI_Y = {
    "B.DH.TXD": [
        "Hàm này có mẫu số hoặc căn bậc hai không? Hãy viết điều kiện để biểu thức có nghĩa.",
        "Tập xác định là những x làm cho mẫu khác 0 và biểu thức dưới căn không âm. Ghi thành một dòng.",
        # SP-17: cấp 3 TXĐ để trống (mọi cấp 3 đều lộ kết quả) -> hết thang thì chuyển bài tương tự
        None,
    ],
    "B.DH.DAOHAM": [
        "Em tính đạo hàm từng hạng tử bằng quy tắc nào?",
        "Nhớ (x^n)' = n x^{n-1} và đạo hàm của tổng. Với thương, dùng (u/v)' = (u'v − uv')/v^2.",
        "Tính riêng đạo hàm từng hạng tử rồi cộng lại. Viết y' ở dòng đầu của bước này.",
    ],
    "B.DH.NGHIEM": [
        "Những điểm nào có thể làm đổi chiều biến thiên?",
        "Giải y' = 0 bằng cách đưa về tích các nhân tử. Đừng bỏ điểm làm cho y' không xác định nếu điểm đó thuộc tập xác định.",
        "Đặt nhân tử chung để y' thành tích; mỗi nhân tử bằng 0 cho một điểm tới hạn. Ghi mỗi điểm một dòng.",
    ],
    "B.DH.XETDAU": [
        "Trên mỗi khoảng, em lấy dấu của y' bằng cách nào?",
        # SP-11: cấp 2 là kỹ thuật lấy dấu, không phải kiến thức bước kết luận
        "y' là tích các nhân tử nào? Mỗi nhân tử đổi dấu ở đâu? Dấu của tích trên một khoảng là tích các dấu.",
        "Chọn một giá trị trong từng khoảng, thay vào y' để biết dấu, rồi chọn mũi tên tương ứng. Chưa cần viết kết luận.",
    ],
    "B.DH.KETLUAN": [
        "Từ dấu của y' em kết luận chiều biến thiên trên từng khoảng riêng, không gộp qua điểm bị loại.",
        "y' đổi dấu từ dương sang âm thì là điểm cực đại; từ âm sang dương thì là điểm cực tiểu. y' = 0 mà không đổi dấu thì chưa phải cực trị.",
        "Viết mỗi khoảng đơn điệu một dòng, rồi ghi điểm cực trị nếu có. Em tự đọc từ bảng của mình nhé.",
    ],
}


def _hints(bl=None, de_bai=None):
    """Thang gợi ý 3 cấp mỗi bước.

    Bậc ba / trùng phương / hữu tỉ bậc nhất-bậc nhất: thang `chung` của thang mẫu Sư phạm (app/thang_mau.py), điền
    {ham}/{tu}/{mau} nguyên văn từ đề — mỗi dạng một thang riêng, đa thức không còn dùng chung một thang. Dạng khác: thang
    GOI_Y cũ bên dưới (dự phòng)."""
    if bl and bl.get("ham"):
        from app import thang_mau
        dang = thang_mau.dang_cua(bl.get("ham"))
        if dang:
            de = de_bai or "Hàm số $y = %s$." % latex_ham(bl["ham"])
            t = thang_mau.thang_chung(dang, de)
            if t:
                return t
    return _hints_cu(bl)


def _hints_cu(bl=None):
    """(Dự phòng cho dạng ngoài thang mẫu) Thang gợi ý 3 cấp mỗi bước. Có lời giải máy thì cấp 3 theo bài (SP-11): điểm thử cụ thể, không nêu dấu;
    cấp 1–2 bước đạo hàm theo dạng hàm (thương thì quy tắc thương, không nói "tách tổng")."""
    goi = {k: list(v) for k, v in GOI_Y.items()}
    if bl:
        ham = bl.get("ham") or ""
        if "/" in ham:
            goi["B.DH.DAOHAM"] = [
                "Hàm có dạng thương u/v. Em dùng quy tắc nào cho thương?",
                "(u/v)' = (u'v − uv')/v². Em xác định u, v rồi tính u', v' trước.",
                "Viết u, v, u', v' mỗi thứ một dòng nháp, rồi thế vào công thức thương và rút gọn tử số.",
            ]
            # Phân thức: điểm tới hạn đến từ tử (y' = 0) và mẫu (y' không xác định); dấu = dấu tử / dấu mẫu
            goi["B.DH.NGHIEM"][1:] = [
                "y' là một phân thức: tử số bằng 0 cho nghiệm của y' = 0, mẫu số bằng 0 cho điểm y' không xác định. "
                "Điểm nào không thuộc tập xác định thì không phải điểm tới hạn.",
                "Rút gọn y' thành một phân thức, rồi xét riêng tử số và mẫu số. Nếu tử là hằng số khác 0 thì y' = 0 vô nghiệm.",
            ]
            goi["B.DH.XETDAU"][1] = ("Dấu của một thương là dấu của tử chia dấu của mẫu. Mẫu có dạng bình phương thì luôn dương "
                                   "tại mọi điểm thuộc tập xác định.")
        else:
            goi["B.DH.DAOHAM"][1] = "Nhớ (x^n)' = n·x^(n-1), đạo hàm của tổng bằng tổng các đạo hàm, hằng số có đạo hàm 0."
        try:
            thu = _diem_thu(bl)
        except Exception:
            thu = None
        if thu:
            goi["B.DH.XETDAU"][2] = ("Thử lần lượt x = %s (mỗi số nằm trong một khoảng của bảng), thay vào y' để lấy dấu từng ô, "
                                   "rồi chọn mũi tên tương ứng." % ", ".join(thu))
    out = []
    for ma, caps in goi.items():
        out.append({"ma_buoc": ma, "cac_cap": [
            {"cap": i + 1, "noi_dung": caps[i]} if caps[i] else
            {"cap": i + 1, "noi_dung": None, "ly_do_trong": "Cấp này để trống theo Sư phạm: nói thêm là lộ kết quả."}
            for i in range(3)]})
    return out


def _phan_so(s):
    """Mốc (chuỗi máy sinh) -> Fraction, không dùng eval (F-01)."""
    from fractions import Fraction
    from app.paths import load_kiem
    v = load_kiem().P(str(s).replace("^", "**"))
    try:
        return Fraction(str(v))
    except (ValueError, ZeroDivisionError):
        return Fraction(float(v)).limit_denominator(100)


def _diem_thu(bl):
    """Một điểm thử nguyên (hoặc nửa nguyên) trong mỗi khoảng của bảng máy."""
    from fractions import Fraction
    moc = bl["bang"]["moc"]
    pts = []
    for i in range(len(moc) - 1):
        a, b = moc[i], moc[i + 1]
        if a in ("-oo",):
            v = _phan_so(b) - 1
        elif b in ("+oo", "oo"):
            v = _phan_so(a) + 1
        else:
            fa, fb = _phan_so(a), _phan_so(b)
            v = (fa + fb) / 2
            import math
            c = math.floor(fa) + 1
            if fa < c < fb:
                v = Fraction(c)
        pts.append(str(v))
    return pts


def _ham_bac_ba(rng):
    # p < q, p+q chẵn để a nguyên: y' = 3(x-p)(x-q)
    while True:
        p = rng.randint(-3, 2)
        q = rng.randint(p + 1, 4)
        if (p + q) % 2 == 0 and not (p == 0 and q == 0):
            break
    a = -3 * (p + q) // 2
    b = 3 * p * q
    c = rng.randint(-2, 3)
    ham = "x**3 + (%s)*x**2 + (%s)*x + (%s)" % (a, b, c)
    return ham, "VAN_DUNG", "APPLY", "VAN_DUNG"


def _ham_trung_phuong(rng):
    k = rng.choice([1, 2])
    a = -2 * k * k
    b = rng.randint(-2, 2)
    ham = "x**4 + (%s)*x**2 + (%s)" % (a, b)
    return ham, "VAN_DUNG", "APPLY", "VAN_DUNG"


def _ham_huu_ti(rng):
    n = rng.choice([-2, -1, 1, 2, 3])
    m = rng.choice([v for v in (-3, -1, 0, 1, 2, 4) if v != n])
    ham = "(x+(%s))/(x+(%s))" % (m, n)
    # SP-06: quy trình 5 bước với hàm cụ thể là Vận dụng; hữu tỉ không có cực trị -> kỹ năng T12.DH.03
    return ham, "VAN_DUNG", "APPLY", "VAN_DUNG"


_DANG = {"bac_ba": _ham_bac_ba, "trung_phuong": _ham_trung_phuong, "huu_ti": _ham_huu_ti}


def sinh(payload):
    dang = payload.get("dang") or "bac_ba"
    if dang not in _DANG:
        return {"loi": "dang khong ho tro"}
    rng = random.Random(int(payload.get("seed") or 1))
    ham, muc4, bloom, muc3 = _DANG[dang](rng)
    bl = bai_lam_may(ham)
    if not bl:
        return {"loi": "khong dung duoc loi giai may", "ham": ham}
    gp = bai_lam_sang_payload(bl, ham)
    g = grade(gp)
    if g["ket_qua"] != "DAT":
        return {"loi": "loi giai may khong qua tu cham", "ham": ham, "cham": g["ket_qua"], "thong_bao": g.get("thong_bao"), "buoc_sai": g.get("buoc_sai")}
    sk = su_kien_bao_ve(bl)
    lx = latex_ham(ham)
    de_bai = "Tìm các khoảng đồng biến, nghịch biến và cực trị của hàm số $y = %s$." % lx
    hints = _hints(bl, de_bai)
    for block in hints:
        for cap in block["cac_cap"]:
            if not cap.get("noi_dung"):
                continue
            quyet = loc_ban_nhap(cap["noi_dung"], sk, "")
            cap["qua_loc"] = quyet["cho_phep"]
            if not quyet["cho_phep"]:
                # Không thay bằng câu chung: để trống cấp này (không tạo dòng gợi ý), hết thang thì chuyển bài tương tự
                cap["noi_dung"] = None
                cap["ly_do_trong"] = "Bộ lọc chặn vì có thể lộ kết quả (%s)." % quyet.get("ly_do")
    return {
        "dang": dang,
        "ham": ham,
        "latex": lx,
        "de_bai": de_bai,
        "muc_do_4": muc4,
        "muc_do_bo_3": muc3,
        "muc_bloom": bloom,
        "bai_lam": bl,
        "payload_cham": gp,
        "thang_goi_y": hints,
        "su_kien": [{"loai": a, "gia_tri": b} for a, b in sk],
        "ky_nang_chinh": "T12.DH.05" if dang == "trung_phuong" else "T12.DH.03",
    }
