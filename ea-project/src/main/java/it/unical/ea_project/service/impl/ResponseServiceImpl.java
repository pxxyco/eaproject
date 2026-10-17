package it.unical.ea_project.service.impl;


import it.unical.ea_project.domain.Response;
import it.unical.ea_project.domain.Review;
import it.unical.ea_project.domain.User;
import it.unical.ea_project.dto.ResponseDTO;
import it.unical.ea_project.mapper.ResponseMapper;
import it.unical.ea_project.repository.ReviewRepository;
import it.unical.ea_project.repository.UserRepository;
import it.unical.ea_project.service.ResponseService;
import it.unical.ea_project.repository.ResponseRepository;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

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
        Response response = findOwnedResponse(id,organizerId);

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