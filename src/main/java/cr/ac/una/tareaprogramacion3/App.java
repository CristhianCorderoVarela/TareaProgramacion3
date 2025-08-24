package cr.ac.una.tareaprogramacion3;

import cr.ac.una.tareaprogramacion3.util.FlowController;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Inicializa el flujo con el stage principal
        FlowController.getInstance().initializeFlow(stage, null);

        // Configura tamaño mínimo si lo deseas
        stage.setMinWidth(400);
        stage.setMinHeight(300);

        // Abre la vista login en una ventana
        FlowController.getInstance().goViewInWindow("login");
    }

    public static void main(String[] args) {
        launch();
    }
}
