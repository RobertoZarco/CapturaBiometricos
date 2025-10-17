package com.biometricos;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class CapturaBiometricosApp extends Application {

    private ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        // Iniciar Spring Boot sin configuración de base de datos
        springContext = new SpringApplicationBuilder()
                .sources(SpringConfig.class)
                .run(getParameters().getRaw().toArray(new String[0]));
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Cargar FXML después de que Spring esté listo
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/main.fxml"));
        loader.setControllerFactory(springContext::getBean);
        Parent root = loader.load();

        primaryStage.setTitle("Sistema de Captura Biométrica - UMG");
        primaryStage.setScene(new Scene(root, 1000, 700));
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    @Override
    public void stop() {
        if (springContext != null) {
            springContext.close();
        }
        Platform.exit();
    }
}