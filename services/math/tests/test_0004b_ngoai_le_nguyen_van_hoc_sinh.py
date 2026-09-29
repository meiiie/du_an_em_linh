# -*- coding: utf-8 -*-
"""Bản vá 0004b (Kiểm định, 29/09; luật No chốt) — ngoại lệ "gia sư nhắc NGUYÊN VĂN câu của học sinh" cho luật y' (0004).

Trường mới của /v1/filter (job "filter"): `dong_hoc_sinh: list[str]` (tùy chọn, mặc định rỗng) = các dòng bài nộp của học
sinh (submission_steps.latex của bài nộp gần nhất; web lấy phía server, không từ câu mô hình).
Một đoạn biểu thức đáng lẽ bị chặn vì YPHAY được qua nếu trùng nguyên văn một biểu thức TRỌN VẸN học sinh viết (cả dòng,
hoặc nguyên một vế của '='). So chuỗi: chỉ bỏ qua khoảng trắng, cách viết LaTeX, ký hiệu nhân, ký hiệu mũ — KHÔNG SymPy.
Biến đổi đại số (rút gọn, khai triển, phân tích, đổi thứ tự, chia hằng số) vẫn chặn; dẫn một phần (chỉ tử số) vẫn chặn;
không có dòng học sinh / không khớp -> chặn như 0004. Các luật khác vẫn chạy trên cả câu.
"""
import os

import pytest

import app.leakfilter as lf
from app.leakfilter import loc_ban_nhap
from app.machine import bai_lam_may, su_kien_bao_ve

HS = "em chưa hiểu bước này"
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
BAC_BA = "x**3 - 6*x**2 + 9*x + 2"          # y' = 3x² − 12x + 9
TH02 = "(x**2 - 3*x + 6)/(x - 1)"           # DH12-03-TH-02: y' = (x² − 2x − 3)/(x − 1)²
DE_TH02 = ("Cho hàm số y = (x² − 3x + 6)/(x − 1) có đạo hàm y' = (x² − 2x − 3)/(x − 1)². Không cần tính lại đạo hàm, "
           "hãy tìm các khoảng đồng biến, nghịch biến của hàm số.")
H12 = "Phương trình 3x^2 - 12x + 9 = 0 em chia hai vế cho 3 cho gọn."


def sk(ham):
    return su_kien_bao_ve(bai_lam_may(ham))


def qua(cau, s, dong, hs=HS):
    return loc_ban_nhap(cau, s, hs, dong_hoc_sinh=dong)["cho_phep"]


# ------------------------------------------------------------------ H12 hai chiều
@pytest.mark.parametrize("dong", [
    ["3x^2 - 12x + 9 = 0"],
    ["3x² − 12x + 9 = 0"],
    ["3x^{2}-12x+9=0"],                               # MathLive
    ["3\\cdot x^{2}-12\\cdot x+9=0"],
    ["$3x^2-12x+9=0$"],
    ["D = \\mathbb{R}", "y'=3x^{2}-12x+9"],           # dòng y' của học sinh: vế phải trọn vẹn
    ["3*x**2 - 12*x + 9 = 0"],
], ids=["ascii", "unicode", "latex_mathlive", "latex_cdot", "latex_dola", "dong_y_phay", "dau_sao"])
def test_h12_hoc_sinh_da_viet_thi_qua(dong):
    assert qua(H12, sk(BAC_BA), dong)


@pytest.mark.parametrize("dong", [
    None,
    [],
    ["x = 1", "x = 3"],                               # học sinh chưa viết dòng đó
    ["3(x-1)(x-3) = 0"],                              # dạng khác
    ["3(x − 1)(x − 3) = 0"],
    ["x^2 - 4x + 3 = 0"],                             # chia hằng số
    ["9 - 12x + 3x^2 = 0"],                           # đổi thứ tự
    ["3(x^2-4x+3)=0"],
    ["-12x + 3x^2 + 9 = 0"],
], ids=["khong_truong", "rong", "chua_viet", "phan_tich", "phan_tich_unicode", "chia_3", "doi_thu_tu", "dat_nhan_tu",
        "doi_thu_tu_2"])
