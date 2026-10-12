package it.unical.ea_project_javafx.model;


import javafx.scene.Node;

public interface MainNavigator {
    void goToHome(Node sourceNode);
    void goToLogin(Node sourceNode);
    void goToProfile(Node sourceNode);
}