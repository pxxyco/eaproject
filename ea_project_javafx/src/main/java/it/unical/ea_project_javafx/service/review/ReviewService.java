package it.unical.ea_project_javafx.service.review;

import com.google.gson.reflect.TypeToken;
import it.unical.ea_project_javafx.dto.review.ReviewDTO;
import it.unical.ea_project_javafx.service.api.ApiService;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Consumer;

public final class ReviewService {
    private static final String BASE_URL = ApiService.BASE_URL + "/api/reviews";
    private static final Type REVIEW_LIST = new TypeToken<List<ReviewDTO>>() {
    }.getType();

    private ReviewService() {
    }

    public static void create(ReviewDTO review, Consumer<ReviewDTO> onSuccess, Runnable onFailure,
            Consumer<Boolean> loading) {
        request("", review, ApiService.Method.POST, ReviewDTO.class, onSuccess, onFailure, loading);
    }

    public static void getActivityReview(Long id, Consumer<List<ReviewDTO>> onSuccess, Runnable onFailure,
            Consumer<Boolean> loading) {
        request("/activity/" + id, null, ApiService.Method.GET, REVIEW_LIST, onSuccess, onFailure, loading);
    }

    public static void getTripReview(Long id, Consumer<List<ReviewDTO>> onSuccess, Runnable onFailure,
            Consumer<Boolean> loading) {
        request("/trip/" + id, null, ApiService.Method.GET, REVIEW_LIST, onSuccess, onFailure, loading);
    }

    public static void update(ReviewDTO review, Consumer<ReviewDTO> onSuccess, Runnable onFailure,
            Consumer<Boolean> loading) {
        ReviewDTO body = ReviewDTO.builder().rating(review.getRating()).comment(review.getComment()).build();
        request("/" + review.getReviewId(), body, ApiService.Method.PUT, ReviewDTO.class, onSuccess, onFailure,
                loading);
    }

    public static void deleteReview(Long reviewId, Runnable onSuccess, Runnable onFailure, Consumer<Boolean> loading) {
        request("/" + reviewId, null, ApiService.Method.DELETE, Void.class,
                ignored -> {
                    if (onSuccess != null)
                        onSuccess.run();
                }, onFailure, loading);
    }

    private static <T> void request(String path, Object body, ApiService.Method method, Type type,
            Consumer<T> onSuccess, Runnable onFailure, Consumer<Boolean> loading) {
        ApiService.callDetailed(BASE_URL + path, body == null ? null : ApiService.toJson(body), method,
                response -> {
                    T result = type == Void.class ? null : ApiService.fromJson(response.body(), type);
                    if (onSuccess != null)
                        onSuccess.accept(result);
                }, message -> {
                    if (onFailure != null)
                        onFailure.run();
                }, loading);
    }
}
