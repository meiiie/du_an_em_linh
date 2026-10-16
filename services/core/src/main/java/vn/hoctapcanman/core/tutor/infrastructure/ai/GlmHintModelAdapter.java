package vn.hoctapcanman.core.tutor.infrastructure.ai;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.tutor.application.dto.hocsinh.HintPrompt;
import vn.hoctapcanman.core.tutor.application.dto.TutorSessionDto.ModelUsage;
import vn.hoctapcanman.core.tutor.application.port.HintModel;

/** SSE của nhà → token nội dung nội bộ; không công bố reasoning, bản nháp hay lỗi chứa khóa. */
public final class GlmHintModelAdapter implements HintModel {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(GlmHintModelAdapter.class);
    private final io.micrometer.core.instrument.Counter unexpectedReasoning;
    private final RestClient client;
    private final String key;
    private final String model;
    private final String reasoningEffort;
    private static final java.util.regex.Pattern FORCED_THINKING_MODEL=java.util.regex.Pattern.compile(
        "glm-5\\.(?:[2-9]|[1-9][0-9]+)(?:-flash)?",java.util.regex.Pattern.CASE_INSENSITIVE);
    private final Duration timeout;
    private final int maxTokens;
    private final double temperature;
    private final double topP;
    private final Duration firstTokenTimeout;
    // Chừa thời gian lưu terminal/usage trước deadline toàn lượt 120 giây.
    private static final Duration MAX_BUDGET=Duration.ofSeconds(110);
    public GlmHintModelAdapter(RestClient client,String key,String model,Duration timeout,int maxTokens) {
        this(client,key,model,timeout,maxTokens,Duration.ofSeconds(20));
    }
    public GlmHintModelAdapter(RestClient client,String key,String model,Duration timeout,int maxTokens,Duration firstTokenTimeout) {
        this(client,key,model,timeout,maxTokens,firstTokenTimeout,0.8,0.9);
    }
    public GlmHintModelAdapter(RestClient client,String key,String model,Duration timeout,int maxTokens,Duration firstTokenTimeout,double temperature,double topP) {
        this(client,key,model,timeout,maxTokens,firstTokenTimeout,temperature,topP,io.micrometer.core.instrument.Metrics.globalRegistry);
    }
    public GlmHintModelAdapter(RestClient client,String key,String model,Duration timeout,int maxTokens,Duration firstTokenTimeout,double temperature,double topP,io.micrometer.core.instrument.MeterRegistry meters) {
        this(client,key,model,timeout,maxTokens,firstTokenTimeout,temperature,topP,"low",meters);
    }
    public GlmHintModelAdapter(RestClient client,String key,String model,Duration timeout,int maxTokens,Duration firstTokenTimeout,double temperature,double topP,String reasoningEffort,io.micrometer.core.instrument.MeterRegistry meters) {
        unexpectedReasoning=meters.counter("gia_su_glm.reasoning_khong_mong_doi");
        if(timeout.isZero() || timeout.isNegative() || firstTokenTimeout.isZero() || firstTokenTimeout.isNegative())
            throw new IllegalArgumentException("Thời gian chờ phải dương.");
        if(!Double.isFinite(temperature) || temperature<=0 || temperature>1 || !Double.isFinite(topP) || topP<=0 || topP>1)
            throw new IllegalArgumentException("Tham số lấy mẫu phải lớn hơn 0 và không quá 1.");
        this.client=client;this.key=key;this.model=model;this.timeout=timeout;this.maxTokens=maxTokens;
        this.firstTokenTimeout=firstTokenTimeout;
        this.temperature=temperature;this.topP=topP;this.reasoningEffort=reasoningEffort;
    }
    @Override public String provider(){return "GLM";}
    @Override public Optional<String> draft(HintPrompt prompt) {
        var text=new StringBuilder();
        try {stream(prompt,text::append);return text.isEmpty()?Optional.empty():Optional.of(text.toString());}
        catch(RuntimeException ex){return Optional.empty();}
    }
    @Override public ModelUsage stream(HintPrompt prompt,Consumer<String> tokens) {
        return stream(prompt,tokens,new Cancellation());
    }
    @Override public ModelUsage stream(HintPrompt prompt,Consumer<String> tokens,Cancellation cancellation) {
        long deadline=System.nanoTime()+MAX_BUDGET.toNanos();
        for(int retry=0;retry<=1;retry++) {
            cancellation.check();
            if(System.nanoTime()>=deadline)throw unavailable();
            var attempt=new Attempt(Math.min(deadline,System.nanoTime()+boundedNanos(timeout)));
            try { return runAttempt(prompt,tokens,cancellation,attempt); }
            catch(RetryableFailure ex) {
                cancellation.check();
                if(retry==1 || attempt.emitted())throw unavailable();
                backoff(cancellation,deadline);
            }
        }
        throw unavailable();
    }
    private ModelUsage runAttempt(HintPrompt prompt,Consumer<String> tokens,Cancellation parent,Attempt attempt) {
        FutureTask<ModelUsage> task=new FutureTask<>(() -> call(prompt,text -> {
            attempt.beforeToken();
            tokens.accept(text); // Không giữ khóa và không interrupt khi consumer có thể đang ghi JDBC.
            attempt.cancellation.check();
        },attempt));
        AutoCloseable stop=() -> { task.cancel(false);attempt.cancellation.cancel(); };
        parent.attach(stop);
        try {
            Thread.startVirtualThread(task);
            for(;;) {
                parent.check();
                if(!task.isDone() && attempt.expire())throw new RetryableFailure();
                try { return task.get(Math.max(1,attempt.remaining()),TimeUnit.NANOSECONDS); }
                catch(TimeoutException ex) { /* Kiểm lại hạn idle: hoạt động vừa nhận có thể đã dời hạn. */ }
            }
        } catch(ExecutionException ex) {
            parent.check();
            if(ex.getCause() instanceof RuntimeException runtime)throw runtime;
            throw unavailable();
        } catch(InterruptedException ex) {
            parent.cancel();Thread.currentThread().interrupt();throw new CancellationException();
        } finally {
            attempt.stop();parent.detach(stop);task.cancel(false);attempt.cancellation.cancel();
        }
    }
    private static long boundedNanos(Duration duration) {
        return duration.compareTo(MAX_BUDGET)>0?MAX_BUDGET.toNanos():duration.toNanos();
    }
    private static void backoff(Cancellation cancellation,long deadline) {
        // neko-core retryDelayMs: jitter đối xứng ±20%, chỉ một lần retry cùng nhà.
        long delay=ThreadLocalRandom.current().nextLong(640,961);
        var cancelled=new CountDownLatch(1);
        AutoCloseable stop=cancelled::countDown;
        cancellation.attach(stop);
        try {
            if(deadline-System.nanoTime()<=TimeUnit.MILLISECONDS.toNanos(delay))throw unavailable();
            cancelled.await(delay,TimeUnit.MILLISECONDS);
            cancellation.check();
        } catch(InterruptedException ex) {
            cancellation.cancel();Thread.currentThread().interrupt();throw new CancellationException();
        } finally { cancellation.detach(stop); }
    }
    private final class Attempt {
        final Cancellation cancellation=new Cancellation();
        private final long deadline;
        private long idleDeadline=System.nanoTime()+boundedNanos(firstTokenTimeout);
        private boolean emitted,stopped,reasoningReported;
        synchronized void reasoningWithoutContent() {
            if(reasoningReported)return;
            reasoningReported=true;
            LOG.warn("gia_su_glm reasoning_khong_mong_doi");
            unexpectedReasoning.increment();
        }
        Attempt(long deadline) { this.deadline=deadline; }
        synchronized boolean emitted() { return emitted; }
        synchronized long remaining() { return Math.min(deadline,idleDeadline)-System.nanoTime(); }
        synchronized boolean expire() {
            if(remaining()>0)return false;
            stopped=true;return true;
        }
        synchronized void stop() { stopped=true; }
        synchronized void beforeToken() {
            cancellation.check();
            if(stopped)throw new CancellationException();
            if(remaining()<=0)throw new RetryableFailure();
            // Ghim trước callback: timeout không được replay ngay cả khi callback chưa trả về.
            emitted=true;idleDeadline=System.nanoTime()+boundedNanos(firstTokenTimeout);
        }
        synchronized void activity() {
            // Heartbeat/reasoning không kéo dài hạn token nội dung đầu tiên.
            if(emitted)idleDeadline=System.nanoTime()+boundedNanos(firstTokenTimeout);
        }
    }
    private static final class RetryableFailure extends IllegalStateException {
        RetryableFailure() { super("MODEL_UNAVAILABLE"); }
    }
    private ModelUsage call(HintPrompt prompt,Consumer<String> tokens,Attempt attempt) {
        var cancellation=attempt.cancellation;
        cancellation.check();
        Map<String,Object> body=new LinkedHashMap<>();
        body.put("model",model);
        body.put("max_tokens",maxTokens);
        body.put("stream",true);
        // Prompt tóm tắt do core dựng, không phân loại từ dữ liệu học sinh.
        body.put("temperature",prompt.system().startsWith("Tóm tắt hội thoại học toán")?0.2:temperature);
        body.put("top_p",topP);
        boolean forcedThinking=FORCED_THINKING_MODEL.matcher(model).matches();
        body.put("thinking",Map.of("type",forcedThinking?"enabled":"disabled"));
        if(forcedThinking && !reasoningEffort.isBlank())body.put("reasoning_effort",reasoningEffort);
        body.put("messages",List.of(
            Map.of("role","system","content",prompt.system()),Map.of("role","user","content",prompt.user())));
        try { return client.post().uri("/chat/completions").header(HttpHeaders.AUTHORIZATION,"Bearer "+key)
            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM).body(body)
            .exchange((request,response) -> {
                cancellation.check();
                int status=response.getStatusCode().value();
                if(status==429 || status>=500 && status<=599)throw new RetryableFailure();
                if(!response.getStatusCode().is2xxSuccessful())throw unavailable();
                if(response.getHeaders().getContentType()==null || !MediaType.TEXT_EVENT_STREAM.isCompatibleWith(response.getHeaders().getContentType()))throw unavailable();
                InputStream bodyStream=response.getBody();
                cancellation.attach(bodyStream);
                var activityStream=new FilterInputStream(bodyStream) {
                    @Override public int read() throws IOException {
                        int value=in.read();if(value>=0)attempt.activity();return value;
                    }
                    @Override public int read(byte[] bytes,int offset,int length) throws IOException {
                        int count=in.read(bytes,offset,length);if(count>0)attempt.activity();return count;
                    }
                };
                try(var reader=new BufferedReader(new InputStreamReader(activityStream,StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)))) {
                    return parse(reader,prompt,tokens,attempt);
                } catch(java.nio.charset.CharacterCodingException ex) { throw unavailable(); }
                catch(IOException ex) { cancellation.check();throw new RetryableFailure(); }
                finally {cancellation.detach(bodyStream);}
            }); } catch(ResourceAccessException ex) { cancellation.check();throw new RetryableFailure(); }
    }
    private ModelUsage parse(BufferedReader reader,HintPrompt prompt,Consumer<String> tokens,Attempt attempt)throws IOException {
        var cancellation=attempt.cancellation;
        StringBuilder event=new StringBuilder(),content=new StringBuilder();
        ModelUsage usage=null;
        boolean finish=false;
        for(String line;(line=reader.readLine())!=null;) {
            cancellation.check();
            if(line.length()>65536)throw unavailable();
            if(line.startsWith("data:")) {
                if(!event.isEmpty())event.append('\n');
                event.append(line.substring(5).stripLeading());
                if(event.length()>65536)throw unavailable();
            } else if(line.isEmpty() && !event.isEmpty()) {
                String data=event.toString();event.setLength(0);
                if(data.equals("[DONE]")) {
                    if(!finish || content.toString().isBlank())throw unavailable();
                    return usage!=null?usage:new ModelUsage(HintModel.estimate(prompt.system()+prompt.user()),HintModel.estimate(content.toString()),true);
                }
                final tools.jackson.databind.JsonNode node;
                try {node=JSON.readTree(data);}catch(RuntimeException ex){throw unavailable();}
                if(node==null || !node.isObject() || node.has("error"))throw unavailable();
                var choices=node.path("choices");
                if(!choices.isArray() || choices.size()>1 || choices.isEmpty() && !node.path("usage").isObject())throw unavailable();
                var choice=choices.path(0);
                if(!choices.isEmpty() && (!choice.isObject() || !choice.path("delta").isObject()))throw unavailable();
                var reason=choice.path("finish_reason");
                if(!reason.isMissingNode() && !reason.isNull() && !reason.isString())throw unavailable();
                if(reason.isString()) {
                    if(!reason.asString().equals("stop") && !reason.asString().equals("length"))throw unavailable();

                }
                var delta=choice.path("delta");
                if(delta.has("tool_calls") || delta.has("function_call"))throw unavailable();
                var text=delta.path("content");
                if(delta.path("reasoning_content").isString() && !delta.path("reasoning_content").asString().isBlank()
                    && (!text.isString() || text.asString().isEmpty()))attempt.reasoningWithoutContent();
                if(text.isString() && !text.asString().isEmpty()) {
                    if(content.length()+text.asString().length()>12000)throw unavailable();
                    if(finish)throw unavailable();
                    content.append(text.asString());tokens.accept(text.asString());
                } else if(!text.isMissingNode() && !text.isNull() && !text.isString())throw unavailable();
                if(reason.isString())finish=true;
                var u=node.path("usage");
                if(u.path("prompt_tokens").isIntegralNumber() && u.path("completion_tokens").isIntegralNumber()) {
                    int input=u.path("prompt_tokens").asInt(),output=u.path("completion_tokens").asInt();
                    if(input<0 || output<0)throw unavailable();
                    usage=new ModelUsage(input,output,false);
                }
            }
        }
        // EOF thiếu [DONE] là lỗi.
        cancellation.check();
        throw unavailable();
    }
    private static IllegalStateException unavailable(){return new IllegalStateException("MODEL_UNAVAILABLE");}
}
