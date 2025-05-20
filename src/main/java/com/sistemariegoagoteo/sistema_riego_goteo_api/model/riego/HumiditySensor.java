package com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "humidity_sensor")
public class HumiditySensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sensor_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    @Column(name = "sensor_type", length = 50)
    private String sensorType;

    @Column(name = "humidity_level", precision = 5, scale = 2)
    private BigDecimal humidityLevel;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "measurement_datetime") 
    private Date measurementDatetime;

    @OneToMany(mappedBy = "humiditySensor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<HumidityAlert> humidityAlerts = new HashSet<>();
}