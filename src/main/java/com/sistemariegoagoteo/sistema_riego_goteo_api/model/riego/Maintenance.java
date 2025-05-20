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
@Table(name = "maintenance")
public class Maintenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maintenance_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private IrrigationEquipment irrigationEquipment; // Referencia actualizada

    @Temporal(TemporalType.DATE)
    @Column(name = "date")
    private Date date;

    @Lob
    @Column(name = "description", columnDefinition="TEXT") // Explicitando TEXT
    private String description;

    @Column(name = "work_hours", precision = 5, scale = 2)
    private BigDecimal workHours;
}