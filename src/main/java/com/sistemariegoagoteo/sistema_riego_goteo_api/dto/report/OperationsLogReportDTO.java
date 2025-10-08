package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report;

import lombok.Data;
import java.util.List;

@Data
public class OperationsLogReportDTO {
    private String farmName;
    private DateRange dateRange;
    private List<Operation> operations;

    @Data
    public static class DateRange {
        private String start;
        private String end;
    }

    @Data
    public static class Operation {
        private String datetime;
        private String type;
        private String description;
        private String location;
        private String userName;
    }
}