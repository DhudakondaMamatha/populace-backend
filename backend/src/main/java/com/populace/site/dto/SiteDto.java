package com.populace.site.dto;

public record SiteDto(
    Long id,
    String name,
    String code,
    String address,
    String city,
    String state,
    String postalCode,
    String country,
    String contactName,
    String contactEmail,
    String contactPhone,
    boolean active,
    boolean enforceSameRoleBreakExclusivity
) {}
