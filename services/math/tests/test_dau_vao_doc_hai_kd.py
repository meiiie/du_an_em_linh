# -*- coding: utf-8 -*-
"""F-01: port bộ ca ĐẦU VÀO ĐỘC HẠI VÔ HẠI của Kiểm định (kiemdinh/bo-de-kiem-thu/ca-dau-vao-doc-hai.yaml, v1.0-f01).

Cách sinh ca giống bộ chạy tham chiếu kiemdinh/giai-doan-2/chay_doc_hai.py (22 chuỗi × 8 vị trí 5 bước, 3 kiểu tầng 1,
2 vị trí bộ lọc, 2 ca tầng 1 thêm). Kỳ vọng của bộ ca:
  - chấm 5 bước (/v1/grade, chạy qua sandbox thật): KHONG_KIEM_DUOC, không bao giờ DAT;
  - tầng 1 (kiem): KHONG_KIEM_DUOC với ly_do DAU_VAO_KHONG_HOP_LE;
  - bộ lọc (/v1/filter): cho_phep = false;
  - không quá thời hạn mỗi ca, không tạo file cờ /tmp/kd-f01-co-*.
"""
import copy
import glob
import hashlib
import os
import time

import pytest
import yaml

from app.grader import bai_lam_sang_payload
from app.leakfilter import loc_ban_nhap
from app.paths import KIEMDINH, load_kiem
from app.sandbox import run_sympy_job

FILE_BO = os.path.join(KIEMDINH, "bo-de-kiem-thu", "ca-dau-vao-doc-hai.yaml")
SHA_KHOA = "ce50b1737d0287c20f43206deb32cf46d423c21c37aedae09ca2e2eb79d47c76"  # khóa 29/09 11:59 (v1.0-f01)
_RAW = open(FILE_BO, "rb").read()
BO = yaml.safe_load(_RAW.decode("utf-8"))
HAN = int(BO["thoi_han_moi_ca_s"])
K = load_kiem()
CO = "/tmp/kd-f01-co-*"


def _chuoi(c):
    if "lap" in c:
        return c["lap"]["mau"] * c["lap"]["so_lan"] + c["lap"]["duoi"]
    return c["chuoi"]


def _dat_vao(bl, vi_tri, s):
    bl = copy.deepcopy(bl)
    if vi_tri == "ket_luan_o.cd":
        bl.pop("ket_luan", None)
        o = dict(BO["ket_luan_o_nen"])
        o["cd"] = o["cd"].replace("{CHUOI}", s)
        bl["ket_luan_o"] = o
        return bl
    phan = vi_tri.split(".")
    cur = bl
    for p in phan[:-1]:
        cur = cur[int(p)] if p.isdigit() else cur[p]
    last = phan[-1]
    cur[int(last) if last.isdigit() else last] = s
    return bl


def _xoa_co():
    for p in glob.glob(CO):
        os.remove(p)


@pytest.fixture(autouse=True)
def khong_co_file_co():
    _xoa_co()
    yield
    co = glob.glob(CO)
    _xoa_co()
    assert not co, "chuỗi độc đã bị THỰC THI: tạo file cờ %s" % co


CA_5B = [(c["id"], vt, _chuoi(c)) for c in BO["chuoi"] for vt in BO["vi_tri_5_buoc"]]


def _payload_5b(bl):
    """Giống hệt chay_doc_hai.py::goi_http (ô kết luận gõ tự do -> dòng kết luận thô)."""
    if "ket_luan_o" in bl:
        b2 = dict(bl)
        b2["ket_luan"] = {}
        b2.pop("ket_luan_o")
        body = bai_lam_sang_payload(b2, b2["ham"])
        o = bl["ket_luan_o"]
        body["cac_buoc"][4] = {"ma_buoc": "B.DH.KETLUAN", "khai_bao": ["dong_bien", "nghich_bien", "cuc_dai", "cuc_tieu"],
                               "cac_dong": [{"dong": i, "latex": t} for i, t in enumerate(
                                   ["đồng biến trên " + o["db"], "nghịch biến trên " + o["nb"], "cực đại tại " + o["cd"],
                                    "cực tiểu tại " + o["ct"]])]}
        return body
    return bai_lam_sang_payload(bl, bl["ham"])


@pytest.mark.parametrize("cid,vi_tri,s", CA_5B, ids=["%s@%s" % (a, b) for a, b, _ in CA_5B])
def test_cham_5_buoc_tu_choi(cid, vi_tri, s):
    payload = _payload_5b(_dat_vao(BO["bai_nen"], vi_tri, s))  # dựng payload lỗi = NGOAI_LE ở bộ chạy KĐ -> test hỏng
    payload["timeout_s"] = HAN
    t0 = time.monotonic()
    r = run_sympy_job("grade", payload, timeout=HAN)
    assert time.monotonic() - t0 < HAN, (cid, vi_tri, "quá thời hạn")
    assert r.get("ket_qua") != "DAT", (cid, vi_tri, r.get("thong_bao"))
    assert r.get("ket_qua") == "KHONG_KIEM_DUOC", (cid, vi_tri, r.get("ket_qua"), r.get("loai_ket_qua"), r.get("thong_bao"))
    assert r.get("ly_do") == "DAU_VAO_KHONG_HOP_LE", (cid, vi_tri, r.get("ly_do"))


