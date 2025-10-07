package com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "operation_log")
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "operation_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "operation_datetime") // <-- CAMBIO DE NOMBRE
    private Date operationDatetime;

    // --- CAMPO AÑADIDO (SUGERENCIA) ---
    @Column(name = "operation_type", length = 100)
    private String operationType;

    @Lob
    @Column(name = "description", columnDefinition="TEXT")
    private String description;
}