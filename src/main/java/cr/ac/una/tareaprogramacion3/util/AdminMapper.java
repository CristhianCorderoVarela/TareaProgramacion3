package cr.ac.una.tareaprogramacion3.util;

import cr.ac.una.client.soap.AdministradorDto;
import cr.ac.una.tareaprogramacion3.model.AdministradorModel;

public final class AdminMapper {
    private AdminMapper(){}

    public static AdministradorDto toDto(AdministradorModel m) {
        AdministradorDto d = new AdministradorDto();
        if (m.getId() != null) d.setId(m.getId());
        d.setNombre(m.getNombre());
        d.setApellidos(m.getApellidos());
        d.setCedula(m.getCedula());
        d.setCorreo(m.getCorreo());
        d.setUsuario(m.getUsuario());
        d.setEstado(m.getEstado());
        return d;
    }

    public static AdministradorModel toModel(AdministradorDto d) {
        AdministradorModel m = new AdministradorModel();
        m.setId(d.getId());
        m.setNombre(d.getNombre());
        m.setApellidos(d.getApellidos());
        m.setCedula(d.getCedula());
        m.setCorreo(d.getCorreo());
        m.setUsuario(d.getUsuario());
        m.setEstado(d.getEstado());
        return m;
    }
}
