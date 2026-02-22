package com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad que representa una Finca o Explotación Agrícola.
 * <p>
 * Una finca es la unidad principal de gestión, agrupando sectores, equipos de
 * riego,
 * personal asignado y registros operativos.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "farm")
public class Farm {

    /**
     * Identificador único de la finca.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "farm_id")
    private Integer id;

    /**
     * Nombre descriptivo de la finca.
     */
    @Column(name = "name", length = 100)
    private String name;

    /**
     * Ubicación geográfica o dirección de la finca.
     */
    @Column(name = "location", length = 255)
    private String location;

    /**
     * Capacidad máxima del reservorio de agua en metros cúbicos.
     */
    @Column(name = "reservoir_capacity", precision = 10, scale = 2)
    private BigDecimal reservoirCapacity;

    /**
     * Tamaño total de la finca en hectáreas.
     */
    @Column(name = "farm_size", precision = 10, scale = 2)
    private BigDecimal farmSize;

    /**
     * Usuarios vinculados a la finca (administradores, analistas u operarios).
     */
    @ManyToMany(mappedBy = "farms", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<User> users = new HashSet<>();

    /**
     * Sectores que componen la finca.
     */
    @OneToMany(mappedBy = "farm", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Sector> sectors = new HashSet<>();

    /**
     * Equipos de riego instalados en la finca.
     */
    @OneToMany(mappedBy = "farm", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<IrrigationEquipment> irrigationEquipments = new HashSet<>();

    /**
     * Fuentes de agua disponibles en la finca.
     */
    @OneToMany(mappedBy = "farm", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<WaterSource> waterSources = new HashSet<>();

    /**
     * Registros de consumo energético asociados a la finca.
     */
    @OneToMany(mappedBy = "farm", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<EnergyConsumption> energyConsumptions = new HashSet<>();

    /**
     * Logs de auditoría de las operaciones realizadas en la finca.
     */
    @OneToMany(mappedBy = "farm", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<OperationLog> operationLogs = new HashSet<>();

    /**
     * Registros de precipitación medidos en la finca.
     */
    @OneToMany(mappedBy = "farm", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Precipitation> precipitations = new HashSet<>();

    /**
     * Coordenada de latitud para geolocalización.
     */
    @Column(name = "latitude")
    private BigDecimal latitude;

    /**
     * Coordenada de longitud para geolocalización.
     */
    @Column(name = "longitude")
    private BigDecimal longitude;
}
