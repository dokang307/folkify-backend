package com.folkify.practice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.folkify.common.exception.ApiException;
import com.folkify.common.exception.ErrorCode;
import com.folkify.practice.config.PracticeProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Chạy server HTTP giả (JDK built-in) để kiểm tra multipart gửi đi và ánh xạ lỗi. */
class AiServiceClientTest {

    HttpServer server;
    final AtomicReference<String> lastBody = new AtomicReference<>();

    AiServiceClient start(int status, String json) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1));
            byte[] out = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        server.start();
        return client("http://127.0.0.1:" + server.getAddress().getPort()); // scalability-ok: loopback test stub, ephemeral port
    }

    static AiServiceClient client(String baseUrl) {
        PracticeProperties props = new PracticeProperties();
        props.setBaseUrl(baseUrl);
        props.setTimeoutMs(5000);
        return new AiServiceClient(props, RestClient.builder(), new ObjectMapper());
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    @Test
    void analyze_sendsReferenceAndParsesResult() throws IOException {
        AiServiceClient client = start(200,
                "{\"overall\":81,\"pitchScore\":80,\"rhythmScore\":70,\"stabilityScore\":90,\"feedback\":[\"Khá tốt\"]}");
        AiAnalysis a = client.analyze(new byte[]{1, 2}, "take.webm", "sao-truc", Map.of("midi", List.of(67.0)));
        assertThat(a.overall()).isEqualTo(81);
        assertThat(a.rhythmScore()).isEqualTo(70);
        assertThat(a.feedback()).containsExactly("Khá tốt");
        assertThat(lastBody.get()).doesNotContain("name=\"mode\"")
                .contains("name=\"reference\"").contains("filename=\"take.webm\"").contains("sao-truc");
    }

    @Test
    void unprocessableAudio_mapsToAudioInvalidWithDetail() throws IOException {
        AiServiceClient client = start(422, "{\"detail\":\"Bản ghi quá ngắn\"}");
        assertThatThrownBy(() -> client.analyze(new byte[]{1}, "x.webm", null, null))
                .isInstanceOf(ApiException.class)
                .hasMessage("Bản ghi quá ngắn")
                .extracting("errorCode").isEqualTo(ErrorCode.AUDIO_INVALID);
    }

    @Test
    void serverError_mapsToServiceUnavailable() throws IOException {
        AiServiceClient client = start(500, "{}");
        assertThatThrownBy(() -> client.analyze(new byte[]{1}, "x.webm", null, null))
                .extracting("errorCode").isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE);
    }

    @Test
    void unreachableService_mapsToServiceUnavailable() {
        AiServiceClient client = client("http://127.0.0.1:1"); // scalability-ok: deliberately unreachable port
        assertThatThrownBy(() -> client.analyze(new byte[]{1}, "x.webm", null, null))
                .extracting("errorCode").isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE);
    }
}
