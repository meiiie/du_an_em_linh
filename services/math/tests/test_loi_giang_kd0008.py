# -*- coding: utf-8 -*-
"""KD-0008: nhãn thủ công; câu hỏi/kế hoạch không được bao che kết quả cụ thể."""
from copy import deepcopy
from pathlib import Path
import unicodedata

import pytest

from app.loi_giang import kiem_loi_giang
from tests.test_loi_giang_kd0006 import BANG, HAM


TSV = Path(__file__).parent / "data" / "tu-choi-glm-2026-10-14c.tsv"
# Phân loại độc lập, không dùng regex/bộ phân loại sản phẩm làm oracle.
# Hiện không giữ từ chối câu nào: tất cả chỉ giao thao tác/hỏi kế hoạch.
AN_TOAN = {
    "Em sẽ giải phương trình đạo hàm bằng không hay kiểm tra nơi đạo hàm không xác định trước, và em định làm cách nào để tìm các giá trị đó?": "điều kiện chung qua bảng, không cho nghiệm",
    "Em định đặt đạo hàm bằng không và giải như thế nào để tìm mốc xét dấu?": "đặt điều kiện chung qua bảng, hỏi cách giải",
    "Em hãy đặt đạo hàm em vừa tính bằng không và tự giải phương trình tìm nghiệm.": "yêu cầu tự giải, không xác nhận đạo hàm",
    "Giữa các mốc, em dự định kiểm tra dấu của đạo hàm bằng cách nào, chẳng hạn thử chọn một giá trị x thay vào đạo hàm?": "biến x trong vai trò giá trị thử, không cho giá trị",
    "Em định dựa vào dấu của đạo hàm trên mỗi khoảng giữa các mốc theo quy tắc đơn điệu [4] như thế nào, em thử chọn một khoảng và kiểm tra xem?": "khoảng chưa chọn, tham chiếu quy tắc",
    "Em thử chọn một giá trị đại diện trong mỗi khoảng và tự kiểm tra nhé.": "một là định từ, chưa cho điểm thử",
    "Giữa hai mốc đã tìm, em chọn dòng [4] trong bảng công thức để suy ra chiều biến thiên từ dấu của đạo hàm nhé?": "hai mốc tham chiếu, không cho vị trí",
    "Em sẽ dựa vào dòng [4] trong bảng công thức để suy ra chiều biến thiên của $y = x^{2} - 6 x + 4$ từ dấu đạo hàm trên từng khoảng nhé?": "trích đúng hàm của đề, giao đọc chiều biến thiên",
    "Em dự định xác định dấu của đạo hàm trên mỗi khoảng bằng cách nào, chọn một giá trị đại diện trong khoảng đó hay dựa vào hệ số bậc hai?": "hỏi phương pháp, chưa cho dấu/hệ số",
    "Em hãy chọn một điểm thử trong từng khoảng giữa hai mốc rồi thay vào đạo hàm để xác định dấu, sau đó đối chiếu với quy tắc đơn điệu [4].": "điểm thử chưa điền, chuỗi thao tác",
    "Em sẽ đọc chiều biến thiên trên từng khoảng của bảng theo quy tắc đơn điệu [4] và nhận cực trị theo chiều đổi dấu [5] như thế nào?": "gọi loại cần tìm, chưa chọn kết luận",
    "Dựa vào bảng xét dấu đã có, em sẽ dùng quy tắc đơn điệu [4] và quy tắc cực trị [5] để đọc ra khoảng đồng biến, nghịch biến và cực trị nhé?": "danh mục kết luận là bổ ngữ hành động",
    "Dựa vào bảng xét dấu đã có, em sẽ đọc ra chiều biến thiên trên từng khoảng theo dòng [4] và xác định cực trị theo chiều đổi dấu ở dòng [5] như thế nào?": "đọc bảng, không nêu chiều/loại cụ thể",
    "Em định dựa vào dấu của đạo hàm ở từng khoảng để suy ra chiều biến thiên như thế nào nào?": "hỏi cách làm, lặp từ nào không thay ý",
    "Sau đó em dùng quy tắc cực trị [5] để xác định loại cực trị tại điểm đạo hàm đổi dấu nhé.": "điều kiện chung, chưa chọn loại/vị trí cực trị",
    "Em thử tính lại đạo hàm từng hạng tử của $y = - x^{2} - 2 x + 5$ theo dòng [1], rồi ghép lại bằng dòng [2], xem kết quả có còn như vậy không?": "trích đúng hàm, học sinh tự kiểm không xác nhận đúng",
    'Em đã dùng dòng [1] cho từng hạng tử lũy thừa và dòng [2] cho tổng chưa, hay em mới áp dụng cho hạng tử nào?': 'hỏi dòng đã dùng/hạng tử cần áp dụng, không cho đạo hàm',
    'Mình cùng em tập trung hoàn thành bước tính đạo hàm nhé.': 'nhắc tập trung bước đang làm, không cho kết quả',
    'Mình cùng em tập trung vào bước tính đạo hàm nhé, còn kết quả cụ thể em sẽ tự kiểm tra lại sau khi làm xong.': 'yêu cầu học sinh tự kiểm tra sau khi làm, không xác nhận đúng',
    'Mình cùng em tập trung hoàn thành bước tính đạo hàm trước nhé.': 'nhắc tập trung bước trước, không cho kết quả',
    'Em xem hàm $y = - x^{2} - 2 x + 5$ có phép toán nào như phân thức hay căn khiến giá trị không xác định tại một số điểm nào không?': 'hỏi tồn tại hạn chế miền xác định, không khẳng định hàm có căn/mẫu',
    'Em dự định chọn dòng [1] để tính đạo hàm từng hạng tử và dòng [2] để áp dụng cho tổng của hàm $y = - x^{2} - 2 x + 5$ chứ?': 'chia thao tác đạo hàm theo dòng, trích đúng hàm',
    'Em dựa vào dòng [4] để đọc khoảng đồng biến, nghịch biến từ bảng xét dấu và dòng [5] để nhận cực trị từ chiều đổi dấu của đạo hàm nhé?': 'tham chiếu bảng và gọi nhóm kết luận chưa điền',
    'Em định dùng dòng [4] để kết luận khoảng đồng biến, nghịch biến và dòng [5] để đọc ra cực trị từ bảng xét dấu đã có như thế nào?': 'kết luận là động từ, khoảng/loại chưa được chọn',
    'Mình cùng em tập trung vào bước tính đạo hàm nhé.': 'nhắc tập trung bước đang làm, không cho kết quả',
    'Em dùng dòng [4] để đọc khoảng đồng biến, nghịch biến từ dấu đạo hàm, và dòng [5] để nhận xét chiều đổi dấu ở mỗi mốc nhé?': 'hỏi chiều đổi dấu ở các mốc, không cho dấu',
    'Em dựa vào dòng [4] để đọc khoảng đồng biến, nghịch biến và dòng [5] để nhận dạng cực trị từ bảng xét dấu đã làm, em thấy kết luận của mình là gì?': 'học sinh tự đọc bảng và nêu kết luận',
    'Em dựa vào dòng [4] để đọc các khoảng đồng biến, nghịch biến và dòng [5] để đọc cực trị từ bảng xét dấu đã làm, em dự định viết kết luận theo trình tự nào?': 'hỏi trình tự viết kết luận, không cho khoảng/cực trị',
    'Em định dùng dòng [4] để đọc khoảng đồng biến, nghịch biến và dòng [5] để nhận loại cực trị ở mỗi mốc từ bảng xét dấu như thế nào?': 'hỏi cách nhận loại tại mỗi mốc, không xác định vị trí/loại',
    'Em định dựa vào dòng [4] để viết khoảng đồng biến, nghịch biến và dòng [5] để chỉ ra cực trị từ bảng xét dấu như thế nào?': 'yêu cầu viết nhóm khoảng/chỉ ra cực trị, không cho kết quả',
    'Em xem hàm $y = - x^{2} - 2 x + 5$ có chứa phép chia hay căn nào hạn chế giá trị của $x$ không?': 'hỏi điều kiện theo đề, x chỉ là tên biến',
    'Em dùng dòng [4] để đọc khoảng đồng biến, nghịch biến từ dấu đạo hàm trong bảng, rồi dựa vào dòng [5] để xác định loại cực trị tại mỗi mốc nhé?': 'đọc nhóm khoảng và tìm loại cực trị ở mỗi mốc',
    'Sau đó em dựa vào dòng [5] để nhận xét dấu của đạo hàm đổi thế nào tại mốc và tự kết luận loại cực trị nhé?': 'hỏi chiều đổi dấu rồi tự kết luận loại, không cho chiều/loại',
}
# Mọi câu giữ từ chối phải liệt kê nguyên văn với lý do ở đây.
GIU_TU_CHOI = {}


