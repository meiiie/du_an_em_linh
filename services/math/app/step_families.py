"""Khung giải tích và xác suất, chỉ chạy trong sandbox.

`bai_toan` là dữ kiện của đề do core cung cấp, không phải đáp án do trình duyệt
cung cấp. Tự tính lại đáp án; không đọc `cham_buoc` của seed khi chấm.
Giữ nguyên hợp đồng kết quả của B.DH, không trả đáp án trong phản hồi.
"""
import ast

import sympy as s

from app.dau_vao import ly_do_tu_choi, tu_choi_payload
from app.normalizer import normalize_expr
from app.paths import load_kiem

K = load_kiem()
GT_ORDER = [f"B.GT.{name}" for name in ("MIEN", "BIENDOI", "TRUNGGIAN", "TINH", "KETLUAN")]
XS_ORDER = [f"B.XS.{name}" for name in ("DUKIEN", "THANHPHAN1", "THANHPHAN2", "KETHOP", "KETLUAN")]


def read(raw, scalar=False):
    if isinstance(raw, bool) or not isinstance(raw, (str, int, float)) or ly_do_tu_choi(raw):
        raise ValueError("Dữ kiện không hợp lệ")
    normalized = normalize_expr(str(raw))
    if normalized is None:
        raise ValueError("Không đọc được biểu thức")
    value = K.phan_tich_an_toan(normalized)
    if value.has(s.oo, s.zoo, s.nan) or value.is_real is False or value.free_symbols - {K.x} or (scalar and value.free_symbols):
        raise ValueError("Biểu thức không hữu hạn hoặc có biến ngoài đề")
    return value


def check_expression_domain(raw, reference):
    """Không nhận phép rút gọn x/x hay sqrt(x)² làm mất điều kiện xác định.

    Parser an toàn đã kiểm chuỗi trước. AST chỉ dùng tìm các phép toán gốc
    có thể bị SymPy rút gọn mất; không thực thi AST.
    """
    normalized = normalize_expr(raw)
    tree = ast.parse(normalized, mode="eval")
    required = s.calculus.util.continuous_domain(reference, K.x, s.S.Reals)
    for node in ast.walk(tree):
        if isinstance(node, ast.BinOp) and isinstance(node.op, ast.Div):
            denominator = K.phan_tich_an_toan(ast.unparse(node.right))
            if s.solveset(denominator, K.x, domain=required) != s.S.EmptySet:
                raise ValueError("Biểu thức thêm điểm không xác định")
        if isinstance(node, ast.Call) or (isinstance(node, ast.BinOp) and isinstance(node.op, ast.Pow)):
            term = K.phan_tich_an_toan(ast.unparse(node))
            domain = s.calculus.util.continuous_domain(term, K.x, s.S.Reals)
            if required.is_subset(domain) is not True:
                raise ValueError("Phép biến đổi làm hẹp miền xác định")


def count(p, key, low=0, high=100):
    value = p[key]
    if type(value) is not int or not low <= value <= high:
        raise ValueError("Số lượng ngoài phạm vi")
    return value


def probability(raw):
    value = read(raw, scalar=True)
    if not value.is_Rational or not 0 <= value <= 1:
        raise ValueError("Xác suất phải là số hữu tỉ trong [0; 1]")
    return value


def step(title, instruction, value, kind="BIEU_THUC"):
    values = value if isinstance(value, (list, tuple)) else [value]
    return {"ten": title, "viec": instruction, "kieu": kind,
            "dap_an": [str(v) for v in values]}


def domain_text(domain):
    if domain == s.S.Reals:
        return "R"
    excluded = s.S.Reals - domain
    if isinstance(excluded, s.FiniteSet):
        return "R \\ {" + "; ".join(str(v) for v in excluded) + "}"
    raise ValueError("Miền xác định chưa hỗ trợ")


