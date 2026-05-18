package ru.practicum.events.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import ru.practicum.GeneralConstants;
import ru.practicum.dto.StatisticDto;
import ru.practicum.events.dto.EventRespFull;
import ru.practicum.events.dto.EventRespShort;
import ru.practicum.events.services.EventsServicePublic;
import ru.practicum.statistic.StatisticClient;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/events")
@Validated
@RequiredArgsConstructor
@Slf4j
public class EventPublicController {

    private final EventsServicePublic eventService;
    private final StatisticClient statisticClient;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Collection<EventRespShort> searchEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Integer> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = GeneralConstants.DATA_PATTERN) LocalDateTime rangeStart,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = GeneralConstants.DATA_PATTERN) LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") boolean onlyAvailable,
            @RequestParam(required = false) String sort,
            @Min(0) @RequestParam(defaultValue = "0") int from,
            @Min(0) @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        log.info("Public search events: text={}, categories={}, rangeStart={}, rangeEnd={}",
                text, categories, rangeStart, rangeEnd);

        sendHit(request);

        return eventService.searchEvents(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public EventRespFull getEvent(@PathVariable("id") Long id, HttpServletRequest request) {
        log.info("Public get event details: id={}, uri={}", id, request.getRequestURI());

        sendHit(request);

        return eventService.getEvent(id, request.getRequestURI());
    }

    private void sendHit(HttpServletRequest request) {
        StatisticDto statisticDto = StatisticDto.builder()
                .app("ewm-main-service")
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now())
                .build();

        try {
            statisticClient.addStat(statisticDto);
            log.info("Statistic sent for URI: {}", request.getRequestURI());
        } catch (Exception e) {
            log.error("Failed to send statistics: {}", e.getMessage());
        }
    }
}