def _doc_tsv():
    return [(i, *line.split("\t", 1)) for i, line in enumerate(TSV.read_text(encoding="utf-8").splitlines(), 1) if line.strip()]


def _chay(cau, bang=BANG, ham=HAM, bai_lam=()):
    return kiem_loi_giang({"cau": cau, "bang_cong_thuc": list(bang), "ham": ham,
                          "bai_lam_hoc_sinh": list(bai_lam)})


@pytest.mark.parametrize("so,buoc,cau", _doc_tsv(), ids=lambda v: str(v)[:70])
def test_moi_cau_tsv_co_nhan_va_phan_quyet(so, buoc, cau):
    assert buoc in {"B.DH.TXD", "B.DH.DAOHAM", "B.DH.NGHIEM", "B.DH.XETDAU", "B.DH.KETLUAN"}
    assert (cau in AN_TOAN) != (cau in GIU_TU_CHOI), f"Dòng {so} cần Kiểm định gán nhãn/lý do: {cau}"
    ham = "-x**2-2*x+5" if "$y = - x^{2} - 2 x + 5$" in cau else HAM
    kq = _chay(cau, ham=ham)
    assert "loi" not in kq
    assert kq["cau_sach"] == (cau if cau in AN_TOAN else ""), (so, cau, kq)
    assert kq["thay_bang_goi_y"] is (cau in GIU_TU_CHOI)


