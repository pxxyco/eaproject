package it.unical.ea_project_javafx.dto.home;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripHomeDTO {

    private Long tripId;

    private String title;

    private String location;

    private BigDecimal totalPrice;

    private BigDecimal averageRating;

    private String coverPhotoUrl;

    private String startDate;

    private String endDate;

}
