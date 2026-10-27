package it.unical.ea_project.service.activity;

import it.unical.ea_project.domain.activity.ActivityImage;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface ActivityImageService {
    List<ActivityImage> getImagesByActivityId(Long activityId);

    ActivityImage uploadCover(Long activityId, Long organizerId, MultipartFile file);

    List<ActivityImage> uploadPhotos(Long activityId, Long organizerId, List<MultipartFile> files);

    void deleteImage(Long imageId, Long organizerId);
}
