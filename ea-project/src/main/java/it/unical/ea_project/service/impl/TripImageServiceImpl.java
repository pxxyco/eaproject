package it.unical.ea_project.service.impl;

import it.unical.ea_project.domain.Trip;
import it.unical.ea_project.domain.TripImage;
import it.unical.ea_project.repository.TripImageRepository;
import it.unical.ea_project.repository.TripRepository;
import it.unical.ea_project.service.TripImageService;
import it.unical.ea_project.service.PhotoStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class TripImageServiceImpl implements TripImageService {

    private final TripImageRepository tripImageRepository;
    private final TripRepository tripRepository;
    private final PhotoStorageService photoStorageService;

    @Override
    @Transactional(readOnly = true)
    public List<TripImage> getImagesByTripId(Long tripId) {
        if (!tripRepository.existsByTripIdAndDeletedAtIsNull(tripId)) {
            throw new RuntimeException("Viaggio non trovato con ID: " + tripId);
        }
        return tripImageRepository.findByTrip_TripIdAndTrip_DeletedAtIsNullOrderByOrderIndexAsc(tripId);
    }

    @Override
    @Transactional
    public TripImage uploadCover(Long tripId, Long organizerId, MultipartFile file) {
        return uploadPhotos(tripId, organizerId, List.of(file), true).getFirst();
    }

    @Override
    @Transactional
    public List<TripImage> uploadPhotos(Long tripId, Long organizerId, List<MultipartFile> files) {
        return uploadPhotos(tripId, organizerId, files, false);
    }

    private List<TripImage> uploadPhotos(Long tripId, Long organizerId, List<MultipartFile> files,
                                         boolean makePrimary) {
        Trip trip = tripRepository.findByTripIdAndDeletedAtIsNull(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaggio non trovato."));
        if (trip.getCreatedBy() == null || !Objects.equals(trip.getCreatedBy().getId(), organizerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non puoi modificare le foto di questo viaggio.");
        }
        validateFiles(files);

        List<TripImage> existingImages = tripImageRepository.findByTripOrderByOrderIndexAsc(trip);
        for (int index = 0; index < existingImages.size(); index++) {
            existingImages.get(index).setOrderIndex(index + (makePrimary ? 2 : 1));
        }
        int nextOrderIndex = makePrimary ? 1 : existingImages.size() + 1;
        tripImageRepository.saveAll(existingImages);

        List<TripImage> uploadedImages = new ArrayList<>();
        for (MultipartFile file : files) {
            TripImage photo = tripImageRepository.saveAndFlush(
                    new TripImage(null, trip, "", nextOrderIndex++)
            );
            String imageUrl = photoStorageService.storeTripPhoto(tripId, photo.getImageId(), file);
            photo.setImageUrl(imageUrl);
            if (uploadedImages.isEmpty() && (makePrimary || existingImages.isEmpty())) {
                trip.setCoverPhotoUrl(imageUrl);
            }
            uploadedImages.add(tripImageRepository.save(photo));
        }
        return uploadedImages;
    }

    @Override
    @Transactional
    public void deleteImage(Long imageId, Long organizerId) {
        TripImage image = tripImageRepository.findById(imageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto non trovata."));
        Trip offer = image.getTrip();
        if (offer.getCreatedBy() == null || !Objects.equals(offer.getCreatedBy().getId(), organizerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non puoi eliminare le foto di un'altra offerta.");
        }
        tripImageRepository.delete(image);
        if (Objects.equals(offer.getCoverPhotoUrl(), image.getImageUrl())) {
            offer.setCoverPhotoUrl(tripImageRepository.findByTripOrderByOrderIndexAsc(offer).stream()
                    .map(TripImage::getImageUrl).findFirst().orElse(null));
        }
    }

    private void validateFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty() || files.stream().anyMatch(file -> file == null || file.isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seleziona almeno una foto valida.");
        }
        files.forEach(photoStorageService::validatePhoto);
    }
}