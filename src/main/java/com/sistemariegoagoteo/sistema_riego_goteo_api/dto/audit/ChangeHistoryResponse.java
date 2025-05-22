package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.audit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.audit.ChangeHistory;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class ChangeHistoryResponse {
    private Integer id;
    private Long userId; // ID del usuario que hizo el cambio
    private String username; // Username para fácil visualización
    private String affectedTable;
    private String changedField;
    private String oldValue;
    private String newValue;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private Date changeDatetime;

    public ChangeHistoryResponse(ChangeHistory changeHistory) {
        this.id = changeHistory.getId();
        if (changeHistory.getUser() != null) {
            this.userId = changeHistory.getUser().getId();
            this.username = changeHistory.getUser().getUsername();
        }
        this.affectedTable = changeHistory.getAffectedTable();
        this.changedField = changeHistory.getChangedField();
        this.oldValue = changeHistory.getOldValue();
        this.newValue = changeHistory.getNewValue();
        this.changeDatetime = changeHistory.getChangeDatetime();
    }
}