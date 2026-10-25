package it.unical.ea_project_javafx.controller.home;

import com.google.gson.JsonParser;
import it.unical.ea_project_javafx.controller.activity.ActivityDetailsController;
import it.unical.ea_project_javafx.dto.home.ActivityHomeDTO;
import it.unical.ea_project_javafx.dto.home.TripHomeDTO;
import it.unical.ea_project_javafx.util.ApiService;
import it.unical.ea_project_javafx.util.SceneNavigator;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

/**
 * Bounded editorial selections, fetched and rendered only near the viewport.
 */
final class DiscoveryFeed implements AutoCloseable {
    private enum Layout {
        MOSAIC, CAROUSEL, SPOTLIGHT, ROWS, PAIR
    }

    private record Selection(String title, String subtitle, String path, String category, Layout layout,
            boolean trips) {
    }

    private record Offer(String title, String place, String date, String price, String rating, String photo,
            String id, ActivityDetailsController.Type type) {
    }

    private final ScrollPane scroll;
    private final VBox container;
    private final Consumer<String> browse;
    private final List<Section> sections = new ArrayList<>();
    private boolean closed, scheduled, ready;

    DiscoveryFeed(ScrollPane scroll, VBox container, Consumer<String> browse) {
        this.scroll = scroll;
        this.container = container;
        this.browse = browse;
        add(new Selection("Viaggi da mettere in agenda",
                "Partenze da esplorare, con luoghi, date e prezzi a colpo d’occhio.",
                "/api/trips/top/cards?page=0&size=3", "TRIP", Layout.MOSAIC, true));
        add(new Selection("Attività con le valutazioni più alte",
                "Le esperienze ordinate per valutazione. Scorri la selezione al tuo ritmo.",
                "/api/activities/top/cards?page=0&size=6", null, Layout.CAROUSEL, false));
        add(new Selection("Fuori dai soliti percorsi",
                "Escursioni e giornate all’aria aperta proposte dagli organizzatori.",
                "/api/activities/search?category=EXCURSION&page=0&size=5", "EXCURSION", Layout.SPOTLIGHT, false));
        add(new Selection("La cultura, da vicino",
                "Musei, visite e luoghi da conoscere: scegli la storia che ti incuriosisce.",
                "/api/activities/search?category=VISIT&page=0&size=3", "VISIT", Layout.ROWS, false));
        add(new Selection("Un viaggio anche nei sapori",
                "Esperienze a tavola e occasioni per conoscere un luogo attraverso il gusto.",
                "/api/activities/search?category=MEAL&page=0&size=2", "MEAL", Layout.PAIR, false));
        scroll.vvalueProperty().addListener((o, a, b) -> schedule());
        scroll.viewportBoundsProperty().addListener((o, a, b) -> schedule());
        container.layoutBoundsProperty().addListener((o, a, b) -> schedule());
        container.visibleProperty().addListener((o, a, b) -> {
            if (b)
                schedule();
        });
        schedule();
    }

    private void add(Selection selection) {
        Section section = new Section(selection);
        sections.add(section);
        container.getChildren().add(section.root);
    }

    void start() {
        ready = true;
        schedule();
    }

    void schedule() {
        if (!ready || closed || scheduled)
            return;
        scheduled = true;
        Platform.runLater(() -> {
            scheduled = false;
            if (closed || !container.isVisible() || scroll.getScene() == null)
                return;
            Node viewport = scroll.lookup(".viewport");
            if (viewport == null)
                return;
            Node content = scroll.getContent();
            double height = scroll.getViewportBounds().getHeight();
            double range = Math.max(0, content.getLayoutBounds().getHeight() - height);
            double offset = scroll.getVmax() <= scroll.getVmin() ? 0
                    : (scroll.getVvalue() - scroll.getVmin()) / (scroll.getVmax() - scroll.getVmin()) * range;
            for (Section section : sections) {
                var bounds = content.sceneToLocal(section.root.localToScene(section.root.getLayoutBounds()));
                if (!section.requested && bounds.getMinY() < offset + height + 220
                        && bounds.getMaxY() > offset - 120)
                    section.load();
            }
        });
    }

    private final class Section {
        private final Selection selection;
        private final VBox root = new VBox(18);
        private final VBox body = new VBox(12);
        private List<Offer> offers = List.of();
        private boolean requested;
        private int offset;