def calculus(kind, p):
    if kind == "quadratic_min":
        f = read(p["ham"])
        if not f.is_polynomial(K.x) or s.degree(f, K.x) != 2:
            raise ValueError("Cần một tam thức bậc hai")
        a, b, _ = s.Poly(f, K.x).all_coeffs()
        if not a > 0:
            raise ValueError("Khung giá trị nhỏ nhất cần hệ số bậc hai dương")
        vertex = -b / (2 * a)
        minimum = f.subs(K.x, vertex)
        return [step("Hệ số bậc hai", "Xác định hệ số bậc hai và kiểm tra dấu dương.", a),
                step("Hoành độ đỉnh", "Tìm hoành độ đỉnh từ hai hệ số đầu.", vertex),
                step("Tung độ đỉnh", "Thay hoành độ đỉnh vào hàm số.", minimum),
                step("Hoàn thành bình phương", "Viết bình phương có dạng (x − hoành độ đỉnh)² để chứng minh giá trị nhỏ nhất.", a * (K.x - vertex) ** 2 + minimum, "BINH_PHUONG"),
                step("Giá trị nhỏ nhất", "Kết luận giá trị nhỏ nhất của hàm số.", minimum)]
    if kind == "trig_roots":
        f = read(p["ham"])
        lo, hi = read(p["can_duoi"], True), read(p["can_tren"], True)
        if not lo < hi or hi - lo > 4 * s.pi:
            raise ValueError("Khung phương trình lượng giác cần một đoạn hữu hạn ngắn")
        rewritten = s.expand_trig(f).subs(s.cos(K.x) ** 2, 1 - s.sin(K.x) ** 2).expand()
        u = s.Dummy("u", real=True)
        polynomial = rewritten.subs(s.sin(K.x), u)
        if polynomial.free_symbols - {u} or not polynomial.is_polynomial(u) or s.degree(polynomial, u) > 2:
            raise ValueError("Khung này chỉ hỗ trợ phương trình bậc nhất/bậc hai theo sin(x)")
        levels = s.solveset(polynomial, u, domain=s.Interval(-1, 1))
        if not isinstance(levels, s.FiniteSet) or not levels:
            raise ValueError("Chưa kiểm được các giá trị sin(x)")
        interval = s.Interval(lo, hi)
        first = s.solveset(s.sin(K.x) - min(levels), K.x, domain=interval)
        roots = s.solveset(f, K.x, domain=interval)
        if not isinstance(first, s.FiniteSet) or not isinstance(roots, s.FiniteSet):
            raise ValueError("Chưa kiểm được tập nghiệm lượng giác")
        return [step("Miền nghiệm", "Viết hai đầu mút của đoạn xét nghiệm theo thứ tự tăng dần.", [lo, hi], "DAY_GIA_TRI"),
                step("Đưa về sin", "Viết biểu thức vế trái hoàn toàn theo sin(x), đặt bằng 0.", rewritten),
                step("Giá trị sin", "Tìm tập giá trị sin(x) thỏa phương trình và thuộc [−1; 1].", list(levels), "TAP_HUU_HAN"),
                step("Giải nhánh đầu", "Tìm tập nghiệm trên đoạn đã cho ứng với giá trị sin(x) nhỏ nhất ở bước trước.", list(first), "TAP_HUU_HAN"),
                step("Tập nghiệm", "Kết luận tập tất cả nghiệm trên đoạn đã cho.", list(roots), "TAP_HUU_HAN")]
    if kind == "between_area":
        f, g = read(p["f"]), read(p["g"])
        roots = s.solveset(f - g, K.x, domain=s.S.Reals)
        if not isinstance(roots, s.FiniteSet) or len(roots) != 2:
            raise ValueError("Cần đúng hai giao điểm")
        lo, hi = sorted(roots)
        # Các bài hỗ trợ có một miền kín; không tự lấy trị tuyệt đối của
        # tích phân nếu hiệu hai hàm còn đổi dấu ở bên trong.
        h = s.simplify(g - f)
        zeros = s.solveset(h, K.x, domain=s.Interval.open(lo, hi))
        if zeros != s.S.EmptySet:
            raise ValueError("Cần chia miền trước khi tích phân")
        F = s.integrate(h, K.x)
        signed = s.integrate(h, (K.x, lo, hi))
        return [step("Giao điểm", "Tìm tập hoành độ giao điểm.", list(roots), "TAP_HUU_HAN"),
                step("Hiệu hai hàm", "Viết hàm thứ hai trừ hàm thứ nhất.", h),
                step("Nguyên hàm", "Tìm một nguyên hàm của hiệu hai hàm.", F, "NGUYEN_HAM"),
                step("Tích phân", "Tính tích phân của hiệu trên miền giữa hai giao điểm.", signed),
                step("Diện tích", "Lấy trị tuyệt đối để kết luận diện tích.", s.Abs(signed))]
    f = read(p["ham"])
    domain = s.calculus.util.continuous_domain(f, K.x, s.S.Reals)
    if kind in ("integral", "area"):
        lo, hi = read(p["can_duoi"], True), read(p["can_tren"], True)
        if not lo < hi or not s.Interval(lo, hi).is_subset(domain):
            raise ValueError("Hàm phải liên tục trên đoạn tích phân")
        if kind == "area" and s.solveset(f, K.x, domain=s.Interval.open(lo, hi)) != s.S.EmptySet:
            raise ValueError("Diện tích đổi dấu cần khung chia khoảng")
        F = s.integrate(f, K.x)
        result = s.integrate(f, (K.x, lo, hi))
        return [step("Miền tích phân", "Viết hai cận theo thứ tự cận dưới; cận trên.", [lo, hi], "DAY_GIA_TRI"),
                step("Nguyên hàm", "Tìm một nguyên hàm trên đoạn tích phân.", F, "NGUYEN_HAM"),
                step("Giá trị tại cận dưới", "Thay cận dưới vào nguyên hàm em đã chọn.", F.subs(K.x, lo)),
                step("Giá trị tại cận trên", "Thay cận trên vào nguyên hàm em đã chọn.", F.subs(K.x, hi)),
                step("Diện tích" if kind == "area" else "Tích phân",
                     "Lấy trị tuyệt đối của hiệu hai giá trị." if kind == "area" else "Lấy giá trị tại cận trên trừ giá trị tại cận dưới.",
                     s.Abs(result) if kind == "area" else result)]
    fp = s.diff(f, K.x)
    if kind == "derivative_value":
        point = read(p["tai"], True)
        if point not in s.calculus.util.continuous_domain(fp, K.x, s.S.Reals):
            raise ValueError("Đạo hàm không xác định tại điểm đã cho")
        # Hai phần của quy tắc tổng cho đa thức; với thương/hàm hợp,
        # bước thứ hai là tử của đạo hàm hoặc hệ số đạo hàm hàm trong.
        if f.is_polynomial(K.x):
            leading = s.Poly(f, K.x).LC() * K.x ** s.degree(f, K.x)
            partial = s.diff(leading, K.x)
            instruction = "Tính đạo hàm của hạng tử có bậc cao nhất."
        elif f.func == s.sin:
            partial = s.diff(f.args[0], K.x)
            instruction = "Tính đạo hàm của biểu thức trong hàm sin."
        else:
            numerator, denominator = s.fraction(f)
            partial = s.diff(numerator, K.x) * denominator - numerator * s.diff(denominator, K.x)
            instruction = "Tính tử số theo quy tắc đạo hàm của thương, chưa chia cho bình phương mẫu."
        return [step("Điều kiện", "Viết tập xác định của hàm số.", domain_text(domain), "MIEN"),
                step("Áp dụng quy tắc", instruction, partial),
                step("Đạo hàm", "Tính và rút gọn đạo hàm của hàm số.", fp),
                step("Giá trị hàm tại điểm", "Tính giá trị hàm số tại điểm đã cho, phân biệt với giá trị đạo hàm.", f.subs(K.x, point)),
                step("Giá trị đạo hàm", "Thay điểm đã cho vào đạo hàm và kết luận.", fp.subs(K.x, point))]
    if kind == "tangent_through":
        px, py = [read(v, True) for v in p["P"]]
        condition = s.expand(f + (px - K.x) * fp - py)
        roots = s.solveset(condition, K.x, domain=domain)
        if not isinstance(roots, s.FiniteSet):
            raise ValueError("Tập tiếp điểm chưa kiểm được")
        return [step("Điều kiện", "Viết tập xác định của hàm số.", domain_text(domain), "MIEN"),
                step("Đạo hàm", "Tính hệ số góc tiếp tuyến theo hoành độ tiếp điểm.", fp),
                step("Điều kiện đi qua điểm", "Viết f(x) + (hoành độ P − x)f′(x) − tung độ P, đặt biểu thức này bằng 0.", condition),
                step("Hệ số góc", "Tính tập hệ số góc tại tất cả nghiệm của điều kiện đi qua P.", [fp.subs(K.x, r) for r in roots], "TAP_HUU_HAN"),
                step("Hoành độ tiếp điểm", "Kết luận tập tất cả hoành độ tiếp điểm, kể cả tiếp tuyến ngang.", list(roots), "TAP_HUU_HAN")]
    if kind == "maximum":
        lo, hi = read(p["can_duoi"], True), read(p["can_tren"], True)
        # Giới hạn khung này ở parabol lõm, chứng minh được cực đại toàn cục.
        if not f.is_polynomial(K.x) or s.degree(f, K.x) != 2 or not s.diff(f, K.x, 2) < 0:
            raise ValueError("Khung tối ưu chỉ hỗ trợ parabol lõm")
        roots = s.solveset(fp, K.x, domain=s.Interval.open(lo, hi))
        if len(roots) != 1:
            raise ValueError("Cần một điểm cực đại trong miền")
        point = next(iter(roots))
        candidates = [f.subs(K.x, v) for v in (lo, point, hi)]
        return [step("Miền của biến", "Viết hai đầu mút của miền chiều rộng theo thứ tự tăng dần.", [lo, hi], "DAY_GIA_TRI"),
                step("Đạo hàm", "Tính đạo hàm của diện tích theo chiều rộng.", fp),
                step("Điểm tới hạn", "Tìm tập nghiệm đạo hàm bằng 0 trong miền.", list(roots), "TAP_HUU_HAN"),
                step("So sánh giá trị", "Tính diện tích tại đầu mút trái, điểm tới hạn, đầu mút phải theo thứ tự này.", candidates, "DAY_GIA_TRI"),
                step("Cực đại toàn cục", "Viết chiều rộng và diện tích lớn nhất theo thứ tự này.", [point, max(candidates)], "DAY_GIA_TRI")]
    raise ValueError("Dạng giải tích chưa hỗ trợ")


