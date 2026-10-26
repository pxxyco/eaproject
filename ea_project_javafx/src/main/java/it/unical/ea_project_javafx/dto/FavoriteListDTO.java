package it.unical.ea_project_javafx.dto;

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

    private String visibility; // FavoriteList.Visibility: PRIVATE, SHARED, PUBLIC

    private boolean deleted;

    private String name;

}
