package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.ConcertDTO;
import se325.assignment01.concert.common.dto.ConcertSummaryDTO;
import se325.assignment01.concert.service.domain.Concert;

import java.util.stream.Collectors;

public class ConcertMapper {

    public static ConcertDTO toDTO(Concert domainConcert) {
        ConcertDTO dtoConcert = new ConcertDTO(
                domainConcert.getId(),
                domainConcert.getTitle(),
                domainConcert.getImageName(),
                domainConcert.getBlurb()
        );

        dtoConcert.getDates().addAll(domainConcert.getDates());
        dtoConcert.getPerformers().addAll(
                domainConcert.getPerformers().stream().map(PerformerMapper::toDTO).collect(Collectors.toList()));

        return dtoConcert;
    }

    public static ConcertSummaryDTO toSummaryDTO(Concert domainConcert) {
        return new ConcertSummaryDTO(
                domainConcert.getId(),
                domainConcert.getTitle(),
                domainConcert.getImageName()
        );
    }
}
