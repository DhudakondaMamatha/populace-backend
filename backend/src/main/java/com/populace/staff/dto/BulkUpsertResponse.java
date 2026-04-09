package com.populace.staff.dto;

import java.util.List;

/**
 * Response DTO for bulk upsert operations.
 * Contains results for each item in the batch, including successes and failures.
 */
public record BulkUpsertResponse(
    int totalCount,
    int successCount,
    int errorCount,
    List<UpsertResult> results
) {
    /**
     * Creates a response from a list of results, calculating counts.
     */
    public static BulkUpsertResponse withErrors(List<UpsertResult> results) {
        int successCount = (int) results.stream().filter(UpsertResult::success).count();
        int errorCount = results.size() - successCount;
        return new BulkUpsertResponse(results.size(), successCount, errorCount, results);
    }

    /**
     * Result for a single upsert operation in the batch.
     */
    public record UpsertResult(
        int index,
        boolean success,
        Long staffId,
        String staffName,
        List<ValidationError> errors
    ) {
        /**
         * Creates a successful result.
         */
        public static UpsertResult success(int index, Long staffId, String staffName) {
            return new UpsertResult(index, true, staffId, staffName, List.of());
        }

        /**
         * Creates a failed result with errors.
         */
        public static UpsertResult failure(int index, List<ValidationError> errors) {
            return new UpsertResult(index, false, null, null, errors);
        }
    }

    /**
     * Validation error for a single field.
     */
    public record ValidationError(
        String field,
        String message,
        String code
    ) {}
}
