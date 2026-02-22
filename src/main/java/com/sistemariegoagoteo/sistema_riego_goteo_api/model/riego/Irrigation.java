package com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID; // Importar UUID

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "irrigation", uniqueConstraints = { // Asegurar unicidad del localMobileId si es global
        @UniqueConstraint(columnNames = { "local_mobile_id" })
})
public class Irrigation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "irrigation_id")
    private Integer id; // ID del servidor

    @Column(name = "local_mobile_id", nullable = false, unique = true, length = 36) // Nuevo campo
    private String localMobileId; // Ej. un UUID generado por el móvil

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private IrrigationEquipment equipment;

    private LocalDateTime startDatetime;

    private LocalDateTime endDatetime;

    @Column(name = "water_amount", precision = 10, scale = 2)
    private BigDecimal waterAmount;

    @Column(name = "irrigation_hours", precision = 5, scale = 2)
    private BigDecimal irrigationHours;

    // Método para generar un localMobileId si no se proporciona (aunque el móvil
    // debería enviarlo)
    @PrePersist
    public void autofill() {
        if (this.localMobileId == null) {
            this.localMobileId = UUID.randomUUID().toString();
        }
    }
}