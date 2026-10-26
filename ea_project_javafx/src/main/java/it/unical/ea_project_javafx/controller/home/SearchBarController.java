package it.unical.ea_project_javafx.controller.home;

import com.google.gson.reflect.TypeToken;
import it.unical.ea_project_javafx.dto.home.ActivitySuggestionDTO;
import it.unical.ea_project_javafx.dto.home.TripSuggestionDTO;
import it.unical.ea_project_javafx.util.ApiService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.util.Duration;
import lombok.Setter;

import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class SearchBarController {

    @FXML
    private javafx.scene.layout.HBox searchControls;
    @FXML
    private javafx.scene.layout.StackPane searchSlot;

    public javafx.scene.layout.HBox controls() {
        return searchControls;
    }

    public javafx.scene.layout.StackPane slot() {
        return searchSlot;
    }

    public void relocated() {
        if (suggestionsPopup != null)
            suggestionsPopup.hide();
    }

    public void close() {
        debounce.stop();
        suggestionRequestId++;
        relocated();
    }

    @FXML
    public CheckBox SetData;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> categoryComboBox;
    @FXML
    private DatePicker datePicker;

    @Setter
    private ExperienceListController experienceListController;

    private ContextMenu suggestionsPopup;
    private final PauseTransition debounce = new PauseTransition(Duration.millis(300));

    private long suggestionRequestId = 0;
    private boolean suppressSuggestions = false;

    private static final String TRIP_LABEL = "Viaggi";
    private static final Map<String, String> CATEGORY_MAP = Map.of(
            "Escursione", "EXCURSION",
            "Visita", "VISIT",
            "Trasporto", "TRANSPORT",
            "Hotel", "HOTEL",
            "Ristorazione", "MEAL",
            "Altro", "OTHER");

    @FXML
    public void initialize() {
        createCombobox();
        if (SetData != null)
            SetData.setSelected(false);

        if (datePicker != null && SetData != null) {
            datePicker.setValue(LocalDate.now());
            datePicker.disableProperty().bind(SetData.selectedProperty().not());
        }

        if (categoryComboBox != null) {
            categoryComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
                debounce.stop();
                suggestionRequestId++;
                if (suggestionsPopup != null)
                    suggestionsPopup.hide();
            });
        }

        if (searchField != null) {
            suggestionsPopup = new ContextMenu();
            debounce.setOnFinished(event -> fetchSuggestions(searchField.getText()));

            searchField.setOnAction(event -> {
                if (suggestionsPopup != null) {
                    suggestionsPopup.hide();
                }
                handleSearch(event);
            });

            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                suggestionRequestId++;
                debounce.stop();

                if (suppressSuggestions || newValue == null || newValue.isBlank()) {
                    suggestionsPopup.hide();
                    return;
                }
                debounce.playFromStart();
            });
        }
    }

    private void createCombobox() {
        ObservableList<String> categories = FXCollections.observableArrayList(
                "Attività", TRIP_LABEL, "Escursione", "Visita", "Trasporto", "Hotel", "Ristorazione", "Altro");
        if (categoryComboBox != null) {
            categoryComboBox.setItems(categories);
            categoryComboBox.getSelectionModel().selectFirst();
        }
    }

    private boolean isTripSelected() {
        return categoryComboBox != null && TRIP_LABEL.equals(categoryComboBox.getValue());
    }

    private void fetchSuggestions(String query) {
        if (query == null || query.isBlank()) {
            suggestionsPopup.hide();
            return;
        }

        String normalizedQuery = query.trim();
        long requestId = suggestionRequestId;
        String endpoint = isTripSelected() ? "/api/trips/suggest" : "/api/activities/suggest";
        String url = ApiService.BASE_URL + endpoint + "?query="
                + URLEncoder.encode(normalizedQuery, StandardCharsets.UTF_8) + "&limit=6";

        ApiService.get(
                url,
                response -> {
                    if (response.statusCode() != 200) {
                        Platform.runLater(() -> {
                            if (requestId == suggestionRequestId)
                                suggestionsPopup.hide();
                        });
                        return;
                    }

                    if (isTripSelected()) {
                        List<TripSuggestionDTO> suggestions = parseTripSuggestions(response.body());
                        Platform.runLater(() -> {
                            if (requestId == suggestionRequestId)
                                showTripSuggestions(suggestions);
                        });
                    } else {
                        List<ActivitySuggestionDTO> suggestions = parseActivitySuggestions(response.body());
                        Platform.runLater(() -> {
                            if (requestId == suggestionRequestId)
                                showActivitySuggestions(suggestions);
                        });
                    }
                },
                () -> Platform.runLater(() -> {
                    if (requestId == suggestionRequestId)
                        suggestionsPopup.hide();
                }),
                null);
    }

    private void showActivitySuggestions(List<ActivitySuggestionDTO> suggestions) {
        suggestionsPopup.getItems().clear();
        if (suggestions == null || suggestions.isEmpty()) {
            suggestionsPopup.hide();
            return;
        }

        for (ActivitySuggestionDTO suggestion : suggestions) {
            String city = suggestion.getCity() != null ? suggestion.getCity() : "";
            MenuItem menuItem = new MenuItem(suggestion.getTitle() + " - " + city);

            menuItem.setOnAction(event -> {
                suggestionRequestId++;
                suppressSuggestions = true;
                searchField.setText(suggestion.getTitle());
                suppressSuggestions = false;
                suggestionsPopup.hide();
            });

            suggestionsPopup.getItems().add(menuItem);
        }
        showSuggestionsPopup();
    }

    private void showTripSuggestions(List<TripSuggestionDTO> suggestions) {
        suggestionsPopup.getItems().clear();
        if (suggestions == null || suggestions.isEmpty()) {
            suggestionsPopup.hide();
            return;
        }

        for (TripSuggestionDTO suggestion : suggestions) {
            String location = suggestion.getLocation() != null ? suggestion.getLocation() : "";
            MenuItem menuItem = new MenuItem(suggestion.getTitle() + " - " + location);

            menuItem.setOnAction(event -> {
                suggestionRequestId++;
                suppressSuggestions = true;
                searchField.setText(suggestion.getTitle());
                suppressSuggestions = false;
                suggestionsPopup.hide();
            });

            suggestionsPopup.getItems().add(menuItem);
        }
        showSuggestionsPopup();
    }

    private void showSuggestionsPopup() {
        if (suggestionsPopup.isShowing())
            return;

        Point2D point = searchField.localToScreen(0, searchField.getHeight());
        if (point != null) {
            suggestionsPopup.show(searchField, point.getX(), point.getY());
        }
    }

    private List<ActivitySuggestionDTO> parseActivitySuggestions(String jsonBody) {
        try {
            Type listType = new TypeToken<List<ActivitySuggestionDTO>>() {
            }.getType();
            List<ActivitySuggestionDTO> result = ApiService.fromJson(jsonBody, listType);
            return result != null ? result : List.of();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    private List<TripSuggestionDTO> parseTripSuggestions(String jsonBody) {
        try {
            Type listType = new TypeToken<List<TripSuggestionDTO>>() {
            }.getType();
            List<TripSuggestionDTO> result = ApiService.fromJson(jsonBody, listType);
            return result != null ? result : List.of();
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    @FXML
    void handleSearch(ActionEvent event) {
        if (experienceListController == null)
            return;

        String query = searchField != null ? searchField.getText() : "";
        boolean tripSearch = isTripSelected();
        String selectedLabel = categoryComboBox != null ? categoryComboBox.getValue() : null;
        String backendCategory = tripSearch ? null : CATEGORY_MAP.get(selectedLabel);
        LocalDate selectedDate = (SetData != null && SetData.isSelected() && datePicker != null) ? datePicker.getValue()
                : null;

        experienceListController.search(
                tripSearch ? ExperienceListController.SearchMode.TRIP : ExperienceListController.SearchMode.ACTIVITY,
                query,
                backendCategory,
                selectedDate);
    }

    public void reset() {
        debounce.stop();
        suggestionRequestId++;

        if (suggestionsPopup != null) {
            suggestionsPopup.hide();
            suggestionsPopup.getItems().clear();
        }
        if (searchField != null)
            searchField.clear();
        if (categoryComboBox != null)
            categoryComboBox.getSelectionModel().selectFirst();
        if (datePicker != null)
            datePicker.setValue(LocalDate.now());
        if (SetData != null)
            SetData.setSelected(false);
    }
}
