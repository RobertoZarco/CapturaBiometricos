package com.biometricos.model;

import java.util.HashMap;
import java.util.Map;

public class SolicitudCaptura {
    private String dispositivo;
    private Map<String, Object> configuracion;

    public SolicitudCaptura(String dispositivo) {
        this.dispositivo = dispositivo;
        this.configuracion = new HashMap<>();
        this.configuracion.put("ancho", 512);
        this.configuracion.put("alto", 512);
    }

    // Getters y Setters
    public String getDispositivo() { return dispositivo; }
    public void setDispositivo(String dispositivo) { this.dispositivo = dispositivo; }

    public Map<String, Object> getConfiguracion() { return configuracion; }
    public void setConfiguracion(Map<String, Object> configuracion) { this.configuracion = configuracion; }
}