        Section(Selection selection) {
            this.selection = selection;
            root.getStyleClass().add("discovery-section");
            root.setMaxWidth(Double.MAX_VALUE);
            Label title = label(selection.title(), "section-title");
            title.setWrapText(true);
            Label subtitle = label(selection.subtitle(), "section-subtitle");
            subtitle.setWrapText(true);
            VBox text = new VBox(5, title, subtitle);
            HBox.setHgrow(text, Priority.ALWAYS);
            Button all = new Button("Esplora tutto →");
            all.getStyleClass().add("discovery-link");
            all.setMinWidth(120);
            all.setOnAction(event -> browse.accept(selection.category()));
            HBox header = new HBox(16, text, all);
            header.setAlignment(Pos.CENTER_LEFT);
            body.setMinHeight(selection.layout() == Layout.ROWS ? 300 : 360);
            Label hint = label("La selezione si carica mentre esplori.", "section-subtitle");
            body.getChildren().add(hint);
            root.getChildren().addAll(header, body);
            root.widthProperty().addListener((o, a, b) -> {
                if (Math.abs(a.doubleValue() - b.doubleValue()) > 1 && requested && !offers.isEmpty())
                    render();
            });
        }

        void load() {
            if (closed || requested)
                return;
            requested = true;
            body.getChildren().setAll(label("Caricamento della selezione…", "section-subtitle"));
            ApiService.callDetailed(ApiService.BASE_URL + selection.path(), null, ApiService.Method.GET, response -> {
                if (closed)
                    return;
                try {
                    List<Offer> loaded = new ArrayList<>();
                    for (var item : JsonParser.parseString(response.body()).getAsJsonArray()) {
                        if (selection.trips()) {
                            TripHomeDTO dto = ApiService.fromJson(item.toString(), TripHomeDTO.class);
                            loaded.add(new Offer(safe(dto.getTitle(), "Viaggio"), safe(dto.getLocation(), ""),
                                    date(dto.getStartDate()),
                                    money(dto.getTotalPrice()), rating(dto.getAverageRating()), dto.getCoverPhotoUrl(),
                                    String.valueOf(dto.getTripId()), ActivityDetailsController.Type.TRIP));
                        } else {
                            ActivityHomeDTO dto = ApiService.fromJson(item.toString(), ActivityHomeDTO.class);
                            loaded.add(new Offer(safe(dto.getTitle(), "Esperienza"), safe(dto.getCity(), ""),
                                    date(dto.getStartDate()),
                                    money(dto.getPrice()), rating(dto.getAverageRating()), dto.getImageUrl(),
                                    String.valueOf(dto.getActivityId()), ActivityDetailsController.Type.ACTIVITY));
                        }
                    }
                    offers = List.copyOf(loaded);
                    render();
                } catch (RuntimeException ex) {
                    failed();
                }
            }, message -> {
                if (!closed)
                    failed();
            }, null);
        }

        private void failed() {
            body.setMinHeight(100);
            Button retry = new Button("Selezione non disponibile · Riprova");
            retry.getStyleClass().add("discovery-link");
            retry.setOnAction(event -> {
                requested = false;
                load();
            });
            body.getChildren().setAll(retry);
        }

        private void render() {
            body.getChildren().clear();
            if (offers.isEmpty()) {
                body.setMinHeight(90);
                body.getChildren().add(
                        label("Le prossime proposte appariranno qui quando saranno disponibili.", "section-subtitle"));
                return;
            }
            body.setMinHeight(Region.USE_COMPUTED_SIZE);
            boolean narrow = root.getWidth() < 780;
            switch (selection.layout()) {
                case CAROUSEL -> carousel(narrow ? 2 : 3);
                case ROWS -> offers.forEach(offer -> body.getChildren().add(row(offer, false)));
                case PAIR -> {
                    HBox pair = new HBox(18);
                    for (Offer offer : offers)
                        addGrowing(pair, tile(offer, 190, false));
                    body.getChildren().add(pair);
                }
                case MOSAIC, SPOTLIGHT -> {
                    HBox mosaic = new HBox(18);
                    Button hero = tile(offers.getFirst(), selection.layout() == Layout.MOSAIC ? 245 : 265, true);
                    addGrowing(mosaic, hero);
                    if (offers.size() > 1) {
                        VBox side = new VBox(12);
                        side.setMinWidth(0);
                        side.setPrefWidth(narrow ? 280 : 400);
                        side.setMaxWidth(Double.MAX_VALUE);
                        HBox.setHgrow(side, Priority.ALWAYS);
                        if (selection.layout() == Layout.SPOTLIGHT && !narrow && offers.size() > 3) {
                            for (int start = 1; start < offers.size(); start += 2) {
                                HBox mini = new HBox(12);
                                for (int i = start; i < Math.min(start + 2, offers.size()); i++)
                                    addGrowing(mini, tile(offers.get(i), 76, false));
                                side.getChildren().add(mini);
                            }
                        } else {
                            offers.stream().skip(1).forEach(offer -> side.getChildren().add(row(offer, true)));
                        }
                        mosaic.getChildren().add(side);
                    }
                    body.getChildren().add(mosaic);
                }
            }
            schedule();
        }