HANH_DONG = [
    "đặt đạo hàm bằng không và tự giải phương trình tìm nghiệm",
    "giải phương trình đạo hàm bằng không",
    "chọn một giá trị đại diện trong mỗi khoảng và tự kiểm tra",
    "chọn một điểm thử trong từng khoảng giữa hai mốc rồi thay vào đạo hàm để xác định dấu",
    "dựa vào dấu của đạo hàm ở từng khoảng để suy ra chiều biến thiên",
    "đọc ra khoảng đồng biến, nghịch biến và cực trị",
    "xác định chiều biến thiên trên từng khoảng",
    "dùng quy tắc đơn điệu [4] và quy tắc cực trị [5] để đọc ra khoảng đồng biến, nghịch biến và cực trị",
    "tính lại đạo hàm từng hạng tử của $y = x^{2} - 6 x + 4$ theo dòng [1], rồi ghép lại bằng dòng [2]",
]


@pytest.mark.parametrize("hanh_dong", HANH_DONG)
@pytest.mark.parametrize("khung", ["Em định {} như thế nào?", "Em sẽ {} nhé?", "Em dự định {} bằng cách nào?", "Em thử {} nhé."])
def test_ke_hoach_va_bo_ngu_chua_dien_duoc_giu(hanh_dong, khung):
    cau = khung.format(hanh_dong)
    kq = _chay(cau)
    assert kq["cau_sach"] == cau, kq
    assert kq["thay_bang_goi_y"] is False
    assert all(p["trang_thai"] == "DAT" for p in kq["bieu_thuc"])


