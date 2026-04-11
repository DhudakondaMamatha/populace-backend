package com.populace.allocation.dto;

import java.time.LocalDate;

public record BulkReallocateRequest(
    LocalDate startDate,
    LocalDate endDate,
    Long siteId,
    boolean forceReallocate
) {}
