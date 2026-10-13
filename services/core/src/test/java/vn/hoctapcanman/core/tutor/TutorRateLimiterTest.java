package vn.hoctapcanman.core.tutor;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.tutor.application.service.TutorRateLimiter;
import vn.hoctapcanman.core.workspace.application.exception.WorkspaceException;

class TutorRateLimiterTest {
    @Test void eachStudentHasAnIndependentSlidingWindow() {
        AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-14T00:00:00Z"));
        Clock clock = new Clock() {
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zone) { return this; }
            public Instant instant() { return now.get(); }
        };
        var limiter = new TutorRateLimiter(clock, 2, Duration.ofMinutes(10));
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        limiter.acquire(a);
        limiter.acquire(a);
        assertEquals(429, assertThrows(WorkspaceException.class, () -> limiter.acquire(a)).status());
        assertDoesNotThrow(() -> limiter.acquire(b));
        now.set(now.get().plus(Duration.ofMinutes(10)).minusMillis(1));
        assertThrows(WorkspaceException.class, () -> limiter.acquire(a));
        now.set(now.get().plusMillis(1));
        assertDoesNotThrow(() -> limiter.acquire(a));
    }
    @Test void concurrentRequestsCannotExceedLimit() throws Exception {
        var limiter = new TutorRateLimiter(Clock.systemUTC(), 12, Duration.ofMinutes(10));
        UUID student = UUID.randomUUID();
        AtomicInteger accepted = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(24);
        for (int i = 0; i < 24; i++) Thread.startVirtualThread(() -> {
            try { start.await(); limiter.acquire(student); accepted.incrementAndGet(); }
            catch (WorkspaceException ignored) { }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            finally { done.countDown(); }
        });
        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals(12, accepted.get());
    }
}
