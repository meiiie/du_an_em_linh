# -*- coding: utf-8 -*-
"""Job kiem_loi_giang (ADR 013, T028, T030): cổng thế giới đóng cho câu gia sư.

Bảng là bảng 6 dòng đã khóa của v0 (data/v0/bang-cong-thuc.json). Bài là y = x³ − 3x² + 2 (hữu tỉ khi cần tử, mẫu).
"""
import io
import json
import os

import pytest

from app import dong_cong_thuc
from app.loi_giang import kiem_loi_giang

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
CT, QT, TBL, TDB = "CONG_THUC_TONG_QUAT", "QUY_TAC_BANG_LOI", "TRICH_BAI_LAM", "TRICH_DE_BAI"
KQ, KPT = "KET_QUA_CU_THE", "KHONG_PHAN_TICH_DUOC"
DAT, SAI, KKD = "DAT", "SAI", "KHONG_KIEM_DUOC"
HAM = "x**3 - 3*x**2 + 2"
HAM_HUU_TI = "(x**2 + 3)/(x - 1)"
BAI_LAM = ["3x^2-6x", "x=0", "x=2", "f'(x) = 3x^2 - 6x", "(uv)' = u'v'"]


def _bang(bo=()):
    with io.open(os.path.join(REPO, "data", "v0", "bang-cong-thuc.json"), encoding="utf-8") as f:
        dong = json.load(f)["formulas"]
    return [{"id": d["ma"], "tieu_de": d["title"], "latex": d["latex"], "phat_bieu": d["noiDung"],
             "trich_dan": {"tai_lieu": "sp-tai-lieu-0001", "doan": "p-1"}} for d in dong if d["ma"] not in bo]


BANG = _bang()


def _chay(cau, bang=BANG, bai_lam=BAI_LAM, ham=HAM):
    return kiem_loi_giang({"cau": cau, "bang_cong_thuc": bang, "bai_lam_hoc_sinh": bai_lam, "ham": ham,
                           "du_kien_bao_ve": [["NGHIEM", "0"], ["NGHIEM", "2"]]})


def _pq(kq):
    return [(p["doan"], p["loai"], p["trang_thai"], p.get("dong_bang")) for p in kq["bieu_thuc"]]


# ------------------------------------------------------------------ công thức tổng quát: khớp bảng có trích dẫn
@pytest.mark.parametrize("cau,doan,dong", [
    ("Theo dòng [1] của bảng: $(x^n)' = n x^{n-1}$.", "(x^n)' = n x^{n-1}", "d-1"),
    ("Dòng [3] viết gọn là $\\left(\\frac{u}{v}\\right)' = \\frac{u'v - uv'}{v^2}$.",
     "\\left(\\frac{u}{v}\\right)' = \\frac{u'v - uv'}{v^2}", "d-3"),
    ("Dòng [3] còn viết là $\\left(\\frac{u}{v}\\right)' = \\frac{u'}{v} - \\frac{uv'}{v^2}$.",
     "\\left(\\frac{u}{v}\\right)' = \\frac{u'}{v} - \\frac{uv'}{v^2}", "d-3"),
    ("$(v+u)' = v' + u'$ cũng chính là dòng [2].", "(v+u)' = v' + u'", "d-2"),
    ("Dòng [3] cho $\\frac{u'v-uv'}{v^2}$.", "\\frac{u'v-uv'}{v^2}", "d-3"),
    ("Em xem dòng [4]: $y' \\ge 0,\\ y' = 0 \\text{ chỉ tại hữu hạn điểm} \\Rightarrow \\text{đồng biến}$.",
     "y' \\ge 0,\\ y' = 0 \\text{ chỉ tại hữu hạn điểm} \\Rightarrow \\text{đồng biến}", "d-4"),
    ("Dòng [5]: \\(+ \\to - : \\text{cực đại}\\)", "+ \\to - : \\text{cực đại}", "d-5"),
], ids=["nguyen-van", "latex-khac", "bien-doi-ve-phai", "doi-cho", "chi-ve-phai", "dinh-li", "ngoac-tron"])
def test_cong_thuc_khop_bang_co_trich_dan_duoc_giu(cau, doan, dong):
    kq = _chay(cau)
    assert kq["bieu_thuc"] == [{"doan": doan, "loai": CT, "trang_thai": DAT, "dong_bang": dong,
                                "tang": {"1": DAT, "2": DAT, "3": DAT}, "cau": 0}]
    assert (kq["cau_sach"], kq["thay_bang_goi_y"]) == (cau, False)


