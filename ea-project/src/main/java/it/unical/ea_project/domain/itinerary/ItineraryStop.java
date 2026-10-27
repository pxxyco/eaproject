package it.unical.ea_project.domain.itinerary;

import it.unical.ea_project.domain.favorites.FavoriteList;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
