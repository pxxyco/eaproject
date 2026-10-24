package it.unical.ea_project.itinerary;

import it.unical.ea_project.domain.FavoriteList;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "itinerary_stop", indexes = @Index(name = "itinerary_stop_list", columnList = "list_id,day_number,position"))
@Getter
@Setter
@NoArgsConstructor
public class ItineraryStop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "list_id")
    private FavoriteList list;
    @Version
    private long version;
    @Column(nullable = false, length = 180)
    private String title;
    @Column(length = 150)
    private String city;
    @Column(length = 2000)
    private String notes;
    @Column(name = "day_number", nullable = false)
    private int day;
    @Column(nullable = false)
    private int position;
    private LocalTime startTime;
    private LocalTime endTime;
}
