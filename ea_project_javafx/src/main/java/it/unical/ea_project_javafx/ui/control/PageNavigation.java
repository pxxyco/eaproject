package it.unical.ea_project_javafx.ui.control;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class PageNavigation extends VBox {
    private final IntegerProperty currentPageIndex = new SimpleIntegerProperty();
    private final IntegerProperty pageCount = new SimpleIntegerProperty(1);
    private final HBox numbers = new HBox(8);
    private final Button previous = new Button("← Precedente"), next = new Button("Successiva →");
    private final Label summary = new Label();

    public PageNavigation() {
        super(12);
        setAlignment(Pos.CENTER);
        numbers.setAlignment(Pos.CENTER);
        getStyleClass().add("page-navigation");
        getStylesheets().add(getClass().getResource("/it/unical/ea_project_javafx/css/app.css").toExternalForm());
        previous.getStyleClass().add("page-nav");
        next.getStyleClass().add("page-nav");
        summary.getStyleClass().add("section-subtitle");
        HBox row = new HBox(10, previous, numbers, next);
        row.setAlignment(Pos.CENTER);
        getChildren().addAll(row, summary);
        previous.setOnAction(e -> setCurrentPageIndex(getCurrentPageIndex() - 1));
        next.setOnAction(e -> setCurrentPageIndex(getCurrentPageIndex() + 1));
        currentPageIndex.addListener((o, a, b) -> render());
        pageCount.addListener((o, a, b) -> {
            setCurrentPageIndex(getCurrentPageIndex());
            render();
        });
        render();
    }

    public IntegerProperty currentPageIndexProperty() {
        return currentPageIndex;
    }

    public int getCurrentPageIndex() {
        return currentPageIndex.get();
    }

    public void setCurrentPageIndex(int index) {
        currentPageIndex.set(Math.max(0, Math.min(getPageCount() - 1, index)));
    }

    public int getPageCount() {
        return pageCount.get();
    }

    public void setPageCount(int count) {
        pageCount.set(Math.max(1, count));
    }

    private void render() {
        numbers.getChildren().clear();
        int current = getCurrentPageIndex(), count = getPageCount();
        for (int p = 0; p < count; p++) {
            if (p != 0 && p != count - 1 && Math.abs(p - current) > 1) {
                if (p == 1 || p == count - 2)
                    numbers.getChildren().add(new Label("…"));
                continue;
            }
            int target = p;
            ToggleButton button = new ToggleButton(Integer.toString(p + 1));
            button.getStyleClass().add("page-button");
            button.setSelected(p == current);
            button.setOnAction(e -> {
                setCurrentPageIndex(target);
                button.setSelected(true);
            });
            numbers.getChildren().add(button);
        }
        previous.setDisable(current == 0);
        next.setDisable(current == count - 1);
        summary.setText("Pagina " + (current + 1) + " di " + count);
    }
}
