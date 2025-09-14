// AdministradorServiceCliente.java (cliente JavaFX)
package cr.ac.una.tareaprogramacion3.service;

import cr.ac.una.client.soap.AdministradorDto;
import cr.ac.una.client.soap.AdministradorService;
import cr.ac.una.client.soap.AdministradorWS;
import cr.ac.una.client.soap.RespuestaGeneral;

import jakarta.xml.ws.BindingProvider;
import java.util.Collections;
import java.util.List;

public class AdministradorServiceCliente {

    // Usa localhost para que funcione donde se despliegue el WS localmente
    private String endpoint = "http://localhost:8080/AdministradorService/AdministradorWS";

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String url) { this.endpoint = url; }

    private AdministradorWS port() {
        AdministradorService svc = new AdministradorService();
        AdministradorWS p = svc.getAdministradorWSPort();
        ((BindingProvider)p).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
        return p;
    }

    public boolean isServerUp() {
        try { port().ping(); return true; } catch (Exception e) { return false; }
    }

    // --- Listado plano ---
    public List<AdministradorDto> obtenerTodosList() {
        try {
            List<AdministradorDto> lista = port().obtenerTodosPlano();
            return (lista != null) ? lista : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    // --- CRUD envueltos en RespuestaGeneral (generados por wsimport) ---
    public RespuestaGeneral crear(AdministradorDto dto)      { return port().crearAdministrador(dto); }
    public RespuestaGeneral actualizar(AdministradorDto dto)  { return port().actualizarAdministrador(dto); }
    public RespuestaGeneral eliminar(Long id)                 { return port().eliminarAdministrador(id); }
}
