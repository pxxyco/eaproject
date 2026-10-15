package it.unical.ea_project.controller;

import it.unical.ea_project.domain.ActivityImage;
import it.unical.ea_project.dto.ActivityImageDTO;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.ActivityImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/activity-images")
@RequiredArgsConstructor
public class ActivityImageController {

    private final ActivityImageService activityImageService;

    @GetMapping("/activity/{activityId}")
    public ResponseEntity<List<ActivityImageDTO>> getImagesByActivity(@PathVariable Long activityId) {
        List<ActivityImageDTO> images = activityImageService.getImagesByActivityId(activityId).stream()
                .map(img -> ActivityImageDTO.builder()
                        .imageId(img.getImageId())
                        .imageUrl(img.getImageUrl())
                        .activityId(activityId)
                        .orderIndex(img.getOrderIndex()).build())
                .toList();
        return ResponseEntity.ok(images);
    }

    @PostMapping("/activity/{activityId}/cover")
    public ResponseEntity<ActivityImageDTO> uploadCover(
            @PathVariable Long activityId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthUser user) {
        ActivityImage saved = activityImageService.uploadCover(activityId, user.id(), file);
        ActivityImageDTO dto = ActivityImageDTO.builder()
                .imageId(saved.getImageId())
                .imageUrl(saved.getImageUrl())
                .activityId(activityId)
                .orderIndex(saved.getOrderIndex())
                .build();
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/activity/{activityId}/photos")
    public ResponseEntity<List<ActivityImageDTO>> uploadPhotos(
            @PathVariable Long activityId,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal AuthUser user) {
        List<ActivityImageDTO> photos = activityImageService.uploadPhotos(activityId, user.id(), files).stream()
                .map(image -> ActivityImageDTO.builder()
                        .imageId(image.getImageId())
                        .imageUrl(image.getImageUrl())
                        .activityId(activityId)
                        .orderIndex(image.getOrderIndex())
                        .build())
                .toList();
        return ResponseEntity.ok(photos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) {
        activityImageService.deleteImage(id);
        return ResponseEntity.noContent().build();
    }
}