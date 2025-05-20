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
    @Column(name = "date")
    private Date date;

    @Column(name = "fertilizer_type", length = 100) 
    private String fertilizerType;

    @Column(name = "liters_applied", precision = 10, scale = 2)
    private BigDecimal litersApplied;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;
}