def test_h12_hoc_sinh_chua_viet_hoac_dang_khac_thi_chan(dong):
    assert not qua(H12, sk(BAC_BA), dong)


# ------------------------------------------------------------------ giả mạo: học sinh có nguyên văn, gia sư viết dạng khác
@pytest.mark.parametrize("cau", [
    "Em sẽ được y' = 3(x − 1)(x − 3).",
    "Đạo hàm là 9 − 12x + 3x², em xem lại.",
    "Em viết y' = 3(x^2 - 4x + 3) cho gọn.",
    "Em viết y' = 3x^2 - 12x + 9, tức là 3(x - 1)(x - 3).",   # một đoạn khớp, đoạn kia không -> chặn
])
def test_gia_mao_hoc_sinh_co_nguyen_van_gia_su_bien_doi_van_chan(cau):
    assert not qua(cau, sk(BAC_BA), ["y' = 3x^2 - 12x + 9", "3x^2-12x+9=0"])


@pytest.mark.parametrize("cau", [
    "Em đã có y' = (x − 3)(x + 1)/(x − 1)².",
    "Tử số là x² − 2x − 3, em xét dấu nhé.",          # dẫn MỘT PHẦN (tử số) phân thức học sinh viết -> chặn
    "Em viết y' = (x^2-2x-3)/(x^2-2x+1).",            # mẫu khai triển
])
def test_th02_hoc_sinh_viet_phan_thuc_gia_su_bien_doi_hoac_mot_phan_van_chan(cau):
    assert not qua(cau, sk(TH02), ["y'=\\frac{x^{2}-2x-3}{\\left(x-1\\right)^{2}}"])


@pytest.mark.parametrize("cau", [
    "Em đã viết $y' = \\frac{x^2 - 2x - 3}{(x - 1)^2}$, giờ tìm chỗ y' bằng 0.",
    "Em đã viết y' = (x² − 2x − 3)/(x − 1)², giờ tìm chỗ y' bằng 0.",
    "Em đã viết y' = (x^2-2x-3)/((x-1)^2), giờ tìm chỗ y' bằng 0.",
])
def test_th02_nhac_nguyen_van_phan_thuc_hoc_sinh_qua(cau):
    assert qua(cau, sk(TH02), ["y'=\\frac{x^{2}-2x-3}{\\left(x-1\\right)^{2}}"])
    assert not qua(cau, sk(TH02), [])


def test_tu_so_qua_khi_hoc_sinh_viet_tron_dong_tu_so():
    cau = "Tử số là x² − 2x − 3, em xét dấu nhé."
    assert qua(cau, sk(TH02), ["y'=\\frac{x^{2}-2x-3}{\\left(x-1\\right)^{2}}", "x^{2}-2x-3=0"])


# ------------------------------------------------------------------ y' sai của học sinh, dẫn nguyên văn -> qua
def test_hoc_sinh_viet_y_phay_sai_gia_su_dan_nguyen_van_qua():
    cau = "Em viết y' = 3x² − 12x + 8, em kiểm lại hệ số tự do."
    assert qua(cau, sk(BAC_BA), ["y'=3x^{2}-12x+8"])
    assert qua(cau, sk(BAC_BA), None)                 # 0004: biểu thức sai vốn không bị luật y' chặn


# ------------------------------------------------------------------ ngoại lệ chỉ gỡ đúng lần khớp YPHAY
@pytest.mark.parametrize("cau", [
    "Em có 3x^2 - 12x + 9 = 0 nên x = 1 và x = 3.",
    "Phương trình 3x^2 - 12x + 9 = 0 có hai nghiệm 1 và 3.",
    "3x^2 - 12x + 9 = 0, hàm đồng biến trên (−∞; 1) và (3; +∞).",
])
def test_luat_khac_van_chay_tren_ca_cau(cau):
    assert not qua(cau, sk(BAC_BA), ["3x^2 - 12x + 9 = 0", "x = 1", "x = 3"])


