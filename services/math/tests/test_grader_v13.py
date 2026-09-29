# -*- coding: utf-8 -*-
"""v1.3 qua payload sản phẩm: đủ cac_van_de, không lộ giá trị điểm, tên hàng HOA, mã lỗi cụ thể (SP-05, SP-07)."""
import copy
import os

import pytest
import yaml

from app.grader import bai_lam_sang_payload, grade
from app.paths import KIEMDINH

BO = os.path.join(KIEMDINH, "bo-de-kiem-thu", "cac-ca-5-buoc-bo-sung-v1.3.yaml")
CA = yaml.safe_load(open(BO, encoding="utf-8"))["ca"]


@pytest.mark.parametrize("ca", CA, ids=[c["id"] for c in CA])
def test_payload_v13(ca):
    r = grade(bai_lam_sang_payload(ca["bai_lam"]))
    if ca["nhan"] == "dung":
        assert r["ket_qua"] == "DAT"
        assert r["cac_van_de"] == []
        return
    assert r["ket_qua"] == "SAI"
    loai = []
    for v in r["cac_van_de"]:
        assert "diem" not in v
        o = v["buoc_sai"].get("o")
        if o:
            assert o["hang"] in ("X", "DAU_YPHAY", "BIEN_THIEN")
        if v["loai_ket_qua"] not in loai:
            loai.append(v["loai_ket_qua"])
    assert loai == ca["loai_ket_qua_nhan"]
    assert r["buoc_sai"] == ca["buoc_sai_nhan"]
    for a, b in zip(r["cac_van_de"], ca["cac_van_de_nhan"]):
        assert a["buoc_sai"] == b["buoc_sai"]
        if "ma_loi" in b:
            assert a["ma_loi"] == b["ma_loi"]
    assert r["ma_loi"] == (ca["cac_van_de_nhan"][0].get("ma_loi") or r["ma_loi"])


def _bai(**thay):
    bl = {
        "ham": "x**3 - 3*x**2 + 2", "TXD": "R", "dao_ham": "3*x**2 - 6*x",
        "y_phay_bang_0": ["0", "2"], "y_phay_khong_xd": [],
        "bang": {"moc": ["-oo", "0", "2", "oo"], "dau": ["+", "-", "+"], "chieu": ["tang", "giam", "tang"]},
        "ket_luan": {"dong_bien": ["(-oo; 0)", "(2; +oo)"], "nghich_bien": ["(0; 2)"], "cuc_dai_x": ["0"], "cuc_tieu_x": ["2"]},
    }
    bl = copy.deepcopy(bl)
    for k, v in thay.items():
        bl[k] = v
    return bl


def test_sp05_loai_theo_bo_kiem():
    r = grade(bai_lam_sang_payload(_bai(TXD="R \\ {1}")))
    assert r["loai_ket_qua"] == "SAI_TXD"
    r = grade(bai_lam_sang_payload(_bai(bang={"moc": ["-oo", "0", "2", "oo"], "dau": ["+", "+", "+"]})))
    assert r["loai_ket_qua"] == "SAI_DAU"
    assert r["do_tin_cay"] < 0.65  # chỉ đoán theo loại


def test_sp07_dao_chieu_don_dieu_la_err08():
    kl = {"dong_bien": ["(0; 2)"], "nghich_bien": ["(-oo; 0)", "(2; +oo)"], "cuc_dai_x": ["0"], "cuc_tieu_x": ["2"]}
    r = grade(bai_lam_sang_payload(_bai(ket_luan=kl)))
    assert r["loai_ket_qua"] == "SAI_KET_LUAN" and r["ma_loi"] == "ERR.DH.08"


def test_sp07_doi_loai_cuc_tri_err12():
    kl = {"dong_bien": ["(-oo; 0)", "(2; +oo)"], "nghich_bien": ["(0; 2)"], "cuc_dai_x": ["2"], "cuc_tieu_x": ["0"]}
    r = grade(bai_lam_sang_payload(_bai(ket_luan=kl)))
    assert r["ma_loi"] == "ERR.DH.12"


def test_sp07_cuc_tri_khi_khong_doi_dau_err10():
    bl = {"ham": "x**3", "TXD": "R", "dao_ham": "3*x**2", "y_phay_bang_0": ["0"], "y_phay_khong_xd": [],
          "bang": {"moc": ["-oo", "0", "oo"], "dau": ["+", "+"]},
          "ket_luan": {"dong_bien": ["(-oo; +oo)"], "cuc_dai_x": [], "cuc_tieu_x": ["0"]}}
    r = grade(bai_lam_sang_payload(bl))
    assert r["ket_qua"] == "SAI" and r["ma_loi"] == "ERR.DH.10"


def test_sp07_gia_tri_cuc_tri_thay_vao_y_phay_err22():
    kl = {"dong_bien": ["(-oo; 0)", "(2; +oo)"], "nghich_bien": ["(0; 2)"], "cuc_dai_x": ["0"], "cuc_tieu_x": ["2"],
          "gia_tri_cuc_dai": ["0"], "gia_tri_cuc_tieu": ["-2"]}
    r = grade(bai_lam_sang_payload(_bai(ket_luan=kl)))
    assert r["ket_qua"] == "SAI" and r["ma_loi"] == "ERR.DH.22"


def test_sp07_mui_ten_trai_dau_err26():
    r = grade(bai_lam_sang_payload(_bai(bang={"moc": ["-oo", "0", "2", "oo"], "dau": ["+", "-", "+"], "chieu": ["tang", "tang", "tang"]})))
    assert r["loai_ket_qua"] == "SAI_BIEN_DOI" and r["ma_loi"] == "ERR.DH.26"


def test_sp07_diem_ngoai_txd_err02_khong_goi_diem_toi_han():
    bl = {"ham": "x/(x+3)", "TXD": "R \\ {-3}", "dao_ham": "3/(x+3)**2", "y_phay_bang_0": [], "y_phay_khong_xd": [],
          "bang": {"moc": ["-oo", "oo"], "dau": ["+"]},
          "ket_luan": {"dong_bien": ["(-oo; -3)", "(-3; +oo)"]}}
    r = grade(bai_lam_sang_payload(bl))
    assert r["ket_qua"] == "SAI" and r["loai_ket_qua"] == "DIEM_THIEU"
    assert r["ma_loi"] == "ERR.DH.02"
    assert "điểm tới hạn" not in r["thong_bao"]
