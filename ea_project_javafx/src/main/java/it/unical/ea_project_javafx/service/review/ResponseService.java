package it.unical.ea_project_javafx.service.review;

import it.unical.ea_project_javafx.dto.review.ResponseDTO;
import it.unical.ea_project_javafx.service.api.ApiService;
import java.util.Map;
import java.util.function.Consumer;

public class ResponseService {

    private static final String BASE_URL = ApiService.BASE_URL + "/api/responses";

    public static void create(Long reviewId, String comment, Consumer<ResponseDTO> onSuccess, Runnable onFailure) {
        ApiService.post(
                BASE_URL,
                ApiService.toJson(Map.of("reviewId", reviewId, "comment", comment)),
                response -> {
                    ResponseDTO created = ApiService.fromJson(response.body(), ResponseDTO.class);
                    if (onSuccess != null)
                        onSuccess.accept(created);
                },
                onFailure,
                null);
    }

    public static void update(Long id, String comment, Consumer<ResponseDTO> onSuccess, Runnable onFailure) {
        ApiService.put(
                BASE_URL + "/" + id,
                ApiService.toJson(Map.of("comment", comment)),
                response -> {
                    ResponseDTO updated = ApiService.fromJson(response.body(), ResponseDTO.class);
                    if (onSuccess != null)
                        onSuccess.accept(updated);
                },
                onFailure,
                null);
    }

    public static void delete(Long id, Runnable onSuccess, Runnable onFailure) {
        ApiService.delete(
                BASE_URL + "/" + id,
                response -> {
                    if (onSuccess != null)
                        onSuccess.run();
                },
                onFailure,
                null);
    }

}
