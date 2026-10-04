package vn.hoctapcanman.core.content.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Dữ liệu mẫu tổng hợp cho test model nội dung. */
final class Mau {

    static final Instant LUC = Instant.parse("2026-10-02T08:00:00Z");
    static final UUID LOP = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    static final UUID BAI = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    static final UUID BANG = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
    static final UUID GV = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    static final UUID DOAN = UUID.fromString("00000000-0000-0000-0000-0000000000e1");
    static final String BAM = "a".repeat(64);
    static final String BAM_KHAC = "b".repeat(64);

    private Mau() {}

    static List<TierResult> tang(CheckStatus t1, CheckStatus t2, CheckStatus t3) {
        return List.of(TierResult.of(1, t1), TierResult.of(2, t2), TierResult.of(3, t3));
    }

    static VerificationRun luot(CheckStatus t1, CheckStatus t2, CheckStatus t3) {
        return VerificationRun.forProblem(LOP, BAI, BAM, 1, BANG, tang(t1, t2, t3), List.of(DOAN), LUC);
    }

    /** Dựng lượt kiểm như nạp lại từ CSDL (constructor công khai), để thử tổ hợp lệch. */
    static VerificationRun napLai(CheckStatus tong, ReleaseStatus phatHanh, List<TierResult> tang) {
        return new VerificationRun(UUID.randomUUID(), LOP, SubjectKind.PROBLEM, BAI, BAM, 1, BANG, tong, phatHanh, false, LUC, tang, List.of(DOAN));
    }

    static Problem bai() {
        return new Problem(BAI, "DH12-NB-01", "T12.DH.03", List.of("T12.DH.02"), Level4.NHAN_BIET, Level3.BIET, BloomLevel.NHO,
            0.5, "Xét tính đơn điệu của hàm số y = x^2.", "y = x^2", "x**2", Problem.TU_LUAN_5_BUOC, null, "VI_DU_CONG", BAM,
            GV, LUC, LUC);
    }

    static Formula dong(int thuTu, String ma) {
        return Formula.unchecked(thuTu, ma, "T12.DH.03", "Đạo hàm tổng", "(u+v)' = u' + v'", "Đạo hàm của tổng bằng tổng các đạo hàm.");
    }

    /** Kết quả DAT ở hai tầng cho đúng nội dung dòng {@code dong}. */
    static FormulaCheck dat(Formula dong) {
        return FormulaCheck.of(dong, FormulaKind.DANG_THUC, CheckStatus.DAT, CheckStatus.DAT, "{\"muc_bang_chung\": \"CAS\"}",
            "{\"trich_dan\": {\"doan\": \"p-1\"}}", DOAN);
    }

    /** Kết quả DAT cho mọi dòng của bảng, theo mã dòng. */
    static Map<String, FormulaCheck> datCaBang(FormulaSheet bang) {
        return bang.rows().stream().collect(Collectors.toMap(Formula::code, Mau::dat));
    }
}
