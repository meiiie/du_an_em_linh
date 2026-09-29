# -*- coding: utf-8 -*-
"""Bản vá bộ chấm 0002c trên main 507874c (+ 0002b), Kiểm định 29/09.
1) Luật dấu U (Sư phạm chốt 12:13; DAC-TA §3.4(e)): kết luận ĐƠN ĐIỆU nối các khoảng bằng U / u / ∪ / \\cup
   -> SAI_KET_LUAN, ERR.DH.07, B.DH.KETLUAN, kể cả khi đúng về toán; cờ toan_dung (true: hàm số thật sự đơn điệu đúng chiều
   trên cả tập; false: vắt qua điểm loại hoặc không đơn điệu trên cả tập). Chỉ áp cho bước kết luận: TXĐ học sinh viết bằng
   U / ∪ vẫn DAT (Sư phạm 12:26).
2) buoc_bat_dau (bài khung ngắn; DAC-TA §3.4(a)): các bước trước bước bắt đầu do đề cho sẵn, lấy từ lời giải máy, không chấm;
   bài bắt đầu ở B.DH.XETDAU thiếu mốc -> DIEM_THIEU ở B.DH.XETDAU, o = {X, null}; thừa mốc -> ERR.DH.31.
Ca: DH12-03-TH-02 bản 2 (2 ca Sư phạm nêu) + ca dấu U sẵn có ở DH12-03-NB-01, DH12-03-NB-02 (phương án D), DH12-03-TH-01."""
import pytest

from app.grader import grade

H_TH02 = "(x**2 - 3*x + 6)/(x - 1)"
H_NB01 = "x**3 - 3*x**2 - 9*x + 2"
H_NB02 = "(x + 2)/(x - 1)"
H_TH01 = "x**3 + 3*x**2 - 24*x + 1"


def _o(xs, daus, tai):
    """xs: mốc; daus: dấu các khoảng; tai: ô tại điểm ('0' / '||')."""
    cells = [{"hang": "X", "k": i, "gia_tri": v} for i, v in enumerate(xs)]
    for j, d in enumerate(daus):
        cells.append({"hang": "DAU_YPHAY", "k": 2 * j, "gia_tri": d})
    for j, t in enumerate(tai):
        if t is not None:   # điểm thừa không phải điểm tới hạn: để trống ô tại điểm
            cells.append({"hang": "DAU_YPHAY", "k": 2 * j + 1, "gia_tri": t})
    return cells


BAI = {
    "TH02": dict(ham=H_TH02, txd="\\mathbb{R}\\setminus\\{1\\}", dh="\\frac{x^{2}-2x-3}{(x-1)^{2}}",
                 nghiem=[{"dong": 0, "latex": "x = -1"}, {"dong": 1, "latex": "x = 3"}],
                 cells=_o(["-1", "1", "3"], ["+", "-", "-", "+"], ["0", "||", "0"]),
                 db="(-\\infty;-1) và (3;+\\infty)", nb="(-1;1) và (1;3)"),
    "NB01": dict(ham=H_NB01, txd="\\mathbb{R}", dh="3x^{2}-6x-9",
                 nghiem=[{"dong": 0, "latex": "x = -1"}, {"dong": 1, "latex": "x = 3"}],
                 cells=_o(["-1", "3"], ["+", "-", "+"], ["0", "0"]), db="(-\\infty;-1) và (3;+\\infty)", nb="(-1;3)"),
    "TH01": dict(ham=H_TH01, txd="\\mathbb{R}", dh="3x^{2}+6x-24",
                 nghiem=[{"dong": 0, "latex": "x = -4"}, {"dong": 1, "latex": "x = 2"}],
                 cells=_o(["-4", "2"], ["+", "-", "+"], ["0", "0"]), db="(-\\infty;-4) và (2;+\\infty)", nb="(-4;2)"),
    "NB02": dict(ham=H_NB02, txd="\\mathbb{R}\\setminus\\{1\\}", dh="\\frac{-3}{(x-1)^{2}}",
                 nghiem=[{"dong": 0, "latex": "không có nghiệm"}],
                 cells=_o(["1"], ["-", "-"], ["||"]), db="không có", nb="(-\\infty;1) và (1;+\\infty)"),
    # hàm có điểm loại x = -3 (hai cách viết TXĐ Sư phạm nêu nguyên văn 29/09 12:26)
    "M3": dict(ham="(x - 1)/(x + 3)", txd="\\mathbb{R}\\setminus\\{-3\\}", dh="\\frac{4}{(x+3)^{2}}",
               nghiem=[{"dong": 0, "latex": "không có nghiệm"}],
               cells=_o(["-3"], ["+", "+"], ["||"]), db="(-\\infty;-3) và (-3;+\\infty)", nb="không có"),
}


