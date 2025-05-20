package com.sistemariegoagoteo.sistema_riego_goteo_api.model.audit; 

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "synchronization")
public class Synchronization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sync_id")
    private Integer id;

    @Column(name = "modified_table", length = 100)
    private String modifiedTable;

    @Column(name = "modified_record_id") 
    private Integer modifiedRecordId; 

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "modification_datetime")
    private Date modificationDatetime;

    @Column(name = "is_synchronized") 
    private Boolean isSynchronized;
}