package it.unical.ea_project.recommendation;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import it.unical.ea_project.security.AuthUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/recommendations")
@lombok.RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class RecommendationController {
    private final RecommendationService service;

    @GetMapping("/mine")
    public RecommendationService.Picks mine(@AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "6") int limit) {
        return service.recommend(user.id(), limit);
    }
}
