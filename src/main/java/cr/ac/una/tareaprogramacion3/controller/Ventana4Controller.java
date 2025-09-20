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
import javafx.util.StringConverter;

// WS
import cr.ac.una.client.soap.*;

// Base/Utils
import cr.ac.una.tareaprogramacion3.util.Controller;
import cr.ac.una.tareaprogramacion3.util.UserSession;
import jakarta.xml.ws.BindingProvider;
import javafx.scene.layout.HBox;

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
        limpiarFormulario();
        cargarProyectos();               // carga al entrar
        prepararReglasFormulario();
    }

    // (Método sin-args opcional; no marca @Override para evitar conflictos si la base no lo declara)
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

    // Barra + porcentaje
    colPorcentaje.setCellValueFactory(d ->
            new SimpleIntegerProperty(d.getValue().getPorcentajeAvance() == null ? 0 : d.getValue().getPorcentajeAvance()));
    colPorcentaje.setCellFactory(col -> new TableCell<>() {
        private final ProgressBar bar = new ProgressBar(0);
        private final Label lbl = new Label();
        private final HBox box = new HBox(8, bar, lbl);
        {
            bar.setPrefWidth(120);
        }
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

    tablaSeguimientos.getSelectionModel().selectedItemProperty().addListener((obs, o, s) -> {
        if (s != null) {
            fechaSeguimiento.setValue(toLocalDate(s.getFechaSeguimiento())); // queda bloqueada
            int selPct = s.getPorcentajeAvance() == null ? 0 : s.getPorcentajeAvance();
// El mínimo siempre es el porcentaje del ÚLTIMO seguimiento
sliderPorcentaje.setMin(ultimoPct);
sliderPorcentaje.setValue(Math.max(ultimoPct, selPct));
            txtObservaciones.setText(s.getObservaciones() == null ? "" : s.getObservaciones());
            txtResponsable.setText(s.getCreadoPorNombre() == null ? "" : s.getCreadoPorNombre());
        }
    });

    // Orden por fecha DESC al cargar datos
    tablaSeguimientos.getSortOrder().setAll(colFecha);
    colFecha.setSortType(TableColumn.SortType.DESCENDING);
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
    if (newP == null) return;

    // Cargar seguimientos del proyecto y posicionar barra con el último % (o % del proyecto)
    cargarSeguimientos();
});
    }

    private void prepararEventos() {
        btnCargarSeguimientos.setOnAction(e -> cargarSeguimientos());
       
        btnLimpiarSeguimiento.setOnAction(e -> limpiarFormulario());
        btnGuardarSeguimiento.setOnAction(e -> guardar());
        btnEditarSeguimiento.setOnAction(e -> editar());
        btnEliminarSeguimiento.setOnAction(e -> eliminar());
    }
    
    private static final int OBS_MAX = 500; // ajusta si tu columna en BD tiene otro tamaño