# Mỗi lõi độc lập cho sẵn nghiệm, dấu, vị trí, biểu thức hoặc xác nhận.
LOI_LO = [
    "thấy đạo hàm bằng không tại x bằng ba",
    "kết luận hàm đồng biến trên khoảng từ âm vô cùng đến ba",
    "kết luận đạo hàm đổi dấu từ dương sang âm nên có cực đại",
    "chọn một giá trị đại diện bằng ba trong mỗi khoảng",
    "chọn một giá trị đại diện là âm một",
    "chọn một điểm thử tại ba",
    "chọn điểm thử là hai",
    "chọn một khoảng từ hai đến ba",
    "xét mỗi khoảng từ âm vô cùng đến ba",
    "đọc chiều biến thiên giữa hai mốc âm một và ba",
    "đọc chiều biến thiên giữa hai mốc là một và ba",
    "đặt đạo hàm bằng không rồi ghi nghiệm ba",
    "giải phương trình đạo hàm bằng không để tìm nghiệm là ba",
    "đặt đạo hàm em vừa tính bằng không tại x bằng ba",
    "xác định dấu của đạo hàm là dương",
    "xác định dấu của đạo hàm âm trên khoảng này",
    "suy ra chiều biến thiên là đồng biến",
    "đọc ra khoảng đồng biến từ ba đến vô cùng",
    "đọc ra khoảng nghịch biến từ âm vô cùng đến ba",
    "xác định loại cực trị là cực đại",
    "xác định loại cực trị tại ba",
    "xác định cực đại tại mốc bên trái",
    "xác định cực tiểu tại mốc bên phải",
    "xác định cực đại tại điểm đầu tiên",
    "xác định cực tiểu tại điểm thứ hai",
    "xác định cực đại tại mốc này",
    "kết luận hàm đồng biến trên khoảng đó",
    "kết luận hàm nghịch biến trên khoảng bên trái",
    "dựa vào hệ số bậc hai âm nên hàm đạt cực đại",
    "dựa vào hệ số bậc hai dương nên đạo hàm dương",
    "dựa vào hệ số bậc hai bằng âm một",
    "dựa vào hệ số bậc hai là hai",
    "tính đạo hàm là hai x trừ sáu",
    "tính đạo hàm của x bình trừ sáu x",
    "rút gọn x mũ hai cộng hai x",
    "giữ kết quả vì em làm đúng rồi",
    "đối chiếu vì bài làm của em đúng",
    "tự kiểm tra, kết quả của em chính xác",
    "giải phương trình rồi xác nhận nghiệm của em đúng",
    "đặt đạo hàm bằng không vì nghiệm là 3",
    "đặt đạo hàm bằng không tại $x=3$",
    "ghi $y'=2x-6$",
    "đọc ra khoảng đồng biến $(3;+\\infty)$",
    "đọc ra cực tiểu tại $x=3$",
    "dựa vào quy tắc đơn điệu [4] để ghi dương trên khoảng này",
    "dùng quy tắc cực trị [5] để ghi cực đại tại ba",
    "đọc chiều biến thiên của $y = x^{2} - 6 x + 4$ là đồng biến trên khoảng đó",
    "tính đạo hàm của $y = x^{2} - 6 x + 4$ là hai x trừ sáu",
    "dựa vào bảng xét dấu đã có để ghi dấu cộng",
    "dựa vào bảng xét dấu đã có để ghi dấu trừ",
    "chọn một điểm thử bằng không",
    "chọn một giá trị đại diện bằng hai phẩy năm",
    "giữa hai mốc đã tìm kết luận hàm đồng biến",
    "đặt đạo hàm bằng không nên có cực tiểu tại ba",
    "dùng dòng [4] để suy ra hàm đồng biến trên mọi khoảng",
    "nhận cực trị theo chiều đổi dấu từ âm sang dương tại ba",
    "đọc chiều biến thiên là giảm trên từng khoảng",
    "chọn một khoảng rồi ghi không có nghiệm",
    "tính lại đạo hàm từng hạng tử rồi ghép lại bằng hai x trừ sáu",
    "chọn một giá trị đại diện trong mỗi khoảng rồi ghi hai",
    "chọn một điểm thử trong mỗi khoảng rồi ghi x bằng ba",
    "chọn một khoảng và xác nhận dấu em điền đúng rồi",
    "xác định cực đại tại nghiệm bên trái",
    "xác định cực tiểu tại điểm vừa tìm",
    "xác định cực trị tại mốc thứ nhất",
    "chọn giá trị đại diện là số âm một",
    "chọn giá trị đại diện bằng pi",
    "chọn giá trị đại diện là âm một",
    "chọn giá trị đại diện là một nửa",
    "chọn giá trị đại diện dương",
    "chọn giá trị đại diện không âm",
]
KHUNG_LO = ["Em có thấy {} không?", "Em định {} như thế nào?", "Em sẽ {} nhé?", "Em dự định {} bằng cách nào?"]


