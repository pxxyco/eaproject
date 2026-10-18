package it.unical.ea_project_javafx.service;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import it.unical.ea_project_javafx.dto.ReviewDTO;
import it.unical.ea_project_javafx.util.ApiService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

// TODO: replace generic status code range check (>=200 && <300) with specific status codes per case

public class ReviewService {

    private static final String BASE_URL = ApiService.BASE_URL + "/api/reviews" ;

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, type, ctx) ->
                    src == null ? null : new JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, ctx) ->
                    LocalDateTime.parse(json.getAsString()))
            .create();

    public static void create(ReviewDTO review, Consumer<ReviewDTO> onSuccess, Runnable onFailure, Consumer<Boolean> loading) {
        ApiService.post(
                BASE_URL,
                GSON.toJson(review),
                response -> {
                    if(response.statusCode() >= 200 && response.statusCode() <= 300) {
                        ReviewDTO created = GSON.fromJson(response.body(), ReviewDTO.class);
                        if(onSuccess != null) onSuccess.accept(created);
                    }
                    else  if(onFailure != null) onFailure.run();
                },
                onFailure,
                loading
        );
    }

    public static void getActivityReview(Long id, Consumer<List<ReviewDTO>> onSuccess, Runnable onFailure, Consumer<Boolean> loading) {
        ApiService.get(
                BASE_URL + "/activity/" + id,
                response -> {
                    if(response.statusCode() >= 200 && response.statusCode() <= 300) {
                        List<ReviewDTO> reviews = GSON.fromJson(response.body(), new TypeToken<List<ReviewDTO>>() {}.getType());
                        if (onSuccess != null) onSuccess.accept(reviews);
                    }
                    else  if(onFailure != null) onFailure.run();
                },
                onFailure,
                loading
        );
    }

    public static void getTripReview(Long id, Consumer<List<ReviewDTO>> onSuccess, Runnable onFailure, Consumer<Boolean> loading) {
        ApiService.get(
                BASE_URL + "/trip/" + id,
                response -> {
                    if(response.statusCode() >= 200 && response.statusCode() <= 300) {
                        List<ReviewDTO> reviews = GSON.fromJson(response.body(), new TypeToken<List<ReviewDTO>>() {}.getType());
                        if (onSuccess != null) onSuccess.accept(reviews);
                    }
                    else  if(onFailure != null) onFailure.run();
                },
                onFailure,
                loading
        );
    }

    public static void update(ReviewDTO review, Consumer<ReviewDTO> onSuccess, Runnable onFailure, Consumer<Boolean> loading) {
        ReviewDTO body = ReviewDTO.builder()
                .rating(review.getRating())
                .comment(review.getComment())
                .build();

        ApiService.put(
            BASE_URL + "/" + review.getReviewId(),
                GSON.toJson(body),
                response -> {
                    ReviewDTO updated =  GSON.fromJson(response.body(), ReviewDTO.class);
                    if(onSuccess != null) onSuccess.accept(updated);
                },
                onFailure,
                loading
        );
    }

    public static void deleteReview(Long reviewId, Runnable onSuccess, Runnable onFailure, Consumer<Boolean> loading) {
        ApiService.delete(
                BASE_URL + "/" + reviewId,
                response -> {if(onSuccess != null) onSuccess.run(); },
                onFailure,
                loading
        );
    }
}
