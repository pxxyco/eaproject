package it.unical.ea_project.service;

import it.unical.ea_project.domain.TripImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TripImageService {
    List<TripImage> getImagesByTripId(Long tripId);
    TripImage uploadCover(Long tripId, Long organizerId, MultipartFile file);
    List<TripImage> uploadPhotos(Long tripId, Long organizerId, List<MultipartFile> files);
    void deleteImage(Long imageId, Long organizerId);
}