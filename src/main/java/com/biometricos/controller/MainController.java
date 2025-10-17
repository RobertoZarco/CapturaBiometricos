package com.biometricos.controller;

import com.biometricos.model.Aspirante;
import com.biometricos.service.AspiranteService;
import com.biometricos.model.HuellaResponse;
import com.biometricos.service.GestorArchivosService;
import com.biometricos.service.HuellaClientService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class MainController {

    @FXML private TextField txtFolio;
    @FXML private Button btnBuscar, btnNuevo, btnCapturar, btnGuardar;
    @FXML private Label lblNombre, lblFolio, lblReferencia, lblEstatus, lblCalidad, lblMinucias;
    @FXML private ComboBox<String> cbDispositivo;
    @FXML private ProgressBar pbCalidad;
    @FXML private VBox panelResultados;
    @FXML private TextArea taLog;


    @Autowired
    private AspiranteService aspiranteService;

    @Autowired
    private GestorArchivosService gestorArchivosService;

    @Autowired
    private HuellaClientService huellaClientService;

    private String referenciaActual;
    private HuellaResponse huellaCapturada;
    private String fotoCapturada;
    private String firmaCapturada;

    @FXML
    public void initialize() {
        configurarControles();
        verificarConexiones();
    }

    private void configurarControles() {
        // Configurar ComboBox
        cbDispositivo.getItems().addAll("CROSSMATCH", "DIGITAL_PERSONA");
        cbDispositivo.setValue("CROSSMATCH");

        // Event handlers
        btnBuscar.setOnAction(e -> buscarAspirante());
        btnNuevo.setOnAction(e -> nuevoAspirante());
        btnCapturar.setOnAction(e -> capturarHuella());
        btnGuardar.setOnAction(e -> guardarBiometricos());

        // Estado inicial
        panelResultados.setVisible(false);
        limpiarDatos();
    }

    private void verificarConexiones() {
        // Verificar microservicio en hilo separado
        new Thread(() -> {
            boolean microservicioConectado = huellaClientService.verificarConexion();
            Platform.runLater(() -> {
                if (microservicioConectado) {
                    agregarLog("✅ Microservicio de huellas conectado");
                } else {
                    agregarLog("❌ Microservicio de huellas NO disponible");
                    btnCapturar.setDisable(true);
                }
            });
        }).start();

        agregarLog("✅ Sistema listo - Modo archivos locales");
        // No verificamos BD porque todo se guarda en archivos
    }

    private void buscarAspirante() {
        String referencia = txtFolio.getText().trim();
        if (referencia.isEmpty()) {
            mostrarAlerta("Error", "Ingrese una referencia (clave pre-aspirante)");
            return;
        }

        new Thread(() -> {
            try {
                // Buscar en la base de datos PreReg_CSDB.PreAspirante
                Aspirante aspirante = aspiranteService.buscarPorReferencia(referencia);

                Platform.runLater(() -> {
                    if (aspirante != null) {
                        // Mostrar datos del pre-aspirante encontrado
                        mostrarDatosAspirante(aspirante);
                        referenciaActual = referencia;
                        agregarLog("✅ Pre-Aspirante encontrado: " + aspirante.getNombre());

                        // Habilitar captura biométrica
                        btnCapturar.setDisable(false);
                    } else {
                        // No encontrado en PreAspirante
                        mostrarConfirmacionNuevaCaptura(referencia);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    agregarLog("❌ Error al buscar en BD: " + e.getMessage());
                    // Fallback: permitir captura aunque falle la consulta
                    prepararCapturaConReferencia(referencia);
                });
            }
        }).start();
    }

    private void mostrarDatosAspirante(Aspirante aspirante) {
        lblNombre.setText(aspirante.getNombre() != null ? aspirante.getNombre() : "N/A");
        lblFolio.setText(aspirante.getFolio() != null ? aspirante.getFolio() : "N/A");

        if (lblReferencia != null) {
            lblReferencia.setText(aspirante.getFolio() != null ? aspirante.getFolio() : "N/A");
        }

        lblEstatus.setText("Pre-Aspirante Encontrado");
        lblEstatus.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        panelResultados.setVisible(true);
    }

    private void mostrarConfirmacionNuevaCaptura(String referencia) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Referencia No Encontrada");
            alert.setHeaderText("No se encontró en base de datos: " + referencia);
            alert.setContentText("¿Desea capturar datos biométricos para esta referencia?"); // Mensaje más claro

            if (alert.showAndWait().get() == ButtonType.OK) {
                prepararCapturaConReferencia(referencia);
                agregarLog("⚠️ Capturando para referencia no registrada: " + referencia);
            }
        });
    }


    private void nuevoAspirante() {
        try {
            String referenciaPredefinida = txtFolio.getText().trim();

            // Cargar la vista de nuevo aspirante
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/biometricos/view/nuevo-aspirante.fxml"));
            Parent root = loader.load();

            // Obtener el controlador y configurar datos
            NuevoAspiranteController nuevoAspiranteController = loader.getController();
            nuevoAspiranteController.setMainController(this);

            // Pasar la referencia si existe
            if (!referenciaPredefinida.isEmpty()) {
                nuevoAspiranteController.setReferenciaPredefinida(referenciaPredefinida);
            }

            if (!referenciaPredefinida.isEmpty()) {
                nuevoAspiranteController.setFolioPredefinido(referenciaPredefinida);
            }

            // Crear nueva escena y stage
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Nueva Referencia");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            agregarLog("❌ Error al cargar ventana de nueva referencia: " + e.getMessage());
            mostrarAlerta("Error", "No se pudo cargar la ventana de nueva referencia");
        }
    }

    // Método para preparar captura con referencia (llamado desde NuevoAspiranteController)
    public void prepararCapturaConReferencia(String referencia) {
        limpiarDatos();
        txtFolio.setText(referencia);
        referenciaActual = referencia;
        panelResultados.setVisible(true);
        lblEstatus.setText("Listo para Captura");
        lblEstatus.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

        // Actualizar labels con la referencia
        lblNombre.setText(referencia);
        lblFolio.setText(referencia);

        if (lblReferencia != null) {
            lblReferencia.setText(referencia);
        }

        // Habilitar captura
        btnCapturar.setDisable(false);
        btnGuardar.setDisable(true);

        agregarLog("Sistema listo para capturar datos de: " + referencia);
    }

    @FXML
    private void capturarHuella() {
        if (cbDispositivo.getValue() == null) {
            mostrarAlerta("Error", "Seleccione un dispositivo de huella");
            return;
        }

        // Deshabilitar botón durante la captura
        btnCapturar.setDisable(true);
        agregarLog("Iniciando captura de huella...");

        new Thread(() -> {
            try {
                String dispositivo = cbDispositivo.getValue();

                // Llamar al microservicio para capturar huella
                HuellaResponse respuesta = huellaClientService.capturarHuella(dispositivo);

                Platform.runLater(() -> {
                    btnCapturar.setDisable(false);

                    if (respuesta != null && respuesta.isExitoso()) {
                        huellaCapturada = respuesta;
                        mostrarResultadosHuella(respuesta);
                        agregarLog("✅ Huella capturada exitosamente");

                        // Habilitar guardado
                        btnGuardar.setDisable(false);
                    } else {
                        String mensajeError = respuesta != null ? respuesta.getMensaje() : "Error desconocido";
                        agregarLog("❌ Error en captura: " + mensajeError);
                        mostrarAlerta("Error", "No se pudo capturar la huella: " + mensajeError);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnCapturar.setDisable(false);
                    agregarLog("❌ Error en captura: " + e.getMessage());
                    mostrarAlerta("Error", "Excepción durante captura: " + e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void guardarBiometricos() {
        String referencia = txtFolio.getText().trim();

        if (referencia.isEmpty()) {
            mostrarAlerta("Error", "No hay referencia para guardar");
            return;
        }

        if (huellaCapturada == null) {
            mostrarAlerta("Error", "Primero debe capturar una huella");
            return;
        }

        btnGuardar.setDisable(true);
        agregarLog("Guardando datos biométricos...");

        new Thread(() -> {
            try {
                // Guardar en la estructura de carpetas
                gestorArchivosService.guardarDatosCompletos(
                        referencia,
                        huellaCapturada,
                        huellaCapturada.getImagenBase64(),
                        fotoCapturada,
                        firmaCapturada
                );

                Platform.runLater(() -> {
                    btnGuardar.setDisable(false);
                    agregarLog("✅ Datos guardados para: " + referencia);
                    agregarLog("📁 Estructura: " + gestorArchivosService.obtenerEstructuraCarpetas());
                    mostrarAlerta("Éxito",
                            "Datos guardados correctamente:\n" +
                                    "• Huella: datos_biometricos/huellas/" + referencia + "_huella.jpg\n" +
                                    "• JSON: datos_biometricos/registros/" + referencia + ".json\n" +
                                    (fotoCapturada != null ? "• Foto: datos_biometricos/fotos/" + referencia + ".jpg\n" : "") +
                                    (firmaCapturada != null ? "• Firma: datos_biometricos/firmas/" + referencia + ".jpg" : "")
                    );

                    // Limpiar para nueva captura
                    limpiarCapturas();
                    txtFolio.clear();
                    limpiarDatos();
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnGuardar.setDisable(false);
                    agregarLog("❌ Error al guardar: " + e.getMessage());
                    mostrarAlerta("Error", "Error al guardar archivos: " + e.getMessage());
                });
            }
        }).start();
    }

    // Métodos para capturar foto y firma (puedes agregar botones para estos)
    @FXML
    private void capturarFoto() {
        agregarLog("📸 Función de captura de foto llamada");
        // Aquí implementarías la lógica para capturar foto
        // fotoCapturada = "base64_de_la_foto_aqui";
    }

    @FXML
    private void capturarFirma() {
        agregarLog("✍️ Función de captura de firma llamada");
        // Aquí implementarías la lógica para capturar firma
        // firmaCapturada = "base64_de_la_firma_aqui";
    }

    private void mostrarResultadosHuella(HuellaResponse respuesta) {
        if (respuesta == null) return;

        // Mostrar calidad
        if (respuesta.getCalidad() != null) {
            lblCalidad.setText(respuesta.getCalidad().toString() + "%");
            pbCalidad.setProgress(respuesta.getCalidad() / 100.0);

            // Color según calidad
            if (respuesta.getCalidad() >= 80) {
                lblCalidad.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            } else if (respuesta.getCalidad() >= 60) {
                lblCalidad.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
            } else {
                lblCalidad.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            }
        }

        // Mostrar minucias
        if (respuesta.getMinucias() != null) {
            lblMinucias.setText(respuesta.getMinucias().toString());
        }

        // Mostrar información del dedo si está disponible
        if (respuesta.getDedo() != null && respuesta.getMano() != null) {
            lblEstatus.setText("Huella " + respuesta.getMano() + " - Dedo " + respuesta.getDedo());
        } else {
            lblEstatus.setText("Huella Capturada");
        }

        // Mostrar panel de resultados
        panelResultados.setVisible(true);

        // Habilitar botón guardar
        btnGuardar.setDisable(false);
    }

    private void limpiarCapturas() {
        huellaCapturada = null;
        fotoCapturada = null;
        firmaCapturada = null;
        btnGuardar.setDisable(true);
    }

    private void limpiarDatos() {
        lblNombre.setText("N/A");
        lblFolio.setText("N/A");
        if (lblReferencia != null) {
            lblReferencia.setText("N/A");
        }
        lblEstatus.setText("No buscado");
        lblEstatus.setStyle("-fx-text-fill: gray;");
        lblCalidad.setText("0%");
        lblMinucias.setText("0");
        pbCalidad.setProgress(0);

        referenciaActual = null;
        huellaCapturada = null;
        fotoCapturada = null;
        firmaCapturada = null;

        lblCalidad.setStyle("-fx-text-fill: black;");
    }

    private boolean validarReferencia(String referencia) {
        if (referencia == null || referencia.trim().isEmpty()) {
            return false;
        }

        referencia = referencia.trim();

        if (referencia.length() < 3) {
            return false;
        }

        if (!referencia.matches("^[A-Za-z0-9_-]+$")) {
            return false;
        }

        return true;
    }

    private void agregarLog(String mensaje) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        Platform.runLater(() -> {
            taLog.appendText(timestamp + " - " + mensaje + "\n");
        });
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }
}