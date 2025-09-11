package cr.ac.una.tareaprogramacion3.service;

import cr.ac.una.client.soap.AdministradorDto;
import cr.ac.una.client.soap.AdministradorService;
import cr.ac.una.client.soap.AdministradorWS;
import cr.ac.una.client.soap.RespuestaGeneral;

import jakarta.xml.ws.BindingProvider;

public class AdministradorServiceCliente {
    private final AdministradorWS port;

    public AdministradorServiceCliente() {
        this.port = new AdministradorService().getAdministradorWSPort();
        // OJO: no forzamos URL. Usamos la que viene del WSDL.
    }

    /** Para mostrar en mensajes a qué URL está llamando el cliente realmente. */
    public String getEndpoint() {
        try {
            Object v = ((BindingProvider) port).getRequestContext()
                    .get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
            return v != null ? v.toString() : "desconocido";
        } catch (Exception e) {
            return "desconocido";
        }
    }

    public boolean isServerUp() {
        try { return port.ping() != null; } catch (Exception e) { return false; }
    }

    // --- Operaciones CRUD simples (sin adornos) ---
    public RespuestaGeneral obtenerTodos() { return port.obtenerTodosAdministradores(); }
    public RespuestaGeneral buscar(String filtro) { return port.buscarAdministradores(filtro); }
    public RespuestaGeneral buscarPorId(Long id) { return port.buscarAdministradorPorId(id); }
    public RespuestaGeneral crear(AdministradorDto dto) { return port.crearAdministrador(dto); }
    public RespuestaGeneral actualizar(AdministradorDto dto) { return port.actualizarAdministrador(dto); }
    public RespuestaGeneral eliminar(Long id) { return port.eliminarAdministrador(id); }
}
