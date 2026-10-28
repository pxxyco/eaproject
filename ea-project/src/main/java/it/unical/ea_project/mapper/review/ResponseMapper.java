package it.unical.ea_project.mapper.review;

import it.unical.ea_project.domain.review.Response;
import it.unical.ea_project.dto.review.ResponseDTO;

public class ResponseMapper {

    public static ResponseDTO toResponseDTO(Response response) {
        return ResponseDTO.builder()
                .id(response.getId())
                .organizerId(response.getOrganizer().getId())
                .reviewId(response.getReview().getReviewId())
                .comment(response.getComment())
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .deleted(response.isDeleted())
                .build();
    }

}
