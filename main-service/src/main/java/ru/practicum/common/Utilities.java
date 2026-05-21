package ru.practicum.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.events.dto.EventRespShort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ru.practicum.common.EventConstants.ZERO_VIEWS;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Utilities {

    public static List<? extends EventRespShort> addViewsAndConfirmedRequests(List<? extends EventRespShort> eventRespShorts,
                                                                              Map<Long, Long> confirmedRequests,
                                                                              List<Long> views) {
        for (int i = 0; i < eventRespShorts.size(); i++) {
            if ((!views.isEmpty()) && (views.get(i) != 0)) {
                eventRespShorts.get(i).setViews(views.get(i));
            } else {
                eventRespShorts.get(i).setViews(ZERO_VIEWS);
            }
            eventRespShorts.get(i)
                    .setConfirmedRequests(confirmedRequests
                            .getOrDefault(eventRespShorts.get(i).getId(), ZERO_VIEWS));
        }
        return eventRespShorts;
    }

    public static <T> List<T> checkTypes(List<?> list, Class<T> clazz) {
        List<T> result = new ArrayList<>();
        for (Object item : list) {
            try {
                result.add(clazz.cast(item));
            } catch (ClassCastException e) {
                System.out.println(" ");
            }
        }
        return result;
    }
}