def probabilities(kind, p):
    C = s.binomial
    if kind in ("hypergeom", "conditional_group"):
        good, bad, draw = count(p, "good"), count(p, "bad"), count(p, "draw", 1, 12)
        if draw > good + bad:
            raise ValueError("Số chọn vượt tổng số")
        total = C(good + bad, draw)
        if kind == "conditional_group":
            excluded, favorable = C(bad, draw), C(good, draw)
            if total == excluded:
                raise ValueError("Biến cố điều kiện có xác suất bằng 0")
            values = [total, excluded, total - excluded, favorable, favorable / (total - excluded)]
            titles = ["Không gian mẫu", "Trường hợp bị loại", "Biến cố điều kiện", "Trường hợp thuận lợi", "Xác suất có điều kiện"]
            instructions = ["Đếm mọi cách chọn.", "Đếm cách chọn toàn phần tử không thuận lợi.", "Đếm cách chọn có ít nhất một phần tử thuận lợi.", "Đếm cách chọn toàn phần tử thuận lợi.", "Chia số trường hợp thuận lợi cho số trường hợp của biến cố điều kiện."]
        else:
            want = count(p, "want", 0, draw)
            first, second = C(good, want), C(bad, draw - want)
            values = [total, first, second, first * second, first * second / total]
            titles = ["Không gian mẫu", "Chọn phần tử thuận lợi", "Chọn phần tử còn lại", "Biến cố", "Xác suất"]
            instructions = ["Đếm mọi cách chọn không xét thứ tự.", "Đếm cách chọn đúng số phần tử thuận lợi đề yêu cầu.", "Đếm cách chọn các phần tử còn lại từ nhóm kia.", "Nhân hai số cách chọn để đếm biến cố.", "Lấy số kết quả thuận lợi chia tổng số kết quả."]
    elif kind == "binomial":
        n, k, prob = count(p, "n", 1, 100), count(p, "k"), probability(p["p"])
        if k > n:
            raise ValueError("Số thành công vượt số phép thử")
        values = [C(n, k), prob ** k, (1 - prob) ** (n - k), prob ** k * (1 - prob) ** (n - k), C(n, k) * prob ** k * (1 - prob) ** (n - k)]
        titles = ["Vị trí thành công", "Các lần thành công", "Các lần thất bại", "Một cách sắp xếp", "Xác suất"]
        instructions = ["Đếm cách chọn vị trí các lần thành công.", "Tính tích xác suất các lần thành công.", "Tính tích xác suất các lần thất bại.", "Tính xác suất một cách sắp xếp cụ thể.", "Nhân với số cách sắp xếp."]
    elif kind in ("bayes", "bayes_sample", "bayes_transfer", "bayes_given"):
        if kind == "bayes_given":
            joint, evidence = probability(p["joint"]), probability(p["evidence"])
            if not 0 < evidence or joint > evidence:
                raise ValueError("Dữ kiện xác suất có điều kiện không hợp lệ")
            values = [evidence, 1 - evidence, joint, evidence - joint, joint / evidence]
            titles = ["Biến cố điều kiện", "Biến cố đối", "Giao hai biến cố", "Phần còn lại", "Xác suất có điều kiện"]
            instructions = ["Ghi xác suất biến cố điều kiện.", "Tính xác suất biến cố đối của điều kiện.", "Ghi xác suất giao hai biến cố.", "Tính xác suất điều kiện xảy ra nhưng biến cố cần tìm không xảy ra.", "Áp dụng định nghĩa xác suất có điều kiện."]
        else:
            if kind == "bayes_transfer":
                green, red = count(p, "source_green"), count(p, "source_red")
                tg, tr, draw = count(p, "target_green"), count(p, "target_red"), count(p, "draw", 1, 12)
                if green + red == 0 or draw > tg + tr + 1:
                    raise ValueError("Hộp hoặc số chọn không hợp lệ")
                prior = s.Rational(green, green + red)
                first, second = C(tg + 1, draw) / C(tg + tr + 1, draw), C(tg, draw) / C(tg + tr + 1, draw)
            else:
                prior = probability(p["prior"])
                g1, b1, g2, b2 = [count(p, key) for key in ("g1", "b1", "g2", "b2")]
                draw = count(p, "draw", 1, 12) if kind == "bayes_sample" else 1
                want = count(p, "want", 0, draw) if kind == "bayes_sample" else 1
                if draw > min(g1 + b1, g2 + b2):
                    raise ValueError("Số chọn vượt số phần tử trong hộp")
                first = C(g1, want) * C(b1, draw - want) / C(g1 + b1, draw)
                second = C(g2, want) * C(b2, draw - want) / C(g2 + b2, draw)
            evidence = prior * first + (1 - prior) * second
            if evidence == 0:
                raise ValueError("Điều kiện có xác suất bằng 0")
            values = [prior, first, second, evidence, prior * first / evidence]
            titles = ["Giả thuyết thứ nhất", "Điều kiện theo giả thuyết thứ nhất", "Điều kiện theo giả thuyết thứ hai", "Xác suất toàn phần", "Xác suất hậu nghiệm"]
            instructions = ["Ghi xác suất giả thuyết thứ nhất.", "Tính xác suất quan sát được biến cố khi giả thuyết thứ nhất đúng.", "Tính xác suất quan sát được biến cố khi giả thuyết thứ hai đúng.", "Tính xác suất quan sát được biến cố bằng công thức xác suất toàn phần.", "Áp dụng công thức Bayes cho giả thuyết thứ nhất."]
    elif kind == "three_colors":
        colors = p["counts"]
        if not isinstance(colors, list) or len(colors) != 3:
            raise ValueError("Cần ba nhóm màu")
        counts = [count({"n": c}, "n", 1) for c in colors]
        # Khung hiện tại xét đúng một màu lặp hai lần; không áp cho draw khác.
        if count(p, "draw") != 4:
            raise ValueError("Khung ba màu hỗ trợ chọn bốn thẻ")
        parts = [C(counts[i], 2) * counts[(i + 1) % 3] * counts[(i + 2) % 3] for i in range(3)]
        total = C(sum(counts), 4)
        values = [total, *parts, sum(parts) / total]
        titles = ["Không gian mẫu", "Màu thứ nhất lặp", "Màu thứ hai lặp", "Màu thứ ba lặp", "Xác suất đủ ba màu"]
        instructions = ["Đếm mọi cách chọn bốn thẻ.", "Đếm trường hợp hai thẻ màu thứ nhất, mỗi màu còn lại một thẻ.", "Đếm trường hợp hai thẻ màu thứ hai, mỗi màu còn lại một thẻ.", "Đếm trường hợp hai thẻ màu thứ ba, mỗi màu còn lại một thẻ.", "Cộng ba trường hợp rời nhau rồi chia tổng số cách chọn."]
    elif kind == "committee_roles":
        women, men, draw = count(p, "women", 1), count(p, "men", 1), count(p, "draw", 2, 12)
        minimum = count(p, "min_women", 0, draw)
        if draw > women + men:
            raise ValueError("Số chọn vượt tổng số người")
        total = (women + men) * (women + men - 1) * C(women + men - 2, draw - 2)
        remaining = sum(C(women - 1, k) * C(men - 1, draw - 2 - k) for k in range(max(0, minimum - 1), draw - 1))
        values = [total, women, men, remaining, women * men * remaining / total]
        titles = ["Không gian mẫu", "Trưởng nhóm", "Thư ký", "Các thành viên còn lại", "Xác suất"]
        instructions = ["Đếm cách chọn hai chức vụ khác nhau rồi chọn các thành viên còn lại.", "Đếm cách chọn trưởng nhóm là nữ.", "Đếm cách chọn thư ký là nam.", "Đếm cách chọn các thành viên còn lại để đủ số nữ tối thiểu.", "Nhân số cách thuận lợi rồi chia tổng số cách."]
    else:
        raise ValueError("Dạng xác suất chưa hỗ trợ")
    return [step(title, instruction, value) for title, instruction, value in zip(titles, instructions, values)]


