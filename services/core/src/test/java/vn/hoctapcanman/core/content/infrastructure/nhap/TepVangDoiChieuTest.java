package vn.hoctapcanman.core.content.infrastructure.nhap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Codex #135 (P2): tệp vàng đối chiếu v0 ({@code specs/001-lat-cat-doc/doi-chieu/}, lời giải v0 trong tài nguyên test) chỉ
 * đúng với nguồn đã sinh ra nó.
 * Test của core phát lại chúng thay dịch vụ toán và mã v0, nên nguồn đổi mà không sinh lại tệp vàng thì phép đối chiếu vẫn
 * xanh trên hành vi cũ.
 *
 * <p>Test duyệt mọi khóa trong {@code nguon} của tệp vàng. Mỗi khóa hoặc chỉ tới một đối tượng git (cây, blob) được so với
 * checkout, hoặc nằm trong danh sách miễn kèm lý do. Khóa mới mà không thuộc loại nào thì test đỏ, nên script sinh ghi thêm
 * nguồn nào cũng buộc test kiểm nó. Đường dẫn được kiểm mà có thay đổi chưa commit cũng đỏ: cây HEAD khi đó không phải bản
 * đang làm việc.
 */
class TepVangDoiChieuTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Path GOC = NhapNoiDungChungTest.thuMucData().getParent();
    private static final String DOI_CHIEU = "specs/001-lat-cat-doc/doi-chieu/";
    private static final String LOI_GIAI_V0 = "services/core/src/test/resources/content/loi-giai-v0.json";

    /** Khóa của {@code v0-bai.json} không phải đối tượng git của checkout, và vì sao không cần so. */
    private static final Map<String, String> MIEN_XUAT_V0 = Map.of(
        "seed.tep", "đường dẫn của seed.ts, dùng để so seed.blob",
        "seed.commit", "ghi chú commit lúc chạy; nội dung đã cố định bằng seed.blob",
        "dich_vu_toan.anh_goc.", "môi trường chạy lúc sinh (ảnh gốc), không suy ra được từ checkout",
        "dich_vu_toan.python", "môi trường chạy lúc sinh, không suy ra được từ checkout",
        "dich_vu_toan.goi_python", "môi trường chạy lúc sinh, không suy ra được từ checkout",
        "dich_vu_toan.suc_khoe.", "phản hồi /health lúc sinh",
        "kho_lop.", "kho lớp; NhapNoiDungTest.bamKho so kho của từng yêu cầu verify với nó");

    @Test
    void moiNguonCuaXuatV0DeuKhopCheckout() {
        Map<String, Object> nguon = phang(doc(DOI_CHIEU + "v0-bai.json"));
        kiemNguon("v0-bai.json, phan-hoi-toan.json (node " + DOI_CHIEU + "xuat-v0.ts)", nguon, khoa -> {
            if (khoa.equals("seed.blob")) {
                return (String) nguon.get("seed.tep");
            }
            if (khoa.equals("dich_vu_toan.cay_git")) {
                return "services/math";
            }
            return khoa.startsWith("du_lieu.") ? khoa.substring("du_lieu.".length()) : null;
        }, MIEN_XUAT_V0);
    }

    @Test
    void moiNguonCuaKhoaBangDeuKhopCheckout() {
        Map<String, String> duongDan = Map.of("services_math", "services/math", "data_v0", "data/v0", "data_supham", "data/supham",
            "script", DOI_CHIEU + "khoa-bang-v0.py");
        kiemNguon("khoa-bang-v0.json (services/math/.venv python " + DOI_CHIEU + "khoa-bang-v0.py)",
            phang(doc(DOI_CHIEU + "khoa-bang-v0.json")), duongDan::get, Map.of());
    }

    @Test
    void moiNguonCuaChamV0DeuKhopCheckout() {
        kiemNguon("cham-v0.json (node " + DOI_CHIEU + "cham-v0.ts)", phang(doc(DOI_CHIEU + "cham-v0.json")),
            khoa -> khoa.startsWith("git.") ? khoa.substring("git.".length()) : null,
            Map.of("dich_vu_toan.", "môi trường chạy lúc sinh (ảnh gốc, Python, gói, /health), không suy ra được từ checkout"));
    }

    @Test
    void moiNguonCuaLoiGiaiV0DeuKhopCheckout() {
        Map<String, Object> nguon = phang(doc(LOI_GIAI_V0));
        kiemNguon(LOI_GIAI_V0 + " (node " + DOI_CHIEU + "loi-giai-v0.ts)", nguon, khoa -> switch (khoa) {
            case "blob" -> (String) nguon.get("tep");
            case "script" -> DOI_CHIEU + "loi-giai-v0.ts";
            default -> null;
        }, Map.of("tep", "đường dẫn của tệp nguồn, dùng để so blob"));
    }

    /**
     * Lưới an toàn cho cả lớp lỗi: mọi tệp vàng có khóa {@code nguon} (trong {@code doi-chieu} và tài nguyên test của core)
     * phải có một test ở đây so nguồn của nó với checkout. Thêm tệp vàng mà quên kiểm nguồn thì đỏ.
     */
    @Test
    void moiTepVangCoNguonDeuDuocKiem() throws IOException {
        List<String> coNguon = new ArrayList<>();
        for (Path thuMuc : List.of(GOC.resolve(DOI_CHIEU), GOC.resolve("services/core/src/test/resources"))) {
            try (Stream<Path> tep = Files.walk(thuMuc)) {
                for (Path t : tep.filter(x -> x.toString().endsWith(".json")).toList()) {
                    if (JSON.readTree(Files.readString(t)).has("nguon")) {
                        coNguon.add(GOC.relativize(t).toString().replace('\\', '/'));
                    }
                }
            }
        }
        assertThat(coNguon).as("tệp vàng có «nguon» so với tệp vàng có test kiểm nguồn")
            .containsExactlyInAnyOrder(DOI_CHIEU + "v0-bai.json", DOI_CHIEU + "khoa-bang-v0.json", DOI_CHIEU + "cham-v0.json", LOI_GIAI_V0);
    }

    @Test
    void khoaNguonChuaPhanLoaiThiDo() {
        // Lưới an toàn của chính test: một nguồn mới mà không ai phân loại phải làm test đỏ, không bị bỏ qua.
        Map<String, Object> nguon = Map.of("cay_moi", "0".repeat(40));
        try {
            kiemNguon("thử", nguon, khoa -> null, Map.of());
        } catch (AssertionError e) {
            assertThat(e).hasMessageContaining("cay_moi");
            return;
        }
        fail("khóa nguồn chưa phân loại không làm test đỏ");
    }

    private static void kiemNguon(String tepVang, Map<String, Object> nguon, Function<String, @Nullable String> duongDanCua,
            Map<String, String> mien) {
        int daSo = 0;
        for (Map.Entry<String, Object> e : nguon.entrySet()) {
            String duongDan = duongDanCua.apply(e.getKey());
            if (duongDan != null) {
                // Đường dẫn so nguyên văn: [id] của Next.js là glob với git, khớp nhầm thư mục i, d.
                assertThat(git("--literal-pathspecs", "status", "--porcelain", "--", duongDan))
                    .as("%s có thay đổi chưa commit: commit rồi sinh lại %s", duongDan, tepVang).isEmpty();
                assertThat(git("rev-parse", "HEAD:" + duongDan))
                    .as("%s (nguon.%s) đã đổi so với lúc sinh %s: sinh lại tệp vàng", duongDan, e.getKey(), tepVang)
                    .isEqualTo(e.getValue());
                daSo++;
            } else if (mien.keySet().stream().noneMatch(m -> m.endsWith(".") ? e.getKey().startsWith(m) : e.getKey().equals(m))) {
                fail("nguon.%s của %s chưa được so với checkout hay miễn có lý do", e.getKey(), tepVang);
            }
        }
        assertThat(daSo).as("số nguồn của %s được so với checkout", tepVang).isPositive();
    }

    /** Khóa lồng nhau viết liền bằng dấu chấm; giá trị không phải đối tượng (chữ, số, danh sách) là lá. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> phang(Map<String, Object> nguon) {
        Map<String, Object> ra = new LinkedHashMap<>();
        nguon.forEach((k, v) -> {
            if (v instanceof Map<?, ?> con) {
                phang((Map<String, Object>) con).forEach((kc, vc) -> ra.put(k + "." + kc, vc));
            } else {
                ra.put(k, v);
            }
        });
        return ra;
    }

    private static String git(String... lenh) {
        try {
            Process p = new ProcessBuilder(Stream.concat(Stream.of("git", "-C", GOC.toString()), Stream.of(lenh)).toList())
                .redirectErrorStream(true).start();
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
    private static Map<String, Object> doc(String duongDan) {
        try {
            return (Map<String, Object>) JSON.readValue(Files.readString(GOC.resolve(duongDan)), Map.class).get("nguon");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
