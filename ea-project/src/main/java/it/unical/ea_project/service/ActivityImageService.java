package it.unical.ea_project.service;

import it.unical.ea_project.domain.ActivityImage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ActivityImageService {
    List<ActivityImage> getImagesByActivityId(Long activityId);
    ActivityImage uploadCover(Long activityId, Long organizerId, MultipartFile file);
    List<ActivityImage> uploadPhotos(Long activityId, Long organizerId, List<MultipartFile> files);
    void deleteImage(Long imageId);
}