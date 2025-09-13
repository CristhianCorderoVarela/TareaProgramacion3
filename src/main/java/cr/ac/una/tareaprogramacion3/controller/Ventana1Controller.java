package cr.ac.una.tareaprogramacion3.controller;

import cr.ac.una.tareaprogramacion3.util.Controller;
import cr.ac.una.tareaprogramacion3.util.FlowController;
import cr.ac.una.client.soap.ProyectoService;
import cr.ac.una.client.soap.ProyectoWS;
import cr.ac.una.client.soap.ProyectoDto;
import cr.ac.una.client.soap.RespuestaGeneralLista;
import jakarta.xml.ws.BindingProvider;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
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
    private final String endpoint = "http://DESKTOP-NLRO95A:8080/ProyectoService/ProyectoWS";

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

        lv.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                ProyectoDto sel = lv.getSelectionModel().getSelectedItem();
                if (sel != null) abrirDialogo(sel);
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

    // === Botones (conectados desde FXML) ===
    @FXML private void onNuevoProyecto()   { abrirDialogo(null); }
    @FXML private void onEditarSeleccion() {
        ProyectoDto sel = obtenerSeleccion();
        if (sel == null) { alerta("Seleccione un proyecto en cualquiera de las listas."); return; }
        abrirDialogo(sel);
    }
    @FXML private void onRefrescar()       { cargarTodos(); }

    private ProyectoDto obtenerSeleccion() {
        ProyectoDto p;
        if (listaPlanificados!=null && (p = listaPlanificados.getSelectionModel().getSelectedItem()) != null) return p;
        if (listaEnProceso   !=null && (p = listaEnProceso.getSelectionModel().getSelectedItem())    != null) return p;
        if (listaSuspendidos !=null && (p = listaSuspendidos.getSelectionModel().getSelectedItem())  != null) return p;
        if (listaFinalizados !=null && (p = listaFinalizados.getSelectionModel().getSelectedItem())  != null) return p;
        return null;
    }

    /** Abre el diálogo: intenta con FlowController y si no, usa FXMLLoader. */
    private void abrirDialogo(ProyectoDto dto) {
        // --- Intento con FlowController (sin 'put', porque tu clase no lo tiene) ---
        try {
            FlowController fc = FlowController.getInstance();
            // Ajusta al método real que tengas disponible:
            fc.goViewInWindowModal("ProyectoDialog", getStage(), Boolean.FALSE);
            // No podemos pasar el dto por FlowController porque tu API no lo expone.
            // Por eso dejamos el fallback abajo que sí inyecta el dto.
        } catch (Throwable ignore) {
            // seguimos al fallback
        }

        // --- Fallback garantizado con FXMLLoader (pasa endpoint y dto) ---
        try {
            FXMLLoader l = new FXMLLoader(getClass().getResource("/cr/ac/una/tareaprogramacion3/view/ProyectoDialog.fxml"));
            Region root = l.load();
            ProyectoDialogController ctrl = l.getController();
            ctrl.init(endpoint, dto); // dto puede ser null (nuevo)

            Stage st = new Stage();
            st.setTitle(dto == null ? "Nuevo Proyecto" : "Editar Proyecto");
            st.initModality(Modality.APPLICATION_MODAL);
            st.initOwner(getStage());
            st.setScene(new Scene(root));
            st.setResizable(false);
            st.showAndWait();

            if (ctrl.isGuardado()) cargarTodos();
        } catch (IOException ex) {
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
