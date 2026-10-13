package vn.hoctapcanman.core.tutor.application.service;

import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;

/** Cửa sổ trượt dùng chung giữa các lượt SSE của mỗi học sinh trong một tiến trình. */
@Service
public class TutorRateLimiter {
    private final Clock clock;
    private final int limit;
    private final Duration window;
    private final Map<UUID, Deque<Instant>> requests = new HashMap<>();

    public TutorRateLimiter(Clock clock,
        @Value("${mathl.gia-su.rate-limit.requests:12}") int limit,
        @Value("${mathl.gia-su.rate-limit.window:10m}") Duration window) {
        if (limit < 1 || window.isZero() || window.isNegative()) throw new IllegalArgumentException("Giới hạn gia sư phải dương.");
        this.clock = clock;
        this.limit = limit;
        this.window = window;
    }

    public synchronized void acquire(UUID student) {
        Instant now = clock.instant(), cutoff = now.minus(window);
        // Dọn cả học sinh không còn gửi yêu cầu để không giữ định danh vô thời hạn.
        requests.values().forEach(times -> { while (!times.isEmpty() && !times.peekFirst().isAfter(cutoff)) times.removeFirst(); });
        requests.values().removeIf(Deque::isEmpty);
        Deque<Instant> times = requests.computeIfAbsent(student, ignored -> new ArrayDeque<>());
        if (times.size() >= limit) throw new WorkspaceException(429,
            "Em đã hỏi nhiều lần trong thời gian ngắn. Em chờ một chút rồi thử lại nhé.");
        times.addLast(now);
    }
}
