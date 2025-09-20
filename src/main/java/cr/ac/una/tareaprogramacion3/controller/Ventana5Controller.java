package cr.ac.una.tareaprogramacion3.controller;

import cr.ac.una.tareaprogramacion3.util.Controller;
import cr.ac.una.tareaprogramacion3.util.AppEvents;

// WS
import cr.ac.una.client.soap.ProyectoDto;
import cr.ac.una.client.soap.ProyectoService;
import cr.ac.una.client.soap.ProyectoWS;
import cr.ac.una.client.soap.SeguimientoProyectoDto;
import cr.ac.una.client.soap.SeguimientoService;
import cr.ac.una.client.soap.SeguimientoWS;
import jakarta.xml.ws.BindingProvider;

// JavaFX
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

// Java
import javax.xml.datatype.XMLGregorianCalendar;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class Ventana5Controller extends Controller {

    // Top
    @FXML private Button btnActualizar;

    // Indicadores
    @FXML private Label lblTotalProyectos;
    @FXML private Label lblProyectosActivos;
    @FXML private Label lblProyectosFinalizados;
    @FXML private Label lblAvancePromedio;

    // Tabla proyectos activos
    @FXML private TableView<ProyectoDto> tablaProyectosActivos;
    @FXML private TableColumn<ProyectoDto, String>    colProyecto;
    @FXML private TableColumn<ProyectoDto, Number>    colAvance;
    @FXML private TableColumn<ProyectoDto, LocalDate> colUltimoSeguimiento;
    @FXML private TableColumn<ProyectoDto, String>    colEstado;
    @FXML private TableColumn<ProyectoDto, Void>      colAcciones;

    // Gráficos
    @FXML private PieChart graficoEstadoProyectos;
    @FXML private BarChart<String, Number> graficoAvanceProyectos;

    // Puertos WS
    private ProyectoWS proyPort;
    private SeguimientoWS segPort;

    // ========= Ciclo de vida =========
    @FXML
    public void initialize() {
        System.out.println("[V5] initialize()");
        crearPuertos();
        configurarTabla();
        configurarEventos();
        cargarDashboard(); // carga inicial

        // Mantener sincronizado cuando otras ventanas actualizan un proyecto
        AppEvents.onProyectoActualizado(id -> Platform.runLater(this::cargarDashboard));
    }

    private void crearPuertos() {
        proyPort = new ProyectoService().getProyectoWSPort();
        segPort  = new SeguimientoService().getSeguimientoWSPort();
        Map<String, Object> c1 = ((BindingProvider) proyPort).getRequestContext();
        Map<String, Object> c2 = ((BindingProvider) segPort).getRequestContext();
        c1.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "http://localhost:8080/ProyectoService/ProyectoWS");
        c2.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, "http://localhost:8080/SeguimientoService/SeguimientoWS");
        System.out.println("[V5] Puertos WS configurados");
    }

    private void configurarEventos() {
        btnActualizar.setOnAction(e -> {
            System.out.println("[V5] Botón Actualizar presionado");
            cargarDashboard();
        });
    }

    private void configurarTabla() {
        colProyecto.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getNombre() == null ? "" : d.getValue().getNombre()
        ));
        colAvance.setCellValueFactory(d -> new SimpleIntegerProperty(
                d.getValue().getPorcentajeAvance() == null ? 0 : d.getValue().getPorcentajeAvance()
        ));
        colUltimoSeguimiento.setCellValueFactory(d -> {
            LocalDate ld = ultimoSeguimientoFecha(d.getValue().getId());
            return new SimpleObjectProperty<>(ld);
        });
        colEstado.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().getEstado() == null ? "" : d.getValue().getEstado()
        ));

        // Acciones: botón por fila para ver seguimientos en diálogo
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Ver seguimientos");
            {
                btn.setOnAction(e -> {
                    ProyectoDto p = getTableView().getItems().get(getIndex());
                    mostrarDialogoSeguimientos(p);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        System.out.println("[V5] Tabla configurada");
    }

    // ========= Carga del dashboard =========
    private void cargarDashboard() {
        System.out.println("[V5] cargarDashboard() start");
        new Thread(() -> {
            try {
                // 1) Proyectos
                Object r = proyPort.obtenerTodosProyectos();
                List<ProyectoDto> proyectos = extraerListaRobusta(r, ProyectoDto.class);
                System.out.println("[V5] Proyectos recibidos: " + proyectos.size());

                // Indicadores
                int total = proyectos.size();
                long finalizados = proyectos.stream()
                        .filter(p -> "FINALIZADO".equalsIgnoreCase(n(p.getEstado()))).count();
                long activos = total - finalizados;

                double prom = proyectos.isEmpty() ? 0.0 :
                        proyectos.stream()
                                .map(p -> p.getPorcentajeAvance() == null ? 0 : p.getPorcentajeAvance())
                                .mapToInt(Integer::intValue)
                                .average().orElse(0.0);

                // 2) Pie (estado)
                Map<String, Long> porEstado = proyectos.stream()
                        .collect(Collectors.groupingBy(p -> {
                            String est = n(p.getEstado());
                            return est.isBlank() ? "PLANIFICADO" : est;
                        }, Collectors.counting()));

                // 3) Solo activos (no finalizados) para tabla + barras
                List<ProyectoDto> activosList = proyectos.stream()
                        .filter(p -> !"FINALIZADO".equalsIgnoreCase(n(p.getEstado())))
                        .collect(Collectors.toList());

                // 4) Serie de barras
                XYChart.Series<String, Number> serie = new XYChart.Series<>();
                for (ProyectoDto p : activosList) {
                    int av = p.getPorcentajeAvance() == null ? 0 : p.getPorcentajeAvance();
                    serie.getData().add(new XYChart.Data<>(n(p.getNombre()), av));
                }

                // 5) Pintar UI
                Platform.runLater(() -> {
                    lblTotalProyectos.setText(String.valueOf(total));
                    lblProyectosActivos.setText(String.valueOf(activos));
                    lblProyectosFinalizados.setText(String.valueOf(finalizados));
                    lblAvancePromedio.setText(Math.round(prom) + "%");

                    graficoEstadoProyectos.getData().setAll(
                            porEstado.entrySet().stream()
                                    .map(e -> new PieChart.Data(e.getKey(), e.getValue()))
                                    .collect(Collectors.toList())
                    );

                    tablaProyectosActivos.getItems().setAll(activosList);

                    graficoAvanceProyectos.getData().clear();
                    graficoAvanceProyectos.getData().add(serie);
                });

                System.out.println("[V5] Proyectos activos: " + activosList.size());
                System.out.println("[V5] cargarDashboard() end");
            } catch (Throwable ex) {
                mostrarError("Error cargando dashboard", ex);
            }
        }, "dash-load").start();
    }

    // ========= Diálogo de seguimientos =========
    private void mostrarDialogoSeguimientos(ProyectoDto p) {
        if (p == null || p.getId() == null) {
            alertaInfo("Seleccione un proyecto válido.");
            return;
        }
        new Thread(() -> {
            try {
                Object resp = segPort.buscarSeguimientosPorProyecto(p.getId());
                List<SeguimientoProyectoDto> segs = extraerListaRobusta(resp, SeguimientoProyectoDto.class);

                // Orden DESC por fecha
                segs.sort(Comparator.comparing(
                        (SeguimientoProyectoDto s) -> toDate(s.getFechaSeguimiento()),
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed());

                Platform.runLater(() -> abrirDialogoTablaSeguimientos(p, segs));
            } catch (Throwable ex) {
                mostrarError("Error consultando seguimientos", ex);
            }
        }, "load-segs").start();
    }

    private void abrirDialogoTablaSeguimientos(ProyectoDto p, List<SeguimientoProyectoDto> segs) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Seguimientos de: " + n(p.getNombre()));
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TableView<SeguimientoProyectoDto> table = new TableView<>();
        TableColumn<SeguimientoProyectoDto, LocalDate> cFecha = new TableColumn<>("Fecha");
        TableColumn<SeguimientoProyectoDto, Number>     cPct  = new TableColumn<>("Avance %");
        TableColumn<SeguimientoProyectoDto, String>     cObs  = new TableColumn<>("Observaciones");
        TableColumn<SeguimientoProyectoDto, String>     cResp = new TableColumn<>("Responsable");

        cFecha.setCellValueFactory(d -> new SimpleObjectProperty<>(toLocalDate(d.getValue().getFechaSeguimiento())));
        cPct.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getPorcentajeAvance() == null ? 0 : d.getValue().getPorcentajeAvance()));
        cObs.setCellValueFactory(d -> new SimpleStringProperty(n(d.getValue().getObservaciones())));
        cResp.setCellValueFactory(d -> new SimpleStringProperty(n(d.getValue().getCreadoPorNombre())));

        cFecha.setPrefWidth(110); cPct.setPrefWidth(90); cObs.setPrefWidth(350); cResp.setPrefWidth(160);
        table.getColumns().setAll(cFecha, cPct, cObs, cResp);
        table.getItems().setAll(segs);

        dlg.getDialogPane().setContent(new HBox(table));
        dlg.showAndWait();
    }

    // ========= Helpers de UI/fecha =========
    private void mostrarError(String titulo, Throwable ex) {
        String msg = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "Error desconocido";
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
            a.setHeaderText(titulo);
            a.showAndWait();
        });
    }

    private void alertaInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private String n(String s) { return s == null ? "" : s; }

    private Date toDate(XMLGregorianCalendar x) {
        return x == null ? null : x.toGregorianCalendar().getTime();
    }
    private LocalDate toLocalDate(XMLGregorianCalendar x) {
        Date d = toDate(x);
        return d == null ? null : d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /** Devuelve la fecha del ÚLTIMO seguimiento (más reciente) de un proyecto. */
    private LocalDate ultimoSeguimientoFecha(Long proyectoId) {
        if (proyectoId == null) return null;
        try {
            Object r = segPort.buscarSeguimientosPorProyecto(proyectoId);
            List<SeguimientoProyectoDto> segs = extraerListaRobusta(r, SeguimientoProyectoDto.class);
            return segs.stream()
                    .filter(Objects::nonNull)
                    .max(Comparator.comparing(
                            (SeguimientoProyectoDto s) -> toDate(s.getFechaSeguimiento()),
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ))
                    .map(s -> toLocalDate(s.getFechaSeguimiento()))
                    .orElse(null);
        } catch (Throwable ex) {
            // Silencioso; la celda queda vacía
            return null;
        }
    }

    // ========= Extractores ROBUSTOS para WS =========
    @SuppressWarnings("unchecked")
    private <T> List<T> extraerListaRobusta(Object resp, Class<T> type) {
        if (resp == null) return Collections.emptyList();

        // 0) Directo: si ya es lista/array del tipo pedido
        List<T> direct = aLista(resp, type);
        if (direct != null && !direct.isEmpty()) {
            System.out.println("[V5] extraerLista<" + type.getSimpleName() + "> -> " + direct.size() + " (direct)");
            return direct;
        }

        // 1) Probar getters típicos de wrappers
        Object data = null;
        for (String getter : new String[]{
                "getData", "getDatos", "getLista", "getItems",
                "getProyectoOrSeguimientoOrActividad",
                "getProyectos", "getSeguimientos"
        }) {
            data = tryGet(resp, getter);
            if (data != null) break;
        }

        // 1.b) Si el "data" también es wrapper (ej. getLista() -> objeto con getItem())
        List<Object> bruto = null;
        if (data != null) {
            bruto = aLista(data, Object.class);
            if (bruto == null) {
                Object items = tryGet(data, "getItem"); // muy común en wrappers JAX-WS
                bruto = aLista(items, Object.class);
            }
        }

        // 2) Si aún no hay lista, barrer TODOS los getters sin args que devuelvan List/array y tomar el primero útil
        if (bruto == null) bruto = firstListLike(resp);
        if (bruto == null && data != null) bruto = firstListLike(data);

        if (bruto == null) {
            logDumpRespuesta(resp, "No se encontró lista en wrapper.");
            return Collections.emptyList();
        }

        // 3) Des-envolver/adaptar cada item
        List<T> out = new ArrayList<>();
        for (Object o : bruto) {
            Object v = (o instanceof jakarta.xml.bind.JAXBElement<?> j) ? j.getValue() : o;

            // Ya es del tipo
            if (v != null && type.isInstance(v)) {
                out.add((T) v);
                continue;
            }

            // Adaptar a tipos conocidos
            try {
                if (type == ProyectoDto.class) {
                    ProyectoDto dto = new ProyectoDto();
                    dto.setId(       (Long) tryCall(v, "getId", Long.class));
                    dto.setNombre(   (String) tryCall(v, "getNombre", String.class));
                    dto.setEstado(   (String) tryCall(v, "getEstado", String.class));
                    Object pa = tryCall(v, "getPorcentajeAvance", Integer.class);
                    if (pa instanceof Integer i) dto.setPorcentajeAvance(i);
                    if (dto.getId() != null || (dto.getNombre() != null && !dto.getNombre().isBlank())) {
                        out.add((T) dto);
                        continue;
                    }
                } else if (type == SeguimientoProyectoDto.class) {
                    SeguimientoProyectoDto dto = new SeguimientoProyectoDto();
                    dto.setId( (Long) tryCall(v, "getId", Long.class));
                    Object fx = tryCall(v, "getFechaSeguimiento", javax.xml.datatype.XMLGregorianCalendar.class);
                    if (fx instanceof javax.xml.datatype.XMLGregorianCalendar x) dto.setFechaSeguimiento(x);
                    Object pct = tryCall(v, "getPorcentajeAvance", Integer.class);
                    if (pct instanceof Integer i) dto.setPorcentajeAvance(i);
                    dto.setObservaciones( (String) tryCall(v, "getObservaciones", String.class));
                    dto.setCreadoPorNombre((String) tryCall(v, "getCreadoPorNombre", String.class));
                    if (dto.getId() != null || dto.getFechaSeguimiento() != null || dto.getPorcentajeAvance() != null) {
                        out.add((T) dto);
                        continue;
                    }
                }
            } catch (Throwable ignored) { /* omitir si no se puede adaptar */ }
        }

        System.out.println("[V5] extraerLista<" + type.getSimpleName() + "> -> " + out.size() + " elementos");
        if (out.isEmpty()) logDumpRespuesta(resp, "Lista vacía tras adaptar; revisa forma de respuesta.");
        return out;
    }

    /** Devuelve la PRIMERA lista/array encontrada en cualquier getter sin argumentos del objeto. */
    @SuppressWarnings("unchecked")
    private List<Object> firstListLike(Object src) {
        if (src == null) return null;
        for (Method m : src.getClass().getMethods()) {
            if (m.getParameterCount() != 0) continue;
            String n = m.getName();
            if (!(n.startsWith("get") || n.startsWith("is"))) continue;
            try {
                Object val = m.invoke(src);
                if (val == null) continue;
                if (val instanceof jakarta.xml.bind.JAXBElement<?> j) val = j.getValue();
                List<Object> asList = aLista(val, Object.class);
                if (asList != null && !asList.isEmpty()) return asList;
            } catch (Exception ignore) {}
        }
        return null;
    }

    /** Convierte un objeto (List, array o wrapper con getItem()) a List<T>. */
    @SuppressWarnings("unchecked")
    private <T> List<T> aLista(Object data, Class<T> type) {
        if (data == null) return null;

        List<T> out = new ArrayList<>();

        // Caso: ya es List
        if (data instanceof List<?> l) {
            for (Object o : l) {
                Object v = (o instanceof jakarta.xml.bind.JAXBElement<?> j) ? j.getValue() : o;
                if (type == Object.class || (v != null && type.isInstance(v))) out.add((T) v);
                else if (type == Object.class && v != null) out.add((T) v);
            }
            return out;
        }

        // Caso: array
        if (data.getClass().isArray()) {
            int n = java.lang.reflect.Array.getLength(data);
            for (int i = 0; i < n; i++) {
                Object o = java.lang.reflect.Array.get(data, i);
                Object v = (o instanceof jakarta.xml.bind.JAXBElement<?> j) ? j.getValue() : o;
                if (type == Object.class || (v != null && type.isInstance(v))) out.add((T) v);
                else if (type == Object.class && v != null) out.add((T) v);
            }
            return out;
        }

        // Caso: wrapper con getItem()
        Object items = tryGet(data, "getItem");
        if (items != null) return aLista(items, type);

        return null;
    }

    // ========= Utilitarios de reflexión / debug =========
    private Object tryGet(Object target, String getter) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(getter);
            return m.invoke(target);
        } catch (Exception ignored) { return null; }
    }

    private Object tryCall(Object target, String getter, Class<?> expected) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(getter);
            Object v = m.invoke(target);
            if (v instanceof jakarta.xml.bind.JAXBElement<?> j) v = j.getValue();
            if (v != null && (expected == Object.class || expected.isInstance(v))) return v;
        } catch (Exception ignored) {}
        return null;
    }

    private void logDumpRespuesta(Object resp, String msg) {
        try {
            System.out.println("[V5][DUMP] " + msg + " -> clase=" + (resp != null ? resp.getClass().getName() : "null"));
            if (resp != null) {
                for (Method m : resp.getClass().getMethods()) {
                    if (m.getParameterCount() == 0 && (m.getName().startsWith("get") || m.getName().startsWith("is"))) {
                        try {
                            Object v = m.invoke(resp);
                            if (v == null) continue;
                            String tipo = v.getClass().getName();
                            int tam = 0;
                            if (v instanceof List<?> l) tam = l.size();
                            else if (v.getClass().isArray()) tam = java.lang.reflect.Array.getLength(v);
                            System.out.println("  • " + m.getName() + " -> " + tipo + (tam > 0 ? " (" + tam + ")" : ""));
                        } catch (Exception ignore) {}
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[V5][DUMP] error: " + e.getMessage());
        }
    }
}