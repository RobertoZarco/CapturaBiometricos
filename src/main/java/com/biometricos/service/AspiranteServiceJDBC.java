package com.biometricos.service;

import com.biometricos.model.Aspirante;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class AspiranteServiceJDBC {

    private final DataSource dataSource;

    public AspiranteServiceJDBC(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Buscar aspirante por folio
     */
    public Aspirante buscarPorFolio(String folio) {
        String sql = "SELECT folio, nombre, carrera FROM aspirantes WHERE folio = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, folio);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Aspirante aspirante = new Aspirante();
                aspirante.setFolio(rs.getString("folio"));
                aspirante.setNombre(rs.getString("nombre"));
                //aspirante.setCarrera(rs.getString("carrera"));
                return aspirante;
            }
            return null;

        } catch (SQLException e) {
            System.err.println("Error buscando aspirante: " + e.getMessage());
            return null;
        }
    }

    /**
     * Guardar datos biométricos
     */
    public boolean guardarBiometricos(String folio, String imagenWsq, String minucias, Integer calidad) {
        String sql = "UPDATE aspirantes SET imagen_wsq = ?, minucias = ?, calidad_huella = ?, " +
                "fecha_actualizacion = GETDATE() WHERE folio = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, imagenWsq);
            stmt.setString(2, minucias);
            stmt.setInt(3, calidad);
            stmt.setString(4, folio);

            int filasAfectadas = stmt.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            System.err.println("Error guardando biométricos: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verificar conexión a BD
     */
    public boolean verificarConexionBD() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2); // 2 segundos timeout
        } catch (SQLException e) {
            System.err.println("Error de conexión a BD: " + e.getMessage());
            return false;
        }
    }
}