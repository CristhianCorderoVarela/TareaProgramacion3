package cr.ac.una.tareaprogramacion3.controller;

import cr.ac.una.tareaprogramacion3.util.Controller;
import cr.ac.una.tareaprogramacion3.util.FlowController;

import cr.ac.una.client.soap.ProyectoDto;
import cr.ac.una.client.soap.ProyectoService;
import cr.ac.una.client.soap.ProyectoWS;
import cr.ac.una.client.soap.RespuestaGeneralLista;

import jakarta.xml.ws.BindingProvider;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Window;

import java.util.List;
import java.util.stream.Collectors;

public class Ventana1Controller extends Controller {

    @FXML private ListView<ProyectoDto> listaPlanificados;
    @FXML private ListView<ProyectoDto> listaEnProceso;
    @FXML private ListView<ProyectoDto> listaSuspendidos;
    @FXML private ListView<ProyectoDto> listaFinalizados;

    private final ObservableList<ProyectoDto> dataPlanificados = FXCollections.observableArrayList();
    private final ObservableList<ProyectoDto> dataEnProceso    = FXCollections.observableArrayList();
    private final ObservableList<ProyectoDto> dataSuspendidos  = FXCollections.observableArrayList();
    private final ObservableList<ProyectoDto> dataFinalizados  = FXCollections.observableArrayList();

    private ProyectoWS port;
    private final String endpoint = "http://localhost:8080/ProyectoService/ProyectoWS";

    @Override
    public void initialize() {
        crearPort(endpoint);

        prepararLista(listaPlanificados, dataPlanificados);
        prepararLista(listaEnProceso,    dataEnProceso);
        prepararLista(listaSuspendidos,  dataSuspendidos);
        prepararLista(listaFinalizados,  dataFinalizados);

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
                setText(empty || p == null ? null : render(p));
            }
        });

        // doble click -> editar
        lv.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                ProyectoDto sel = lv.getSelectionModel().getSelectedItem();
                if (sel != null) onEditarSeleccion();
            }
        });
    }

    private String render(ProyectoDto p){
        String nombre = nvl(p.getNombre());
        String estado = nvl(p.getEstado());
        return estado.isEmpty() ? nombre : nombre + "  ·  " + estado;
    }
    private String nvl(String s){ return s==null?"":s; }

    private void cargarTodos() {
        Task<List<ProyectoDto>> task = new Task<>() {
            @Override protected List<ProyectoDto> call() {
                RespuestaGeneralLista r = port.obtenerTodosProyectos();
                if (r == null || !Boolean.TRUE.equals(r.isOk()))
                    throw new RuntimeException(r == null ? "Sin respuesta del servidor" : nvl(r.getMensaje()));
                return extraerProyectos(r);
            }
        };
        task.setOnSucceeded(e -> distribuirPorEstado(task.getValue()));
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
    }

    private void distribuirPorEstado(List<ProyectoDto> lista) {
        if (lista == null) lista = List.of();

        final String PLANIFICADO = "PLANIFICADO";
        final String EN_CURSO    = "EN_CURSO";
        final String SUSPENDIDO  = "SUSPENDIDO";
        final String FINALIZADO  = "FINALIZADO";

        var planificados = lista.stream().filter(p -> PLANIFICADO.equalsIgnoreCase(nvl(p.getEstado()))).collect(Collectors.toList());
        var enCurso      = lista.stream().filter(p -> EN_CURSO.equalsIgnoreCase(nvl(p.getEstado()))).collect(Collectors.toList());
        var suspendidos  = lista.stream().filter(p -> SUSPENDIDO.equalsIgnoreCase(nvl(p.getEstado()))).collect(Collectors.toList());
        var finalizados  = lista.stream().filter(p -> FINALIZADO.equalsIgnoreCase(nvl(p.getEstado()))).collect(Collectors.toList());

        Platform.runLater(() -> {
            dataPlanificados.setAll(planificados);
            dataEnProceso.setAll(enCurso);
            dataSuspendidos.setAll(suspendidos);
            dataFinalizados.setAll(finalizados);
        });
    }

    // === Botones ===
    @FXML private void onNuevoProyecto()    { abrirDialogoViaFlow(null); }
    @FXML private void onRefrescar()        { cargarTodos(); }
    @FXML private void onEditarSeleccion() {
        ProyectoDto sel = obtenerSeleccion();
        if (sel == null) { alerta("Seleccione un proyecto en cualquiera de las listas."); return; }
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

        // 1) Obtener el controller tipado desde el Flow
        ProyectoDialogController dialogCtrl =
                (ProyectoDialogController) fc.getController("ProyectoDialog");

        // 2) Pasar datos (dto puede ser null para NUEVO)
        dialogCtrl.init(endpoint, dto);

        // 3) Abrir modal (bloquea hasta cerrar)
        fc.goViewInWindowModal("ProyectoDialog", getStage(), false);

        // 4) Si guardó, refrescar
        if (dialogCtrl.isGuardado()) {
            cargarTodos();
        }
    } catch (Exception ex) {
        mostrarError("No se pudo abrir el diálogo", ex);
    }
}


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
