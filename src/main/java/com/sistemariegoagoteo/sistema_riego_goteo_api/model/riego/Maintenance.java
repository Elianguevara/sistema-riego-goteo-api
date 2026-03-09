package com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "maintenance", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"local_mobile_id"})
})
public class Maintenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maintenance_id")
    private Integer id;

    /** UUID enviado por el móvil para idempotencia en sincronización offline. */
    @Column(name = "local_mobile_id", unique = true, length = 36)
    private String localMobileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private IrrigationEquipment irrigationEquipment;

    @Temporal(TemporalType.DATE)
    @Column(name = "date")
    private Date date;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "work_hours", precision = 5, scale = 2)
    private BigDecimal workHours;

    @PrePersist
    public void autofill() {
        if (this.localMobileId == null) {
            this.localMobileId = UUID.randomUUID().toString();
        }
    }
}
