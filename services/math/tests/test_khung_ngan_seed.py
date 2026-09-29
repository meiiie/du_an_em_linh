"""Bài khung ngắn (Sư phạm, supham/bai-khung-ngan §11, seed-v01 sha256 cb390098…963c):
- cổng kiểm định 3 tầng chấm ĐÚNG khung ngắn (verify với buoc_bat_dau) -> 8/8 DAT, thang gợi ý sạch;
- bộ chấm với buoc_bat_dau: lời giải đúng 8/8 DAT, 32/32 lời giải sai bị bắt (4 kiểu sai mỗi bài).
"""
import copy
import hashlib
import json
from pathlib import Path

import pytest

from app.grader import bai_lam_sang_payload, grade
from app.machine import bai_lam_may
from app.verify import verify

F = Path(__file__).resolve().parents[3] / "data" / "supham" / "bai-khung-ngan.seed-v01.json"
SHA = "cb3900983734adeaf1b21d85bb79343e092132d3d12c710e0546190efa1b963c"
ORDER = ["B.DH.TXD", "B.DH.DAOHAM", "B.DH.NGHIEM", "B.DH.XETDAU", "B.DH.KETLUAN"]
BAI = json.loads(F.read_text(encoding="utf-8"))


def test_file_dung_sha_va_du_8_bai():
    assert hashlib.sha256(F.read_bytes()).hexdigest() == SHA
    assert len(BAI) == 8 and all(b["buoc_bat_dau"] in ("B.DH.NGHIEM", "B.DH.XETDAU") for b in BAI)


def _payload(b, bl=None):
    ham = b["de_bai"]["ham_so_sympy"]
    bl = bl or bai_lam_may(ham)
    gp = bai_lam_sang_payload(bl, ham)
    bd = b["buoc_bat_dau"]
    gp["cac_buoc"] = [s for s in gp["cac_buoc"] if s["ma_buoc"] in ORDER[ORDER.index(bd):]]
    gp["buoc_bat_dau"] = bd
    return gp


@pytest.mark.parametrize("b", BAI, ids=[b["id"] for b in BAI])
def test_verify_khung_ngan_DAT(b):
    ham = b["de_bai"]["ham_so_sympy"]
    v = verify({"ham": ham, "bai_lam": bai_lam_may(ham), "buoc_bat_dau": b["buoc_bat_dau"], "thang_goi_y": b["thang_goi_y"]})
    t1 = v["tang"][0]
    assert t1["trang_thai"] == "DAT", t1
    assert "thang_goi_y_lo" not in t1


@pytest.mark.parametrize("b", BAI, ids=[b["id"] for b in BAI])
def test_loi_giai_dung_DAT(b):
    assert grade(_payload(b))["ket_qua"] == "DAT"


def _sai(b, kieu):
    bl = copy.deepcopy(bai_lam_may(b["de_bai"]["ham_so_sympy"]))
    kl = bl["ket_luan"]
    if kieu == "dao_db_nb":
        kl["dong_bien"], kl["nghich_bien"] = kl.get("nghich_bien") or [], kl.get("dong_bien") or []
    elif kieu == "doi_dau":
        bl["bang"]["dau"] = [{"+": "-", "-": "+"}.get(s, s) for s in bl["bang"]["dau"]]
    elif kieu == "thieu_moc":
        moc = bl["bang"]["moc"]
        i = 1
        bl["bang"]["moc"] = moc[:i] + moc[i + 1:]
        bl["bang"]["dau"] = bl["bang"]["dau"][:i] + bl["bang"]["dau"][i + 1:]
        bl["bang"]["chieu"] = bl["bang"]["chieu"][:i] + bl["bang"]["chieu"][i + 1:]
        if bl["bang"].get("dau_tai_diem"):
            bl["bang"]["dau_tai_diem"] = bl["bang"]["dau_tai_diem"][1:]
    elif kieu == "gop_U":
        k = "dong_bien" if len(kl.get("dong_bien") or []) >= 2 else "nghich_bien"
        ds = kl.get(k) or []
        if len(ds) >= 2:
            kl[k] = [" U ".join(ds)]
        else:
            kl["dong_bien"] = [(kl.get("dong_bien") or ["(-oo; 0)"])[0] + " U (100; 101)"]
    return bl


@pytest.mark.parametrize("kieu", ["dao_db_nb", "doi_dau", "thieu_moc", "gop_U"])
@pytest.mark.parametrize("b", BAI, ids=[b["id"] for b in BAI])
def test_loi_giai_sai_bi_bat(b, kieu):
    r = grade(_payload(b, _sai(b, kieu)))
    assert r["ket_qua"] != "DAT", (kieu, r.get("ket_qua"), r.get("thong_bao"))
