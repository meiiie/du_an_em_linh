# -*- coding: utf-8 -*-
"""Tiến trình con: nơi duy nhất được gọi SymPy với dữ liệu không tin cậy."""
import json
import sys
import time


def _run(kind, payload):
    if kind == "grade":
        from app.grader import grade
        return grade(payload)
    if kind == "verify":
        from app.verify import verify
        return verify(payload)
    if kind == "filter":
        from app.leakfilter import loc_ban_nhap
        # Chốt 29/09: trường chuẩn là `cau_hoc_sinh`; `cau_hs` giữ làm bí danh cũ
        cau = payload.get("cau_hoc_sinh", payload.get("cau_hs"))
        return loc_ban_nhap(payload.get("ban_nhap") or "", payload.get("su_kien") or [], cau)
    if kind == "generate":
        from app.generator import sinh
        return sinh(payload)
    if kind == "solve":
        from app.generator import _hints
        from app.grader import bai_lam_sang_payload, grade
        from app.machine import bai_lam_may, latex_ham, su_kien_bao_ve
        ham = payload.get("ham")
        bl = bai_lam_may(ham) if ham else None
        if not bl:
            return {"dat": False, "ham": ham, "ly_do": "khong_giai_duoc"}
        gp = bai_lam_sang_payload(bl, ham)
        g = grade(gp)
        sk = su_kien_bao_ve(bl)
        return {
            "dat": g["ket_qua"] == "DAT",
            "ham": ham,
            "latex": latex_ham(ham),
            "bai_lam": bl,
            "payload_cham": gp,
            "thong_bao": g.get("thong_bao"),
            "buoc_sai": g.get("buoc_sai"),
            "su_kien": [{"loai": a, "gia_tri": b} for a, b in sk],
            "thang_goi_y": _hints(bl),
        }
    if kind == "spin":
        time.sleep(float(payload.get("giay") or 30))
        return {"ok": True}
    if kind == "extract_pdf":
        return _extract_pdf(payload)
    return {"loi": "kind khong ho tro", "kind": kind}


def _extract_pdf(payload):
    path = payload.get("path")
    if not path:
        return {"text": "", "trang_thai": "KHONG_CO_FILE"}
    try:
        from pypdf import PdfReader
        reader = PdfReader(path)
        parts = []
        for page in reader.pages:
            parts.append(page.extract_text() or "")
        text = "\n".join(parts).strip()
        if not text:
            return {"text": "", "trang_thai": "KHONG_TRICH_DUOC"}
        return {"text": text, "trang_thai": "OK"}
    except Exception as ex:
        return {"text": "", "trang_thai": "KHONG_TRICH_DUOC", "ly_do": type(ex).__name__}


def main():
    raw = sys.stdin.read()
    req = json.loads(raw)
    try:
        result = _run(req.get("kind"), req.get("payload") or {})
    except Exception as ex:
        result = {
            "ket_qua": "KHONG_KIEM_DUOC",
            "loai_ket_qua": "KHONG_KIEM_DUOC",
            "trang_thai": "KHONG_KIEM_DUOC",
            "ly_do": "%s: %s" % (type(ex).__name__, ex),
        }
    sys.stdout.write(json.dumps(result, ensure_ascii=False, default=str))


if __name__ == "__main__":
    main()
