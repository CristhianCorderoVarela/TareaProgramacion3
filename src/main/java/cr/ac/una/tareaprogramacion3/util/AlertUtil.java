package cr.ac.una.tareaprogramacion3.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public final class AlertUtil {
    private AlertUtil(){}

    public static void error(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title == null ? "Error" : title);
        a.setHeaderText(null);
        a.setContentText(message == null ? "" : message);
        a.showAndWait();
    }

    public static void info(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title == null ? "Información" : title);
        a.setHeaderText(null);
        a.setContentText(message == null ? "" : message);
        a.showAndWait();
    }

    public static void warn(String title, String message) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(title == null ? "Atención" : title);
        a.setHeaderText(null);
        a.setContentText(message == null ? "" : message);
        a.showAndWait();
    }

    /** Devuelve true si el usuario confirma (OK) */
    public static boolean confirm(String title, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle(title == null ? "Confirmar" : title);
        a.setHeaderText(null);
        a.setContentText(message == null ? "" : message);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.OK;
    }
}
