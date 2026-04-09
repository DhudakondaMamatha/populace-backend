package com.populace.staff.service;

import com.populace.staff.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for orchestrating bulk staff upload operations.
 * Uses UnifiedStaffProvisioningService for actual staff creation,
 * ensuring consistent logic between manual and bulk creation.
 */
@Service
public class BulkStaffUploadService {

    private static final Logger log = LoggerFactory.getLogger(BulkStaffUploadService.class);

    private final CsvParserService csvParserService;
    private final BulkValidationService validationService;
    private final BulkRowTransactionHelper rowTransactionHelper;

    public BulkStaffUploadService(CsvParserService csvParserService,
                                   BulkValidationService validationService,
                                   BulkRowTransactionHelper rowTransactionHelper) {
        this.csvParserService = csvParserService;
        this.validationService = validationService;
        this.rowTransactionHelper = rowTransactionHelper;
    }

    /**
     * Parses a CSV file, validates all rows, and creates staff members.
     * Creation errors are caught per-row and returned as structured errors.
     */
    public BulkUploadResponse parseAndCreateStaffFromCsv(Long businessId, MultipartFile file) {
        log.info("Processing bulk staff upload for business {}", businessId);

        // Step 1: Parse CSV
        CsvParserService.ParseResult parseResult = csvParserService.parse(file);
        if (parseResult.hasErrors()) {
            log.warn("CSV parsing failed with {} errors", parseResult.errors().size());
            return BulkUploadResponse.withValidationErrors(0, parseResult.errors());
        }

        int totalRows = parseResult.rows().size();
        log.info("Parsed {} rows from CSV", totalRows);

        // Step 2: Validate all rows
        BulkValidationService.ValidationResult validationResult =
            validationService.validate(businessId, parseResult.rows());

        if (validationResult.hasErrors()) {
            log.warn("Validation failed with {} errors", validationResult.errors().size());
            return BulkUploadResponse.withValidationErrors(totalRows, validationResult.errors());
        }

        log.info("Validation passed for all {} rows", totalRows);

        // Step 3: Create staff members, catching per-row creation failures
        return processValidatedRows(businessId, totalRows, validationResult.validatedRows());
    }

    /**
     * Creates staff members from validated rows.
     * Catches per-row creation failures and returns structured errors.
     */
    private BulkUploadResponse processValidatedRows(Long businessId, int totalRows, List<BulkStaffRow.Validated> rows) {
        List<BulkUploadResponse.CreatedStaffSummary> createdStaff = new ArrayList<>();
        List<BulkUploadError> creationErrors = new ArrayList<>();

        for (BulkStaffRow.Validated row : rows) {
            try {
                UnifiedStaffCreateRequest request = convertToStaffCreateRequest(row);
                StaffDto created = rowTransactionHelper.createStaffInNewTransaction(businessId, request);

                createdStaff.add(new BulkUploadResponse.CreatedStaffSummary(
                    created.id(),
                    created.employeeCode(),
                    created.firstName() + " " + created.lastName()
                ));
            } catch (Exception e) {
                log.error("Failed to create staff member at row {}: {}", row.rowNumber(), e.getMessage());
                creationErrors.add(new BulkUploadError(
                    row.rowNumber(), null, null, "CREATION_FAILED", e.getMessage()
                ));
            }
        }

        if (!creationErrors.isEmpty()) {
            log.warn("Creation failed for {} rows", creationErrors.size());
            return BulkUploadResponse.withValidationErrors(totalRows, creationErrors);
        }

        log.info("Successfully created {} staff members", createdStaff.size());
        return BulkUploadResponse.success(totalRows, createdStaff);
    }

    /**
     * Converts a validated bulk upload row to a unified staff create request.
     * This ensures both bulk and manual creation use the same data model.
     */
    private UnifiedStaffCreateRequest convertToStaffCreateRequest(BulkStaffRow.Validated row) {
        List<RoleAssignment> roleAssignments = buildRoleListWithPrimaryFlag(
            row.roleIds(), row.competenceLevels(), row.primaryRoleId(),
            row.minBreakMinutes(), row.maxBreakMinutes(),
            row.minWorkMinutesBeforeBreak(), row.maxContinuousWorkMinutes());

        return new UnifiedStaffCreateRequest(
            row.firstName(),
            row.lastName(),
            row.email(),
            row.phone(),
            row.employeeCode(),
            row.employmentType(),
            null, // roleIds - using roleAssignments instead
            row.siteIds(),
            row.competenceLevels(), // Pass competence levels for StaffCompetenceLevel creation
            roleAssignments,
            row.compensationType(),
            row.hourlyRate(),
            row.monthlySalary(),
            row.minHoursPerDay(),
            row.maxHoursPerDay(),
            row.minHoursPerMonth(),
            row.maxHoursPerMonth(),
            row.minDaysOffPerWeek(),
            row.maxSitesPerDay(),
            row.minHoursPerWeek(),
            row.maxHoursPerWeek(),
            row.mustGoOnLeaveAfterDays(),
            row.accruesOneDayLeaveAfterDays()
        );
    }

    /**
     * Pairs role IDs with competence levels by position.
     * CSV format: roles and competence_levels columns are pipe-separated and aligned by position.
     * Example: roles="cheff|waiter|manager" competence_levels="L2|L3|L3"
     * Results in: cheff→competent, waiter→expert, manager→expert
     *
     * Mapping: L1→trainee, L2→competent, L3→expert
     */
    private List<RoleAssignment> buildRoleListWithPrimaryFlag(
            List<Long> roleIds, List<String> competenceLevels, Long primaryRoleId,
            Integer minBreakMinutes, Integer maxBreakMinutes,
            Integer minWorkMinutesBeforeBreak, Integer maxContinuousWorkMinutes) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }

        List<RoleAssignment> assignments = new ArrayList<>();
        for (int i = 0; i < roleIds.size(); i++) {
            Long roleId = roleIds.get(i);
            String competenceLevel = getCompetenceLevelAtIndex(competenceLevels, i);
            String skillLevel = mapCompetenceToSkillLevel(competenceLevel);
            Boolean isPrimary = primaryRoleId != null ? roleId.equals(primaryRoleId) : null;
            assignments.add(new RoleAssignment(roleId, skillLevel, null, isPrimary,
                minBreakMinutes, maxBreakMinutes, minWorkMinutesBeforeBreak, maxContinuousWorkMinutes));
        }
        return assignments;
    }

    private String getCompetenceLevelAtIndex(List<String> competenceLevels, int index) {
        if (competenceLevels == null || index >= competenceLevels.size()) {
            return null;
        }
        return competenceLevels.get(index);
    }

    /**
     * Maps CSV competence levels to skill levels (L1, L2, L3).
     * Passes through if already in L-format.
     */
    private String mapCompetenceToSkillLevel(String competenceLevel) {
        if (competenceLevel == null) {
            return null;
        }
        return switch (competenceLevel.toUpperCase()) {
            case "L1", "L2", "L3" -> competenceLevel.toUpperCase();
            case "TRAINEE" -> "L1";
            case "COMPETENT" -> "L2";
            case "EXPERT" -> "L3";
            default -> competenceLevel;
        };
    }

    /**
     * Generates sample CSV content for download.
     */
    public String generateSampleCsv() {
        return csvParserService.generateSampleCsv();
    }
}
