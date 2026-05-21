package ru.practicum.events.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import ru.practicum.events.dto.EventRespFull;
import ru.practicum.events.dto.EventRespShort;
import ru.practicum.events.services.EventsServicePublic;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/events")
@Validated
@RequiredArgsConstructor
@Slf4j
public class EventPublicController {

    private final EventsServicePublic eventService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Collection<EventRespShort> searchEvents(@RequestParam(value = "text", required = false) String text,
                                                   @RequestParam(value = "categories", required = false)
                                                   List<Integer> categories,
                                                   @RequestParam(value = "paid", required = false) Boolean paid,
                                                   @RequestParam(value = "rangeStart", required = false)
                                                   String rangeStart,
                                                   @RequestParam(value = "rangeEnd", required = false)
                                                   String rangeEnd,
                                                   @RequestParam(value = "onlyAvailable", required = false,
                                                           defaultValue = "false") boolean onlyAvailable,
                                                   @RequestParam(value = "sort", required = false) String sort,
                                                   @Min(0) @RequestParam(value = "from", defaultValue = "0") int from,
                                                   @Min(0) @RequestParam(value = "size", defaultValue = "10") int size,
                                                   HttpServletRequest request) {

        log.info("EventPublicController, searchEvents, text: {}, categories: {}, paid: {}, rangeStart: {}, " +
                        "rangeEnd: {}, onlyAvailable: {}, sort: {}, from: {}, size: {}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        return eventService.searchEvents(
                text,
                categories,
                paid,
                rangeStart,
                rangeEnd,
                onlyAvailable,
                sort,
                from,
                size,
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