def test_dong_bang_khong_co_trich_dan_thi_cong_thuc_bi_bo():
    bang = [dict(d, trich_dan=None) if d["id"] == "d-2" else d for d in BANG]
    kq = _chay("Theo [2]: $(u+v)' = u' + v'$.", bang=bang)
    assert _pq(kq) == [("(u+v)' = u' + v'", CT, KKD, "d-2")]
    assert (kq["cau_sach"], kq["thay_bang_goi_y"]) == ("", True)


def test_dong_bang_sai_lot_vao_thi_cong_thuc_trung_no_la_sai():
    bang = BANG + [{"id": "d-9", "latex": "(uv)' = u'v'", "phat_bieu": "", "trich_dan": {"tai_lieu": "tl-1"}}]
    assert _pq(_chay("Theo [9]: $(uv)' = u'v'$.", bang=bang)) == [("(uv)' = u'v'", CT, SAI, "d-9")]


def test_dong_diem_toi_han_can_tieu_de_de_may_doc_lai():
    """Dạng ký hiệu của dòng [6] chỉ đọc được khi biết tiêu đề «Điểm tới hạn» (như lúc khóa)."""
    cau = "Theo [6], em tìm chỗ $y'=0 \\text{ hoặc } y' \\text{ không xác định}$."
    assert _pq(_chay(cau))[0][2] == DAT
    khong_tieu_de = [dict(d, tieu_de=None) for d in BANG]
    assert _pq(_chay(cau, bang=khong_tieu_de)) == [("y'=0 \\text{ hoặc } y' \\text{ không xác định}", CT, KKD, "d-6")]


# ------------------------------------------------------------------ ngoài bảng, sai
@pytest.mark.parametrize("cau,doan,trang_thai", [
    ("Đạo hàm của tích: $(uv)' = u'v + uv'$.", "(uv)' = u'v + uv'", KKD),
    ("Với hiệu: $(u - v)' = u' - v'$.", "(u - v)' = u' - v'", KKD),
    ("Em nhớ $(\\sin x)' = \\cos x$.", "(\\sin x)' = \\cos x", KKD),
    ("Định lí: $y' > 0 \\text{ trên } K \\Rightarrow \\text{đồng biến trên } K$.",
     "y' > 0 \\text{ trên } K \\Rightarrow \\text{đồng biến trên } K", KKD),
    ("Quy tắc thương: $(u/v)' = \\frac{u'v + uv'}{v^2}$.", "(u/v)' = \\frac{u'v + uv'}{v^2}", SAI),
    ("Lũy thừa: $(x^n)' = x^{n-1}$.", "(x^n)' = x^{n-1}", SAI),
    ("Nhớ là $y' < 0 \\Rightarrow \\text{đồng biến}$.", "y' < 0 \\Rightarrow \\text{đồng biến}", SAI),
], ids=["tich-dung", "hieu-dung", "sin-dung", "DD1-dung", "thuong-sai", "luy-thua-sai", "don-dieu-nguoc"])
def test_cong_thuc_ngoai_bang_hay_sai_bi_bo(cau, doan, trang_thai):
    kq = _chay(cau)
    assert _pq(kq) == [(doan, CT, trang_thai, None)]
    assert (kq["cau_sach"], kq["thay_bang_goi_y"]) == ("", True)


def test_cong_thuc_sai_kem_phan_vi_du():
    p = _chay("Em dùng $(u+v)' = u'v'$.")["bieu_thuc"][0]
    assert p["trang_thai"] == SAI
    assert set(p["phan_vi_du"]) >= {"u", "v", "x", "hieu_hai_ve"}


