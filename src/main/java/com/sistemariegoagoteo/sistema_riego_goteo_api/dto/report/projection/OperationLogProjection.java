package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.projection;

public interface OperationLogProjection {
    java.util.Date getDatetime();

    String getType();

    String getDescription();

    String getLocation();

    String getUserName();
}
