package com.populace.business.dto;

import java.math.BigDecimal;

public record BusinessSettingsDto(
    Long id,
    String name,
    int coverageWindowMinutes,
    BigDecimal toleranceOverPercentage,
    BigDecimal toleranceUnderPercentage
) {}
