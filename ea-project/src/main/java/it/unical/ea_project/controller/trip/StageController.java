package it.unical.ea_project.controller.trip;

import it.unical.ea_project.domain.trip.Stage;
import it.unical.ea_project.dto.trip.StageDTO;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.trip.StageService;
import it.unical.ea_project.service.trip.TripService;
import it.unical.ea_project.validation.ExperienceValidation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trips/{tripId}/stages")
public class StageController {

    private final StageService stageService;
    private final TripService tripService;

    public StageController(StageService stageService, TripService tripService) {
        this.stageService = stageService;
        this.tripService = tripService;
    }

    @PostMapping
    public ResponseEntity<StageDTO> addStage(@PathVariable Long tripId, @RequestBody StageDTO stageDTO,
            @AuthenticationPrincipal AuthUser user) {
        return tripService.getTripById(tripId)
                .map(trip -> {
                    if (trip.getCreatedBy() == null || !trip.getCreatedBy().getId().equals(user.id())) {
                        throw new AccessDeniedException("Puoi modificare solo le tappe dei tuoi viaggi.");
                    }
                    Stage stage = toEntity(stageDTO);
                    ExperienceValidation.stage(trip, stage);
                    Stage saved = stageService.addStageToTrip(trip, stage);
                    return ResponseEntity.ok(StageDTO.toDto(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private Stage toEntity(StageDTO dto) {
        Stage stage = new Stage();
        stage.setTitle(dto.getTitle());
        stage.setDescription(dto.getDescription());
        stage.setCategory(dto.getCategory() != null ? Stage.StageCategory.valueOf(dto.getCategory()) : null);
        stage.setLocationName(dto.getLocationName());
        stage.setCity(dto.getCity());
        stage.setCountry(dto.getCountry());
        stage.setLatitude(dto.getLatitude());
        stage.setLongitude(dto.getLongitude());
        stage.setDay(dto.getDay());
        stage.setOrderInDay(dto.getOrderInDay());
        stage.setStartTime(dto.getStartTime());
        stage.setEndTime(dto.getEndTime());
        stage.setPhotoUrl(dto.getPhotoUrl());
        stage.setNotes(dto.getNotes());
        return stage;
    }

}
