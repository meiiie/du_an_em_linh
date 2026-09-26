# -*- coding: utf-8 -*-
"""Kiểm định 3 tầng + cổng phát hành.

Tầng 1: SymPy (kiem_5_buoc), không LLM.
Tầng 2: tìm全文 trên tài liệu đã nạp; bắt buộc trích dẫn, nếu không thì KHONG_KIEM_DUOC.
Tầng 3: đối chiếu kết luận với bảng công thức giáo viên (quy tắc dấu y').
"""
from app.grader import bai_lam_sang_payload, grade

TU_KHOA_TAI_LIEU = ("đồng biến", "nghịch biến", "cực trị", "đạo hàm", "xét dấu")
TU_KHOA_CONG_THUC = ("đồng biến", "nghịch biến", "cực đại", "cực tiểu", "đạo hàm")


def _excerpt(text, term, radius=80):
    i = text.lower().find(term.lower())
    if i < 0:
        return text[:160]
    a = max(0, i - radius)
    b = min(len(text), i + len(term) + radius)
    return text[a:b].strip()


def tang_2(tai_lieu):
    hits = []
    for doc in tai_lieu or []:
        if (doc.get("license_status") or doc.get("quyen") or "") == "chua_ro":
            continue
        text = doc.get("text") or doc.get("noi_dung") or ""
        found = [t for t in TU_KHOA_TAI_LIEU if t in text.lower()]
        if len(found) >= 2:
            hits.append({
                "document_id": doc.get("id"),
                "phien_ban": doc.get("phien_ban") or doc.get("version") or 1,
                "cum_tu": found,
                "trich": _excerpt(text, found[0]),
            })
    if not hits:
        return {
            "tang": 2,
            "trang_thai": "KHONG_KIEM_DUOC",
            "ly_do": "Không có đoạn tài liệu trích dẫn được cho phương pháp xét dấu / đơn điệu.",
            "trich_dan": [],
        }
    return {"tang": 2, "trang_thai": "DAT", "ly_do": "Có trích dẫn tài liệu.", "trich_dan": hits[:3]}


def tang_3(cong_thuc, ket_qua_tang_1):
    khop = []
    for ct in cong_thuc or []:
        blob = " ".join([ct.get("latex") or "", ct.get("noi_dung") or "", ct.get("ten") or ""]).lower()
        found = [t for t in TU_KHOA_CONG_THUC if t in blob]
        if found:
            khop.append({"formula_id": ct.get("id"), "cum_tu": found})
    if not khop:
        return {
            "tang": 3,
            "trang_thai": "KHONG_KIEM_DUOC",
            "ly_do": "Bảng công thức chưa có quy tắc đơn điệu hoặc cực trị để đối chiếu.",
            "cong_thuc": [],
        }
    if ket_qua_tang_1 == "SAI":
        return {
            "tang": 3,
            "trang_thai": "SAI",
            "ly_do": "Lời giải mâu thuẫn với quy tắc dấu của đạo hàm trong bảng công thức.",
            "cong_thuc": khop[:5],
        }
    if ket_qua_tang_1 == "KHONG_KIEM_DUOC":
        return {
            "tang": 3,
            "trang_thai": "KHONG_KIEM_DUOC",
            "ly_do": "Tầng 1 chưa kết luận được nên chưa đối chiếu quy tắc.",
            "cong_thuc": khop[:5],
        }
    return {
        "tang": 3,
        "trang_thai": "DAT",
        "ly_do": "Kết luận khớp quy tắc trong bảng công thức.",
        "cong_thuc": khop[:5],
    }


def cong_phat_hanh(tang):
    states = [t["trang_thai"] for t in tang]
    if any(s == "SAI" for s in states):
        return "BI_CHAN", "SAI"
    if any(s == "KHONG_KIEM_DUOC" for s in states):
        return "CHO_GIAO_VIEN_DUYET", "KHONG_KIEM_DUOC"
    if states and all(s == "DAT" for s in states):
        return "DA_PHAT_HANH", "DAT"
    return "CHO_GIAO_VIEN_DUYET", "KHONG_KIEM_DUOC"


def verify(payload):
    if payload.get("payload_cham"):
        gp = payload["payload_cham"]
    elif payload.get("bai_lam"):
        gp = bai_lam_sang_payload(payload["bai_lam"], payload.get("ham"))
    else:
        t1 = {"tang": 1, "trang_thai": "KHONG_KIEM_DUOC", "ly_do": "Chưa có lời giải cấu trúc 5 bước để máy tự kiểm."}
        t2 = tang_2(payload.get("tai_lieu") or [])
        t3 = tang_3(payload.get("cong_thuc") or [], "KHONG_KIEM_DUOC")
        tang = [t1, t2, t3]
        phat_hanh, tong = cong_phat_hanh(tang)
        return {"trang_thai_tong": tong, "trang_thai_phat_hanh": phat_hanh, "tang": tang, "phien_ban_chuan_hoa": "norm-0.1"}
    g = grade(gp)
    t1_state = g["ket_qua"]
    t1 = {
        "tang": 1,
        "trang_thai": t1_state,
        "loai_ket_qua": g.get("loai_ket_qua"),
        "buoc_sai": g.get("buoc_sai"),
        "ma_loi": g.get("ma_loi"),
        "do_tin_cay": g.get("do_tin_cay"),
        "ly_do": g.get("thong_bao"),
    }
    t2 = tang_2(payload.get("tai_lieu") or [])
    t3 = tang_3(payload.get("cong_thuc") or [], t1_state)
    tang = [t1, t2, t3]
    phat_hanh, tong = cong_phat_hanh(tang)
    return {
        "trang_thai_tong": tong,
        "trang_thai_phat_hanh": phat_hanh,
        "tang": tang,
        "phien_ban_chuan_hoa": g.get("phien_ban_chuan_hoa"),
    }
