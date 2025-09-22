package cr.ac.una.tareaprogramacion3;

import cr.ac.una.tareaprogramacion3.util.FlowController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        // Manejador global de excepciones en el hilo de JavaFX (evita cierres silenciosos)
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace(); // aquí puedes llamar a tu util de alertas si tienes una
        });

        try {
            // Inicializa el flujo con el Stage principal
            FlowController.getInstance().initializeFlow(stage, null);

            // Tamaños mínimos del Stage (tu mismo comportamiento)
            stage.setMinWidth(400);
            stage.setMinHeight(300);

            // Abre la vista de login (tu misma vista y método)
            FlowController.getInstance().goViewInWindow("loginView");
            // Nota: si FlowController ya hace stage.show(), no es necesario repetirlo aquí.
            // Si no mostrara nada, descomenta la siguiente línea:
            // stage.show();

        } catch (Exception ex) {
            ex.printStackTrace();
            // Si algo crítico falla al iniciar, cerramos la app de forma segura
            Platform.exit();
        }
    }

    @Override
    public void stop() {
        // Aquí puedes hacer limpieza si la necesitas (cerrar puertos, guardar prefs, etc.)
        // Se llama siempre cuando la app se cierra correctamente.
    }

    public static void main(String[] args) {
        launch(args); // lanza JavaFX
    }
}