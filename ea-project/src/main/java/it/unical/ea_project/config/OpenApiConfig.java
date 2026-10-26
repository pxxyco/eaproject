package it.unical.ea_project.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
        @Bean
        OpenAPI itineraApi() {
                return new OpenAPI().info(new Info().title("Itinera API").version("1.0")
                                .description(
                                                "API di Itinera. Gli endpoint protetti richiedono un access token JWT; il refresh si invia nell'header X-Refresh-Token."))
                                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
        }

        @Bean
        OpenApiCustomizer publicOperations() {
                return api -> {
                        if (api.getPaths() != null)
                                api.getPaths().forEach((path, item) -> item.readOperationsMap()
                                                .forEach((method, operation) -> {
                                                        boolean publicAuth = path.equals("/api/health")
                                                                        || path.equals("/api/users/login")
                                                                        || path.equals("/api/users/refresh")
                                                                        || path.equals("/api/users/exists")
                                                                        || path.equals("/api/users")
                                                                                        && method == HttpMethod.POST;
                                                        boolean publicRead = method == HttpMethod.GET
                                                                        && !path.contains("/organizer/")
                                                                        && (path.startsWith("/api/trips/") || path
                                                                                        .startsWith("/api/activities/")
                                                                                        || (path.equals("/api/reviews")
                                                                                                        || path.startsWith(
                                                                                                                        "/api/reviews/"))
                                                                                        || path.startsWith(
                                                                                                        "/api/activity-images/")
                                                                                        || path.startsWith(
                                                                                                        "/api/trip-images/"));
                                                        if (publicAuth || publicRead)
                                                                operation.setSecurity(List.of());
                                                }));
                };
        }
}
