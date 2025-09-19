package cr.ac.una.tareaprogramacion3.controller;

import cr.ac.una.tareaprogramacion3.util.Controller;
import cr.ac.una.tareaprogramacion3.util.DateUtil;
import cr.ac.una.tareaprogramacion3.util.AppEvents;

// Stubs generados por wsimport
import cr.ac.una.client.soap.ProyectoService;
import cr.ac.una.client.soap.ProyectoWS;
import cr.ac.una.client.soap.ProyectoDto;
import cr.ac.una.client.soap.ActividadDto;
import cr.ac.una.client.soap.RespuestaGeneralLista;
import cr.ac.una.client.soap.RespuestaGeneral;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.ws.BindingProvider;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Ventana3Controller extends Controller {

    // Top bar
    @FXML private ComboBox<ProyectoDto> cbProyectos;
    @FXML private Button btnCargarActividades;
    @FXML private Button btnNuevaActividad;

    // Kanban: 4 columnas
    @FXML private ListView<ActividadDto> lvPlanificada;
    @FXML private ListView<ActividadDto> lvEnCurso;
    @FXML private ListView<ActividadDto> lvPostergada;
    @FXML private ListView<ActividadDto> lvFinalizada;

    // Formulario de actividad
    @FXML private TextArea txtDescripcion;
    @FXML private TextField txtEncargado;
    @FXML private TextField txtEncargadoCorreo;
    @FXML private ComboBox<String> cbEstadoActividad;
    @FXML private DatePicker fechaInicioPlanificada;
    @FXML private DatePicker fechaFinPlanificada;
    @FXML private DatePicker fechaInicioReal;
    @FXML private DatePicker fechaFinReal;
    @FXML private Spinner<Integer> spinnerOrden;

    @FXML private Button btnGuardarActividad;
    @FXML private Button btnEditarActividad;
    @FXML private Button btnEliminarActividad;
    @FXML private Button btnLimpiar;

    // Datos observables
    private final ObservableList<ProyectoDto> proyectos  = FXCollections.observableArrayList();

    private final ObservableList<ActividadDto> planificadas = FXCollections.observableArrayList();
    private final ObservableList<ActividadDto> enCurso      = FXCollections.observableArrayList();
    private final ObservableList<ActividadDto> postergadas  = FXCollections.observableArrayList();
    private final ObservableList<ActividadDto> finalizadas  = FXCollections.observableArrayList();

    // Índice por id para DnD/actualizaciones
    private final Map<Long, ActividadDto> porId = new ConcurrentHashMap<>();

    private ProyectoWS port;
    private ActividadDto actividadActual = null;

    @Override
    public void initialize() {
        crearPort("http://localhost:8080/ProyectoService/ProyectoWS");
        configurarComboProyectos();
        configurarComboEstados();
        configurarColumnasKanban();    // Kanban + DnD
        configurarSpinner();
        conectarEventos();
        configurarBotonesParaNuevaActividad();
        enlazarEstadoConFormulario();  // mueve la tarjeta si se cambia el estado en el formulario
        cargarProyectos();
    }

    private void crearPort(String endpointUrl) {
        try {
            ProyectoService svc = new ProyectoService();
            this.port = svc.getProyectoWSPort();
            Map<String, Object> ctx = ((BindingProvider) port).getRequestContext();
            ctx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);
        } catch (Exception ex) {
            mostrarError("Error de conexión", ex);
        }
    }

    private void configurarComboProyectos() {
        cbProyectos.setItems(proyectos);
        cbProyectos.setCellFactory(listView -> new ListCell<ProyectoDto>() {
            @Override
            protected void updateItem(ProyectoDto proyecto, boolean empty) {
                super.updateItem(proyecto, empty);
                setText((empty || proyecto == null) ? null : nvl(proyecto.getNombre()));
            }
        });
        cbProyectos.setButtonCell(new ListCell<ProyectoDto>() {
            @Override
            protected void updateItem(ProyectoDto proyecto, boolean empty) {
                super.updateItem(proyecto, empty);
                setText((empty || proyecto == null) ? "Seleccionar proyecto..." : nvl(proyecto.getNombre()));
            }
        });
    }

    private void configurarComboEstados() {
        cbEstadoActividad.getItems().setAll("PLANIFICADA", "EN_CURSO", "POSTERGADA", "FINALIZADA");
        cbEstadoActividad.setValue("PLANIFICADA");
    }

    private void configurarSpinner() {
        spinnerOrden.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
        spinnerOrden.setEditable(true);
    }

    private void conectarEventos() {
        btnCargarActividades.setOnAction(e -> cargarActividades());
        btnNuevaActividad.setOnAction(e -> { limpiarFormulario(); txtDescripcion.requestFocus(); });
        btnGuardarActividad.setOnAction(e -> guardarActividad());
        btnEditarActividad.setOnAction(e -> editarActividad());
        btnEliminarActividad.setOnAction(e -> eliminarActividad());
        btnLimpiar.setOnAction(e -> limpiarFormulario());
    }

    private void configurarColumnasKanban() {
        lvPlanificada.setItems(planificadas);
        lvEnCurso.setItems(enCurso);
        lvPostergada.setItems(postergadas);
        lvFinalizada.setItems(finalizadas);

        javafx.util.Callback<ListView<ActividadDto>, ListCell<ActividadDto>> factory = lv -> {
            ListCell<ActividadDto> cell = new ListCell<>() {
                @Override
                protected void updateItem(ActividadDto a, boolean empty) {
                    super.updateItem(a, empty);
                    if (empty || a == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        String orden = a.getOrdenEjecucion() != null ? ("#" + a.getOrdenEjecucion() + " ") : "";
                        setText(orden + nvl(a.getDescripcion()));
                    }
                }
            };

            // Drag start
            cell.setOnDragDetected(e -> {
                ActividadDto a = cell.getItem();
                if (a == null) return;
                Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent cc = new ClipboardContent();
                cc.putString(String.valueOf(a.getId()));
                db.setContent(cc);
                e.consume();
            });

            // Drag over
            cell.setOnDragOver(e -> {
                if (e.getDragboard().hasString()) e.acceptTransferModes(TransferMode.MOVE);
                e.consume();
            });

            // Drop sobre celda
            cell.setOnDragDropped(e -> {
                Dragboard db = e.getDragboard();
                boolean success = false;
                if (db.hasString()) {
                    Long id = Long.valueOf(db.getString());
                    ActividadDto dragged = porId.get(id);
                    if (dragged != null) {
                        @SuppressWarnings("unchecked")
                        ListView<ActividadDto> targetList = (ListView<ActividadDto>) cell.getListView();
                        ObservableList<ActividadDto> targetItems = targetList.getItems();

                        ObservableList<ActividadDto> sourceItems = listOfEstado(dragged.getEstado());
                        int dropIndex = cell.getIndex();
                        if (dropIndex < 0 || dropIndex > targetItems.size()) dropIndex = targetItems.size();

                        String nuevoEstado = estadoDeListView(targetList);
                        moverEntreListas(dragged, sourceItems, targetItems, dropIndex, nuevoEstado);
                        success = true;
                    }
                }
                e.setDropCompleted(success);
                e.consume();
            });

            // Click → cargar en formulario
            cell.setOnMouseClicked(e -> {
                if (cell.getItem() != null && e.getClickCount() == 1) {
                    cargarActividadEnFormulario(cell.getItem());
                }
            });

            return cell;
        };

        lvPlanificada.setCellFactory(factory);
        lvEnCurso.setCellFactory(factory);
        lvPostergada.setCellFactory(factory);
        lvFinalizada.setCellFactory(factory);

        // Drop en zona vacía
        java.util.function.Consumer<ListView<ActividadDto>> setupEmptyDrop = lv -> {
            lv.setOnDragOver(e -> {
                if (e.getDragboard().hasString()) e.acceptTransferModes(TransferMode.MOVE);
                e.consume();
            });
            lv.setOnDragDropped(e -> {
                Dragboard db = e.getDragboard();
                boolean success = false;
                if (db.hasString()) {
                    Long id = Long.valueOf(db.getString());
                    ActividadDto dragged = porId.get(id);
                    if (dragged != null) {
                        ObservableList<ActividadDto> targetItems = lv.getItems();
                        ObservableList<ActividadDto> sourceItems = listOfEstado(dragged.getEstado());
                        int dropIndex = targetItems.size();
                        String nuevoEstado = estadoDeListView(lv);
                        moverEntreListas(dragged, sourceItems, targetItems, dropIndex, nuevoEstado);
                        success = true;
                    }
                }
                e.setDropCompleted(success);
                e.consume();
            });
        };

        setupEmptyDrop.accept(lvPlanificada);
        setupEmptyDrop.accept(lvEnCurso);
        setupEmptyDrop.accept(lvPostergada);
        setupEmptyDrop.accept(lvFinalizada);
    }

    private void enlazarEstadoConFormulario() {
        cbEstadoActividad.valueProperty().addListener((obs, oldV, newV) -> {
            if (actividadActual == null || newV == null) return;
            ObservableList<ActividadDto> source = listOfEstado(actividadActual.getEstado());
            ObservableList<ActividadDto> target = listOfEstado(newV);
            if (source != target) {
                source.remove(actividadActual);
                target.add(actividadActual);
                actividadActual.setEstado(newV);
                recomputarOrden(source);
                recomputarOrden(target);
                // Persistimos y sincronizamos proyecto
                persistirEstadoYOrden(actividadActual);
            }
        });
    }

    private ObservableList<ActividadDto> listOfEstado(String estado) {
        return switch (estado) {
            case "PLANIFICADA" -> planificadas;
            case "EN_CURSO"    -> enCurso;
            case "POSTERGADA"  -> postergadas;
            case "FINALIZADA"  -> finalizadas;
            default -> planificadas;
        };
    }

    private String estadoDeListView(ListView<ActividadDto> lv) {
        if (lv == lvPlanificada) return "PLANIFICADA";
        if (lv == lvEnCurso)    return "EN_CURSO";
        if (lv == lvPostergada) return "POSTERGADA";
        if (lv == lvFinalizada) return "FINALIZADA";
        return "PLANIFICADA";
    }

    private void moverEntreListas(ActividadDto a,
                                  ObservableList<ActividadDto> source,
                                  ObservableList<ActividadDto> target,
                                  int dropIndex,
                                  String nuevoEstado) {
        source.remove(a);
        if (dropIndex < 0 || dropIndex > target.size()) dropIndex = target.size();
        target.add(dropIndex, a);
        if (!nuevoEstado.equals(a.getEstado())) a.setEstado(nuevoEstado);
        recomputarOrden(source);
        recomputarOrden(target);
        persistirEstadoYOrden(a); // ← guarda y sincroniza proyecto
    }

    private void recomputarOrden(ObservableList<ActividadDto> items) {
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setOrdenEjecucion(i + 1);
        }
    }

    /** Persiste cambios de actividad y luego recalcula/actualiza el proyecto + dispara evento. */
    private void persistirEstadoYOrden(ActividadDto a) {
        Task<ActividadDto> t = new Task<>() {
            @Override
            protected ActividadDto call() throws Exception {
                RespuestaGeneral r = port.actualizarActividad(a);
                if (r == null || !Boolean.TRUE.equals(r.isOk())) {
                    String msj = (r != null && r.getMensaje() != null) ? r.getMensaje() : "Error al actualizar";
                    throw new RuntimeException(msj);
                }
                return (ActividadDto) r.getData();
            }
        };
        t.setOnSucceeded(ev -> {
            ActividadDto actualizado = t.getValue();
            if (actualizado != null && actualizado.getId() != null) {
                porId.put(actualizado.getId(), actualizado);
                ObservableList<ActividadDto> list = listOfEstado(actualizado.getEstado());
                for (int i = 0; i < list.size(); i++) {
                    if (Objects.equals(list.get(i).getId(), actualizado.getId())) {
                        list.set(i, actualizado);
                        break;
                    }
                }
            }
            // ← sincroniza proyecto
            sincronizarProyectoConActividades();
        });
        t.setOnFailed(ev -> mostrarError("No se pudo actualizar actividad", t.getException()));
        new Thread(t, "persistir-estado-orden").start();
    }

    private void cargarProyectos() {
        Task<List<ProyectoDto>> task = new Task<>() {
            @Override
            protected List<ProyectoDto> call() throws Exception {
                RespuestaGeneralLista r = port.obtenerTodosProyectos();
                if (r == null) throw new RuntimeException("Sin respuesta del servidor");
                if (!Boolean.TRUE.equals(r.isOk())) {
                    String msj = r.getMensaje() != null ? r.getMensaje() : "Error al cargar proyectos";
                    throw new RuntimeException(msj);
                }
                List<ProyectoDto> lista = tryGetListRobusto(r);
                return lista != null ? lista : new ArrayList<>();
            }
        };
        task.setOnSucceeded(e -> Platform.runLater(() -> proyectos.setAll(task.getValue())));
        task.setOnFailed(e -> mostrarError("Error cargando proyectos", task.getException()));
        new Thread(task, "cargar-proyectos").start();
    }

    private void cargarActividades() {
        ProyectoDto proyectoSeleccionado = cbProyectos.getValue();
        if (proyectoSeleccionado == null) {
            mostrarAdvertencia("Debe seleccionar un proyecto primero");
            return;
        }

        Task<List<ActividadDto>> task = new Task<>() {
            @Override
            protected List<ActividadDto> call() throws Exception {
                RespuestaGeneralLista r = port.obtenerActividadesPorProyecto(proyectoSeleccionado.getId());
                if (r == null) throw new RuntimeException("Sin respuesta del servidor");
                if (!Boolean.TRUE.equals(r.isOk())) {
                    String msj = r.getMensaje() != null ? r.getMensaje() : "Error al cargar actividades";
                    throw new RuntimeException(msj);
                }
                List<ActividadDto> lista = tryGetListRobusto(r);
                return lista != null ? lista : new ArrayList<>();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            List<ActividadDto> lista = task.getValue();

            planificadas.clear();
            enCurso.clear();
            postergadas.clear();
            finalizadas.clear();
            porId.clear();

            for (ActividadDto a : lista) {
                if (a.getId() != null) porId.put(a.getId(), a);
                String est = nvl(a.getEstado());
                switch (est) {
                    case "PLANIFICADA" -> planificadas.add(a);
                    case "EN_CURSO"    -> enCurso.add(a);
                    case "POSTERGADA"  -> postergadas.add(a);
                    case "FINALIZADA"  -> finalizadas.add(a);
                    default            -> planificadas.add(a);
                }
            }

            ordenarPorOrden(planificadas);
            ordenarPorOrden(enCurso);
            ordenarPorOrden(postergadas);
            ordenarPorOrden(finalizadas);

            limpiarFormulario();
        }));

        task.setOnFailed(e -> mostrarError("Error cargando actividades", task.getException()));
        new Thread(task, "cargar-actividades").start();
    }

    private void ordenarPorOrden(ObservableList<ActividadDto> list) {
        FXCollections.sort(list, (a, b) -> {
            Integer oa = a.getOrdenEjecucion() == null ? Integer.MAX_VALUE : a.getOrdenEjecucion();
            Integer ob = b.getOrdenEjecucion() == null ? Integer.MAX_VALUE : b.getOrdenEjecucion();
            return oa.compareTo(ob);
        });
    }
    
    private void configurarBotonesParaNuevaActividad() {
    if (btnGuardarActividad != null) {
        btnGuardarActividad.setVisible(true);
        btnGuardarActividad.setDisable(false);
    }
    if (btnEditarActividad != null) {
        btnEditarActividad.setVisible(false);
        btnEditarActividad.setDisable(true);
    }
    if (btnEliminarActividad != null) {
        btnEliminarActividad.setVisible(false);
        btnEliminarActividad.setDisable(true);
    }
}

