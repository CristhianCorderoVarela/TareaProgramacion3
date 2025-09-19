package cr.ac.una.tareaprogramacion3.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Bus de eventos súper sencillo para notificar cambios entre ventanas. */
public final class AppEvents {

    private AppEvents() {}

    /* ===================== Proyecto ===================== */

    public interface ProyectoListener {
        void onProyectoActualizado(Long proyectoId);
    }

    private static final List<ProyectoListener> PROYECTO_LISTENERS = new CopyOnWriteArrayList<>();

    public static void addProyectoListener(ProyectoListener l) {
        if (l != null) PROYECTO_LISTENERS.add(l);
    }

    public static void removeProyectoListener(ProyectoListener l) {
        PROYECTO_LISTENERS.remove(l);
    }

    public static void fireProyectoActualizado(Long proyectoId) {
        for (ProyectoListener l : PROYECTO_LISTENERS) {
            try { l.onProyectoActualizado(proyectoId); } catch (Exception ignore) {}
        }
    }
}