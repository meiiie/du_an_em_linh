# -*- coding: utf-8 -*-
"""Mô hình Pydantic cho API. Tiến trình HTTP không import SymPy."""
from pydantic import BaseModel, ConfigDict, Field, model_validator


class HealthOut(BaseModel):
    ok: bool
    service: str
    version: str


class JobIn(BaseModel):
    # Payload job vẫn là dict tự do (grade/verify/filter), nhưng timeout bị chặn trần 20 s (F-01/B-21).
    model_config = ConfigDict(extra="allow")
    timeout_s: int | None = Field(default=None, ge=1, le=20)


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


class SoHangIn(BaseModel):
    """Đối chiếu expected, hoặc đạo hàm của function; không trả biểu thức đối chiếu."""
    model_config = ConfigDict(extra="forbid")
    expected: str | None = Field(default=None, min_length=1, max_length=300)
    function: str | None = Field(default=None, min_length=1, max_length=300)
    student: str = Field(min_length=1, max_length=300)
    variable: str = Field(default="x", pattern=r"^[a-df-z]$")
    timeout_s: int = Field(default=4, ge=1, le=4)

    @model_validator(mode="after")
    def one_reference(self):
        if (self.expected is None) == (self.function is None):
            raise ValueError("Cần đúng một biểu thức đối chiếu hoặc hàm để lấy đạo hàm.")
        return self
