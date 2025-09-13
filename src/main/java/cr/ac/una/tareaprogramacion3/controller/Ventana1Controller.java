package cr.ac.una.tareaprogramacion3.controller;

import cr.ac.una.tareaprogramacion3.util.Controller;

import cr.ac.una.client.soap.ProyectoService;
import cr.ac.una.client.soap.ProyectoWS;
import cr.ac.una.client.soap.ProyectoDto;
import cr.ac.una.client.soap.RespuestaGeneralLista;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.ws.BindingProvider;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Ventana1Controller extends Controller {

    @FXML private ListView<ProyectoDto> listaEnProceso;
    @FXML private ListView<ProyectoDto> listaEnPausa;
    @FXML private ListView<ProyectoDto> listaFinalizados;

    private final ObservableList<ProyectoDto> dataEnProceso   = FXCollections.observableArrayList();
    private final ObservableList<ProyectoDto> dataEnPausa     = FXCollections.observableArrayList();
    private final ObservableList<ProyectoDto> dataFinalizados = FXCollections.observableArrayList();

    private ProyectoWS port;
    // Usa el endpoint que te muestra Payara en los logs:
    private String endpoint = "http://DESKTOP-NLRO95A:8080/ProyectoService/ProyectoWS";

    @Override
    public void initialize() {
        crearPort(endpoint);

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
                setText(empty || p == null ? null : render(p));
            }
        });

        // doble clic -> editar
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
                return toProyectoList(r);
            }
        };
        task.setOnSucceeded(e -> distribuirPorEstado(task.getValue()));
        task.setOnFailed(e -> mostrarError("No se pudieron cargar los proyectos", task.getException()));
        new Thread(task, "ws-cargar-todos").start();
    }

    @SuppressWarnings("unchecked")
    private List<ProyectoDto> toProyectoList(RespuestaGeneralLista r){
        var items = (r == null) ? null : r.getItems();
        if (items == null || items.getItem() == null) return List.of();

        List<ProyectoDto> out = new ArrayList<>();
        for (Object o : items.getItem()) {
            if (o instanceof ProyectoDto) {
                out.add((ProyectoDto) o);
            } else if (o instanceof JAXBElement) {
                Object v = ((JAXBElement<?>) o).getValue();
                if (v instanceof ProyectoDto) out.add((ProyectoDto) v);
            }
        }
        return out;
    }

   private void distribuirPorEstado(List<ProyectoDto> lista) {
    if (lista == null) lista = List.of();

    var enProceso = lista.stream()
            .filter(p -> "EN_CURSO".equalsIgnoreCase(nvl(p.getEstado())))
            .collect(Collectors.toList());

    var enPausa = lista.stream()
            .filter(p -> {
                String e = nvl(p.getEstado()).trim().toUpperCase();
                e = e.replace('Á','A');         // quitar acento por si acaso
                // aceptar variantes comunes
                return e.equals("EN_PAUSA") || e.equals("EN PAUSA") || e.equals("PAUSA") || e.equals("PAUSADO");
            })
            .collect(Collectors.toList());

    var finalizados = lista.stream()
            .filter(p -> {
                String e = nvl(p.getEstado()).trim().toUpperCase();
                return e.equals("FINALIZADO") || e.equals("FINALIZADA");
            })
            .collect(Collectors.toList());

    // (opcional) log para ver qué viene realmente
    // lista.stream().map(p -> nvl(p.getEstado())).collect(Collectors.toSet()).forEach(s -> System.out.println("ESTADO -> " + s));

    Platform.runLater(() -> {
        dataEnProceso.setAll(enProceso);
        dataEnPausa.setAll(enPausa);
        dataFinalizados.setAll(finalizados);
    });
}


    @FXML
    private void onNuevoProyecto() { abrirDialogo(null); }

    @FXML
    private void onEditarSeleccion() {
        ProyectoDto sel = obtenerSeleccion();
        if (sel == null) {
            alerta("Seleccione un proyecto en cualquiera de las listas.");
            return;
        }
        abrirDialogo(sel);
    }

    @FXML
    private void onRefrescar() { cargarTodos(); }

    private ProyectoDto obtenerSeleccion() {
        ProyectoDto p = null;
        if (listaEnProceso != null && (p = listaEnProceso.getSelectionModel().getSelectedItem()) != null) return p;
        if (listaEnPausa   != null && (p = listaEnPausa.getSelectionModel().getSelectedItem())   != null) return p;
        if (listaFinalizados != null && (p = listaFinalizados.getSelectionModel().getSelectedItem()) != null) return p;
        return null;
    }

    private void abrirDialogo(ProyectoDto dto) {
        try {
            FXMLLoader l = new FXMLLoader(getClass().getResource("/cr/ac/una/tareaprogramacion3/view/ProyectoDialog.fxml"));
            Region root = l.load();
            ProyectoDialogController ctrl = l.getController();
            ctrl.init(endpoint, dto == null ? null : clonar(dto)); // copia defensiva

            Stage st = new Stage();
            st.setTitle(dto == null ? "Nuevo Proyecto" : "Editar Proyecto");
            st.initModality(Modality.APPLICATION_MODAL);
            st.setScene(new Scene(root));
            st.setResizable(false);
            st.showAndWait();

            if (ctrl.isGuardado()) {
                cargarTodos(); // refresca
            }
        } catch (IOException ex) {
            mostrarError("No se pudo abrir el diálogo", ex);
        }
    }

    private ProyectoDto clonar(ProyectoDto p) {
        ProyectoDto x = new ProyectoDto();
        x.setId(p.getId());
        x.setNombre(p.getNombre());
        x.setEstado(p.getEstado());
        x.setPatrocinadorNombre(p.getPatrocinadorNombre());
        x.setLiderUsuarioNombre(p.getLiderUsuarioNombre());
        x.setLiderTecnicoNombre(p.getLiderTecnicoNombre());
        x.setDescripcion(p.getDescripcion());
        x.setFechaInicioPlanificada(p.getFechaInicioPlanificada());
        x.setFechaFinalPlanificada(p.getFechaFinalPlanificada());
        x.setFechaInicioReal(p.getFechaInicioReal());
        x.setFechaFinalReal(p.getFechaFinalReal());
        x.setPorcentajeAvance(p.getPorcentajeAvance());
        return x;
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
