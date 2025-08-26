package com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fertilization")
public class Fertilization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fertilization_id")
    private Integer id;

    @Temporal(TemporalType.DATE)
    @Column(name = "fertilization_date") // Considera renombrar a "fertilization_date" por claridad
    private Date date;

    @Column(name = "fertilizer_type", length = 100)
    private String fertilizerType;

    // --- CAMBIOS PRINCIPALES AQUÍ ---

    // 1. Campo genérico para la cantidad (reemplaza a litersApplied)
    @Column(name = "quantity", precision = 10, scale = 2, nullable = false)
    private BigDecimal quantity;

    // 2. Nuevo campo para la unidad de medida
    @Enumerated(EnumType.STRING)
    @Column(name = "quantity_unit", length = 10, nullable = false)
    private UnitOfMeasure quantityUnit;

    // --- FIN DE LOS CAMBIOS ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;
}