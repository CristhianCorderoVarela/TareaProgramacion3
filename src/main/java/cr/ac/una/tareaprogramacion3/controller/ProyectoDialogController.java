package cr.ac.una.tareaprogramacion3.controller;

import cr.ac.una.client.soap.ProyectoDto;
import cr.ac.una.client.soap.ProyectoService;
import cr.ac.una.client.soap.ProyectoWS;
import cr.ac.una.client.soap.RespuestaGeneral;
import cr.ac.una.client.soap.SeguimientoService;
import cr.ac.una.client.soap.SeguimientoWS;
import jakarta.xml.ws.BindingProvider;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.stream.Collectors;

public class ProyectoDialogController extends cr.ac.una.tareaprogramacion3.util.Controller {

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
    private ProyectoWS proyPort;
    private SeguimientoWS segPort;
    private ProyectoDto modelo;
    private boolean guardado = false;

    // Flag: hay seguimientos -> “PLANIFICADO” visible pero deshabilitado
    private boolean bloquearPlanificado = false;
    private String ultimoEstadoValido = null;

    // Estados posibles
    private static final List<String> ESTADOS_ALL = Arrays.asList(
            "PLANIFICADO", "EN_CURSO", "SUSPENDIDO", "FINALIZADO"
    );

    @Override
    public void initialize() {
        // no-op
    }

    /** Llamado por Ventana1Controller antes de abrir el modal */
    public void init(String proyectoEndpointUrl, ProyectoDto dto) {
        // ====== Configurar WS Proyecto ======
        ProyectoService svc = new ProyectoService();
        this.proyPort = svc.getProyectoWSPort();
        Map<String, Object> ctx = ((BindingProvider) proyPort).getRequestContext();
        ctx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, proyectoEndpointUrl);

        // ====== Configurar WS Seguimiento (derivado del endpoint de proyecto) ======
        String segEndpoint = proyectoEndpointUrl
                .replace("ProyectoService/ProyectoWS", "SeguimientoService/SeguimientoWS");
        SeguimientoService segSvc = new SeguimientoService();
        this.segPort = segSvc.getSeguimientoWSPort();
        Map<String, Object> ctx2 = ((BindingProvider) segPort).getRequestContext();
        ctx2.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, segEndpoint);

        // ====== DTO (nuevo o edición) ======
        this.modelo = (dto != null) ? dto : new ProyectoDto();

        txtNombre.setText(nv(modelo.getNombre()));

        // Hay seguimientos?
        bloquearPlanificado = (modelo.getId() != null && contarSeguimientos(modelo.getId()) > 0);

        // Cargar estados SIEMPRE mostrando PLANIFICADO
        cmbEstado.getItems().setAll(ESTADOS_ALL);

        // CellFactory: deshabilitar visualmente PLANIFICADO si corresponde
        cmbEstado.setCellFactory(listView -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setDisable(false);
                    setMouseTransparent(false);
                    setStyle(null);
                } else {
                    setText(item);
                    boolean disable = bloquearPlanificado && "PLANIFICADO".equals(item);
                    setDisable(disable);
                    // evita clicks sobre el item deshabilitado
                    setMouseTransparent(disable);
                    // atenuar visualmente
                    setStyle(disable ? "-fx-opacity: 0.55;" : null);
                    if (disable) {
                        setTooltip(new Tooltip("No disponible: el proyecto ya tiene seguimientos."));
                    } else {
                        setTooltip(null);
                    }
                }
            }
        });

        // ButtonCell coherente con el estado seleccionado
        cmbEstado.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
            }
        });

        // Selección inicial
        String estadoInicial = nv(modelo.getEstado()).isEmpty() ? "PLANIFICADO" : modelo.getEstado();
        if (bloquearPlanificado && "PLANIFICADO".equals(estadoInicial)) {
            // Si por datos viene en PLANIFICADO pero ya tiene seguimientos,
            // muévelo a un estado permitido (EN_CURSO por defecto).
            estadoInicial = "EN_CURSO";
        }
        cmbEstado.getSelectionModel().select(estadoInicial);
        ultimoEstadoValido = estadoInicial;

        // Listener para impedir elegir PLANIFICADO cuando está bloqueado
        cmbEstado.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (bloquearPlanificado && "PLANIFICADO".equals(newVal)) {
                // revertir y avisar
                runLaterSafe(() -> {
                    cmbEstado.getSelectionModel().select(ultimoEstadoValido);
                    warn("No permitido",
                            "Este proyecto ya tiene seguimientos registrados.\n" +
                            "No puede volver al estado PLANIFICADO.");
                });
            } else {
                ultimoEstadoValido = newVal;
            }
        });

        // Resto de campos
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

    private void runLaterSafe(Runnable r) {
        try { javafx.application.Platform.runLater(r); } catch (Exception ignore) {}
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
        int at = s.indexOf('@');
        return at > 0 && at < s.length() - 1 && s.indexOf(' ') < 0;
    }

    // ===== Acciones =====
    
