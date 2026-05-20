package ru.practicum.events.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.practicum.common.EventConstants;
import ru.practicum.common.PaginationConstants;
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
import ru.practicum.statistic.StatisticClient;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServicePublicImp implements EventsServicePublic {

    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final StatisticClient statisticClient;

    @Override
    public Collection<EventRespShort> searchEvents(String text, List<Integer> categories, Boolean paid,
                                                   String rangeStartStr, String rangeEndStr,
                                                   boolean onlyAvailable, String sort, int from, int size,
                                                   String ip, String path) {
        sendHit(path, ip);

        LocalDateTime rangeStart = convertToLocalDataTime(decode(rangeStartStr));
        LocalDateTime rangeEnd = convertToLocalDataTime(decode(rangeEndStr));

        validateDates(rangeStart, rangeEnd);

        int startPage = from > 0 ? (from / size) : PaginationConstants.FIRST_PAGE_INDEX;
        Pageable pageable = PageRequest.of(startPage, size);

        if (text == null) text = "";
        if (categories == null) categories = List.of();
        if (rangeStart == null) rangeStart = LocalDateTime.now();
        if (rangeEnd == null) rangeEnd = GeneralConstants.defaultEndTime;

        List<EventRespShort> events = eventRepository
                .searchEvents(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, pageable)
                .stream()
                .map(EventMapper::mapToEventRespShort)
                .toList();

        return events;
    }

    @Override
    public EventRespFull getEvent(long eventId, String ip, String path) {
        sendHit(path, ip);

        Event event = eventRepository.findByIdAndState(eventId, String.valueOf(EventStates.PUBLISHED))
                .orElseThrow(() -> new NotFoundException("Event with id = " + eventId + " was not found"));

        EventRespFull eventFull = EventMapper.mapToEventRespFull(event);

        long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, String.valueOf(RequestStatus.CONFIRMED));
        eventFull.setConfirmedRequests(confirmedRequests);

        List<Long> views = ConnectToStatServer.getViews(GeneralConstants.defaultStartTime,
                GeneralConstants.defaultEndTime, path, true, statisticClient);

        eventFull.setViews(views.isEmpty() ? EventConstants.ZERO_VIEWS : views.get(0));
        return eventFull;
    }

    private void sendHit(String path, String ip) {
        ru.practicum.dto.StatisticDto statisticDto = ru.practicum.dto.StatisticDto.builder()
                .app("ewm-main-service")
                .uri(path)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();

        try {
            ResponseEntity<Object> response = statisticClient.addStat(statisticDto);
            if (response.getStatusCode().isError()) {
                log.error("Error sending statistics. Status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send statistics to server", e);
        }
    }

    private String decode(String parameter) {
        if (parameter == null) return null;
        return URLDecoder.decode(parameter, StandardCharsets.UTF_8);
    }

    private LocalDateTime convertToLocalDataTime(String date) {
        if (date == null) return null;
        return LocalDateTime.parse(date, GeneralConstants.DATE_FORMATTER);
    }

    private void validateDates(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new ValidationException("Start time must be before end time");
        }
    }
}