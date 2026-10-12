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

        if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            return rawUrl;
        }

        String baseUrl = ApiService.BASE_URL;
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String formattedPath = rawUrl.startsWith("/") ? rawUrl : "/" + rawUrl;
        return baseUrl + formattedPath;
    }
}