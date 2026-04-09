package com.populace.staff.dto;

/**
 * Represents a single validation error during bulk upload.
 */
public record BulkUploadError(
    int row,
    String column,
    String value,
    String errorCode,
    String message
) {}
