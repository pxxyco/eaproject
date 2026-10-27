package it.unical.ea_project.dto.review;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
