# -*- coding: utf-8 -*-
"""Chạy lại bộ ca Tầng 1 của Kiểm định và so với kết quả đã đo (ket-qua-tang1.json)."""
import json
import os

import yaml

from app.paths import KIEMDINH, load_kiem

K = load_kiem()
BO = os.path.join(KIEMDINH, "bo-de-kiem-thu", "cac-ca.yaml")
DA_DO = os.path.join(KIEMDINH, "ket-qua", "ket-qua-tang1.json")


def test_khop_ket_qua_da_do():
    raw = open(BO, "rb").read()
    bo = yaml.safe_load(raw.decode("utf-8"))
    da_do = json.load(open(DA_DO, encoding="utf-8"))
    mong = {d["id"]: d for d in da_do["ket_qua_tung_ca"]}
    assert len(bo["ca"]) == len(mong)
    lech = []
    dem = {"sai_bat": 0, "sai_dat": 0, "sai_kkd": 0, "dung_dat": 0, "dung_sai": 0, "dung_kkd": 0}
    for ca in bo["ca"]:
        r = K.kiem(ca["kiem"])
        cu = mong[ca["id"]]
        if r["trang_thai"] != cu["trang_thai"]:
            lech.append((ca["id"], cu["trang_thai"], r["trang_thai"]))
        if ca["nhan"] == "sai":
            dem[{"SAI": "sai_bat", "DAT": "sai_dat", "KHONG_KIEM_DUOC": "sai_kkd"}[r["trang_thai"]]] += 1
        else:
            dem[{"SAI": "dung_sai", "DAT": "dung_dat", "KHONG_KIEM_DUOC": "dung_kkd"}[r["trang_thai"]]] += 1
    print("TANG1", json.dumps(dem, ensure_ascii=False), "lech", lech[:8])
    assert lech == []
    assert dem["sai_bat"] == da_do["tong_hop"]["loi_bat_duoc"]
    assert dem["dung_sai"] == da_do["tong_hop"]["dung_bao_nham"]
