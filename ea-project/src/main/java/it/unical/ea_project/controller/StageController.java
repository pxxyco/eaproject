package it.unical.ea_project.controller;

import it.unical.ea_project.security.AuthUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import it.unical.ea_project.domain.Stage;
import it.unical.ea_project.domain.Trip;
import it.unical.ea_project.dto.StageDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import it.unical.ea_project.service.StageService;
import it.unical.ea_project.service.TripService;

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
    public ResponseEntity<StageDTO> addStage(@PathVariable Long tripId, @RequestBody StageDTO stageDTO, @AuthenticationPrincipal AuthUser user) {
        return tripService.getTripById(tripId)
                .map(trip -> {
                    if (trip.getCreatedBy() == null || !trip.getCreatedBy().getId().equals(user.id())) {
                        throw new AccessDeniedException("Puoi modificare solo le tappe dei tuoi viaggi.");
                    }
                    if (stageDTO.getCategory() == null || stageDTO.getTitle() == null || stageDTO.getTitle().isBlank()
                            || stageDTO.getDay() == null || stageDTO.getDay() < 1
                            || stageDTO.getOrderInDay() == null || stageDTO.getOrderInDay() < 1) {
                        throw new IllegalArgumentException("Titolo, giorno e ordine della tappa sono obbligatori e validi.");
                    }
                    Stage stage = toEntity(stageDTO);
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
