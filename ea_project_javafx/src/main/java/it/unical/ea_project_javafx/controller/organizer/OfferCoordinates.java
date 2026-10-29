package it.unical.ea_project_javafx.controller.organizer;

final class OfferCoordinates {
    private OfferCoordinates() {}
    static Double[] parse(String latitude, String longitude) {
        String lat = latitude == null ? "" : latitude.trim().replace(',', '.');
        String lon = longitude == null ? "" : longitude.trim().replace(',', '.');
        if (lat.isEmpty() && lon.isEmpty()) return new Double[]{null, null};
        try {
            double first = Double.parseDouble(lat), second = Double.parseDouble(lon);
            if (Double.isFinite(first) && Double.isFinite(second) && first >= -90 && first <= 90 && second >= -180 && second <= 180)
                return new Double[]{first, second};
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Inserisci entrambe le coordinate: latitudine tra -90 e 90, longitudine tra -180 e 180.");
    }
}
