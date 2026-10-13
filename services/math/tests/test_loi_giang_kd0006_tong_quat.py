# -*- coding: utf-8 -*-
"""KD-0006 tiếp: cấu trúc gợi mở, dẫn chiếu và điều kiện tổng quát có căn cứ bảng."""
import json
from pathlib import Path
import re

import pytest

from app.loi_giang import kiem_loi_giang
from tests.test_loi_giang_kd0006 import BANG, _chay


@pytest.mark.parametrize("cau", [
    "Em đã thử giải phương trình đạo hàm bằng 0 chưa?",
    "Em xem quy tắc [4] và [5] trong bảng công thức nhé.",
    "Em hãy dùng quy tắc [4] để nhìn dấu của đạo hàm trên từng khoảng mà kết luận đồng biến hay nghịch biến nhé.",
    "Em hãy đối chiếu [4] để phân biệt khoảng đồng biến, nghịch biến nhé.",
    "Em hãy tra công thức [1] rồi lấy đạo hàm từng hạng tử nhé.",
    "Em thử vận dụng [2] để cộng lại các thành phần nhé.",
    "Em hãy đọc dấu của đạo hàm trong bảng rồi xác định cực đại hay cực tiểu theo [5].",
    "Em nhìn xem đạo hàm đổi dấu theo chiều nào để kết luận cực trị nhé?",
    "Em đã chia được trục số thành các khoảng chưa?",
    "Em thử một giá trị $x$ bất kỳ trong mỗi khoảng để xét dấu nhé.",
    "Giá trị $x$ nào được phép lấy trong tập xác định?",
    "Em xem dấu của đạo hàm thay đổi thế nào qua nghiệm của $y' = 0$?",
    "Em cần xét điều kiện $y' > 0$ hay $y' < 0$ để đọc bảng nhé.",
    "Em cần xét điều kiện $y' = 0$ theo [6] nhé.",
    "Em lập bảng dấu trên các khoảng chia bởi nghiệm, rồi dùng [4] để kết luận đồng biến hay nghịch biến nhé.",
    "Em chia bảng xét dấu ra sao rồi?",
    "Em xem tại điểm đạo hàm bằng 0, dấu đổi theo chiều nào để phân biệt cực đại hay cực tiểu nhé.",
    "Em xem tại điểm đạo hàm triệt tiêu, dấu hai bên ra sao?",
    "Em xem dấu đổi từ dương sang âm hay từ âm sang dương để phân biệt cực đại hay cực tiểu nhé.",
])
def test_cau_goi_mo_theo_cau_truc_duoc_giu(cau):
    kq = _chay(cau)
    assert kq["cau_sach"] == cau
    assert not kq["thay_bang_goi_y"]
    assert all(p["trang_thai"] == "DAT" for p in kq["bieu_thuc"])


