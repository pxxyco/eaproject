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
public class FavoriteDTO {

    private Long id;

    private String type;

    private Long itemId;

    private String title;

    private String city;

    private LocalDateTime addedAt;

}