def _payload(ten, db=None, nb=None, bat_dau=None, txd=None, cells=None):
    b = BAI[ten]
    kl = [{"dong": 0, "latex": b["db"] if db is None else db, "loai": "DONG_BIEN"},
          {"dong": 1, "latex": b["nb"] if nb is None else nb, "loai": "NGHICH_BIEN"}]
    buoc = [{"ma_buoc": "B.DH.TXD", "cac_dong": [{"dong": 0, "latex": b["txd"] if txd is None else txd}]},
            {"ma_buoc": "B.DH.DAOHAM", "cac_dong": [{"dong": 0, "latex": b["dh"]}]},
            {"ma_buoc": "B.DH.NGHIEM", "cac_dong": b["nghiem"]},
            {"ma_buoc": "B.DH.XETDAU", "bang": {"loai_bang": "XET_DAU", "cac_o": b["cells"] if cells is None else cells}},
            {"ma_buoc": "B.DH.KETLUAN", "cac_dong": kl, "khai_bao": ["dong_bien", "nghich_bien"]}]
    p = {"ham": b["ham"], "nop_toi": "B.DH.KETLUAN", "cac_buoc": buoc}
    if bat_dau:
        p["buoc_bat_dau"] = bat_dau
        p["cac_buoc"] = buoc[["B.DH.TXD", "B.DH.DAOHAM", "B.DH.NGHIEM", "B.DH.XETDAU", "B.DH.KETLUAN"].index(bat_dau):]
    return p


def _van_de_U(r, dong):
    vs = [v for v in r["cac_van_de"] if v["loai_ket_qua"] == "SAI_KET_LUAN" and v["buoc_sai"]["ma_buoc"] == "B.DH.KETLUAN"]
    assert len(vs) == 1, r
    assert vs[0]["buoc_sai"] == {"ma_buoc": "B.DH.KETLUAN", "dong": dong, "o": None}, r
    assert vs[0]["ma_loi"] == "ERR.DH.07" and r["ma_loi"] == "ERR.DH.07", r
    return vs[0]


@pytest.mark.parametrize("ten", list(BAI))
def test_ban_dung_khong_U_DAT(ten):
    r = grade(_payload(ten))
    assert r["ket_qua"] == "DAT", r


# ---- DH12-03-TH-02 bản 2: hai ca Sư phạm nêu
@pytest.mark.parametrize("db", ["(-\\infty;-1) \\cup (3;+\\infty)", "(−∞; −1) ∪ (3; +∞)", "(-oo;-1) U (3;+oo)",
                                "(-\\infty;-1)U(3;+\\infty)", "(-\\infty; -1) u (3; +\\infty)"])
def test_TH02_dong_bien_U_dung_toan_van_SAI(db):
    r = grade(_payload("TH02", db=db))
    assert r["ket_qua"] == "SAI", r
    v = _van_de_U(r, 0)
    assert v["toan_dung"] is True and r["toan_dung"] is True


def test_TH02_nghich_bien_U_sai_toan():
    r = grade(_payload("TH02", nb="(-1;1) \\cup (1;3)"))
    v = _van_de_U(r, 1)
    assert v["toan_dung"] is False and r["toan_dung"] is False


# ---- ca dấu U sẵn có ở NB-01, NB-02, TH-01: đều sai toán
@pytest.mark.parametrize("ten,db,nb,dong", [
    ("NB01", "(-\\infty;-1) \\cup (3;+\\infty)", None, 0),
    ("TH01", "(-\\infty;-4) \\cup (2;+\\infty)", None, 0),
    ("NB02", None, "(-\\infty;1) \\cup (1;+\\infty)", 1),
])
def test_ca_U_san_co_toan_dung_false(ten, db, nb, dong):
    r = grade(_payload(ten, db=db, nb=nb))
    v = _van_de_U(r, dong)
    assert v["toan_dung"] is False


def test_khong_U_vat_qua_diem_loai_khong_co_co():
    r = grade(_payload("TH02", nb="(-1;3)"))
    assert r["ket_qua"] == "SAI" and r["buoc_sai"]["ma_buoc"] == "B.DH.KETLUAN" and r["ma_loi"] == "ERR.DH.07", r
    assert "toan_dung" not in r and all("toan_dung" not in v for v in r["cac_van_de"])


# ---- phạm vi: TXĐ viết bằng U / ∪ vẫn DAT
@pytest.mark.parametrize("ten,txd", [
    ("TH02", "(-\\infty;1)\\cup(1;+\\infty)"), ("TH02", "(−∞; 1) ∪ (1; +∞)"), ("TH02", "(-oo;1) U (1;+oo)"),
    ("NB02", "(-\\infty; 1) U (1; +\\infty)"), ("NB02", "(-oo;1)∪(1;+oo)"),
    ("M3", "(−∞; −3) ∪ (−3; +∞)"), ("M3", "(-oo;-3) U (-3;+oo)"), ("M3", "(-oo; -3) u (-3; +oo)"),
    ("M3", "\\left(-\\infty; -3\\right) \\cup \\left(-3; +\\infty\\right)"),
])
def test_txd_viet_bang_U_van_DAT(ten, txd):
    r = grade(_payload(ten, txd=txd))
    assert r["ket_qua"] == "DAT", r
    assert "ERR.DH.07" not in str(r)


