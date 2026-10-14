package vn.hoctapcanman.core.tutor.infrastructure.ai;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.tutor.application.dto.hocsinh.HintPrompt;
import vn.hoctapcanman.core.tutor.application.dto.TutorSessionDto.ModelUsage;
import vn.hoctapcanman.core.tutor.application.port.HintModel;

/** SSE của nhà → token nội dung nội bộ; không công bố reasoning, bản nháp hay lỗi chứa khóa. */
public final class GlmHintModelAdapter implements HintModel {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private final RestClient client;
    private final String key;
    private final String model;
    private final Duration timeout;
    private final int maxTokens;
    public GlmHintModelAdapter(RestClient client,String key,String model,Duration timeout,int maxTokens) {
        this.client=client;this.key=key;this.model=model;this.timeout=timeout;this.maxTokens=maxTokens;
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
        cancellation.check();
        FutureTask<ModelUsage> task=new FutureTask<>(() -> call(prompt,tokens,cancellation));
        AutoCloseable stop=() -> task.cancel(false);
        cancellation.attach(stop);
        try {
            Thread.startVirtualThread(task);
            return task.get(timeout.toNanos(),TimeUnit.NANOSECONDS);
        }
        catch(ExecutionException ex) {
            cancellation.check();
            if(ex.getCause() instanceof RuntimeException runtime)throw runtime;
            throw unavailable();
        } catch(InterruptedException ex){cancellation.cancel();Thread.currentThread().interrupt();throw new CancellationException();}
        catch(TimeoutException ex){cancellation.cancel();throw unavailable();}
        finally {cancellation.detach(stop);if(!task.isDone())task.cancel(false);}
    }
    private ModelUsage call(HintPrompt prompt,Consumer<String> tokens,Cancellation cancellation) {
        cancellation.check();
        Map<String,Object> body=Map.of("model",model,"max_tokens",maxTokens,"stream",true,
            "thinking",Map.of("type","disabled"),"messages",List.of(
                Map.of("role","system","content",prompt.system()),Map.of("role","user","content",prompt.user())));
        // Không retry tự động hoặc chuyển nhà.
        return client.post().uri("/chat/completions").header(HttpHeaders.AUTHORIZATION,"Bearer "+key)
            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.TEXT_EVENT_STREAM).body(body)
            .exchange((request,response) -> {
                if(!response.getStatusCode().is2xxSuccessful())throw unavailable();
                if(response.getHeaders().getContentType()==null || !MediaType.TEXT_EVENT_STREAM.isCompatibleWith(response.getHeaders().getContentType()))throw unavailable();
                InputStream bodyStream=response.getBody();
                cancellation.attach(bodyStream);
                try(var reader=new BufferedReader(new InputStreamReader(bodyStream,StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)))) {
                    return parse(reader,prompt,tokens,cancellation);
                } finally {cancellation.detach(bodyStream);}
            });
    }
    private ModelUsage parse(BufferedReader reader,HintPrompt prompt,Consumer<String> tokens,Cancellation cancellation)throws IOException {
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
                if(node.has("error"))throw unavailable();
                var choice=node.path("choices").path(0);
                var reason=choice.path("finish_reason");
                if(reason.isString()) {
                    if(!reason.asString().equals("stop"))throw unavailable();

                }
                var delta=choice.path("delta");
                if(delta.has("tool_calls"))throw unavailable();
                var text=delta.path("content");
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