def test_doi_chung_bai_nen_van_dat():
    """Bài nền (không chèn chuỗi) vẫn DAT qua sandbox: cổng F-01 không chặn bài làm đúng."""
    r = run_sympy_job("grade", _payload_5b(copy.deepcopy(BO["bai_nen"])), timeout=HAN)
    assert r.get("ket_qua") == "DAT", r
    o = dict(BO["ket_luan_o_nen"])
    o["cd"] = o["cd"].replace("{CHUOI}", "0")
    bl = copy.deepcopy(BO["bai_nen"])
    bl["ket_luan_o"] = o
    r = run_sympy_job("grade", _payload_5b(bl), timeout=HAN)
    assert r.get("ket_qua") == "DAT", r


CA_T1 = [(c["id"], t, _chuoi(c)) for c in BO["chuoi"] for t in BO["tang1"]]


@pytest.mark.parametrize("cid,t,s", CA_T1, ids=["%s@tang1.%s" % (a, t["kieu"]) for a, t, _ in CA_T1])
def test_tang1_tu_choi(cid, t, s):
    k = {kk: vv for kk, vv in t.items() if kk != "truong"}
    k[t["truong"]] = s
    r = K.kiem(k)
    assert r["trang_thai"] == "KHONG_KIEM_DUOC", (cid, t["kieu"], r["trang_thai"], r.get("chi_tiet"))
    assert r.get("ly_do") == "DAU_VAO_KHONG_HOP_LE", (cid, t["kieu"], r.get("chi_tiet"))


@pytest.mark.parametrize("t", BO["tang1_them"], ids=[t["id"] for t in BO["tang1_them"]])
def test_tang1_them_tu_choi(t):
    k = {kk: vv for kk, vv in t.items() if kk not in ("id", "co", "ghi_chu")}
    r = K.kiem(k)
    assert r["trang_thai"] == "KHONG_KIEM_DUOC", (t["id"], r["trang_thai"], r.get("chi_tiet"))
    assert r.get("ly_do") == "DAU_VAO_KHONG_HOP_LE", (t["id"], r.get("chi_tiet"))


SK_NEN = [{"loai": a, "gia_tri": b} for a, b in BO["loc"]["bai"]["su_kien"]]


@pytest.mark.parametrize("c", BO["chuoi"], ids=[c["id"] for c in BO["chuoi"]])
def test_loc_ban_nhap_chan(c):
    text = BO["loc"]["mau_ban_nhap"].replace("{CHUOI}", _chuoi(c))
    r = run_sympy_job("filter", {"ban_nhap": text, "su_kien": SK_NEN}, timeout=HAN)
    assert r["cho_phep"] is False, (c["id"], r)
    assert r.get("ly_do") == "DAU_VAO_KHONG_HOP_LE", (c["id"], r)


@pytest.mark.parametrize("c", BO["chuoi"], ids=[c["id"] for c in BO["chuoi"]])
def test_loc_su_kien_doc_chan(c):
    doc = BO["loc"]["su_kien_doc_hai"]
    sk = [{"loai": a, "gia_tri": (_chuoi(c) if a == doc else b)} for a, b in BO["loc"]["bai"]["su_kien"]]
    r = run_sympy_job("filter", {"ban_nhap": "Em thử xét dấu y' xem sao.", "su_kien": sk}, timeout=HAN)
    assert r["cho_phep"] is False, (c["id"], r)


def test_cau_hop_le_van_qua():
    """Đối chứng: câu gợi ý bình thường với sự kiện bình thường vẫn qua (không chặn hết cho xong)."""
    assert loc_ban_nhap("Em thử xét dấu y' xem sao.", SK_NEN, "")["cho_phep"] is True
    assert K.kiem({"kieu": "dao_ham", "ham": "x**2", "dao_ham_ai": "2*x"})["trang_thai"] == "DAT"
    assert K.kiem({"kieu": "dem", "khong_gian": "combinations(range(10), 3)", "gia_tri_ai": "120"})["trang_thai"] == "DAT"
    assert K.kiem({"kieu": "xac_suat", "khong_gian": "product(range(1, 7), repeat=2)", "su_kien": "lambda t: t[0] + t[1] == 7",
                   "gia_tri_ai": "1/6"})["trang_thai"] == "DAT"


def test_bo_ca_dung_khoa_va_du_288():
    assert hashlib.sha256(_RAW).hexdigest() == SHA_KHOA
    assert BO["phien_ban"] == "1.0-f01"
    so = len(CA_5B) + len(CA_T1) + len(BO["tang1_them"]) + 2 * len(BO["chuoi"])
    assert (len(CA_5B), len(CA_T1), len(BO["tang1_them"]), 2 * len(BO["chuoi"])) == (176, 66, 2, 44)
    assert so == 288
