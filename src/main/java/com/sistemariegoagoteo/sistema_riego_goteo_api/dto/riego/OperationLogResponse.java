package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.OperationLog;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class OperationLogResponse {
    private Integer id;

    private Integer farmId;
    private String farmName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private Date startDatetime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private Date endDatetime;

    public OperationLogResponse(OperationLog operationLog) {
        this.id = operationLog.getId();
        if (operationLog.getFarm() != null) {
            this.farmId = operationLog.getFarm().getId();
            this.farmName = operationLog.getFarm().getName();
        }
        this.startDatetime = operationLog.getStartDatetime();
        this.endDatetime = operationLog.getEndDatetime();
    }
}