@pytest.mark.parametrize("loi", LOI_LO)
@pytest.mark.parametrize("khung", KHUNG_LO)
@pytest.mark.parametrize("bang", [(), BANG], ids=["bang-rong", "bang-khoa"])
def test_doi_khang_khong_lo_trong_cau_hoi_ke_hoach(loi, khung, bang):
    cau = khung.format(loi)
    kq = _chay(cau, bang)
    assert "loi" not in kq, (cau, kq)
    assert kq["cau_sach"] == "", (cau, kq)
    assert kq["thay_bang_goi_y"] is True
    assert all(not c["giu"] for c in kq["cac_cau"]), (cau, kq)


@pytest.mark.parametrize("ham", ["", "x**2-6*x+5", "4-6*x+x**2", "2*x-6", "(x**2-6*x+4)/(x-1)"])
def test_trich_ham_phai_khop_toan_bo_de_khong_chi_tu_mau(ham):
    cau = "Em sẽ dựa vào dòng [4] trong bảng công thức để suy ra chiều biến thiên của $y = x^{2} - 6 x + 4$ từ dấu đạo hàm trên từng khoảng nhé?"
    assert _chay(cau, ham=ham)["cau_sach"] == ""


@pytest.mark.parametrize("truong", ["rong", "thieu-trich-dan", "sai", "khong-co-manh"])
def test_dieu_kien_bang_khong_can_bang_dung_duoc(truong):
    bang = deepcopy(BANG)
    if truong == "rong":
        bang = []
    elif truong == "thieu-trich-dan":
        for d in bang:
            d.pop("trich_dan", None)
    elif truong == "sai":
        bang = [{"id": "sai", "latex": "(u+v)'=u'v'", "trich_dan": {"tai_lieu": "tong-hop"}}]
    else:
        bang = [bang[1]]
    assert _chay("Em định đặt đạo hàm bằng không và giải như thế nào để tìm mốc xét dấu?", bang)["cau_sach"] == ""


@pytest.mark.parametrize("cau", [
    "Em định đặt đạo hàm bằng không và giải như thế nào để tìm mốc xét dấu?",
    "Em thử chọn một giá trị đại diện trong mỗi khoảng và tự kiểm tra nhé.",
    "Em sẽ đọc ra khoảng đồng biến, nghịch biến và cực trị nhé?",
])
def test_nfc_khong_thay_phan_quyet(cau):
    assert _chay(unicodedata.normalize("NFD", cau))["cau_sach"] == cau


def test_khong_bao_che_cau_ke_tiep_va_loi_dau_vao():
    an_toan = "Em sẽ chọn một giá trị đại diện trong mỗi khoảng nhé?"
    lo = "Đạo hàm bằng không tại x bằng ba nhé?"
    kq = _chay(an_toan + " " + lo)
    assert kq["cau_sach"] == an_toan
    assert [c["giu"] for c in kq["cac_cau"]] == [True, False]
    assert kiem_loi_giang({"cau": an_toan * 200})["cau_sach"] == ""


# Chèn một mệnh đề lộ vào chính các câu live, trước dấu kết câu: cùng một
# câu không được hưởng ngoại lệ từ phần gợi mở/trích dẫn đứng trước.
@pytest.mark.parametrize("cau", list(AN_TOAN))
@pytest.mark.parametrize("chen", [
    ", nghiệm là ba", ", đạo hàm dương trên khoảng đó",
    ", hàm đồng biến trên khoảng bên trái", ", cực đại tại mốc bên phải",
    ", hệ số bậc hai âm nên hàm đạt cực đại", ", bài làm của em đúng rồi",
])
def test_chen_lo_trong_chinh_cau_live(cau, chen):
    chen_cau = cau.rstrip(".?!") + chen + " nhé?"
    ham = "-x**2-2*x+5" if "$y = - x^{2} - 2 x + 5$" in cau else HAM
    assert _chay(chen_cau, ham=ham)["cau_sach"] == "", chen_cau


@pytest.mark.parametrize("trich", [
    "$x=3$", "$y'=2x-6$", "$y=x^2-6x+5$", "$y=4-6x+x^2$",
    "$y=x^2-6x+4", "$y=x^{2-6x+4$", "$y=x^2-6x+4$ và $x=3$",
])
def test_trich_hong_hoac_ket_qua_khong_duoc_che(trich):
    cau = f"Em sẽ tính lại đạo hàm từng hạng tử của {trich} theo dòng [1] nhé?"
    assert _chay(cau)["cau_sach"] == "", cau


