package it.unical.ea_project_javafx.controller.home;

import com.google.gson.reflect.TypeToken;
import it.unical.ea_project_javafx.dto.home.ActivityHomeDTO;
import it.unical.ea_project_javafx.dto.home.TripHomeDTO;
import it.unical.ea_project_javafx.security.UserSession;
import it.unical.ea_project_javafx.util.ApiService;
import it.unical.ea_project_javafx.util.PageTransitions;
import it.unical.ea_project_javafx.util.ScrollSupport;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Setter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ExperienceListController {
    @FXML public Label label1, label2;
    @FXML private VBox cardsContainer, loadingIndicatorBox;
    @FXML private HBox pageNumbers;
    @FXML private Button resetButton, previousPageButton, nextPageButton, retryPageButton;
    @FXML private Label pageSummary, pageError;
    @Setter private SearchBarController searchBarController;

    private static final int PAGE_SIZE = 9;
    private int currentPage = -1, requestedPage, totalPages, requestCounter;
    private long totalResults;
    private boolean loading, closed, revealResults;
    private String currentQuery = "", currentCategoryFilter;
    private LocalDate currentDateFilter;
    public enum SearchMode { TOP, ACTIVITY, TRIP, ALL }
    private SearchMode mode = SearchMode.TOP;
    private boolean organizerHome;

    @FXML private void initialize() { updatePager(); }

    public void close() { closed = true; requestCounter++; }

    public void loadHomeForCurrentUser() {
        organizerHome = "ORGANIZER".equalsIgnoreCase(UserSession.getInstance().getRole());
        resetSearch(false);
    }

    private void requestPage(int page, boolean reveal) {
        if (closed) return;
        requestedPage = page;
        revealResults = reveal;
        requestCounter++;
        loading = true;
        pageError.setVisible(false); pageError.setManaged(false);
        retryPageButton.setVisible(false); retryPageButton.setManaged(false);
        showLoading(true);
        updatePager();
        switch (mode) {
            case TOP -> loadPage("/api/activities/top/cards", new TypeToken<List<ActivityHomeDTO>>(){}.getType(), this::appendActivityCards);
            case ACTIVITY -> loadPage(searchPath(false), new TypeToken<List<ActivityHomeDTO>>(){}.getType(), this::appendActivityCards);
            case TRIP -> loadPage(searchPath(true), new TypeToken<List<TripHomeDTO>>(){}.getType(), this::appendTripCards);
            case ALL -> loadAllResults();
        }
    }

    private String searchPath(boolean trip) {
        StringBuilder path = new StringBuilder(trip ? "/api/trips/search" : "/api/activities/search");
        path.append("?query=").append(URLEncoder.encode(currentQuery, StandardCharsets.UTF_8));
        if (!trip && currentCategoryFilter != null) path.append("&category=").append(currentCategoryFilter);
        if (currentDateFilter != null) path.append("&date=").append(currentDateFilter);
        return path.toString();
    }

    private String pageUrl(String path) {
        return ApiService.BASE_URL + path + (path.contains("?") ? "&" : "?") + "page=" + requestedPage + "&size=" + PAGE_SIZE;
    }

    private static long total(HttpResponse<String> response) {
        if (response.statusCode() != 200) throw new IllegalStateException("Risposta non valida");
        long count = Long.parseLong(response.headers().firstValue("X-Total-Count").orElseThrow());
        if (count < 0) throw new IllegalStateException("Conteggio non valido");
        return count;
    }

    private <T> void loadPage(String path, Type type, Consumer<List<T>> render) {
        int request = requestCounter, page = requestedPage;
        ApiService.get(pageUrl(path), response -> {
            try {
                List<T> parsed = ApiService.fromJson(response.body(), type);
                List<T> items = parsed == null ? List.of() : parsed;
                long count = total(response);
                Platform.runLater(() -> {
                    if (closed || request != requestCounter) return;
                    finishPage(page, count, pages(count), () -> render.accept(items));
                });
            } catch (RuntimeException error) { failPage(request); }
        }, () -> failPage(request), null);
    }

    private static int pages(long count) { return (int)Math.min(Integer.MAX_VALUE, count / PAGE_SIZE + (count % PAGE_SIZE == 0 ? 0 : 1)); }

    private void loadAllResults() {
        int request = requestCounter, page = requestedPage;
        AtomicInteger remaining = new AtomicInteger(2);
        AtomicBoolean failed = new AtomicBoolean();
        AtomicReference<List<ActivityHomeDTO>> activities = new AtomicReference<>(List.of());
        AtomicReference<List<TripHomeDTO>> trips = new AtomicReference<>(List.of());
        AtomicLong activityCount = new AtomicLong(), tripCount = new AtomicLong();
        Runnable complete = () -> {
            if (remaining.decrementAndGet() != 0) return;
            Platform.runLater(() -> {
                if (closed || request != requestCounter) return;
                if (failed.get()) { failPage(request); return; }
                finishPage(page, activityCount.get() + tripCount.get(), Math.max(pages(activityCount.get()), pages(tripCount.get())), () -> {
                    appendActivityCards(activities.get()); appendTripCards(trips.get());
                });
            });
        };
        ApiService.get(pageUrl("/api/activities/search"), response -> {
            try {
                List<ActivityHomeDTO> parsed = ApiService.fromJson(response.body(), new TypeToken<List<ActivityHomeDTO>>(){}.getType());
                activities.set(parsed == null ? List.of() : parsed); activityCount.set(total(response));
            } catch (RuntimeException error) { failed.set(true); }
            finally { complete.run(); }
        }, () -> { failed.set(true); complete.run(); }, null);
        ApiService.get(pageUrl("/api/trips/search"), response -> {
            try {
                List<TripHomeDTO> parsed = ApiService.fromJson(response.body(), new TypeToken<List<TripHomeDTO>>(){}.getType());
                trips.set(parsed == null ? List.of() : parsed); tripCount.set(total(response));
            } catch (RuntimeException error) { failed.set(true); }
            finally { complete.run(); }
        }, () -> { failed.set(true); complete.run(); }, null);
    }

    private void finishPage(int page, long count, int pages, Runnable render) {
        // A concurrent deletion may remove the requested last page: return to the new last page.
        if (pages > 0 && page >= pages) { requestPage(pages - 1, true); return; }
        cardsContainer.getChildren().clear();
        render.run();
        currentPage = pages == 0 ? 0 : page;
        totalResults = count; totalPages = pages; loading = false;
        if (cardsContainer.getChildren().isEmpty()) {
            Label empty = new Label("Nessuna esperienza corrisponde alla ricerca. Prova un'altra destinazione o modifica i filtri.");
            empty.setWrapText(true); empty.getStyleClass().add("section-subtitle");
            cardsContainer.getChildren().add(empty);
        }
        showLoading(false); updatePager();
        PageTransitions.appear(cardsContainer);
        if (revealResults) ScrollSupport.reveal(label1, searchBarController == null ? 24 : searchBarController.controls().getHeight() + 24);
    }

    private void failPage(int request) {
        Platform.runLater(() -> {
            if (closed || request != requestCounter) return;
            loading = false; showLoading(false); updatePager();
            if (currentPage < 0) pageSummary.setText("Caricamento non riuscito");
            pageError.setText("Non riesco a caricare la pagina " + (requestedPage + 1) + ". Riprova.");
            pageError.setVisible(true); pageError.setManaged(true);
            retryPageButton.setVisible(true); retryPageButton.setManaged(true);
        });
    }

    private void showLoading(boolean show) { loadingIndicatorBox.setVisible(show); loadingIndicatorBox.setManaged(show); }

    private void updatePager() {
        previousPageButton.setDisable(loading || currentPage <= 0);
        nextPageButton.setDisable(loading || totalPages == 0 || currentPage >= totalPages - 1);
        pageNumbers.getChildren().clear();
        if (totalPages == 0) { pageSummary.setText(loading ? "Caricamento delle esperienze…" : "Nessun risultato"); return; }
        pageSummary.setText("Pagina " + (currentPage + 1) + " di " + totalPages + " · " + totalResults + (totalResults == 1 ? " esperienza" : " esperienze"));
        TreeSet<Integer> visible = new TreeSet<>();
        if (totalPages <= 7) {
            for (int i = 0; i < totalPages; i++) visible.add(i);
        } else {
            visible.add(0); visible.add(totalPages - 1);
            for (int i = Math.max(0, currentPage - 1); i <= Math.min(totalPages - 1, currentPage + 1); i++) visible.add(i);
        }
        int last = -1;
        for (int page : visible) {
            if (last >= 0 && page > last + 1) {
                Label gap = new Label("…"); gap.getStyleClass().add("section-subtitle"); pageNumbers.getChildren().add(gap);
            }
            ToggleButton button = new ToggleButton(Integer.toString(page + 1));
            button.getStyleClass().add("home-page-button"); button.setSelected(page == currentPage);
            button.setAccessibleText("Pagina " + (page + 1) + (page == currentPage ? ", attuale" : ""));
            button.setDisable(loading);
            button.setOnAction(event -> { button.setSelected(page == currentPage); goToPage(page); });
            pageNumbers.getChildren().add(button); last = page;
        }
    }

    private void goToPage(int page) { if (!loading && page >= 0 && page < totalPages && page != currentPage) requestPage(page, true); }
    @FXML private void handlePreviousPage() { goToPage(currentPage - 1); }
    @FXML private void handleNextPage() { goToPage(currentPage + 1); }
    @FXML private void handleRetryPage() { if (!loading) requestPage(requestedPage, true); }

    private void appendActivityCards(List<ActivityHomeDTO> activities) {
        appendCardsGeneric(activities, ExperienceCardController::setData);
    }

    private void appendTripCards(List<TripHomeDTO> trips) {
        appendCardsGeneric(trips, ExperienceCardController::setData);
    }

    private <T> void appendCardsGeneric(List<T> items, Consumer2<ExperienceCardController, T> binder) {
        if (cardsContainer == null || items == null || items.isEmpty()) return;

        final int CARDS_PER_ROW = 3;
        HBox currentRow = null;

        if (!cardsContainer.getChildren().isEmpty()) {
            javafx.scene.Node lastNode = cardsContainer.getChildren().get(cardsContainer.getChildren().size() - 1);
            if (lastNode instanceof HBox row && row.getChildren().size() < CARDS_PER_ROW) {
                currentRow = row;
            }
        }

        for (T item : items) {
            if (currentRow == null || currentRow.getChildren().size() >= CARDS_PER_ROW) {
                currentRow = new HBox(16);
                currentRow.getStyleClass().add("cards-row");
                currentRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                cardsContainer.getChildren().add(currentRow);
            }
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/it/unical/ea_project_javafx/fxml/home/experience-card.fxml"));
                VBox cardNode = loader.load();
                ExperienceCardController controller = loader.getController();
                binder.accept(controller, item);
                currentRow.getChildren().add(cardNode);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FunctionalInterface
    private interface Consumer2<T, U> {
        void accept(T t, U u);
    }

    public void search(SearchMode mode, String query, String category, LocalDate date) {
        this.mode = mode;
        currentQuery = query == null ? "" : query.trim();
        currentCategoryFilter = category; currentDateFilter = date;
        label1.setText(mode == SearchMode.TRIP ? "Ricerca tra i viaggi" : category == null ? "Ricerca su tutte le attività" : "Ricerca su categoria " + category);
        label2.setText(currentQuery);
        resetButton.setVisible(true); resetButton.setManaged(true);
        resetPages(true);
    }

    @FXML public void handleReset(javafx.event.ActionEvent event) { resetSearch(true); }
    public void resetSearch() { resetSearch(true); }
    private void resetSearch(boolean reveal) {
        label1.setText(organizerHome ? "Tutti i viaggi e le attività" : "Esperienze più apprezzate dai viaggiatori");
        label2.setText(organizerHome ? "Esplora le offerte pubblicate" : "Valutate da utenti di tutto il mondo");
        currentQuery = ""; currentCategoryFilter = null; currentDateFilter = null;
        mode = organizerHome ? SearchMode.ALL : SearchMode.TOP;
        resetButton.setVisible(false); resetButton.setManaged(false);
        if (searchBarController != null) searchBarController.reset();
        resetPages(reveal);
    }

    private void resetPages(boolean reveal) {
        cardsContainer.getChildren().clear(); currentPage = -1; totalPages = 0; totalResults = 0;
        requestPage(0, reveal);
    }
}
