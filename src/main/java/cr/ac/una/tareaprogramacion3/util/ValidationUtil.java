package cr.ac.una.tareaprogramacion3.util;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;

import java.util.regex.Pattern;

public final class ValidationUtil {
    private ValidationUtil(){}

    private static final String ERROR_STYLE = "-fx-border-color: #e74c3c; -fx-border-width: 1.5; -fx-border-radius: 4;";
    private static final String OK_STYLE = "";

    private static void mark(Node n, boolean ok) {
        if (n == null) return;
        n.setStyle(ok ? OK_STYLE : ERROR_STYLE);
    }

    /** Campo requerido (TextField/PasswordField/TextArea) */
    public static boolean require(TextInputControl field, String name) {
        boolean ok = field != null && field.getText() != null && !field.getText().trim().isEmpty();
        mark(field, ok);
        if (!ok) AlertUtil.warn("Dato requerido", "Completa el campo: " + name);
        return ok;
    }

    /** Combo requerido */
    public static boolean require(ComboBox<?> combo, String name) {
        boolean ok = combo != null && combo.getValue() != null;
        mark(combo, ok);
        if (!ok) AlertUtil.warn("Dato requerido", "Selecciona un valor en: " + name);
        return ok;
    }

    /** Validación simple de correo */
    private static final Pattern EMAIL_RX = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public static boolean email(TextField field) {
        String v = field == null ? null : field.getText();
        boolean ok = v != null && !v.isBlank() && EMAIL_RX.matcher(v.trim()).matches();
        mark(field, ok);
        if (!ok) AlertUtil.warn("Correo inválido", "Escribe un correo válido.");
        return ok;
    }
}
