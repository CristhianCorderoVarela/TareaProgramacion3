package cr.ac.una.tareaprogramacion3.controller;

import cr.ac.una.client.soap.ProyectoDto;
import cr.ac.una.client.soap.ProyectoService;
import cr.ac.una.client.soap.ProyectoWS;
import cr.ac.una.client.soap.RespuestaGeneral;
import jakarta.xml.ws.BindingProvider;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.GregorianCalendar;
import java.util.Map;

public class ProyectoDialogController {

    @FXML private TextField txtNombre;
    @FXML private ComboBox<String> cmbEstado;
    @FXML private TextField txtPatrocinador;
    @FXML private TextField txtLiderUsuario;
    @FXML private TextField txtLiderTecnico;
    @FXML private TextArea  txtCorreos; // visual solamente; el DTO no tiene este campo
    @FXML private DatePicker dpInicioPlan, dpFinPlan, dpInicioReal, dpFinReal;

    private ProyectoWS port;
    private ProyectoDto modelo;       // dto que se edita/crea
    private boolean guardado = false; // indica si se guardó correctamente

    // Configurable desde el caller (Ventana1Controller)
    public void init(String endpointUrl, ProyectoDto dto) {
        ProyectoService svc = new ProyectoService();
        this.port = svc.getProyectoWSPort();
        Map<String,Object> ctx = ((BindingProvider) port).getRequestContext();
        ctx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointUrl);

        this.modelo = (dto != null) ? dto : new ProyectoDto();

        // poblar UI
        cmbEstado.getItems().setAll("PLANIFICADO", "EN_CURSO", "EN_PAUSA", "FINALIZADO");
        txtNombre.setText(nv(modelo.getNombre()));
        cmbEstado.getSelectionModel().select(nv(modelo.getEstado()).isEmpty() ? "PLANIFICADO" : modelo.getEstado());
        txtPatrocinador.setText(nv(modelo.getPatrocinadorNombre()));
        txtLiderUsuario.setText(nv(modelo.getLiderUsuarioNombre()));
        txtLiderTecnico.setText(nv(modelo.getLiderTecnicoNombre()));

        setDate(dpInicioPlan, modelo.getFechaInicioPlanificada());
        setDate(dpFinPlan,    modelo.getFechaFinalPlanificada());
        setDate(dpInicioReal, modelo.getFechaInicioReal());
        setDate(dpFinReal,    modelo.getFechaFinalReal());
    }

    private String nv(String s){ return s==null?"":s; }

    private void setDate(DatePicker dp, XMLGregorianCalendar xcal){
        if (dp==null) return;
        if (xcal==null){ dp.setValue(null); return;}
        LocalDate ld = xcal.toGregorianCalendar().toZonedDateTime().toLocalDate();
        dp.setValue(ld);
    }

    private XMLGregorianCalendar toXml(LocalDate ld){
        if (ld==null) return null;
        try {
            GregorianCalendar gc = GregorianCalendar.from(ld.atStartOfDay(ZoneId.systemDefault()));
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
        } catch (DatatypeConfigurationException e) {
            return null;
        }
    }

    @FXML
    private void onGuardar() {
        // Validaciones mínimas
        if (txtNombre.getText()==null || txtNombre.getText().isBlank()){
            alerta("Validación", "El nombre es obligatorio."); return;
        }
        if (cmbEstado.getValue()==null){
            alerta("Validación", "Seleccione un estado."); return;
        }

        // Pasar UI -> DTO
        modelo.setNombre(txtNombre.getText().trim());
        modelo.setEstado(cmbEstado.getValue());
        modelo.setPatrocinadorNombre(nv(txtPatrocinador.getText()).trim());
        modelo.setLiderUsuarioNombre(nv(txtLiderUsuario.getText()).trim());
        modelo.setLiderTecnicoNombre(nv(txtLiderTecnico.getText()).trim());
        // El DTO no expone 'correos' -> se omite

        modelo.setFechaInicioPlanificada(toXml(dpInicioPlan.getValue()));
        modelo.setFechaFinalPlanificada(toXml(dpFinPlan.getValue()));
        modelo.setFechaInicioReal(toXml(dpInicioReal.getValue()));
        modelo.setFechaFinalReal(toXml(dpFinReal.getValue()));

        // Firmas reales del stub:
        // crearProyecto(ProyectoDto, Long)   ← requiere id de administrador
        // actualizarProyecto(ProyectoDto)    ← solo el dto
        Long idAdministrador = 1L; // TODO: reemplazar por el id real de tu sesión

        RespuestaGeneral r;
        if (modelo.getId()==null){
            r = port.crearProyecto(modelo, idAdministrador);
        } else {
            r = port.actualizarProyecto(modelo);
        }

        if (r==null || !Boolean.TRUE.equals(r.isOk())){
            error("Error", (r==null) ? "Sin respuesta del servidor" : nv(r.getMensaje()));
            return;
        }

        guardado = true;
        cerrar();
    }

    @FXML
    private void onCancelar() {
        guardado = false;
        cerrar();
    }

    private void cerrar(){
        Stage st = (Stage) txtNombre.getScene().getWindow();
        st.close();
    }

    public boolean isGuardado(){ return guardado; }

    private void alerta(String titulo, String msg){
        var a = new Alert(Alert.AlertType.WARNING);
        a.setHeaderText(titulo);
        a.setContentText(msg);
        a.showAndWait();
    }
    private void error(String titulo, String msg){
        var a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(titulo);
        a.setContentText(msg);
        a.showAndWait();
    }
}
