package cr.ac.una.tareaprogramacion3.model;

import javafx.beans.property.*;
import java.util.Date;

/** Modelo para Actividades de un proyecto. */
public class ActividadModel {

    private final ObjectProperty<Long> id = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Long> proyectoId = new SimpleObjectProperty<>(null);

    private final IntegerProperty orden = new SimpleIntegerProperty(0); // orden de ejecución
    private final StringProperty descripcion = new SimpleStringProperty("");

    private final StringProperty encargado = new SimpleStringProperty("");
    private final StringProperty correoEncargado = new SimpleStringProperty("");

    private final StringProperty estado = new SimpleStringProperty(""); // PLANIFICADA/EN_CURSO/POSTERGADA/FINALIZADA

    // Fechas planificadas
    private final ObjectProperty<Date> fechaInicioPlanificada = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Date> fechaFinalPlanificada = new SimpleObjectProperty<>(null);

    // Fechas reales
    private final ObjectProperty<Date> fechaInicioReal = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Date> fechaFinalReal = new SimpleObjectProperty<>(null);

    // Apoyo para tabla (texto)
    private final StringProperty fechaInicioPlanificadaStr = new SimpleStringProperty("");
    private final StringProperty fechaFinalPlanificadaStr = new SimpleStringProperty("");

    // id
    public Long getId() { return id.get(); }
    public void setId(Long v) { id.set(v); }
    public ObjectProperty<Long> idProperty() { return id; }

    public Long getProyectoId() { return proyectoId.get(); }
    public void setProyectoId(Long v) { proyectoId.set(v); }
    public ObjectProperty<Long> proyectoIdProperty() { return proyectoId; }

    public int getOrden() { return orden.get(); }
    public void setOrden(int v) { orden.set(v); }
    public IntegerProperty ordenProperty() { return orden; }

    public String getDescripcion() { return descripcion.get(); }
    public void setDescripcion(String v) { descripcion.set(v); }
    public StringProperty descripcionProperty() { return descripcion; }

    public String getEncargado() { return encargado.get(); }
    public void setEncargado(String v) { encargado.set(v); }
    public StringProperty encargadoProperty() { return encargado; }

    public String getCorreoEncargado() { return correoEncargado.get(); }
    public void setCorreoEncargado(String v) { correoEncargado.set(v); }
    public StringProperty correoEncargadoProperty() { return correoEncargado; }

    public String getEstado() { return estado.get(); }
    public void setEstado(String v) { estado.set(v); }
    public StringProperty estadoProperty() { return estado; }

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

    public String getFechaInicioPlanificadaStr() { return fechaInicioPlanificadaStr.get(); }
    public void setFechaInicioPlanificadaStr(String v) { fechaInicioPlanificadaStr.set(v); }
    public StringProperty fechaInicioPlanificadaStrProperty() { return fechaInicioPlanificadaStr; }

    public String getFechaFinalPlanificadaStr() { return fechaFinalPlanificadaStr.get(); }
    public void setFechaFinalPlanificadaStr(String v) { fechaFinalPlanificadaStr.set(v); }
    public StringProperty fechaFinalPlanificadaStrProperty() { return fechaFinalPlanificadaStr; }
}