def test_khung_ngan_yphay_de_va_dong_hoc_sinh():
    s = sk(TH02) + lf.su_kien_de_cho(DE_TH02, "B.DH.XETDAU")
    assert any(a == "YPHAY_DE" for a, _b in s)
    cau = "Em đã có y' = (x − 3)(x + 1)/(x − 1)²."
    assert not qua(cau, s, [])                        # biến đổi so với đề -> chặn (0004)
    assert qua(cau, s, ["y' = (x-3)(x+1)/(x-1)^2"])
    assert qua("Em đã có $y' = \\frac{(x-3)(x+1)}{(x-1)^2}$.", s,
               ["y'=\\frac{\\left(x-3\\right)\\left(x+1\\right)}{\\left(x-1\\right)^{2}}"])
    assert not qua(cau, s, ["y'=\\frac{x^{2}-2x-3}{\\left(x-1\\right)^{2}}"])
    # quyết định: a·b/c khác \\frac{a·b}{c} = (a·b)/c về chuỗi (không nằm trong danh sách ký hiệu được bỏ qua) -> chặn
    assert not qua(cau, s, ["y'=\\frac{\\left(x-3\\right)\\left(x+1\\right)}{\\left(x-1\\right)^{2}}"])


# ------------------------------------------------------------------ nguồn dòng học sinh
def test_cau_chat_cua_hoc_sinh_khong_phai_dong_bai_lam():
    # câu chat (cau_hoc_sinh) chưa qua bộ chấm -> không mở ngoại lệ
    assert not qua(H12, sk(BAC_BA), [], hs="3x^2 - 12x + 9 = 0 đúng không ạ?")


@pytest.mark.parametrize("dong", ["3x^2 - 12x + 9 = 0", {"a": "3x^2-12x+9=0"}, [1, 2], [None], ["3x^2-12x+9=0" + " " * 500],
                                  ["__import__('os').system('id')"], [""] * 5])
def test_truong_sai_kieu_hoac_la_khong_mo_ngoai_le_khong_loi(dong):
    q = loc_ban_nhap(H12, sk(BAC_BA), HS, dong_hoc_sinh=dong)
    assert q["cho_phep"] is False and q["loi"] is False


def test_khong_truong_moi_hanh_vi_nhu_0004():
    cac_cau = [H12, "Em sẽ được y' = 3x² − 12x + 9.", "Đạo hàm là 3x² − 12x + 9, em chép lại nhé.",
               "Em tính y' rồi giải y' = 0 nhé.", "Em viết y' = 3x² − 12x + 8, em kiểm lại hệ số tự do.",
               "Em xét dấu y' trên từng khoảng."]
    for c in cac_cau:
        a = loc_ban_nhap(c, sk(BAC_BA), HS)
        assert loc_ban_nhap(c, sk(BAC_BA), HS, dong_hoc_sinh=None) == a
        assert loc_ban_nhap(c, sk(BAC_BA), HS, dong_hoc_sinh=[]) == a
    assert [loc_ban_nhap(c, sk(BAC_BA), HS)["cho_phep"] for c in cac_cau] == [False, False, False, True, True, True]


def test_job_filter_truyen_dong_hoc_sinh():
    from app.job_runner import _run
    p = {"ban_nhap": H12, "su_kien": sk(BAC_BA), "cau_hoc_sinh": HS}
    assert _run("filter", p)["cho_phep"] is False
    assert _run("filter", dict(p, dong_hoc_sinh=["3x^{2}-12x+9=0"]))["cho_phep"] is True
    assert _run("filter", dict(p, dong_hoc_sinh=["3(x-1)(x-3)=0"]))["cho_phep"] is False


