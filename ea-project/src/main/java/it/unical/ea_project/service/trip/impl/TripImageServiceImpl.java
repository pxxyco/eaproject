package it.unical.ea_project.service.trip.impl;

import it.unical.ea_project.domain.trip.Trip;
import it.unical.ea_project.domain.trip.TripImage;
import it.unical.ea_project.repository.trip.TripImageRepository;
import it.unical.ea_project.repository.trip.TripRepository;
import it.unical.ea_project.service.media.PhotoStorageService;
import it.unical.ea_project.service.trip.TripImageService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaggio non trovato.");
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
        Trip trip = tripRepository.findActiveForUpdate(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaggio non trovato."));
        if (trip.getCreatedBy() == null || !Objects.equals(trip.getCreatedBy().getId(), organizerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non puoi modificare le foto di questo viaggio.");
        }
        photoStorageService.validatePhotos(files);

        List<TripImage> existingImages = tripImageRepository.findByTripOrderByOrderIndexAsc(trip);
        if (existingImages.size() + files.size() > 50)
            throw new IllegalArgumentException("Un'offerta può contenere al massimo 50 foto.");
        for (int index = 0; index < existingImages.size(); index++) {
            existingImages.get(index).setOrderIndex(index + (makePrimary ? 2 : 1));
        }
        int nextOrderIndex = makePrimary ? 1 : existingImages.size() + 1;
        tripImageRepository.saveAll(existingImages);

        List<TripImage> uploadedImages = new ArrayList<>();
        for (MultipartFile file : files) {
            TripImage photo = tripImageRepository.saveAndFlush(
                    new TripImage(null, trip, "", nextOrderIndex++));
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
        if (offer != null)
            offer = tripRepository.findActiveForUpdate(offer.getTripId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offerta non disponibile."));
        if (offer == null
                || offer.getDeletedAt() != null
                || offer.getCreatedBy() == null
                || !Objects.equals(offer.getCreatedBy().getId(), organizerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non puoi eliminare le foto di un'altra offerta.");
        }

        if (Objects.equals(offer.getCoverPhotoUrl(), image.getImageUrl())) {
            offer.setCoverPhotoUrl(tripImageRepository.findByTripOrderByOrderIndexAsc(offer).stream()
                    .filter(candidate -> !Objects.equals(candidate.getImageId(), imageId))
                    .map(TripImage::getImageUrl).findFirst().orElse(null));
        }

        tripImageRepository.delete(image);
        photoStorageService.deleteOfferPhoto(image.getImageUrl());
    }

}
