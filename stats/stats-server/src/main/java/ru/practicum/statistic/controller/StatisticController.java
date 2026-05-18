package ru.practicum.statistic.controller;

import ru.practicum.GeneralConstants;
import ru.practicum.dto.StatisticDto;
import ru.practicum.dto.StatisticResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import ru.practicum.statistic.service.StatisticService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class StatisticController {
    private final StatisticService statisticService;


    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public StatisticDto addInStats(@Valid @RequestBody StatisticDto statisticDto) {
        log.info("StatisticController, addInStats, Request body app: {}, uri: {}, ip: {}, timestamp: {}",
                statisticDto.getApp(), statisticDto.getUri(), statisticDto.getIp(), statisticDto.getTimestamp());
        return statisticService.addToStats(statisticDto);
    }

    @GetMapping("/stats")
    @ResponseStatus(HttpStatus.OK)
    public List<StatisticResponse> getStats(
            @RequestParam("start")
            @DateTimeFormat(pattern = GeneralConstants.DATA_PATTERN) LocalDateTime start,
            @RequestParam("end")
            @DateTimeFormat(pattern = GeneralConstants.DATA_PATTERN) LocalDateTime end,
            @RequestParam(value = "uris", required = false) List<String> uris,
            @RequestParam(value = "unique", required = false, defaultValue = "false") boolean unique) {

        log.info("Statistic Controller, getStats, parameters: start {}, end {}, uris {}, unique {}",
                start, end, uris, unique);

        return statisticService.getStats(start, end, uris, unique);
    }

    private LocalDateTime convertToLocalDataTime(String dataTime) {
        return LocalDateTime.parse(dataTime, GeneralConstants.DATE_FORMATTER);
    }


}