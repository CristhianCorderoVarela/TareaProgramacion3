package cr.ac.una.tareaprogramacion3.controller;

import cr.ac.una.tareaprogramacion3.util.Controller;

import cr.ac.una.client.soap.ProyectoDto;
import cr.ac.una.client.soap.ProyectoService;
import cr.ac.una.client.soap.ProyectoWS;
import cr.ac.una.client.soap.RespuestaGeneral;
import jakarta.xml.ws.BindingProvider;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.GregorianCalendar;
import java.util.Map;

public class ProyectoDialogController extends Controller {

    // ===== Controles =====
    @FXML private TextField txtNombre;
    @FXML private ComboBox<String> cmbEstado;

    @FXML private TextField txtPatrocinador;
    @FXML private TextField txtCorreoPatrocinador;

    @FXML private TextField txtLiderUsuario;
    @FXML private TextField txtCorreoLiderUsuario;

    @FXML private TextField txtLiderTecnico;
    @FXML private TextField txtCorreoLiderTecnico;

    // (si en tu FXML todavía existe, no hace nada; puedes eliminarlo del FXML)
    @FXML private TextArea txtCorreos;

    @FXML private DatePicker dpInicioPlan, dpFinPlan, dpInicioReal, dpFinReal;

    // ===== Estado =====
    private ProyectoWS port;
    private ProyectoDto modelo;
    private boolean guardado = false;

    @Override
    public void initialize() {
        // FlowController la invoca al abrir; no necesitamos lógica aquí.
    }

    /** Llamado por Ventana1Controller antes de abrir el modal */
    public void init(String endpointUrl, ProyectoDto dto) {
        // Configurar stub y endpoint
        ProyectoService svc = new ProyectoService();
        this.port = svc.getProyectoWSPort();
        Map<String, Object> ctx = ((BindingProvider) port).getRequestContext();
        ctx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

        // DTO (nuevo o edición)
        this.modelo = (dto != null) ? dto : new ProyectoDto();

        // Poblar combos y campos
        cmbEstado.getItems().setAll("PLANIFICADO", "EN_CURSO", "SUSPENDIDO", "FINALIZADO");

        txtNombre.setText(nv(modelo.getNombre()));
        cmbEstado.getSelectionModel().select(nv(modelo.getEstado()).isEmpty() ? "PLANIFICADO" : modelo.getEstado());

        txtPatrocinador.setText(nv(modelo.getPatrocinadorNombre()));
        txtCorreoPatrocinador.setText(nv(modelo.getPatrocinadorCorreo()));

        txtLiderUsuario.setText(nv(modelo.getLiderUsuarioNombre()));
        txtCorreoLiderUsuario.setText(nv(modelo.getLiderUsuarioCorreo()));

        txtLiderTecnico.setText(nv(modelo.getLiderTecnicoNombre()));
        txtCorreoLiderTecnico.setText(nv(modelo.getLiderTecnicoCorreo()));

        setDate(dpInicioPlan, modelo.getFechaInicioPlanificada());
        setDate(dpFinPlan,    modelo.getFechaFinalPlanificada());
        setDate(dpInicioReal, modelo.getFechaInicioReal());
        setDate(dpFinReal,    modelo.getFechaFinalReal());
    }

    // ===== Utilitarios =====
    private String nv(String s) { return s == null ? "" : s; }

    private void setDate(DatePicker dp, XMLGregorianCalendar xcal) {
        if (dp == null) return;
        if (xcal == null) { dp.setValue(null); return; }
        LocalDate ld = xcal.toGregorianCalendar().toZonedDateTime().toLocalDate();
        dp.setValue(ld);
    }

    private XMLGregorianCalendar toXml(LocalDate ld) {
        if (ld == null) return null;
        try {
            GregorianCalendar gc = GregorianCalendar.from(ld.atStartOfDay(ZoneId.systemDefault()));
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
        } catch (DatatypeConfigurationException e) {
            return null;
        }
    }