# ------------------------------------------------------------------ trích bài làm, trích đề
def test_trich_nguyen_van_dong_bai_lam_co_khung_loi_hoc_sinh_duoc_giu():
    kq = _chay("Em viết $3x^2-6x$, giờ em tìm nghiệm của nó nhé.")
    assert _pq(kq) == [("3x^2-6x", TBL, DAT, None)]
    assert kq["cau_sach"] == "Em viết $3x^2-6x$, giờ em tìm nghiệm của nó nhé."


@pytest.mark.parametrize("cau,doan", [
    ("Ta có $3x^2-6x$.", "3x^2-6x"),                                       # không phải lời học sinh
    ("Em viết $3x^2 - 6x + 0$.", "3x^2 - 6x + 0"),                         # không nguyên văn
    ("Em viết $3x(x-2)$.", "3x(x-2)"),                                     # dạng đã biến đổi
    ("Em viết ở dòng hai là $3x^2-6x$.", "3x^2-6x"),                       # khung không đứng ngay trước
])
def test_trich_bai_lam_khong_nguyen_van_hay_khong_khung_bi_bo(cau, doan):
    assert _pq(_chay(cau)) == [(doan, KQ, KKD, None)]


def test_trich_hai_dong_noi_bang_va():
    kq = _chay("Em viết $x=0$ và $x=2$, em kiểm tra lại nhé.")
    assert _pq(kq) == [("x=0", TBL, DAT, None), ("x=2", TBL, DAT, None)]
    assert kq["thay_bang_goi_y"] is False


@pytest.mark.parametrize("cau,doan,ham", [
    ("Mình bắt đầu từ $y = x^3 - 3x^2 + 2$ nhé.", "y = x^3 - 3x^2 + 2", HAM),
    ("Đề cho $x^{3} - 3x^{2} + 2$.", "x^{3} - 3x^{2} + 2", HAM),
    ("Em nhập $x**3 - 3*x**2 + 2$ vào máy.", "x**3 - 3*x**2 + 2", HAM),
    ("Đề cho $x³ − 3x² + 2$.", "x³ − 3x² + 2", HAM),
    ("Đề bài cho $y = \\frac{x^2 + 3}{x - 1}$.", "y = \\frac{x^2 + 3}{x - 1}", HAM_HUU_TI),
    ("Em xét riêng $x^2 + 3$ trước nhé.", "x^2 + 3", HAM_HUU_TI),
    ("Còn $x - 1$ thì em xét sau.", "x - 1", HAM_HUU_TI),
], ids=["co-y", "ngoac-nhon", "sympy", "unicode", "phan-thuc", "tu", "mau"])
def test_trich_de_sau_chuan_hoa_cach_viet_duoc_giu(cau, doan, ham):
    kq = _chay(cau, ham=ham)
    assert _pq(kq) == [(doan, TDB, DAT, None)]
    assert kq["cau_sach"] == cau


@pytest.mark.parametrize("cau,doan", [
    ("Hàm $2 - 3x^2 + x^3$ của đề.", "2 - 3x^2 + x^3"),                                  # đổi thứ tự hạng tử
    ("Viết lại $x^3 - 3x^2 + 2 = (x - 1)(x^2 - 2x - 2)$.", "x^3 - 3x^2 + 2 = (x - 1)(x^2 - 2x - 2)"),
    ("Hàm là $y = x^2(x - 3) + 2$ của đề.", "y = x^2(x - 3) + 2"),                       # phân tích
    ("Ta có $y' = x^3 - 3x^2 + 2$.", "y' = x^3 - 3x^2 + 2"),                             # vế trái khác y
])
def test_dang_bien_doi_cua_de_khong_phai_trich(cau, doan):
    assert [(d, t) for d, l, t, _ in _pq(_chay(cau)) if l != QT] == [(doan, KKD)]


