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
@Table(name = "energy_consumption")
public class EnergyConsumption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "consumption_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Temporal(TemporalType.DATE)
    @Column(name = "consumption_date", nullable = false)
    private Date consumptionDate;

    @Column(name = "kwh_consumed", precision = 10, scale = 2)
    private BigDecimal kwhConsumed;

    @Column(name = "energy_type", length = 50)
    private String energyType;
}