# -*- coding: utf-8 -*-
"""Tầng 2/3 theo bộ ca ĐỘC LẬP của Kiểm định (kiemdinh/giai-doan-2/ca-tang23-dat-sai.yaml, T23-01..10) + ca seed."""
import os

import pytest
import yaml

from app.paths import KIEMDINH
from app.verify import cong_phat_hanh, verify

BO = yaml.safe_load(open(os.path.join(KIEMDINH, "giai-doan-2", "ca-tang23-dat-sai.yaml"), encoding="utf-8"))
CA5 = {c["id"]: c for c in yaml.safe_load(open(os.path.join(KIEMDINH, "bo-de-kiem-thu", "cac-ca-5-buoc.yaml"), encoding="utf-8"))["ca"]}


@pytest.mark.parametrize("ca", BO["ca"], ids=[c["id"] for c in BO["ca"]])
def test_t23(ca):
    dv = ca["dau_vao"]
    ma = dv["bai_lam"].split()[0]
    r = verify({"ham": dv["ham"], "bai_lam": CA5[ma]["bai_lam"], "tai_lieu": dv.get("tai_lieu") or [], "cong_thuc": dv.get("cong_thuc") or []})
    t = {x["tang"]: x for x in r["tang"]}
    mong = ca["ket_qua_dung"]
    assert (t[2]["trang_thai"], t[3]["trang_thai"], r["trang_thai_phat_hanh"]) == (mong["tang_2"], mong["tang_3"], mong["trang_thai_phat_hanh"])
    if t[2]["trang_thai"] in ("DAT", "SAI") and t[2]["trich_dan"]:
        c = t[2]["trich_dan"][0]
        doc = next(d for d in dv["tai_lieu"] if d["id"] == c["document_id"])
        assert c["trich"] in doc["text"]  # trích nguyên văn


def test_cong_khong_co_ket_qua_thi_khong_phat_hanh():
    assert cong_phat_hanh([])[0] == "CHO_GIAO_VIEN_DUYET"
    assert cong_phat_hanh([{"trang_thai": "DAT"}, {"trang_thai": "GV_DUYET"}])[0] == "DA_PHAT_HANH"
