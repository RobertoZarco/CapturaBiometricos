package com.biometricos.controller;

import com.biometricos.model.Aspirante;
import com.biometricos.service.AspiranteService;
import com.biometricos.model.HuellaResponse;
import com.biometricos.service.GestorArchivosService;
import com.biometricos.service.HuellaClientService;
import com.biometricos.util.MapeadorDedos;
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
import java.util.HashMap;
import java.util.Map;


@Controller
public class MainController {

    // Cambiar de una huella a un mapa de huellas por dedo
    private Map<Integer, HuellaResponse> huellasCapturadas = new HashMap<>();
    private int dedoActual = MapeadorDedos.MANO_IZQUIERDA_PULGAR; // Por defecto pulgar izquierdo

    // Agregar ComboBox para seleccionar dedo
    @FXML private ComboBox<String> cbDedo;

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
        // Configurar ComboBox de dispositivo
        cbDispositivo.getItems().addAll("CROSSMATCH", "DIGITAL_PERSONA");
        cbDispositivo.setValue("CROSSMATCH");

        // Configurar ComboBox de dedos
        configurarComboDedos();

        // Event handlers
        btnBuscar.setOnAction(e -> buscarAspirante());
        btnNuevo.setOnAction(e -> nuevoAspirante());
        btnCapturar.setOnAction(e -> capturarHuella());
        btnGuardar.setOnAction(e -> guardarBiometricos());

