# -*- coding: utf-8 -*-
"""Bản vá 0004 (Kiểm định, 29/09) — bộ lọc lộ đáp án (app/leakfilter.py): câu gợi ý viết ra biểu thức y'.

thiet-ke-ai-v0 mục 24: câu gia sư / câu gợi ý không bao giờ viết ra biểu thức của y' (hay tử số của y', dạng rút gọn,
phân tích nhân tử) do tự tính. Bộ lọc so bằng SymPy với sự kiện YPHAY (đạo hàm thật, su_kien_bao_ve tính lại từ hàm):
  - biểu thức (có x, hoặc đứng sau "y' =", "đạo hàm là") mà cancel(bt − y') = 0 -> chặn;
  - biểu thức sau "tử số (của y') là / =" mà bt / tử(y') là hằng số dương -> chặn ("Tử số của y' là −2").
Quyết định 0004: biểu thức SAI (khác đạo hàm thật) không bị luật này chặn (không lộ đáp án; việc chấm sai là của bộ chấm).
Ngoại lệ (Sư phạm + AI, mục 24): bài khung ngắn có buoc_bat_dau B.DH.NGHIEM / B.DH.XETDAU -> y' là dữ kiện đề cho
(sự kiện YPHAY_DE, NGUYÊN VĂN dòng y' của đề): nhắc lại nguyên văn thì qua, viết dạng rút gọn / biến đổi thì vẫn chặn.
32 câu lộ cài sẵn của Sư phạm (supham/thang-goi-y-mau/_kiem/kiem_thang.py TU_KIEM): 3 câu y' biểu thức + 1 câu tử số
còn lọt sau 0003 -> nay bị chặn.
"""
import json
import os

import pytest

import app.leakfilter as lf
from app.leakfilter import loc_ban_nhap
from app.machine import bai_lam_may, su_kien_bao_ve


def su_kien_de_cho(de_bai, buoc_bat_dau):
    # gọi qua module để trên nền chưa vá test ĐỎ từng ca (AttributeError) thay vì lỗi thu thập
    return lf.su_kien_de_cho(de_bai, buoc_bat_dau)

HS = "em chưa hiểu bước này"
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))


def sk(ham):
    return su_kien_bao_ve(bai_lam_may(ham))


def chan(cau, s, hs=HS):
    return not loc_ban_nhap(cau, s, hs)["cho_phep"]


BAC_BA = "x**3 - 6*x**2 + 9*x + 2"          # y' = 3x² − 12x + 9 (TU_KIEM bac_ba (1, -6, 9, 2))
TRUNG_PHUONG = "x**4 - 2*x**2"              # y' = 4x³ − 4x (TU_KIEM trung_phuong (1, -2, 0))
HUU_TI = "(1*x + (1))/(1*x + (-1))"         # y' = −2/(x − 1)² (TU_KIEM huu_ti (1, 1, 1, -1), fstr của kiem_thang)
TH02 = "(x**2 - 3*x + 6)/(x - 1)"           # DH12-03-TH-02: y' = (x² − 2x − 3)/(x − 1)²
DE_TH02 = ("Cho hàm số y = (x² − 3x + 6)/(x − 1) có đạo hàm y' = (x² − 2x − 3)/(x − 1)². Không cần tính lại đạo hàm, "
           "hãy tìm các khoảng đồng biến, nghịch biến của hàm số.")


# ------------------------------------------------------------------ sự kiện YPHAY
def _yp(ham):
    from sympy import Symbol, sympify
    v = [b for a, b in sk(ham) if a == "YPHAY"]
    assert len(v) == 1
    return sympify(v[0], locals={"x": Symbol("x")})


def test_su_kien_co_yphay_tinh_tu_ham():
    from sympy import Symbol, expand
    x = Symbol("x")
    assert expand(_yp(BAC_BA) - (3 * x**2 - 12 * x + 9)) == 0
    assert expand(_yp(TRUNG_PHUONG) - (4 * x**3 - 4 * x)) == 0


