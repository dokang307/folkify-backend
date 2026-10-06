package com.folkify.practice.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.folkify.common.exception.ApiException;
import com.folkify.common.exception.ErrorCode;
import com.folkify.practice.config.PracticeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/** Gọi service DSP folkify_ai (nội bộ). Lỗi audio → 1312, service lỗi/không kết nối được → 1313. */
@Component
public class AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);
    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP = new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AiServiceClient(PracticeProperties properties, RestClient.Builder builder, ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getTimeoutMs());
        factory.setReadTimeout(properties.getTimeoutMs());
        this.restClient = builder.baseUrl(properties.getBaseUrl()).requestFactory(factory).build();
        this.objectMapper = objectMapper;
    }

    /** Chấm bản ghi so với đường cao độ bản mẫu của tác phẩm (AI tự dò đoạn đã chơi). */
    public AiAnalysis analyze(byte[] audio, String filename, String instrumentSlug, Map<String, Object> referenceContour) {
        MultiValueMap<String, Object> form = baseForm(audio, filename, instrumentSlug);
        form.add("reference", toJson(referenceContour));
        return AiAnalysis.from(post("/analyze", form));
    }

    public Map<String, Object> extractReference(byte[] audio, String filename, String instrumentSlug) {
        return post("/extract-reference", baseForm(audio, filename, instrumentSlug));
    }

    private Map<String, Object> post(String path, MultiValueMap<String, Object> form) {
        try {
            Map<String, Object> body = restClient.post().uri(path)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()
                    .body(JSON_MAP);
            if (body == null) {
                throw new ApiException(ErrorCode.AI_SERVICE_UNAVAILABLE);
            }
            return body;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.UNPROCESSABLE_ENTITY)
                    || e.getStatusCode().isSameCodeAs(HttpStatus.PAYLOAD_TOO_LARGE)) {
                throw new ApiException(ErrorCode.AUDIO_INVALID, detail(e));
            }
            log.warn("folkify_ai từ chối request {}: {}", path, e.getStatusCode());
            throw new ApiException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        } catch (RestClientException e) {
            log.error("Không gọi được folkify_ai {}", path, e);
            throw new ApiException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    private MultiValueMap<String, Object> baseForm(byte[] audio, String filename, String instrumentSlug) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(audio) {
            @Override
            public String getFilename() {
                return filename != null && !filename.isBlank() ? filename : "recording";
            }
        });
        if (instrumentSlug != null) {
            form.add("instrument", instrumentSlug);
        }
        return form;
    }

    private String detail(HttpClientErrorException e) {
        try {
            Object detail = e.getResponseBodyAs(JSON_MAP).get("detail");
            return detail instanceof String s ? s : ErrorCode.AUDIO_INVALID.getMessage();
        } catch (RuntimeException ignored) {
            return ErrorCode.AUDIO_INVALID.getMessage();
        }
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new ApiException(ErrorCode.UNEXPECTED_ERROR);
        }
    }
}
