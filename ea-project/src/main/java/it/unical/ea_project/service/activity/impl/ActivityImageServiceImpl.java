package it.unical.ea_project.service.activity.impl;

import it.unical.ea_project.domain.activity.Activity;
import it.unical.ea_project.domain.activity.ActivityImage;
import it.unical.ea_project.domain.user.User;
import it.unical.ea_project.repository.activity.ActivityImageRepository;
import it.unical.ea_project.repository.activity.ActivityRepository;
import it.unical.ea_project.service.activity.ActivityImageService;
import it.unical.ea_project.service.media.PhotoStorageService;
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
public class ActivityImageServiceImpl implements ActivityImageService {

    private final ActivityImageRepository activityImageRepository;
    private final ActivityRepository activityRepository;
    private final PhotoStorageService photoStorageService;

    @Override
    @Transactional(readOnly = true)
    public List<ActivityImage> getImagesByActivityId(Long activityId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("Attività non trovata con ID: " + activityId));
        return activityImageRepository.findByActivityOrderByOrderIndexAsc(activity);
    }

    @Override
    @Transactional
    public ActivityImage uploadCover(Long activityId, Long organizerId, MultipartFile file) {
        return uploadPhotos(activityId, organizerId, List.of(file), true).getFirst();
    }

    @Override
    @Transactional
    public List<ActivityImage> uploadPhotos(Long activityId, Long organizerId, List<MultipartFile> files) {
        return uploadPhotos(activityId, organizerId, files, false);
    }

    private List<ActivityImage> uploadPhotos(Long activityId, Long organizerId, List<MultipartFile> files,
            boolean makePrimary) {
        Activity activity = activityRepository.findByActivityIdAndDeletedAtIsNull(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attività non trovata."));
        User creator = activity.getCreatedBy();
        if (creator == null || !Objects.equals(creator.getId(), organizerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non puoi modificare le foto di questa attività.");
        }
        photoStorageService.validatePhotos(files);

        List<ActivityImage> existingImages = activityImageRepository.findByActivityOrderByOrderIndexAsc(activity);
        for (int index = 0; index < existingImages.size(); index++) {
            existingImages.get(index).setOrderIndex(index + (makePrimary ? 2 : 1));
        }
        int nextOrderIndex = makePrimary ? 1 : existingImages.size() + 1;
        activityImageRepository.saveAll(existingImages);

        List<ActivityImage> uploadedImages = new ArrayList<>();
        for (MultipartFile file : files) {
            ActivityImage photo = activityImageRepository.saveAndFlush(
                    new ActivityImage(null, activity, "", nextOrderIndex++));
            photo.setImageUrl(photoStorageService.storeActivityPhoto(activityId, photo.getImageId(), file));
            uploadedImages.add(activityImageRepository.save(photo));
        }
        return uploadedImages;
    }

    @Override
    @Transactional
    public void deleteImage(Long imageId, Long organizerId) {
        ActivityImage image = activityImageRepository.findById(imageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto non trovata."));
        Activity offer = image.getActivity();
        if (offer == null
                || offer.getDeletedAt() != null
                || offer.getCreatedBy() == null
                || !Objects.equals(offer.getCreatedBy().getId(), organizerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non puoi eliminare le foto di un'altra offerta.");
        }
        activityImageRepository.delete(image);
    }

}
