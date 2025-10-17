package com.biometricos.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.stereotype.Controller;

@Controller
public class NuevoAspiranteController {

    @FXML private TextField txtReferencia;
    @FXML private Button btnContinuar;
    @FXML private Button btnCancelar;

    private MainController mainController;
    private String referenciaPredefinida;

    @FXML
    public void initialize() {
        // Si hay referencia predefinida, establecerla
        if (referenciaPredefinida != null && !referenciaPredefinida.isEmpty()) {
            txtReferencia.setText(referenciaPredefinida);
        }

        // Validar que el campo no esté vacío
        btnContinuar.disableProperty().bind(
                txtReferencia.textProperty().isEmpty()
        );
    }


    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setReferenciaPredefinida(String referencia) {
        this.referenciaPredefinida = referencia;
        if (txtReferencia != null && referencia != null && !referencia.isEmpty()) {
            Platform.runLater(() -> {
                txtReferencia.setText(referencia);
            });
        }
    }

    public void setFolioPredefinido(String folio) {
        this.referenciaPredefinida = folio;
        if (txtReferencia != null && folio != null && !folio.isEmpty()) {
            Platform.runLater(() -> {
                txtReferencia.setText(folio);
            });
        }
    }

    @FXML
    private void continuar() {
        String referencia = txtReferencia.getText().trim();

        if (referencia.isEmpty()) {
            mostrarError("Ingrese una referencia válida");
            return;
        }

        // Validar referencia
        if (!referencia.matches("^[A-Za-z0-9_-]+$") || referencia.length() < 3) {
            mostrarError("Referencia inválida. Use solo letras, números y guiones (mínimo 3 caracteres)");
            return;
        }

        referencia = referencia.toUpperCase();

        // Cerrar ventana y pasar la referencia al main controller
        cerrarVentana();

        if (mainController != null) {
            mainController.prepararCapturaConReferencia(referencia);
        }
    }

    @FXML
    private void cancelar() {
        cerrarVentana();
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}