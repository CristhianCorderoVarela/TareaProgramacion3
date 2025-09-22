package cr.ac.una.tareaprogramacion3.controller;

import cr.ac.una.client.soap.AdministradorDto;
import cr.ac.una.client.soap.RespuestaGeneral;
import cr.ac.una.tareaprogramacion3.model.AdministradorModel;
import cr.ac.una.tareaprogramacion3.service.AdministradorServiceCliente;
import cr.ac.una.tareaprogramacion3.util.AdminMapper;
import cr.ac.una.tareaprogramacion3.util.AlertUtil;
import cr.ac.una.tareaprogramacion3.util.Controller;
import cr.ac.una.tareaprogramacion3.util.ValidationUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

public class AdministradoresController extends Controller {

   
    @FXML private TextField txtBuscar;

    
    @FXML private TextField txtNombre;
    @FXML private TextField txtApellidos;
    @FXML private TextField txtCedula;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtContrasena;     
    @FXML private PasswordField txtContrasenaConf;  
    @FXML private CheckBox chkCambiarPass;          
    @FXML private ComboBox<String> cbEstado;
    @FXML private VBox panelForm;

    
    @FXML private Button btnAgregar;
    @FXML private Button btnEditar;
    @FXML private Button btnEliminar;
    @FXML private Button btnBuscar;
    @FXML private Button btnLimpiarBusqueda;

    
    @FXML private TableView<AdministradorModel> tabla;
    @FXML private TableColumn<AdministradorModel, String> colNombre;
    @FXML private TableColumn<AdministradorModel, String> colApellidos;
    @FXML private TableColumn<AdministradorModel, String> colCedula;
    @FXML private TableColumn<AdministradorModel, String> colCorreo;
    @FXML private TableColumn<AdministradorModel, String> colUsuario;
    @FXML private TableColumn<AdministradorModel, String> colEstado;

