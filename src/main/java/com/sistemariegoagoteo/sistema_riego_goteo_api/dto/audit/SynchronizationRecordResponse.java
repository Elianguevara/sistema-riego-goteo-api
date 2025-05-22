package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.audit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.audit.Synchronization;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class SynchronizationRecordResponse {
    private Integer id; // sync_id
    private String modifiedTable;
    private Integer modifiedRecordId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private Date modificationDatetime;
    private Boolean isSynchronized;

    public SynchronizationRecordResponse(Synchronization syncRecord) {
        this.id = syncRecord.getId();
        this.modifiedTable = syncRecord.getModifiedTable();
        this.modifiedRecordId = syncRecord.getModifiedRecordId();
        this.modificationDatetime = syncRecord.getModificationDatetime();
        this.isSynchronized = syncRecord.getIsSynchronized();
    }
}