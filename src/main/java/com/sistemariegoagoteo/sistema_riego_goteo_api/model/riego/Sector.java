package com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

/**
 * Entidad que representa un Sector de riego dentro de una finca.
 * <p>
 * Los sectores permiten dividir la finca en áreas manejables que cuentan con
 * sensores de humedad y equipos de riego específicos.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sector")
public class Sector {

    /**
     * Identificador único del sector.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sector_id")
    private Integer id;

    /**
     * Nombre descriptivo del sector (ej. "Lote Norte", "Hectárea 5").
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Finca a la que pertenece este sector.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    /**
     * Equipo de riego asignado actualmente a este sector.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id")
    private IrrigationEquipment equipment; // Referencia actualizada

    /**
     * Historial de riegos realizados en este sector.
     */
    @OneToMany(mappedBy = "sector", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Irrigation> irrigations = new HashSet<>();

    /**
     * Historial de fertilizaciones aplicadas a este sector.
     */
    @OneToMany(mappedBy = "sector", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Fertilization> fertilizations = new HashSet<>();

    /**
     * Sensores de humedad instalados en este sector.
     */
    @OneToMany(mappedBy = "sector", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<HumiditySensor> humiditySensors = new HashSet<>();
}