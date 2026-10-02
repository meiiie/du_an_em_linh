package vn.hoctoanai.core.shared.infrastructure.math;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Client duy nhất của core tới {@code services/math} (research R2). Đóng mặc định: mọi lỗi (hết giờ, không kết nối,
 * HTTP lỗi hay mã ngoài 2xx, thân không phải đối tượng JSON, phong bì lỗi của sandbox) thành {@link MathResult.Failed},
 * không bao giờ thành phán quyết «đạt». {@code timeout_s} gửi đi bị chặn trần dưới hết giờ phía core. Không gửi lại
 * khi lỗi. Không ghi payload vào log (có bài làm của học sinh).
 */
public class MathServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(MathServiceClient.class);
    private static final ParameterizedTypeReference<Map<String, @Nullable Object>> JSON_OBJECT =
        new ParameterizedTypeReference<Map<String, @Nullable Object>>() {};
    /** Khóa của phong bì lỗi mà {@code app/sandbox.py} và {@code app/job_runner.py} trả với HTTP 200. */
    private static final Set<String> KHOA_PHONG_BI_LOI = Set.of("ket_qua", "loai_ket_qua", "trang_thai", "ly_do", "cho_phep");
    /** Sandbox dừng job trước hết giờ phía core, để core nhận lý do thay vì chỉ thấy hết giờ. */
    private static final Duration DU_PHONG_SANDBOX = Duration.ofSeconds(2);
    private static final int DO_DAI_CHI_TIET_TOI_DA = 200;

    private final Map<MathJob, RestClient> clients = new EnumMap<>(MathJob.class);
    private final Function<MathJob, Duration> timeouts;

    public MathServiceClient(RestClient.Builder builder, URI baseUrl, Duration connectTimeout, Function<MathJob, Duration> timeouts) {
        this.timeouts = timeouts;
        HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(connectTimeout).build();
        for (MathJob job : MathJob.values()) {
            JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
            factory.setReadTimeout(timeouts.apply(job));
            clients.put(job, builder.clone().baseUrl(baseUrl.toString()).requestFactory(factory).build());
        }
    }

    public MathResult call(MathJob job, Map<String, ?> payload) {
        Map<String, @Nullable Object> body = new LinkedHashMap<>(payload);
        // Sandbox (app/sandbox.py, #104) gói chờ suất + chạy + dọn trong một hạn chót timeout_s, nên trần là hết giờ
        // phía core trừ dự phòng cho khởi động tiến trình và HTTP: job không giữ suất sau khi core đã thôi chờ.
        // Nơi gọi chỉ được đặt nhỏ hơn trần; giá trị không phải số thì dùng trần.
        long tran = timeoutSeconds(job);
        body.put("timeout_s", body.get("timeout_s") instanceof Number so ? Math.max(1, Math.min(so.longValue(), tran)) : tran);
        try {
            Map<String, @Nullable Object> response = clients.get(job).post()
                .uri(job.path())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                // Mọi mã ngoài 2xx (cả 3xx: JDK HttpClient không đi theo chuyển hướng) là lỗi, trước khi đọc thân
                .onStatus(status -> !status.is2xxSuccessful(), (request, res) -> {
                    throw new RestClientResponseException("HTTP " + res.getStatusCode().value(), res.getStatusCode(),
                        res.getStatusText(), res.getHeaders(), null, null);
                })
                .body(JSON_OBJECT);
            if (response == null) {
                return failed(job, MathResult.Reason.BAD_RESPONSE, "Thân phản hồi rỗng.");
            }
            if (isErrorEnvelope(response)) {
                return failed(job, MathResult.Reason.JOB_FAILED, sanitize(String.valueOf(response.get("ly_do"))));
            }
            return new MathResult.Ok(response);
        } catch (RestClientResponseException ex) {
            return failed(job, MathResult.Reason.HTTP_ERROR, "HTTP " + ex.getStatusCode().value());
        } catch (ResourceAccessException ex) {
            return isTimeout(ex)
                ? failed(job, MathResult.Reason.TIMEOUT, "Quá " + timeouts.apply(job).toMillis() + " ms.")
                : failed(job, MathResult.Reason.UNAVAILABLE, "Không kết nối được dịch vụ toán.");
        } catch (RestClientException ex) {
            return failed(job, MathResult.Reason.BAD_RESPONSE, "Thân phản hồi không phải đối tượng JSON.");
        }
    }

    private long timeoutSeconds(MathJob job) {
        return Math.max(1, timeouts.apply(job).minus(DU_PHONG_SANDBOX).toSeconds());
    }

    /** {@code ly_do} của sandbox có thể chứa {@code str(ex)} lặp lại đầu vào: bỏ ký tự điều khiển, cắt ngắn. */
    static String sanitize(String text) {
        String sach = text.replaceAll("\\p{Cntrl}+", " ").replaceAll("\\s+", " ").strip();
        return sach.length() <= DO_DAI_CHI_TIET_TOI_DA ? sach : sach.substring(0, DO_DAI_CHI_TIET_TOI_DA) + "…";
    }

    static boolean isErrorEnvelope(Map<String, ?> response) {
        return response.containsKey("ly_do")
            && "KHONG_KIEM_DUOC".equals(response.get("trang_thai"))
            && KHOA_PHONG_BI_LOI.containsAll(response.keySet());
    }

    private static boolean isTimeout(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof HttpTimeoutException || t instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }

    private static MathResult.Failed failed(MathJob job, MathResult.Reason reason, String detail) {
        // Chỉ ghi job và lý do chuẩn hóa; chi tiết có thể lặp lại dữ liệu đầu vào nên không vào log.
        LOG.warn("Dịch vụ toán {} lỗi: {}", job, reason);
        return new MathResult.Failed(reason, detail);
    }
}
