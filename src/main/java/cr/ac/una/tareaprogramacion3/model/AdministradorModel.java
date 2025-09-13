package cr.ac.una.tareaprogramacion3.model;

import javafx.beans.property.*;
import java.util.Date;


public class AdministradorModel {

    private final ObjectProperty<Long> id = new SimpleObjectProperty<>(null);
    private final StringProperty nombre = new SimpleStringProperty("");
    private final StringProperty apellidos = new SimpleStringProperty("");
    private final StringProperty cedula = new SimpleStringProperty("");
    private final StringProperty correo = new SimpleStringProperty("");
    private final StringProperty usuario = new SimpleStringProperty("");
    private final StringProperty estado = new SimpleStringProperty(""); 
    private final ObjectProperty<Date> fechaCreacion = new SimpleObjectProperty<>(null);
    private final ObjectProperty<Date> fechaModificacion = new SimpleObjectProperty<>(null);

    // --- Propiedad útil para tabla: nombre + apellidos
    private final StringProperty nombreApellidos = new SimpleStringProperty("");

    public AdministradorModel() {
        nombreApellidos.bind(nombre.concat(" ").concat(apellidos));
    }

    // Getters/Setters simples
    public Long getId() { return id.get(); }
    public void setId(Long v) { id.set(v); }
    public ObjectProperty<Long> idProperty() { return id; }

    public String getNombre() { return nombre.get(); }
    public void setNombre(String v) { nombre.set(v); }
    public StringProperty nombreProperty() { return nombre; }

    public String getApellidos() { return apellidos.get(); }
    public void setApellidos(String v) { apellidos.set(v); }
    public StringProperty apellidosProperty() { return apellidos; }

    public String getCedula() { return cedula.get(); }
    public void setCedula(String v) { cedula.set(v); }
    public StringProperty cedulaProperty() { return cedula; }

    public String getCorreo() { return correo.get(); }
    public void setCorreo(String v) { correo.set(v); }
    public StringProperty correoProperty() { return correo; }

    public String getUsuario() { return usuario.get(); }
    public void setUsuario(String v) { usuario.set(v); }
    public StringProperty usuarioProperty() { return usuario; }

    public String getEstado() { return estado.get(); }
    public void setEstado(String v) { estado.set(v); }
    public StringProperty estadoProperty() { return estado; }

    public Date getFechaCreacion() { return fechaCreacion.get(); }
    public void setFechaCreacion(Date d) { fechaCreacion.set(d); }
    public ObjectProperty<Date> fechaCreacionProperty() { return fechaCreacion; }

    public Date getFechaModificacion() { return fechaModificacion.get(); }
    public void setFechaModificacion(Date d) { fechaModificacion.set(d); }
    public ObjectProperty<Date> fechaModificacionProperty() { return fechaModificacion; }

    public String getNombreApellidos() { return nombreApellidos.get(); }
    public StringProperty nombreApellidosProperty() { return nombreApellidos; }
}