# ------------------------------------------------------------------ kết quả cụ thể, LaTeX hỏng, toán viết trần
@pytest.mark.parametrize("cau,doan", [
    ("Đạo hàm của hàm này là $y' = 3x^2 - 6x$.", "y' = 3x^2 - 6x"),
    ("Nghiệm là $x = 0$ và $x = 2$.", "x = 0"),
    ("Giá trị cực đại là $y(0) = 2$.", "y(0) = 2"),
    ("Hàm đồng biến trên $(-\\infty; 0)$.", "(-\\infty; 0)"),
    ("Cực tiểu bằng $-2$.", "-2"),
])
def test_ket_qua_cu_the_bi_bo(cau, doan):
    kq = _chay(cau)
    assert (doan, KQ, KKD, None) in _pq(kq)
    assert (kq["cau_sach"], kq["thay_bang_goi_y"]) == ("", True)


@pytest.mark.parametrize("cau,doan", [
    ("Em xem $\\frac{u'v - uv'}{v^2$.", "\\frac{u'v - uv'}{v^2"),
    ("Bình phương: $x^{2$.", "x^{2"),
    ("Dòng [3]: $\\left( \\frac{u}{v} \\right' = \\frac{u'v - uv'}{v^2}$.", "\\left( \\frac{u}{v} \\right' = \\frac{u'v - uv'}{v^2}"),
    ("Ta có $y' = $ rồi xét dấu.", "y' ="),
    ("Em tính $\\foo{x} + 1$.", "\\foo{x} + 1"),
    ("Nghịch đảo $\\frac{1}{x}}$.", "\\frac{1}{x}}"),
    ("Điều kiện $x^2 \\le\\ $.", "x^2 \\le\\"),
    ("Quy tắc $$(u+v)' = u' + v'$.", "(u+v)' = u' + v'"),
    ("Dòng [1]: $(x^n)' = n x^{n-1}.", "(x^n)' = n x^{n-1}."),
], ids=["frac-thieu-ngoac", "mu-thieu-ngoac", "right-khong-ngoac", "cut", "lenh-la", "thua-ngoac", "quan-he-cut",
        "dong-sai-dau", "khong-dong"])
def test_latex_hong_bi_bo(cau, doan):
    kq = _chay(cau)
    assert _pq(kq) == [(doan, KPT, KKD, None)]
    assert (kq["cau_sach"], kq["thay_bang_goi_y"]) == ("", True)


@pytest.mark.parametrize("cau,doan", [
    ("Đạo hàm của tích là (uv)' = u'v' nhé.", ["(uv)' = u'v'"]),
    ("Quy tắc thương: (u/v)' = (u'v - uv')/v^2.", ["(u/v)' = (u'v - uv')/v^2"]),
    ("Em tính được y' = 3x^2 - 6x rồi.", ["y' = 3x^2 - 6x"]),
    ("Dùng 3x^2 cho đạo hàm của x^3.", ["3x^2", "x^3"]),
    ("Ta có \\frac{1}{2} ở đây.", ["\\frac{1}{2}"]),
    ("Vì y'(0) = 0 nên ta xét điểm đó.", ["y'(0) = 0"]),
    ("Biểu thức u' v đi với v bình.", ["u' v"]),
    ("Ta có y′ = 3x² − 6x rồi.", ["y′ = 3x² − 6x"]),
    ("Em xét √x ở đây.", ["√x"]),
    ("Ta xét dấu của 3x(x - 2).", ["3x(x - 2)"]),
    ("Nghiệm x＝２ nhé.", ["x＝２"]),
    ("Mình bắt đầu từ y = x^3 - 3x^2 + 2 nhé.", ["y = x^3 - 3x^2 + 2"]),
], ids=["tich", "thuong", "dao-ham", "hai-doan", "lenh", "gia-tri", "dau-phay-tren", "unicode", "can", "tich-so",
        "toan-kho", "trich-de-viet-tran"])
def test_toan_viet_tran_ngoai_dau_phan_cach_bi_bo(cau, doan):
    kq = _chay(cau)
    assert [d for d, l, _, _ in _pq(kq) if l == KPT] == doan
    assert kq["cau_sach"] == ""


