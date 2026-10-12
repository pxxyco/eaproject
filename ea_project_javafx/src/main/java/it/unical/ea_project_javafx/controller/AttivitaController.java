package it.unical.ea_project_javafx.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import it.unical.ea_project_javafx.controller.attivita.*;
import it.unical.ea_project_javafx.controller.attivita.recensioni.NewRecensioneController;
import it.unical.ea_project_javafx.dto.ActivityDTO;
import it.unical.ea_project_javafx.dto.TripDTO;
import it.unical.ea_project_javafx.model.UserSession;
import it.unical.ea_project_javafx.util.ApiService;
import it.unical.ea_project_javafx.util.WallpaperService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class AttivitaController {

    @FXML private StackPane root;
    @FXML private StackPane bodyContainer;

    @FXML private Button btnDescrizione;
    @FXML private Button btnFoto;
    @FXML private Button btnItinerario;
    @FXML private Button btnRecensioni;
    
    @FXML private Spinner<Integer> spinnerPartecipanti;
    @FXML private DatePicker datePicker;
    
    @FXML private ImageView heroImage;

    @FXML private Label lblCategoria;

    @FXML private Label lblLocalita;

    @FXML private Label lblPrezzo;

    @FXML private Label lblRating;

    @FXML private Label lblTitolo;

    @FXML private Label lblTotale;
    @FXML private Button btnPrenota;
    @FXML private javafx.scene.layout.VBox bookingCard;

    @FXML private LoadingOverlayController loadingCardController;
    @FXML private StackPane loadingOverlay;

    @FXML private StackPane newRecCard;
    @FXML private NewRecensioneController newRecCardController;
    private RecensioniContainerController recensioniController;

    @FXML private ScrollPane contentScroll;
    @FXML private StackPane hero;

    @FXML private StackPane navbarContainer;
    @FXML private ImageView bgImageView;

    private List<Button> sectionButtons;

    private Node itinerarioNode;
    private Node descrizioneNode;
    private Node fotoNode;
    private Node recensioniNode;
    private ActivityDTO activity;
    private TripDTO trip;
    private String id;
    private Type myType;

    private final static Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, ctx) ->
                    LocalDateTime.parse(json.getAsString()))
            .registerTypeAdapter(LocalDate.class, (JsonDeserializer<LocalDate>) (json, type, ctx) ->
                    LocalDate.parse(json.getAsString()))
            .registerTypeAdapter(LocalTime.class, (JsonDeserializer<LocalTime>) (json, type, ctx) ->
                    LocalTime.parse(json.getAsString()))
            .create();

    public enum Type {
        ACTIVITY, TRIP
    }

    final String path = "/it/unical/ea_project_javafx/fxml/attivita";

    @FXML
    void initialize() {

        setupNavbar();
        setupBackground();
        
        loadingOverlay.setVisible(true);
        boolean isOrganizer = "ORGANIZER".equalsIgnoreCase(UserSession.getInstance().getRole());
        bookingCard.setVisible(!isOrganizer);
        bookingCard.setManaged(!isOrganizer);
        
        btnInitialize();

        // USE FOR DEBUG AND TEST
        //loadData(Type.ACTIVITY, "1");
    }

    private void setupNavbar() {
        try {
            boolean isLoggedIn = UserSession.getInstance().isLoggedIn(); // O il tuo metodo per verificare la sessione
            String navbarPath = isLoggedIn
                    ? "/it/unical/ea_project_javafx/fxml/home/navbar-logged-in.fxml"
                    : "/it/unical/ea_project_javafx/fxml/home/navbar-logged-out.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(navbarPath));
            Node navbar = loader.load();
            navbarContainer.getChildren().setAll(navbar);
        } catch (IOException e) {
            System.err.println("Errore nel caricamento della navbar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupBackground() {
        if (bgImageView == null) {
            return;
        }
        if (root != null) {
            bgImageView.fitWidthProperty().bind(root.widthProperty());
            bgImageView.fitHeightProperty().bind(root.heightProperty());
        }
        loadBackground();
    }

    private void loadBackground() {
        if (bgImageView == null) {
            return;
        }

        new Thread(() -> {
            String imageUrl = WallpaperService.getDailyWallpaperUrl();
            Platform.runLater(() -> bgImageView.setImage(new Image(
                    imageUrl,
                    1920,
                    1080,
                    true,
                    true,
                    true
            )));
        }).start();
    }

    public void loadData(Type type, String id) {
        
        myType = type;
        this.id = id;
        
        switch (type) {
            case ACTIVITY -> {
                ApiService.get(
                        ApiService.BASE_URL + "/api/activities/" + id,
                        response ->  {
                            activity = GSON.fromJson(response.body(), ActivityDTO.class);

                            setActivityData();
                            handleDescrizione(null);
                            contentScroll.setManaged(true);
                            contentScroll.setVisible(true);
                            loadingOverlay.setVisible(false);
                        },
                        () -> {
                            loadingOverlay.setVisible(true);
                            loadingCardController.setError("Errore nel caricamento");
                        },
                        loading -> {
                            loadingOverlay.setVisible(true);
                            loadingCardController.setMessage("Caricamento...");
                        }
                );
            }
            case TRIP -> {
                ApiService.get(
                        ApiService.BASE_URL + "/api/trips/" + id,
                        response ->  {
                            trip = GSON.fromJson(response.body(), TripDTO.class);

                            setTripData();
                            handleDescrizione(null);
                            loadingOverlay.setVisible(false);
                            contentScroll.setManaged(true);
                            contentScroll.setVisible(true);
                        },
                        () -> {
                            loadingOverlay.setVisible(true);
                            loadingCardController.setError("Errore nel caricamento");
                        },
                        loading -> {
                            loadingOverlay.setVisible(true);
                            loadingCardController.setMessage("Caricamento...");
                        }
                );
            }
        }
    }

    private void setActivityData() {
        if (activity.getImages().isEmpty()) {
            heroImage.setImage(null);
        } else {
            // heroImage.setImage(new Image(ApiService.BASE_URL + activity.getImages().getFirst().getImageUrl()));
            heroImage.setImage(null);
        }

        if (activity.getStartDate() != null) {
            datePicker.setValue(LocalDate.from(activity.getStartDate()));
        }
        else {
            datePicker.setValue(LocalDate.now());
        }

        lblLocalita.setText(activity.getCity());
        lblTitolo.setText(activity.getTitle());
        if (activity.getAverageRating() != null) {
            lblRating.setText( "★ " + BigDecimal.valueOf(activity.getAverageRating()));
        }
        else {
            lblRating.setText("★ " + BigDecimal.ZERO);
        }


        BigDecimal prezzo = activity.getPrice() != null ? BigDecimal.valueOf(activity.getPrice()) : null;
        lblPrezzo.setText(setMoneyCurrency(prezzo));
        lblTotale.setText(setMoneyCurrency(prezzo));

    }

    private void setTripData() {
        if (trip.getCoverPhotoUrl() == null) {
            heroImage.setImage(null);
        } else {

            heroImage.setImage(new Image(ApiService.BASE_URL + trip.getCoverPhotoUrl()));
        }

        if (trip.getStartDate() != null) {
            datePicker.setValue(trip.getStartDate());
        }
        else {
            datePicker.setValue(LocalDate.now());
        }

        lblLocalita.setText(trip.getDestinationCity());
        lblTitolo.setText(trip.getTitle());

        if(trip.getAverageRating() != null) {
            lblRating.setText( "★ " + trip.getAverageRating());
        }
        else {
            lblRating.setText( "★ " + BigDecimal.ZERO);
        }

        lblPrezzo.setText(setMoneyCurrency(trip.getTotalPrice()));
        lblTotale.setText(setMoneyCurrency(trip.getTotalPrice()));
    }

    private Node loadSection(String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path + "/" + fxmlName));
            Node node = loader.load();
            Object controller = loader.getController();

            if (controller instanceof FotoContainerController fcc) {
                fcc.setOnGalleryRequested(this::openGallery);
            }

            if(controller instanceof RecensioniContainerController rcc){
                rcc.setOnNewReviewRequested(this::openNewReview);
                recensioniController = rcc;

            }

            if (myType == Type.ACTIVITY && activity != null
                    && controller instanceof ActivitySectionController asc) {
                asc.setData(activity);
            } else if (myType == Type.TRIP && trip != null
                    && controller instanceof TripSectionController tsc) {
                tsc.setData(trip);
            }

            return node;
        } catch (IOException e) {
            throw new UncheckedIOException("Impossibile caricare la sezione: " + fxmlName, e);
        }
    }

    private void openGallery(List<String> urls) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path + "/FotoGallery.fxml"));
            Node galleryNode = loader.load();
            FotoGalleryController controller = loader.getController();

            controller.setImages(urls);

            Stage stage = (Stage) root.getScene().getWindow();
            double width = stage.getScene() != null ? stage.getScene().getWidth() : 1280;
            double height = stage.getScene() != null ? stage.getScene().getHeight() : 720;

            Scene previousScene = root.getScene();
            controller.setOnBackLink( () -> {
                stage.setScene(previousScene);
            });

            Scene newScene = new Scene((Parent) galleryNode, width, height);
            stage.setScene(newScene);
            stage.show();

        } catch (IOException e) {
            throw new UncheckedIOException("Impossibile caricare la galleria", e);
        }
    }

    private void openNewReview(){
        newRecCardController.setOnClose( () -> {
            newRecCard.setManaged(false);
            newRecCard.setVisible(false);
            newRecCard.setDisable(true);
            newRecCardController.reset();
        });

        newRecCardController.setOnReviewCreated(review -> {
            if (recensioniController != null) recensioniController.addReview(review);
        });

        Long tripId = (myType == Type.TRIP) ? trip.getTripId() : null;
        Long activityId = (myType == Type.ACTIVITY) ? activity.getActivityId() : null;
        newRecCardController.setType(tripId, activityId);

        newRecCard.setDisable(false);
        newRecCard.setManaged(true);
        newRecCard.setVisible(true);
    }

    private void showSection(Node node) {
        bodyContainer.getChildren().setAll(node);
    }


    @FXML
    private void handleDescrizione(ActionEvent event) {
        setActiveButton(btnDescrizione);
        descrizioneNode = loadSection("Descrizione.fxml");
        showSection(descrizioneNode);
    }

    @FXML
    private void handleItinerario(ActionEvent event) {
        setActiveButton(btnItinerario);
        itinerarioNode = loadSection("Itinerario.fxml");
        showSection(itinerarioNode);
    }

    @FXML
    protected void handleFoto(ActionEvent event) {
        setActiveButton(btnFoto);
        fotoNode = loadSection("FotoContainer.fxml");
        showSection(fotoNode);
    }

    @FXML
    protected void handleRecensioni(ActionEvent event) {
        setActiveButton(btnRecensioni);
        recensioniNode = loadSection("RecensioniContainer.fxml");
        showSection(recensioniNode);
    }

    private void btnInitialize(){
        sectionButtons = List.of(btnDescrizione, btnItinerario, btnFoto, btnRecensioni);

        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 1);
        spinnerPartecipanti.setValueFactory(valueFactory);

        spinnerPartecipanti.valueProperty().addListener((obs, oldValue, newValue) -> {
            aggiornaPrezzoTotale(newValue);
        });
    }

    @FXML
    private void handlePrenota() {
        if ("ORGANIZER".equalsIgnoreCase(UserSession.getInstance().getRole())) {
            return;
        }
        if (!UserSession.getInstance().isLoggedIn()) {
            showAlert(Alert.AlertType.WARNING, "Accesso richiesto", "Devi effettuare il login per prenotare.");
            return;
        }

        Integer seats = spinnerPartecipanti.getValue();
        BigDecimal total = (myType == Type.ACTIVITY)
                ? BigDecimal.valueOf(activity.getPrice()).multiply(BigDecimal.valueOf(seats))
                : trip.getTotalPrice().multiply(BigDecimal.valueOf(seats));
        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Partecipanti: " + seats + "\nTotale: " + setMoneyCurrency(total) + "\nConfermi la prenotazione?",
                ButtonType.CANCEL,
                ButtonType.OK
        );
        confirmation.setHeaderText("Conferma prenotazione");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        String endpoint = myType == Type.TRIP
                ? "/api/bookings/trips/" + trip.getTripId()
                : "/api/bookings/activities/" + activity.getActivityId();

        ApiService.post(
                ApiService.BASE_URL + endpoint + "?seats=" + seats,
                null,
                response -> showAlert(Alert.AlertType.INFORMATION, "Prenotazione completata", "La prenotazione è stata registrata."),
                () -> showAlert(Alert.AlertType.ERROR, "Prenotazione non completata", "Non è stato possibile completare la prenotazione."),
                null
        );
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message);
        alert.setHeaderText(title);
        alert.showAndWait();
    }

    private void aggiornaPrezzoTotale(Integer newValue) {
        Integer partecipanti = spinnerPartecipanti.getValue();
        BigDecimal prezzo = (myType == Type.ACTIVITY)
                ? (activity.getPrice() != null ? BigDecimal.valueOf(activity.getPrice()) : BigDecimal.ZERO)
                : trip.getTotalPrice();

        BigDecimal totale = (prezzo != null) ? (prezzo.multiply(BigDecimal.valueOf(partecipanti))) : BigDecimal.ZERO;

        lblTotale.setText(setMoneyCurrency(totale));
    }

    private String setMoneyCurrency(BigDecimal amount) {
        return amount + "€";
    }

    private void setActiveButton(Button active) {
        for (Button b : sectionButtons) {
            b.getStyleClass().remove("section-button-active");
            if (!b.getStyleClass().contains("section-button")) {
                b.getStyleClass().add("section-button");
            }
        }
        active.getStyleClass().remove("section-button");
        active.getStyleClass().add("section-button-active");
    }

}
