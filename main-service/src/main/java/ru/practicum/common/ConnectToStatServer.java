package ru.practicum.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import ru.practicum.dto.StatisticResponse;
import ru.practicum.errors.ClientException;
import ru.practicum.statistic.StatisticClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ConnectToStatServer {

    public static List<Long> getViews(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique,
                                      StatisticClient statisticClient) {
        ResponseEntity<List<StatisticResponse>> response = statisticClient.getStats(start, end, uris, unique);

        if (response.getStatusCode().is4xxClientError()) {
            log.warn("Bad request. Status code is {}", response.getStatusCode());
            throw new ClientException("Bad request. Status code is: " + response.getStatusCode());
        }

        if (response.getStatusCode().is5xxServerError()) {
            log.warn("Internal server error statusCode is {}", response.getStatusCode());
            throw new ClientException("Internal server error statusCode is " + response.getStatusCode());
        }

        List<StatisticResponse> body = response.getBody();
        if (body == null) {
            log.warn("Returned empty body");
            return List.of();
        }

        return body.stream()
                .map(StatisticResponse::getHits)
                .collect(Collectors.toList());
    }

    public static List<String> prepareUris(List<Long> ids) {
        return ids.stream()
                .map(id -> "/events/" + id)
                .collect(Collectors.toList());
    }
}