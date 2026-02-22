package com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "precipitation")
public class Precipitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "precipitation_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Column(name = "precipitation_date")
    private LocalDate precipitationDate;

    @Column(name = "mm_rain", precision = 6, scale = 2) // Lluvia total
    private BigDecimal mmRain;

    @Column(name = "mm_effective_rain", precision = 6, scale = 2) // Lluvia efectiva calculada
    private BigDecimal mmEffectiveRain;
}