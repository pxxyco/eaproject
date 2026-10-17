module it.unical.ea_project_javafx {

    requires transitive javafx.graphics;
    
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.base;
    requires java.prefs;
    requires java.net.http;
    
    requires com.google.gson;
    requires com.gluonhq.maps;
    requires static lombok;

    exports it.unical.ea_project_javafx;
    
    opens it.unical.ea_project_javafx to javafx.fxml;

    opens it.unical.ea_project_javafx.dto to com.google.gson;
    opens it.unical.ea_project_javafx.dto.home to com.google.gson;

    opens it.unical.ea_project_javafx.controller.activity to javafx.fxml;
    opens it.unical.ea_project_javafx.controller.activity.photo to javafx.fxml;
    opens it.unical.ea_project_javafx.controller.activity.reviews to javafx.fxml;
    opens it.unical.ea_project_javafx.controller.common to javafx.fxml;
    opens it.unical.ea_project_javafx.controller.home to com.google.gson, javafx.fxml;
    opens it.unical.ea_project_javafx.controller.login to com.google.gson, javafx.fxml;
    opens it.unical.ea_project_javafx.controller.organizer to com.google.gson, javafx.fxml;
    opens it.unical.ea_project_javafx.controller.profile to javafx.fxml;
    opens it.unical.ea_project_javafx.controller.traveler to com.google.gson, javafx.fxml;

}