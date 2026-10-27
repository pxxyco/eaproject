package it.unical.ea_project.controller.recommendation;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.unical.ea_project.security.AuthUser;
import it.unical.ea_project.service.recommendation.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class RecommendationController {
    private final RecommendationService service;

    @GetMapping("/mine")
    public RecommendationService.Picks mine(@AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "6") int limit) {
        return service.recommend(user.id(), limit);
    }
}