@pytest.mark.parametrize("txd", ["(−∞; −3) ∪ (−3; +∞)", "(-oo;-3) U (-3;+oo)"])
def test_nop_rieng_txd_viet_U_DAT(txd):
    # nộp riêng bước TXĐ (ô TXĐ gõ tự do): không có bước kết luận, luật U không được chạm tới
    r = grade({"ham": "(x - 1)/(x + 3)", "nop_toi": "B.DH.TXD",
               "cac_buoc": [{"ma_buoc": "B.DH.TXD", "cac_dong": [{"dong": 0, "latex": txd}]}]})
    assert r["ket_qua"] == "DAT", r
    assert "ERR.DH.07" not in str(r) and "toan_dung" not in r


def test_txd_U_sai_diem_loai_van_SAI_o_TXD_khong_ERR07():
    r = grade(_payload("M3", txd="(-oo;3) U (3;+oo)"))
    assert r["ket_qua"] == "SAI" and r["buoc_sai"]["ma_buoc"] == "B.DH.TXD", r
    assert r.get("ma_loi") != "ERR.DH.07" and "toan_dung" not in r, r


# ---- buoc_bat_dau
def test_bat_dau_nghiem_DAT_va_khong_co_thi_nhu_cu():
    assert grade(_payload("TH02", bat_dau="B.DH.NGHIEM"))["ket_qua"] == "DAT"
    p = _payload("TH02", bat_dau="B.DH.NGHIEM"); p.pop("buoc_bat_dau")
    assert grade(p)["ket_qua"] == "KHONG_KIEM_DUOC"   # thiếu TXĐ mà không khai báo bước bắt đầu: như main


def test_bat_dau_nghiem_U_van_ap_luat():
    r = grade(_payload("TH02", db="(-\\infty;-1) \\cup (3;+\\infty)", bat_dau="B.DH.NGHIEM"))
    assert _van_de_U(r, 0)["toan_dung"] is True


@pytest.mark.parametrize("ten", ["NB01", "NB02"])
def test_bat_dau_xetdau_DAT(ten):
    assert grade(_payload(ten, bat_dau="B.DH.XETDAU"))["ket_qua"] == "DAT"


def test_bat_dau_xetdau_thieu_moc():
    r = grade(_payload("NB01", bat_dau="B.DH.XETDAU", cells=_o(["-1"], ["+", "-"], ["0"])))
    assert r["ket_qua"] == "SAI", r
    v = r["cac_van_de"][0]
    assert v["loai_ket_qua"] == "DIEM_THIEU" and v["buoc_sai"] == {"ma_buoc": "B.DH.XETDAU", "dong": None, "o": {"hang": "X", "k": None}}, r


def test_bat_dau_xetdau_thua_moc_giong_bai_day_du():
    # Thừa mốc chỉ ở bảng: vấn đề nằm ở B.DH.XETDAU, KHÔNG rơi về bước đề cho sẵn, và giống hệt khi nộp đủ 5 bước
    # (trên main, ô tại điểm thừa để trống cho SAI_GIA_TRI/ERR.DH.06 ở ô đó — hành vi sẵn có, không đổi ở bản vá này).
    cells = _o(["-1", "0", "3"], ["+", "-", "-", "+"], ["0", None, "0"])
    r = grade(_payload("NB01", bat_dau="B.DH.XETDAU", cells=cells))
    r_du = grade(_payload("NB01", cells=cells))
    assert r["ket_qua"] == "SAI" and all(v["buoc_sai"]["ma_buoc"] == "B.DH.XETDAU" for v in r["cac_van_de"]), r
    assert [(v["loai_ket_qua"], v["buoc_sai"], v["ma_loi"]) for v in r["cac_van_de"]] == \
        [(v["loai_ket_qua"], v["buoc_sai"], v["ma_loi"]) for v in r_du["cac_van_de"]]


@pytest.mark.parametrize("gt", ["B.DH.XYZ", "", 3, None])
def test_buoc_bat_dau_la_bi_tu_choi(gt):
    p = _payload("TH02", bat_dau="B.DH.NGHIEM"); p["buoc_bat_dau"] = gt
    r = grade(p)
    assert r["ket_qua"] == "KHONG_KIEM_DUOC" and r.get("ly_do") == "DAU_VAO_KHONG_HOP_LE", r
