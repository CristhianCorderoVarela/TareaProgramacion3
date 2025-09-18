package cr.ac.una.tareaprogramacion3.controller;

import cr.ac.una.client.soap.ProyectoDto;
import cr.ac.una.client.soap.ProyectoService;
import cr.ac.una.client.soap.ProyectoWS;
import cr.ac.una.client.soap.RespuestaGeneralLista;
import cr.ac.una.tareaprogramacion3.util.Controller;
import cr.ac.una.tareaprogramacion3.util.FlowController;
import jakarta.xml.ws.BindingProvider;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import javax.xml.datatype.XMLGregorianCalendar;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.scene.layout.VBox;

public class Ventana1Controller extends Controller {

    // === Barra superior ===
    @FXML private TextField txtBuscar;  // <-- Asegúrate de tenerlo en el FXML

    // === Listas por estado ===
    @FXML private ListView<ProyectoDto> listaPlanificados;
    @FXML private ListView<ProyectoDto> listaEnProceso;
    @FXML private ListView<ProyectoDto> listaSuspendidos;
    @FXML private ListView<ProyectoDto> listaFinalizados;

    // Observable por columna/estado
    private final ObservableList<ProyectoDto> dataPlanificados = FXCollections.observableArrayList();
    private final ObservableList<ProyectoDto> dataEnProceso    = FXCollections.observableArrayList();
    private final ObservableList<ProyectoDto> dataSuspendidos  = FXCollections.observableArrayList();
    private final ObservableList<ProyectoDto> dataFinalizados  = FXCollections.observableArrayList();

    // Caché de todos los proyectos (última carga del WS)
    private List<ProyectoDto> cache = new ArrayList<>();

    // WS
    private ProyectoWS port;
    private final String endpoint = "http://localhost:8080/ProyectoService/ProyectoWS";

    private final SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void initialize() {
        crearPort(endpoint);

        prepararLista(listaPlanificados, dataPlanificados);
        prepararLista(listaEnProceso,    dataEnProceso);
        prepararLista(listaSuspendidos,  dataSuspendidos);
        prepararLista(listaFinalizados,  dataFinalizados);

        // Búsqueda en vivo (streams)
        if (txtBuscar != null) {
            txtBuscar.textProperty().addListener((obs, ov, nv) -> aplicarFiltro());
        }

        cargarTodos();
    }

    private void crearPort(String endpointUrl) {
        ProyectoService svc = new ProyectoService();
        this.port = svc.getProyectoWSPort();
        ((BindingProvider) port).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);
    }

    

private void prepararLista(ListView<ProyectoDto> lv, ObservableList<ProyectoDto> backing) {
    if (lv == null) return;

    lv.setItems(backing);
    lv.setFixedCellSize(-1); // altura variable (asegura que no use celdas fijas)

    lv.setCellFactory(view -> new ListCell<>() {
        private final Label l1 = new Label(); // título/estado/%avance
        private final Label l2 = new Label(); // líderes
        private final Label l3 = new Label(); // fechas
        private final VBox box = new VBox(2, l1, l2, l3);

        {
            // wrap y ancho atado al ancho visible de la lista
            l1.setWrapText(true);
            l2.setWrapText(true);
            l3.setWrapText(true);

            // ancho de contenido: lista - padding/scrollbar (~24px)
            box.maxWidthProperty().bind(lv.widthProperty().subtract(24));
            l1.maxWidthProperty().bind(box.maxWidthProperty());
            l2.maxWidthProperty().bind(box.maxWidthProperty());
            l3.maxWidthProperty().bind(box.maxWidthProperty());

            // estilos visuales opcionales
            l1.setStyle("-fx-font-weight: bold;"); // resalta la primera línea
            box.setFillWidth(true);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY); // usamos el VBox como “graphic”
        }

        @Override
        protected void updateItem(ProyectoDto p, boolean empty) {
            super.updateItem(p, empty);
            if (empty || p == null) {
                setGraphic(null);
            } else {
                l1.setText(headerText(p));
                l2.setText(leadersText(p));
                l3.setText(datesText(p));
                setGraphic(box);
            }
        }
    });
}

private String headerText(ProyectoDto p) {
    StringBuilder sb = new StringBuilder();
    if (p.getNombre() != null) sb.append(p.getNombre().trim());
    if (p.getEstado() != null && !p.getEstado().isBlank()) sb.append(" · ").append(p.getEstado());
    if (p.getPorcentajeAvance() != null) sb.append(" · ").append(p.getPorcentajeAvance()).append("%");
    return sb.toString();
}

private String leadersText(ProyectoDto p) {
    String u = p.getLiderUsuarioNombre() == null ? "" : p.getLiderUsuarioNombre().trim();
    String t = p.getLiderTecnicoNombre() == null ? "" : p.getLiderTecnicoNombre().trim();
    String s1 = u.isEmpty() ? "" : "Líder usuario: " + u;
    String s2 = t.isEmpty() ? "" : "Líder técnico: " + t;
    if (!s1.isEmpty() && !s2.isEmpty()) return s1 + "  |  " + s2;
    return s1.isEmpty() ? s2 : s1;
}

