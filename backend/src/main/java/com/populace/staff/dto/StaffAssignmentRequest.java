package com.populace.staff.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record StaffAssignmentRequest(
    @NotNull List<Long> ids,
    List<Long> primaryIds
) {
    public List<Long> getIds() {
        return ids;
    }

    public List<Long> getPrimaryIds() {
        return primaryIds;
    }
}
