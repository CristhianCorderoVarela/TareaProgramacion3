package cr.ac.una.tareaprogramacion3.service;

import cr.ac.una.client.soap.AdministradorDto;
import cr.ac.una.client.soap.AuthService;
import cr.ac.una.client.soap.AuthWS;
import cr.ac.una.client.soap.RespuestaLogin;

import java.util.Optional;

public class AuthServiceCliente {

    private final AuthWS port;

    public AuthServiceCliente() {
        this.port = new AuthService().getAuthWSPort();
    }

  
    public boolean isServerUp() {
        try {
            Object resp = port.ping(); // 
            if (resp instanceof Boolean b) return b;
            if (resp instanceof String s) {
                String v = s.trim().toLowerCase();
                return v.contains("pong") || v.contains("ok") || v.equals("true") || v.equals("1");
            }
            return resp != null; 
        } catch (Exception ex) {
            return false;
        }
    }

    
    public Optional<AdministradorDto> login(String usuario, String contrasenna) {
        try {
            RespuestaLogin r = port.login(usuario, contrasenna);
            if (r != null && r.isOk()) {
                return Optional.ofNullable(r.getAdministrador());
            }
        } catch (Exception ex) {
            
        }
        return Optional.empty();
    }
}