        // Estado inicial
        panelResultados.setVisible(false);
        limpiarDatos();
    }

    private void configurarComboDedos() {
        cbDedo.getItems().clear();
        cbDedo.getItems().addAll(
                "Pulgar Izquierdo (4)",
                "Pulgar Derecho (5)",
                "Índice Izquierdo (3)",
                "Índice Derecho (6)",
                "Medio Izquierdo (2)",
                "Medio Derecho (7)",
                "Anular Izquierdo (1)",
                "Anular Derecho (8)",
                "Meñique Izquierdo (0)",
                "Meñique Derecho (9)"
        );
        cbDedo.setValue("Pulgar Izquierdo (4)");

        // Actualizar dedoActual cuando cambie la selección
        cbDedo.setOnAction(e -> {
            String seleccion = cbDedo.getValue();
            if (seleccion != null) {
                // Extraer el número del paréntesis
                String numeroStr = seleccion.substring(seleccion.lastIndexOf("(") + 1, seleccion.lastIndexOf(")"));
                try {
                    dedoActual = Integer.parseInt(numeroStr);
                    System.out.println("🎯 Dedo seleccionado: " + dedoActual + " - " + MapeadorDedos.obtenerNombreDedo(dedoActual));

                    // Mostrar huella previamente capturada si existe
                    mostrarHuellaCapturada(dedoActual);
                } catch (NumberFormatException ex) {
                    System.err.println("Error parseando número de dedo: " + numeroStr);
                }
            }
        });
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

        // ✅ SI es una referencia DIFERENTE, limpiar todo
        if (referenciaActual != null && !referenciaActual.equals(referencia)) {
            agregarLog("🔁 Nueva referencia detectada - Limpiando datos anteriores");
            limpiarDatos(); // Limpiar todo para nuevo aspirante
        }

        agregarLog("🔍 Buscando en BD: " + referencia);
        btnBuscar.setDisable(true);

        new Thread(() -> {
            try {
                Aspirante aspirante = aspiranteService.buscarPorReferencia(referencia);

                Platform.runLater(() -> {
                    btnBuscar.setDisable(false);

                    if (aspirante != null) {
                        // Aspirante encontrado en BD
                        mostrarDatosAspirante(aspirante);
                        referenciaActual = referencia;
                        agregarLog("✅ Pre-Aspirante encontrado: " + aspirante.getNombre());

                        // Habilitar captura pero NO limpiar huellas existentes
                        btnCapturar.setDisable(false);

                        // Si ya hay huellas capturadas, mostrar resumen
                        if (!huellasCapturadas.isEmpty()) {
                            agregarLog("📋 Huellas capturadas previamente: " + huellasCapturadas.size());
                            mostrarResumenHuellasCapturadas();
                        }
                    } else {
                        // No encontrado en BD
                        agregarLog("❌ No encontrado en BD");
                        mostrarConfirmacionNuevaCaptura(referencia);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    btnBuscar.setDisable(false);
                    agregarLog("⚠️ Error al buscar en BD: " + e.getMessage());
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

        if (referenciaActual == null) {
            mostrarAlerta("Error", "Primero busque una referencia válida");
            return;
        }

        String nombreDedo = MapeadorDedos.obtenerNombreDedo(dedoActual);
        agregarLog("📸 Capturando huella: " + nombreDedo);
        btnCapturar.setDisable(true);

        new Thread(() -> {
            try {
                String dispositivo = cbDispositivo.getValue();
                HuellaResponse respuesta = huellaClientService.capturarHuella(dispositivo);

                Platform.runLater(() -> {
                    btnCapturar.setDisable(false);

                    if (respuesta != null && respuesta.isExitoso()) {
                        // ✅ Guardar en el mapa con el código del dedo actual
                        huellasCapturadas.put(dedoActual, respuesta);

                        // ✅ Actualizar información del dedo en la respuesta
                        respuesta.setDedo(String.valueOf(dedoActual));
                        respuesta.setMano(MapeadorDedos.obtenerMano(dedoActual));

                        // ✅ Llamar al método ACTUALIZADO con dos parámetros
                        mostrarResultadosHuella(respuesta, dedoActual);
                        agregarLog("✅ Huella capturada: " + nombreDedo);

                        // Mostrar resumen de huellas capturadas
                        mostrarResumenHuellasCapturadas();

                    } else {
                        String mensajeError = respuesta != null ? respuesta.getMensaje() : "Error desconocido";
                        agregarLog("❌ Error capturando " + nombreDedo + ": " + mensajeError);
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
        if (referenciaActual == null || referenciaActual.isEmpty()) {
            mostrarAlerta("Error", "No hay referencia para guardar");
            return;
        }

        if (huellasCapturadas.isEmpty()) {
            mostrarAlerta("Error", "Primero debe capturar al menos una huella");
            return;
        }

        btnGuardar.setDisable(true);
        agregarLog("💾 Guardando " + huellasCapturadas.size() + " huella(s) para: " + referenciaActual);

        new Thread(() -> {
            try {
                gestorArchivosService.guardarDatosCompletos(
                        referenciaActual,
                        huellasCapturadas,
                        fotoCapturada,
                        firmaCapturada
                );

                Platform.runLater(() -> {
                    btnGuardar.setDisable(false);
                    agregarLog("✅ " + huellasCapturadas.size() + " huella(s) guardadas exitosamente para: " + referenciaActual);

                    // ✅ MOSTRAR RESUMEN PERO NO LIMPIAR LA REFERENCIA
                    mostrarResumenGuardadoCompleto(referenciaActual, huellasCapturadas);

                    // ✅ SOLO LIMPIAR CAPTURAS PERO MANTENER REFERENCIA Y DATOS
                    limpiarCapturasParcial();

                    // ❌ NO limpiarDatos() - eso borra la referencia
                    // ❌ NO txtFolio.clear() - eso borra el campo de texto

                    agregarLog("💡 Referencia mantenida: " + referenciaActual + " - Lista para capturar más huellas");
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

    private void mostrarResultadosHuella(HuellaResponse respuesta, int codigoDedo) {
        System.out.println("🎯 INICIANDO mostrarResultadosHuella para dedo: " + codigoDedo);

        if (respuesta == null) {
            System.out.println("❌ Respuesta es null en mostrarResultadosHuella");
            return;
        }

        String nombreDedo = MapeadorDedos.obtenerNombreDedo(codigoDedo);

        // Mostrar calidad
        Integer calidadMostrar = respuesta.getCalidadTotal();
        String descripcionCalidad = respuesta.getDescripcionCalidad();

        if (calidadMostrar != null) {
            lblCalidad.setText(calidadMostrar.toString() + "% - " + descripcionCalidad);
            pbCalidad.setProgress(calidadMostrar / 100.0);

            // Color según calidad
            if (calidadMostrar >= 80) {
                lblCalidad.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            } else if (calidadMostrar >= 60) {
                lblCalidad.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
            } else {
                lblCalidad.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            }

            agregarLog("📊 Calidad " + nombreDedo + ": " + calidadMostrar + "% (" + descripcionCalidad + ")");
        }

        // Mostrar minucias
        if (respuesta.getMinucias() != null) {
            String minuciasTexto = respuesta.getMinuciasFormateadas();
            lblMinucias.setText(minuciasTexto);
            agregarLog("🔢 " + nombreDedo + " - " + minuciasTexto);
        } else {
            lblMinucias.setText("0 minucias");
        }

        // Mostrar información de la imagen
        if (respuesta.tieneImagenWsqReal()) {
            agregarLog("📷 " + nombreDedo + " - Imagen WSQ (" + respuesta.getImagenWsq().length() + " caracteres)");
        }

        // Mostrar información del dedo
        lblEstatus.setText(nombreDedo + " - Huella Capturada");
        lblEstatus.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");

        // HABILITAR BOTÓN GUARDAR
        btnGuardar.setDisable(false);
        agregarLog("💾 " + nombreDedo + " listo para guardar");

        // Mostrar panel de resultados
        panelResultados.setVisible(true);

        System.out.println("✅ FIN mostrarResultadosHuella - " + nombreDedo);
    }

    private void limpiarResultadosHuella() {
        System.out.println("🧹 Limpiando resultados de huella");

        lblCalidad.setText("0%");
        lblMinucias.setText("0");
        pbCalidad.setProgress(0);
        lblEstatus.setText("No capturado");
        lblEstatus.setStyle("-fx-text-fill: gray;");
        lblCalidad.setStyle("-fx-text-fill: black;");
    }

    private void limpiarDatos() {
        System.out.println("🔄 Limpiando TODOS los datos - Nuevo aspirante");

        // Limpiar labels de datos del aspirante
        lblNombre.setText("N/A");
        lblFolio.setText("N/A");
        if (lblReferencia != null) {
            lblReferencia.setText("N/A");
        }

        // Limpiar estado de captura
        lblEstatus.setText("No buscado");
        lblEstatus.setStyle("-fx-text-fill: gray;");

        // Limpiar resultados de huella
        limpiarResultadosHuella();

        // Limpiar todas las variables
        referenciaActual = null;
        limpiarCapturas(); // Esto limpia el mapa completo

        // Limpiar campo de búsqueda
        txtFolio.clear();

        // Ocultar panel de resultados
        panelResultados.setVisible(false);

        // Deshabilitar botones
        btnCapturar.setDisable(true);
        btnGuardar.setDisable(true);

        agregarLog("🧹 Todos los datos limpiados - Listo para nuevo aspirante");
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

    private void mostrarResumenGuardado(String referencia, HuellaResponse huella) {
        String mensaje = "Datos guardados correctamente:\n\n" +
                "• Referencia: " + referencia + "\n" +
                "• Calidad: " + huella.getDescripcionCalidad() + "\n" +
                "• Minucias: " + huella.getMinuciasFormateadas() + "\n" +
                "• JSON: datos_biometricos/registros/" + referencia + ".json\n" +
                "• TXT: datos_biometricos/registros/" + referencia + ".txt";

        if (huella.getImagenWsq() != null) {
            mensaje += "\n• Huella WSQ: datos_biometricos/huellas/" + referencia + "_huella.wsq";
        }

        if (huella.getImagenBmp() != null) {
            mensaje += "\n• Huella BMP: datos_biometricos/huellas/" + referencia + "_huella.bmp";
        }

        if (fotoCapturada != null) {
            mensaje += "\n• Foto: datos_biometricos/fotos/" + referencia + ".jpg";
        }

        if (firmaCapturada != null) {
            mensaje += "\n• Firma: datos_biometricos/firmas/" + referencia + ".jpg";
        }

        mostrarAlerta("Éxito", mensaje);
    }

    public void verificarEstado() {
        System.out.println("=== ESTADO ACTUAL ===");
        System.out.println("referenciaActual: " + referenciaActual);
        System.out.println("huellaCapturada: " + (huellaCapturada != null));
        System.out.println("btnGuardar disabled: " + btnGuardar.isDisabled());

        if (huellaCapturada != null) {
            System.out.println("Huella capturada - Éxito: " + huellaCapturada.isExitoso());
            System.out.println("Imagen WSQ: " + (huellaCapturada.tieneImagenWsqReal() ?
                    huellaCapturada.getImagenWsq().length() + " chars" : "no"));
        }
        System.out.println("=====================");
    }

    private void mostrarHuellaCapturada(int codigoDedo) {
        HuellaResponse huella = huellasCapturadas.get(codigoDedo);
        if (huella != null) {
            mostrarResultadosHuella(huella, codigoDedo);
            agregarLog("📁 Mostrando huella capturada previamente para: " + MapeadorDedos.obtenerNombreDedo(codigoDedo));
        } else {
            limpiarResultadosHuella();
            agregarLog("💡 Listo para capturar: " + MapeadorDedos.obtenerNombreDedo(codigoDedo));
        }
    }

    private void mostrarResumenHuellasCapturadas() {
        if (!huellasCapturadas.isEmpty()) {
            agregarLog("📋 Huellas capturadas: " + huellasCapturadas.size() + " dedo(s)");
            for (Map.Entry<Integer, HuellaResponse> entry : huellasCapturadas.entrySet()) {
                String nombreDedo = MapeadorDedos.obtenerNombreDedo(entry.getKey());
                HuellaResponse huella = entry.getValue();
                agregarLog("   • " + nombreDedo + ": " +
                        huella.getCalidadTotal() + "% calidad, " +
                        huella.getMinuciasFormateadas());
            }
        }
    }

    private void mostrarResumenGuardadoCompleto(String referencia, Map<Integer, HuellaResponse> huellas) {
        StringBuilder mensaje = new StringBuilder();
        mensaje.append("Datos guardados correctamente:\n\n");
        mensaje.append("• Referencia: ").append(referencia).append("\n");
        mensaje.append("• Huellas guardadas: ").append(huellas.size()).append(" dedo(s)\n\n");

        for (Map.Entry<Integer, HuellaResponse> entry : huellas.entrySet()) {
            int codigoDedo = entry.getKey();
            HuellaResponse huella = entry.getValue();
            String nombreDedo = MapeadorDedos.obtenerNombreDedo(codigoDedo);
            String codigoArchivo = MapeadorDedos.obtenerCodigoArchivo(codigoDedo);

            mensaje.append("• ").append(nombreDedo).append(":\n");
            mensaje.append("  - Archivo: ").append(referencia).append("_").append(codigoArchivo).append(".wsq\n");
            mensaje.append("  - Calidad: ").append(huella.getCalidadTotal()).append("%\n");
            mensaje.append("  - Minucias: ").append(huella.getMinuciasFormateadas()).append("\n\n");
        }

        mensaje.append("¿Desea capturar más huellas para este mismo aspirante?\n\n");
        mensaje.append("• Presione 'Capturar Huella' para agregar más dedos\n");
        mensaje.append("• Presione 'Nueva Búsqueda' para buscar otro aspirante");

        // Usar CONFIRMATION en lugar de INFORMATION
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Guardado Exitoso");
        alert.setHeaderText("Huellas Guardadas Correctamente");
        alert.setContentText(mensaje.toString());
        alert.showAndWait();
    }

    private void limpiarCapturas() {
        huellasCapturadas.clear();
        fotoCapturada = null;
        firmaCapturada = null;
        btnGuardar.setDisable(true);
        System.out.println("🧹 Todas las huellas capturadas fueron limpiadas");
    }

    private void limpiarCapturasParcial() {
        int huellasAnteriores = huellasCapturadas.size();
        huellasCapturadas.clear();
        fotoCapturada = null;
        firmaCapturada = null;
        btnGuardar.setDisable(true);

        // Limpiar solo los resultados de huella, no los datos del aspirante
        limpiarResultadosHuella();

        System.out.println("🧹 Capturas limpiadas (" + huellasAnteriores + " huellas) - Referencia mantenida: " + referenciaActual);
        agregarLog("🧹 " + huellasAnteriores + " huella(s) limpiadas - Listo para capturar más");
    }

    @FXML
    private void nuevaBusqueda() {
        agregarLog("🔄 Iniciando nueva búsqueda...");
        limpiarDatos(); // Esto limpia todo completamente
        agregarLog("💡 Ingrese una nueva referencia para buscar");
    }


}