package com.biometricos.service;

import com.biometricos.model.HuellaResponse;
import org.springframework.stereotype.Service;

@Service
public class HuellaClientService {

    public HuellaResponse capturarHuella(String dispositivo) {
        try {
            // Simular una captura exitosa para testing
            Thread.sleep(2000); // Simular tiempo de captura

            HuellaResponse respuesta = new HuellaResponse();
            respuesta.setExitoso(true);  // ✅ CORRECTO
            respuesta.setMensaje("Huella capturada exitosamente desde " + dispositivo);
            respuesta.setCalidad(75 + (int)(Math.random() * 20)); // Calidad entre 75-95
            respuesta.setMinucias(35 + (int)(Math.random() * 15)); // Minucias entre 35-50
            respuesta.setDispositivo(dispositivo);
            respuesta.setDedo("4"); // Pulgar izquierdo
            respuesta.setMano("izquierda");
            respuesta.setImagenBase64("mock_base64_image_data"); // Datos mock

            return respuesta;

        } catch (Exception e) {
            HuellaResponse respuesta = new HuellaResponse();
            respuesta.setExitoso(false);  // ✅ CORRECTO
            respuesta.setMensaje("Error en captura: " + e.getMessage());
            return respuesta;
        }
    }

    public boolean verificarConexion() {
        try {
            // Simular verificación de conexión
            Thread.sleep(500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}