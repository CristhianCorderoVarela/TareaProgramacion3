module cr.ac.una.administradorproyectos {
    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;

    // SOAP / JAXB
    requires jakarta.xml.ws;
    requires jakarta.xml.bind;
    requires java.xml;

    
    requires jakarta.persistence;
    requires java.logging;

   
    exports cr.ac.una.tareaprogramacion3;
    exports cr.ac.una.tareaprogramacion3.controller;
    exports cr.ac.una.tareaprogramacion3.model;
    exports cr.ac.una.tareaprogramacion3.service;
    exports cr.ac.una.tareaprogramacion3.util;
    

    
    opens cr.ac.una.tareaprogramacion3 to javafx.fxml;
    opens cr.ac.una.tareaprogramacion3.controller to javafx.fxml;
    opens cr.ac.una.tareaprogramacion3.views to javafx.fxml;

 
    opens cr.ac.una.client.soap;


    opens cr.ac.una.tareaprogramacion3.model to jakarta.persistence;
}