        private void carousel(int visible) {
            offset = Math.min(offset, Math.max(0, offers.size() - visible));
            HBox cards = new HBox(16);
            for (int i = offset; i < Math.min(offers.size(), offset + visible); i++)
                addGrowing(cards, tile(offers.get(i), 175, false));
            body.getChildren().add(cards);
            if (offers.size() <= visible)
                return;
            Button previous = new Button("←");
            Button next = new Button("→");
            previous.setAccessibleText("Esperienze precedenti");
            next.setAccessibleText("Altre esperienze");
            previous.setDisable(offset == 0);
            next.setDisable(offset + visible >= offers.size());
            previous.getStyleClass().add("discovery-arrow");
            next.getStyleClass().add("discovery-arrow");
            previous.setOnAction(event -> {
                offset = Math.max(0, offset - visible);
                render();
            });
            next.setOnAction(event -> {
                offset = Math.min(offers.size() - visible, offset + visible);
                render();
            });
            HBox controls = new HBox(12, previous, label((offset + 1) + "–" + Math.min(offers.size(), offset + visible)
                    + " di " + offers.size(), "section-subtitle"), next);
            controls.setAlignment(Pos.CENTER_RIGHT);
            body.getChildren().add(controls);
        }
    }

    private static void addGrowing(HBox box, Region child) {
        child.setMinWidth(0);
        child.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(child, Priority.ALWAYS);
        box.getChildren().add(child);
    }

    private Button tile(Offer offer, double imageHeight, boolean hero) {
        Button button = offerButton(offer);
        button.getStyleClass().add(hero ? "discovery-tile-hero" : "discovery-tile");
        double height = imageHeight + (hero ? 155 : 136);
        button.setMinHeight(height);
        button.setPrefHeight(height);
        button.setMaxHeight(height);
        VBox content = new VBox();
        content.setMinHeight(height - 2);
        content.setPrefHeight(height - 2);
        content.setMaxHeight(height - 2);
        content.prefWidthProperty().bind(button.widthProperty().subtract(2));
        content.setMinWidth(0);
        VBox details = new VBox(7);
        details.setPadding(new Insets(hero ? 22 : 14));
        Label title = label(offer.title(), hero ? "discovery-hero-title" : "discovery-title");
        title.setWrapText(true);
        title.setMaxHeight(hero ? 70 : 48);
        Label meta = label(offer.place() + (offer.date().isBlank() ? "" : " · " + offer.date()), "discovery-meta");
        meta.setWrapText(true);
        HBox footer = new HBox(12, label(offer.rating(), "discovery-rating"), new Region(),
                label(offer.price(), "discovery-price"));
        HBox.setHgrow(footer.getChildren().get(1), Priority.ALWAYS);
        details.getChildren().addAll(title, meta, footer);
        content.getChildren().addAll(photo(offer.photo(), imageHeight), details);
        button.setGraphic(content);
        return button;
    }

    private Button row(Offer offer, boolean compact) {
        Button button = offerButton(offer);
        button.getStyleClass().add("discovery-row");
        double height = compact ? 192 : 116;
        button.setMinHeight(height);
        button.setPrefHeight(height);
        button.setMaxHeight(height);
        BorderPane content = new BorderPane();
        content.setMinHeight(height - 2);
        content.setPrefHeight(height - 2);
        content.setMaxHeight(height - 2);
        content.prefWidthProperty().bind(button.widthProperty().subtract(2));
        StackPane photo = photo(offer.photo(), height - 2);
        photo.setMinWidth(compact ? 112 : 160);
        photo.setPrefWidth(compact ? 112 : 160);
        photo.setMaxWidth(compact ? 112 : 160);
        Label title = label(offer.title(), "discovery-title");
        title.setWrapText(true);
        title.setMaxHeight(48);
        Label meta = label(offer.place() + " · " + offer.date(), "discovery-meta");
        meta.setWrapText(true);
        VBox text = new VBox(8, title, meta, label(offer.price() + "   " + offer.rating(), "discovery-price"));
        text.setPadding(new Insets(12, 16, 12, 0));
        text.setMinWidth(0);
        text.setMinHeight(0);
        BorderPane.setMargin(text, new Insets(0, 0, 0, 16));
        content.setLeft(photo);
        content.setCenter(text);
        button.setGraphic(content);
        return button;
    }