def test_yphay_khong_tin_dao_ham_trong_bai_lam():
    """Bài ví dụ cổng DH12-DEMO-CHAN-01 sửa dao_ham thành 3x: YPHAY vẫn là đạo hàm thật 2x."""
    bl = dict(bai_lam_may("x**2"))
    bl["dao_ham"] = "3*x"
    assert ["YPHAY", "2*x"] in su_kien_bao_ve(bl)


# ------------------------------------------------------------------ 4 câu của TU_KIEM còn lọt sau 0003
@pytest.mark.parametrize("ham,cau", [
    (BAC_BA, "Em sẽ được y' = 3x² − 12x + 9."),
    (BAC_BA, "Đạo hàm là 3x² − 12x + 9, em chép lại nhé."),
    (TRUNG_PHUONG, "Đạo hàm y' = 4x³ − 4x."),
    (HUU_TI, "Tử số của y' là −2."),
], ids=["bac_ba_y_phay", "bac_ba_dao_ham_la", "trung_phuong", "huu_ti_tu_so"])
def test_tu_kiem_y_phay_bi_chan(ham, cau):
    assert chan(cau, sk(ham))
    assert loc_ban_nhap(cau, sk(ham), None)["ly_do"] == "LO_DAP_AN"


# ------------------------------------------------------------------ dạng rút gọn / nhân tử / LaTeX / f'(x) vẫn chặn
@pytest.mark.parametrize("ham,cau", [
    (BAC_BA, "y' = 3(x − 1)(x − 3) nên em giải tiếp."),
    (BAC_BA, "Ta có $y' = 3x^{2} - 12x + 9$."),
    (BAC_BA, "f'(x) = 3x^2 - 12x + 9"),
    (BAC_BA, "Em được 3(x − 3)(x − 1) rồi cho bằng 0."),
    (TRUNG_PHUONG, "y' = 4x(x − 1)(x + 1)"),
    (TRUNG_PHUONG, "Đạo hàm bằng 4x(x² − 1)."),
    (HUU_TI, "y' = \\dfrac{-2}{(x-1)^2}"),
    (HUU_TI, "Đạo hàm là −2/(x − 1)²."),
    (HUU_TI, "Tử số của đạo hàm bằng −2, mẫu là (x − 1)²."),
    (TH02, "Tử số của y' là (x − 3)(x + 1)."),
    (TH02, "Tử số của y' là 2x² − 4x − 6."),   # nhân hằng số dương: cùng dấu, vẫn lộ
])
def test_dang_bien_doi_bi_chan(ham, cau):
    assert chan(cau, sk(ham))


# ------------------------------------------------------------------ không chặn nhầm
@pytest.mark.parametrize("ham,cau", [
    (BAC_BA, "Em xét dấu y' trên từng khoảng rồi đọc bảng."),
    (BAC_BA, "Cho hàm số y = x³ − 6x² + 9x + 2, em tính y' trước."),
    (BAC_BA, "Giải phương trình y' = 0 rồi xét dấu."),
    (BAC_BA, "Nhớ (x^n)' = n·x^(n-1), đạo hàm của tổng bằng tổng các đạo hàm, hằng số có đạo hàm 0."),
    (HUU_TI, "Nhớ (u/v)' = (u'v − uv')/v², rồi rút gọn tử số; mẫu số giữ nguyên dạng bình phương."),
    (HUU_TI, "Tử số của y' là một hằng số, em xét dấu của nó."),
    (HUU_TI, "Tử số của y' bằng 0 thì sao?"),
    ("x**2", "Thử lần lượt các giá trị x nằm hẳn bên trong mỗi khoảng, thay vào y' để lấy dấu."),
])
def test_cau_goi_y_hop_le_qua(ham, cau):
    assert not chan(cau, sk(ham))


