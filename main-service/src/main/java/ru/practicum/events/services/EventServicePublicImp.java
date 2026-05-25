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

        LocalDateTime[] range = parseAndValidateRange(rangeStart, rangeEnd);
        LocalDateTime start = range[0];
        LocalDateTime end = range[1];

        Pageable pageable = createPageable(from, size);
        String searchText = (text == null) ? "" : text;
        List<Integer> categoryIds = (categories == null) ? List.of() : categories;

        LocalDateTime effectiveStart = (start == null) ? LocalDateTime.now() : start;
        LocalDateTime effectiveEnd = (end == null) ? GeneralConstants.defaultEndTime : end;

        List<EventRespShort> events = fetchEventsFromRepository(
                searchText, categoryIds, paid, effectiveStart, effectiveEnd, onlyAvailable, pageable);

        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        enrichEventsWithData(events);

        return events;
    }

    private LocalDateTime[] parseAndValidateRange(String rangeStart, String rangeEnd) {
        LocalDateTime start = convertToLocalDateTime(decode(rangeStart));
        LocalDateTime end = convertToLocalDateTime(decode(rangeEnd));
        validateDates(start, end);
        return new LocalDateTime[]{start, end};
    }

    private List<EventRespShort> fetchEventsFromRepository(String text, List<Integer> categories, Boolean paid,
                                                           LocalDateTime start, LocalDateTime end,
                                                           boolean onlyAvailable, Pageable pageable) {
        return eventRepository
                .searchEvents(text, categories, paid, start, end, onlyAvailable, pageable)
                .stream()
                .map(EventMapper::mapToEventRespShort)
                .collect(Collectors.toList());
    }

    @Override
    public EventRespFull getEvent(long eventId, String ip, String path) {
        sendStatistic(ip, path);

        Event event = eventRepository.findByIdAndState(eventId, String.valueOf(EventStates.PUBLISHED))
                .orElseThrow(() -> {
                    log.warn("Attempt to get unknown event id={}", eventId);
                    return new NotFoundException("Event with id = " + eventId + " was not found");
                });

        EventRespFull eventFull = EventMapper.mapToEventRespFull(event);
        eventFull.setConfirmedRequests(fetchConfirmedRequestsCount(eventId));
        eventFull.setViews(fetchViews(path));

        return eventFull;
    }

    private void enrichEventsWithData(List<EventRespShort> events) {
        List<Long> eventIds = events.stream()
                .map(EventRespShort::getId)
                .collect(Collectors.toList());

        Map<Long, Long> confirmedRequests = getConfirmedRequestsMap(eventIds);
        Map<Long, Long> viewsMap = getViewsMap(eventIds);

        for (EventRespShort event : events) {
            event.setConfirmedRequests(confirmedRequests.getOrDefault(event.getId(), ZERO_VIEWS));
            event.setViews(viewsMap.getOrDefault(event.getId(), ZERO_VIEWS));
        }
    }

    private long fetchConfirmedRequestsCount(long eventId) {
        return requestRepository.countByEventIdAndStatus(
                eventId, String.valueOf(RequestStatus.CONFIRMED));
    }

    private long fetchViews(String path) {
        List<Long> views = ConnectToStatServer.getViews(
                GeneralConstants.defaultStartTime,
                GeneralConstants.defaultEndTime,
                path,
                true,
                statisticClient
        );
        return views.isEmpty() ? ZERO_VIEWS : views.get(0);
    }

    private Map<Long, Long> getConfirmedRequestsMap(List<Long> eventIds) {
        return requestRepository
                .countByEventIdInAndStatusGroupByEvent(eventIds, String.valueOf(RequestStatus.CONFIRMED))
                .stream()
                .collect(Collectors.toMap(EventIdByRequestsCount::getEvent, EventIdByRequestsCount::getCount));
    }

    private Map<Long, Long> getViewsMap(List<Long> eventIds) {
        List<Long> views = ConnectToStatServer.getViews(
                GeneralConstants.defaultStartTime,
                GeneralConstants.defaultEndTime,
                ConnectToStatServer.prepareUris(eventIds),
                true,
                statisticClient
        );

        Map<Long, Long> viewsMap = new HashMap<>();
        for (int i = 0; i < eventIds.size(); i++) {
            Long viewCount = (views != null && views.size() > i) ? views.get(i) : ZERO_VIEWS;
            viewsMap.put(eventIds.get(i), viewCount);
        }
        return viewsMap;
    }

    private Pageable createPageable(int from, int size) {
        int startPage = from > 0 ? (from / size) : PaginationConstants.FIRST_PAGE_INDEX;
        return PageRequest.of(startPage, size);
    }

    private void sendStatistic(String ip, String path) {
        StatisticDto statisticDto = StatisticDto.builder()
                .app("ewm-main-service")
                .uri(path)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();

        try {
            ResponseEntity<Object> response = statisticClient.addStat(statisticDto);
            if (response.getStatusCode().isError()) {
                log.error("Failed to send stats: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error connecting to stat-server: {}", e.getMessage());
        }
    }

    private String decode(String parameter) {
        return parameter == null ? null : URLDecoder.decode(parameter, StandardCharsets.UTF_8);
    }

    private LocalDateTime convertToLocalDateTime(String date) {
        return date == null ? null : LocalDateTime.parse(date, GeneralConstants.DATE_FORMATTER);
    }

    private void validateDates(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            log.warn("Validation failed: start {} is after end {}", start, end);
            throw new ValidationException("Range start must be before range end");
        }
    }
}