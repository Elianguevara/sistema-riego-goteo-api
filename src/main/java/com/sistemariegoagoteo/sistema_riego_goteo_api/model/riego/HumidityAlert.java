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
@Table(name = "humidity_alert")
public class HumidityAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", nullable = false)
    private HumiditySensor humiditySensor;

    @Column(name = "humidity_level", precision = 5, scale = 2)
    private BigDecimal humidityLevel;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "alert_datetime")
    private Date alertDatetime;

    @Column(name = "alert_message", length = 255)
    private String alertMessage;

    @Column(name = "humidity_threshold", precision = 5, scale = 2)
    private BigDecimal humidityThreshold;
}