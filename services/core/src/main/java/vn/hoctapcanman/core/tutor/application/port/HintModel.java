package vn.hoctapcanman.core.tutor.application.port;

import java.util.Optional;
import java.util.function.Consumer;
import vn.hoctapcanman.core.tutor.application.dto.hocsinh.HintPrompt;
import vn.hoctapcanman.core.tutor.application.dto.TutorSessionDto.ModelUsage;

/** Chỉ token nội dung; bên nhận phải kiểm trước khi phát. */
public interface HintModel {
    Optional<String> draft(HintPrompt prompt);
    String provider();
    default HintModel selected(String provider) { return this; }
    default ModelUsage stream(HintPrompt prompt, Consumer<String> tokens) {
        String text = draft(prompt).filter(s -> !s.isBlank()).orElseThrow(() -> new IllegalStateException("MODEL_UNAVAILABLE"));
        tokens.accept(text);
        return new ModelUsage(estimate(prompt.system() + prompt.user()), estimate(text), true);
    }
    default ModelUsage stream(HintPrompt prompt, Consumer<String> tokens, Cancellation cancellation) {
        cancellation.check();
        ModelUsage usage=stream(prompt,t -> { cancellation.check(); tokens.accept(t); cancellation.check(); });
        cancellation.check();
        return usage;
    }
    /** Hủy hợp tác: đóng HTTP, tuyệt đối không ngắt thread có thể đang gọi JDBC. */
    final class Cancellation {
        private final java.util.concurrent.atomic.AtomicBoolean cancelled=new java.util.concurrent.atomic.AtomicBoolean();
        private final java.util.Set<AutoCloseable> resources=java.util.concurrent.ConcurrentHashMap.newKeySet();
        public void check() { if(cancelled.get())throw new java.util.concurrent.CancellationException(); }
        public void attach(AutoCloseable resource) {
            resources.add(resource);
            if(cancelled.get()) { detach(resource); close(resource); check(); }
        }
        public void detach(AutoCloseable resource) { resources.remove(resource); }
        public void cancel() {
            cancelled.set(true);
            for(AutoCloseable resource:resources)if(resources.remove(resource))close(resource);
        }
        private static void close(AutoCloseable resource) {
            try { resource.close(); } catch(Exception ignored) { /* Cờ hủy vẫn có hiệu lực khi đóng HTTP lỗi. */ }
        }
    }
    static int estimate(String text) { return (text.length() + 3) / 4; }
}
