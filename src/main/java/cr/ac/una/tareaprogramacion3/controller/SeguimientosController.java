package cr.ac.una.tareaprogramacion3.controller;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import javax.xml.datatype.*;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

// WS
import cr.ac.una.client.soap.ProyectoDto;
import cr.ac.una.client.soap.ProyectoService;
import cr.ac.una.client.soap.ProyectoWS;
import cr.ac.una.client.soap.SeguimientoProyectoDto;
import cr.ac.una.client.soap.SeguimientoService;
import cr.ac.una.client.soap.SeguimientoWS;
import cr.ac.una.tareaprogramacion3.util.AppEvents;

// Base/Utils
import cr.ac.una.tareaprogramacion3.util.Controller;
import cr.ac.una.tareaprogramacion3.util.UserSession;
import jakarta.xml.ws.BindingProvider;

public class SeguimientosController extends Controller implements Initializable {

    // --- Top bar
    @FXML private ComboBox<ProyectoDto> cbProyectos;
    @FXML private Button btnCargarSeguimientos;

    // --- Tabla
    @FXML private TableView<SeguimientoProyectoDto> tablaSeguimientos;
    @FXML private TableColumn<SeguimientoProyectoDto, LocalDate> colFecha;
    @FXML private TableColumn<SeguimientoProyectoDto, Number> colPorcentaje;
    @FXML private TableColumn<SeguimientoProyectoDto, String> colObservaciones;
    @FXML private TableColumn<SeguimientoProyectoDto, String> colResponsable;

    // --- Formulario
    @FXML private DatePicker fechaSeguimiento;
    @FXML private Slider sliderPorcentaje;
    @FXML private Label lblPorcentaje;
    @FXML private TextArea txtObservaciones;
    @FXML private TextField txtResponsable;

    @FXML private Button btnGuardarSeguimiento;
    @FXML private Button btnEditarSeguimiento;
    @FXML private Button btnEliminarSeguimiento;
    @FXML private Button btnLimpiarSeguimiento;

    // % vigente (último seguimiento o % del proyecto); gobierna “no decrecer”
    private int ultimoPct = 0;

    // ===== Endpoints =====
    private <T> T withEndpoint(T port, String path) {
        String base = "http://localhost:8080";
        Map<String, Object> ctx = ((BindingProvider) port).getRequestContext();
        ctx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, base + path);
        return port;
    }
    private SeguimientoWS segPort() {
        return withEndpoint(new SeguimientoService().getSeguimientoWSPort(),
                "/SeguimientoService/SeguimientoWS");
    }
    private ProyectoWS proyPort() {
        return withEndpoint(new ProyectoService().getProyectoWSPort(),
                "/ProyectoService/ProyectoWS");
    }

    // ====== Inicialización JavaFX ======
    @Override
