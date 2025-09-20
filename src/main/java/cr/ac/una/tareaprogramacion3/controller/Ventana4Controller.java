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
import javafx.scene.control.*;
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

public class Ventana4Controller extends Controller implements Initializable {

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
    }
    public void initialize() { /* no-op */ }

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

        // NO registramos aquí un listener de selección para evitar duplicarlo en recargas
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

        sliderPorcentaje.setBlockIncrement(1);
        sliderPorcentaje.setMajorTickUnit(25);
        sliderPorcentaje.setMinorTickCount(4);
        sliderPorcentaje.setSnapToTicks(true);
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

    /** Listener reutilizable para selección en tabla, así evitamos duplicados. */
    private final javafx.beans.value.ChangeListener<SeguimientoProyectoDto> tablaSelListener =
            (obs, oldSel, sel) -> {
                if (sel != null) {
                    txtObservaciones.setText(sel.getObservaciones() == null ? "" : sel.getObservaciones());
                    txtResponsable.setText(sel.getCreadoPorNombre() == null ? "" : sel.getCreadoPorNombre());
                } else {
                    txtObservaciones.clear();
                    txtResponsable.clear();
                }
                actualizarBotonesSegunSeleccion();
            };

    /** Carga y deja todo ordenado DESC por fecha, ajusta slider y botones. */
    private void cargarSeguimientos() {
        ProyectoDto p = cbProyectos.getSelectionModel().getSelectedItem();
        if (p == null || p.getId() == null) { warn("Seleccione un proyecto."); return; }

        try {
            Object r = segPort().buscarSeguimientosPorProyecto(p.getId());
            List<SeguimientoProyectoDto> lista =
                    Ventana4Controller.<SeguimientoProyectoDto>respListOf(r, SeguimientoProyectoDto.class);

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

            // Actualizar estado de botones según selección actual
            actualizarBotonesSegunSeleccion();

        } catch (Exception ex) {
            error("Error consultando seguimientos", ex.getMessage());
        }
    }

    // ====== Acciones ======
    private void guardar() {
        ProyectoDto p = cbProyectos.getSelectionModel().getSelectedItem();
        if (p == null || p.getId() == null) { warn("Seleccione un proyecto."); return; }

        // NO permitir seguimiento si no hay actividades
        if (!proyectoTieneActividades(p.getId())) {
            warn("Antes de registrar un seguimiento, debe existir al menos una actividad en el proyecto.");
            return;
        }

        int pct = (int) Math.round(sliderPorcentaje.getValue());
        pct = Math.max(pct, ultimoPct); // no decrecer
        if (pct < 0 || pct > 100) { warn("El porcentaje debe estar entre 0 y 100."); return; }

        String obs = txtObservaciones.getText() == null ? "" : txtObservaciones.getText().trim();
        if (obs.isEmpty()) { warn("Las observaciones son obligatorias."); return; }
        if (obs.length() > OBS_MAX) { warn("Las observaciones no deben exceder " + OBS_MAX + " caracteres."); return; }

        Long adminId = UserSession.get().getAdminId();
        if (adminId == null) { warn("No hay usuario autenticado en sesión."); return; }

        SeguimientoProyectoDto dto = new SeguimientoProyectoDto();
        dto.setProyectoId(p.getId());
        dto.setFechaSeguimiento(toXmlCal(LocalDate.now()));
        dto.setPorcentajeAvance(pct);
        dto.setObservaciones(obs);
        dto.setCreadoPorId(adminId);

        try {
            Object res = segPort().crearSeguimiento(dto);
            if (respOk(res)) {
                int current = p.getPorcentajeAvance() == null ? 0 : p.getPorcentajeAvance();
                int nuevo   = Math.max(current, pct);
                if (pct >= 100) { nuevo = 100; try { p.setEstado("FINALIZADO"); } catch (Exception ignore) {} }
                if (nuevo != current || pct >= 100) {
                    p.setPorcentajeAvance(nuevo);
                    proyPort().actualizarProyecto(p);
                    AppEvents.fireProyectoActualizado(p.getId());

                    sliderPorcentaje.setMin(p.getPorcentajeAvance() == null ? 0 : p.getPorcentajeAvance());
                    sliderPorcentaje.setValue(p.getPorcentajeAvance() == null ? 0 : p.getPorcentajeAvance());
                    limpiarFormulario();
                }
                ultimoPct = nuevo;
                sliderPorcentaje.setMin(nuevo);
                sliderPorcentaje.setValue(nuevo);

                info("Seguimiento guardado.");
                cargarSeguimientos();
                ordenarTablaDescPorFecha(); // redundante pero asegura UI
                limpiarFormulario();
            } else {
                String m = respMsg(res);
                warn(m != null ? m : "No se pudo guardar.");
            }
        } catch (Exception ex) {
            error("Error al guardar", ex.getMessage());
        }
    }

    private void editar() {
        ProyectoDto p = cbProyectos.getSelectionModel().getSelectedItem();
        SeguimientoProyectoDto sel = tablaSeguimientos.getSelectionModel().getSelectedItem();
        if (p == null || p.getId() == null) { warn("Seleccione un proyecto."); return; }
        if (sel == null) { warn("Seleccione un seguimiento de la tabla."); return; }

        // Solo permitir editar el último (más reciente)
        if (!esUltimoSeleccionado()) {
            warn("Solo se puede editar el último seguimiento registrado.");
            return;
        }

        int pct = (int) Math.round(sliderPorcentaje.getValue());
        pct = Math.max(pct, ultimoPct); // no decrecer
        if (pct < 0 || pct > 100) { warn("El porcentaje debe estar entre 0 y 100."); return; }

        String obs = txtObservaciones.getText() == null ? "" : txtObservaciones.getText().trim();
        if (obs.isEmpty()) { warn("Las observaciones son obligatorias."); return; }
        if (obs.length() > OBS_MAX) { warn("Las observaciones no deben exceder " + OBS_MAX + " caracteres."); return; }

        sel.setPorcentajeAvance(pct);
        sel.setObservaciones(obs);

        try {
            Object res = segPort().actualizarSeguimiento(sel);
            if (respOk(res)) {
                int current = p.getPorcentajeAvance() == null ? 0 : p.getPorcentajeAvance();
                int nuevo   = Math.max(current, pct);
                if (pct >= 100) { nuevo = 100; try { p.setEstado("FINALIZADO"); } catch (Exception ignore) {} }

                if (nuevo != current || pct >= 100) {
                    p.setPorcentajeAvance(nuevo);
                    proyPort().actualizarProyecto(p);

                    // Notificar y mantener slider coherente
                    AppEvents.fireProyectoActualizado(p.getId());
                    sliderPorcentaje.setMin(p.getPorcentajeAvance() == null ? 0 : p.getPorcentajeAvance());
                    sliderPorcentaje.setValue(p.getPorcentajeAvance() == null ? 0 : p.getPorcentajeAvance());
                }

                ultimoPct = nuevo;
                sliderPorcentaje.setMin(nuevo);
                sliderPorcentaje.setValue(nuevo);

                info("Seguimiento actualizado.");

                // Recargar (volverá a ordenar DESC y a re-habilitar botones)
                cargarSeguimientos();

            } else {
                String m = respMsg(res);
                warn(m != null ? m : "No se pudo actualizar.");
            }
        } catch (Exception ex) {
            error("Error al actualizar", ex.getMessage());
        }
    }

    /** Tras eliminar un seguimiento, recalcula el % del proyecto:
     *  - Si queda algún seguimiento: usa max(% último seguimiento, % por actividades).
     *  - Si no hay seguimientos: usa % por actividades.
     *  Permite BAJAR el % solo aquí (consistente con la eliminación).
     */
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

    // NUEVO: solo se puede eliminar el de mayor %
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
        sliderPorcentaje.setValue(ultimoPct); // conserva el vigente
        txtObservaciones.clear();
        String nombre = null;
        try { nombre = UserSession.get().getAdminNombre(); } catch (Exception ignored) {}
        txtResponsable.setText(nombre != null ? nombre : "");
        tablaSeguimientos.getSelectionModel().clearSelection();
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

    /** True si el seleccionado en la tabla es el PRIMERO (último seguimiento). */
   private boolean esUltimoSeleccionado() {
    SeguimientoProyectoDto sel = tablaSeguimientos.getSelectionModel().getSelectedItem();
    if (sel == null) return false;

    // Encuentra el seguimiento con FECHA más reciente
    SeguimientoProyectoDto masReciente = tablaSeguimientos.getItems().stream()
            .filter(Objects::nonNull)
            .max(Comparator.comparing(
                    (SeguimientoProyectoDto s) -> toDate(s.getFechaSeguimiento()),
                    Comparator.nullsLast(Comparator.naturalOrder())
            ))
            .orElse(null);

    if (masReciente == null) return false;

    // Compara por id si existe, si no por fecha
    if (masReciente.getId() != null && sel.getId() != null) {
        return Objects.equals(masReciente.getId(), sel.getId());
    }
    Date dSel = toDate(sel.getFechaSeguimiento());
    Date dMax = toDate(masReciente.getFechaSeguimiento());
    return Objects.equals(dSel, dMax);
}

    /** Habilita Editar/Eliminar únicamente cuando el seleccionado es el último. */
    private void actualizarBotonesSegunSeleccion() {
    boolean hayItems = !tablaSeguimientos.getItems().isEmpty();
    boolean ultimo = esUltimoSeleccionado();                // regla vigente para EDITAR
    boolean mayorPct = esMayorPorcentajeSeleccionado();     // NUEVO: regla para ELIMINAR

    btnEditarSeguimiento.setDisable(!(hayItems && ultimo));
    btnEliminarSeguimiento.setDisable(!(hayItems && mayorPct));
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
        List<?> acts = respListGeneric(rAct); // <— usa el extractor robusto
        return acts != null && !acts.isEmpty();
    } catch (Exception ex) {
        warn("No se pudieron consultar las actividades del proyecto.");
        return false;
    }
}

    // ====== Alerts ======
    private void info(String m){ Alert a=new Alert(Alert.AlertType.INFORMATION,"",ButtonType.OK); a.setHeaderText(null); a.setContentText(m); a.showAndWait(); }
    private void warn(String m){ Alert a=new Alert(Alert.AlertType.WARNING,"",ButtonType.OK); a.setHeaderText(null); a.setContentText(m); a.showAndWait(); }
    private void error(String t,String d){ Alert a=new Alert(Alert.AlertType.ERROR,"",ButtonType.OK); a.setHeaderText(t); a.setContentText(d!=null?d:""); a.showAndWait(); }
}