package it.unical.ea_project_javafx.util;

public class ImageUtils {

    public static final String DEFAULT_IMAGE_PATH = "/photo/default/experience.jpg";

    public static String getDefaultImageUrl() {
        return buildFullUrl(DEFAULT_IMAGE_PATH);
    }

    public static String buildFullUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            rawUrl = DEFAULT_IMAGE_PATH;
        }

        if (rawUrl.startsWith("file:")) return rawUrl;
        return ApiService.resolveUrl(rawUrl);
    }
}
