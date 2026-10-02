# -*- coding: utf-8 -*-
"""Đường /v1. Mọi việc SymPy đi qua sandbox."""
from fastapi import APIRouter

from app.sandbox import run_sympy_job
from app.schemas import JobIn

v1 = APIRouter(prefix="/v1", tags=["kiem-toan"])


def _payload(body: JobIn) -> dict:
    return body.model_dump(exclude_none=True)


@v1.post("/grade")
def cham_bai(body: JobIn) -> dict:
    data = _payload(body)
    return run_sympy_job("grade", data, timeout=int(data.get("timeout_s") or 12))


@v1.post("/verify")
def kiem_dinh(body: JobIn) -> dict:
    data = _payload(body)
    return run_sympy_job("verify", data, timeout=int(data.get("timeout_s") or 20))


@v1.post("/filter")
def loc_lo_dap_an(body: JobIn) -> dict:
    data = _payload(body)
    return run_sympy_job("filter", data, timeout=int(data.get("timeout_s") or 12))


@v1.post("/generate")
def sinh_bien_the(body: JobIn) -> dict:
    data = _payload(body)
    return run_sympy_job("generate", data, timeout=int(data.get("timeout_s") or 20))


@v1.post("/solve")
def giai_may(body: JobIn) -> dict:
    data = _payload(body)
    return run_sympy_job("solve", data, timeout=int(data.get("timeout_s") or 20))


@v1.post("/goi-y")
def goi_y_thang_mau(body: JobIn) -> dict:
    # Thang gợi ý mẫu Sư phạm theo (bước, loại kết quả, cấp); câu đã điền đi qua bộ lọc lộ đáp án trong sandbox
    data = _payload(body)
    return run_sympy_job("goi_y", data, timeout=int(data.get("timeout_s") or 12))


@v1.post("/kiem-dong-cong-thuc")
def kiem_dong_cong_thuc(body: JobIn) -> dict:
    # ADR 013: core gọi khi khóa bảng công thức; khóa được khi mọi dòng DAT ở tầng 1 và tầng 2
    data = _payload(body)
    return run_sympy_job("kiem_dong_cong_thuc", data, timeout=int(data.get("timeout_s") or 20))


@v1.post("/extract")
def trich_pdf(body: JobIn) -> dict:
    data = _payload(body)
    return run_sympy_job("extract_pdf", data, timeout=20)
