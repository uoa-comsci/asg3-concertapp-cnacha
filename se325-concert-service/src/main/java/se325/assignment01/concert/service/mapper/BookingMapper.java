package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.BookingDTO;
import se325.assignment01.concert.service.domain.Booking;

import java.util.stream.Collectors;

public class BookingMapper {

    public static BookingDTO toDTO(Booking domainBooking) {
        return new BookingDTO(
                domainBooking.getConcert().getId(),
                domainBooking.getDate(),
                domainBooking.getSeats().stream().map(SeatMapper::toDTO).collect(Collectors.toList())
        );
    }

}
