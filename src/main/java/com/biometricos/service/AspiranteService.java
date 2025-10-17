package com.biometricos.service;

import com.biometricos.model.Aspirante;
import com.biometricos.repository.AspiranteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AspiranteService {

    @Autowired
    private AspiranteRepository aspiranteRepository;

    public Aspirante buscarPorReferencia(String referencia) {
        return aspiranteRepository.findByReferencia(referencia);
    }

    public boolean existeReferencia(String referencia) {
        return aspiranteRepository.existePorReferencia(referencia) > 0;
    }

    public String obtenerNombrePorReferencia(String referencia) {
        return aspiranteRepository.findNombreByReferencia(referencia);
    }

    public boolean verificarConexionBD() {
        try {
            // Consulta simple para verificar conexión
            aspiranteRepository.existePorReferencia("TEST");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}