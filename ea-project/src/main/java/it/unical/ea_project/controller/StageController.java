package it.unical.ea_project.controller;

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
    public ResponseEntity<StageDTO> addStage(@PathVariable Long tripId, @RequestBody StageDTO stageDTO,
            @org.springframework.security.core.annotation.AuthenticationPrincipal it.unical.ea_project.security.AuthUser user) {
        return tripService.getTripById(tripId)
                .map(trip -> {
                    if (trip.getCreatedBy() == null || !trip.getCreatedBy().getId().equals(user.id())) {
                        throw new org.springframework.security.access.AccessDeniedException("Il viaggio appartiene a un altro organizzatore.");
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
