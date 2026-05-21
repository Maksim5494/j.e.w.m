package ru.practicum.events.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.practicum.common.PaginationConstants;
import ru.practicum.dto.StatisticDto;
import ru.practicum.errors.NotFoundException;
import ru.practicum.errors.ValidationException;
import ru.practicum.common.ConnectToStatServer;
import ru.practicum.common.GeneralConstants;
import ru.practicum.events.EventMapper;
import ru.practicum.events.EventRepository;
import ru.practicum.events.EventStates;
import ru.practicum.events.dto.EventRespFull;
import ru.practicum.events.dto.EventRespShort;
import ru.practicum.events.model.Event;
import ru.practicum.requests.RequestRepository;
import ru.practicum.requests.RequestStatus;
import ru.practicum.requests.dto.EventIdByRequestsCount;
import ru.practicum.statistic.StatisticClient;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ru.practicum.common.EventConstants.ZERO_VIEWS;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServicePublicImp implements EventsServicePublic {

    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final StatisticClient statisticClient;

    @Override
    public Collection<EventRespShort> searchEvents(String text, List<Integer> categories, Boolean paid,
                                                   String rangeStart, String rangeEnd, boolean onlyAvailable,
                                                   String sort, int from, int size, String ip, String path) {
        sendStatistic(ip, path);

        LocalDateTime start = convertToLocalDateTime(decode(rangeStart));
        LocalDateTime end = convertToLocalDateTime(decode(rangeEnd));

        validateDates(start, end);

        int startPage = from > 0 ? (from / size) : PaginationConstants.FIRST_PAGE_INDEX;
        Pageable pageable = PageRequest.of(startPage, size);

        if (text == null) {
            text = "";
        }
        if (categories == null) {
            categories = List.of();
        }
        if (start == null) {
            start = LocalDateTime.now();
        }
        if (end == null) {
            end = GeneralConstants.defaultEndTime;
        }

        List<EventRespShort> events = eventRepository
                .searchEvents(text, categories, paid, start, end, onlyAvailable, pageable)
                .stream()
                .map(EventMapper::mapToEventRespShort)
                .toList();

        List<Long> eventsIds = events.stream()
                .map(EventRespShort::getId)
                .toList();

        Map<Long, Long> confirmedRequestsByEvents = requestRepository
                .countByEventIdInAndStatusGroupByEvent(eventsIds, String.valueOf(RequestStatus.CONFIRMED))
                .stream()
                .collect(Collectors.toMap(EventIdByRequestsCount::getEvent, EventIdByRequestsCount::getCount));

        List<Long> views = ConnectToStatServer.getViews(
                GeneralConstants.defaultStartTime,
                GeneralConstants.defaultEndTime,
                ConnectToStatServer.prepareUris(eventsIds),
                true,
                statisticClient
        );

        for (int i = 0; i < events.size(); i++) {
            events.get(i).setViews((!views.isEmpty() && views.get(i) != 0) ? views.get(i) : ZERO_VIEWS);
            events.get(i).setConfirmedRequests(
                    confirmedRequestsByEvents.getOrDefault(events.get(i).getId(), ZERO_VIEWS)
            );
        }

        return events;
    }

    @Override
    public EventRespFull getEvent(long eventId, String ip, String path) {
        sendStatistic(ip, path);

        Event event = eventRepository.findByIdAndState(eventId, String.valueOf(EventStates.PUBLISHED))
                .orElseThrow(() -> {
                    log.warn("Attempt to get unknown event");
                    return new NotFoundException("Event with id = " + eventId + " was not found");
                });

        long confirmedRequests = requestRepository.countByEventIdAndStatus(
                eventId, String.valueOf(RequestStatus.CONFIRMED));

        EventRespFull eventFull = EventMapper.mapToEventRespFull(event);
        eventFull.setConfirmedRequests(confirmedRequests);

        List<Long> views = ConnectToStatServer.getViews(
                GeneralConstants.defaultStartTime,
                GeneralConstants.defaultEndTime,
                path,
                true,
                statisticClient
        );

        eventFull.setViews(views.isEmpty() ? ZERO_VIEWS : views.get(0));
        return eventFull;
    }

    private void sendStatistic(String ip, String path) {
        StatisticDto statisticDto = StatisticDto.builder()
                .app("ewm-main-service")
                .uri(path)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();

        ResponseEntity<Object> response = statisticClient.addStat(statisticDto);

        if (response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
            log.error("Status code: {}, responseBody: {}", response.getStatusCode(), response.getBody());
        }

        log.info("Statistic was sent to stats-server, statisticDto: {}", statisticDto);
    }

    private String decode(String parameter) {
        return parameter == null ? null : URLDecoder.decode(parameter, StandardCharsets.UTF_8);
    }

    private LocalDateTime convertToLocalDateTime(String date) {
        return date == null ? null : LocalDateTime.parse(date, GeneralConstants.DATE_FORMATTER);
    }

    private void validateDates(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return;
        }
        if (start.isAfter(end)) {
            log.warn("Prohibited. Start is after end. Start: {}, end: {}", start, end);
            throw new ValidationException("Event must be published");
        }
    }
}