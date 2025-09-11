package cr.ac.una.tareaprogramacion3.model;

import javafx.beans.property.*;
import java.util.Date;

/** Modelo para Seguimientos de proyecto. */
public class SeguimientoModel {

    private final ObjectProperty<Long> id = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Long> proyectoId = new SimpleObjectProperty<>(null);

    private final ObjectProperty<Date> fecha = new SimpleObjectProperty<>(null);
    private final StringProperty observaciones = new SimpleStringProperty("");

    private final IntegerProperty porcentajeAvance = new SimpleIntegerProperty(0);
    private final StringProperty porcentajeAvanceStr = new SimpleStringProperty("0%");

    public SeguimientoModel() {
        porcentajeAvanceStr.bind(porcentajeAvance.asString().concat("%"));
    }

    public Long getId() { return id.get(); }
    public void setId(Long v) { id.set(v); }
    public ObjectProperty<Long> idProperty() { return id; }

    public Long getProyectoId() { return proyectoId.get(); }
    public void setProyectoId(Long v) { proyectoId.set(v); }
    public ObjectProperty<Long> proyectoIdProperty() { return proyectoId; }

    public Date getFecha() { return fecha.get(); }
    public void setFecha(Date d) { fecha.set(d); }
    public ObjectProperty<Date> fechaProperty() { return fecha; }

    public String getObservaciones() { return observaciones.get(); }
    public void setObservaciones(String v) { observaciones.set(v); }
    public StringProperty observacionesProperty() { return observaciones; }

    public int getPorcentajeAvance() { return porcentajeAvance.get(); }
    public void setPorcentajeAvance(int v) { porcentajeAvance.set(v); }
    public IntegerProperty porcentajeAvanceProperty() { return porcentajeAvance; }

    public String getPorcentajeAvanceStr() { return porcentajeAvanceStr.get(); }
    public StringProperty porcentajeAvanceStrProperty() { return porcentajeAvanceStr; }
}