# Ca lọt của lượt rà độc lập PR #151 (HEAD 93ad1b5): job giữ nguyên câu và /v1/filter cũng cho qua.
@pytest.mark.parametrize("cau,doan", [
    ("y bằng 3 x x trừ 6 x.", ["3 x x", "6 x"]),
    ("Ta có 3 x x ‒ 6 x.", ["3 x x ‒ 6 x"]),
    ("Ta có y ꞊ 3 x x ‒ 6 x.", ["y ꞊ 3 x x ‒ 6 x"]),
    ("Ta có x ᐀ 2.", ["x ᐀ 2"]),
    ("Ta có x ⹀ 2.", ["x ⹀ 2"]),
    ("Ta có x゠2.", ["x゠2"]),
    ("Ta có x \x00 2.", ["x \x00 2"]),
    ("Ta có y ‐ 2 ‐ x.", ["y ‐ 2 ‐ x"]),
    ("Ta có x 🟰 2.", ["x 🟰 2"]),
    ("x thuộc ]0, 2[", ["]0, 2["]),
    ("Nó đi xuống trên ]0; 2[.", ["]0; 2["]),
    ("Đỉnh ⟨0; 2⟩, đáy ⟨2; −2⟩.", ["⟨0; 2⟩", "⟨2; −2⟩"]),
    ("Đỉnh 〔0; 2〕 nhé.", ["〔0; 2〕"]),
    ("Giá trị ở đáy là −2.", ["−2"]),
    ("Đáy ở -2.", ["-2"]),
], ids=["nhan-viet-cach", "gach-so", "bang-chu-sk", "gach-doi-canada", "gach-doi", "gach-doi-kana", "ky-tu-nul",
        "gach-noi", "emoji-bang", "khoang-nguoc-phay", "khoang-nguoc", "ngoac-goc", "ngoac-mai-rua", "tru-unicode", "tru"])
def test_ca_lot_ra_doc_lap_bi_bo(cau, doan):
    kq = _chay(cau)
    assert _pq(kq) == [(d, KPT, KKD, None) for d in doan]
    assert len(kq["cac_cau"]) == 1
    assert (kq["cau_sach"], kq["thay_bang_goi_y"]) == ("", True)


def test_khoang_nguoc_khong_bi_tach_cau_o_dau_cham_phay():
    kq = _chay("Khoảng ]0; 2[ là chỗ nó đi xuống. Em xem lại nhé.")
    assert kq["cac_cau"] == [{"cau": "Khoảng ]0; 2[ là chỗ nó đi xuống.", "giu": False},
                             {"cau": "Em xem lại nhé.", "giu": True}]
    assert _pq(kq) == [("Khoảng ]0; 2[ là chỗ nó đi xuống", QT, KKD, None), ("]0; 2[", KPT, KKD, None)]
    assert kq["cau_sach"] == "Em xem lại nhé."


def test_trich_dan_chi_co_khoang_trang_khong_phai_trich_dan():
    bang = [dict(d, trich_dan={"tai_lieu": "  ", "doan": "p-2"}) if d["id"] == "d-2" else d for d in BANG]
    kq = _chay("Theo [2]: $(u+v)' = u' + v'$.", bang=bang)
    assert _pq(kq) == [("(u+v)' = u' + v'", CT, KKD, "d-2")]
    assert kq["bieu_thuc"][0]["ly_do"] == "Khớp dòng d-2, nhưng dòng chưa có trích dẫn tài liệu."
    assert (kq["cau_sach"], kq["thay_bang_goi_y"]) == ("", True)


def test_ky_hieu_dung_rieng_khong_thuoc_loai_nao():
    assert _pq(_chay("Em kiểm tra lại dòng $y'$ đầu tiên nhé.")) == [("y'", KPT, KKD, None)]