@pytest.mark.parametrize("cau", [
    "Em xem [1], đạo hàm của hằng số bằng một đúng không?",
    "Em hãy dùng quy tắc [2]: đạo hàm của tổng bằng tích các đạo hàm nhé.",
    "Em đã thử quy tắc đạo hàm của thương bằng tổng các đạo hàm chưa?",
    "Em hãy tra [4] để nhớ nếu đạo hàm âm thì hàm đồng biến nhé.",
    "Em hãy đối chiếu [5], đạo hàm không đổi dấu thì cực đại phải không?",
    "Em xem [4] để kết luận đạo hàm âm suy ra đồng biến nhé.",
    "Em nhìn xem đạo hàm bằng 0 tại x = 3 chưa?",
    "Đạo hàm bằng không tại ba đúng không?",
    "Em xét điều kiện $y' = 0$ tại $x = 3$ nhé.",
    "Hàm đồng biến trên khoảng (3; +∞) đúng không?",
    "Em hãy xem hàm đồng biến trên khoảng từ ba đến vô cùng nhé.",
    "Em hãy kết luận cực tiểu bằng -5 nhé.",
    "Cực tiểu bằng âm năm theo [5] phải không?",
    "Em xem quy tắc [4] và nhớ hàm đồng biến trên $(3; +\\infty)$ nhé.",
    "Em xét điều kiện $y' > 0$ vì đạo hàm âm thì đồng biến nhé.",
    "Ta có $y' > 0$.",
    "Đạo hàm là $y' < 0$ đúng không?",
    "Em xét điều kiện $y' > 1$ nhé.",
    "Em xét điều kiện $y' > 0 + 0$ nhé.",
    "Em xét điều kiện $x > 0$ nhé.",
    "Em dùng [4] để thử $0$ nhé.",
    "Em dùng [4] để thử 0 nhé.",
    "Em dùng [4] để nhớ đạo hàm dương và hàm nghịch biến nhé.",
    "Em hãy xem đạo hàm đổi dấu theo chiều nào vì đạo hàm của tổng bằng tích nhé?",
    "Em xem đạo hàm đổi dấu vì đạo hàm của hằng số bằng một theo chiều nào nhé?",
    "Em xem tại điểm đạo hàm bằng 0 tại ba nhé.",
    "Em xem tại điểm đạo hàm triệt tiêu tại ba nhé.",
    "Em lập bảng trên các khoảng chia bởi nghiệm là ba nhé.",
    "Em xem quy tắc [4]: dấu đổi từ dương sang âm thì cực tiểu đúng không?",
    "Em đã viết tổng các đạo hàm của tích theo [2] chưa?",
    "Em hãy viết tổng các đạo hàm của thương theo [3] nhé.",
    "Em hãy viết tổng các đạo hàm khi tính đạo hàm của tích nhé.",
    "Em hãy tính đạo hàm của tích: tích đạo hàm phải không?",
    "Em xem tại điểm đạo hàm bằng không, dấu không thay đổi nhé.",
    "Em xem tại điểm đạo hàm bằng 0 không có cực trị theo [6] nhé.",
    "Em xem tại điểm đạo hàm bằng 0 không đồng biến theo [4] nhé.",
    "Em hãy tìm giá trị cực tiểu: mười hai nhé.",
    "Em cần xét điều kiện $y' > 0$ để kết luận nghịch biến nhé.",
    "Em cần xét điều kiện $y' < 0$ và kết luận đồng biến nhé.",
    "Em cần xét điều kiện $y' = 0$ rồi kết luận cực đại nhé.",
    "Em hãy xem đạo hàm của thương cộng lại nhé.",
    "Em cần xét điều kiện $y' > 0$ để kết luận nghịch biến và cực tiểu nhé.",
    "Em cần xét điều kiện $y' > 0$ để kết luận nghịch biến, cực tiểu nhé.",
    "Em xem dấu đổi từ dương sang âm hay từ âm sang dương để kết luận cực tiểu nhé.",
    "Em hãy dùng [3] để cộng lại đạo hàm của thương nhé.",
    "Em hãy viết tổng các đạo hàm đó của tích theo [2] nhé.",
    "Em hãy viết tổng các đạo hàm đó khi tính đạo hàm của tích nhé.",
    "Em hãy viết lại tổng các đạo hàm này để tính đạo hàm của thương theo [3] nhé.",
    "Em đã giải phương trình đạo hàm bằng 0 ra ba chưa?",
])
def test_dan_bang_va_cau_hoi_khong_mien_quan_he_khang_dinh(cau):
    kq = _chay(cau)
    assert kq["cau_sach"] == ""
    assert kq["thay_bang_goi_y"]


@pytest.mark.parametrize("dieu_kien", ["y' = 0", "y' > 0", "y' < 0"])
@pytest.mark.parametrize("doi", ["rỗng", "thiếu mảnh", "thiếu trích dẫn", "sai tầng một"])
def test_dieu_kien_tong_quat_can_dong_bang_dung_duoc(dieu_kien, doi):
    bang = [dict(d) for d in BANG if d["id"] == "d-4"]
    if doi == "rỗng":
        bang = []
    elif doi == "thiếu mảnh":
        bang[0]["latex"] = r"y' \text{ đổi dấu}"
    elif doi == "thiếu trích dẫn":
        bang[0]["trich_dan"] = None
    else:
        bang[0]["latex"] = r"y'=0 \text{ trên } K \Rightarrow \text{nghịch biến trên } K"
        bang[0]["phat_bieu"] = "Nếu đạo hàm bằng 0 trên khoảng K thì hàm nghịch biến trên K."
    assert _chay(f"Em cần xét điều kiện ${dieu_kien}$ nhé.", bang=bang)["cau_sach"] == ""


@pytest.mark.parametrize("ten", ["ban-nhap-glm-2026-10-14.json", "ban-nhap-glm-2026-10-14b.json"])
def test_moi_corpus_bo_toan_bo_khong_qua_hai_muoi_phan_tram(ten):
    corpus = json.loads((Path(__file__).parent / "data" / ten).read_text(encoding="utf-8"))
    bo = 0
    for d in corpus["ban_nhap"]:
        payload = {"cau": d["draft"], "ham": d["ham"], "bang_cong_thuc": corpus["bang_cong_thuc"]}
        kq = kiem_loi_giang(payload)
        assert "loi" not in kq
        assert kq == kiem_loi_giang(payload)
        # Kiểm độc lập với nhãn của cổng: số đứng riêng vẫn là giá trị dù câu dẫn bảng.
        # Điều kiện y'=0 không khớp mẫu số đứng riêng này.
        for c in kq["cac_cau"]:
            if re.search(r"\$\s*[+-]?\d+(?:[.,]\d+)?\s*\$", c["cau"]):
                assert not c["giu"]
        bo += not bool(kq["cau_sach"])
    assert bo / len(corpus["ban_nhap"]) <= 0.20, (ten, bo, len(corpus["ban_nhap"]))