public void initialize(URL url, ResourceBundle rb) {
    configurarTabla();
    configurarBindings();
    prepararEventos();
    prepararResponsableDesdeSesion();
    prepararReglasFormulario();
    limpiarFormulario();
    cargarProyectos(); // carga inicial

    // Permite que otra ventana te diga “cargá X proyecto y mostrámelo”
    AppEvents.onIrASeguimientos(proyectoId -> {
        cbProyectos.getItems().stream()
                .filter(p -> Objects.equals(p.getId(), proyectoId))
                .findFirst()
                .ifPresent(p -> {
                    cbProyectos.getSelectionModel().select(p);
                    cargarSeguimientos();
                });
    });

    // Limpiar selección y volver a "agregar nuevo" solo si hacen clic FUERA de la tabla y del formulario
    tablaSeguimientos.sceneProperty().addListener((obs, oldScene, scene) -> {
        if (scene == null) return;
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            // Si el clic es dentro de la tabla o de cualquiera de los controles del formulario, no limpiar
            if (isInside(tablaSeguimientos, e.getTarget())
                    || isInside(txtObservaciones, e.getTarget())
                    || isInside(sliderPorcentaje, e.getTarget())
                    || isInside(fechaSeguimiento, e.getTarget())
                    || isInside(txtResponsable, e.getTarget())
                    || isInside(btnGuardarSeguimiento, e.getTarget())
                    || isInside(btnEditarSeguimiento, e.getTarget())
                    || isInside(btnEliminarSeguimiento, e.getTarget())
                    || isInside(btnLimpiarSeguimiento, e.getTarget())) {
                return;
            }

            // Clic fuera -> limpiar selección y volver a modo "agregar nuevo"
            if (tablaSeguimientos.getSelectionModel().getSelectedItem() != null) {
                tablaSeguimientos.getSelectionModel().clearSelection();
                limpiarFormulario();
            }
        });
    });
}
    public void initialize() { /* no-op; compatible con Controller base */ }

    private void prepararResponsableDesdeSesion() {
        try {
            Long id = UserSession.get().getAdminId();
            String nombre = UserSession.get().getAdminNombre();
            if (id != null) {
                txtResponsable.setText(nombre != null ? nombre : "");
                txtResponsable.setEditable(false);
                txtResponsable.setDisable(true);
            }
        } catch (Exception ignored) {}
    }

    private void configurarTabla() {
        colFecha.setCellValueFactory(data -> {
            LocalDate ld = toLocalDate(data.getValue().getFechaSeguimiento());
            return new SimpleObjectProperty<>(ld);
        });

        colPorcentaje.setCellValueFactory(d ->
                new SimpleIntegerProperty(d.getValue().getPorcentajeAvance() == null ? 0 : d.getValue().getPorcentajeAvance()));
        colPorcentaje.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar bar = new ProgressBar(0);
            private final Label lbl = new Label();
            private final HBox box = new HBox(8, bar, lbl);
            { bar.setPrefWidth(120); }
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setGraphic(null);
                } else {
                    int pct = Math.max(0, Math.min(100, value.intValue()));
                    bar.setProgress(pct / 100.0);
                    lbl.setText(pct + "%");
                    setGraphic(box);
                }
            }
        });

        colObservaciones.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getObservaciones() == null ? "" : d.getValue().getObservaciones()));
        colResponsable.setCellValueFactory(d ->
                new SimpleStringProperty(d.getValue().getCreadoPorNombre() == null ? "" : d.getValue().getCreadoPorNombre()));

        // Orden visual por fecha DESC
        colFecha.setSortType(TableColumn.SortType.DESCENDING);
        tablaSeguimientos.getSortOrder().setAll(colFecha);
    }

    private void configurarBindings() {
        lblPorcentaje.textProperty().bind(sliderPorcentaje.valueProperty().asString("%.0f%%"));
        sliderPorcentaje.setBlockIncrement(5);
        sliderPorcentaje.setMajorTickUnit(25);
        sliderPorcentaje.setMinorTickCount(4);
        sliderPorcentaje.setShowTickLabels(true);
        sliderPorcentaje.setShowTickMarks(true);

        cbProyectos.setConverter(new StringConverter<>() {
            @Override public String toString(ProyectoDto p) { return p == null ? "" : p.getNombre(); }
            @Override public ProyectoDto fromString(String s) { return null; }
        });
        cbProyectos.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(ProyectoDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNombre());
            }
        });
        cbProyectos.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(ProyectoDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNombre());
            }
        });

        cbProyectos.getSelectionModel().selectedItemProperty().addListener((obs, oldP, newP) -> {
            if (newP != null) cargarSeguimientos(); // fija ultimoPct + slider
        });
    }

    private void prepararEventos() {
        btnCargarSeguimientos.setOnAction(e -> cargarSeguimientos());
        btnLimpiarSeguimiento.setOnAction(e -> limpiarFormulario());
        btnGuardarSeguimiento.setOnAction(e -> guardar());
        btnEditarSeguimiento.setOnAction(e -> editar());
        btnEliminarSeguimiento.setOnAction(e -> eliminar());
    }

    private static final int OBS_MAX = 500;

    private void prepararReglasFormulario() {
    fechaSeguimiento.setValue(LocalDate.now());
    fechaSeguimiento.setEditable(false);
    fechaSeguimiento.setDisable(true);
    fechaSeguimiento.setStyle("-fx-opacity: 1;");
    fechaSeguimiento.setMouseTransparent(true);

    txtObservaciones.setTextFormatter(new TextFormatter<String>(c -> {
        String next = c.getControlNewText();
        if (next != null && next.length() > OBS_MAX) return null;
        return c;
    }));

    // Pasos de 1, sin "snap" a marcas de 5
    sliderPorcentaje.setBlockIncrement(1);
    sliderPorcentaje.setMajorTickUnit(10);
    sliderPorcentaje.setMinorTickCount(9); // marcas cada 1
    sliderPorcentaje.setSnapToTicks(false); // evita saltos de 5
    sliderPorcentaje.setShowTickLabels(true);
    sliderPorcentaje.setShowTickMarks(true);
}
    
    /** Devuelve el porcentaje del seguimiento inmediatamente anterior
 *  en el orden de MAYOR A MENOR POR PORCENTAJE (empates: más reciente primero).
 *  Si no hay anterior, usa ultimoPct como base.
 */
