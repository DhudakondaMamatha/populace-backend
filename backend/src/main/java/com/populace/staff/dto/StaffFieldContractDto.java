package com.populace.staff.dto;

import com.populace.staff.contract.StaffFieldContract;

/**
 * DTO exposing staff field contract metadata via the API.
 * Enables frontend and external consumers to discover field definitions programmatically.
 */
public record StaffFieldContractDto(
    String csvHeader,
    String apiFieldName,
    String category,
    String dataType,
    boolean required,
    boolean usedInAllocation,
    String[] validValues
) {
    public static StaffFieldContractDto fromContract(StaffFieldContract field) {
        return new StaffFieldContractDto(
            field.csvHeader(),
            field.apiFieldName(),
            field.category().name(),
            field.dataType().name(),
            field.required(),
            field.usedInAllocation(),
            field.validValues()
        );
    }
}
