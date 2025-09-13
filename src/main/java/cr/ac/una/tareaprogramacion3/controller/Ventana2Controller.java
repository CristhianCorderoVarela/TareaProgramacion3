package cr.ac.una.tareaprogramacion3.controller;

import cr.ac.una.client.soap.AdministradorDto;
import cr.ac.una.client.soap.RespuestaGeneral;
import cr.ac.una.tareaprogramacion3.model.AdministradorModel;
import cr.ac.una.tareaprogramacion3.service.AdministradorServiceCliente;
import cr.ac.una.tareaprogramacion3.util.AdminMapper;
import cr.ac.una.tareaprogramacion3.util.AlertUtil;
import cr.ac.una.tareaprogramacion3.util.Controller;
import cr.ac.una.tareaprogramacion3.util.SoapDataHelper;
import cr.ac.una.tareaprogramacion3.util.ValidationUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.stream.Collectors;

public class Ventana2Controller extends Controller {

    // === Formulario (ids EXACTOS como en tu FXML) ===
    @FXML private TextField txtNombre;
    @FXML private TextField txtApellidos;
    @FXML private TextField txtCedula;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtContrasena; // El WS de Admin no usa password en el DTO; lo dejamos visual/limpieza

    @FXML private ComboBox<String> cbEstado;

    // === Botones ===
    @FXML private Button btnAgregar;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;

    // === Tabla ===
    @FXML private TableView<AdministradorModel> tabla;
    @FXML private TableColumn<AdministradorModel, String> colNombre;
    @FXML private TableColumn<AdministradorModel, String> colApellidos;
    @FXML private TableColumn<AdministradorModel, String> colCedula;
    @FXML private TableColumn<AdministradorModel, String> colCorreo;
    @FXML private TableColumn<AdministradorModel, String> colUsuario;
    @FXML private TableColumn<AdministradorModel, String> colEstado;

    // === Servicio y estado ===
    private final AdministradorServiceCliente adminSvc = new AdministradorServiceCliente();
    private final ObservableList<AdministradorModel> data = FXCollections.observableArrayList();
    private AdministradorModel seleccionado;
    private boolean avisoInicialMostrado = false;

    // 2.2) Reemplaza initialize() por este
    @Override
    @FXML
    public void initialize() {
        cbEstado.setItems(FXCollections.observableArrayList("ACTIVO","INACTIVO"));

        colNombre.setCellValueFactory(c -> c.getValue().nombreProperty());
        colApellidos.setCellValueFactory(c -> c.getValue().apellidosProperty());
        colCedula.setCellValueFactory(c -> c.getValue().cedulaProperty());
        colCorreo.setCellValueFactory(c -> c.getValue().correoProperty());
        colUsuario.setCellValueFactory(c -> c.getValue().usuarioProperty());
        colEstado.setCellValueFactory(c -> c.getValue().estadoProperty());
        tabla.setItems(data);

        tabla.getSelectionModel().selectedItemProperty().addListener((obs, old, cur) -> {
            seleccionado = cur;
            if (cur != null) llenarFormulario(cur);
    });

    // Carga inicial segura
    cargarTodos();
}


    // === CARGAS ===
    // 2.3) Reemplaza cargarTodos() por este
private void cargarTodos() {
    try {
        // 1) Si el servidor ni siquiera responde al ping, no intentamos cargar.
        if (!adminSvc.isServerUp()) {
            if (!avisoInicialMostrado) {
                AlertUtil.warn("Aviso",
                        "El servicio de Administradores no responde en:\n" +
                        adminSvc.getEndpoint() +
                        "\nAbre el servidor o corrige la ruta del servicio.");
                avisoInicialMostrado = true;
            }
            return;
        }

        // 2) Llamada real
        RespuestaGeneral resp = adminSvc.obtenerTodos();

        // 3) Procesar respuesta
        if (resp != null && resp.isOk()) {
            List<AdministradorDto> lista = SoapDataHelper.asAdminList(resp.getData());
            data.setAll(lista.stream().map(AdminMapper::toModel).collect(Collectors.toList()));
        } else {
            if (!avisoInicialMostrado) {
                String msg = (resp != null && resp.getMensaje() != null && !resp.getMensaje().isBlank())
                        ? resp.getMensaje()
                        : "No se pudieron cargar los administradores.";
                AlertUtil.warn("Aviso", msg);
                avisoInicialMostrado = true;
            }
        }
    } catch (Exception ex) {
        // Cualquier fallo tipo “Envelope”, 404, etc. -> un solo aviso
        if (!avisoInicialMostrado) {
            AlertUtil.error("Error",
                    "Fallo al consultar el servicio en:\n" + adminSvc.getEndpoint() +
                    "\n\nDetalle: " + ex.getMessage());
            avisoInicialMostrado = true;
        }
    }
}


