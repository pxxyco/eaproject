package it.unical.ea_project_javafx.ui;

import java.util.function.Function;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;

public final class TableSupport {
    private TableSupport() {
    }

    public static <T> void textColumn(TableColumn<T, String> column, Function<T, String> value) {
        column.setCellValueFactory(cell -> new SimpleStringProperty(value.apply(cell.getValue())));
        column.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                setText(empty ? null : text);
                setTooltip(empty || text == null || text.isBlank() ? null : new Tooltip(text));
            }
        });
    }
}
