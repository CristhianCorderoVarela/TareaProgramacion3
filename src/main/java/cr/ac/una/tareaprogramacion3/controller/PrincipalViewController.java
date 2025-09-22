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

   
    @FXML private VBox VBoxMenuAdmin;

  
    @FXML private Button BtnHome;
    @FXML private Button BtnVentana1;
    @FXML private Button BtnVentana2;
    @FXML private Button BtnVentana3;
    @FXML private Button BtnVentana4;
    @FXML private Button BtnVentana5;
    @FXML private Button btnSalir;

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
}
