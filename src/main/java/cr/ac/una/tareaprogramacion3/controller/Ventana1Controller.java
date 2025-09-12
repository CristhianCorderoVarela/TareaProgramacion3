package cr.ac.una.tareaprogramacion3.controller;

import cr.ac.una.tareaprogramacion3.util.Controller;

// ===== Stubs generados por tu wsimport =====
import cr.ac.una.client.soap.ProyectoService;       // clase Service generada
import cr.ac.una.client.soap.ProyectoWS;            // interfaz del Port
import cr.ac.una.client.soap.ProyectoDto;
import cr.ac.una.client.soap.RespuestaGeneralLista; // respuesta para listas (stubs devuelven Object en getData)
// ===========================================

import jakarta.xml.ws.BindingProvider;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Ventana1Controller extends Controller {

    @FXML private ListView<ProyectoDto> listaEnProceso;
    @FXML private ListView<ProyectoDto> listaEnPausa;
    @FXML private ListView<ProyectoDto> listaFinalizados;

    private final ObservableList<ProyectoDto> dataEnProceso   = FXCollections.observableArrayList();
    private final ObservableList<ProyectoDto> dataEnPausa     = FXCollections.observableArrayList();
    private final ObservableList<ProyectoDto> dataFinalizados = FXCollections.observableArrayList();

    private ProyectoWS port;

    @Override
    public void initialize() {
        // Si tu Payara anuncia DESKTOP-NLRO95A en el log, puedes usarlo en lugar de localhost.
        crearPort("http://localhost:8080/ProyectoService/ProyectoWS");

        prepararLista(listaEnProceso, dataEnProceso);
        prepararLista(listaEnPausa, dataEnPausa);
        prepararLista(listaFinalizados, dataFinalizados);

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
        lv.setCellFactory(v -> new ListCell<>() {
            @Override protected void updateItem(ProyectoDto p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setText(null);
                } else {
                    String nombre = nvl(p.getNombre());
                    String estado = nvl(p.getEstado());
                    setText(estado.isEmpty() ? nombre : nombre + "  ·  " + estado);
                }
            }
        });
    }

    /** Llama ProyectoWS.obtenerTodosProyectos() y distribuye por estado. */
    private void cargarTodos() {
        Task<List<ProyectoDto>> task = new Task<>() {
            @Override protected List<ProyectoDto> call() {
                RespuestaGeneralLista r = port.obtenerTodosProyectos();
                validarRespuesta(r, "Sin respuesta del servidor");
                return extraerLista(r.getData());
            }
        };
        task.setOnSucceeded(e -> distribuirPorEstado(task.getValue()));
        task.setOnFailed(e -> mostrarError("No se pudieron cargar los proyectos", task.getException()));
        new Thread(task, "ws-cargar-todos").start();
    }

    /** Búsqueda usando el método con Streams del WS. */
    @SuppressWarnings("unused")
    private void buscar(String filtro) {
        Task<List<ProyectoDto>> task = new Task<>() {
            @Override protected List<ProyectoDto> call() {
                RespuestaGeneralLista r = (filtro == null || filtro.isBlank())
                        ? port.buscarProyectosActivos()
                        : port.buscarProyectosConStreams(filtro.trim());
                validarRespuesta(r, "Sin respuesta del servidor");
                return extraerLista(r.getData());
            }
        };
        task.setOnSucceeded(e -> distribuirPorEstado(task.getValue()));
        task.setOnFailed(e -> mostrarError("No se pudo realizar la búsqueda", task.getException()));
        new Thread(task, "ws-buscar").start();
    }

    private void validarRespuesta(RespuestaGeneralLista r, String mensajeNulo) {
        if (r == null) throw new RuntimeException(mensajeNulo);
        Boolean ok = r.isOk();
        if (ok == null ? false : !ok) {
            String msj = r.getMensaje();
            throw new RuntimeException(msj == null ? "Operación no exitosa" : msj);
        }
    }

    /**
     * Convierte el Object de getData() a List<ProyectoDto>.
     * - Si ya es List -> lo retorna.
     * - Si viene envuelto (ej. ArrayOfProyectoDto) intenta llamar getItem() por reflexión.
     */
    @SuppressWarnings("unchecked")
    private List<ProyectoDto> extraerLista(Object data) {
        if (data == null) return List.of();
        if (data instanceof List<?>) {
            return (List<ProyectoDto>) data;
        }
        // Intento común con wrappers generados por wsimport: getItem()
        try {
            Method m = data.getClass().getMethod("getItem");
            Object value = m.invoke(data);
            if (value instanceof List<?>) {
                return (List<ProyectoDto>) value;
            }
        } catch (ReflectiveOperationException ignore) {
            // seguimos abajo
        }
        // Último recurso: no se reconoce el tipo -> lista vacía (evita romper la UI)
        return List.of();
    }

    private void distribuirPorEstado(List<ProyectoDto> lista) {
        if (lista == null) lista = List.of();

        final String EN_CURSO   = "EN_CURSO";
        final String EN_PAUSA   = "EN_PAUSA";
        final String FINALIZADO = "FINALIZADO";

        var enProceso = lista.stream()
                .filter(p -> EN_CURSO.equalsIgnoreCase(nvl(p.getEstado())))
                .collect(Collectors.toList());

        var enPausa = lista.stream()
                .filter(p -> EN_PAUSA.equalsIgnoreCase(nvl(p.getEstado())))
                .collect(Collectors.toList());

        var finalizados = lista.stream()
                .filter(p -> FINALIZADO.equalsIgnoreCase(nvl(p.getEstado())))
                .collect(Collectors.toList());

        Platform.runLater(() -> {
            dataEnProceso.setAll(enProceso);
            dataEnPausa.setAll(enPausa);
            dataFinalizados.setAll(finalizados);
        });
    }

    private String nvl(String s) { return s == null ? "" : s; }

    private void mostrarError(String titulo, Throwable ex) {
        String msg = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "Error desconocido";
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setHeaderText(titulo);
            a.setContentText(msg);
            a.showAndWait();
        });
    }
}