def build_steps(problem):
    """Dữ kiện công khai -> năm đáp án riêng ở sandbox/kiểm thử, không gửi HS."""
    if not isinstance(problem, dict) or set(problem) != {"loai", "du_kien"} or not isinstance(problem["du_kien"], dict):
        raise ValueError("Đề sai khuôn")
    kind, p = problem["loai"], problem["du_kien"]
    if kind in {"quadratic_min", "trig_roots", "derivative_value", "tangent_through", "maximum", "integral", "area", "between_area"}:
        order, steps = GT_ORDER, calculus(kind, p)
    else:
        order, steps = XS_ORDER, probabilities(kind, p)
    for code, item in zip(order, steps):
        item["ma_buoc"] = code
    return steps


def _values(lines, kind):
    if len(lines) != 1:
        raise ValueError("Mỗi bước cần một dòng")
    raw = lines[0]["latex"].strip()
    if kind == "MIEN":
        # Parser miền của bộ kiểm nhận đúng cú pháp R, R \\ {a}, khoảng.
        from app.normalizer import normalize_domain
        return K.parse_tap(normalize_domain(raw))
    if kind == "TAP_HUU_HAN":
        raw = raw.replace(r"\left", "").replace(r"\right", "")
        raw = raw.replace(r"\{", "{").replace(r"\}", "}")
        if raw in ("∅", r"\emptyset", "không có nghiệm"):
            return s.S.EmptySet
        if not raw.startswith("{") or not raw.endswith("}"):
            raise ValueError("Tập hữu hạn cần ngoặc nhọn và dấu chấm phẩy")
        raw = raw[1:-1]
        return s.FiniteSet(*(read(v, True) for v in raw.split(";") if v.strip()))
    if kind == "DAY_GIA_TRI":
        return [read(v, True) for v in raw.split(";")]
    return read(raw)


