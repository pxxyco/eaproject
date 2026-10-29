package it.unical.ea_project_javafx.service.favorites;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import it.unical.ea_project_javafx.dto.favorites.FavoriteDTO;
import it.unical.ea_project_javafx.service.api.ApiService;
import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Consumer;

public final class FavoriteService {
    public enum Target {
        ACTIVITY("activities"), TRIP("trips");
        private final String path;
        Target(String path) { this.path = path; }
    }
    private static final String BASE = ApiService.BASE_URL + "/api/favorites";
    private static final Type LIST = new TypeToken<List<FavoriteDTO>>() {}.getType();
    private FavoriteService() {}
    public static void list(Consumer<List<FavoriteDTO>> success, Consumer<String> failure) {
        ApiService.callDetailed(BASE, null, ApiService.Method.GET,
                response -> success.accept(ApiService.fromJson(response.body(), LIST)), failure, null);
    }
    public static void state(Target target, String id, Consumer<Boolean> success,
            Consumer<String> failure, Consumer<Boolean> loading) {
        request(target, id, ApiService.Method.GET, success, failure, loading);
    }
    public static void set(Target target, String id, boolean favorite, Consumer<Boolean> success,
            Consumer<String> failure, Consumer<Boolean> loading) {
        request(target, id, favorite ? ApiService.Method.POST : ApiService.Method.DELETE, success, failure, loading);
    }
    private static void request(Target target, String id, ApiService.Method method,
            Consumer<Boolean> success, Consumer<String> failure, Consumer<Boolean> loading) {
        ApiService.callDetailed(BASE + "/" + target.path + "/" + id, null, method,
                response -> {
                    JsonObject state = ApiService.fromJson(response.body(), JsonObject.class);
                    success.accept(state.get("favorite").getAsBoolean());
                }, failure, loading);
    }
}