@pytest.mark.parametrize("ham,cau", [
    (BAC_BA, "Em viết y' = 3x² − 12x + 8 à? Em kiểm lại hạng tử cuối."),
    (BAC_BA, "y' = 3x² + 12x + 9"),
    (HUU_TI, "Tử số của y' là 2."),
    (TH02, "Tử số của y' là x² − 2x + 3."),
])
def test_quyet_dinh_bieu_thuc_sai_khong_chan(ham, cau):
    """Quyết định 0004: biểu thức SAI không lộ đáp án nên luật y' không chặn (ghi rõ trong README bản vá)."""
    assert not chan(cau, sk(ham))


# ------------------------------------------------------------------ ngoại lệ khung ngắn: DH12-03-TH-02, hai chiều
def test_su_kien_de_cho_chi_khi_bat_dau_nghiem_xetdau():
    assert su_kien_de_cho(DE_TH02, "B.DH.NGHIEM") == [["YPHAY_DE", "(x² − 2x − 3)/(x − 1)²"]]
    assert su_kien_de_cho(DE_TH02, "B.DH.XETDAU") == [["YPHAY_DE", "(x² − 2x − 3)/(x − 1)²"]]
    for b in (None, "", "B.DH.TXD", "B.DH.DAOHAM", "B.DH.KETLUAN"):
        assert su_kien_de_cho(DE_TH02, b) == []
    assert su_kien_de_cho("Tìm các khoảng đơn điệu của hàm số y = x³ − 3x.", "B.DH.NGHIEM") == []


NGUYEN_VAN = [
    "Em xét dấu tử số của y′ đề cho: y′ = (x² − 2x − 3)/(x − 1)².",
    "Đề cho y' = (x^2 - 2x - 3)/(x - 1)^2, em cho tử bằng 0.",
    "$y' = \\dfrac{x^2-2x-3}{(x-1)^2}$, em xét dấu tử số.",
    "Tử số của y' là x² − 2x − 3 (như đề viết), em giải nó bằng 0.",
]
BIEN_DOI = [
    "y' = (x − 3)(x + 1)/(x − 1)²",
    "Tử số của y' là (x − 3)(x + 1).",
    "y' = (x² − 2x − 3)/(x² − 2x + 1)",
    "Rút gọn: y' = (x + 1)(x − 3)/(x − 1)².",
]


@pytest.mark.parametrize("cau", NGUYEN_VAN)
def test_th02_nhac_nguyen_van_y_phay_de_cho_qua(cau):
    s = sk(TH02) + su_kien_de_cho(DE_TH02, "B.DH.NGHIEM")
    assert not chan(cau, s)


@pytest.mark.parametrize("cau", BIEN_DOI)
def test_th02_rut_gon_bien_doi_van_chan(cau):
    s = sk(TH02) + su_kien_de_cho(DE_TH02, "B.DH.NGHIEM")
    assert chan(cau, s)


@pytest.mark.parametrize("cau", NGUYEN_VAN[:2])
def test_th02_khong_phai_khung_ngan_thi_nguyen_van_cung_chan(cau):
    """Không có YPHAY_DE (bài đầy đủ 5 bước / buoc_bat_dau khác NGHIEM, XETDAU): y' do gia sư tự viết -> chặn."""
    assert chan(cau, sk(TH02))
    assert chan(cau, sk(TH02) + su_kien_de_cho(DE_TH02, "B.DH.DAOHAM"))


def test_th02_qua_verify_kiem_thang_goi_y():
    """Luồng seed: /v1/verify kèm buoc_bat_dau + de_bai (seed.ts gửi de_bai: opts.text)."""
    from app.verify import kiem_thang_goi_y
    bl = bai_lam_may(TH02)
    thang = [{"ma_buoc": "B.DH.NGHIEM", "cac_cap": [{"cap": 1, "noi_dung": NGUYEN_VAN[0]}]}]
    assert kiem_thang_goi_y(thang, bl, DE_TH02, "B.DH.NGHIEM") == []
    assert kiem_thang_goi_y(thang, bl, None, "B.DH.NGHIEM")            # không có đề -> không biết y' đề cho -> chặn
    assert kiem_thang_goi_y(thang, bl, DE_TH02, None)                  # bài đủ bước -> chặn
    thang2 = [{"ma_buoc": "B.DH.NGHIEM", "cac_cap": [{"cap": 1, "noi_dung": BIEN_DOI[0]}]}]
    assert kiem_thang_goi_y(thang2, bl, DE_TH02, "B.DH.NGHIEM")


