package com.biometricos.model;
import javax.persistence.*;


@Entity
@Table(name = "PreAspirante", schema = "PreReg_CSDB.dbo")  // Especificar schema y tabla
public class Aspirante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "asp_clvpr") // Ajusta los nombres de columnas
    private String folio;

    @Column(name = "asp_nombre")
    private String nombre;

    // Constructores
    public Aspirante() {}

    // Getters y Setters

    public String getFolio() { return folio; }
    public void setFolio(String folio) { this.folio = folio; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }


    @Override
    public String toString() {
        return "Aspirante{" +
                "asp_clvpr='" + folio + '\'' +
                ", nombre='" + nombre + '\'' +
                '}';
    }
}