package it.unical.ea_project_javafx.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListSharingDTO {

    private Long listId;

    private Long userId;

    private String permission;  // ListSharing.SharingPermission: VIEW, EDIT

    private LocalDateTime sharedAt;

}
