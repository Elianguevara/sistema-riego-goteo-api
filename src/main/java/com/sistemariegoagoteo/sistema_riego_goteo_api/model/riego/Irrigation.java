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
@Table(name = "irrigation")
public class Irrigation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "irrigation_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private IrrigationEquipment equipment; // Referencia actualizada

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "start_datetime")
    private Date startDatetime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "end_datetime")
    private Date endDatetime;

    @Column(name = "water_amount", precision = 10, scale = 2)
    private BigDecimal waterAmount;

    @Column(name = "irrigation_hours", precision = 5, scale = 2)
    private BigDecimal irrigationHours;
}