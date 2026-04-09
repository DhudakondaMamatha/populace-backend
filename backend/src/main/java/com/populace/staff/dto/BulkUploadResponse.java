package com.populace.staff.dto;

import java.util.List;

/**
 * Response DTO for bulk staff upload operation.
 */
public record BulkUploadResponse(
    boolean success,
    int totalRows,
    Integer validRows,
    Integer createdCount,
    Integer errorCount,
    List<BulkUploadError> errors,
    List<CreatedStaffSummary> createdStaff
) {
    /**
     * Summary of a created staff member.
     */
    public record CreatedStaffSummary(
        Long id,
        String employeeCode,
        String name
    ) {}

    /**
     * Creates a success response.
     */
    public static BulkUploadResponse success(int totalRows, List<CreatedStaffSummary> createdStaff) {
        return new BulkUploadResponse(
            true,
            totalRows,
            totalRows,
            createdStaff.size(),
            0,
            List.of(),
            createdStaff
        );
    }

    /**
     * Creates a validation error response.
     * Counts distinct rows with errors to determine valid row count.
     */
    public static BulkUploadResponse withValidationErrors(int totalRows, List<BulkUploadError> errors) {
        int rowsWithErrors = (int) errors.stream()
            .map(BulkUploadError::row)
            .distinct()
            .count();
        return new BulkUploadResponse(
            false,
            totalRows,
            totalRows - rowsWithErrors,
            0,
            errors.size(),
            errors,
            List.of()
        );
    }

    /**
     * Creates a file-level error response.
     */
    public static BulkUploadResponse fileError(String errorCode, String message) {
        return new BulkUploadResponse(
            false,
            0,
            0,
            0,
            1,
            List.of(new BulkUploadError(0, null, null, errorCode, message)),
            List.of()
        );
    }
}
