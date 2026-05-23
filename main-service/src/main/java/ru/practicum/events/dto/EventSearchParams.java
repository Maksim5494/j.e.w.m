package ru.practicum.events.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

@Data
public class EventSearchParams {
    private String text;
    private List<Integer> categories;
    private Boolean paid;
    private String rangeStart;
    private String rangeEnd;
    private Boolean onlyAvailable = false;
    private String sort;

    @Min(0)
    private int from = 0;

    @Min(0)
    private int size = 10;
}
