package com.populace.shift.dto;

import java.util.List;

public record ScheduleTemplateDto(
    Long id,
    Long siteId,
    String siteName,
    String name,
    boolean active,
    List<ScheduleTemplateEntryDto> entries
) {}