@pytest.mark.parametrize("cau", [
    "Em định đặt đạo hàm bằng không tại x bằng ba như thế nào?",
    "Em sẽ chọn giá trị đại diện là âm một nhé?",
    "Em sẽ xác định cực đại tại mốc bên trái nhé?",
    "Em sẽ dựa vào hệ số bậc hai âm nên hàm đạt cực đại nhé?",
])
@pytest.mark.parametrize("bien_doi", [
    lambda s: unicodedata.normalize("NFD", s),
    lambda s: s.replace("ba", "b\u200ba").replace("âm", "â\u200bm"),
    lambda s: "".join(c for c in unicodedata.normalize("NFD", s.replace("đ", "d").replace("Đ", "D")) if unicodedata.category(c) != "Mn"),
])
def test_lo_unicode_va_khong_dau_van_bi_chan(cau, bien_doi):
    assert _chay(bien_doi(cau))["cau_sach"] == ""


@pytest.mark.parametrize("gia_tri", ["e", "logarit của hai", "trừ một", "vô cùng", "căn ba", "một phần hai", "pi", "số âm một", "lớn hơn ba", "nhỏ hơn ba"])
@pytest.mark.parametrize("doi_tuong", ["giá trị đại diện", "điểm thử"])
@pytest.mark.parametrize("gan", [" bằng ", " "])
def test_gan_diem_thu_phai_chan_moi_ve_phai(doi_tuong, gia_tri, gan):
    cau = f"Em sẽ chọn {doi_tuong}{gan}{gia_tri} nhé?"
    assert _chay(cau)["cau_sach"] == "", cau


@pytest.mark.parametrize("cau", [
    "Em có thấy đạo hàm bằng không tại x bằng ba không?",
    "Em định kết luận hàm đồng biến trên khoảng từ âm vô cùng đến ba à?",
    "Đạo hàm đổi dấu từ dương sang âm nên có cực đại nhé?",
])
def test_vi_du_doi_khang_nguyen_van(cau):
    assert _chay(cau)["cau_sach"] == ""


@pytest.mark.parametrize("doi_tuong", ["khoảng đồng biến", "khoảng nghịch biến", "cực trị", "chiều biến thiên"])
@pytest.mark.parametrize("hanh_dong", ["tìm", "xác định", "đọc ra"])
def test_ten_nhom_la_bo_ngu_chua_chon_ket_qua(doi_tuong, hanh_dong):
    cau = f"Em định {hanh_dong} {doi_tuong} như thế nào?"
    assert _chay(cau)["cau_sach"] == cau


@pytest.mark.parametrize("loai", ["khoảng đồng biến", "khoảng nghịch biến"])
@pytest.mark.parametrize("khoang", ["khoảng đó", "một khoảng", "từng khoảng", "mỗi khoảng"])
def test_khong_chon_don_dieu_cho_mot_khoang(loai, khoang):
    cau = f"Em sẽ kết luận {loai} trên {khoang} nhé?"
    assert _chay(cau)["cau_sach"] == "", cau


@pytest.mark.parametrize("thao_tac", ["giải phương trình", "giải như thế nào", "tính đạo hàm từng hạng tử"])
def test_chuoi_dai_khong_quay_lui_vo_han(thao_tac):
    import json
    import subprocess
    import sys

    cau = "Em định " + " rồi ".join([thao_tac] * 120) + " và nghiệm là ba nhé?"
    # Process riêng có timeout: lỗi regex không được treo cả suite; không mock cổng.
    code = ("import json, sys\n"
            "from app.loi_giang import kiem_loi_giang\n"
            "print(json.dumps(kiem_loi_giang(json.load(sys.stdin))))\n")
    proc = subprocess.run([sys.executable, "-c", code], input=json.dumps({"cau": cau}),
                          text=True, capture_output=True, timeout=5, check=True)
    kq = json.loads(proc.stdout)
    assert "loi" not in kq
    assert kq["cau_sach"] == ""