    private Button offerButton(Offer offer) {
        Button button = new Button();
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setAlignment(Pos.TOP_LEFT);
        button.setPadding(Insets.EMPTY);
        button.setPrefWidth(320);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setMinWidth(0);
        button.setAccessibleText(offer.title() + ", " + offer.place() + ", " + offer.date() + ", " + offer.price());
        button.setOnAction(event -> SceneNavigator.getInstance().loadScene(
                "/it/unical/ea_project_javafx/fxml/activity/activity-details.fxml", controller -> {
                    if (controller instanceof ActivityDetailsController details)
                        details.loadData(offer.type(), offer.id());
                }));
        return button;
    }

    private static StackPane photo(String raw, double height) {
        StackPane frame = new StackPane();
        frame.getStyleClass().add("discovery-photo");
        frame.setMinWidth(0);
        frame.setPrefWidth(0);
        frame.setMinHeight(height);
        frame.setPrefHeight(height);
        frame.setMaxHeight(height);
        ImageView image = new ImageView();
        image.setPreserveRatio(false);
        image.setSmooth(true);
        image.setManaged(false);
        image.fitWidthProperty().bind(frame.widthProperty());
        image.setFitHeight(height);
        frame.widthProperty().addListener((o, a, b) -> cropImage(image));
        loadPhoto(image, raw, false);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(frame.widthProperty());
        clip.heightProperty().bind(frame.heightProperty());
        clip.setArcWidth(22);
        clip.setArcHeight(22);
        frame.setClip(clip);
        frame.getChildren().add(image);
        return frame;
    }

    private static void loadPhoto(ImageView view, String raw, boolean fallback) {
        javafx.scene.image.Image image = fallback ? fallbackPhoto() : ExperienceCardController.previewImage(raw);
        view.setImage(image);
        Runnable completed = () -> {
            if (view.getImage() != image)
                return;
            if (image.isError() && !fallback) {
                ExperienceCardController
                        .removeCachedImage(it.unical.ea_project_javafx.util.ImageUtils.buildFullUrl(raw), image);
                loadPhoto(view, null, true);
            } else
                cropImage(view);
        };
        if (image.isError() || image.getProgress() >= 1)
            completed.run();
        else {
            var target = new java.lang.ref.WeakReference<>(view);
            javafx.beans.value.ChangeListener<Number> listener = new javafx.beans.value.ChangeListener<>() {
                @Override
                public void changed(javafx.beans.value.ObservableValue<? extends Number> property, Number old,
                        Number progress) {
                    if (progress.doubleValue() >= 1 || image.isError()) {
                        image.progressProperty().removeListener(this);
                        ImageView current = target.get();
                        if (current == null || current.getImage() != image)
                            return;
                        if (image.isError() && !fallback) {
                            ExperienceCardController.removeCachedImage(
                                    it.unical.ea_project_javafx.util.ImageUtils.buildFullUrl(raw), image);
                            loadPhoto(current, null, true);
                        } else
                            cropImage(current);
                    }
                }
            };
            image.progressProperty().addListener(listener);
        }
    }

    private static javafx.scene.image.Image fallbackImage;

    private static javafx.scene.image.Image fallbackPhoto() {
        if (fallbackImage == null)
            fallbackImage = new javafx.scene.image.Image(DiscoveryFeed.class.getResource(
                    "/it/unical/ea_project_javafx/images/experience-fallback.jpg").toExternalForm());
        return fallbackImage;
    }

    private static void cropImage(ImageView view) {
        var image = view.getImage();
        if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0 || view.getFitWidth() <= 0)
            return;
        double width = image.getWidth(), height = image.getHeight();
        double aspect = view.getFitWidth() / view.getFitHeight();
        if (width / height > aspect) {
            double crop = height * aspect;
            view.setViewport(new javafx.geometry.Rectangle2D((width - crop) / 2, 0, crop, height));
        } else {
            double crop = width / aspect;
            view.setViewport(new javafx.geometry.Rectangle2D(0, (height - crop) / 2, width, crop));
        }
    }

    private static Label label(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        return label;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String money(Number value) {
        return value == null ? "Prezzo da confermare"
                : String.format(Locale.ITALIAN, "da € %,.0f", value.doubleValue());
    }

    private static String rating(Number value) {
        return value == null || value.doubleValue() <= 0 ? "Nuova esperienza"
                : String.format(Locale.ITALIAN, "★ %.1f", value.doubleValue());
    }

    private static String date(String value) {
        try {
            return LocalDate.parse(value.substring(0, 10)).format(DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN));
        } catch (RuntimeException ex) {
            return "";
        }
    }

    @Override
    public void close() {
        closed = true;
    }
}