private int pctAnteriorPorPorcentaje(SeguimientoProyectoDto sel) {
    if (sel == null) return ultimoPct;

    // Ordenar por % desc, empate por fecha desc
    List<SeguimientoProyectoDto> items = new ArrayList<>(tablaSeguimientos.getItems());
    items.sort(Comparator
            .comparingInt((SeguimientoProyectoDto s) -> s.getPorcentajeAvance() == null ? 0 : s.getPorcentajeAvance())
            .reversed()
            .thenComparing((SeguimientoProyectoDto s) -> toDate(s.getFechaSeguimiento()),
                    Comparator.nullsLast(Comparator.reverseOrder()))
    );

    // Ubicar sel por id; si no hay id, por (% , fecha)
    int idx = -1;
    for (int i = 0; i < items.size(); i++) {
        SeguimientoProyectoDto s = items.get(i);
        boolean same =
                (s.getId() != null && sel.getId() != null && Objects.equals(s.getId(), sel.getId()))
             || (Objects.equals(s.getPorcentajeAvance(), sel.getPorcentajeAvance())
                 && Objects.equals(toDate(s.getFechaSeguimiento()), toDate(sel.getFechaSeguimiento())));
        if (same) { idx = i; break; }
    }

    if (idx >= 0 && idx + 1 < items.size()) {
        Integer p = items.get(idx + 1).getPorcentajeAvance();
        return p == null ? ultimoPct : Math.max(0, Math.min(100, p));
    }
    return ultimoPct;
}

    // ====== Cargas ======
    private void cargarProyectos() {
        try {
            Object r = proyPort().obtenerTodosProyectos();
            debugIfEmpty("Proyectos", r);
            List<ProyectoDto> lista = castList(respListGeneric(r));
            cbProyectos.getItems().setAll(lista);
            if (!lista.isEmpty()) cbProyectos.getSelectionModel().selectFirst();
            else warn("No se recibieron proyectos del servicio.");
        } catch (Exception ex) {
            error("Error cargando proyectos", ex.getMessage());
        }
    }

    /** Listener reutilizable para selección en tabla */
    private final javafx.beans.value.ChangeListener<SeguimientoProyectoDto> tablaSelListener =
            (obs, oldSel, sel) -> {
                if (sel != null) {
                    txtObservaciones.setText(sel.getObservaciones() == null ? "" : sel.getObservaciones());
                    txtResponsable.setText(sel.getCreadoPorNombre() == null ? "" : sel.getCreadoPorNombre());
                } else {
                    txtObservaciones.clear();
                    String nombre = null;
                    try { nombre = UserSession.get().getAdminNombre(); } catch (Exception ignored) {}
                    txtResponsable.setText(nombre != null ? nombre : "");
                }
                actualizarEstadoFormularioSegunSeleccion();
            };

    /** Carga y deja todo ordenado DESC por fecha, ajusta slider y botones. */
    private void cargarSeguimientos() {
        ProyectoDto p = cbProyectos.getSelectionModel().getSelectedItem();
        if (p == null || p.getId() == null) { warn("Seleccione un proyecto."); return; }

        try {
            Object r = segPort().buscarSeguimientosPorProyecto(p.getId());
            List<SeguimientoProyectoDto> lista =
                    SeguimientosController.<SeguimientoProyectoDto>respListOf(r, SeguimientoProyectoDto.class);

            // Ordenar DESC por fecha
            lista = ordenarDescPorFecha(lista);

            // Cargar en tabla y ordenar por columna fecha DESC
            tablaSeguimientos.getItems().setAll(lista);
            colFecha.setSortable(true);
            colFecha.setSortType(TableColumn.SortType.DESCENDING);
            tablaSeguimientos.getSortOrder().setAll(colFecha);
            tablaSeguimientos.sort();

            // % base: max(% proyecto, % del último seguimiento)
            int base = p.getPorcentajeAvance() == null ? 0 : p.getPorcentajeAvance();
            if (!lista.isEmpty()) {
                Integer ult = lista.get(0).getPorcentajeAvance();
                if (ult != null) base = Math.max(base, ult);
            }
            ultimoPct = base;
            sliderPorcentaje.setMin(base);
            sliderPorcentaje.setValue(base);

            fechaSeguimiento.setValue(LocalDate.now());

            // Registrar (o re-registrar) el listener de selección una sola vez
            tablaSeguimientos.getSelectionModel().selectedItemProperty().removeListener(tablaSelListener);
            tablaSeguimientos.getSelectionModel().selectedItemProperty().addListener(tablaSelListener);

            // Estado del formulario según selección actual
            actualizarEstadoFormularioSegunSeleccion();

        } catch (Exception ex) {
            error("Error consultando seguimientos", ex.getMessage());
        }
    }

    // ====== Reglas de UI segun selección ======
    /** Reglas:
     *  - Sin selección: Guardar habilitado, Editar/Eliminar deshabilitados, slider activo con min=ultimoPct.
     *  - Selección NO mayor %: Guardar/Editar deshabilitados, slider deshabilitado.
     *  - Selección ES mayor %: Guardar deshabilitado, Editar habilitado, slider activo con min=porcentaje del anterior por FECHA.
     */
    /** Reglas para edición:
 *  - Sin selección: se puede AGREGAR (Guardar habilitado), slider desde ultimoPct, observación editable.
 *  - Con selección:
 *      * Solo si es el ÚLTIMO por FECHA (el más reciente) se puede EDITAR:
 *          - Editar habilitado, Guardar deshabilitado.
 *          - Slider habilitado con mínimo = porcentaje del seguimiento anterior por FECHA.
 *          - Observación editable.
 *      * Cualquier otro registro queda solo lectura (sin editar, slider deshabilitado, observación no editable).
 */
