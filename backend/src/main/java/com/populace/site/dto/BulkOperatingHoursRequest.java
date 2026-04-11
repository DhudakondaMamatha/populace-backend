package com.populace.site.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BulkOperatingHoursRequest(
    @NotNull(message = "Operating hours list is required")
    @Size(min = 1, max = 7, message = "Must provide 1-7 operating hours entries")
    @Valid
    List<OperatingHoursRequest> hours
) {}