@pytest.mark.parametrize("cau", [
    "Em làm tốt lắm, mình sang bước tiếp theo nhé.",
    "Em mở dòng [2] của bảng công thức để xem lại nhé.",
    "Em xét dấu đạo hàm trên từng khoảng nhé.",
    "Em đặt nhân tử chung ra nhé.",
    "Em lập bảng biến thiên nhé.",
    "Em ghi dấu + vào ô đó nhé.",
    "Bước 2 em làm lại nhé.",
    "Em làm tốt lắm — giờ sang bước 3 nhé.",
    "Em xem lại “bảng biến thiên” nhé…",
])
def test_cau_khong_co_toan_giu_nguyen(cau):
    kq = _chay(cau)
    assert (kq["cau_sach"], kq["bieu_thuc"], kq["thay_bang_goi_y"]) == (cau, [], False)


@pytest.mark.parametrize("cau,doan", [
    ("Em làm bước 2 y như bước 1 nhé.", "2 y"),
    ("Em làm tốt lắm 😊", "😊"),
], ids=["so-cach-chu-don", "emoji"])
def test_danh_sach_trang_chon_bo_thua(cau, doan):
    """Đã chọn: chữ số cách một chữ cái ASCII đứng riêng là nhân viết cách, mọi ký tự ngoài danh sách trắng là dấu hiệu
    toán. Câu lời thường như vậy bị bỏ; đổi lại không có dấu nhìn giống toán nào lọt."""
    kq = _chay(cau)
    assert _pq(kq) == [(doan, KPT, KKD, None)]
    assert kq["cau_sach"] == ""


# ------------------------------------------------------------------ quy tắc bằng lời
@pytest.mark.parametrize("cau,dong", [
    ("Đạo hàm của tổng bằng tổng các đạo hàm.", "d-2"),
    ("đạo hàm của tổng bằng tổng các đạo hàm", "d-2"),
    ("Đạo hàm của tổng bằng tổng các đạo hàm [2].", "d-2"),
    ("Hằng số có đạo hàm bằng 0.", "d-1"),
    ("Với thương, tử là u'v trừ uv', mẫu là v bình.", "d-3"),
    ("Đạo hàm bằng 0 mà không đổi dấu thì chưa phải cực trị.", "d-5"),
    ("Trên một khoảng, nếu y' ≥ 0 và y' = 0 chỉ tại hữu hạn điểm thì hàm số đồng biến trên khoảng đó.", "d-4"),
    ("Nếu đạo hàm đổi dấu từ dương sang âm khi x đi qua x0 thì x0 là điểm cực đại.", "d-5"),
], ids=["nguyen-van", "chu-thuong", "co-dan", "cau-hai-cua-dong", "co-toan-tran", "CT2", "doc-lai-DD2", "doc-lai-CT1"])
def test_quy_tac_bang_loi_khop_dong_bang_duoc_giu(cau, dong):
    kq = _chay(cau)
    assert [(p["loai"], p["trang_thai"], p.get("dong_bang")) for p in kq["bieu_thuc"]] == [(QT, DAT, dong)]
    assert kq["cau_sach"] == cau


@pytest.mark.parametrize("cau,trang_thai", [
    ("Đạo hàm của tích bằng đạo hàm u nhân v cộng u nhân đạo hàm v.", KKD),   # đúng, ngoài bảng
    ("Nếu đạo hàm dương trên một khoảng thì hàm số đồng biến trên khoảng đó.", KKD),  # DD1, bảng chỉ có DD2
    ("Đạo hàm của tích bằng tích các đạo hàm.", KKD),                         # sai, máy không đọc được nghĩa lời
    ("Nếu đạo hàm âm thì hàm số đồng biến.", SAI),                              # sai, có phản ví dụ
    ("Đạo hàm của tích: tích các đạo hàm.", KKD),
    ("dao ham cua tich bang tich cac dao ham", KKD),
    ("Đạo hàm của tích b​ằng tích các đạo hàm.", KKD),                     # ký tự định dạng chèn giữa từ
    ("Luỹ thừa là thế.", KKD),                                                  # dấu thanh kiểu mới
    ("Nếu y' ≥ 0 trên (0; 2) và y' = 0 chỉ tại hữu hạn điểm thì hàm số đồng biến trên (0; 2).", KKD),
], ids=["tich-dung", "DD1", "tich-sai", "nguoc", "hai-cham", "khong-dau", "zwsp", "dau-thanh", "khoang-cu-the"])
def test_quy_tac_bang_loi_ngoai_bang_hay_sai_bo_ca_cau(cau, trang_thai):
    kq = _chay(cau)
    assert kq["bieu_thuc"][0]["loai"] == QT and kq["bieu_thuc"][0]["trang_thai"] == trang_thai
    assert (kq["cau_sach"], kq["thay_bang_goi_y"]) == ("", True)