@FXML
private void onGuardar() {
    LocalDate hoy = LocalDate.now();

    // --- Validaciones mínimas ---
    if (blank(txtNombre.getText())) { warn("Validación", "El nombre es obligatorio."); return; }
    if (cmbEstado.getValue() == null) { warn("Validación", "Seleccione un estado."); return; }

    if (blank(txtPatrocinador.getText())) { warn("Validación", "El nombre del patrocinador es obligatorio."); return; }
    if (!emailOk(txtCorreoPatrocinador.getText())) { warn("Validación", "El correo del patrocinador es obligatorio y debe ser válido."); return; }

    if (blank(txtLiderUsuario.getText())) { warn("Validación", "El nombre del líder de usuario es obligatorio."); return; }
    if (!emailOk(txtCorreoLiderUsuario.getText())) { warn("Validación", "El correo del líder de usuario es obligatorio y debe ser válido."); return; }

    if (blank(txtLiderTecnico.getText())) { warn("Validación", "El nombre del líder técnico es obligatorio."); return; }
    if (!emailOk(txtCorreoLiderTecnico.getText())) { warn("Validación", "El correo del líder técnico es obligatorio y debe ser válido."); return; }

    // ===== Fechas planificadas (OBLIGATORIAS) =====
    if (dpInicioPlan.getValue() == null || dpFinPlan.getValue() == null) {
        warn("Validación", "Las fechas planificadas (inicio y fin) son obligatorias."); return;
    }
    if (dpInicioPlan.getValue().isBefore(hoy)) {
        warn("Validación", "La fecha de inicio planificada no puede ser anterior a hoy."); return;
    }
    if (dpFinPlan.getValue().isBefore(dpInicioPlan.getValue())) {
        warn("Validación", "La fecha fin planificada no puede ser menor que la fecha inicio planificada."); return;
    }

    // ===== Fechas reales (opcionales) =====
    LocalDate iniReal = dpInicioReal.getValue();
    LocalDate finReal = dpFinReal.getValue();

    if (iniReal != null && iniReal.isBefore(hoy)) {
        warn("Validación", "La fecha de inicio real no puede ser anterior a hoy."); return;
    }
    if (iniReal == null && finReal != null) {
        warn("Validación", "Ingrese la fecha de inicio real antes de la fecha de fin real."); return;
    }
    if (iniReal != null && finReal != null) {
        if (finReal.isBefore(iniReal)) {
            warn("Validación", "La fecha fin real no puede ser menor que la fecha inicio real."); return;
        }
        if (finReal.isBefore(hoy)) {
            warn("Validación", "La fecha fin real no puede ser anterior a hoy."); return;
        }
    }

    // ===== Estado =====
    String estadoElegido = cmbEstado.getValue();
    if (bloquearPlanificado && "PLANIFICADO".equals(estadoElegido)) {
        warn("No permitido",
                "Este proyecto ya tiene seguimientos registrados.\n" +
                "No puede guardar en estado PLANIFICADO.");
        return;
    }

    // --- UI -> DTO ---
    modelo.setNombre(txtNombre.getText().trim());
    modelo.setEstado(estadoElegido);

    modelo.setPatrocinadorNombre(nv(txtPatrocinador.getText()).trim());
    modelo.setPatrocinadorCorreo(nv(txtCorreoPatrocinador.getText()).trim());

    modelo.setLiderUsuarioNombre(nv(txtLiderUsuario.getText()).trim());
    modelo.setLiderUsuarioCorreo(nv(txtCorreoLiderUsuario.getText()).trim());

    modelo.setLiderTecnicoNombre(nv(txtLiderTecnico.getText()).trim());
    modelo.setLiderTecnicoCorreo(nv(txtCorreoLiderTecnico.getText()).trim());

    modelo.setFechaInicioPlanificada(toXml(dpInicioPlan.getValue()));
    modelo.setFechaFinalPlanificada(toXml(dpFinPlan.getValue()));
    modelo.setFechaInicioReal(toXml(iniReal));
    modelo.setFechaFinalReal(toXml(finReal));

    // --- Guardar vía WS ---
    Long idAdministrador = 1L; // TODO: reemplazar por el id real de sesión si corresponde

    RespuestaGeneral r = (modelo.getId() == null)
            ? proyPort.crearProyecto(modelo, idAdministrador)
            : proyPort.actualizarProyecto(modelo);

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

    // ====== Helpers: contar seguimientos de forma robusta ======

    /** Devuelve la cantidad de seguimientos para el proyecto. */
    private int contarSeguimientos(Long proyectoId) {
        try {
            Object resp = segPort.buscarSeguimientosPorProyecto(proyectoId);
            List<?> lista = extraerLista(resp);
            return (lista == null) ? 0 : lista.size();
        } catch (Exception ex) {
            System.out.println("[ProyectoDialog] No se pudo contar seguimientos: " + ex.getMessage());
            return 0;
        }
        }

    /** Intenta extraer una lista desde distintas formas comunes de respuesta JAX-WS. */
    @SuppressWarnings("unchecked")
    private List<?> extraerLista(Object resp) {
        if (resp == null) return Collections.emptyList();

        if (resp instanceof List<?> l) return l;

        if (resp.getClass().isArray()) {
            int n = Array.getLength(resp);
            List<Object> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) out.add(Array.get(resp, i));
            return out;
        }

        for (String getter : new String[]{
                "getData", "getDatos", "getLista", "getItems",
                "getProyectoOrSeguimientoOrActividad", "getSeguimientos"
        }) {
            Object data = tryGet(resp, getter);
            List<?> asList = aLista(data);
            if (asList != null) return asList;
        }

        return firstListLike(resp);
    }

    private Object tryGet(Object target, String getter) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(getter);
            return m.invoke(target);
        } catch (Exception ignored) { return null; }
    }

    @SuppressWarnings("unchecked")
    private List<?> aLista(Object data) {
        if (data == null) return null;

        if (data instanceof List<?> l) return l;

        if (data.getClass().isArray()) {
            int n = Array.getLength(data);
            List<Object> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) out.add(Array.get(data, i));
            return out;
        }

        Object items = tryGet(data, "getItem"); // muy común en wrappers JAX-WS
        if (items != null) return aLista(items);

        return null;
    }

    /** Devuelve la PRIMERA lista/array encontrada en cualquier getter sin argumentos del objeto. */
    @SuppressWarnings("unchecked")
    private List<?> firstListLike(Object src) {
        if (src == null) return null;
        for (Method m : src.getClass().getMethods()) {
            if (m.getParameterCount() != 0) continue;
            String n = m.getName();
            if (!(n.startsWith("get") || n.startsWith("is"))) continue;
            try {
                Object val = m.invoke(src);
                if (val == null) continue;
                if (val instanceof List<?>) return (List<?>) val;
                if (val.getClass().isArray()) return aLista(val);
                Object item = tryGet(val, "getItem");
                if (item != null) return aLista(item);
            } catch (Exception ignore) {}
        }
        return null;
    }
}