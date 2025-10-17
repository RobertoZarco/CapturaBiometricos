package com.biometricos.repository;

import com.biometricos.model.Aspirante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AspiranteRepository extends JpaRepository<Aspirante, Long> {

    // Buscar por clave (asp_clvpr)
    Aspirante findByFolio(String folio);

    // ✅ CONSULTA NATIVA usando el nombre real de la tabla
    @Query(value = "SELECT * FROM PreReg_CSDB.dbo.PreAspirante WHERE asp_clvpr = :referencia", nativeQuery = true)
    Aspirante findByReferencia(@Param("referencia") String referencia);

    // ✅ Consulta nativa para verificar existencia
    @Query(value = "SELECT COUNT(*) FROM PreReg_CSDB.dbo.PreAspirante WHERE asp_clvpr = :referencia", nativeQuery = true)
    int existePorReferencia(@Param("referencia") String referencia);

    // ✅ Consulta nativa para obtener solo el nombre
    @Query(value = "SELECT asp_nombre FROM PreReg_CSDB.dbo.PreAspirante WHERE asp_clvpr = :referencia", nativeQuery = true)
    String findNombreByReferencia(@Param("referencia") String referencia);

    // Verificar si existe la referencia
    boolean existsByFolio(String folio);
}