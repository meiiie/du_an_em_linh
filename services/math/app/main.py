# -*- coding: utf-8 -*-
"""API HTTP. Module này không import SymPy."""
from fastapi import FastAPI
from pydantic import BaseModel

from app.sandbox import run_sympy_job

app = FastAPI(title="Dịch vụ toán — Học toán với AI", version="0.1.0")


class Job(BaseModel):
    model_config = {"extra": "allow"}


@app.get("/health")
def health():
    return {"ok": True, "service": "math"}


@app.post("/v1/grade")
def grade_ep(body: dict):
    return run_sympy_job("grade", body, timeout=int(body.get("timeout_s") or 12))


@app.post("/v1/verify")
def verify_ep(body: dict):
    return run_sympy_job("verify", body, timeout=int(body.get("timeout_s") or 20))


@app.post("/v1/filter")
def filter_ep(body: dict):
    return run_sympy_job("filter", body, timeout=int(body.get("timeout_s") or 12))


@app.post("/v1/generate")
def generate_ep(body: dict):
    return run_sympy_job("generate", body, timeout=int(body.get("timeout_s") or 20))


@app.post("/v1/solve")
def solve_ep(body: dict):
    return run_sympy_job("solve", body, timeout=int(body.get("timeout_s") or 20))


@app.post("/v1/extract")
def extract_ep(body: dict):
    return run_sympy_job("extract_pdf", body, timeout=20)
