package vn.hoctapcanman.core.content.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProblemTest {

    @Test
    void baiHopLeVaDanhSachKyNangPhuKhongDoiTuBenNgoai() {
        List<String> phu = new ArrayList<>(List.of("T12.DH.02"));
        Problem mau = Mau.bai();
        Problem bai = new Problem(mau.id(), mau.code(), mau.skillCode(), phu, mau.level4(), mau.level3(), mau.bloomLevel(),
            mau.difficulty(), mau.statementText(), mau.statementLatex(), mau.functionSympy(), mau.answerForm(), mau.startStep(),
            mau.origin(), mau.contentHash(), mau.createdBy(), mau.createdAt(), mau.updatedAt());
        phu.add("T12.DH.07");
        assertThat(bai.extraSkillCodes()).containsExactly("T12.DH.02");
        assertThat(bai.isFiveStep()).isTrue();
    }

    @Test
    void khaiBaoKetLuanSuyTuDeNhuV0() {
        Problem m = Mau.bai();
        assertThat(deLa(m, "Xét tính đơn điệu của hàm số y = x^3 - 3x^2 + 2.").conclusionClaims())
            .containsExactly("dong_bien", "nghich_bien");
        assertThat(deLa(m, "Tìm khoảng đơn điệu và CỰC TRỊ của hàm số y = x^3 - 3x.").conclusionClaims())
            .containsExactly("dong_bien", "nghich_bien", "cuc_dai", "cuc_tieu");
        assertThat(deLa(m, "Tìm điểm cực tiểu của hàm số y = x^2.").conclusionClaims()).hasSize(4);
        Problem tracNghiem = new Problem(m.id(), m.code(), m.skillCode(), m.extraSkillCodes(), m.level4(), m.level3(), m.bloomLevel(),
            m.difficulty(), "Tìm cực trị.", m.statementLatex(), m.functionSympy(), "TRAC_NGHIEM", m.startStep(), m.origin(), m.contentHash(),
            m.createdBy(), m.createdAt(), m.updatedAt());
        assertThat(tracNghiem.conclusionClaims()).isEmpty();
    }

    private static Problem deLa(Problem m, String de) {
        return new Problem(m.id(), m.code(), m.skillCode(), m.extraSkillCodes(), m.level4(), m.level3(), m.bloomLevel(), m.difficulty(), de,
            m.statementLatex(), m.functionSympy(), m.answerForm(), m.startStep(), m.origin(), m.contentHash(), m.createdBy(), m.createdAt(),
            m.updatedAt());
    }

    @Test
    void tuChoiMaNguonDauVanTayDoKhoKhongHopLe() {
        Problem m = Mau.bai();
        assertThatThrownBy(() -> sua(m, "DH12 NB 01", m.contentHash(), m.difficulty(), m.origin())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sua(m, "x".repeat(65), m.contentHash(), m.difficulty(), m.origin())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sua(m, m.code(), "ABC", m.difficulty(), m.origin())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sua(m, m.code(), "A".repeat(64), m.difficulty(), m.origin())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sua(m, m.code(), m.contentHash(), 1.5, m.origin())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sua(m, m.code(), m.contentHash(), Double.NaN, m.origin())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sua(m, m.code(), m.contentHash(), m.difficulty(), "supham")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deBaiTrongHaySuaTruocKhiTaoThiTuChoi() {
        Problem m = Mau.bai();
        assertThatThrownBy(() -> new Problem(m.id(), m.code(), m.skillCode(), m.extraSkillCodes(), m.level4(), m.level3(),
            m.bloomLevel(), m.difficulty(), "  ", m.statementLatex(), m.functionSympy(), m.answerForm(), m.startStep(), m.origin(),
            m.contentHash(), m.createdBy(), m.createdAt(), m.updatedAt())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Problem(m.id(), m.code(), m.skillCode(), m.extraSkillCodes(), m.level4(), m.level3(),
            m.bloomLevel(), m.difficulty(), m.statementText(), m.statementLatex(), m.functionSympy(), m.answerForm(), m.startStep(),
            m.origin(), m.contentHash(), m.createdBy(), m.createdAt(), m.createdAt().minusSeconds(1)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void loiGiaiKhongInNoiDungVaoLog() {
        Solution loiGiai = new Solution(UUID.randomUUID(), "{\"dap_an\": \"x = 1\"}", "[\"y(1) = 0\"]", "Cực tiểu tại x = 1");
        assertThat(loiGiai.toString()).doesNotContain("dap_an").doesNotContain("y(1)").doesNotContain("Cực tiểu");
        assertThatThrownBy(() -> new Solution(UUID.randomUUID(), null, " ", null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void capGoiYTuMotDenBa() {
        assertThat(new HintLevel(Mau.BAI, "B.DH.XETDAU", 3, "Xét dấu y' trên từng khoảng.").level()).isEqualTo(3);
        assertThatThrownBy(() -> new HintLevel(Mau.BAI, "B.DH.XETDAU", 4, "…")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HintLevel(Mau.BAI, "B.DH.XETDAU", 0, "…")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HintLevel(Mau.BAI, "B.DH.XETDAU", 1, " ")).isInstanceOf(IllegalArgumentException.class);
    }

    private static Problem sua(Problem m, String ma, String bam, Double doKho, String nguon) {
        return new Problem(m.id(), ma, m.skillCode(), m.extraSkillCodes(), m.level4(), m.level3(), m.bloomLevel(), doKho,
            m.statementText(), m.statementLatex(), m.functionSympy(), m.answerForm(), m.startStep(), nguon, bam, m.createdBy(),
            m.createdAt(), m.updatedAt());
    }
}
