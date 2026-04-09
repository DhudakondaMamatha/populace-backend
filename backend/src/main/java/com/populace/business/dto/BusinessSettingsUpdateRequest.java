package com.populace.business.dto;

import java.math.BigDecimal;

public record BusinessSettingsUpdateRequest(
    Integer coverageWindowMinutes,
    BigDecimal toleranceOverPercentage,
    BigDecimal toleranceUnderPercentage
) {}