@pytest.mark.parametrize("cau", [
    "Em đã đọc ra khoảng đồng biến nhé.",
    "Em đã đọc ra cực trị tại mỗi mốc nhé.",
    "Em sẽ nhận cực trị tại mỗi mốc nhé?",
    "Em sẽ đọc ra cực trị tại mỗi mốc nhé?",
])
def test_khong_xac_nhan_bai_lam_hoac_co_cuc_tri_o_moi_moc(cau):
    assert _chay(cau)["cau_sach"] == "", cau


@pytest.mark.parametrize("hanh_dong", ["đọc ra", "xác định", "kết luận", "tìm", "chỉ ra"])
@pytest.mark.parametrize("loai", ["khoảng đồng biến", "khoảng nghịch biến"])
@pytest.mark.parametrize("gioi_tu", ["trên", "ở", "trong"])
@pytest.mark.parametrize("khoang", ["khoảng đó", "khoảng bên trái", "mỗi khoảng", "một khoảng", "từng khoảng"])
@pytest.mark.parametrize("khung", ["Em hãy {} nhé?", "Em thử {} nhé?", "Em sẽ dùng dòng [4] để {} nhé?", "Em định dùng dòng [4] để {} nhé?"])
def test_duong_cu_khong_mien_don_dieu_tren_khoang(hanh_dong, loai, gioi_tu, khoang, khung):
    cau = khung.format(f"{hanh_dong} {loai} {gioi_tu} {khoang}")
    assert _chay(cau)["cau_sach"] == "", cau


@pytest.mark.parametrize("loai", ["cực đại", "cực tiểu", "đồng biến", "nghịch biến"])
@pytest.mark.parametrize("khung", ["Em hãy {} nhé?", "Em thử {} nhé?", "Em sẽ {} nhé?", "Em định {} như thế nào?"])
def test_he_so_khong_duoc_goi_y_ket_luan_da_chon(loai, khung):
    cau = khung.format(f"dựa vào hệ số bậc hai từ đề để kết luận {loai}")
    assert _chay(cau)["cau_sach"] == "", cau


@pytest.mark.parametrize("cau", [
    "Em hãy xác định khoảng đồng biến của $y = x^{2} - 6 x + 4$ trên khoảng đó nhé?",
    "Em sẽ dùng dòng [4] để kết luận khoảng nghịch biến của $y = x^{2} - 6 x + 4$ trên khoảng bên trái nhé?",
])
def test_trich_de_khong_bao_che_don_dieu_da_chon(cau):
    assert _chay(cau)["cau_sach"] == ""


def test_hai_chieu_de_ngo_khong_phai_ket_luan_da_chon():
    cau = "Em hãy dùng dòng [4] để kết luận hàm đồng biến hay nghịch biến trên từng khoảng nhé?"
    assert _chay(cau)["cau_sach"] == cau


@pytest.mark.parametrize("cau", [
    "Em hãy xác định khoảng đồng biến trên đoạn này nhé?",
    "Em hãy xác định khoảng nghịch biến trên đoạn bên trái nhé?",
    "Em hãy xác định khoảng nghịch biến của đạo hàm trên khoảng đó nhé?",
    "Em hãy đọc ra cực trị tại các mốc nhé?",
    "Em hãy đọc ra cực trị giữa hai mốc nhé?",
    "Em hãy đọc ra cực trị bên trái nhé?",
    "Em hãy tìm nghiệm tại mốc bên trái nhé?",
    "Em hãy tìm điểm tới hạn ở mốc bên phải nhé?",
    "Em hãy tìm nghiệm tại điểm đầu tiên nhé?",
    "Em hãy ghi nghiệm bên trái nhé?",
])
def test_khong_lo_vi_tri_khong_dung_so(cau):
    assert _chay(cau)["cau_sach"] == "", cau


@pytest.mark.parametrize("cau", [
    "Em định tìm khoảng đồng biến trên khoảng nào?",
    "Em sẽ tìm cực trị ở điểm nào?",
    "Em định xác định loại cực trị tại mốc nào?",
])
def test_hoi_dia_diem_chua_dien_khong_khang_dinh_vi_tri(cau):
    assert _chay(cau)["cau_sach"] == cau
