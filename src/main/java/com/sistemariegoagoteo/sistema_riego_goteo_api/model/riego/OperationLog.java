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
    @JoinColumn(name = "farm_id", nullable = false) // Columna en MER es "farm, id INT", se asume farm_id
    private Farm farm;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "start_datetime")
    private Date startDatetime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "end_datetime")
    private Date endDatetime;
}