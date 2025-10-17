package com.biometricos.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DatosBiometricos {
    private String referencia;
    private String fechaCaptura;
    private HuellaResponse huella;

    // Constructores
    public DatosBiometricos() {}

    public DatosBiometricos(String referencia, HuellaResponse huella) {
        this.referencia = referencia;
        this.huella = huella;
        this.fechaCaptura = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // Getters y Setters
    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getFechaCaptura() {
        return fechaCaptura;
    }

    public void setFechaCaptura(String fechaCaptura) {
        this.fechaCaptura = fechaCaptura;
    }

    public HuellaResponse getHuella() {
        return huella;
    }

    public void setHuella(HuellaResponse huella) {
        this.huella = huella;
    }

    @Override
    public String toString() {
        return "DatosBiometricos{" +
                "referencia='" + referencia + '\'' +
                ", fechaCaptura='" + fechaCaptura + '\'' +
                ", huella=" + huella +
                '}';
    }
}