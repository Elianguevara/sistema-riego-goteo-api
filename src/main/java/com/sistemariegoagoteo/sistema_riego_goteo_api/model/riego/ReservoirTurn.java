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
@Table(name = "reservoir_turn")
public class ReservoirTurn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "turn_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private WaterSource waterSource;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "start_datetime")
    private Date startDatetime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "end_datetime")
    private Date endDatetime;
}