private void actualizarEstadoFormularioSegunSeleccion() {
    SeguimientoProyectoDto sel = tablaSeguimientos.getSelectionModel().getSelectedItem();
    boolean hayItems = !tablaSeguimientos.getItems().isEmpty();
    boolean mayorPct = esMayorPorcentajeSeleccionado();

    if (sel == null) {
        // modo "agregar nuevo"
        btnGuardarSeguimiento.setDisable(false);
        btnEditarSeguimiento.setDisable(true);
        btnEliminarSeguimiento.setDisable(true);
        sliderPorcentaje.setDisable(false);
        sliderPorcentaje.setMin(ultimoPct);
        sliderPorcentaje.setValue(Math.max(sliderPorcentaje.getValue(), ultimoPct));
    } else {
        if (mayorPct) {
            // Solo el de mayor % es editable; mínimo = penúltimo por porcentaje
            btnGuardarSeguimiento.setDisable(true);
            btnEditarSeguimiento.setDisable(false);
            btnEliminarSeguimiento.setDisable(!(hayItems));
            sliderPorcentaje.setDisable(false);

            int min = pctAnteriorPorPorcentaje(sel);
            sliderPorcentaje.setMin(min);
            int val = sel.getPorcentajeAvance() == null ? min : sel.getPorcentajeAvance();
            sliderPorcentaje.setValue(Math.max(val, min));
        } else {
            // No mayor %: no se puede editar ni mover slider
            btnGuardarSeguimiento.setDisable(true);
            btnEditarSeguimiento.setDisable(true);
            btnEliminarSeguimiento.setDisable(true);
            sliderPorcentaje.setDisable(true);
            int val = sel.getPorcentajeAvance() == null ? ultimoPct : sel.getPorcentajeAvance();
            sliderPorcentaje.setMin(0);
            sliderPorcentaje.setValue(val);
        }
    }
}

    // ====== Acciones ======
    private void guardar() {
    ProyectoDto pSel = cbProyectos.getSelectionModel().getSelectedItem();
    if (pSel == null || pSel.getId() == null) { warn("Seleccione un proyecto."); return; }

    // 0) Refrescar estado real del proyecto desde el WS (evita decidir con datos viejos)
    ProyectoDto p = respData(proyPort().buscarProyectoPorId(pSel.getId()), ProyectoDto.class);
    if (p == null) p = pSel; // fallback
    String estadoActual = (p.getEstado() == null) ? "" : p.getEstado().trim().toUpperCase();

    // 1) Si el proyecto YA está finalizado -> NO permitir agregar seguimiento
    if ("FINALIZADO".equals(estadoActual)) {
        warn("Este proyecto ya está FINALIZADO. No es posible agregar nuevos seguimientos.");
        return;
    }

    // (opcional) Regla de negocio previa que ya tenías: debe haber al menos 1 actividad
    if (!proyectoTieneActividades(p.getId())) {
        warn("Antes de registrar un seguimiento, debe existir al menos una actividad en el proyecto.");
        return;
    }

    // 2) Lectura/validación de formulario
    int pct = (int) Math.round(sliderPorcentaje.getValue());
    // no permitir retroceder respecto al % vigente
    pct = Math.max(pct, ultimoPct);
    if (pct < 0 || pct > 100) { warn("El porcentaje debe estar entre 0 y 100."); return; }

    String obs = txtObservaciones.getText() == null ? "" : txtObservaciones.getText().trim();
    if (obs.isEmpty()) { warn("Las observaciones son obligatorias."); return; }
    if (obs.length() > OBS_MAX) { warn("Las observaciones no deben exceder " + OBS_MAX + " caracteres."); return; }

    Long adminId = UserSession.get().getAdminId();
    if (adminId == null) { warn("No hay usuario autenticado en sesión."); return; }

    // 3) Construir DTO de seguimiento
    SeguimientoProyectoDto dto = new SeguimientoProyectoDto();
    dto.setProyectoId(p.getId());
    dto.setFechaSeguimiento(toXmlCal(LocalDate.now()));
    dto.setPorcentajeAvance(pct);
    dto.setObservaciones(obs);
    dto.setCreadoPorId(adminId);

    try {
        // 4) Crear seguimiento
        Object res = segPort().crearSeguimiento(dto);
        if (!respOk(res)) {
            String m = respMsg(res);
            warn(m != null ? m : "No se pudo guardar.");
            return;
        }

        // 5) Ajustar % y estado del proyecto si corresponde
        int pctProyectoActual = (p.getPorcentajeAvance() == null) ? 0 : p.getPorcentajeAvance();
        int nuevoPctProyecto   = Math.max(pctProyectoActual, pct);

        boolean debeFinalizar = (pct >= 100) || (nuevoPctProyecto >= 100);
        if (debeFinalizar) {
            nuevoPctProyecto = 100;
            p.setEstado("FINALIZADO");
        }

        // Sólo llamar al WS si hay cambios
        if (!Objects.equals(p.getPorcentajeAvance(), nuevoPctProyecto) || debeFinalizar) {
            p.setPorcentajeAvance(nuevoPctProyecto);
            Object upd = proyPort().actualizarProyecto(p);
            if (!respOk(upd)) {
                warn("El seguimiento se guardó, pero no se pudo actualizar el proyecto.");
            } else if (debeFinalizar) {
                info("Seguimiento guardado y proyecto marcado como FINALIZADO (100%).");
            }
            // Notificar a otras ventanas/listeners
            AppEvents.fireProyectoActualizado(p.getId());
        } else {
            info("Seguimiento guardado.");
        }

        // 6) Dejar UI coherente con el nuevo % del proyecto
        ultimoPct = (p.getPorcentajeAvance() == null) ? nuevoPctProyecto : p.getPorcentajeAvance();
        sliderPorcentaje.setMin(ultimoPct);
        sliderPorcentaje.setValue(ultimoPct);

        // Recargar tabla y limpiar form
        cargarSeguimientos();
        ordenarTablaDescPorFecha();
        limpiarFormulario();

    } catch (Exception ex) {
        error("Error al guardar", ex.getMessage());
    }
}

    private void editar() {
    ProyectoDto pSel = cbProyectos.getSelectionModel().getSelectedItem();
    SeguimientoProyectoDto sel = tablaSeguimientos.getSelectionModel().getSelectedItem();

    if (pSel == null || pSel.getId() == null) { warn("Seleccione un proyecto."); return; }
    if (sel == null) { warn("Seleccione un seguimiento de la tabla."); return; }

    // Refrescar estado actual del proyecto desde WS
    ProyectoDto p = respData(proyPort().buscarProyectoPorId(pSel.getId()), ProyectoDto.class);
    if (p == null) p = pSel; // fallback
    String estadoActual = (p.getEstado() == null) ? "" : p.getEstado().trim().toUpperCase();

    // Si ya está finalizado, no permitir edición
    if ("FINALIZADO".equals(estadoActual)) {
        warn("Este proyecto ya está FINALIZADO. No se pueden editar seguimientos.");
        return;
    }

    // Solo permitir editar el de MAYOR porcentaje
    if (!esMayorPorcentajeSeleccionado()) {
        warn("Solo se puede editar el seguimiento con mayor porcentaje.");
        return;
    }

    // Calcular mínimo válido (penúltimo por porcentaje)
    int minEdicion = pctAnteriorPorPorcentaje(sel);
    int pct = (int) Math.round(sliderPorcentaje.getValue());
    pct = Math.max(pct, minEdicion); // no decrecer por debajo del anterior
    if (pct < 0 || pct > 100) { warn("El porcentaje debe estar entre 0 y 100."); return; }

    String obs = txtObservaciones.getText() == null ? "" : txtObservaciones.getText().trim();
    if (obs.isEmpty()) { warn("Las observaciones son obligatorias."); return; }
    if (obs.length() > OBS_MAX) { warn("Las observaciones no deben exceder " + OBS_MAX + " caracteres."); return; }

    // Actualizar DTO de seguimiento
    sel.setPorcentajeAvance(pct);
    sel.setObservaciones(obs);

    try {
        Object res = segPort().actualizarSeguimiento(sel);
        if (!respOk(res)) {
            String m = respMsg(res);
            warn(m != null ? m : "No se pudo actualizar.");
            return;
        }

        // Ajustar porcentaje y estado del proyecto
        int currentPct = (p.getPorcentajeAvance() == null) ? 0 : p.getPorcentajeAvance();
        int nuevoPct   = Math.max(currentPct, pct);

        boolean debeFinalizar = (pct >= 100) || (nuevoPct >= 100);
        if (debeFinalizar) {
            nuevoPct = 100;
            p.setEstado("FINALIZADO");
        }

        if (!Objects.equals(p.getPorcentajeAvance(), nuevoPct) || debeFinalizar) {
            p.setPorcentajeAvance(nuevoPct);
            Object upd = proyPort().actualizarProyecto(p);
            if (!respOk(upd)) {
                warn("El seguimiento se actualizó, pero no se pudo actualizar el proyecto.");
            } else if (debeFinalizar) {
                info("Seguimiento actualizado y proyecto marcado como FINALIZADO (100%).");
            }
            AppEvents.fireProyectoActualizado(p.getId());
        } else {
            info("Seguimiento actualizado.");
        }

        // Ajustar UI
        ultimoPct = nuevoPct;
        sliderPorcentaje.setMin(nuevoPct);
        sliderPorcentaje.setValue(nuevoPct);

        // Recargar tabla
        cargarSeguimientos();

    } catch (Exception ex) {
        error("Error al actualizar", ex.getMessage());
    }
}

    /** Tras eliminar un seguimiento, recalcula el % del proyecto */
    private void actualizarProyectoTrasEliminarSeguimiento(Long proyectoId) {
        try {
            // 1) % por seguimiento remanente de MAYOR porcentaje
            Object rSeg = segPort().buscarSeguimientosPorProyecto(proyectoId);
            List<SeguimientoProyectoDto> segs = respListOf(rSeg, SeguimientoProyectoDto.class);
            int pSeg = -1;
            if (segs != null && !segs.isEmpty()) {
                pSeg = segs.stream()
                        .map(s -> s.getPorcentajeAvance() == null ? 0 : s.getPorcentajeAvance())
                        .max(Integer::compareTo)
                        .orElse(0);
            }

            // 2) % por actividades actuales (vía ProyectoWS)
            Object rAct = proyPort().obtenerActividadesPorProyecto(proyectoId);
            List<cr.ac.una.client.soap.ActividadDto> acts =
                    respListOf(rAct, cr.ac.una.client.soap.ActividadDto.class);
            int pAct = 0;
            if (acts != null && !acts.isEmpty()) {
                long fin = acts.stream()
                        .filter(a -> "FINALIZADA".equalsIgnoreCase(String.valueOf(a.getEstado())))
                        .count();
                pAct = (int) Math.floor((fin * 100.0) / acts.size());
            }

            // 3) Nuevo % (max del mayor seguimiento remanente vs % por actividades; si no hay seguimientos, solo pAct)
            int nuevoPct = (pSeg >= 0) ? Math.max(pSeg, pAct) : pAct;
            nuevoPct = Math.max(0, Math.min(100, nuevoPct));

            // 4) Estado coherente
            String nuevoEstado = "PLANIFICADO";
            if (acts != null && !acts.isEmpty()) {
                boolean anyEnCurso = acts.stream().anyMatch(a -> "EN_CURSO".equalsIgnoreCase(String.valueOf(a.getEstado())));
                boolean anyPost = acts.stream().anyMatch(a -> "POSTERGADA".equalsIgnoreCase(String.valueOf(a.getEstado())));
                boolean allFin = acts.stream().allMatch(a -> "FINALIZADA".equalsIgnoreCase(String.valueOf(a.getEstado())));
                if (allFin || nuevoPct >= 100) nuevoEstado = "FINALIZADO";
                else if (anyEnCurso) nuevoEstado = "EN_CURSO";
                else if (anyPost) nuevoEstado = "SUSPENDIDO";
            }

            // 5) Actualizar el proyecto (usar DTO completo)
            ProyectoDto p = respData(proyPort().buscarProyectoPorId(proyectoId), ProyectoDto.class);
            if (p == null) {
                p = cbProyectos.getSelectionModel().getSelectedItem();
                if (p == null) { warn("No se pudo obtener el proyecto para actualizar su porcentaje."); return; }
            }

            Integer actualPct = p.getPorcentajeAvance() == null ? 0 : p.getPorcentajeAvance();
            String  actualEst = p.getEstado();
            if (Objects.equals(actualPct, nuevoPct) && Objects.equals(actualEst, nuevoEstado)) {
                ultimoPct = nuevoPct;
                sliderPorcentaje.setMin(nuevoPct);
                sliderPorcentaje.setValue(nuevoPct);
                AppEvents.fireProyectoActualizado(proyectoId);
                return;
            }

            p.setPorcentajeAvance(nuevoPct);
            p.setEstado(nuevoEstado);

            Object upd = proyPort().actualizarProyecto(p);
            if (!respOk(upd)) warn("No se pudo actualizar el proyecto con el nuevo porcentaje/estado.");

            ultimoPct = nuevoPct;
            sliderPorcentaje.setMin(nuevoPct);
            sliderPorcentaje.setValue(nuevoPct);
            AppEvents.fireProyectoActualizado(proyectoId);

        } catch (Exception ex) {
            warn("No se pudo ajustar el proyecto tras eliminar el seguimiento: " + ex.getMessage());
        }
    }

    private void eliminar() {
        SeguimientoProyectoDto sel = tablaSeguimientos.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getId() == null) { warn("Seleccione un seguimiento."); return; }

        // Regla: solo se puede eliminar el de mayor %
        if (!esMayorPorcentajeSeleccionado()) {
            warn("Solo se puede eliminar el seguimiento con mayor porcentaje.");
            return;
        }

        if (new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar seguimiento seleccionado?")
                .showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        ProyectoDto pSel = cbProyectos.getSelectionModel().getSelectedItem();

        try {
            Object res = segPort().eliminarSeguimiento(sel.getId());
            if (respOk(res)) {
                info("Seguimiento eliminado.");

                if (pSel != null && pSel.getId() != null) {
                    // Recalcular % del proyecto después de eliminar (puede bajar)
                    actualizarProyectoTrasEliminarSeguimiento(pSel.getId());
                    // Notificar a otras ventanas
                    AppEvents.fireProyectoActualizado(pSel.getId());
                }

                // Recargar tabla y limpiar form
                cargarSeguimientos();
                limpiarFormulario();

            } else {
                String m = respMsg(res);
                warn(m != null ? m : "No se pudo eliminar.");
            }
        } catch (Exception ex) {
            error("Error al eliminar", ex.getMessage());
        }
    }

    private void limpiarFormulario() {
        // Limpiar textos
        txtObservaciones.clear();
        String nombre = null;
        try { nombre = UserSession.get().getAdminNombre(); } catch (Exception ignored) {}
        txtResponsable.setText(nombre != null ? nombre : "");

        // Quitar selección de la tabla
        tablaSeguimientos.getSelectionModel().clearSelection();

        // Slider en modo "agregar nuevo"
        sliderPorcentaje.setDisable(false);
        sliderPorcentaje.setMin(ultimoPct);
        sliderPorcentaje.setValue(ultimoPct);
        fechaSeguimiento.setValue(LocalDate.now());

        // Botones en modo "agregar"
        btnGuardarSeguimiento.setDisable(false);
        btnEditarSeguimiento.setDisable(true);
        btnEliminarSeguimiento.setDisable(true);
    }

    // ====== Helpers de respuesta ======
    private static boolean respOk(Object resp) {
        if (resp == null) return false;
        Boolean ok = asBool(callNoArg(resp, "isOk", "getOk", "getExito"));
        if (ok != null) return ok;
        String estado = asStr(callNoArg(resp, "getEstado", "getStatus"));
        return estado != null && (estado.equalsIgnoreCase("OK") || estado.equalsIgnoreCase("SUCCESS"));
    }
    private static String respMsg(Object resp) {
        Object m = callNoArg(resp, "getMensaje", "getMessage", "getDetalle");
        return m == null ? null : String.valueOf(m);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> respListGeneric(Object resp) {
        Object data = callNoArg(resp,
                "getData", "getDatos", "getLista", "getItems", "getProyectos", "getSeguimientos");
        if (data == null) return Collections.emptyList();
        if (data instanceof List) return (List<T>) data;
        if (data.getClass().isArray()) {
            int n = Array.getLength(data);
            List<T> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) out.add((T) Array.get(data, i));
            return out;
        }
        Object item = callNoArg(data, "getItem");
        if (item instanceof List) return (List<T>) item;
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> respListOf(Object resp, Class<T> type) {
        if (resp == null) return Collections.emptyList();
        Object data = callNoArg(resp,
                "getData", "getDatos", "getLista", "getItems",
                "getProyectoOrSeguimientoOrActividad");
        List<T> out = new ArrayList<>();
        if (data instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof jakarta.xml.bind.JAXBElement<?> j) o = j.getValue();
                if (o != null && type.isInstance(o)) out.add(type.cast(o));
            }
        } else if (data != null && data.getClass().isArray()) {
            int n = Array.getLength(data);
            for (int i = 0; i < n; i++) {
                Object o = Array.get(data, i);
                if (o instanceof jakarta.xml.bind.JAXBElement<?> j) o = j.getValue();
                if (o != null && type.isInstance(o)) out.add(type.cast(o));
            }
        }
        return out;
    }

    private static Object callNoArg(Object target, String... names) {
        if (target == null) return null;
        for (String name : names) {
            try {
                Method m = target.getClass().getMethod(name);
                m.setAccessible(true);
                return m.invoke(target);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static Boolean asBool(Object o) { return (o instanceof Boolean) ? (Boolean)o : null; }
    private static String asStr(Object o) { return o == null ? null : String.valueOf(o); }

    @SuppressWarnings("unchecked")
    private static <T> List<T> castList(List<?> in) {
        return in == null ? Collections.emptyList() : (List<T>) in;
    }

    // ====== Fechas ======
    private static XMLGregorianCalendar toXmlCal(Date d) {
        if (d == null) return null;
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(d);
        try { return DatatypeFactory.newInstance().newXMLGregorianCalendar(gc); }
        catch (DatatypeConfigurationException e) { throw new RuntimeException(e); }
    }
    private static XMLGregorianCalendar toXmlCal(LocalDate ld) {
        if (ld == null) return null;
        Date d = Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
        return toXmlCal(d);
    }
    private static Date toDate(XMLGregorianCalendar x) {
        return x == null ? null : x.toGregorianCalendar().getTime();
    }
    private static LocalDate toLocalDate(XMLGregorianCalendar x) {
        Date d = toDate(x);
        if (d == null) return null;
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    // ====== Debug ======
    private void debugIfEmpty(String quien, Object resp) {
        try {
            List<?> l = respListGeneric(resp);
            Boolean ok = respOk(resp);
            System.out.println("[Ventana4] " + quien + " -> resp=" +
                    (resp != null ? resp.getClass().getName() : "null") +
                    " ok=" + ok + " size=" + (l != null ? l.size() : -1));
            if (l == null || l.isEmpty()) {
                if (resp != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Clase: ").append(resp.getClass().getName()).append("\nGetters:\n");
                    for (Method m : resp.getClass().getMethods()) {
                        if (m.getParameterCount() == 0 && (m.getName().startsWith("get") || m.getName().startsWith("is")))
                            sb.append("• ").append(m.getName()).append("\n");
                    }
                    System.out.println(sb);
                }
            }
        } catch (Exception ex) {
            System.out.println("[Ventana4] debugIfEmpty error: " + ex.getMessage());
        }
    }

    // ====== Helpers extra ======
    @SuppressWarnings("unchecked")
    private static <T> T respData(Object resp, Class<T> type) {
        if (resp == null) return null;
        Object d = callNoArg(resp, "getData", "getProyecto");
        if (d instanceof jakarta.xml.bind.JAXBElement<?> j) d = j.getValue();
        return (type.isInstance(d)) ? type.cast(d) : null;
    }

    /** True si el seleccionado es el seguimiento con MAYOR porcentaje.
     *  En caso de empate por %, se toma el más reciente.
     */
    private boolean esMayorPorcentajeSeleccionado() {
        SeguimientoProyectoDto sel = tablaSeguimientos.getSelectionModel().getSelectedItem();
        if (sel == null) return false;

        SeguimientoProyectoDto mayor = tablaSeguimientos.getItems().stream()
                .filter(Objects::nonNull)
                .max(Comparator
                        .comparingInt((SeguimientoProyectoDto s) ->
                                s.getPorcentajeAvance() == null ? 0 : s.getPorcentajeAvance())
                        .thenComparing(s -> toDate(s.getFechaSeguimiento()),
                                Comparator.nullsLast(Comparator.naturalOrder())) // más reciente gana empates
                )
                .orElse(null);

        if (mayor == null) return false;

        if (mayor.getId() != null && sel.getId() != null) {
            return Objects.equals(mayor.getId(), sel.getId());
        }
        // Fallback si no hay id:
        Integer pSel = sel.getPorcentajeAvance() == null ? 0 : sel.getPorcentajeAvance();
        Integer pMax = mayor.getPorcentajeAvance() == null ? 0 : mayor.getPorcentajeAvance();
        Date dSel = toDate(sel.getFechaSeguimiento());
        Date dMax = toDate(mayor.getFechaSeguimiento());
        return Objects.equals(pSel, pMax) && Objects.equals(dSel, dMax);
    }

    /** Devuelve nueva lista ordenada DESC por fecha (más reciente primero). */
    private List<SeguimientoProyectoDto> ordenarDescPorFecha(List<SeguimientoProyectoDto> lista) {
        if (lista == null) return Collections.emptyList();
        List<SeguimientoProyectoDto> out = new ArrayList<>(lista);
        out.sort(Comparator.comparing(
                (SeguimientoProyectoDto s) -> toDate(s.getFechaSeguimiento()),
                Comparator.nullsLast(Comparator.naturalOrder())
        ).reversed());
        return out;
    }

    /** Devuelve el porcentaje del seguimiento inmediatamente anterior por FECHA al seleccionado.
     *  Si no existe anterior, devuelve ultimoPct (base del proyecto/último registro).
     */
    private int pctAnteriorPorFecha(SeguimientoProyectoDto sel) {
        if (sel == null) return ultimoPct;
        List<SeguimientoProyectoDto> items = ordenarDescPorFecha(new ArrayList<>(tablaSeguimientos.getItems())); // más reciente primero
        int idx = items.indexOf(sel);
        if (idx == -1) {
            // Si equals() no coincide, buscar por id/fecha
            for (int i = 0; i < items.size(); i++) {
                SeguimientoProyectoDto s = items.get(i);
                if ((s.getId() != null && sel.getId() != null && Objects.equals(s.getId(), sel.getId())) ||
                    Objects.equals(toDate(s.getFechaSeguimiento()), toDate(sel.getFechaSeguimiento()))) {
                    idx = i; break;
                }
            }
        }
        if (idx >= 0 && idx + 1 < items.size()) {
            Integer p = items.get(idx + 1).getPorcentajeAvance();
            return p == null ? 0 : Math.max(0, Math.min(100, p));
        }
        return ultimoPct;
    }

    /** Reordenamiento visual redundante (por si hay cambios en caliente). */
    private void ordenarTablaDescPorFecha() {
        List<SeguimientoProyectoDto> items = new ArrayList<>(tablaSeguimientos.getItems());
        items.sort(Comparator.comparing(
                (SeguimientoProyectoDto s) -> toDate(s.getFechaSeguimiento()),
                Comparator.nullsLast(Comparator.naturalOrder())
        ).reversed());
        tablaSeguimientos.getItems().setAll(items);

        colFecha.setSortable(true);
        colFecha.setSortType(TableColumn.SortType.DESCENDING);
        tablaSeguimientos.getSortOrder().setAll(colFecha);
        tablaSeguimientos.sort();
    }

    /** Verifica si el proyecto tiene al menos una actividad. */
    private boolean proyectoTieneActividades(Long proyectoId) {
        try {
            Object rAct = proyPort().obtenerActividadesPorProyecto(proyectoId);
            List<?> acts = respListGeneric(rAct);
            return acts != null && !acts.isEmpty();
        } catch (Exception ex) {
            warn("No se pudieron consultar las actividades del proyecto.");
            return false;
        }
    }

    // ====== Click fuera de la tabla ======
    private boolean isInside(Node root, Object target) {
        if (!(target instanceof Node n)) return false;
        for (Node x = n; x != null; x = x.getParent()) {
            if (x == root) return true;
        }
        return false;
    }

    // ====== Alerts ======
    private void info(String m){ Alert a=new Alert(Alert.AlertType.INFORMATION,"",ButtonType.OK); a.setHeaderText(null); a.setContentText(m); a.showAndWait(); }
    private void warn(String m){ Alert a=new Alert(Alert.AlertType.WARNING,"",ButtonType.OK); a.setHeaderText(null); a.setContentText(m); a.showAndWait(); }
    private void error(String t,String d){ Alert a=new Alert(Alert.AlertType.ERROR,"",ButtonType.OK); a.setHeaderText(t); a.setContentText(d!=null?d:""); a.showAndWait(); }
}