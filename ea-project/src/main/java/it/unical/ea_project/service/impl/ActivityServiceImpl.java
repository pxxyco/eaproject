package it.unical.ea_project.service.impl;

import it.unical.ea_project.domain.Activity;
import it.unical.ea_project.domain.ActivityImage;
import it.unical.ea_project.domain.User;
import it.unical.ea_project.dto.ActivityDTO;
import it.unical.ea_project.dto.ActivityImageDTO;
import it.unical.ea_project.dto.home.ActivityHomeDTO;
import it.unical.ea_project.dto.home.ActivitySuggestionDTO;
import it.unical.ea_project.repository.ActivityImageRepository;
import it.unical.ea_project.repository.ActivityRepository;
import it.unical.ea_project.repository.UserRepository;
import it.unical.ea_project.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityImageRepository activityImageRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Activity> getAllActivities() {
        return activityRepository.findByDeletedAtIsNull();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Activity> getActivityById(Long id) {
        return activityRepository.findByActivityIdAndDeletedAtIsNull(id);
    }

    @Override
    @Transactional
    public Activity createActivity(Activity activity, Long creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Utente organizzatore non trovato con ID: " + creatorId));
        activity.setCreatedBy(creator);
        return activityRepository.save(activity);
    }

    @Override
    @Transactional
    public Activity updateActivity(Long id, Activity details) {
        Activity existing = getActivityById(id)
                .orElseThrow(() -> new RuntimeException("Attività non trovata con ID: " + id));

        existing.setTitle(details.getTitle());
        existing.setDescription(details.getDescription());
        existing.setCategory(details.getCategory());
        existing.setLatitude(details.getLatitude());
        existing.setLongitude(details.getLongitude());
        existing.setPlaceName(details.getPlaceName());
        existing.setCity(details.getCity());
        existing.setStartDate(details.getStartDate());
        existing.setEndDate(details.getEndDate());
        existing.setDurationMinutes(details.getDurationMinutes());
        existing.setPrice(details.getPrice());
        existing.setMaxSeats(details.getMaxSeats());
        existing.setAvailableSeats(details.getAvailableSeats());
        existing.setStatus(details.getStatus());
        existing.setNotes(details.getNotes());

        return activityRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteActivity(Long id) {
        getActivityById(id).ifPresent(activity -> {
            activity.setDeletedAt(LocalDateTime.now());
            activityRepository.save(activity);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<Activity> getActivitiesByCategory(Activity.Category category) {
        return activityRepository.findByDeletedAtIsNullAndCategory(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Activity> getTopActivities(int page, int size) {
        return activityRepository.findByDeletedAtIsNullOrderByAverageRatingDesc(PageRequest.of(page, size));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ActivityDTO> getActivityDtoById(Long id) {
        return activityRepository.findByActivityIdAndDeletedAtIsNull(id)
                .map(activity -> {
                    List<ActivityImage> images = activityImageRepository.findByActivityOrderByOrderIndexAsc(activity);
                    return toDto(activity, images);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityDTO> getTopActivityDtos(int page, int size) {
        return toActivityDtos(getTopActivities(page, size));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityDTO> getActivityDtosByCategory(Activity.Category category) {
        return toActivityDtos(getActivitiesByCategory(category));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityDTO> getActivityDtosByCreator(Long creatorId) {
        return toActivityDtos(activityRepository.findByCreatedByIdAndDeletedAtIsNullOrderByCreatedAtDesc(creatorId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityHomeDTO> getTopActivityHomeDtos(int page, int size) {
        return toHomeDtos(getTopActivities(page, size));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityHomeDTO> searchActivityHomeDtos(String query, Activity.Category category, LocalDate date, int page, int size) {
        String normalizedQuery = query == null ? "" : query.trim();
        Pageable pageable = PageRequest.of(page, size);

        List<Activity> activities;
        if (date == null) {
            activities = activityRepository.searchWithoutDate(normalizedQuery, category, pageable);
        } else {
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime nextDayStart = dayStart.plusDays(1);
            activities = activityRepository.search(normalizedQuery, category, dayStart, nextDayStart, pageable);
        }
        return toHomeDtos(activities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivitySuggestionDTO> getSuggestions(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return activityRepository.findSuggestions(query.trim(), PageRequest.of(0, limit));
    }

    private List<ActivityDTO> toActivityDtos(List<Activity> activities) {
        if (activities == null || activities.isEmpty()) {
            return List.of();
        }

        Map<Long, List<ActivityImage>> imagesByActivity = loadImagesByActivity(activities);

        return activities.stream()
                .map(activity -> toDto(
                        activity,
                        imagesByActivity.getOrDefault(activity.getActivityId(), List.of())
                ))
                .toList();
    }

    private List<ActivityHomeDTO> toHomeDtos(List<Activity> activities) {
        if (activities == null || activities.isEmpty()) {
            return List.of();
        }

        Map<Long, List<ActivityImage>> imagesByActivity = loadImagesByActivity(activities);

        return activities.stream()
                .map(activity -> toHomeDto(
                        activity,
                        imagesByActivity.getOrDefault(activity.getActivityId(), List.of())
                ))
                .toList();
    }

    private Map<Long, List<ActivityImage>> loadImagesByActivity(List<Activity> activities) {
        List<Long> activityIds = activities.stream()
                .map(Activity::getActivityId)
                .toList();

        if (activityIds.isEmpty()) {
            return Map.of();
        }

        List<ActivityImage> images = activityImageRepository.findByActivityIds(activityIds);

        return images.stream()
                .collect(Collectors.groupingBy(image -> image.getActivity().getActivityId()));
    }

    private ActivityDTO toDto(Activity activity, List<ActivityImage> images) {
        List<ActivityImageDTO> imageDtos = images.stream()
                .map(img -> ActivityImageDTO.builder()
                        .imageId(img.getImageId())
                        .activityId(activity.getActivityId())
                        .imageUrl(img.getImageUrl())
                        .orderIndex(img.getOrderIndex())
                        .build())
                .toList();

        return ActivityDTO.builder()
                .activityId(activity.getActivityId())
                .createdByUserId(activity.getCreatedBy() != null ? activity.getCreatedBy().getId() : null)
                .title(activity.getTitle())
                .description(activity.getDescription())
                .category(activity.getCategory() != null ? activity.getCategory().name() : "OTHER")
                .latitude(activity.getLatitude())
                .longitude(activity.getLongitude())
                .placeName(activity.getPlaceName())
                .city(activity.getCity())
                .startDate(activity.getStartDate())
                .endDate(activity.getEndDate())
                .durationMinutes(activity.getDurationMinutes())
                .price(activity.getPrice())
                .maxSeats(activity.getMaxSeats())
                .availableSeats(activity.getAvailableSeats())
                .status(activity.getStatus() != null ? activity.getStatus().name() : null)
                .averageRating(activity.getAverageRating())
                .notes(activity.getNotes())
                .createdAt(activity.getCreatedAt())
                .updatedAt(activity.getUpdatedAt())
                .deletedAt(activity.getDeletedAt())
                .images(imageDtos)
                .build();
    }

    private ActivityHomeDTO toHomeDto(Activity activity, List<ActivityImage> images) {
        String imageUrl = images.stream()
                .map(ActivityImage::getImageUrl)
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse("/photo/default/experience.jpg");

        return new ActivityHomeDTO(
                activity.getActivityId(),
                activity.getTitle(),
                activity.getCity(),
                activity.getCategory() != null ? activity.getCategory().name() : "OTHER",
                activity.getPrice(),
                activity.getAverageRating(),
                imageUrl,
                activity.getStartDate(),
                activity.getEndDate()
        );
    }
}
