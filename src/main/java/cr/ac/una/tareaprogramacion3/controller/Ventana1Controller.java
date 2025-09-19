package cr.ac.una.tareaprogramacion3.controller;

import cr.ac.una.client.soap.ProyectoDto;
import cr.ac.una.client.soap.ProyectoService;
import cr.ac.una.client.soap.ProyectoWS;
import cr.ac.una.client.soap.ReporteWS;
import cr.ac.una.client.soap.ReporteWSService;
import cr.ac.una.client.soap.RespuestaExcel;
import cr.ac.una.client.soap.RespuestaGeneralLista;

import cr.ac.una.tareaprogramacion3.service.ExcelExportService;
import cr.ac.una.tareaprogramacion3.util.AppEvents;
import cr.ac.una.tareaprogramacion3.util.Controller;
import cr.ac.una.tareaprogramacion3.util.FlowController;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.soap.SOAPBinding;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Ventana1Controller extends Controller {

    // ======= Búsqueda (versión nueva) =======
    @FXML private TextField txtBuscar;

    // ======= Listas por estado =======
    @FXML private ListView<ProyectoDto> listaPlanificados;
    @FXML private ListView<ProyectoDto> listaEnProceso;
    @FXML private ListView<ProyectoDto> listaSuspendidos;
    @FXML private ListView<ProyectoDto> listaFinalizados;

    private final ObservableList<ProyectoDto> dataPlanificados = FXCollections.observableArrayList();
    private final ObservableList<ProyectoDto> dataEnProceso    = FXCollections.observableArrayList();
    private final ObservableList<ProyectoDto> dataSuspendidos  = FXCollections.observableArrayList();
    private final ObservableList<ProyectoDto> dataFinalizados  = FXCollections.observableArrayList();

    // ======= Controles Excel (opcionales en FXML) =======
    @FXML private Button btnGenerarExcel;
    @FXML private ProgressIndicator progressExcel;
    @FXML private Label lblEstadoExcel;
    @FXML private Label lblProyectoSeleccionado;

    // ======= WS Ports =======
    private ProyectoWS proyectoPort;
    private ReporteWS  reportePort;

    // ======= Endpoints =======
    private static final String PROYECTO_ENDPOINT = "http://localhost:8080/ProyectoService/ProyectoWS";
    private static final String REPORTE_ENDPOINT  = "http://localhost:8080/ReporteWSService/ReporteWS";

    // Cache para búsqueda (versión nueva)
    private List<ProyectoDto> cache = new ArrayList<>();

    // Proyecto marcado con Excel generado vigente (para resaltar)
    private ProyectoDto proyectoConExcelGenerado = null;

    private final SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void initialize() {
        // Puertos
        crearProyectoPort(PROYECTO_ENDPOINT);
        crearReportePort(REPORTE_ENDPOINT);

        // Listas
        prepararLista(listaPlanificados, dataPlanificados);
        prepararLista(listaEnProceso,    dataEnProceso);
        prepararLista(listaSuspendidos,  dataSuspendidos);
        prepararLista(listaFinalizados,  dataFinalizados);

        // Búsqueda
        if (txtBuscar != null) {
            txtBuscar.textProperty().addListener((obs, ov, nv) -> aplicarFiltro());
        }

        // Excel UI estado inicial (si existen en FXML)
        if (btnGenerarExcel != null) btnGenerarExcel.setDisable(true);
        if (progressExcel != null)   progressExcel.setVisible(false);
        if (lblEstadoExcel != null)  lblEstadoExcel.setVisible(false);
        if (lblProyectoSeleccionado != null) lblProyectoSeleccionado.setText("Ninguno");

        // Suscripción: cuando Ventana3 u otros actualizan un proyecto, refrescamos
        AppEvents.addProyectoListener(id -> Platform.runLater(this::cargarTodos));

        cargarTodos();
    }

    // ===================== Puertos SOAP =====================
    private void crearProyectoPort(String endpointUrl) {
        ProyectoService svc = new ProyectoService();
        this.proyectoPort = svc.getProyectoWSPort();
        ((BindingProvider) proyectoPort).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);
    }

    private void crearReportePort(String endpointUrl) {
        ReporteWSService svc = new ReporteWSService();
        this.reportePort = svc.getReporteWSPort();
        ((BindingProvider) reportePort).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);
        // MTOM para transferir el byte[] del Excel eficientemente
        SOAPBinding binding = (SOAPBinding) ((BindingProvider) reportePort).getBinding();
        binding.setMTOMEnabled(true);
    }

    // ===================== Listas y celdas =====================
    private void prepararLista(ListView<ProyectoDto> lv, ObservableList<ProyectoDto> backing) {
        if (lv == null) return;

        lv.setItems(backing);
        lv.setFixedCellSize(-1);

        lv.setCellFactory(view -> new ListCell<>() {
            private final Label l1 = new Label();
            private final Label l2 = new Label();
            private final Label l3 = new Label();
            private final VBox box = new VBox(2, l1, l2, l3);

            {
                l1.setWrapText(true);
                l2.setWrapText(true);
                l3.setWrapText(true);

                box.maxWidthProperty().bind(lv.widthProperty().subtract(24));
                l1.maxWidthProperty().bind(box.maxWidthProperty());
                l2.maxWidthProperty().bind(box.maxWidthProperty());
                l3.maxWidthProperty().bind(box.maxWidthProperty());

                l1.setStyle("-fx-font-weight: bold;");
                box.setFillWidth(true);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(ProyectoDto p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) {
                    setGraphic(null);
                    setStyle("");
                } else {
                    l1.setText(headerText(p));
                    l2.setText(leadersText(p));
                    l3.setText(datesText(p));
                    setGraphic(box);

                    // Resaltar si es el proyecto cuyo Excel está "vigente"
                    if (proyectoConExcelGenerado != null &&
                        p.getId() != null &&
                        p.getId().equals(proyectoConExcelGenerado.getId())) {
                        setStyle("-fx-background-color: #d5f4e6; -fx-border-color: #27ae60; -fx-border-width: 1px;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // Doble click -> editar
        lv.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                ProyectoDto sel = lv.getSelectionModel().getSelectedItem();
                if (sel != null) onEditarSeleccion();
            }
        });

        // Selección: habilitar botón Excel, mostrar nombre
        lv.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) {
                limpiarOtrasSelecciones(lv);
                actualizarProyectoSeleccionado(n);
            }
        });
    }

    private void limpiarOtrasSelecciones(ListView<ProyectoDto> exceptoEsta) {
        if (listaPlanificados != null && listaPlanificados != exceptoEsta) listaPlanificados.getSelectionModel().clearSelection();
        if (listaEnProceso    != null && listaEnProceso    != exceptoEsta) listaEnProceso.getSelectionModel().clearSelection();
        if (listaSuspendidos  != null && listaSuspendidos  != exceptoEsta) listaSuspendidos.getSelectionModel().clearSelection();
        if (listaFinalizados  != null && listaFinalizados  != exceptoEsta) listaFinalizados.getSelectionModel().clearSelection();
    }

    private void actualizarProyectoSeleccionado(ProyectoDto proyecto) {
        if (proyecto != null) {
            if (lblProyectoSeleccionado != null) lblProyectoSeleccionado.setText(nvl(proyecto.getNombre()));
            if (btnGenerarExcel != null)         btnGenerarExcel.setDisable(false);
            verificarEstadoExcel(proyecto);
        } else {
            if (lblProyectoSeleccionado != null) lblProyectoSeleccionado.setText("Ninguno");
            if (btnGenerarExcel != null)         btnGenerarExcel.setDisable(true);
            if (lblEstadoExcel != null)          lblEstadoExcel.setVisible(false);
        }
    }

    private void verificarEstadoExcel(ProyectoDto proyecto) {
        if (lblEstadoExcel == null) return;
        if (proyectoConExcelGenerado != null &&
            proyecto.getId() != null &&
            proyecto.getId().equals(proyectoConExcelGenerado.getId())) {
            lblEstadoExcel.setText("Excel disponible");
            lblEstadoExcel.setStyle("-fx-text-fill: #27ae60; -fx-font-style: italic;");
            lblEstadoExcel.setVisible(true);
        } else {
            lblEstadoExcel.setVisible(false);
        }
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

    // ===================== Datos / Filtro =====================
    private void cargarTodos() {
        Task<List<ProyectoDto>> task = new Task<>() {
            @Override protected List<ProyectoDto> call() {
                RespuestaGeneralLista r = proyectoPort.obtenerTodosProyectos();
                if (r == null || !Boolean.TRUE.equals(r.isOk()))
                    throw new RuntimeException(r == null ? "Sin respuesta del servidor" : nvl(r.getMensaje()));
                return extraerProyectos(r);
            }
        };
        task.setOnSucceeded(e -> {
            cache = task.getValue();
            aplicarFiltro();
        });
        task.setOnFailed(e -> mostrarError("No se pudieron cargar los proyectos", task.getException()));
        new Thread(task, "ws-cargar-todos").start();
    }

    private List<ProyectoDto> extraerProyectos(RespuestaGeneralLista r) {
        if (r == null || r.getItems() == null || r.getItems().getItem() == null) return List.of();
        List<Object> raw = r.getItems().getItem();
        return raw.stream()
                .filter(ProyectoDto.class::isInstance)
                .map(ProyectoDto.class::cast)
                .collect(Collectors.toList());
    }

    private void aplicarFiltro() {
        String q = (txtBuscar == null || txtBuscar.getText() == null) ? "" : txtBuscar.getText().trim().toLowerCase();
        List<ProyectoDto> base = cache == null ? List.of() : cache;

        List<ProyectoDto> filtrados = q.isEmpty()
                ? base
                : base.stream().filter(p -> matches(p, q)).collect(Collectors.toList());

        distribuirPorEstado(filtrados);
    }

    private boolean matches(ProyectoDto p, String q) {
        if (p == null) return false;

        if (contains(p.getNombre(), q)) return true;
        if (contains(p.getEstado(), q)) return true;
        if (contains(p.getDescripcion(), q)) return true;

        if (contains(p.getPatrocinadorNombre(), q)) return true;
        if (contains(p.getPatrocinadorCorreo(), q)) return true;

        if (contains(p.getLiderUsuarioNombre(), q)) return true;
        if (contains(p.getLiderUsuarioCorreo(), q)) return true;

        if (contains(p.getLiderTecnicoNombre(), q)) return true;
        if (contains(p.getLiderTecnicoCorreo(), q)) return true;

        if (contains(p.getCreadoPorNombre(), q)) return true;

        if (p.getPorcentajeAvance() != null && String.valueOf(p.getPorcentajeAvance()).contains(q)) return true;

        if (contains(fmtFecha(p.getFechaInicioPlanificada()), q)) return true;
        if (contains(fmtFecha(p.getFechaFinalPlanificada()), q)) return true;
        if (contains(fmtFecha(p.getFechaInicioReal()), q)) return true;
        if (contains(fmtFecha(p.getFechaFinalReal()), q)) return true;

        return false;
    }

    private boolean contains(String s, String q) {
        return s != null && q != null && s.toLowerCase().contains(q);
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

    // ===================== Acciones =====================
    @FXML private void onNuevoProyecto() { abrirDialogoViaFlow(null); }

    @FXML private void onRefrescar() { cargarTodos(); }

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

    private void abrirDialogoViaFlow(ProyectoDto dto) {
        try {
            FlowController fc = FlowController.getInstance();
            ProyectoDialogController dialogCtrl =
                    (ProyectoDialogController) fc.getController("ProyectoDialog");
            dialogCtrl.init(PROYECTO_ENDPOINT, dto);
            fc.goViewInWindowModal("ProyectoDialog", getStage(), false);

            if (dialogCtrl.isGuardado()) {
                cargarTodos();

                // Si se modificó el proyecto que tenía Excel, avisar y desmarcar como vigente
                if (dto != null && proyectoConExcelGenerado != null &&
                    dto.getId() != null && dto.getId().equals(proyectoConExcelGenerado.getId())) {
                    mostrarAviso("Excel desactualizado",
                            "El proyecto ha sido modificado. Se recomienda regenerar el cronograma Excel.");
                    proyectoConExcelGenerado = null;
                    refrescarCeldasListas();
                    if (lblEstadoExcel != null) {
                        lblEstadoExcel.setText("Excel desactualizado");
                        lblEstadoExcel.setStyle("-fx-text-fill: #e67e22; -fx-font-style: italic;");
                        lblEstadoExcel.setVisible(true);
                    }
                }
            }
        } catch (Exception ex) {
            mostrarError("No se pudo abrir el diálogo", ex);
        }
    }

    // ===================== Excel: Generar/Guardar =====================
    @FXML
    private void onGenerarExcel() {
        ProyectoDto sel = obtenerSeleccion();
        if (sel == null) {
            alerta("Seleccione un proyecto para generar el cronograma en Excel.");
            return;
        }

        // UI progreso
        if (btnGenerarExcel != null) btnGenerarExcel.setDisable(true);
        if (progressExcel != null)   progressExcel.setVisible(true);
        if (lblEstadoExcel != null) {
            lblEstadoExcel.setText("Generando Excel...");
            lblEstadoExcel.setStyle("-fx-text-fill: #f39c12; -fx-font-style: italic;");
            lblEstadoExcel.setVisible(true);
        }

        Task<RespuestaExcel> task = new Task<>() {
            @Override
            protected RespuestaExcel call() {
                return reportePort.generarCronogramaProyecto(sel.getId());
            }
        };

        task.setOnSucceeded(e -> {
            RespuestaExcel respuesta = task.getValue();
            Platform.runLater(() -> {
                if (progressExcel != null) progressExcel.setVisible(false);
                if (btnGenerarExcel != null) btnGenerarExcel.setDisable(false);

                if (respuesta != null && respuesta.getArchivoExcel() != null && respuesta.getArchivoExcel().length > 0) {
                    // Éxito: marcar y resaltar
                    proyectoConExcelGenerado = sel;
                    if (lblEstadoExcel != null) {
                        lblEstadoExcel.setText("Excel generado exitosamente");
                        lblEstadoExcel.setStyle("-fx-text-fill: #27ae60; -fx-font-style: italic;");
                        lblEstadoExcel.setVisible(true);
                    }
                    refrescarCeldasListas();

                    // Guardar archivo (con manejo de archivo abierto / sobrescritura)
                    guardarArchivoExcel(respuesta.getArchivoExcel(), respuesta.getNombreArchivo());
                } else {
                    if (lblEstadoExcel != null) {
                        lblEstadoExcel.setText("Error al generar Excel");
                        lblEstadoExcel.setStyle("-fx-text-fill: #e74c3c; -fx-font-style: italic;");
                        lblEstadoExcel.setVisible(true);
                    }
                    mostrarError("Error al generar Excel",
                            new Exception(respuesta != null ? nvl(respuesta.getMensaje()) : "Respuesta nula"));
                }
            });
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                if (progressExcel != null) progressExcel.setVisible(false);
                if (btnGenerarExcel != null) btnGenerarExcel.setDisable(false);
                if (lblEstadoExcel != null) {
                    lblEstadoExcel.setText("Error al generar Excel");
                    lblEstadoExcel.setStyle("-fx-text-fill: #e74c3c; -fx-font-style: italic;");
                    lblEstadoExcel.setVisible(true);
                }
                mostrarError("Error al generar Excel", task.getException());
            });
        });

        new Thread(task, "generar-excel").start();
    }

    private void guardarArchivoExcel(byte[] excelBytes, String nombreArchivo) {
        if (excelBytes == null || excelBytes.length == 0) {
            alerta("No se recibieron datos del Excel.");
            return;
        }

        String dir = ExcelExportService.obtenerDirectorioDescargas();
        String nombreLimpio = ExcelExportService.limpiarNombreArchivo(nombreArchivo);
        String ruta = dir + File.separator + nombreLimpio;

        boolean existe = ExcelExportService.archivoExiste(ruta);
        boolean bloqueado = existe && ExcelExportService.estaBloqueado(ruta);

        if (!existe) {
            boolean ok = ExcelExportService.guardarExcelEn(ruta, excelBytes);
            if (ok) mostrarInfo("Excel guardado", "Se guardó en:\n" + ruta);
            else    mostrarError("No se pudo guardar el archivo", new Exception("Error de escritura en disco."));
            return;
        }

        if (bloqueado) {
            Alert a = new Alert(Alert.AlertType.CONFIRMATION);
            a.setTitle("Archivo en uso");
            a.setHeaderText("El archivo está abierto en otro programa.");
            a.setContentText("Cierra el archivo para sobrescribir, o crea una copia/elige otra ubicación.");
            ButtonType btnCopia = new ButtonType("Crear copia");
            ButtonType btnGuardarComo = new ButtonType("Guardar como…");
            ButtonType btnCancelar = ButtonType.CANCEL;
            a.getButtonTypes().setAll(btnCopia, btnGuardarComo, btnCancelar);

            a.showAndWait().ifPresent(bt -> {
                if (bt == btnCopia) {
                    String copia = ExcelExportService.proponerNombreCopia(ruta);
                    boolean ok = ExcelExportService.guardarExcelEn(copia, excelBytes);
                    if (ok) mostrarInfo("Excel guardado (copia)", "Se guardó en:\n" + copia);
                    else    mostrarError("No se pudo guardar la copia", new Exception("Error de escritura en disco."));
                } else if (bt == btnGuardarComo) {
                    Task<Boolean> saveTask = ExcelExportService.crearTareaDescargaExcel(getStage(), excelBytes, nombreLimpio);
                    saveTask.setOnSucceeded(e -> {
                        if (Boolean.TRUE.equals(saveTask.getValue())) {
                            mostrarInfo("Excel guardado", "Archivo guardado exitosamente.");
                        }
                    });
                    saveTask.setOnFailed(e -> mostrarError("Error al guardar", saveTask.getException()));
                    new Thread(saveTask, "guardar-excel-dialog").start();
                }
            });
            return;
        }

        // Existe y NO está bloqueado → preguntar sobrescritura
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Sobrescribir archivo");
        confirm.setHeaderText("El archivo ya existe:");
        confirm.setContentText(ruta + "\n\n¿Desea sobrescribirlo?");
        ButtonType btnSi = new ButtonType("Sobrescribir");
        ButtonType btnNo = new ButtonType("Guardar como…");
        ButtonType btnCancelar = ButtonType.CANCEL;
        confirm.getButtonTypes().setAll(btnSi, btnNo, btnCancelar);

        confirm.showAndWait().ifPresent(bt -> {
            if (bt == btnSi) {
                boolean ok = ExcelExportService.guardarExcelEn(ruta, excelBytes);
                if (ok) mostrarInfo("Excel guardado", "Se sobrescribió:\n" + ruta);
                else    mostrarError("No se pudo sobrescribir", new Exception("Error de escritura en disco."));
            } else if (bt == btnNo) {
                Task<Boolean> saveTask = ExcelExportService.crearTareaDescargaExcel(getStage(), excelBytes, nombreLimpio);
                saveTask.setOnSucceeded(e -> {
                    if (Boolean.TRUE.equals(saveTask.getValue())) {
                        mostrarInfo("Excel guardado", "Archivo guardado exitosamente.");
                    }
                });
                saveTask.setOnFailed(e -> mostrarError("Error al guardar", saveTask.getException()));
                new Thread(saveTask, "guardar-excel-dialog").start();
            }
        });
    }

    // ===================== Integración con Ventana3 =====================
    /** Ventana3 la puede llamar cuando crea/edita una actividad del proyecto. */
    public void notificarActividadCreada(Long proyectoId) {
        if (proyectoConExcelGenerado != null && proyectoId != null &&
            proyectoId.equals(proyectoConExcelGenerado.getId())) {
            mostrarAviso("Excel desactualizado",
                    "Se ha agregado una nueva actividad al proyecto. El cronograma Excel necesita ser actualizado.");
            proyectoConExcelGenerado = null; // marcar como desactualizado
            refrescarCeldasListas();
            if (lblEstadoExcel != null) {
                lblEstadoExcel.setText("Excel desactualizado");
                lblEstadoExcel.setStyle("-fx-text-fill: #e67e22; -fx-font-style: italic;");
                lblEstadoExcel.setVisible(true);
            }
        }
    }

    private void refrescarCeldasListas() {
        if (listaPlanificados != null) listaPlanificados.refresh();
        if (listaEnProceso    != null) listaEnProceso.refresh();
        if (listaSuspendidos  != null) listaSuspendidos.refresh();
        if (listaFinalizados  != null) listaFinalizados.refresh();
    }

    // ===================== Utilitarios =====================
    private String fmtFecha(Object d) {
        if (d == null) return null;
        try {
            if (d instanceof java.util.Date) return df.format((java.util.Date) d);
            if (d instanceof XMLGregorianCalendar xgc) {
                return df.format(Objects.requireNonNull(xgc.toGregorianCalendar()).getTime());
            }
        } catch (Exception ignore) {}
        return null;
    }

    private String nvl(String s){ return s==null?"":s.trim(); }

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

    private void mostrarInfo(String titulo, String msg) {
        var a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Información");
        a.setHeaderText(titulo);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void mostrarAviso(String titulo, String msg) {
        var a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Aviso");
        a.setHeaderText(titulo);
        a.setContentText(msg);
        a.showAndWait();
    }
}
