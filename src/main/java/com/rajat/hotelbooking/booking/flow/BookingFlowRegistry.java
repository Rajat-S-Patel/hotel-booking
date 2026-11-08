package com.rajat.hotelbooking.booking.flow;

import com.rajat.hotelbooking.common.entity.enums.BookingType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BookingFlowRegistry {

    private final Map<BookingType, BookingFlow> flows;

    public BookingFlowRegistry(List<BookingFlow> flowList) {
        this.flows = flowList.stream().collect(Collectors.toMap(BookingFlow::type, Function.identity()));
    }

    public BookingFlow forType(BookingType type) {
        BookingFlow flow = flows.get(type);
        if (flow == null) {
            throw new IllegalStateException("no booking flow for " + type);
        }
        return flow;
    }
}
