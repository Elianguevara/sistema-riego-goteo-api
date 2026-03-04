package com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Date;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "operation_log", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"local_mobile_id"})
})
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "operation_id")
    private Integer id;

    /** UUID enviado por el móvil para idempotencia en sincronización offline. */
    @Column(name = "local_mobile_id", unique = true, length = 36)
    private String localMobileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "operation_datetime")
    private Date operationDatetime;

    @Column(name = "operation_type", length = 100)
    private String operationType;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @PrePersist
    public void autofill() {
        if (this.localMobileId == null) {
            this.localMobileId = UUID.randomUUID().toString();
        }
    }
}
