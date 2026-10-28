package it.unical.ea_project.dto.favorites;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