def test_web_gia_su_gui_dong_bai_nop_cho_bo_loc():
    p = os.path.join(ROOT, "apps", "web", "lib", "gia-su-luot.ts")
    if not os.path.exists(p):
        pytest.skip("không có apps/web")
    t = open(p, encoding="utf-8").read()
    assert "submissionSteps.latex" in t and "eq(submissionSteps.submissionId, latestSub[0].id)" in t
    assert t.count("dong_hoc_sinh: dongHocSinh") == 2


# ------------------------------------------------------------------ chuẩn hóa ký hiệu (không đại số)
@pytest.mark.parametrize("a,b", [
    ("3x^2 - 12x + 9", "3x² − 12x + 9"),
    ("3x^{2}-12x+9", "3\\cdot x^2 - 12 \\times x + 9"),
    ("3·x^2-12x+9", "3*x^2-12*x+9"),
    ("\\frac{x^2-2x-3}{\\left(x-1\\right)^2}", "(x^2-2x-3)/(x-1)^2"),
    ("\\dfrac{x^2-2x-3}{(x-1)^{2}}", "(x²−2x−3)/((x−1)²)"),
    ("\\frac{-2}{(x-1)^2}", "(-2)/(x-1)^2"),
    ("$3x^2-12x+9$", "3x^2-12x+9."),
])
def test_chuan_nguyen_van_bang_nhau(a, b):
    assert lf._chuan_nguyen_van(a) == lf._chuan_nguyen_van(b)


@pytest.mark.parametrize("a,b", [
    ("3x^2-12x+9", "9-12x+3x^2"),
    ("3x^2-12x+9", "3(x-1)(x-3)"),
    ("3x^2-12x+9", "x^2-4x+3"),
    ("(x^2-2x-3)/(x-1)^2", "(x-3)(x+1)/(x-1)^2"),
    ("(x^2-2x-3)/(x-1)^2", "x^2-2x-3/(x-1)^2"),
    ("2·3x", "23x"),
    ("(x-1)^2", "x-1^2"),
])
def test_chuan_nguyen_van_khac_nhau(a, b):
    assert lf._chuan_nguyen_van(a) != lf._chuan_nguyen_van(b)


# ------------------------------------------------------------------ sửa kèm: hai lỗ của 0004 (đoạn bị bỏ qua cả đoạn)
@pytest.mark.parametrize("ham,cau", [
    (TH02, "$y' = \\frac{x^{2}-2x-3}{(x-1)^{2}}$"),                 # ^{2} trong \frac (LaTeX kiểu MathLive)
    (TH02, "Đạo hàm là $\\dfrac{x^{2}-2x-3}{\\left(x-1\\right)^{2}}$."),
    (BAC_BA, "Em sẽ được y' = 3x² − 12x + 9 (em chép lại nhé)."),     # ngoặc lệch do chữ chen giữa
    (BAC_BA, "Em viết (đạo hàm 3x² − 12x + 9) vào dòng 1."),
])
def test_sua_kem_lo_0004_bi_chan(ham, cau):
    assert not qua(cau, sk(ham), None)
    assert not qua(cau, sk(ham), [])


def test_sua_kem_van_mo_ngoai_le_nguyen_van():
    assert qua("$y' = \\frac{x^{2}-2x-3}{(x-1)^{2}}$", sk(TH02), ["y'=\\frac{x^{2}-2x-3}{\\left(x-1\\right)^{2}}"])
    assert qua("Em đã viết y' = 3x² − 12x + 9 (dòng 1), giờ giải y' = 0.", sk(BAC_BA), ["y'=3x^{2}-12x+9"])


@pytest.mark.parametrize("cau", ["Em xét dấu (theo bảng) nhé.", "Em tính y' (nhớ quy tắc đạo hàm) rồi xét dấu.",
                                 "Em giải phương trình y' = 0 (bước 3) trước."])
def test_sua_kem_cau_hop_le_van_qua(cau):
    assert qua(cau, sk(BAC_BA), None)
