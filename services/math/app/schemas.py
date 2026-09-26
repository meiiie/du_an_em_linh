# -*- coding: utf-8 -*-
"""Mô hình Pydantic cho API. Tiến trình HTTP không import SymPy."""
from pydantic import BaseModel, ConfigDict, Field


class HealthOut(BaseModel):
    ok: bool
    service: str
    version: str


class JobIn(BaseModel):
    model_config = ConfigDict(extra="allow")
    timeout_s: int | None = Field(default=None, ge=1, le=60)


class GradeOut(BaseModel):
    model_config = ConfigDict(extra="allow")
    ket_qua: str | None = None
    loai_ket_qua: str | None = None
    thong_bao: str | None = None


class FilterOut(BaseModel):
    model_config = ConfigDict(extra="allow")
    cho_phep: bool | None = None
    lop_chinh: str | None = None
    lop_phu: str | None = None


class VerifyOut(BaseModel):
    model_config = ConfigDict(extra="allow")
    trang_thai_tong: str | None = None
    trang_thai_phat_hanh: str | None = None
