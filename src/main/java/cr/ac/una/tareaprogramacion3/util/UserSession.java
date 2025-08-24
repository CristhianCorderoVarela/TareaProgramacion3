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
}
