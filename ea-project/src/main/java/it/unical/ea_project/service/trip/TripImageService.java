package it.unical.ea_project.service.trip;

import it.unical.ea_project.domain.trip.TripImage;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface TripImageService {
    List<TripImage> getImagesByTripId(Long tripId);

    TripImage uploadCover(Long tripId, Long organizerId, MultipartFile file);

    List<TripImage> uploadPhotos(Long tripId, Long organizerId, List<MultipartFile> files);

    void deleteImage(Long imageId, Long organizerId);
}
