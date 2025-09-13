package cr.ac.una.tareaprogramacion3.controller;

import cr.ac.una.tareaprogramacion3.util.Controller;
import cr.ac.una.tareaprogramacion3.util.DateUtil;

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

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Ventana3Controller extends Controller {

    // Elementos del FXML
    @FXML private ComboBox<ProyectoDto> cbProyectos;
    @FXML private Button btnCargarActividades;
    @FXML private Button btnNuevaActividad;
    @FXML private ListView<ActividadDto> listaActividades;
    
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
    private final ObservableList<ProyectoDto> proyectos = FXCollections.observableArrayList();
    private final ObservableList<ActividadDto> actividades = FXCollections.observableArrayList();

    private ProyectoWS port;
    private ActividadDto actividadActual = null;

    @Override
    public void initialize() {
        System.out.println("Inicializando Ventana3Controller...");
        
        crearPort("http://localhost:8080/ProyectoService/ProyectoWS");
        configurarComboProyectos();
        configurarComboEstados();
        configurarListaActividades();
        configurarSpinner();
        conectarEventos();
        cargarProyectos();
    }

    private void crearPort(String endpointUrl) {
        try {
            ProyectoService svc = new ProyectoService();
            this.port = svc.getProyectoWSPort();
            Map<String, Object> ctx = ((BindingProvider) port).getRequestContext();
            ctx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);
            
            System.out.println("[ProyectoWS.ping] => " + port.ping());
        } catch (Exception ex) {
            System.err.println("Error creando conexión WebSocket:");
            ex.printStackTrace();
            mostrarError("Error de conexión", ex);
        }
    }

    private void configurarComboProyectos() {
        cbProyectos.setItems(proyectos);
        
        cbProyectos.setCellFactory(listView -> new ListCell<ProyectoDto>() {
            @Override
            protected void updateItem(ProyectoDto proyecto, boolean empty) {
                super.updateItem(proyecto, empty);
                if (empty || proyecto == null) {
                    setText(null);
                } else {
                    setText(nvl(proyecto.getNombre()));
                }
            }
        });
        
        cbProyectos.setButtonCell(new ListCell<ProyectoDto>() {
            @Override
            protected void updateItem(ProyectoDto proyecto, boolean empty) {
                super.updateItem(proyecto, empty);
                if (empty || proyecto == null) {
                    setText("Seleccionar proyecto...");
                } else {
                    setText(nvl(proyecto.getNombre()));
                }
            }
        });
    }
        
    private void configurarComboEstados() {
        cbEstadoActividad.getItems().addAll(
            "PLANIFICADA", "EN_CURSO", "POSTERGADA", "FINALIZADA"
        );
        cbEstadoActividad.setValue("PLANIFICADA");
    }

    private void configurarListaActividades() {
        listaActividades.setItems(actividades);
        
        listaActividades.setCellFactory(listView -> new ListCell<ActividadDto>() {
            @Override
            protected void updateItem(ActividadDto actividad, boolean empty) {
                super.updateItem(actividad, empty);
                if (empty || actividad == null) {
                    setText(null);
                } else {
                    String desc = nvl(actividad.getDescripcion());
                    String estado = nvl(actividad.getEstado());
                    String orden = actividad.getOrdenEjecucion() != null ? 
                                  " (#" + actividad.getOrdenEjecucion() + ")" : "";
                    setText(desc + orden + " - " + estado);
                }
            }
        });

        listaActividades.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    cargarActividadEnFormulario(newValue);
                }
            }
        );
    }

    private void configurarSpinner() {
        spinnerOrden.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
        spinnerOrden.setEditable(true);
    }

    private void conectarEventos() {
        btnCargarActividades.setOnAction(e -> cargarActividades());
        btnNuevaActividad.setOnAction(e -> limpiarFormulario());
        btnGuardarActividad.setOnAction(e -> guardarActividad());
        btnEditarActividad.setOnAction(e -> editarActividad());
        btnEliminarActividad.setOnAction(e -> eliminarActividad());
        btnLimpiar.setOnAction(e -> limpiarFormulario());
    }

    private void cargarProyectos() {
        Task<List<ProyectoDto>> task = new Task<List<ProyectoDto>>() {
            @Override
            protected List<ProyectoDto> call() throws Exception {
                System.out.println("Cargando proyectos...");
                RespuestaGeneralLista r = port.obtenerTodosProyectos();
                
                if (r == null) {
                    throw new RuntimeException("Sin respuesta del servidor");
                }
                
                System.out.println("[WS] ok=" + r.isOk() + " mensaje=" + r.getMensaje());
                if (!Boolean.TRUE.equals(r.isOk())) {
                    String msj = r.getMensaje() != null ? r.getMensaje() : "Error al cargar proyectos";
                    throw new RuntimeException(msj);
                }
                
                List<ProyectoDto> lista = tryGetListRobusto(r);
                System.out.println("[WS] obtenerTodosProyectos -> " + (lista != null ? lista.size() : 0) + " proyectos");
                return lista != null ? lista : new ArrayList<>();
            }
        };
        
        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                proyectos.setAll(task.getValue());
                System.out.println("Proyectos cargados: " + proyectos.size());
            });
        });
        
        task.setOnFailed(e -> {
            mostrarError("Error cargando proyectos", task.getException());
        });
        
        new Thread(task, "cargar-proyectos").start();
    }

    private void cargarActividades() {
        ProyectoDto proyectoSeleccionado = cbProyectos.getValue();
        if (proyectoSeleccionado == null) {
            mostrarAdvertencia("Debe seleccionar un proyecto primero");
            return;
        }

        Task<List<ActividadDto>> task = new Task<List<ActividadDto>>() {
            @Override
            protected List<ActividadDto> call() throws Exception {
                System.out.println("Cargando actividades del proyecto ID: " + proyectoSeleccionado.getId());
                
                // Usar el método del WebService para obtener actividades
                RespuestaGeneralLista r = port.obtenerActividadesPorProyecto(proyectoSeleccionado.getId());
                
                if (r == null) {
                    throw new RuntimeException("Sin respuesta del servidor");
                }
                
                System.out.println("[WS] ok=" + r.isOk() + " mensaje=" + r.getMensaje());
                if (!Boolean.TRUE.equals(r.isOk())) {
                    String msj = r.getMensaje() != null ? r.getMensaje() : "Error al cargar actividades";
                    throw new RuntimeException(msj);
                }
                
                List<ActividadDto> lista = tryGetListRobusto(r);
                System.out.println("[WS] obtenerActividadesPorProyecto -> " + (lista != null ? lista.size() : 0) + " actividades");
                return lista != null ? lista : new ArrayList<>();
            }
        };
        
        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                actividades.setAll(task.getValue());
                limpiarFormulario();
                System.out.println("Actividades cargadas: " + actividades.size());
                
                if (actividades.isEmpty()) {
                    mostrarInformacion("No se encontraron actividades para este proyecto");
                }
            });
        });
        
        task.setOnFailed(e -> {
            mostrarError("Error cargando actividades", task.getException());
        });
        
        new Thread(task, "cargar-actividades").start();
    }

    private void cargarActividadEnFormulario(ActividadDto actividad) {
        actividadActual = actividad;
        
        txtDescripcion.setText(nvl(actividad.getDescripcion()));
        txtEncargado.setText(nvl(actividad.getEncargadoNombre()));
        txtEncargadoCorreo.setText(nvl(actividad.getEncargadoCorreo()));
        cbEstadoActividad.setValue(nvl(actividad.getEstado()));
        
        // Convertir fechas usando DateUtil
        fechaInicioPlanificada.setValue(dateToLocalDate(DateUtil.fromXml(actividad.getFechaInicioPlanificada())));
        fechaFinPlanificada.setValue(dateToLocalDate(DateUtil.fromXml(actividad.getFechaFinalPlanificada())));
        fechaInicioReal.setValue(dateToLocalDate(DateUtil.fromXml(actividad.getFechaInicioReal())));
        fechaFinReal.setValue(dateToLocalDate(DateUtil.fromXml(actividad.getFechaFinalReal())));
        
        if (actividad.getOrdenEjecucion() != null) {
            spinnerOrden.getValueFactory().setValue(actividad.getOrdenEjecucion());
        }
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
        listaActividades.getSelectionModel().clearSelection();
    }

    private void guardarActividad() {
        if (!validarFormulario()) return;
        
        ProyectoDto proyecto = cbProyectos.getValue();
        if (proyecto == null) {
            mostrarAdvertencia("Debe seleccionar un proyecto");
            return;
        }

        Task<ActividadDto> task = new Task<ActividadDto>() {
            @Override
            protected ActividadDto call() throws Exception {
                ActividadDto nuevaActividad = crearActividadDesdeFormulario();
                nuevaActividad.setProyectoId(proyecto.getId());
                
                RespuestaGeneral r = port.crearActividad(nuevaActividad);
                
                if (r == null) {
                    throw new RuntimeException("Sin respuesta del servidor");
                }
                
                if (!Boolean.TRUE.equals(r.isOk())) {
                    String msj = r.getMensaje() != null ? r.getMensaje() : "Error al crear actividad";
                    throw new RuntimeException(msj);
                }
                
                return (ActividadDto) r.getData();
            }
        };
        
        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                mostrarInformacion("Actividad creada exitosamente");
                cargarActividades(); // Recargar la lista
                limpiarFormulario();
            });
        });
        
        task.setOnFailed(e -> {
            mostrarError("Error creando actividad", task.getException());
        });
        
        new Thread(task, "crear-actividad").start();
    }

    private void editarActividad() {
        if (actividadActual == null) {
            mostrarAdvertencia("Debe seleccionar una actividad para editar");
            return;
        }
        
        if (!validarFormulario()) return;
        
        Task<ActividadDto> task = new Task<ActividadDto>() {
            @Override
            protected ActividadDto call() throws Exception {
                ActividadDto actividadModificada = crearActividadDesdeFormulario();
                actividadModificada.setId(actividadActual.getId());
                actividadModificada.setProyectoId(actividadActual.getProyectoId());
                
                RespuestaGeneral r = port.actualizarActividad(actividadModificada);
                
                if (r == null) {
                    throw new RuntimeException("Sin respuesta del servidor");
                }
                
                if (!Boolean.TRUE.equals(r.isOk())) {
                    String msj = r.getMensaje() != null ? r.getMensaje() : "Error al actualizar actividad";
                    throw new RuntimeException(msj);
                }
                
                return (ActividadDto) r.getData();
            }
        };
        
        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                mostrarInformacion("Actividad actualizada exitosamente");
                cargarActividades(); // Recargar la lista
                limpiarFormulario();
            });
        });
        
        task.setOnFailed(e -> {
            mostrarError("Error actualizando actividad", task.getException());
        });
        
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
                Task<Void> task = new Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        RespuestaGeneral r = port.eliminarActividad(actividadActual.getId());
                        
                        if (r == null) {
                            throw new RuntimeException("Sin respuesta del servidor");
                        }
                        
                        if (!Boolean.TRUE.equals(r.isOk())) {
                            String msj = r.getMensaje() != null ? r.getMensaje() : "Error al eliminar actividad";
                            throw new RuntimeException(msj);
                        }
                        
                        return null;
                    }
                };
                
                task.setOnSucceeded(e -> {
                    Platform.runLater(() -> {
                        mostrarInformacion("Actividad eliminada exitosamente");
                        cargarActividades(); // Recargar la lista
                        limpiarFormulario();
                    });
                });
                
                task.setOnFailed(e -> {
                    mostrarError("Error eliminando actividad", task.getException());
                });
                
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
        
        // Convertir fechas usando DateUtil
        actividad.setFechaInicioPlanificada(DateUtil.toXml(localDateToDate(fechaInicioPlanificada.getValue())));
        actividad.setFechaFinalPlanificada(DateUtil.toXml(localDateToDate(fechaFinPlanificada.getValue())));
        actividad.setFechaInicioReal(DateUtil.toXml(localDateToDate(fechaInicioReal.getValue())));
        actividad.setFechaFinalReal(DateUtil.toXml(localDateToDate(fechaFinReal.getValue())));
        
        actividad.setOrdenEjecucion(spinnerOrden.getValue());
        return actividad;
    }

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

    @SuppressWarnings("unchecked")
    private <T> List<T> tryGetListRobusto(Object obj) {
        if (obj == null) return List.of();

        if (obj instanceof JAXBElement<?> j) {
            return tryGetListRobusto(j.getValue());
        }

        if (obj instanceof List<?> l) {
            List<T> out = new ArrayList<>();
            for (Object o : l) {
                try {
                    @SuppressWarnings("unchecked")
                    T item = (T) o;
                    out.add(item);
                } catch (ClassCastException ignore) {
                }
            }
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return List.of();
    }

    private String nvl(String s) { 
        return s == null ? "" : s; 
    }

    // Métodos simples para conversión Date <-> LocalDate (para JavaFX DatePicker)
    private LocalDate dateToLocalDate(Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private Date localDateToDate(LocalDate localDate) {
        if (localDate == null) return null;
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
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