def test_cau_ke_thua_quy_tac_bi_bo_cau_dau_khop_bang_con_lai():
    kq = _chay("Đạo hàm của tổng bằng tổng các đạo hàm. Với tích cũng vậy.")
    assert [(p["doan"], p["trang_thai"], p["cau"]) for p in kq["bieu_thuc"]] == [
        ("Đạo hàm của tổng bằng tổng các đạo hàm", DAT, 0), ("Với tích cũng vậy", KKD, 1)]
    assert kq["cac_cau"] == [{"cau": "Đạo hàm của tổng bằng tổng các đạo hàm.", "giu": True},
                             {"cau": "Với tích cũng vậy.", "giu": False}]
    assert (kq["cau_sach"], kq["thay_bang_goi_y"]) == ("Đạo hàm của tổng bằng tổng các đạo hàm.", False)


# ------------------------------------------------------------------ đáp án cài lậu
@pytest.mark.parametrize("cau", [
    "x = 2 là điểm cực đại.",
    "Điểm $x = 2$ là điểm cực đại.",
    "Em viết $x = 2$ là điểm cực đại.",     # trích đúng dòng học sinh, nhưng câu là ứng viên quy tắc
    "Cực đại bằng hai.",
    "Đạo hàm tại đó bằng hai nhé.",
    "Hàm số đạt cực đại tại $x_0$.",
    "Hàm số nghịch biến trên khoảng (0; 2).",
    "Cực đại tại 0, cực tiểu tại 2.",
])
def test_dap_an_cai_trong_cau_bi_bo(cau):
    kq = _chay(cau)
    assert (kq["cau_sach"], kq["thay_bang_goi_y"]) == ("", True)
    assert any(p["trang_thai"] != DAT for p in kq["bieu_thuc"])


def test_dao_ham_cua_bai_chi_hien_khi_trich_nguyen_van_loi_hoc_sinh():
    assert _pq(_chay("Ta có $f'(x)=3x^2-6x$.")) == [("f'(x)=3x^2-6x", KQ, KKD, None)]
    assert _pq(_chay("Em viết $f'(x) = 3x^2 - 6x$ rồi.")) == [("f'(x) = 3x^2 - 6x", TBL, DAT, None)]
    # thêm «f'(x) =» vào dòng học sinh chỉ viết vế phải là tự tính hộ, không phải trích
    assert _pq(_chay("Em viết $f'(x)=3x^2-6x$.", bai_lam=["3x^2-6x"])) == [("f'(x)=3x^2-6x", KQ, KKD, None)]


def test_cong_thuc_sai_do_hoc_sinh_viet_khong_duoc_trich_lai():
    """Công thức có dạng quy tắc chỉ DAT qua bảng, kể cả khi trùng dòng học sinh: không xác nhận một quy tắc sai."""
    kq = _chay("Em viết $(uv)' = u'v'$, đúng rồi.")
    assert _pq(kq) == [("(uv)' = u'v'", CT, SAI, None)]
    assert kq["cau_sach"] == ""


# ------------------------------------------------------------------ đơn vị câu, câu mất nghĩa
def test_cau_co_doan_bi_bo_bi_bo_ca_cau_cau_khac_giu_nguyen():
    kq = _chay("Em làm tốt lắm! Ta có $y' = 3x^2 - 6x$. Theo [2]: $(u+v)' = u' + v'$.\nEm áp dụng cho từng hạng tử nhé.")
    assert [c["giu"] for c in kq["cac_cau"]] == [True, False, True, True]
    assert kq["cau_sach"] == "Em làm tốt lắm! Theo [2]: $(u+v)' = u' + v'$.\nEm áp dụng cho từng hạng tử nhé."
    assert kq["thay_bang_goi_y"] is False


