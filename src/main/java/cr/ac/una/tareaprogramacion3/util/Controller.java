package cr.ac.una.tareaprogramacion3.util;

import javafx.scene.control.Control;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

/**
 * Clase base para todos los controladores de vistas.
 * Proporciona métodos comunes para manejo de flujo y eventos.
 */
public abstract class Controller {

    protected Stage stage;
    private String accion;

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Simula el evento de TAB para cambiar el foco entre controles.
     */
    public void sendTabEvent(KeyEvent event) {
        event.consume();
        KeyEvent tabEvent = new KeyEvent(
                KeyEvent.KEY_PRESSED, "", "", KeyCode.TAB,
                false, false, false, false
        );
        ((Control) event.getSource()).fireEvent(tabEvent);
    }

    /**
     * Método que debe implementar cada controlador para inicializar su vista.
     */
    public abstract void initialize();
}
