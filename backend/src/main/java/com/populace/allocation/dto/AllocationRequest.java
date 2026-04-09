package com.populace.allocation.dto;

import java.time.LocalDate;
import java.util.List;

public record AllocationRequest(
    LocalDate startDate,
    LocalDate endDate,
    List<Long> siteIds
) {}
