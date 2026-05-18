package ru.practicum.statistic;

import org.springframework.core.ParameterizedTypeReference;
import ru.practicum.GeneralConstants;
import ru.practicum.dto.StatisticDto;
import jakarta.annotation.Nullable;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.BaseClient;
import ru.practicum.dto.StatisticResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticClient extends BaseClient {

    public StatisticClient(RestTemplateBuilder builder, String serverUrl) {
        super(builder
                .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                .build());
    }

    public ResponseEntity<List<StatisticResponse>> getStats(LocalDateTime start, LocalDateTime end,
                                                            @Nullable List<String> uris, boolean unique) {

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("start", start.format(GeneralConstants.DATE_FORMATTER));
        parameters.put("end", end.format(GeneralConstants.DATE_FORMATTER));
        parameters.put("unique", unique);

        String path = "/stats?start={start}&end={end}&unique={unique}";

        if (uris != null && !uris.isEmpty()) {
            parameters.put("uris", uris);
            path += "&uris={uris}";
        }

        return get(path, parameters, new ParameterizedTypeReference<List<StatisticResponse>>() {});
    }

    public ResponseEntity<Object> addStat(StatisticDto statisticDto) {
        return post("/hit", statisticDto, null, Object.class);
    }
}