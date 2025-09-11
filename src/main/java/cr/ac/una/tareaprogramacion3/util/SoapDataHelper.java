package cr.ac.una.tareaprogramacion3.util;

import cr.ac.una.client.soap.AdministradorDto;
import jakarta.xml.bind.JAXBElement;   
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/** Abre la "data" del WS para obtener un admin o una lista de admins. */
public final class SoapDataHelper {
    private SoapDataHelper() {}

    /** Devuelve UN AdministradorDto (o null si no se puede). */
    public static AdministradorDto asAdmin(Object data) {
        Object v = unwrap(data);
        if (v instanceof AdministradorDto a) return a;
        if (v instanceof JAXBElement<?> je && je.getValue() instanceof AdministradorDto a) return a;
        return null;
    }

    
    public static List<AdministradorDto> asAdminList(Object data) {
        Object v = unwrap(data);
        List<AdministradorDto> out = new ArrayList<>();
        if (v == null) return out;

        if (v instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof AdministradorDto a) out.add(a);
                else if (o instanceof JAXBElement<?> je && je.getValue() instanceof AdministradorDto a2) out.add(a2);
            }
            return out;
        }

        if (v.getClass().isArray()) {
            int len = Array.getLength(v);
            for (int i = 0; i < len; i++) {
                Object o = Array.get(v, i);
                if (o instanceof AdministradorDto a) out.add(a);
                else if (o instanceof JAXBElement<?> je && je.getValue() instanceof AdministradorDto a2) out.add(a2);
            }
            return out;
        }

        if (v instanceof AdministradorDto a) out.add(a);
        if (v instanceof JAXBElement<?> je && je.getValue() instanceof AdministradorDto a) out.add(a);

        return out;
    }

    private static Object unwrap(Object d) {
        if (d instanceof JAXBElement<?> je) return je.getValue();
        return d;
    }
}