private void prepararReglasFormulario() {
    // Fecha = hoy, no editable
    fechaSeguimiento.setValue(LocalDate.now());
    fechaSeguimiento.setEditable(false);
    fechaSeguimiento.setDisable(true);
    fechaSeguimiento.setStyle("-fx-opacity: 1;");
    fechaSeguimiento.setMouseTransparent(true);

    // Observaciones: obligatorio y con límite de caracteres
    txtObservaciones.setTextFormatter(new TextFormatter<String>(c -> {
        String next = c.getControlNewText();
        if (next != null && next.length() > OBS_MAX) return null;
        return c;
    }));

    // Slider: ticks y entero
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
            if (!lista.isEmpty())
                cbProyectos.getSelectionModel().selectFirst();
            else
                warn("No se recibieron proyectos del servicio.");
        } catch (Exception ex) {
            error("Error cargando proyectos", ex.getMessage());
        }
    }

    private void cargarSeguimientos() {
    ProyectoDto p = cbProyectos.getSelectionModel().getSelectedItem();
    if (p == null || p.getId() == null) { warn("Seleccione un proyecto."); return; }

    try {
        Object r = segPort().buscarSeguimientosPorProyecto(p.getId());

        // 1) Obtener lista tipada (¡solo una declaración!)
        List<SeguimientoProyectoDto> lista =
                Ventana4Controller.<SeguimientoProyectoDto>respListOf(r, SeguimientoProyectoDto.class);

        // 2) Ordenar por fecha DESC
        lista.sort(Comparator.comparing(
                (SeguimientoProyectoDto s) -> toDate(s.getFechaSeguimiento()),
                Comparator.nullsLast(Comparator.naturalOrder())
        ).reversed());

        // 3) Cargar en la tabla y aplicar sort visual
        tablaSeguimientos.getItems().setAll(lista);
        colFecha.setSortType(TableColumn.SortType.DESCENDING);
        tablaSeguimientos.getSortOrder().setAll(colFecha);
        tablaSeguimientos.sort();

        // 4) Fijar barra de avance al valor correcto (no retrocede)
        int base = p.getPorcentajeAvance() == null ? 0 : p.getPorcentajeAvance();
        if (!lista.isEmpty()) {
            Integer ult = lista.get(0).getPorcentajeAvance();  // el más reciente tras ordenar DESC
            if (ult != null) base = Math.max(base, ult);
        }
        sliderPorcentaje.setMin(base);
        sliderPorcentaje.setValue(base);

        // 5) Fecha visible del día (picker deshabilitado pero mostrando el valor)
        fechaSeguimiento.setValue(LocalDate.now());

    } catch (Exception ex) {
        error("Error consultando seguimientos", ex.getMessage());
    }
}

    // ====== Acciones ======
   private void guardar() {
    ProyectoDto p = cbProyectos.getSelectionModel().getSelectedItem();
    if (p == null || p.getId() == null) { warn("Seleccione un proyecto."); return; }

    int pct = (int) Math.round(sliderPorcentaje.getValue());
    if (pct < 0 || pct > 100) { warn("El porcentaje debe estar entre 0 y 100."); return; }

    String obs = txtObservaciones.getText() == null ? "" : txtObservaciones.getText().trim();
    if (obs.isEmpty()) { warn("Las observaciones son obligatorias."); return; }
    if (obs.length() > OBS_MAX) { warn("Las observaciones no deben exceder " + OBS_MAX + " caracteres."); return; }

    Long adminId = UserSession.get().getAdminId();
    if (adminId == null) { warn("No hay usuario autenticado en sesión."); return; }

    SeguimientoProyectoDto dto = new SeguimientoProyectoDto();
    dto.setProyectoId(p.getId());
    dto.setFechaSeguimiento(toXmlCal(LocalDate.now())); // siempre hoy
    dto.setPorcentajeAvance(pct);
    dto.setObservaciones(obs);
    dto.setCreadoPorId(adminId);

    try {
        Object res = segPort().crearSeguimiento(dto);
        if (respOk(res)) {
            // Regla: sincronizar proyecto y nunca decrementar
            int current = p.getPorcentajeAvance() == null ? 0 : p.getPorcentajeAvance();
            int nuevo = Math.max(current, pct);
            if (nuevo != current) {
                p.setPorcentajeAvance(nuevo);
                proyPort().actualizarProyecto(p);
            }

            info("Seguimiento guardado.");
            cargarSeguimientos();
            // Mantener slider mínimo en el nuevo valor (no retrocede)
            sliderPorcentaje.setMin(nuevo);
            sliderPorcentaje.setValue(nuevo);
            // Notifica otras ventanas si usas eventos
            // AppEvents.fireProyectoActualizado(p.getId());  // descomenta si ya lo usas
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

    int pct = (int) Math.round(sliderPorcentaje.getValue());
    if (pct < 0 || pct > 100) { warn("El porcentaje debe estar entre 0 y 100."); return; }

    String obs = txtObservaciones.getText() == null ? "" : txtObservaciones.getText().trim();
    if (obs.isEmpty()) { warn("Las observaciones son obligatorias."); return; }
    if (obs.length() > OBS_MAX) { warn("Las observaciones no deben exceder " + OBS_MAX + " caracteres."); return; }

    // La fecha queda como está (picker bloqueado)
    sel.setPorcentajeAvance(pct);
    sel.setObservaciones(obs);

    try {
        Object res = segPort().actualizarSeguimiento(sel);
        if (respOk(res)) {
            // Sincronizar proyecto sin decrecer
            int current = p.getPorcentajeAvance() == null ? 0 : p.getPorcentajeAvance();
            int nuevo = Math.max(current, pct);
            if (nuevo != current) {
                p.setPorcentajeAvance(nuevo);
                proyPort().actualizarProyecto(p);
            }

            info("Seguimiento actualizado.");
            cargarSeguimientos();
            sliderPorcentaje.setMin(nuevo);
            sliderPorcentaje.setValue(nuevo);
            // AppEvents.fireProyectoActualizado(p.getId()); // si corresponde
        } else {
            String m = respMsg(res);
            warn(m != null ? m : "No se pudo actualizar.");
        }
    } catch (Exception ex) {
        error("Error al actualizar", ex.getMessage());
    }
}

    private void eliminar() {
        SeguimientoProyectoDto sel = tablaSeguimientos.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getId() == null) { warn("Seleccione un seguimiento."); return; }
        if (new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar seguimiento seleccionado?")
                .showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            Object res = segPort().eliminarSeguimiento(sel.getId());
            if (respOk(res)) {
                info("Seguimiento eliminado.");
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
        fechaSeguimiento.setValue(null);
        sliderPorcentaje.setValue(0);
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

    /**
     * Lista genérica para respuestas "simples" (getData, getLista, etc.) — útil para proyectos.
     */
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

    /**
     * Extrae y filtra por tipo desde respuestas "mixtas" (como RespuestaWsLista#getProyectoOrSeguimientoOrActividad()).
     */
    @SuppressWarnings("unchecked")
    private static <T> List<T> respListOf(Object resp, Class<T> type) {
        if (resp == null) return Collections.emptyList();

        Object data = callNoArg(resp,
                // genéricos
                "getData", "getDatos", "getLista", "getItems",
                // específico del WS generado
                "getProyectoOrSeguimientoOrActividad"
        );

        List<T> out = new ArrayList<>();
        if (data instanceof List<?> list) {
            for (Object o : list) {
                // manejar posibles JAXBElement (Jakarta JAXB en JAX-WS 4)
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

    // ====== Debug útil si algo viene vacío ======
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

    // ====== Pequeñas alerts locales ======
    private void info(String m){ Alert a=new Alert(Alert.AlertType.INFORMATION,"",ButtonType.OK); a.setHeaderText(null); a.setContentText(m); a.showAndWait(); }
    private void warn(String m){ Alert a=new Alert(Alert.AlertType.WARNING,"",ButtonType.OK); a.setHeaderText(null); a.setContentText(m); a.showAndWait(); }
    private void error(String t,String d){ Alert a=new Alert(Alert.AlertType.ERROR,"",ButtonType.OK); a.setHeaderText(t); a.setContentText(d!=null?d:""); a.showAndWait(); }
}