def grade_family(payload):
    from app.grader import _buoc, _pack

    per = {}
    den = payload.get("nop_toi")
    current = None
    try:
        if tu_choi_payload(payload):
            raise ValueError("Đầu vào bị từ chối")
        specs = build_steps(payload.get("bai_toan"))
        order = [item["ma_buoc"] for item in specs]
        den = den or order[-1]
        if den not in order or payload.get("buoc_bat_dau", order[0]) != order[0]:
            raise ValueError("Bước nộp không thuộc khung")
        submitted = payload["cac_buoc"]
        # Không mặc định nộp tới cuối khi ma_buoc lạ, không chấm một tập con thiếu bước.
        if [b["ma_buoc"] for b in submitted] != order[:order.index(den) + 1]:
            raise ValueError("Thiếu, trùng hoặc sai thứ tự bước")
        primitive = None
        for index, (spec, actual) in enumerate(zip(specs, submitted)):
            current = spec["ma_buoc"]
            lines = actual.get("cac_dong", [])
            if actual.get("bang") or len(lines) != 1 or lines[0].get("dong") != 0:
                raise ValueError("Bước cần đúng một dòng số 0")
            value = _values(lines, spec["kieu"])
            expected = [read(v) for v in spec["dap_an"]] if spec["kieu"] != "MIEN" else []
            if spec["kieu"] == "MIEN":
                f = read(payload["bai_toan"]["du_kien"]["ham"])
                matches = value == s.calculus.util.continuous_domain(f, K.x, s.S.Reals)
            elif spec["kieu"] == "TAP_HUU_HAN":
                matches = value == s.FiniteSet(*expected)
            elif spec["kieu"] == "DAY_GIA_TRI":
                matches = len(value) == len(expected) and all(s.simplify(a - b) == 0 for a, b in zip(value, expected))
            elif spec["kieu"] == "NGUYEN_HAM":
                # Nhận mọi hằng số cộng, không chỉ nguyên hàm chuẩn trong seed.
                matches = s.simplify(s.diff(value - expected[0], K.x)) == 0
                primitive = value
            else:
                reference = expected[0]
                p = payload["bai_toan"]["du_kien"]
                if primitive is not None and payload["bai_toan"]["loai"] in ("integral", "area") and index in (2, 3):
                    reference = primitive.subs(K.x, read(p["can_duoi" if index == 2 else "can_tren"], True))
                matches = s.simplify(value - reference) == 0
                if spec["kieu"] == "BINH_PHUONG":
                    # Một đa thức viết lại nguyên đề chưa chứng minh cận dưới.
                    # Phải còn bình phương đúng tâm; nhận đổi dấu (h − x)².
                    vertex = -s.Poly(read(p["ham"]), K.x).all_coeffs()[1] / (2 * s.Poly(read(p["ham"]), K.x).LC())
                    matches = matches and any(power.exp == 2 and
                        (s.simplify(power.base - (K.x - vertex)) == 0 or s.simplify(power.base + (K.x - vertex)) == 0)
                        for power in value.atoms(s.Pow))
            if spec["kieu"] in ("BIEU_THUC", "BINH_PHUONG", "NGUYEN_HAM"):
                check_expression_domain(lines[0]["latex"], expected[0])
            if not matches:
                per[current] = "SAI"
                return _pack("SAI", "SAI_BIEN_DOI", _buoc(current, 0), "khong_tuong_duong", per, [], nop_toi=den, ma_loi_tin=(None, None),
                             thong_bao=f"Bước {spec['ten'].lower()} cần xem lại. Em kiểm tra cách lập công thức và tính toán.")
            per[current] = "DAT"
        return _pack("DAT", "DAT", None, "dat", per, [], chua_xong=den != order[-1], nop_toi=den)
    except Exception:
        return _pack("KHONG_KIEM_DUOC", "KHONG_KIEM_DUOC", _buoc(current, 0) if current else None,
                     "dau_vao_khong_hop_le", per, [], nop_toi=den,
                     thong_bao="Chưa kiểm được bài làm. Em dùng đúng khung bước, mỗi bước một dòng; các giá trị cách nhau bằng dấu chấm phẩy.")
