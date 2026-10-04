"""Tệp vàng của job khóa bảng công thức (T014, Codex #135): phản hồi thật của kiem_dong_cong_thuc cho bảng 6 dòng của v0 và
kho 5 tài liệu của lớp, gửi đúng như importer v2 gửi (NhapTheoLop.khoaBang):

- tài liệu theo thứ tự importer nạp: 3 tài liệu của data/v0/tai-lieu.json rồi sp-tai-lieu-0001, sp-tai-lieu-0002;
- văn bản chuẩn hóa NFC, chia mỗi câu một đoạn như ChiaDoan (cắt sau «.» hay «;» và khoảng trắng, bỏ khoảng trắng hai đầu);
- dòng: id là mã (d-1…), tiêu đề, LaTeX, phát biểu của data/v0/bang-cong-thuc.json.

Id trong tệp vàng ổn định: tài liệu là mã, đoạn là «mã#vị trí» (vị trí của đoạn trong tài liệu, từ 0). Dịch vụ toán giả của
test đổi chúng sang id thật của lần nhập. Script dừng nếu services/math, data/v0 hay data/supham có thay đổi chưa commit.

Chạy từ gốc repo:  uv run --project services/math python specs/001-lat-cat-doc/doi-chieu/khoa-bang-v0.py
"""

import io
import json
import os
import re
import subprocess
import sys
import unicodedata

GOC = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
RA = os.path.join(os.path.dirname(os.path.abspath(__file__)), "khoa-bang-v0.json")


def _git(*args):
    return subprocess.run(["git", "-C", GOC, *args], check=True, capture_output=True, text=True).stdout.strip()


def _doc(duong_dan):
    return json.load(io.open(os.path.join(GOC, duong_dan), encoding="utf-8"))


def main():
    for thu_muc in ("services/math", "data/v0", "data/supham"):
        if _git("status", "--porcelain", "--", thu_muc):
            sys.exit("Dừng: %s có thay đổi chưa commit" % thu_muc)
    sys.path.insert(0, os.path.join(GOC, "services", "math"))
    from app.dong_cong_thuc import kiem_dong_cong_thuc

    nguon = _doc("data/v0/tai-lieu.json") + [_doc("data/supham/tai-lieu/%s.json" % m) for m in ("sp-tai-lieu-0001", "sp-tai-lieu-0002")]
    tai_lieu = []
    for t in nguon:
        van_ban = unicodedata.normalize("NFC", t["textContent"])
        cau = [c.strip() for c in re.split(r"(?<=[.;])\s+", van_ban) if c.strip()]
        tai_lieu.append({"id": t["ma"], "ten": t["title"],
                         "doan": [{"id": "%s#%d" % (t["ma"], i), "trang": None, "text": c} for i, c in enumerate(cau)],
                         "license_status": t["licenseStatus"]})
    dong = [{"id": f["ma"], "tieu_de": f["title"], "latex": f["latex"], "phat_bieu": f["noiDung"]}
            for f in _doc("data/v0/bang-cong-thuc.json")["formulas"]]
    kq = kiem_dong_cong_thuc({"dong": dong, "tai_lieu": tai_lieu})
    vang = {
        "nguon": {
            "services_math": _git("rev-parse", "HEAD:services/math"),
            "data_v0": _git("rev-parse", "HEAD:data/v0"),
            "data_supham": _git("rev-parse", "HEAD:data/supham"),
        },
        # Đoạn đã gửi, để test kiểm importer chia đoạn trùng script (lệch thì id «mã#vị trí» không còn đúng đoạn).
        "doan": {d["id"]: d["text"] for t in tai_lieu for d in t["doan"]},
        "phan_hoi": kq,
    }
    io.open(RA, "w", encoding="utf-8", newline="\n").write(json.dumps(vang, ensure_ascii=False, indent=2) + "\n")
    print("Đã ghi %s: %d dòng, %d đoạn" % (os.path.relpath(RA, GOC), len(kq["dong"]), len(vang["doan"])))


if __name__ == "__main__":
    main()
