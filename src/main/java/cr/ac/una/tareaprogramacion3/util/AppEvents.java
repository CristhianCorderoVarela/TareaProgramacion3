package cr.ac.una.tareaprogramacion3.util;

import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Bus de eventos simple para notificar cambios de proyectos entre ventanas.
 * - fireProyectoActualizado(id): emite evento
 * - onProyectoActualizado(listener): se suscribe (API nueva)
 * - offProyectoActualizado(listener): se desuscribe (API nueva)
 *
 * Compatibilidad (para código antiguo):
 * - addProyectoListener(listener)  -> alias de onProyectoActualizado
 * - removeProyectoListener(listener)-> alias de offProyectoActualizado
 */
public final class AppEvents {

    private AppEvents() {}

    // Lista thread-safe de oyentes
    private static final List<Consumer<Long>> proyectoListeners = new CopyOnWriteArrayList<>();

    /** Emite el evento de que un proyecto con 'proyectoId' fue actualizado. */
    public static void fireProyectoActualizado(Long proyectoId) {
        if (proyectoId == null) return;
        // Garantiza ejecutar listeners en el hilo de JavaFX
        Platform.runLater(() -> {
            for (Consumer<Long> l : proyectoListeners) {
                try { l.accept(proyectoId); } catch (Exception ignored) {}
            }
        });
    }

    /** Suscribe un listener (API nueva). */
    public static void onProyectoActualizado(Consumer<Long> listener) {
        if (listener != null) proyectoListeners.add(listener);
    }

    /** Desuscribe un listener (API nueva). */
    public static void offProyectoActualizado(Consumer<Long> listener) {
        if (listener != null) proyectoListeners.remove(listener);
    }

    /* ================== Alias de compatibilidad con código existente ================== */

    /** Alias antiguo: mantener compatibilidad con Ventana1. */
    public static void addProyectoListener(Consumer<Long> listener) {
        onProyectoActualizado(listener);
    }

    /** Alias antiguo: mantener compatibilidad con Ventana1. */
    public static void removeProyectoListener(Consumer<Long> listener) {
        offProyectoActualizado(listener);
    }
    
    private static java.util.List<java.util.function.Consumer<Long>> irSeguimientos = new java.util.ArrayList<>();
public static void onIrASeguimientos(java.util.function.Consumer<Long> c){ irSeguimientos.add(c); }
public static void fireIrASeguimientos(Long proyectoId){ for(var c: irSeguimientos) try{ c.accept(proyectoId);}catch(Exception ignored){} }
}