private void configurarBotonesParaEdicion() {
    if (btnGuardarActividad != null) {
        btnGuardarActividad.setVisible(false);
        btnGuardarActividad.setDisable(true);
    }
    if (btnEditarActividad != null) {
        btnEditarActividad.setVisible(true);
        btnEditarActividad.setDisable(false);
    }
    if (btnEliminarActividad != null) {
        btnEliminarActividad.setVisible(true);
        btnEliminarActividad.setDisable(false);
    }
}

    private void cargarActividadEnFormulario(ActividadDto actividad) {
        actividadActual = actividad;

        txtDescripcion.setText(nvl(actividad.getDescripcion()));
        txtEncargado.setText(nvl(actividad.getEncargadoNombre()));
        txtEncargadoCorreo.setText(nvl(actividad.getEncargadoCorreo()));
        cbEstadoActividad.setValue(nvl(actividad.getEstado()));

        txtDescripcion.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= 500 ? change : null));

        fechaInicioPlanificada.setValue(dateToLocalDate(DateUtil.fromXml(actividad.getFechaInicioPlanificada())));
        fechaFinPlanificada.setValue(dateToLocalDate(DateUtil.fromXml(actividad.getFechaFinalPlanificada())));
        fechaInicioReal.setValue(dateToLocalDate(DateUtil.fromXml(actividad.getFechaInicioReal())));
        fechaFinReal.setValue(dateToLocalDate(DateUtil.fromXml(actividad.getFechaFinalReal())));

        spinnerOrden.getValueFactory().setValue(
                actividad.getOrdenEjecucion() != null ? actividad.getOrdenEjecucion() : 1
        );

        configurarBotonesParaEdicion();
    }

    private void limpiarFormulario() {
        actividadActual = null;
        txtDescripcion.clear();
        txtEncargado.clear();
        txtEncargadoCorreo.clear();
        cbEstadoActividad.setValue("PLANIFICADA");
        fechaInicioPlanificada.setValue(null);
        fechaFinPlanificada.setValue(null);
        fechaInicioReal.setValue(null);
        fechaFinReal.setValue(null);
        spinnerOrden.getValueFactory().setValue(1);
        configurarBotonesParaNuevaActividad();
    }

    private void guardarActividad() {
        if (!validarFormulario()) return;

        ProyectoDto proyecto = cbProyectos.getValue();
        if (proyecto == null) {
            mostrarAdvertencia("Debe seleccionar un proyecto");
            return;
        }

        Task<ActividadDto> task = new Task<>() {
            @Override
            protected ActividadDto call() throws Exception {
                ActividadDto nuevaActividad = crearActividadDesdeFormulario();
                nuevaActividad.setProyectoId(proyecto.getId());
                RespuestaGeneral r = port.crearActividad(nuevaActividad);
                if (r == null) throw new RuntimeException("Sin respuesta del servidor");
                if (!Boolean.TRUE.equals(r.isOk())) {
                    String msj = r.getMensaje() != null ? r.getMensaje() : "Error al crear actividad";
                    throw new RuntimeException(msj);
                }
                return (ActividadDto) r.getData();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            mostrarInformacion("Actividad creada exitosamente");
            cargarActividades();
            sincronizarProyectoConActividades(); // ← recalcula proyecto
            limpiarFormulario();
        }));

        task.setOnFailed(e -> mostrarError("Error creando actividad", task.getException()));
        new Thread(task, "crear-actividad").start();
    }

    private void editarActividad() {
        if (actividadActual == null) {
            mostrarAdvertencia("Debe seleccionar una actividad para editar");
            return;
        }
        if (!validarFormulario()) return;

        Task<ActividadDto> task = new Task<>() {
            @Override
            protected ActividadDto call() throws Exception {
                ActividadDto actividadModificada = crearActividadDesdeFormulario();
                actividadModificada.setId(actividadActual.getId());
                actividadModificada.setProyectoId(actividadActual.getProyectoId());
                RespuestaGeneral r = port.actualizarActividad(actividadModificada);
                if (r == null) throw new RuntimeException("Sin respuesta del servidor");
                if (!Boolean.TRUE.equals(r.isOk())) {
                    String msj = r.getMensaje() != null ? r.getMensaje() : "Error al actualizar actividad";
                    throw new RuntimeException(msj);
                }
                return (ActividadDto) r.getData();
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            mostrarInformacion("Actividad actualizada exitosamente");
            cargarActividades();
            sincronizarProyectoConActividades(); // ← recalcula proyecto
            limpiarFormulario();
        }));

        task.setOnFailed(e -> mostrarError("Error actualizando actividad", task.getException()));
        new Thread(task, "actualizar-actividad").start();
    }

    private void eliminarActividad() {
        if (actividadActual == null) {
            mostrarAdvertencia("Debe seleccionar una actividad para eliminar");
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setHeaderText("Confirmar eliminación");
        confirmacion.setContentText("¿Está seguro que desea eliminar esta actividad?");
        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        RespuestaGeneral r = port.eliminarActividad(actividadActual.getId());
                        if (r == null) throw new RuntimeException("Sin respuesta del servidor");
                        if (!Boolean.TRUE.equals(r.isOk())) {
                            String msj = r.getMensaje() != null ? r.getMensaje() : "Error al eliminar actividad";
                            throw new RuntimeException(msj);
                        }
                        return null;
                    }
                };
                task.setOnSucceeded(e -> Platform.runLater(() -> {
                    mostrarInformacion("Actividad eliminada exitosamente");
                    cargarActividades();
                    sincronizarProyectoConActividades(); // ← recalcula proyecto
                    limpiarFormulario();
                }));
                task.setOnFailed(e -> mostrarError("Error eliminando actividad", task.getException()));
                new Thread(task, "eliminar-actividad").start();
            }
        });
    }

    private ActividadDto crearActividadDesdeFormulario() {
        ActividadDto actividad = new ActividadDto();
        actividad.setDescripcion(txtDescripcion.getText().trim());
        actividad.setEncargadoNombre(txtEncargado.getText().trim());
        actividad.setEncargadoCorreo(txtEncargadoCorreo.getText().trim());
        actividad.setEstado(cbEstadoActividad.getValue());
        actividad.setFechaInicioPlanificada(DateUtil.toXml(localDateToDate(fechaInicioPlanificada.getValue())));
        actividad.setFechaFinalPlanificada(DateUtil.toXml(localDateToDate(fechaFinPlanificada.getValue())));
        actividad.setFechaInicioReal(DateUtil.toXml(localDateToDate(fechaInicioReal.getValue())));
        actividad.setFechaFinalReal(DateUtil.toXml(localDateToDate(fechaFinReal.getValue())));
        actividad.setOrdenEjecucion(spinnerOrden.getValue());
        return actividad;
    }

    /* =================== Sincronización Proyecto =================== */

    /** Lee las actividades visibles y actualiza el proyecto (estado/avance/fechas) en el WS y notifica. */
    private void sincronizarProyectoConActividades() {
        ProyectoDto p = cbProyectos.getValue();
        if (p == null) return;

        List<ActividadDto> todas = new ArrayList<>();
        todas.addAll(planificadas);
        todas.addAll(enCurso);
        todas.addAll(postergadas);
        todas.addAll(finalizadas);

        String nuevoEstado = calcularEstadoProyecto(todas);
        Integer nuevoAvance = calcularAvanceProyecto(todas);
        Date fiReal = calcularFechaInicioRealProyecto(todas);
        Date ffReal = calcularFechaFinalRealProyecto(todas, nuevoEstado);

        // Actualizamos el DTO (conservando demás campos)
        p.setEstado(nuevoEstado);
        p.setPorcentajeAvance(nuevoAvance);
        p.setFechaInicioReal(fiReal == null ? null : DateUtil.toXml(fiReal));
        p.setFechaFinalReal(ffReal == null ? null : DateUtil.toXml(ffReal));

        // === IMPORTANTE ===
        // Si tu WS no es actualizarProyecto(ProyectoDto) cambia SOLO esta llamada:
        Task<ProyectoDto> t = new Task<>() {
            @Override
            protected ProyectoDto call() throws Exception {
                RespuestaGeneral r = port.actualizarProyecto(p);
                if (r == null || !Boolean.TRUE.equals(r.isOk())) {
                    String msj = (r != null && r.getMensaje() != null) ? r.getMensaje() : "Error al actualizar proyecto";
                    throw new RuntimeException(msj);
                }
                return (ProyectoDto) r.getData();
            }
        };
        t.setOnSucceeded(e -> {
            ProyectoDto actualizado = t.getValue();
            if (actualizado != null) {
                // refrescamos combo (por estética) y notificamos a otras ventanas
                Platform.runLater(() -> {
                    int idx = proyectos.indexOf(p);
                    if (idx >= 0) proyectos.set(idx, actualizado);
                    cbProyectos.getSelectionModel().select(actualizado);
                });
                AppEvents.fireProyectoActualizado(actualizado.getId());
            }
        });
        t.setOnFailed(e -> mostrarError("No se pudo actualizar el proyecto con el cambio de actividades", t.getException()));
        new Thread(t, "sync-proyecto").start();
    }

    private String calcularEstadoProyecto(List<ActividadDto> acts) {
        if (acts == null || acts.isEmpty()) return "PLANIFICADO";
        boolean anyEnCurso = acts.stream().anyMatch(a -> "EN_CURSO".equalsIgnoreCase(nvl(a.getEstado())));
        boolean anyPost = acts.stream().anyMatch(a -> "POSTERGADA".equalsIgnoreCase(nvl(a.getEstado())));
        boolean allFin = !acts.isEmpty() && acts.stream().allMatch(a -> "FINALIZADA".equalsIgnoreCase(nvl(a.getEstado())));
        if (allFin) return "FINALIZADO";
        if (anyEnCurso) return "EN_CURSO";
        if (anyPost) return "SUSPENDIDO";
        return "PLANIFICADO";
    }

    private Integer calcularAvanceProyecto(List<ActividadDto> acts) {
        if (acts == null || acts.isEmpty()) return 0;
        long fin = acts.stream().filter(a -> "FINALIZADA".equalsIgnoreCase(nvl(a.getEstado()))).count();
        return (int)Math.floor( (fin * 100.0) / acts.size() );
    }

    private Date calcularFechaInicioRealProyecto(List<ActividadDto> acts) {
        return acts.stream()
                .map(a -> DateUtil.fromXml(a.getFechaInicioReal()))
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private Date calcularFechaFinalRealProyecto(List<ActividadDto> acts, String estadoProyecto) {
        if (!"FINALIZADO".equalsIgnoreCase(estadoProyecto)) return null;
        return acts.stream()
                .map(a -> DateUtil.fromXml(a.getFechaFinalReal()))
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    /* =================== Validaciones y utilitarios =================== */

    private boolean validarFormulario() {
        if (txtDescripcion.getText().trim().isEmpty()) {
            mostrarAdvertencia("La descripción es obligatoria");
            txtDescripcion.requestFocus();
            return false;
        }
        if (txtEncargado.getText().trim().isEmpty()) {
            mostrarAdvertencia("El nombre del encargado es obligatorio");
            txtEncargado.requestFocus();
            return false;
        }
        if (txtEncargadoCorreo.getText().trim().isEmpty()) {
            mostrarAdvertencia("El correo del encargado es obligatorio");
            txtEncargadoCorreo.requestFocus();
            return false;
        }
        if (fechaInicioPlanificada.getValue() == null) {
            mostrarAdvertencia("La fecha de inicio planificada es obligatoria");
            return false;
        }
        if (fechaFinPlanificada.getValue() == null) {
            mostrarAdvertencia("La fecha de fin planificada es obligatoria");
            return false;
        }
        return true;
    }

    private String nvl(String s) { return s == null ? "" : s; }

    // Date <-> LocalDate
    private LocalDate dateToLocalDate(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private Date localDateToDate(LocalDate localDate) {
        if (localDate == null) return null;
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> tryGetListRobusto(Object obj) {
        if (obj == null) return List.of();
        if (obj instanceof JAXBElement<?> j) return tryGetListRobusto(j.getValue());
        if (obj instanceof List<?> l) {
            List<T> out = new ArrayList<>();
            for (Object o : l) { try { out.add((T) o); } catch (ClassCastException ignore) {} }
            return out;
        }
        Class<?> c = obj.getClass();
        for (String mname : new String[]{"getData", "getItems", "getItem"}) {
            try {
                Method m = c.getMethod(mname);
                Object val = m.invoke(obj);
                List<T> res = tryGetListRobusto(val);
                if (!res.isEmpty()) return res;
                if (val instanceof List) return res;
            } catch (NoSuchMethodException ignore) {
            } catch (Exception e) { e.printStackTrace(); }
        }
        return List.of();
    }

    private void mostrarError(String titulo, Throwable ex) {
        String msg = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "Error desconocido";
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setHeaderText(titulo);
            a.setContentText(msg);
            a.showAndWait();
        });
    }

    private void mostrarAdvertencia(String mensaje) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText("Atención");
        a.setContentText(mensaje);
        a.showAndWait();
    }

    private void mostrarInformacion(String mensaje) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText("Información");
        a.setContentText(mensaje);
        a.showAndWait();
    }
}