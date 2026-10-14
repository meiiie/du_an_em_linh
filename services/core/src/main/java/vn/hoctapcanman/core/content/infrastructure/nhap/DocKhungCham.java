package vn.hoctapcanman.core.content.infrastructure.nhap;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import vn.hoctapcanman.core.content.application.dto.KhungCham;
import vn.hoctapcanman.core.content.application.dto.hocsinh.MoTaBuoc;

/** Đọc allowlist metadata; san_sang_ui trong nguồn ghi trạng thái trước khi nối UI, không phải khả năng CAS. */
final class DocKhungCham {
    private static final tools.jackson.databind.json.JsonMapper JSON = tools.jackson.databind.json.JsonMapper.builder()
        .enable(tools.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build();
    private static final Set<String> KIEU = Set.of("BIEU_THUC", "TAP_HUU_HAN", "DAY_GIA_TRI", "MIEN", "NGUYEN_HAM", "BINH_PHUONG");
    private static final List<String> DH = List.of("B.DH.TXD", "B.DH.DAOHAM", "B.DH.NGHIEM", "B.DH.XETDAU", "B.DH.KETLUAN");
    private DocKhungCham() {}

    static String noiDung(KhungCham khung) { return JSON.writeValueAsString(khung); }

    @SuppressWarnings("unchecked")
    static Map<String, KhungCham> doc(NguonNoiDung nguon) {
        var source = nguon.doiTuong("dev/nguyen-trai/cham-buoc.json");
        int version = ((Number) source.get("phien_ban")).intValue();
        var bank = (List<Map<String, Object>>) source.get("bai");
        return bank.stream().collect(Collectors.toMap(b -> (String) b.get("ma_bai"), b -> {
            if ("B.DH".equals(b.get("ho"))) return new KhungCham(version, DH.stream().map(MoTaBuoc::cu).toList(), Map.of());
            String family = (String) b.get("ho");
            List<String> codes = switch (family) {
                case "B.GT" -> List.of("MIEN", "BIENDOI", "TRUNGGIAN", "TINH", "KETLUAN");
                case "B.XS" -> List.of("DUKIEN", "THANHPHAN1", "THANHPHAN2", "KETHOP", "KETLUAN");
                default -> throw new IllegalArgumentException("Họ khung chưa hỗ trợ");
            };
            var steps = ((List<Map<String, Object>>) b.get("cac_buoc")).stream().map(s -> new MoTaBuoc(
                (String) s.get("ma_buoc"), (String) s.get("ten"), (String) s.get("viec"), (String) s.get("kieu"))).toList();
            if (!steps.stream().map(MoTaBuoc::maBuoc).toList().equals(codes.stream().map(c -> family + "." + c).toList())
                || steps.stream().anyMatch(s -> !KIEU.contains(s.kieu()))) throw new IllegalArgumentException("Khung năm bước không hợp lệ");
            var data = (Map<String, Object>) b.get("bai_toan");
            return new KhungCham(version, steps, Map.of("loai", data.get("loai"), "du_kien", data.get("du_kien")));
        }));
    }
}