    // === FORM ===
    private void llenarFormulario(AdministradorModel m) {
        txtNombre.setText(m.getNombre());
        txtApellidos.setText(m.getApellidos());
        txtCedula.setText(m.getCedula());
        txtCorreo.setText(m.getCorreo());
        txtUsuario.setText(m.getUsuario());
        cbEstado.getSelectionModel().select(m.getEstado());
        if (txtContrasena != null) txtContrasena.clear(); // El DTO no maneja password; limpiamos por seguridad
    }

    private AdministradorModel leerFormulario() {
        AdministradorModel m = (seleccionado != null) ? seleccionado : new AdministradorModel();
        m.setNombre(txtNombre.getText());
        m.setApellidos(txtApellidos.getText());
        m.setCedula(txtCedula.getText());
        m.setCorreo(txtCorreo.getText());
        m.setUsuario(txtUsuario.getText());
        m.setEstado(cbEstado.getValue());
        return m;
    }

    private void limpiarFormulario() {
        seleccionado = null;
        txtNombre.clear();
        txtApellidos.clear();
        txtCedula.clear();
        txtCorreo.clear();
        txtUsuario.clear();
        if (txtContrasena != null) txtContrasena.clear();
        cbEstado.getSelectionModel().clearSelection();
        tabla.getSelectionModel().clearSelection();
    }

    private boolean validar() {
        return ValidationUtil.require(txtNombre,"Nombre")
            & ValidationUtil.require(txtApellidos,"Apellidos")
            & ValidationUtil.require(txtCedula,"Cédula")
            & ValidationUtil.email(txtCorreo)
            & ValidationUtil.require(txtUsuario,"Usuario")
            & ValidationUtil.require(cbEstado,"Estado");
    }

    // === BOTONES ===
    @FXML private void onAgregar() {
        if (!validar()) return;
        seleccionado = null; // Forzamos "nuevo"
        AdministradorDto dto = AdminMapper.toDto(leerFormulario());
        dto.setId(null);
        RespuestaGeneral resp = adminSvc.crear(dto);
        if (resp.isOk()) {
            AlertUtil.info("Listo", msg(resp.getMensaje(), "Administrador creado."));
            cargarTodos();
            limpiarFormulario();
        } else {
            AlertUtil.error("Error", msg(resp.getMensaje(), "No se pudo crear."));
        }
    }

    @FXML private void onEditar() {
        if (seleccionado == null) {
            AlertUtil.warn("Atención", "Selecciona un registro de la tabla.");
            return;
        }
        if (!validar()) return;
        AdministradorDto dto = AdminMapper.toDto(leerFormulario());
        // dto.setPassword(?) -> El DTO no lo tiene. Si el WS requiere password, se maneja por otro endpoint/campo.
        RespuestaGeneral resp = adminSvc.actualizar(dto);
        if (resp.isOk()) {
            AlertUtil.info("Listo", msg(resp.getMensaje(), "Cambios guardados."));
            cargarTodos();
            limpiarFormulario();
        } else {
            AlertUtil.error("Error", msg(resp.getMensaje(), "No se pudo actualizar."));
        }
    }

    @FXML private void onEliminar() {
        if (seleccionado == null) {
            AlertUtil.warn("Atención", "Selecciona un registro de la tabla.");
            return;
        }
        if (!AlertUtil.confirm("Confirmar", "¿Eliminar el administrador seleccionado?")) return;

        RespuestaGeneral resp = adminSvc.eliminar(seleccionado.getId());
        if (resp.isOk()) {
            AlertUtil.info("Listo", msg(resp.getMensaje(), "Eliminado con éxito."));
            cargarTodos();
            limpiarFormulario();
        } else {
            AlertUtil.error("Error", msg(resp.getMensaje(), "No se pudo eliminar."));
        }
    }

    // Mensaje por defecto si viene nulo/vacío
    private String msg(String original, String fallback) {
        return (original != null && !original.isBlank()) ? original : fallback;
    }
}
