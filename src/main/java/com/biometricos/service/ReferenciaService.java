package com.biometricos.service;

import org.springframework.stereotype.Service;

@Service
public class ReferenciaService {

    public boolean validarReferencia(String referencia) {
        // Validaciones básicas
        if (referencia == null || referencia.trim().isEmpty()) {
            return false;
        }

        // Eliminar espacios y convertir a mayúsculas
        referencia = referencia.trim().toUpperCase();

        // Validar longitud mínima
        if (referencia.length() < 3) {
            return false;
        }

        // Validar caracteres permitidos (letras, números, guiones)
        if (!referencia.matches("^[A-Z0-9_-]+$")) {
            return false;
        }

        return true;
    }

    public String formatearReferencia(String referencia) {
        if (referencia == null) return "";
        return referencia.trim().toUpperCase();
    }

    public String generarNombreArchivo(String referencia, String tipo, String extension) {
        referencia = formatearReferencia(referencia);
        return referencia + "_" + tipo + "." + extension;
    }
}
