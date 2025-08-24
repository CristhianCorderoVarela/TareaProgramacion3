package cr.ac.una.tareaprogramacion3.controller;

import cr.ac.una.client.soap.AdministradorDto;
import cr.ac.una.tareaprogramacion3.service.AdministradorService;
import cr.ac.una.tareaprogramacion3.util.Controller;
import cr.ac.una.tareaprogramacion3.util.FlowController;
import cr.ac.una.tareaprogramacion3.util.UserSession;
import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Optional;

public class LoginController extends Controller {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtContraseña;
    @FXML private Button btnInicioSesion;
    @FXML private Label lblErrorLogin;
    @FXML private Label lblEstadoWS;

    private final BooleanProperty bloqueando = new SimpleBooleanProperty(false);
    private final AdministradorService adminService = new AdministradorService();

    public LoginController() {}

    @Override
    @FXML
    public void initialize() {
        if (lblErrorLogin != null) lblErrorLogin.setText("");

        if (btnInicioSesion != null && txtUsuario != null && txtContraseña != null) {
            BooleanBinding formVacio = txtUsuario.textProperty().isEmpty()
                    .or(txtContraseña.textProperty().isEmpty());
            btnInicioSesion.disableProperty().bind(formVacio.or(bloqueando));
        }
        actualizarEstadoWS();
    }

    private void actualizarEstadoWS() {
        if (lblEstadoWS == null) return;
        lblEstadoWS.setText("Estado WS: verificando...");

        Task<Boolean> t = new Task<>() {
            @Override protected Boolean call() {
                return adminService.isServerUp();
            }
        };
        t.setOnSucceeded(ev -> {
            boolean ok = t.getValue();
            lblEstadoWS.setStyle(ok ? "-fx-text-fill:#1b8a2f;" : "-fx-text-fill:#cc0000;");
            lblEstadoWS.setText(ok ? "Estado WS: en línea." : "Estado WS: sin conexión.");
        });
        new Thread(t, "ping-ws").start();
    }

    @FXML
    private void onActionBtnInicioSesion(ActionEvent event) {
        if (lblErrorLogin != null) lblErrorLogin.setText("");

        String usuario = txtUsuario != null ? txtUsuario.getText().trim() : "";
        String contrasenna = txtContraseña != null ? txtContraseña.getText().trim() : "";

        if (usuario.isEmpty() || contrasenna.isEmpty()) {
            if (lblErrorLogin != null) lblErrorLogin.setText("Ingrese usuario y contraseña.");
            return;
        }

        bloqueando.set(true);

        Task<Void> t = new Task<>() {
            @Override protected Void call() {
                Optional<AdministradorDto> opt = adminService.login(usuario, contrasenna);
                Platform.runLater(() -> {
                    bloqueando.set(false);
                    if (opt.isPresent()) {
                        UserSession.get().setAdmin(opt.get());
                        FlowController.getInstance().goMain();
                        Stage s = (Stage) btnInicioSesion.getScene().getWindow();
                        if (s != null) s.close();
                    } else {
                        if (lblErrorLogin != null)
                            lblErrorLogin.setText("Usuario/contraseña inválidos o usuario inactivo.");
                    }
                });
                return null;
            }
        };
        new Thread(t, "login-ws").start();
    }
}
