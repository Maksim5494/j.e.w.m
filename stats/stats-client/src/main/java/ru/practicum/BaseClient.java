package ru.practicum;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class BaseClient {
    protected final RestTemplate restTemplate;

    protected <T> ResponseEntity<T> get(String uri,
                                        @Nullable Map<String, Object> parameters,
                                        Class<T> responseType) {
        return sendRequest(uri, HttpMethod.GET, null, parameters, responseType);
    }

    protected <T, B> ResponseEntity<T> post(String uri, @Nullable B body,
                                            @Nullable Map<String, Object> parameters,
                                            Class<T> responseType) {
        return sendRequest(uri, HttpMethod.POST, body, parameters, responseType);
    }

    protected <T> ResponseEntity<T> get(String uri,
                                        @Nullable Map<String, Object> parameters,
                                        ParameterizedTypeReference<T> responseType) {
        return sendRequest(uri, HttpMethod.GET, null, parameters, responseType);
    }

    private <T, B> ResponseEntity<T> sendRequest(String uri, HttpMethod method,
                                                 @Nullable B body,
                                                 @Nullable Map<String, Object> parameters,
                                                 Class<T> responseType) {
        HttpEntity<B> request = new HttpEntity<>(body, defaultHeaders());

        if (parameters == null) {
            return restTemplate.exchange(uri, method, request, responseType);
        } else {
            return restTemplate.exchange(uri, method, request, responseType, parameters);
        }
    }

    private <T, B> ResponseEntity<T> sendRequest(String uri, HttpMethod method,
                                                 @Nullable B body,
                                                 @Nullable Map<String, Object> parameters,
                                                 ParameterizedTypeReference<T> responseType) {
        HttpEntity<B> request = new HttpEntity<>(body, defaultHeaders());

        if (parameters == null) {
            return restTemplate.exchange(uri, method, request, responseType);
        } else {
            return restTemplate.exchange(uri, method, request, responseType, parameters);
        }
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}