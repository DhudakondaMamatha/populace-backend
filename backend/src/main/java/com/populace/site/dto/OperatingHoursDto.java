package com.populace.site.dto;

import java.time.LocalTime;

public record OperatingHoursDto(
    Long id,
    Long siteId,
    Integer dayOfWeek,
    boolean isClosed,
    LocalTime openTime,
    LocalTime closeTime
) {}
