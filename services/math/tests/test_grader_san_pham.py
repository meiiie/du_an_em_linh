# -*- coding: utf-8 -*-
"""Bộ chấm sản phẩm: k bắt đầu từ 0, không trả đáp án, đường khác vẫn đạt."""
import json
import os

import yaml

from app.grader import bai_lam_sang_payload, grade, nhan_sang_san_pham
from app.machine import bai_lam_may
from app.paths import KIEMDINH

BO = os.path.join(KIEMDINH, "bo-de-kiem-thu", "cac-ca-5-buoc.yaml")


# Nhãn v1.2 bị luật chốt 29/09 thay thế (bộ v1.2 đóng băng nên không sửa YAML):
# S14 có điểm thừa x = 1 CHỈ ở hàng X (bước nghiệm đúng) -> B.DH.XETDAU, ERR.DH.31 (cùng tình huống với S29 của v1.3).
GHI_DE_NHAN = {"S14": "B.DH.XETDAU"}


def _nhan_bs(ca):
    bs = ca["buoc_sai_nhan"]
    if bs is not None and ca["id"] in GHI_DE_NHAN:
        bs = dict(bs, ma_buoc=GHI_DE_NHAN[ca["id"]])
    return bs


def test_16_ca_qua_payload_san_pham():
    bo = yaml.safe_load(open(BO, encoding="utf-8"))
    lech = []
    for ca in bo["ca"]:
        payload = bai_lam_sang_payload(ca["bai_lam"])
        r = grade(payload)
        mong = nhan_sang_san_pham(_nhan_bs(ca))
        if ca["nhan"] == "dung":
            if r["ket_qua"] != "DAT":
                lech.append((ca["id"], r["ket_qua"], r.get("buoc_sai"), r.get("thong_bao")))
            continue
        if r["ket_qua"] != "SAI":
            lech.append((ca["id"], "khong SAI", r["ket_qua"], r.get("thong_bao"), r.get("buoc_sai")))
            continue
        bs = r["buoc_sai"]
        if bs["ma_buoc"] != mong["ma_buoc"] or bs.get("dong") != mong.get("dong"):
            lech.append((ca["id"], "dong", bs, mong))
            continue
        if (bs.get("o") or None) != (mong.get("o") or None):
            lech.append((ca["id"], "o", bs.get("o"), mong.get("o")))
    print("SAN_PHAM lech", json.dumps(lech, ensure_ascii=False, default=str)[:2000])
    assert lech == []


def test_cap_dong_dao_ham_va_khong_lo_dap_an():
    payload = {
        "ham": "x**3 - 3*x**2 + 2",
        "nop_toi": "B.DH.DAOHAM",
        "cac_buoc": [
            {"ma_buoc": "B.DH.TXD", "cac_dong": [{"dong": 0, "latex": r"\mathbb{R}"}]},
            {"ma_buoc": "B.DH.DAOHAM", "cac_dong": [
                {"dong": 0, "latex": "3x^{2}-6x"},
                {"dong": 1, "latex": "3x(x-1)"},
            ]},
        ],
    }
    r = grade(payload)
    assert r["ket_qua"] == "SAI"
    assert r["buoc_sai"] == {"ma_buoc": "B.DH.DAOHAM", "dong": 1, "o": None}
    assert r["loai_ket_qua"] == "SAI_BIEN_DOI"
    blob = json.dumps(r, ensure_ascii=False)
    assert "3*x**2 - 6*x" not in blob
    # B-23: kiểm thật — không lộ dạng đúng của dòng sai (3x(x-2)) trong đầu ra
    assert "x-2" not in blob.replace(" ", "")
    assert "x - 2" not in blob
    # đường đúng khác (phân tích nhân tử) vẫn đạt
    payload["cac_buoc"][1]["cac_dong"][1]["latex"] = "3x(x-2)"
    ok = grade(payload)
    assert ok["ket_qua"] == "DAT"
    assert ok["chua_xong"] is True


def test_may_tu_giai_dat():
    for ham in ("x**3 - 3*x**2 + 2", "x**2", "(x+1)/(x-1)", "x**4 - 2*x**2"):
        bl = bai_lam_may(ham)
        assert bl, ham
        r = grade(bai_lam_sang_payload(bl, ham))
        assert r["ket_qua"] == "DAT", (ham, r.get("thong_bao"), r.get("buoc_sai"), r.get("loai_ket_qua"))
