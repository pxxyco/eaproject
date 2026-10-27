package it.unical.ea_project.controller.trip;

import it.unical.ea_project.domain.trip.TripImage;
import it.unical.ea_project.dto.trip.TripImageDTO;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.trip.TripImageService;
import it.unical.ea_project.service.trip.TripService;
import java.util.Objects;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/trip-images")
@RequiredArgsConstructor
public class TripImageController {

        private final TripImageService tripImageService;
        private final TripService tripService;

        @GetMapping("/trip/{tripId}")
        public ResponseEntity<List<TripImageDTO>> getImagesByTrip(@PathVariable Long tripId,
                        @AuthenticationPrincipal AuthUser user) {
                var offer = tripService.getTripDtoById(tripId);
                if (offer.isEmpty() || "DRAFT".equals(offer.get().getStatus())
                                && (user == null || !Objects.equals(offer.get().getCreatedByUserId(), user.id())))
                        return ResponseEntity.notFound().build();
                List<TripImageDTO> images = tripImageService.getImagesByTripId(tripId).stream()
                                .map(img -> TripImageDTO.builder()
                                                .imageId(img.getImageId())
                                                .imageUrl(img.getImageUrl())
                                                .tripId(tripId)
                                                .orderIndex(img.getOrderIndex()).build())
                                .toList();
                return ResponseEntity.ok(images);
        }

        @PostMapping("/trip/{tripId}/cover")
        public ResponseEntity<TripImageDTO> uploadCover(
                        @PathVariable Long tripId,
                        @RequestParam("file") MultipartFile file,
                        @AuthenticationPrincipal AuthUser user) {
                TripImage saved = tripImageService.uploadCover(tripId, user.id(), file);
                TripImageDTO dto = TripImageDTO.builder()
                                .imageId(saved.getImageId())
                                .imageUrl(saved.getImageUrl())
                                .tripId(tripId)
                                .orderIndex(saved.getOrderIndex()).build();
                return ResponseEntity.ok(dto);
        }

        @PostMapping("/trip/{tripId}/photos")
        public ResponseEntity<List<TripImageDTO>> uploadPhotos(
                        @PathVariable Long tripId,
                        @RequestParam("files") List<MultipartFile> files,
                        @AuthenticationPrincipal AuthUser user) {
                List<TripImageDTO> photos = tripImageService.uploadPhotos(tripId, user.id(), files).stream()
                                .map(image -> TripImageDTO.builder()
                                                .imageId(image.getImageId())
                                                .imageUrl(image.getImageUrl())
                                                .tripId(tripId)
                                                .orderIndex(image.getOrderIndex())
                                                .build())
                                .toList();
                return ResponseEntity.ok(photos);
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteImage(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
                tripImageService.deleteImage(id, user.id());
                return ResponseEntity.noContent().build();
        }
}
