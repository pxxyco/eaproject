package it.unical.ea_project.service.trip.impl;

import it.unical.ea_project.domain.trip.Stage;
import it.unical.ea_project.domain.trip.Trip.TripStatus;
import it.unical.ea_project.domain.trip.Trip;
import it.unical.ea_project.domain.trip.TripImage;
import it.unical.ea_project.domain.user.User;
import it.unical.ea_project.dto.home.TripHomeDTO;
import it.unical.ea_project.dto.home.TripSuggestionDTO;
import it.unical.ea_project.dto.trip.StageDTO;
import it.unical.ea_project.dto.trip.TripDTO;
import it.unical.ea_project.repository.trip.TripImageRepository;
import it.unical.ea_project.repository.trip.TripRepository;
import it.unical.ea_project.repository.user.UserRepository;
import it.unical.ea_project.service.trip.TripService;
import it.unical.ea_project.util.PageUtils;
import it.unical.ea_project.validation.ExperienceValidation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TripServiceImpl implements TripService {

    private static final String DEFAULT_IMAGE_URL = "/photo/default/experience.jpg";

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final TripImageRepository tripImageRepository;

    @Override
    public List<Trip> getTripsByUser(User user) {
        return tripRepository.findByCreatedByAndDeletedAtIsNull(user);
    }

    @Override
    public List<Trip> getPublishedTrips() {
        return tripRepository.findByStatusAndDeletedAtIsNull(TripStatus.PUBLISHED);
    }

    @Override
    public List<Trip> searchByCountry(String country) {
        return tripRepository.findByDestinationCountryContainingIgnoreCaseAndDeletedAtIsNull(country);
    }

    @Override
    public List<Trip> searchByCity(String city) {
        return tripRepository.findByDestinationCityContainingIgnoreCaseAndDeletedAtIsNull(city);
    }

    @Override
    public List<Trip> getTripsInDateRange(LocalDate start, LocalDate end) {
        return tripRepository.findByStartDateGreaterThanEqualAndEndDateLessThanEqualAndDeletedAtIsNull(start, end);
    }

    @Override
    public List<Trip> getAvailableTrips() {
        return tripRepository.findByStatusAndAvailableSeatsGreaterThanAndDeletedAtIsNull(TripStatus.PUBLISHED, 0);
    }

    @Override
    public Optional<Trip> getTripById(Long id) {
        return tripRepository.findByTripIdAndDeletedAtIsNull(id);
    }

    @Override
    public List<Trip> getTopTrips(int page, int size) {
        return tripRepository.findTopTrips(TripStatus.PUBLISHED, LocalDate.now(), PageUtils.of(page, size));
    }

    @Override
    @Transactional
    public Trip createTrip(Trip trip, Long creatorId) {
        ExperienceValidation.trip(trip);
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Utente non trovato con ID: " + creatorId));
        if (creator.getRole() != User.Role.ORGANIZER || creator.getDeletedAt() != null)
            throw new AccessDeniedException("Account organizzatore richiesto.");
        trip.setCreatedBy(creator);
        trip.setAvailableSeats(trip.getMaxSeats());
        if (trip.getStatus() == null) {
            trip.setStatus(TripStatus.PUBLISHED);
        }
        if (trip.getAvailableSeats() == null) {
            trip.setAvailableSeats(trip.getMaxSeats());
        }
        if (trip.getStages() == null)
            trip.setStages(new ArrayList<>());
        trip.getStages().forEach(stage -> stage.setTrip(trip));
        return tripRepository.save(trip);
    }

    @Override
    @Transactional
    public Trip updateTrip(Long id, Trip details) {
        ExperienceValidation.trip(details);
        Trip existing = tripRepository.findActiveForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Viaggio non trovato con ID: " + id));

        if (details.isStagesProvided())
            replaceStages(existing, details);
        else
            for (var stage : existing.getStages())
                ExperienceValidation.stage(details, stage);
        existing.setTitle(details.getTitle());
        existing.setDescription(details.getDescription());
        if (details.getDestinationCountry() != null)
            existing.setDestinationCountry(details.getDestinationCountry());
        existing.setDestinationCity(details.getDestinationCity());
        existing.setStartDate(details.getStartDate());
        existing.setEndDate(details.getEndDate());
        existing.setTotalPrice(details.getTotalPrice());
        int available = ExperienceValidation.available(
                existing.getMaxSeats(), existing.getAvailableSeats(), details.getMaxSeats());
        existing.setMaxSeats(details.getMaxSeats());
        existing.setAvailableSeats(available);
        if (details.getStatus() != null)
            existing.setStatus(details.getStatus());
        return tripRepository.save(existing);
    }

    private void replaceStages(Trip trip, Trip details) {
        List<Stage> requested = details.getStages();
        if (requested == null)
            return;
        Map<Long, Stage> current = trip.getStages().stream()
                .collect(Collectors.toMap(Stage::getStageId, stage -> stage));
        Set<Long> seen = new HashSet<>();
        List<Stage> replacement = new ArrayList<>();
        for (var source : requested) {
            ExperienceValidation.stage(details, source);
            var target = source.getStageId() == null ? new Stage()
                    : current.get(source.getStageId());
            if (target == null || (source.getStageId() != null && !seen.add(source.getStageId()))) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La tappa non appartiene al viaggio o è duplicata.");
            }
            target.setTitle(source.getTitle());
            target.setDescription(source.getDescription());
            target.setCategory(source.getCategory());
            target.setLocationName(source.getLocationName());
            target.setCity(source.getCity());
            target.setCountry(source.getCountry());
            target.setLatitude(source.getLatitude());
            target.setLongitude(source.getLongitude());
            target.setDay(source.getDay());
            target.setOrderInDay(source.getOrderInDay());
            target.setStartTime(source.getStartTime());
            target.setEndTime(source.getEndTime());
            target.setPhotoUrl(source.getPhotoUrl());
            target.setNotes(source.getNotes());
            target.setTrip(trip);
            replacement.add(target);
        }
        trip.getStages().clear();
        trip.getStages().addAll(replacement);
    }

    @Override
    @Transactional
    public Trip saveTrip(Trip trip) {
        return tripRepository.save(trip);
    }

    @Override
    @Transactional
    public void deleteTripLogically(Trip trip) {
        trip.setStatus(TripStatus.CANCELLED);
        trip.setDeletedAt(LocalDateTime.now());
        tripRepository.save(trip);
    }

    @Override
    @Transactional
    public void deleteTrip(Long id) {
        tripRepository.findActiveForUpdate(id).ifPresent(this::deleteTripLogically);
    }

    @Override
    public List<TripDTO> getTripDtosByUser(User user) {
        return toDtoList(getTripsByUser(user));
    }

    @Override
    public Optional<TripDTO> getTripDtoById(Long id) {
        return getTripById(id).map(this::toDto);
    }

    @Override
    public List<TripDTO> getPublishedTripDtos() {
        return toDtoList(getPublishedTrips());
    }

    @Override
    public List<TripDTO> getAvailableTripDtos() {
        return toDtoList(getAvailableTrips());
    }

    @Override
    public List<TripDTO> searchByCountryDtos(String country) {
        return toDtoList(searchByCountry(country));
    }

    @Override
    public List<TripDTO> searchByCityDtos(String city) {
        return toDtoList(searchByCity(city));
    }

    @Override
    public List<TripDTO> getTripsInDateRangeDtos(LocalDate start, LocalDate end) {
        return toDtoList(getTripsInDateRange(start, end));
    }

    @Override
    public List<TripDTO> getTopTripDtos(int page, int size) {
        return toDtoList(getTopTrips(page, size));
    }

    @Override
    @Transactional
    public TripDTO createTripDto(Trip trip, Long creatorId) {
        return toDto(createTrip(trip, creatorId));
    }

    @Override
    @Transactional
    public TripDTO updateTripDto(Long id, Trip details) {
        return toDto(updateTrip(id, details));
    }

    @Override
    public List<TripHomeDTO> searchTripHomeDtos(String query, LocalDate date, int page, int size) {
        String normalizedQuery = (query == null) ? "" : query.trim();
        Pageable pageable = PageUtils.of(page, size);

        List<Trip> trips = (date == null)
                ? tripRepository.searchPublished(normalizedQuery, TripStatus.PUBLISHED, LocalDate.now(), pageable)
                : tripRepository.searchPublishedFromDate(normalizedQuery, TripStatus.PUBLISHED, date, LocalDate.now(),
                        pageable);

        return toHomeDtos(trips);
    }

    @Override
    public List<TripSuggestionDTO> getSuggestions(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return tripRepository.findSuggestions(query.trim(), TripStatus.PUBLISHED, LocalDate.now(),
                PageUtils.suggestions(limit));
    }

    @Override
    public String exportTripToIcs(Long tripId) {
        // TODO: Implementare esportazione file ICS
        throw new UnsupportedOperationException("Unimplemented method 'exportTripToIcs'");
    }

    @Override
    @Transactional(readOnly = true)
    public long countHomeTrips(String query, LocalDate date) {
        String normalized = query == null ? "" : query.trim();
        return date == null ? tripRepository.countSearchPublished(normalized, TripStatus.PUBLISHED, LocalDate.now())
                : tripRepository.countSearchPublishedFromDate(normalized, TripStatus.PUBLISHED, date, LocalDate.now());
    }

    private List<TripHomeDTO> toHomeDtos(List<Trip> trips) {
        if (trips == null || trips.isEmpty()) {
            return List.of();
        }

        List<Long> tripIds = trips.stream()
                .map(Trip::getTripId)
                .toList();

        List<TripImage> images = tripImageRepository.findByTripIds(tripIds);

        Map<Long, List<TripImage>> imagesByTrip = images.stream()
                .collect(Collectors.groupingBy(image -> image.getTrip().getTripId()));

        return trips.stream()
                .map(trip -> toHomeDto(
                        trip,
                        imagesByTrip.getOrDefault(trip.getTripId(), List.of())))
                .toList();
    }

    private List<TripDTO> toDtoList(List<Trip> trips) {
        if (trips == null || trips.isEmpty()) {
            return List.of();
        }
        return trips.stream().map(this::toDto).toList();
    }

    private TripDTO toDto(Trip trip) {
        Long creatorId = (trip.getCreatedBy() != null) ? trip.getCreatedBy().getId() : null;

        return TripDTO.builder()
                .tripId(trip.getTripId())
                .createdByUserId(creatorId)
                .title(trip.getTitle())
                .description(trip.getDescription())
                .destinationCountry(trip.getDestinationCountry())
                .destinationCity(trip.getDestinationCity())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .totalPrice(trip.getTotalPrice())
                .maxSeats(trip.getMaxSeats())
                .availableSeats(trip.getAvailableSeats())
                .status(trip.getStatus() != null ? trip.getStatus().name() : null)
                .icsUid(trip.getIcsUid())
                .averageRating(trip.getAverageRating())
                .coverPhotoUrl(trip.getCoverPhotoUrl())
                .createdAt(trip.getCreatedAt())
                .updatedAt(trip.getUpdatedAt())
                .deletedAt(trip.getDeletedAt())
                .stages(trip.getStages() == null ? List.of()
                        : trip.getStages().stream()
                                .sorted(Comparator
                                        .comparing(Stage::getDay,
                                                Comparator.nullsLast(Comparator.naturalOrder()))
                                        .thenComparing(Stage::getOrderInDay,
                                                Comparator.nullsLast(Comparator.naturalOrder())))
                                .map(StageDTO::toDto).toList())
                .build();
    }

    private TripHomeDTO toHomeDto(Trip trip, List<TripImage> images) {
        String imageUrl = images.stream()
                .sorted(Comparator.comparing(TripImage::getOrderIndex, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(TripImage::getImageUrl)
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(DEFAULT_IMAGE_URL);

        return new TripHomeDTO(
                trip.getTripId(),
                trip.getTitle(),
                buildLocation(trip.getDestinationCity(), trip.getDestinationCountry()),
                trip.getTotalPrice(),
                trip.getAverageRating(),
                imageUrl,
                trip.getStartDate(),
                trip.getEndDate());
    }

    private String buildLocation(String city, String country) {
        boolean hasCity = city != null && !city.isBlank();
        boolean hasCountry = country != null && !country.isBlank();

        if (hasCity && hasCountry)
            return city + ", " + country;
        if (hasCity)
            return city;
        if (hasCountry)
            return country;
        return "";
    }
}
