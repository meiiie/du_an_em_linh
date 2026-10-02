package vn.hoctoanai.core.shared.infrastructure.math;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * T009 (#82): máy chủ giả trả 200 đúng, 500, 404, JSON hỏng, mảng JSON, phong bì lỗi của sandbox, hết giờ, và không
 * kết nối được. Chỉ 200 với {@code ket_qua = DAT} mới là đạt; mọi lỗi thành {@link MathResult.Failed}.
 */
@DisplayName("Client dịch vụ toán đóng mặc định")
class MathServiceClientTest {

    private static final Duration HET_GIO = Duration.ofMillis(500);
    private static final String PHONG_BI_LOI = "{\"ket_qua\":\"KHONG_KIEM_DUOC\",\"loai_ket_qua\":\"KHONG_KIEM_DUOC\","
        + "\"trang_thai\":\"KHONG_KIEM_DUOC\",\"ly_do\":\"Job SymPy vượt quá 20s và đã bị dừng.\",\"cho_phep\":false}";

    private static HttpServer server;
    private static MathServiceClient client;

    @BeforeAll
    static void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        // 200 đúng: trả lại thân đã nhận để kiểm payload gửi đi
        server.createContext(MathJob.GRADE.path(), ex -> tra(ex, 200, "{\"ket_qua\":\"DAT\",\"thong_bao\":\"ok\",\"nhan\":" + doc(ex) + "}"));
        server.createContext(MathJob.VERIFY.path(), ex -> tra(ex, 500, "{\"detail\":\"lỗi\"}"));
        server.createContext(MathJob.FILTER.path(), ex -> tra(ex, 200, "{không phải json"));
        server.createContext(MathJob.GOI_Y.path(), ex -> tra(ex, 200, "[1, 2, 3]"));
        server.createContext(MathJob.GENERATE.path(), ex -> tra(ex, 200, PHONG_BI_LOI));
        server.createContext(MathJob.KIEM_DONG_CONG_THUC.path(), ex -> {
            try {
                Thread.sleep(HET_GIO.multipliedBy(4));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            tra(ex, 200, "{\"dong\":[]}");
        });
        // MathJob.KIEM_LOI_GIANG không đăng ký: HttpServer trả 404
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        client = new MathServiceClient(RestClient.builder(), URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
            Duration.ofSeconds(1), job -> HET_GIO);
    }

    @AfterAll
    static void stop() {
        server.stop(0);
    }

    @Test
    @DisplayName("200 với ket_qua = DAT là đạt; payload đi kèm timeout_s nhỏ hơn hết giờ phía core")
    void datKhi200() {
        MathResult r = client.call(MathJob.GRADE, Map.of("ham", "x**3 - 3*x", "cac_buoc", List.of()));
        assertThat(r).isInstanceOf(MathResult.Ok.class);
        assertThat(r.isDat("ket_qua")).isTrue();
        @SuppressWarnings("unchecked")
        Map<String, Object> nhan = (Map<String, Object>) ((MathResult.Ok) r).body().get("nhan");
        assertThat(nhan).containsEntry("ham", "x**3 - 3*x").containsEntry("timeout_s", 1);
    }

    @Test
    @DisplayName("timeout_s do nơi gọi đặt thì giữ nguyên")
    void giuTimeoutCuaNoiGoi() {
        MathResult r = client.call(MathJob.GRADE, Map.of("timeout_s", 5));
        @SuppressWarnings("unchecked")
        Map<String, Object> nhan = (Map<String, Object>) ((MathResult.Ok) r).body().get("nhan");
        assertThat(nhan).containsEntry("timeout_s", 5);
    }

    @Test
    @DisplayName("HTTP 500 → HTTP_ERROR, không đạt")
    void http500() {
        assertThatFailed(client.call(MathJob.VERIFY, Map.of()), MathResult.Reason.HTTP_ERROR);
    }

    @Test
    @DisplayName("Đường chưa có (404) → HTTP_ERROR, không đạt")
    void http404() {
        MathResult r = client.call(MathJob.KIEM_LOI_GIANG, Map.of("cau", "…"));
        assertThatFailed(r, MathResult.Reason.HTTP_ERROR);
        assertThat(((MathResult.Failed) r).detail()).isEqualTo("HTTP 404");
    }

    @Test
    @DisplayName("JSON hỏng → BAD_RESPONSE, không đạt")
    void jsonHong() {
        assertThatFailed(client.call(MathJob.FILTER, Map.of("ban_nhap", "…")), MathResult.Reason.BAD_RESPONSE);
    }

    @Test
    @DisplayName("Mảng JSON thay vì đối tượng → BAD_RESPONSE, không đạt")
    void mangJson() {
        assertThatFailed(client.call(MathJob.GOI_Y, Map.of()), MathResult.Reason.BAD_RESPONSE);
    }

    @Test
    @DisplayName("Phong bì lỗi của sandbox (HTTP 200) → JOB_FAILED, không đạt")
    void phongBiLoiCuaSandbox() {
        MathResult r = client.call(MathJob.GENERATE, Map.of());
        assertThatFailed(r, MathResult.Reason.JOB_FAILED);
        assertThat(((MathResult.Failed) r).detail()).contains("vượt quá");
    }

    @Test
    @DisplayName("Hết giờ → TIMEOUT, không chờ máy chủ chậm")
    void hetGio() {
        long batDau = System.nanoTime();
        MathResult r = client.call(MathJob.KIEM_DONG_CONG_THUC, Map.of("dong", List.of()));
        assertThatFailed(r, MathResult.Reason.TIMEOUT);
        assertThat(Duration.ofNanos(System.nanoTime() - batDau)).isLessThan(HET_GIO.multipliedBy(3));
    }

    @Test
    @DisplayName("Không kết nối được → UNAVAILABLE, không đạt")
    void khongKetNoi() throws IOException {
        int cong;
        try (ServerSocket s = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            cong = s.getLocalPort();
        }
        MathServiceClient tat = new MathServiceClient(RestClient.builder(), URI.create("http://127.0.0.1:" + cong),
            Duration.ofSeconds(1), job -> HET_GIO);
        assertThatFailed(tat.call(MathJob.GRADE, Map.of()), MathResult.Reason.UNAVAILABLE);
    }

    @Test
    @DisplayName("Phản hồi thật có KHONG_KIEM_DUOC kèm trường riêng của job không bị nhầm là phong bì lỗi")
    void phanHoiThatKhongPhaiPhongBi() {
        assertThat(MathServiceClient.isErrorEnvelope(Map.of("ket_qua", "KHONG_KIEM_DUOC", "trang_thai", "KHONG_KIEM_DUOC",
            "ly_do", "…", "thong_bao", "Bước này máy chưa chấm được."))).isFalse();
        assertThat(MathServiceClient.isErrorEnvelope(Map.of("cho_phep", false, "ly_do", "LO_KET_QUA"))).isFalse();
    }

    private static void assertThatFailed(MathResult r, MathResult.Reason reason) {
        assertThat(r).isInstanceOfSatisfying(MathResult.Failed.class, f -> assertThat(f.reason()).isEqualTo(reason));
        assertThat(r.isDat("ket_qua")).isFalse();
        assertThat(r.isDat("trang_thai")).isFalse();
    }

    private static String doc(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void tra(HttpExchange ex, int status, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, b.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(b);
        }
    }
}
