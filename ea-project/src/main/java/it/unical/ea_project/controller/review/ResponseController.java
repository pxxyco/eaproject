package it.unical.ea_project.controller.review;

import it.unical.ea_project.dto.review.ResponseDTO;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.review.ResponseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/responses")
@RequiredArgsConstructor
public class ResponseController {

    private final ResponseService responseService;

    public record ResponseRequest(Long reviewId, String comment) {
    }

    @PostMapping
    public ResponseEntity<ResponseDTO> createResponse(@RequestBody ResponseRequest request,
            @AuthenticationPrincipal AuthUser user) {
        ResponseDTO newResponse = responseService.create(
                request.reviewId(),
                request.comment(),
                user.id());
        return new ResponseEntity<>(newResponse, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseDTO> updateResponse(@PathVariable Long id,
            @RequestBody ResponseRequest request,
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(responseService.update(id, request.comment(), user.id()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResponse(@PathVariable Long id,
            @AuthenticationPrincipal AuthUser user) {
        responseService.delete(id, user.id());
        return ResponseEntity.noContent().build();
    }
}
