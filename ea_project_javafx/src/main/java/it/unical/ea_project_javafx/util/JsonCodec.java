package it.unical.ea_project_javafx.util;

import com.google.gson.*;
import java.io.IOException;
import java.time.*;

/** Single JSON contract for all frontend consumers of backend data. */
public final class JsonCodec {

    private JsonCodec() {
    }

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, temporalAdapter(JsonCodec::deserializeLocalDateTime))
            .registerTypeAdapter(LocalDate.class, temporalAdapter(JsonCodec::deserializeLocalDate))
            .registerTypeAdapter(LocalTime.class, temporalAdapter(JsonCodec::deserializeLocalTime))
            .create();

    private static <T> TypeAdapter<T> temporalAdapter(java.util.function.Function<JsonElement, T> reader) {
        return new TypeAdapter<T>() {
            @Override
            public void write(com.google.gson.stream.JsonWriter out, T value) throws IOException {
                out.value(value.toString());
            }

            @Override
            public T read(com.google.gson.stream.JsonReader in) throws IOException {
                return reader.apply(JsonParser.parseReader(in));
            }
        }.nullSafe();
    }

    public static <T> T read(String json, java.lang.reflect.Type type) {
        return GSON.fromJson(json, type);
    }

    public static String write(Object value) {
        return GSON.toJson(value);
    }

    public static String string(JsonObject object, String key, String fallback) {
        return object != null && object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString()
                : fallback;
    }

    private static LocalDate deserializeLocalDate(JsonElement json) {
        if (!json.isJsonArray()) {
            return LocalDate.parse(json.getAsString());
        }
        JsonArray parts = json.getAsJsonArray();
        if (parts.size() != 3) {
            throw new JsonParseException("Expected a LocalDate as an ISO string or a 3-part array.");
        }
        return LocalDate.of(parts.get(0).getAsInt(), parts.get(1).getAsInt(), parts.get(2).getAsInt());
    }

    private static LocalDateTime deserializeLocalDateTime(JsonElement json) {
        if (!json.isJsonArray()) {
            return LocalDateTime.parse(json.getAsString());
        }
        JsonArray parts = json.getAsJsonArray();
        if (parts.size() < 5 || parts.size() > 7) {
            throw new JsonParseException("Expected a LocalDateTime as an ISO string or a 5-to-7-part array.");
        }
        int second = parts.size() > 5 ? parts.get(5).getAsInt() : 0;
        int nano = parts.size() > 6 ? parts.get(6).getAsInt() : 0;
        return LocalDateTime.of(
                parts.get(0).getAsInt(),
                parts.get(1).getAsInt(),
                parts.get(2).getAsInt(),
                parts.get(3).getAsInt(),
                parts.get(4).getAsInt(),
                second,
                nano);
    }

    private static LocalTime deserializeLocalTime(JsonElement json) {
        if (!json.isJsonArray()) {
            return LocalTime.parse(json.getAsString());
        }
        JsonArray parts = json.getAsJsonArray();
        if (parts.size() < 2 || parts.size() > 4) {
            throw new JsonParseException("Expected a LocalTime as an ISO string or a 2-to-4-part array.");
        }
        int second = parts.size() > 2 ? parts.get(2).getAsInt() : 0;
        int nano = parts.size() > 3 ? parts.get(3).getAsInt() : 0;
        return LocalTime.of(
                parts.get(0).getAsInt(),
                parts.get(1).getAsInt(),
                second,
                nano);
    }

}
