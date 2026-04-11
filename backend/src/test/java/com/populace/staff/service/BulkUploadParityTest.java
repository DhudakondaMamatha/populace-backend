package com.populace.staff.service;

import com.populace.staff.dto.BulkStaffRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Bulk Upload Field Parity.
 * Verifies that bulk upload supports the same fields as manual staff creation.
 */
@DisplayName("Bulk Upload Parity Tests")
class BulkUploadParityTest {

    @Test
    @DisplayName("BulkStaffRow includes maxSitesPerDay field")
    void bulkStaffRowShouldIncludeMaxSitesPerDay() {
        BulkStaffRow row = createRowWithMaxSitesPerDay("2");

        assertEquals("2", row.maxSitesPerDay());
    }

    @Test
    @DisplayName("BulkStaffRow includes mandatory leave fields")
    void bulkStaffRowShouldIncludeMandatoryLeaveFields() {
        BulkStaffRow row = createRowWithMandatoryLeave("30", "7");

        assertEquals("30", row.mustGoOnLeaveAfterDays());
        assertEquals("7", row.accruesOneDayLeaveAfterDays());
    }

    @Test
    @DisplayName("BulkStaffRow includes weekly hours fields")
    void bulkStaffRowShouldIncludeWeeklyHoursFields() {
        BulkStaffRow row = createRowWithWeeklyHours("20", "48");

        assertEquals("20", row.minHoursPerWeek());
        assertEquals("48", row.maxHoursPerWeek());
    }

    @Test
    @DisplayName("BulkStaffRow.Validated includes all work constraint fields")
    void bulkStaffRowValidatedShouldIncludeAllWorkConstraintFields() {
        BulkStaffRow.Validated validated = new BulkStaffRow.Validated(
            1,
            "EMP001",
            "John",
            "Doe",
            "john@test.com",
            "+1234567890",
            "permanent",
            List.of(1L),
            List.of(1L),
            List.of("L1"),
            1L,     // primaryRoleId
            "hourly",
            new BigDecimal("25.00"),
            null,
            new BigDecimal("4"),
            new BigDecimal("10"),
            new BigDecimal("80"),
            new BigDecimal("180"),
            1,
            2,      // maxSitesPerDay
            new BigDecimal("20"),   // minHoursPerWeek
            new BigDecimal("48"),   // maxHoursPerWeek
            30,     // mustGoOnLeaveAfterDays
            7,      // accruesOneDayLeaveAfterDays
            null,   // minBreakMinutes
            null,   // maxBreakMinutes
            null,   // minWorkMinutesBeforeBreak
            null    // maxContinuousWorkMinutes
        );

        assertEquals(2, validated.maxSitesPerDay());
        assertEquals(new BigDecimal("20"), validated.minHoursPerWeek());
        assertEquals(new BigDecimal("48"), validated.maxHoursPerWeek());
        assertEquals(30, validated.mustGoOnLeaveAfterDays());
        assertEquals(7, validated.accruesOneDayLeaveAfterDays());
    }

    @Test
    @DisplayName("BulkStaffRow includes primary_role field")
    void bulkStaffRowShouldIncludePrimaryRole() {
        BulkStaffRow row = new BulkStaffRow(
            1, "EMP001", "John", "Doe", "john@test.com", "+1234567890", "permanent",
            List.of("Nurse", "Doctor"), List.of("Site A"), List.of("L1", "L2"), "Nurse",
            "hourly", "25.00", null,
            null, null, null, null, null, null,
            null, null,
            null, null,
            null, null, null, null
        );

        assertEquals("Nurse", row.primaryRole());
    }

    @Test
    @DisplayName("BulkStaffRow.Validated includes primaryRoleId field")
    void bulkStaffRowValidatedShouldIncludePrimaryRoleId() {
        BulkStaffRow.Validated validated = new BulkStaffRow.Validated(
            1, "EMP001", "John", "Doe", "john@test.com", "+1234567890", "permanent",
            List.of(1L, 2L), List.of(1L), List.of("L1", "L2"), 1L,
            "hourly", new BigDecimal("25.00"), null,
            null, null, null, null, null, null,
            null, null,
            null, null,
            null, null, null, null
        );

        assertEquals(1L, validated.primaryRoleId());
    }

    @Test
    @DisplayName("All optional fields can be null")
    void optionalFieldsShouldAllowNull() {
        BulkStaffRow row = createMinimalRow();

        assertNull(row.maxSitesPerDay());
        assertNull(row.minHoursPerWeek());
        assertNull(row.maxHoursPerWeek());
        assertNull(row.mustGoOnLeaveAfterDays());
        assertNull(row.accruesOneDayLeaveAfterDays());
        assertNull(row.minBreakMinutes());
        assertNull(row.maxBreakMinutes());
        assertNull(row.minWorkMinutesBeforeBreak());
        assertNull(row.maxContinuousWorkMinutes());
    }

    private BulkStaffRow createRowWithMaxSitesPerDay(String maxSites) {
        return new BulkStaffRow(
            1, "EMP001", "John", "Doe", "john@test.com", "+1234567890", "permanent",
            List.of("Manager"), List.of("Site A"), List.of("L1"), null,
            "hourly", "25.00", null,
            "4", "10", null, "180", "1",
            maxSites,
            null, null,
            null, null,
            null, null, null, null
        );
    }

    private BulkStaffRow createRowWithMandatoryLeave(String mustGoOnLeave, String accruesLeave) {
        return new BulkStaffRow(
            1, "EMP001", "John", "Doe", "john@test.com", "+1234567890", "permanent",
            List.of("Manager"), List.of("Site A"), List.of("L1"), null,
            "hourly", "25.00", null,
            "4", "10", null, "180", "1", null,
            null, null,
            mustGoOnLeave, accruesLeave,
            null, null, null, null
        );
    }

    private BulkStaffRow createRowWithWeeklyHours(String minWeek, String maxWeek) {
        return new BulkStaffRow(
            1, "EMP001", "John", "Doe", "john@test.com", "+1234567890", "permanent",
            List.of("Manager"), List.of("Site A"), List.of("L1"), null,
            "hourly", "25.00", null,
            "4", "10", null, "180", "1", null,
            minWeek, maxWeek,
            null, null,
            null, null, null, null
        );
    }

    private BulkStaffRow createMinimalRow() {
        return new BulkStaffRow(
            1, "EMP001", "John", "Doe", "john@test.com", "+1234567890", "permanent",
            List.of(), List.of(), List.of(), null,
            "hourly", "25.00", null,
            null, null, null, null, null, null,
            null, null,
            null, null,
            null, null, null, null
        );
    }
}