def test_solve_them_yphay_de_cho_khung_ngan():
    from app.job_runner import _run
    r = _run("solve", {"ham": TH02, "de_bai": DE_TH02, "buoc_bat_dau": "B.DH.NGHIEM"})
    assert {"loai": "YPHAY_DE", "gia_tri": "(x² − 2x − 3)/(x − 1)²"} in r["su_kien"]
    r2 = _run("solve", {"ham": TH02})
    assert not any(x["loai"] == "YPHAY_DE" for x in r2["su_kien"])
    assert any(x["loai"] == "YPHAY" for x in r2["su_kien"])


def test_tam_bai_khung_ngan_seed_y_phay_de_dung_va_khop_dao_ham():
    """8 bài data/supham/bai-khung-ngan.seed-v01.json: YPHAY_DE lấy đúng dòng y' của đề và đề cho đúng đạo hàm."""
    from sympy import cancel, together
    L = json.load(open(os.path.join(ROOT, "data/supham/bai-khung-ngan.seed-v01.json"), encoding="utf-8"))
    assert len(L) == 8
    for b in L:
        de = su_kien_de_cho(b["de_bai"]["van_ban"], b["buoc_bat_dau"])
        assert len(de) == 1, b["id"]
        s = sk(b["de_bai"]["ham_so_sympy"])
        yp, [d] = lf._y_phay_su_kien([p for p in s if p[0] == "YPHAY"] + de)
        assert cancel(together(lf._doc_bt(lf._doi_bt(lf._tien_xu_ly_bt(de[0][1]))) - yp)) == 0, b["id"]
        # nhắc nguyên văn thì qua, y' dạng SymPy (đã rút gọn khác đề) thì chặn
        assert not chan("Đề cho y' = %s, em dùng luôn." % de[0][1], s + de), b["id"]
        yp_str = [p for p in s if p[0] == "YPHAY"][0][1]
        if lf._doc_bt(lf._doi_bt(yp_str), rut_gon=False) != lf._doc_bt(lf._doi_bt(lf._tien_xu_ly_bt(de[0][1])), rut_gon=False):
            assert chan("y' = %s" % yp_str, s + de), b["id"]


def test_seed_ts_gui_de_bai_va_buoc_bat_dau():
    src = open(os.path.join(ROOT, "apps/web/scripts/seed.ts"), encoding="utf-8").read()
    assert "de_bai: opts.text" in src
    assert "buoc_bat_dau: ex.buoc_bat_dau" in src


# ------------------------------------------------------------------ fail closed / an toàn đầu vào
def test_yphay_hong_su_kien_loi():
    q = loc_ban_nhap("Em xét dấu y' nhé.", [["DB", "(-oo;oo)"], ["YPHAY", "3*x**"]])
    assert q["cho_phep"] is False and q["ly_do"] == "SU_KIEN_LOI"


def test_bai_cu_khong_co_yphay_khong_doi_hanh_vi():
    """Sự kiện lưu trước 0004 (không có YPHAY): luật y' không chạy — cần seed lại để có YPHAY (ghi trong README)."""
    s = [p for p in sk(BAC_BA) if p[0] != "YPHAY"]
    assert not chan("Em sẽ được y' = 3x² − 12x + 9.", s)


def test_doan_la_khong_lam_nga_ca_cau():
    s = sk(BAC_BA)
    assert not chan("Em xem lại (((x + 1) và ))) chỗ ngoặc; x^^2 không phải cách viết.", s)
    assert chan("Em xem lại (((x + 1) và ))); còn y' = 3x² − 12x + 9.", s)
