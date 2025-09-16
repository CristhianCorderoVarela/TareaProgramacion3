package cr.ac.una.tareaprogramacion3.util;

import cr.ac.una.client.soap.AdministradorDto;

public final class UserSession {
    private static UserSession INSTANCE;
    private AdministradorDto admin;

    private UserSession() {}

    public static UserSession get() {
        if (INSTANCE == null) INSTANCE = new UserSession();
        return INSTANCE;
    }

    public AdministradorDto getAdmin() { return admin; }
    public void setAdmin(AdministradorDto admin) { this.admin = admin; }
    public void clear() { this.admin = null; }

    // Getters usados en Ventana4Controller
    public Long getAdminId() { return admin != null ? admin.getId() : null; }
    public String getAdminNombre() {
        if (admin == null) return null;
        String n = admin.getNombre() != null ? admin.getNombre() : "";
        String a = admin.getApellidos() != null ? admin.getApellidos() : "";
        return (n + " " + a).trim();
    }
}
