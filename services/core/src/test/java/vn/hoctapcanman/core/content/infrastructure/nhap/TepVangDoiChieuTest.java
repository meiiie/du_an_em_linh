package vn.hoctapcanman.core.content.infrastructure.nhap;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Codex #135 (P2): tệp vàng đối chiếu v0 ({@code specs/001-lat-cat-doc/doi-chieu/}) chỉ đúng với cây nguồn đã sinh ra nó.
 * {@code NhapNoiDungTest} phát lại chúng thay dịch vụ toán, nên dịch vụ toán (hay dữ liệu, hay script sinh) đổi mà không sinh
 * lại tệp vàng thì phép đối chiếu vẫn xanh trên hành vi cũ. Test này so cây git ghi trong tệp vàng với checkout: lệch, hay có
 * thay đổi chưa commit ở các thư mục đó, thì đỏ kèm lệnh sinh lại (README của {@code doi-chieu}).
 */
class TepVangDoiChieuTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Path GOC = NhapNoiDungChungTest.thuMucData().getParent();
    private static final String DOI_CHIEU = "specs/001-lat-cat-doc/doi-chieu/";

    @Test
    @SuppressWarnings("unchecked")
    void tepVangCuaXuatV0SinhTuDungCayHienTai() {
        Map<String, Object> nguon = (Map<String, Object>) doc("v0-bai.json").get("nguon");
        Map<String, Object> duLieu = (Map<String, Object>) nguon.get("du_lieu");
        Map<String, Object> dichVuToan = (Map<String, Object>) nguon.get("dich_vu_toan");
        sachVaKhop("v0-bai.json, phan-hoi-toan.json (node " + DOI_CHIEU + "xuat-v0.ts)", Map.of(
            "services/math", dichVuToan.get("cay_git"),
            "data/v0", duLieu.get("data/v0"),
            "data/supham", duLieu.get("data/supham"),
            DOI_CHIEU + "xuat-v0.ts", duLieu.get(DOI_CHIEU + "xuat-v0.ts")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void tepVangKhoaBangSinhTuDungCayHienTai() {
        Map<String, Object> nguon = (Map<String, Object>) doc("khoa-bang-v0.json").get("nguon");
        sachVaKhop("khoa-bang-v0.json (services/math/.venv python " + DOI_CHIEU + "khoa-bang-v0.py)", Map.of(
            "services/math", nguon.get("services_math"),
            "data/v0", nguon.get("data_v0"),
            "data/supham", nguon.get("data_supham"),
            DOI_CHIEU + "khoa-bang-v0.py", nguon.get("script")));
    }

    private static void sachVaKhop(String tepVang, Map<String, Object> daGhi) {
        daGhi.forEach((duongDan, cay) -> {
            assertThat(git("status", "--porcelain", "--", duongDan))
                .as("%s có thay đổi chưa commit: commit rồi sinh lại %s", duongDan, tepVang).isEmpty();
            assertThat(git("rev-parse", "HEAD:" + duongDan))
                .as("%s đã đổi so với lúc sinh %s: sinh lại tệp vàng", duongDan, tepVang).isEqualTo(cay);
        });
    }

    private static String git(String... lenh) {
        try {
            Process p = new ProcessBuilder(java.util.stream.Stream.concat(List.of("git", "-C", GOC.toString()).stream(), List.of(lenh).stream())
                .toList()).redirectErrorStream(true).start();
            String ra = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
            assertThat(p.waitFor()).as("git %s: %s", String.join(" ", lenh), ra).isZero();
            return ra;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> doc(String ten) {
        try {
            return JSON.readValue(Files.readString(GOC.resolve(DOI_CHIEU + ten)), Map.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
