package cr.ac.una.tareaprogramacion3.service;

import cr.ac.una.client.soap.AdministradorDto;
import cr.ac.una.client.soap.AuthService;
import cr.ac.una.client.soap.AuthWS;
import cr.ac.una.client.soap.RespuestaLogin;

import java.util.Optional;

public class AdministradorService {

    private final AuthWS port;

    public AdministradorService() {
        this.port = new AuthService().getAuthWSPort();
    }

    /** Verifica si el WS está arriba. El ping del WS NO recibe parámetros. */
    public boolean isServerUp() {
        try {
            Object resp = port.ping(); // <-- sin argumentos
            if (resp instanceof Boolean b) return b;
            if (resp instanceof String s) {
                String v = s.trim().toLowerCase();
                return v.contains("pong") || v.contains("ok") || v.equals("true") || v.equals("1");
            }
            return resp != null; // cualquier otra cosa no nula la tomamos como “ok”
        } catch (Exception ex) {
            return false;
        }
    }

    /** Llama al WS y devuelve el AdministradorDto cuando el login es correcto. */
    public Optional<AdministradorDto> login(String usuario, String contrasenna) {
        try {
            RespuestaLogin r = port.login(usuario, contrasenna);
            if (r != null && r.isOk()) {
                return Optional.ofNullable(r.getAdministrador());
            }
        } catch (Exception ex) {
            // opcional: log
        }
        return Optional.empty();
    }
}
