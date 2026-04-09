package com.populace.schedule.dto;

import java.math.BigDecimal;

public record WeeklySummaryDto(
    BigDecimal totalHours,
    int totalBreakMinutes
) {
    public static WeeklySummaryDto empty() {
        return new WeeklySummaryDto(BigDecimal.ZERO, 0);
    }
}
