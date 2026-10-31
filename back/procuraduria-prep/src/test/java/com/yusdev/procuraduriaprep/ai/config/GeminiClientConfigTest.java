package com.yusdev.procuraduriaprep.ai.config;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

class GeminiClientConfigTest {

    @Test
    void appliesResponseTimeoutIndependentlyFromConnectionTimeout() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                Thread.sleep(500);
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
        server.start();

        try {
            var client = new GeminiClientConfig()
                    .restClientBuilder(Duration.ofSeconds(2), Duration.ofMillis(100))
                    .build();
            String uri = "http://127.0.0.1:" + server.getAddress().getPort() + "/";

            assertThrows(ResourceAccessException.class,
                    () -> client.get().uri(uri).retrieve().body(String.class));
        } finally {
            server.stop(0);
        }
    }
}
