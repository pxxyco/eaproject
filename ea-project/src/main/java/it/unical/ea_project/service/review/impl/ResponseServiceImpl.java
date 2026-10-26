package it.unical.ea_project.service.review.impl;

import it.unical.ea_project.domain.review.Response;
import it.unical.ea_project.domain.review.Review;
import it.unical.ea_project.domain.user.User;
import it.unical.ea_project.dto.review.ResponseDTO;
import it.unical.ea_project.mapper.review.ResponseMapper;
import it.unical.ea_project.repository.review.ResponseRepository;
import it.unical.ea_project.repository.review.ReviewRepository;
import it.unical.ea_project.repository.user.UserRepository;
import it.unical.ea_project.service.review.ResponseService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ResponseServiceImpl implements ResponseService {

    private final ResponseRepository responseRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional
    public ResponseDTO create(Long reviewId, String comment, Long organizerId) {

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recensione non trovata"));

        checkPlaceOwner(review, organizerId);

        if (!responseRepository.findByReviewReviewIdAndDeletedFalse(reviewId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La recensione ha già una risposta");
        }

        Response response = new Response();
        response.setOrganizer(userRepository.getReferenceById(organizerId));
        response.setReview(review);
        response.setComment(comment);
        response.setCreatedAt(LocalDateTime.now());
        response.setDeleted(false);

        return ResponseMapper.toResponseDTO(responseRepository.save(response));
    }

    @Override
    @Transactional
    public ResponseDTO update(Long id, String comment, Long organizerId) {
        Response response = findOwnedResponse(id, organizerId);

        response.setComment(comment);
        response.setUpdatedAt(LocalDateTime.now());

        return ResponseMapper.toResponseDTO(response);
    }

    @Override
    @Transactional
    public void delete(Long id, Long organizerId) {
        Response response = findOwnedResponse(id, organizerId);

        response.setDeleted(true);
    }

    private Response findOwnedResponse(Long id, Long organizerId) {
        Response response = responseRepository.findById(id)
                .filter(r -> !r.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Risposta non trovata"));

        if (!response.getOrganizer().getId().equals(organizerId)) {
            throw new AccessDeniedException("La risposta non appartiene all'organizzatore autenticato.");
        }
        return response;
    }

    private void checkPlaceOwner(Review review, Long organizerId) {
        User owner = review.getActivity() != null
                ? review.getActivity().getCreatedBy()
                : review.getTrip().getCreatedBy();

        if (owner == null || !owner.getId().equals(organizerId)) {
            throw new AccessDeniedException("Solo il proprietario può rispondere a questa recensione.");
        }
    }
}
