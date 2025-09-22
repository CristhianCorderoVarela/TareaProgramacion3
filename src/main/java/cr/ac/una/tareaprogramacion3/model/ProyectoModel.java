package cr.ac.una.tareaprogramacion3.model;

import javafx.beans.property.*;
import java.util.Date;

/** Modelo para Proyectos. */
public class ProyectoModel {

    private final ObjectProperty<Long> id = new SimpleObjectProperty<>(null);
    private final StringProperty nombre = new SimpleStringProperty("");

    
    private final StringProperty patrocinadorNombre = new SimpleStringProperty("");
    private final StringProperty patrocinadorCorreo = new SimpleStringProperty("");

    private final StringProperty liderUsuarioNombre = new SimpleStringProperty("");
    private final StringProperty liderUsuarioCorreo = new SimpleStringProperty("");

    
    private final StringProperty liderTecnicoNombre = new SimpleStringProperty("");
    private final StringProperty liderTecnicoCorreo = new SimpleStringProperty("");

    
    private final ObjectProperty<Date> fechaInicioPlanificada = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Date> fechaFinalPlanificada = new SimpleObjectProperty<>(null);

    
    private final ObjectProperty<Date> fechaInicioReal = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Date> fechaFinalReal = new SimpleObjectProperty<>(null);

    private final StringProperty estado = new SimpleStringProperty(""); 

    
    private final IntegerProperty porcentajeAvance = new SimpleIntegerProperty(0);

    
    private final StringProperty fechaInicioPlanificadaStr = new SimpleStringProperty("");
    private final StringProperty fechaFinalPlanificadaStr = new SimpleStringProperty("");
    private final StringProperty porcentajeAvanceStr = new SimpleStringProperty("0%");

    public ProyectoModel() {
        porcentajeAvanceStr.bind(porcentajeAvance.asString().concat("%"));
    }

    
    public Long getId() { return id.get(); }
    public void setId(Long v) { id.set(v); }
    public ObjectProperty<Long> idProperty() { return id; }

    
    public String getNombre() { return nombre.get(); }
    public void setNombre(String v) { nombre.set(v); }
    public StringProperty nombreProperty() { return nombre; }

    
    public String getPatrocinadorNombre() { return patrocinadorNombre.get(); }
    public void setPatrocinadorNombre(String v) { patrocinadorNombre.set(v); }
    public StringProperty patrocinadorNombreProperty() { return patrocinadorNombre; }

    public String getPatrocinadorCorreo() { return patrocinadorCorreo.get(); }
    public void setPatrocinadorCorreo(String v) { patrocinadorCorreo.set(v); }
    public StringProperty patrocinadorCorreoProperty() { return patrocinadorCorreo; }

    
    public String getLiderUsuarioNombre() { return liderUsuarioNombre.get(); }
    public void setLiderUsuarioNombre(String v) { liderUsuarioNombre.set(v); }
    public StringProperty liderUsuarioNombreProperty() { return liderUsuarioNombre; }

    public String getLiderUsuarioCorreo() { return liderUsuarioCorreo.get(); }
    public void setLiderUsuarioCorreo(String v) { liderUsuarioCorreo.set(v); }
    public StringProperty liderUsuarioCorreoProperty() { return liderUsuarioCorreo; }

    
    public String getLiderTecnicoNombre() { return liderTecnicoNombre.get(); }
    public void setLiderTecnicoNombre(String v) { liderTecnicoNombre.set(v); }
    public StringProperty liderTecnicoNombreProperty() { return liderTecnicoNombre; }

    public String getLiderTecnicoCorreo() { return liderTecnicoCorreo.get(); }
    public void setLiderTecnicoCorreo(String v) { liderTecnicoCorreo.set(v); }
    public StringProperty liderTecnicoCorreoProperty() { return liderTecnicoCorreo; }

    
    public Date getFechaInicioPlanificada() { return fechaInicioPlanificada.get(); }
    public void setFechaInicioPlanificada(Date d) { fechaInicioPlanificada.set(d); }
    public ObjectProperty<Date> fechaInicioPlanificadaProperty() { return fechaInicioPlanificada; }

    public Date getFechaFinalPlanificada() { return fechaFinalPlanificada.get(); }
    public void setFechaFinalPlanificada(Date d) { fechaFinalPlanificada.set(d); }
    public ObjectProperty<Date> fechaFinalPlanificadaProperty() { return fechaFinalPlanificada; }

    
    public Date getFechaInicioReal() { return fechaInicioReal.get(); }
    public void setFechaInicioReal(Date d) { fechaInicioReal.set(d); }
    public ObjectProperty<Date> fechaInicioRealProperty() { return fechaInicioReal; }

    public Date getFechaFinalReal() { return fechaFinalReal.get(); }
    public void setFechaFinalReal(Date d) { fechaFinalReal.set(d); }
    public ObjectProperty<Date> fechaFinalRealProperty() { return fechaFinalReal; }

    
    public String getEstado() { return estado.get(); }
    public void setEstado(String v) { estado.set(v); }
    public StringProperty estadoProperty() { return estado; }

    
    public int getPorcentajeAvance() { return porcentajeAvance.get(); }
    public void setPorcentajeAvance(int v) { porcentajeAvance.set(v); }
    public IntegerProperty porcentajeAvanceProperty() { return porcentajeAvance; }

    
    public String getFechaInicioPlanificadaStr() { return fechaInicioPlanificadaStr.get(); }
    public void setFechaInicioPlanificadaStr(String v) { fechaInicioPlanificadaStr.set(v); }
    public StringProperty fechaInicioPlanificadaStrProperty() { return fechaInicioPlanificadaStr; }

    public String getFechaFinalPlanificadaStr() { return fechaFinalPlanificadaStr.get(); }
    public void setFechaFinalPlanificadaStr(String v) { fechaFinalPlanificadaStr.set(v); }
    public StringProperty fechaFinalPlanificadaStrProperty() { return fechaFinalPlanificadaStr; }

    public String getPorcentajeAvanceStr() { return porcentajeAvanceStr.get(); }
    public StringProperty porcentajeAvanceStrProperty() { return porcentajeAvanceStr; }
}