    private final AdministradorServiceCliente adminSvc = new AdministradorServiceCliente();
    private final ObservableList<AdministradorModel> data = FXCollections.observableArrayList();
    private AdministradorModel seleccionado;
    private boolean avisoInicialMostrado = false;

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
        
       


tabla.sceneProperty().addListener((obs, oldScene, scene) -> {
    if (scene == null) return;
    scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
        Object tgt = e.getTarget();

        boolean clickEnTabla = isInside(tabla, tgt);
        boolean clickEnForm  = isInside(panelForm, tgt);

       
        if (!clickEnTabla && !clickEnForm) {
            if (tabla.getSelectionModel().getSelectedItem() != null) {
                tabla.getSelectionModel().clearSelection();
                limpiarFormulario();
            }
            return;
        }

       
        if (clickEnTabla) {
            Node n = (tgt instanceof Node) ? (Node) tgt : null;
            while (n != null && n != tabla && !(n instanceof TableRow)) n = n.getParent();
            if (n instanceof TableRow<?> row && row.isEmpty()) {
                tabla.getSelectionModel().clearSelection();
                limpiarFormulario();
            }
            return; 
        }

        
    });
});



        
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, old, cur) -> {
            seleccionado = cur;
            if (cur != null) {
                llenarFormulario(cur);
                setModoEditar();
            } else {
                setModoCrear();
            }
        });

        
        if (chkCambiarPass != null) {
            chkCambiarPass.selectedProperty().addListener((o, ov, nv) -> updatePassControls());
        }

        cargarTodos();
        setModoCrear(); 
        
        
    }

   

    private void setModoCrear() {
        // botones
        btnAgregar.setDisable(false);
        btnEditar.setDisable(true);
        btnEliminar.setDisable(true);

       
        if (chkCambiarPass != null) chkCambiarPass.setSelected(true);
        enablePasswordFields(true);
    }

    private void setModoEditar() {
        
        btnAgregar.setDisable(true);
        btnEditar.setDisable(false);
        btnEliminar.setDisable(false);

        
        if (chkCambiarPass != null) chkCambiarPass.setSelected(false);
        enablePasswordFields(false);
        if (txtContrasena != null) txtContrasena.clear();
        if (txtContrasenaConf != null) txtContrasenaConf.clear();
    }

    private void updatePassControls() {
        
        if (seleccionado != null) {
            boolean enable = chkCambiarPass != null && chkCambiarPass.isSelected();
            enablePasswordFields(enable);
            if (!enable) {
                if (txtContrasena != null) txtContrasena.clear();
                if (txtContrasenaConf != null) txtContrasenaConf.clear();
            }
        }
    }

    private void enablePasswordFields(boolean enable) {
        if (txtContrasena != null) txtContrasena.setDisable(!enable);
        if (txtContrasenaConf != null) txtContrasenaConf.setDisable(!enable);
    }

   

    private void cargarTodos() {
        try {
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

            List<AdministradorDto> lista = adminSvc.obtenerTodosList();

            data.setAll(
                lista.stream()
                     .sorted(Comparator.comparing(a -> a.getNombre() == null ? "" : a.getNombre()))
                     .map(AdminMapper::toModel)
                     .collect(Collectors.toList())
            );

            if (lista.isEmpty() && !avisoInicialMostrado) {
                AlertUtil.warn("Aviso", "No hay administradores para mostrar.");
                avisoInicialMostrado = true;
            }

        } catch (Exception ex) {
            if (!avisoInicialMostrado) {
                AlertUtil.error("Error",
                    "Fallo al consultar el servicio en:\n" + adminSvc.getEndpoint() +
                    "\n\nDetalle: " + ex.getMessage());
                avisoInicialMostrado = true;
            }
        }
    }

    private void llenarFormulario(AdministradorModel m) {
        txtNombre.setText(m.getNombre());
        txtApellidos.setText(m.getApellidos());
        txtCedula.setText(m.getCedula());
        txtCorreo.setText(m.getCorreo());
        txtUsuario.setText(m.getUsuario());
        cbEstado.getSelectionModel().select(m.getEstado());
        if (txtContrasena != null) txtContrasena.clear();        
        if (txtContrasenaConf != null) txtContrasenaConf.clear(); 
    }

    private AdministradorModel leerFormulario() {
        AdministradorModel m = (seleccionado != null) ? seleccionado : new AdministradorModel();
        m.setNombre(s(txtNombre.getText()));
        m.setApellidos(s(txtApellidos.getText()));
        m.setCedula(s(txtCedula.getText()));
        m.setCorreo(s(txtCorreo.getText()));
        m.setUsuario(s(txtUsuario.getText()));
        m.setEstado(cbEstado.getValue());
        return m;
    }

    private static String s(String v){ return v == null ? null : v.trim(); }

    private void limpiarFormulario() {
        seleccionado = null;
        txtNombre.clear();
        txtApellidos.clear();
        txtCedula.clear();
        txtCorreo.clear();
        txtUsuario.clear();
        if (txtContrasena != null) txtContrasena.clear();
        if (txtContrasenaConf != null) txtContrasenaConf.clear();
        cbEstado.getSelectionModel().clearSelection();
        tabla.getSelectionModel().clearSelection();
        setModoCrear();
    }

    

    private boolean validar() {
        boolean ok =  ValidationUtil.require(txtNombre,"Nombre")
            & ValidationUtil.require(txtApellidos,"Apellidos")
            & ValidationUtil.require(txtCedula,"Cédula")
            & ValidationUtil.email(txtCorreo)
            & ValidationUtil.require(txtUsuario,"Usuario")
            & ValidationUtil.require(cbEstado,"Estado");

        
        if (seleccionado == null) {
            ok &= ValidationUtil.require(txtContrasena, "Contraseña");
            ok &= ValidationUtil.require(txtContrasenaConf, "Confirmación");
            if (ok && !txtContrasena.getText().equals(txtContrasenaConf.getText())) {
                AlertUtil.warn("Atención", "La confirmación de contraseña no coincide.");
                ok = false;
            }
        } else {
            
            if (chkCambiarPass != null && chkCambiarPass.isSelected()) {
                ok &= ValidationUtil.require(txtContrasena, "Contraseña");
                ok &= ValidationUtil.require(txtContrasenaConf, "Confirmación");
                if (ok && !txtContrasena.getText().equals(txtContrasenaConf.getText())) {
                    AlertUtil.warn("Atención", "La confirmación de contraseña no coincide.");
                    ok = false;
                }
            }
        }
        return ok;
    }

    

    @FXML
