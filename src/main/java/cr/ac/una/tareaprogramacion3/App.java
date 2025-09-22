package cr.ac.una.tareaprogramacion3;

import cr.ac.una.tareaprogramacion3.util.FlowController;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        
        FlowController.getInstance().initializeFlow(stage, null);

        // Configura tamaño mínimo 
        stage.setMinWidth(400);
        stage.setMinHeight(300);

        // Abre la vista login 
        FlowController.getInstance().goViewInWindow("loginView");
    }

    public static void main(String[] args) {
        launch();
    }
}
