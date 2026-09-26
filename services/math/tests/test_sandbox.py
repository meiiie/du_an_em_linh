# -*- coding: utf-8 -*-
import json
import subprocess
import sys

from app.sandbox import run_sympy_job


def test_api_khong_import_sympy():
    code = "import app.main, sys; raise SystemExit(0 if 'sympy' not in sys.modules else 1)"
    r = subprocess.run([sys.executable, "-c", code], cwd=".", capture_output=True, text=True)
    assert r.returncode == 0, r.stderr


def test_timeout_kill():
    r = run_sympy_job("spin", {"giay": 30}, timeout=1)
    assert r["loai_ket_qua"] == "KHONG_KIEM_DUOC"


def test_grade_qua_tien_trinh():
    r = run_sympy_job("grade", {
        "ham": "x**2",
        "nop_toi": "B.DH.TXD",
        "cac_buoc": [{"ma_buoc": "B.DH.TXD", "cac_dong": [{"dong": 0, "latex": "R"}]}],
    })
    assert r["ket_qua"] == "DAT"


def test_filter_chan_dap_an():
    r = run_sympy_job("filter", {
        "ban_nhap": "Hàm đồng biến trên khoảng (-∞; 0) và cực đại tại x = 0, giá trị cực đại bằng 2.",
        "su_kien": [
            {"loai": "DB", "gia_tri": "(-oo;0)"},
            {"loai": "DCD", "gia_tri": "0"},
            {"loai": "GTCD", "gia_tri": "2"},
        ],
    })
    assert r["cho_phep"] is False
    assert "khop" not in r


def test_filter_goi_y_an_toan():
    r = run_sympy_job("filter", {
        "ban_nhap": "Em hãy xét dấu của đạo hàm trên từng khoảng.",
        "su_kien": [{"loai": "DB", "gia_tri": "(-oo;0)"}, {"loai": "GTCD", "gia_tri": "2"}],
    })
    assert r["cho_phep"] is True
    assert "khop" not in json.dumps(r)