private void onBuscar() {
    String filtro = (txtBuscar.getText() == null) ? "" : txtBuscar.getText().trim().toLowerCase();

    if (filtro.isEmpty()) {
        
        tabla.setItems(data);
        return;
    }

    
    List<AdministradorModel> filtrados = data.stream()
            .filter(a ->
                (a.getNombre() != null && a.getNombre().toLowerCase().contains(filtro)) ||
                (a.getApellidos() != null && a.getApellidos().toLowerCase().contains(filtro)) ||
                (a.getUsuario() != null && a.getUsuario().toLowerCase().contains(filtro)) ||
                (a.getCorreo() != null && a.getCorreo().toLowerCase().contains(filtro)) ||
                (a.getCedula() != null && a.getCedula().toLowerCase().contains(filtro))
            )
            .collect(Collectors.toList());

    tabla.setItems(FXCollections.observableArrayList(filtrados));
}


private boolean matches(AdministradorDto a, String q) {
    return contains(a.getNombre(), q)
        || contains(a.getApellidos(), q)
        || contains(a.getUsuario(), q)
        || contains(a.getCorreo(), q)
        || contains(a.getCedula(), q);
}

private boolean contains(String field, String q) {
    return field != null && field.toLowerCase().contains(q);
}

    @FXML private void onLimpiarBusqueda() {
        txtBuscar.clear();
        cargarTodos();
    }

    @FXML private void onAgregar() {
       
        seleccionado = null;
        if (!validar()) return;

        AdministradorDto dto = AdminMapper.toDto(leerFormulario());
        dto.setId(null);
        dto.setPasswordPlain(txtContrasena.getText());

        RespuestaGeneral resp = adminSvc.crear(dto);
        if (resp != null && Boolean.TRUE.equals(resp.isOk())) {
            AlertUtil.info("Listo", msg(resp.getMensaje(), "Administrador creado."));
            cargarTodos();
            limpiarFormulario();
        } else {
            AlertUtil.error("Error", msg(resp != null ? resp.getMensaje() : null, "No se pudo crear."));
        }
    }

    @FXML private void onEditar() {
        if (seleccionado == null) {
            AlertUtil.warn("Atención", "Selecciona un registro de la tabla.");
            return;
        }
        if (!validar()) return;

        AdministradorDto dto = AdminMapper.toDto(leerFormulario());
        
        if (chkCambiarPass != null && chkCambiarPass.isSelected()) {
            dto.setPasswordPlain(txtContrasena.getText());
        }

        RespuestaGeneral resp = adminSvc.actualizar(dto);
        if (resp != null && Boolean.TRUE.equals(resp.isOk())) {
            AlertUtil.info("Listo", msg(resp.getMensaje(), "Cambios guardados."));
            cargarTodos();
            limpiarFormulario();
        } else {
            AlertUtil.error("Error", msg(resp != null ? resp.getMensaje() : null, "No se pudo actualizar."));
        }
    }

    @FXML private void onEliminar() {
        if (seleccionado == null) {
            AlertUtil.warn("Atención", "Selecciona un registro de la tabla.");
            return;
        }
        if (!AlertUtil.confirm("Confirmar", "¿Eliminar el administrador seleccionado?")) return;

        RespuestaGeneral resp = adminSvc.eliminar(seleccionado.getId());
        if (resp != null && Boolean.TRUE.equals(resp.isOk())) {
            AlertUtil.info("Listo", msg(resp.getMensaje(), "Eliminado con éxito."));
            cargarTodos();
            limpiarFormulario();
        } else {
            AlertUtil.error("Error", msg(resp != null ? resp.getMensaje() : null, "No se pudo eliminar."));
        }
    }

    private String msg(String original, String fallback) {
        return (original != null && !original.isBlank()) ? original : fallback;
    }
    
private boolean isInside(Node container, Object eventTarget) {
    if (!(eventTarget instanceof Node n)) return false;
    for (Node cur = n; cur != null; cur = cur.getParent()) {
        if (cur == container) return true;
    }
    return false;
}
}