    private boolean blank(String s) { return s == null || s.isBlank(); }

    private boolean emailOk(String s) {
        if (blank(s)) return false;
        // Validación simple y suficiente para el caso
        return s.contains("@") && s.indexOf('@') > 0 && s.indexOf('@') < s.length() - 1;
    }

    // ===== Acciones =====
    @FXML
    private void onGuardar() {
        // --- Validaciones mínimas (coinciden con NOT NULL de la tabla) ---
        if (blank(txtNombre.getText())) { warn("Validación", "El nombre es obligatorio."); return; }
        if (cmbEstado.getValue() == null) { warn("Validación", "Seleccione un estado."); return; }

        if (blank(txtPatrocinador.getText())) { warn("Validación", "El nombre del patrocinador es obligatorio."); return; }
        if (!emailOk(txtCorreoPatrocinador.getText())) { warn("Validación", "El correo del patrocinador es obligatorio y debe ser válido."); return; }

        if (blank(txtLiderUsuario.getText())) { warn("Validación", "El nombre del líder de usuario es obligatorio."); return; }
        if (!emailOk(txtCorreoLiderUsuario.getText())) { warn("Validación", "El correo del líder de usuario es obligatorio y debe ser válido."); return; }

        if (blank(txtLiderTecnico.getText())) { warn("Validación", "El nombre del líder técnico es obligatorio."); return; }
        if (!emailOk(txtCorreoLiderTecnico.getText())) { warn("Validación", "El correo del líder técnico es obligatorio y debe ser válido."); return; }

        if (dpInicioPlan.getValue() == null || dpFinPlan.getValue() == null) {
            warn("Validación", "Las fechas planificadas (inicio y fin) son obligatorias."); return;
        }
        if (dpFinPlan.getValue().isBefore(dpInicioPlan.getValue())) {
            warn("Validación", "La fecha fin planificada no puede ser menor que la fecha inicio planificada."); return;
        }

        // --- UI -> DTO ---
        modelo.setNombre(txtNombre.getText().trim());
        modelo.setEstado(cmbEstado.getValue());

        modelo.setPatrocinadorNombre(nv(txtPatrocinador.getText()).trim());
        modelo.setPatrocinadorCorreo(nv(txtCorreoPatrocinador.getText()).trim());

        modelo.setLiderUsuarioNombre(nv(txtLiderUsuario.getText()).trim());
        modelo.setLiderUsuarioCorreo(nv(txtCorreoLiderUsuario.getText()).trim());

        modelo.setLiderTecnicoNombre(nv(txtLiderTecnico.getText()).trim());
        modelo.setLiderTecnicoCorreo(nv(txtCorreoLiderTecnico.getText()).trim());

        modelo.setFechaInicioPlanificada(toXml(dpInicioPlan.getValue()));
        modelo.setFechaFinalPlanificada(toXml(dpFinPlan.getValue()));
        modelo.setFechaInicioReal(toXml(dpInicioReal.getValue()));
        modelo.setFechaFinalReal(toXml(dpFinReal.getValue()));

        // --- Guardar vía WS ---
        Long idAdministrador = 1L; // TODO: reemplazar por el id real de sesión si corresponde

        RespuestaGeneral r = (modelo.getId() == null)
                ? port.crearProyecto(modelo, idAdministrador)
                : port.actualizarProyecto(modelo);

        if (r == null || !Boolean.TRUE.equals(r.isOk())) {
            error("Error", (r == null) ? "Sin respuesta del servidor" : nv(r.getMensaje()));
            return;
        }

        guardado = true;
        cerrar();
    }

    @FXML
    private void onCancelar() {
        guardado = false;
        cerrar();
    }

    private void cerrar() {
        Stage st = (Stage) txtNombre.getScene().getWindow();
        st.close();
    }

    public boolean isGuardado() { return guardado; }

    // ===== Alerts =====
    private void warn(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText(titulo);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void error(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(titulo);
        a.setContentText(msg);
        a.showAndWait();
    }
}
