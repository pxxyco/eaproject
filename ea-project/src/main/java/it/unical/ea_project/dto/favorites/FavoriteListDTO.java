package it.unical.ea_project.dto.favorites;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteListDTO {

    private Long id;

    private Long userId;

    private String name;

    private String visibility; // FavoriteList.Visibility: PRIVATE, SHARED, PUBLIC

    private boolean deleted;

}
