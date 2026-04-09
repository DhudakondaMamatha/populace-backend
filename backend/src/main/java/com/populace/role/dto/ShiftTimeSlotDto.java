package com.populace.role.dto;

/**
 * A distinct shift time slot (startTime–endTime) for a role.
 * Times are in HH:mm format.
 */
public record ShiftTimeSlotDto(String startTime, String endTime) {}
