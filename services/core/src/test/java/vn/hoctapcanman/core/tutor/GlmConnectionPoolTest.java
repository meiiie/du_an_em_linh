package vn.hoctapcanman.core.tutor;

import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.tutor.infrastructure.ai.GlmConnectionPool;

class GlmConnectionPoolTest {
    private static HttpServer localServer() throws java.io.IOException {
        try { return HttpServer.create(new InetSocketAddress("127.0.0.1",0),0); }
        catch(java.net.SocketException ex) {
            if(!"Operation not permitted".equals(ex.getMessage()))throw ex;
            throw new org.opentest4j.TestAbortedException("Sandbox không cho mở cổng TCP cục bộ",ex);
        }
    }
    @Test void startupHeadHasNoCredentialsAndSharesKeepAliveConnectionWithChatTransport() throws Exception {
        var server=localServer();
        var warm=new CountDownLatch(1);
        var requests=new CopyOnWriteArrayList<String>();
        var ports=new CopyOnWriteArrayList<Integer>();
        var credentials=new CopyOnWriteArrayList<String>();
        server.createContext("/api",exchange -> {
            requests.add(exchange.getRequestMethod());ports.add(exchange.getRemoteAddress().getPort());
            credentials.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            exchange.sendResponseHeaders(200,-1);exchange.close();warm.countDown();
        });
        server.start();
        try(var pool=new GlmConnectionPool("http://127.0.0.1:"+server.getAddress().getPort()+"/api",Duration.ofSeconds(3),true)) {
            assertTrue(warm.await(3,TimeUnit.SECONDS));
            pool.client().post().uri("/chat/completions").header("Authorization","Bearer synthetic").retrieve().toBodilessEntity();
            pool.client().post().uri("/chat/completions").retrieve().toBodilessEntity();
            assertEquals(java.util.List.of("HEAD","POST","POST"),requests);
            assertEquals(java.util.List.of("null","Bearer synthetic","null"),credentials);
            assertEquals(1,ports.stream().distinct().count(),"Warm-up và chat phải dùng chung pool TCP");
        } finally { server.stop(0); }
    }
    @Test void unconfiguredProviderDoesNotWarmUp() throws Exception {
        var server=localServer();
        var requests=new java.util.concurrent.atomic.AtomicInteger();
        server.createContext("/api",exchange -> { requests.incrementAndGet();exchange.sendResponseHeaders(200,-1);exchange.close(); });
        server.start();
        try(var pool=new GlmConnectionPool("http://127.0.0.1:"+server.getAddress().getPort()+"/api",Duration.ofSeconds(3),false)) {
            pool.client().get().retrieve().toBodilessEntity();assertEquals(1,requests.get());
        } finally { server.stop(0); }
    }
}
