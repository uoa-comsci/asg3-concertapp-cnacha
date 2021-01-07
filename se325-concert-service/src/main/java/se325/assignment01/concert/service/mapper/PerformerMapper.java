package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.PerformerDTO;
import se325.assignment01.concert.service.domain.Performer;

public class PerformerMapper {

    public static PerformerDTO toDTO(Performer domainPerformer) {
        return new PerformerDTO(
                domainPerformer.getId(),
                domainPerformer.getName(),
                domainPerformer.getImageName(),
                domainPerformer.getGenre(),
                domainPerformer.getBlurb()
        );
    }

}
