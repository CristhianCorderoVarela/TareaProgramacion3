package cr.ac.una.tareaprogramacion3.controller;

import cr.ac.una.tareaprogramacion3.util.Controller;
import cr.ac.una.tareaprogramacion3.util.FlowController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class PrincipalViewController extends Controller {

    // Menú lateral (tal cual en tu FXML)
    @FXML private VBox VBoxMenuAdmin;

    // Botones (nombres EXACTOS a los del FXML)
    @FXML private Button BtnHome;
    @FXML private Button BtnVentana1;
    @FXML private Button BtnVentana2;
    @FXML private Button BtnVentana3;
    @FXML private Button BtnVentana4;
    @FXML private Button BtnVentana5;
    @FXML private Button btnSalir;

    @Override
    public void initialize() {
        // Asegura visibilidad del menú
        if (VBoxMenuAdmin != null) {
            VBoxMenuAdmin.setVisible(true);
            VBoxMenuAdmin.setManaged(true);
        }

        // Carga por defecto: WelcomeView
        Platform.runLater(() -> {
            FlowController.getInstance().limpiarLoader("WelcomeView");
            FlowController.getInstance().goView("WelcomeView");
        });
    }

    // === Handlers ===

    @FXML
    private void onActionBtnHome(ActionEvent e) {
        FlowController.getInstance().limpiarLoader("WelcomeView");
        FlowController.getInstance().goView("WelcomeView");
    }

    @FXML
    private void onActionBtnVentana1(ActionEvent e) {
        FlowController.getInstance().limpiarLoader("Ventana1");
        FlowController.getInstance().goView("Ventana1");
    }

    @FXML
    private void onActionBtnVentana2(ActionEvent e) {
        FlowController.getInstance().limpiarLoader("Ventana2");
        FlowController.getInstance().goView("Ventana2");
    }

    @FXML
    private void onActionBtnVentana3(ActionEvent e) {
        FlowController.getInstance().limpiarLoader("Ventana3");
        FlowController.getInstance().goView("Ventana3");
    }

    @FXML
    private void onActionBtnVentana4(ActionEvent e) {
        FlowController.getInstance().limpiarLoader("Ventana4");
        FlowController.getInstance().goView("Ventana4");
    }

    @FXML
    private void onActionBtnVentana5(ActionEvent e) {
        FlowController.getInstance().limpiarLoader("Ventana5");
        FlowController.getInstance().goView("Ventana5");
    }

    @FXML
    private void onActionBtnSalir(ActionEvent e) {
        // Cierra la ventana actual y vuelve al login
        Stage ventanaActual = (Stage) btnSalir.getScene().getWindow();
        if (ventanaActual != null) ventanaActual.close();

        FlowController.getInstance().limpiarLoader("login"); // nombre de tu login.fxml
        FlowController.getInstance().goViewInWindow("login");
    }
}
