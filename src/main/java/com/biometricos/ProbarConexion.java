package com.biometricos;

import java.sql.Connection;
import java.sql.DriverManager;

public class ProbarConexion {

    public static void main(String[] args) {
        System.out.println("🔍 Probando conexión a Sybase...");

        // Configuración - AJUSTA ESTOS VALORES
        String url = "jdbc:sybase:Tds:132.248.205.1:9019/PreReg_CSDB";
        String user = "rherrera";      // Cambia por tu usuario real
        String password = "q&7Oh5p#aJ"; // Cambia por tu password real

        System.out.println("URL: " + url);
        System.out.println("Usuario: " + user);

        try {
            // Cargar el driver
            Class.forName("com.sybase.jdbc4.jdbc.SybDriver");
            System.out.println("✅ Driver cargado correctamente");

            // Intentar conexión
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("✅ CONEXIÓN EXITOSA a Sybase!");

            // Probar una consulta simple
            var stmt = conn.createStatement();
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM PreReg_CSDB.dbo.PreAspirante");
            if (rs.next()) {
                System.out.println("✅ Tabla PreAspirante existe, registros: " + rs.getInt(1));
            }

            rs.close();
            stmt.close();
            conn.close();
            System.out.println("✅ Conexión cerrada correctamente");

        } catch (ClassNotFoundException e) {
            System.err.println("❌ ERROR: Driver no encontrado");
            System.err.println("   Asegúrate de que jconn3.jar está en el classpath");
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ ERROR de conexión: " + e.getMessage());
            e.printStackTrace();

            // Información adicional para debugging
            System.err.println("\n--- INFORMACIÓN PARA DEBUGGING ---");
            System.err.println("URL: " + url);
            System.err.println("Driver: com.sybase.jdbc3.jdbc.SybDriver");
            System.err.println("Posibles soluciones:");
            System.err.println("1. Verificar que el servidor Sybase esté encendido");
            System.err.println("2. Verificar usuario y contraseña");
            System.err.println("3. Verificar que la base de datos PreReg_CSDB exista");
            System.err.println("4. Verificar que el puerto 5000 esté abierto");
            System.err.println("5. Verificar que jconn3.jar esté en el classpath");
        }
    }
}