package cr.ac.una.tareaprogramacion3.controller;

import cr.ac.una.tareaprogramacion3.util.Controller;
import cr.ac.una.tareaprogramacion3.util.FlowController;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class PrincipalViewController extends Controller {

   
    @FXML private VBox VBoxMenuAdmin;

  
    @FXML private Button BtnHome;
    @FXML private Button BtnVentana1;
    @FXML private Button BtnVentana2;
    @FXML private Button BtnVentana3;
    @FXML private Button BtnVentana4;
    @FXML private Button BtnVentana5;
    @FXML private Button btnSalir;

    // NUEVO: botón de ayuda
    @FXML private Button btnAyuda;

    @Override
    public void initialize() {
        
        if (VBoxMenuAdmin != null) {
            VBoxMenuAdmin.setVisible(true);
            VBoxMenuAdmin.setManaged(true);
        }

        
        Platform.runLater(() -> {
            FlowController.getInstance().limpiarLoader("DashboardView");
            FlowController.getInstance().goView("DashboardView");
        });
    }

    

    @FXML
    private void onActionBtnHome(ActionEvent e) {
        FlowController.getInstance().limpiarLoader("DashboardView");
        FlowController.getInstance().goView("DashboardView");
    }

    @FXML
    private void onActionBtnVentana1(ActionEvent e) {
        FlowController.getInstance().limpiarLoader("ProyectosView");
        FlowController.getInstance().goView("ProyectosView");
    }

    @FXML
    private void onActionBtnVentana2(ActionEvent e) {
        FlowController.getInstance().limpiarLoader("AdministradoresView");
        FlowController.getInstance().goView("AdministradoresView");
    }

    @FXML
    private void onActionBtnVentana3(ActionEvent e) {
        FlowController.getInstance().limpiarLoader("ActividadesView");
        FlowController.getInstance().goView("ActividadesView");
    }

    @FXML
    private void onActionBtnVentana4(ActionEvent e) {
        FlowController.getInstance().limpiarLoader("SeguimientosView");
        FlowController.getInstance().goView("SeguimientosView");
    }

    

    @FXML
    private void onActionBtnSalir(ActionEvent e) {
        // Cierra la ventana actual y vuelve al login
        Stage ventanaActual = (Stage) btnSalir.getScene().getWindow();
        if (ventanaActual != null) ventanaActual.close();

        FlowController.getInstance().limpiarLoader("loginView"); // nombre de tu login.fxml
        FlowController.getInstance().goViewInWindow("loginView");
    }

    // NUEVO: acción del botón de ayuda
    @FXML
    private void onActionBtnAyuda(ActionEvent e) {
        String ayuda = """
                Centro de ayuda — Creación de proyectos

                ▸ Proyectos
                  1) Ve a “Proyectos”.
                  2) Completa datos y fechas (inicio/fin planificadas).
                  3) Guarda. Estado inicial: PLANIFICADO.
                  4) Posibilidad de la creacion de excel en base al proyecto seleccionado.     

                ▸ Actividades
                  1) En “Actividades” elige el proyecto.
                  2) Crea actividades con descripción, encargado/correo y fechas.
                  3) Puedes moverlas entre columnas.
                  Importante: si el proyecto tiene un Seguimiento, no se pueden editar/mover/crear actividades.

                ▸ Seguimientos
                  1) Registra fecha, observación y % avance.
                  2) Mientras exista un seguimiento, las actividades quedan bloqueadas.

                ▸ Administradores
                  1) Crea/edita usuarios. Usuario y correo deben ser únicos.
                  2) Cambios de contraseña se guardan de forma segura.
                """;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Centro de ayuda");
        alert.setHeaderText("Creación de proyectos");
        TextArea ta = new TextArea(ayuda);
        ta.setWrapText(true);
        ta.setEditable(false);
        ta.setPrefRowCount(18);
        ta.setPrefColumnCount(60);
        alert.getDialogPane().setContent(ta);
        alert.showAndWait();
    }
}
