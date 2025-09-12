package cr.ac.una.tareaprogramacion3.controller;

import cr.ac.una.tareaprogramacion3.util.Controller;

// Stubs generados por wsimport
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
import javafx.scene.control.Alert;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
        crearPort("http://desktop-nlro95a:8080/ProyectoService/ProyectoWS");
        prepararLista(listaEnProceso, dataEnProceso);
        prepararLista(listaEnPausa, dataEnPausa);
        prepararLista(listaFinalizados, dataFinalizados);
        cargarTodos();
    }

    private void crearPort(String endpointUrl) {
        ProyectoService svc = new ProyectoService();
        this.port = svc.getProyectoWSPort();
        Map<String, Object> ctx = ((BindingProvider) port).getRequestContext();
        ctx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

        try {
            System.out.println("[ProyectoWS.ping] => " + port.ping());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void prepararLista(ListView<ProyectoDto> lv, ObservableList<ProyectoDto> backing) {
        if (lv == null) return;
        lv.setItems(backing);
        lv.setCellFactory(v -> new ListCell<>() {
            @Override protected void updateItem(ProyectoDto p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) { setText(null); return; }
                String nombre = nvl(p.getNombre());
                String estado = nvl(p.getEstado());
                setText(estado.isEmpty() ? nombre : nombre + "  ·  " + estado);
            }
        });
    }

    /** Carga todos los proyectos usando obtenerTodosProyectos() */
    private void cargarTodos() {
        Task<List<ProyectoDto>> task = new Task<>() {
            @Override protected List<ProyectoDto> call() {
                RespuestaGeneralLista r = port.obtenerTodosProyectos();
                if (r == null) throw new RuntimeException("Sin respuesta del servidor");

                System.out.println("[WS] ok=" + r.isOk() + " mensaje=" + r.getMensaje());
                if (!Boolean.TRUE.equals(r.isOk())) {
                    String msj = (r.getMensaje() == null ? "Operación no exitosa" : r.getMensaje());
                    throw new RuntimeException(msj);
                }

                List<ProyectoDto> lista = tryGetListRobusto(r);
                System.out.println("[WS] obtenerTodosProyectos -> " + (lista == null ? 0 : lista.size()) + " proyectos");
                return (lista == null ? List.of() : lista);
            }
        };
        task.setOnSucceeded(e -> distribuirPorEstado(task.getValue()));
        task.setOnFailed(e -> mostrarError("No se pudieron cargar los proyectos", task.getException()));
        new Thread(task, "ws-cargar-todos").start();
    }

    // ----------------- EXTRACTOR ROBUSTO DE LA LISTA -----------------

    @SuppressWarnings("unchecked")
    private List<ProyectoDto> tryGetListRobusto(Object obj) {
        if (obj == null) return List.of();

        // 0) Desempaquetar JAXBElement si viniera así
        if (obj instanceof JAXBElement<?> j) {
            return tryGetListRobusto(j.getValue());
        }

        // 1) Si ya es una List, depurar tipos y listo
        if (obj instanceof List<?> l) {
            List<ProyectoDto> out = new ArrayList<>();
            for (Object o : l) if (o instanceof ProyectoDto p) out.add(p);
            return out;
        }

        Class<?> c = obj.getClass();

        // 2) Intentos por nombres más comunes del stub
        for (String mname : new String[]{"getData", "getItems", "getItem"}) {
            try {
                Method m = c.getMethod(mname);
                Object val = m.invoke(obj);
                List<ProyectoDto> res = tryGetListRobusto(val); // recursivo: puede ser wrapper o lista
                if (!res.isEmpty()) return res;
                // si está vacío, igual puede ser la respuesta correcta; la devolvemos
                if (val instanceof List) return res;
            } catch (NoSuchMethodException ignore) {
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 3) Si getItems() devuelve un wrapper con getItem()
        try {
            Method mItems = c.getMethod("getItems");
            Object itemsWrapper = mItems.invoke(obj);
            if (itemsWrapper != null) {
                // buscar getItem() dentro del wrapper
                try {
                    Method mItem = itemsWrapper.getClass().getMethod("getItem");
                    Object list = mItem.invoke(itemsWrapper);
                    List<ProyectoDto> res = tryGetListRobusto(list);
                    if (!res.isEmpty() || list instanceof List) return res;
                } catch (NoSuchMethodException ignore) {
                }
            }
        } catch (NoSuchMethodException ignore) {
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 4) Fallback: buscar cualquier getter público que retorne List
        for (Method m : c.getMethods()) {
            if (m.getParameterCount() == 0 &&
                m.getName().startsWith("get") &&
                List.class.isAssignableFrom(m.getReturnType())) {
                try {
                    Object val = m.invoke(obj);
                    List<ProyectoDto> res = tryGetListRobusto(val);
                    if (!res.isEmpty() || val instanceof List) return res;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // 5) Log diagnóstico para saber qué expone el stub
        System.out.println("[WARN] No pude interpretar el tipo de data. Clase: " + c.getName());
        System.out.println("       Métodos disponibles: " + Arrays.stream(c.getMethods())
                .map(Method::getName).distinct().sorted().collect(Collectors.joining(", ")));
        return List.of();
    }

    // -----------------------------------------------------------------

    /** Distribuye por estado. 'SUSPENDIDO' se muestra en la columna 'En Pausa'. */
    private void distribuirPorEstado(List<ProyectoDto> lista) {
        if (lista == null) lista = List.of();

        var enProceso = lista.stream()
                .filter(p -> "EN_CURSO".equalsIgnoreCase(nvl(p.getEstado())))
                .collect(Collectors.toList());

        var enPausa = lista.stream()
                .filter(p -> "SUSPENDIDO".equalsIgnoreCase(nvl(p.getEstado())))
                .collect(Collectors.toList());

        var finalizados = lista.stream()
                .filter(p -> "FINALIZADO".equalsIgnoreCase(nvl(p.getEstado())))
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
