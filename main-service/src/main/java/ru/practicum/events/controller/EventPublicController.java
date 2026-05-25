package ru.practicum.events.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import ru.practicum.events.dto.EventRespFull;
import ru.practicum.events.dto.EventRespShort;
import ru.practicum.events.dto.EventSearchParams;
import ru.practicum.events.services.EventsServicePublic;

import java.util.Collection;

@RestController
@RequestMapping("/events")
@Validated
@RequiredArgsConstructor
@Slf4j
public class EventPublicController {

    private final EventsServicePublic eventService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Collection<EventRespShort> searchEvents(@ModelAttribute @Validated EventSearchParams params,
                                                   HttpServletRequest request) {
        log.info("EventPublicController, searchEvents, params: {}", params);

        return eventService.searchEvents(
                params.getText(),
                params.getCategories(),
                params.getPaid(),
                params.getRangeStart(),
                params.getRangeEnd(),
                params.getOnlyAvailable(),
                params.getSort(),
                params.getFrom(),
                params.getSize(),
                request.getRemoteAddr(),
                request.getRequestURI()
        );
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public EventRespFull getEvent(@PathVariable("id") long eventId,
                                  HttpServletRequest request) {
        log.info("EventPublicController, getEvent, eventId: {}", eventId);
        return eventService.getEvent(eventId, request.getRemoteAddr(), request.getRequestURI());
    }
}