def test_cong_thuc_dung_chung_cau_voi_ket_qua_cu_the_cung_bi_bo():
    kq = _chay("Theo [1], $(x^n)' = nx^{n-1}$, nên $(x^3)' = 3x^2$.")
    assert [(p["dong_bang"] if p["trang_thai"] == DAT else p["trang_thai"]) for p in kq["bieu_thuc"]] == ["d-1", KKD]
    assert kq["cau_sach"] == ""


def test_dau_phan_cach_khong_dong_bo_tu_do_toi_het():
    kq = _chay("Em xem lại nhé. Dòng [1]: $(x^n)' = n x^{n-1}. Rồi làm tiếp.")
    assert kq["cau_sach"] == "Em xem lại nhé."
    assert _pq(kq) == [("(x^n)' = n x^{n-1}. Rồi làm tiếp.", KPT, KKD, None)]


def test_cau_con_lai_chi_co_tu_noi_thi_thay_bang_goi_y():
    kq = _chay("Ta có $y' = 3x^2 - 6x$. Vậy nhé.")
    assert [c["giu"] for c in kq["cac_cau"]] == [False, True]
    assert (kq["cau_sach"], kq["thay_bang_goi_y"]) == ("", True)


# ------------------------------------------------------------------ đóng mặc định
@pytest.mark.parametrize("payload,loi", [
    ({"cau": None}, "DAU_VAO_KHONG_HOP_LE"),
    ({"cau": "Em xem lại nhé.", "bai_lam_hoc_sinh": [3]}, "DAU_VAO_KHONG_HOP_LE"),
    ({"cau": "Em xem lại nhé.", "bang_cong_thuc": "d-1"}, "DAU_VAO_KHONG_HOP_LE"),
    ({"cau": "a" * 4001}, "QUA_GIOI_HAN"),
    ({"cau": " ".join(["$x$"] * 41)}, "QUA_GIOI_HAN"),
    ({"cau": "Em xem lại nhé.", "bai_lam_hoc_sinh": ["x"] * 81}, "QUA_GIOI_HAN"),
], ids=["cau-none", "dong-hs-khong-phai-chuoi", "bang-khong-phai-danh-sach", "cau-qua-dai", "qua-nhieu-doan", "qua-nhieu-dong"])
def test_dau_vao_sai_kieu_hay_qua_gioi_han_khong_giu_gi(payload, loi):
    assert kiem_loi_giang(payload) == {"cau_sach": "", "thay_bang_goi_y": True, "bieu_thuc": [], "cac_cau": [], "loi": loi}


def test_loi_ben_trong_khong_giu_gi(monkeypatch):
    def hong(*_a, **_k):
        raise RuntimeError("bộ đọc hỏng")

    monkeypatch.setattr(dong_cong_thuc, "_doc_dong", hong)
    assert _chay("Em xem lại nhé.") == {"cau_sach": "", "thay_bang_goi_y": True, "bieu_thuc": [], "cac_cau": [],
                                        "loi": "LOI_KIEM_TRA"}


@pytest.mark.skipif(os.name == "nt", reason="sandbox dùng preexec_fn và resource: chỉ chạy trên Linux (CI)")
def test_router_kiem_loi_giang():
    from fastapi.testclient import TestClient

    from app.main import app

    r = TestClient(app).post("/v1/kiem-loi-giang", json={
        "cau": "Theo [2]: $(u+v)' = u' + v'$. Ta có $y' = 3x^2 - 6x$.", "bang_cong_thuc": BANG,
        "bai_lam_hoc_sinh": BAI_LAM, "du_kien_bao_ve": [], "ham": HAM, "timeout_s": 12})
    assert r.status_code == 200
    kq = r.json()
    assert (kq["cau_sach"], kq["thay_bang_goi_y"]) == ("Theo [2]: $(u+v)' = u' + v'$.", False)
    assert [(p["loai"], p["trang_thai"]) for p in kq["bieu_thuc"]] == [(CT, DAT), (KQ, KKD)]
