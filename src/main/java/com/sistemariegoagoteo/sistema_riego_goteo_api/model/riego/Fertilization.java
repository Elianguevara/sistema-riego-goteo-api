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
@Table(name = "fertilization", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"local_mobile_id"})
})
public class Fertilization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fertilization_id")
    private Integer id;

    /** UUID enviado por el móvil para idempotencia en sincronización offline. */
    @Column(name = "local_mobile_id", unique = true, length = 36)
    private String localMobileId;

    @Temporal(TemporalType.DATE)
    @Column(name = "fertilization_date")
    private Date date;

    @Column(name = "fertilizer_type", length = 100)
    private String fertilizerType;

    @Column(name = "quantity", precision = 10, scale = 2, nullable = false)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "quantity_unit", length = 10, nullable = false)
    private UnitOfMeasure quantityUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @PrePersist
    public void autofill() {
        if (this.localMobileId == null) {
            this.localMobileId = UUID.randomUUID().toString();
        }
    }
}
