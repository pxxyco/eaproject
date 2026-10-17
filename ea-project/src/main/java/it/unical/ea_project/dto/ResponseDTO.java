package it.unical.ea_project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseDTO {

    private Long id;
    private Long organizerId;
    private Long reviewId;
    private String comment;
    private LocalDateTime createdAt;
    private boolean deleted;
    private LocalDateTime updatedAt;
}
