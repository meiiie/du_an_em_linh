# Xuất tệp vàng trạng thái phát hành của v0 cho test core (#85): cong_phat_hanh của services/math/app/verify.py chạy
# trên 64 tổ hợp trạng thái tầng 1–3 (DAT, SAI, KHONG_KIEM_DUOC, THIEU = tầng không có trong phản hồi).
# Chạy từ gốc repo: python specs/001-lat-cat-doc/doi-chieu/cong-phat-hanh-v0.py
#   → services/core/src/test/resources/content/cong-phat-hanh-v0.csv (VerificationRunTest so với tệp này).
import io
import itertools
import pathlib
import subprocess
import sys

GOC = pathlib.Path(__file__).resolve().parents[3]
sys.path.insert(0, str(GOC / "services" / "math"))
from app.verify import cong_phat_hanh  # noqa: E402

blob = subprocess.check_output(["git", "rev-parse", "HEAD:services/math/app/verify.py"], cwd=GOC, text=True).strip()
TRANG_THAI = ["DAT", "SAI", "KHONG_KIEM_DUOC", "THIEU"]
dong = [
    f"# Tệp vàng: cong_phat_hanh của services/math/app/verify.py (blob {blob}) trên 64 tổ hợp trạng thái tầng 1–3.",
    "# THIEU = tầng không có trong phản hồi. Hai cột cuối là kết quả của v0 (trạng thái phát hành, trạng thái tổng).",
    "# Sinh bằng specs/001-lat-cat-doc/doi-chieu/cong-phat-hanh-v0.py; không sửa tay.",
    "tang1,tang2,tang3,phat_hanh_v0,tong_v0",
]
for to_hop in itertools.product(TRANG_THAI, repeat=3):
    tang = [{"tang": i + 1, "trang_thai": s} for i, s in enumerate(to_hop) if s != "THIEU"]
    phat_hanh, tong = cong_phat_hanh(tang)
    dong.append(",".join([*to_hop, phat_hanh, tong]))
ra = GOC / "services" / "core" / "src" / "test" / "resources" / "content" / "cong-phat-hanh-v0.csv"
ra.parent.mkdir(parents=True, exist_ok=True)
io.open(ra, "w", encoding="utf-8", newline="\n").write("\n".join(dong) + "\n")
print(f"đã ghi {len(dong) - 4} tổ hợp vào {ra.relative_to(GOC)}")
