# -*- coding: utf-8 -*-
"""API HTTP. Module này không import SymPy."""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routers import v1
from app.schemas import HealthOut

app = FastAPI(
    title="Dịch vụ toán — MathL+",
    version="0.1.0",
    summary="Chấm 5 bước, kiểm định 3 tầng, lọc lộ đáp án, sinh biến thể.",
)
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://127.0.0.1:3000",
        "http://localhost:3000",
    ],
    allow_methods=["GET", "POST"],
    allow_headers=["content-type"],
)
app.include_router(v1)


@app.get("/health")
def health() -> HealthOut:
    return HealthOut(ok=True, service="math", version="0.1.0")
