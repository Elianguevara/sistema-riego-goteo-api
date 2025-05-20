package com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "irrigation_equipment")
public class IrrigationEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "equipment_id")
    private Integer id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "measured_flow", precision = 10, scale = 2)
    private BigDecimal measuredFlow;

    @Column(name = "has_flow_meter")
    private Boolean hasFlowMeter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Column(name = "equipment_type", length = 50)
    private String equipmentType;

    @Column(name = "equipment_status", length = 50) // Ajustado de VARCHAR(2...)
    private String equipmentStatus;

    @OneToMany(mappedBy = "irrigationEquipment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY) // mappedBy actualizado
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Maintenance> maintenances = new HashSet<>();

    @OneToMany(mappedBy = "equipment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Irrigation> irrigations = new HashSet<>();

    // La relación con Sector se maneja desde Sector (Sector N -> 1 IrrigationEquipment)
    // Si un equipo pudiera estar en muchos sectores, la relación sería diferente.
    // Basado en que sector.equipment_id es una FK simple, Sector tiene el ManyToOne.
}