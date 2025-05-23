package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IrrigationSyncResponse {
    private int totalItems;
    private int successfulItems;
    private int failedItems;
    private List<IrrigationSyncResultItem> results;
}