private String datesText(ProyectoDto p) {
    String fiPlan = fmtFecha(p.getFechaInicioPlanificada());
    String ffPlan = fmtFecha(p.getFechaFinalPlanificada());
    String fiReal = fmtFecha(p.getFechaInicioReal());
    String ffReal = fmtFecha(p.getFechaFinalReal());

    String sPlan = (fiPlan != null || ffPlan != null) ? "Plan: " + (fiPlan != null ? fiPlan : "?") + " → " + (ffPlan != null ? ffPlan : "?") : "";
    String sReal = (fiReal != null || ffReal != null) ? "Real: " + (fiReal != null ? fiReal : "?") + " → " + (ffReal != null ? ffReal : "?") : "";
    if (!sPlan.isEmpty() && !sReal.isEmpty()) return sPlan + "   |   " + sReal;
    return sPlan.isEmpty() ? sReal : sPlan;
}



    // Presentación de cada item (3 líneas)
    private String render(ProyectoDto p){
        // 1) Nombre · Estado · %avance
        String linea1 = nvl(p.getNombre());
        if (!isBlank(p.getEstado())) linea1 += " · " + p.getEstado();
        if (p.getPorcentajeAvance() != null) linea1 += " · " + p.getPorcentajeAvance() + "%";

        // 2) Líderes
        String u = nvl(p.getLiderUsuarioNombre());
        String t = nvl(p.getLiderTecnicoNombre());
        String linea2 = "";
        if (!u.isEmpty()) linea2 += "Líder usuario: " + u;
        if (!t.isEmpty()) linea2 += (linea2.isEmpty() ? "" : "  |  ") + "Líder técnico: " + t;

        // 3) Fechas plan/real
        String fiPlan = fmtFecha(p.getFechaInicioPlanificada());
        String ffPlan = fmtFecha(p.getFechaFinalPlanificada());
        String fiReal = fmtFecha(p.getFechaInicioReal());
        String ffReal = fmtFecha(p.getFechaFinalReal());

        String linea3 = "";
        if (fiPlan != null || ffPlan != null)
            linea3 += "Plan: " + (fiPlan != null ? fiPlan : "?") + " → " + (ffPlan != null ? ffPlan : "?");
        if (fiReal != null || ffReal != null)
            linea3 += (linea3.isEmpty() ? "" : "   |   ") + "Real: " + (fiReal != null ? fiReal : "?") + " → " + (ffReal != null ? ffReal : "?");

        return List.of(linea1, linea2, linea3).stream()
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private String nvl(String s){ return s==null?"":s.trim(); }
    private boolean isBlank(String s){ return s==null || s.isBlank(); }

    /* =================== CARGA WS + FILTRO =================== */

    private void cargarTodos() {
        Task<List<ProyectoDto>> task = new Task<>() {
            @Override protected List<ProyectoDto> call() {
                RespuestaGeneralLista r = port.obtenerTodosProyectos();
                if (r == null || !Boolean.TRUE.equals(r.isOk()))
                    throw new RuntimeException(r == null ? "Sin respuesta del servidor" : nvl(r.getMensaje()));
                return extraerProyectos(r);
            }
        };
        task.setOnSucceeded(e -> {
            cache = task.getValue();  // guardamos en caché
            aplicarFiltro();          // filtramos lo que haya escrito
        });
        task.setOnFailed(e -> mostrarError("No se pudieron cargar los proyectos", task.getException()));
        new Thread(task, "ws-cargar-todos").start();
    }

    /** Convierte el wrapper List<Object> a List<ProyectoDto>. */
    private List<ProyectoDto> extraerProyectos(RespuestaGeneralLista r) {
        if (r == null || r.getItems() == null || r.getItems().getItem() == null) return List.of();
        List<Object> raw = r.getItems().getItem();
        return raw.stream()
                .filter(ProyectoDto.class::isInstance)
                .map(ProyectoDto.class::cast)
                .collect(Collectors.toList());
        // Nota: si tu WS ya devuelve List<ProyectoDto> directo, esto igual funciona.
    }

    /** Aplica el filtro de txtBuscar sobre la caché y distribuye por estado. */
    private void aplicarFiltro() {
        String q = (txtBuscar == null || txtBuscar.getText() == null) ? "" : txtBuscar.getText().trim().toLowerCase();
        List<ProyectoDto> base = cache == null ? List.of() : cache;

        List<ProyectoDto> filtrados = q.isEmpty()
                ? base
                : base.stream().filter(p -> matches(p, q)).collect(Collectors.toList());

        distribuirPorEstado(filtrados);
    }

    /** Coincidencia en TODO: nombre, estado, descripción, patrocinador, líderes (nombres/correos), creador, % y fechas. */
    private boolean matches(ProyectoDto p, String q) {
        if (p == null) return false;

        // Strings directos
        if (contains(p.getNombre(), q)) return true;
        if (contains(p.getEstado(), q)) return true;
        if (contains(p.getDescripcion(), q)) return true;

        // Patrocinador
        if (contains(p.getPatrocinadorNombre(), q)) return true;
        if (contains(p.getPatrocinadorCorreo(), q)) return true;

        // Líder usuario
        if (contains(p.getLiderUsuarioNombre(), q)) return true;
        if (contains(p.getLiderUsuarioCorreo(), q)) return true;

        // Líder técnico
        if (contains(p.getLiderTecnicoNombre(), q)) return true;
        if (contains(p.getLiderTecnicoCorreo(), q)) return true;

        // Creador
        if (contains(p.getCreadoPorNombre(), q)) return true;

        // Porcentaje
        if (p.getPorcentajeAvance() != null && String.valueOf(p.getPorcentajeAvance()).contains(q)) return true;

        // Fechas (formateadas)
        if (contains(fmtFecha(p.getFechaInicioPlanificada()), q)) return true;
        if (contains(fmtFecha(p.getFechaFinalPlanificada()), q)) return true;
        if (contains(fmtFecha(p.getFechaInicioReal()), q)) return true;
        if (contains(fmtFecha(p.getFechaFinalReal()), q)) return true;

        return false;
    }

    private boolean contains(String s, String q) {
        return s != null && q != null && s.toLowerCase().contains(q);
    }

    /** Parte la lista filtrada por estado y la refleja en las 4 listas. */
    private void distribuirPorEstado(List<ProyectoDto> lista) {
        if (lista == null) lista = List.of();

        final String PLANIFICADO = "PLANIFICADO";
        final String EN_CURSO    = "EN_CURSO";
        final String SUSPENDIDO  = "SUSPENDIDO";
        final String FINALIZADO  = "FINALIZADO";

        var planificados = lista.stream()
                .filter(p -> PLANIFICADO.equalsIgnoreCase(nvl(p.getEstado())))
                .collect(Collectors.toList());

        var enCurso = lista.stream()
                .filter(p -> EN_CURSO.equalsIgnoreCase(nvl(p.getEstado())))
                .collect(Collectors.toList());

        var suspendidos = lista.stream()
                .filter(p -> SUSPENDIDO.equalsIgnoreCase(nvl(p.getEstado())))
                .collect(Collectors.toList());

        var finalizados = lista.stream()
                .filter(p -> FINALIZADO.equalsIgnoreCase(nvl(p.getEstado())))
                .collect(Collectors.toList());

        Platform.runLater(() -> {
            dataPlanificados.setAll(planificados);
            dataEnProceso.setAll(enCurso);
            dataSuspendidos.setAll(suspendidos);
            dataFinalizados.setAll(finalizados);
        });
    }

    /* =================== Botones =================== */

    @FXML private void onNuevoProyecto() { abrirDialogoViaFlow(null); }

    @FXML private void onRefrescar() { cargarTodos(); }

    @FXML private void onEditarSeleccion() {
        ProyectoDto sel = obtenerSeleccion();
        if (sel == null) {
            alerta("Seleccione un proyecto en cualquiera de las listas.");
            return;
        }
        abrirDialogoViaFlow(sel);
    }

    private ProyectoDto obtenerSeleccion() {
        ProyectoDto p;
        if (listaPlanificados!=null && (p = listaPlanificados.getSelectionModel().getSelectedItem()) != null) return p;
        if (listaEnProceso   !=null && (p = listaEnProceso.getSelectionModel().getSelectedItem())    != null) return p;
        if (listaSuspendidos !=null && (p = listaSuspendidos.getSelectionModel().getSelectedItem())  != null) return p;
        if (listaFinalizados !=null && (p = listaFinalizados.getSelectionModel().getSelectedItem())  != null) return p;
        return null;
    }

    /** Abre el modal usando FlowController, pasando endpoint y dto al controller. */
    private void abrirDialogoViaFlow(ProyectoDto dto) {
        try {
            FlowController fc = FlowController.getInstance();
            ProyectoDialogController dialogCtrl =
                    (ProyectoDialogController) fc.getController("ProyectoDialog");
            dialogCtrl.init(endpoint, dto);
            fc.goViewInWindowModal("ProyectoDialog", getStage(), false);
            if (dialogCtrl.isGuardado()) cargarTodos();
        } catch (Exception ex) {
            mostrarError("No se pudo abrir el diálogo", ex);
        }
    }

    /* =================== Fechas =================== */

    /** Devuelve un String dd/MM/yyyy desde Date o XMLGregorianCalendar; null si no hay fecha. */
    private String fmtFecha(Object d) {
        if (d == null) return null;
        try {
            if (d instanceof java.util.Date) {
                return df.format((java.util.Date) d);
            }
            if (d instanceof XMLGregorianCalendar xgc) {
                return df.format(Objects.requireNonNull(xgc.toGregorianCalendar()).getTime());
            }
        } catch (Exception ignore) { /* null */ }
        return null;
    }

    /* =================== Alerts =================== */

    private void mostrarError(String titulo, Throwable ex) {
        String msg = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "Error desconocido";
        Platform.runLater(() -> {
            var a = new Alert(Alert.AlertType.ERROR);
            a.setHeaderText(titulo);
            a.setContentText(msg);
            a.showAndWait();
        });
    }

    private void alerta(String msg){
        var a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}