package com.populace.staff.service;

import com.populace.common.util.EmailValidator;
import com.populace.domain.Role;
import com.populace.domain.Site;
import com.populace.repository.RoleRepository;
import com.populace.repository.SiteRepository;
import com.populace.repository.StaffMemberRepository;
import com.populace.staff.contract.StaffValidationConstants;
import com.populace.staff.dto.BulkStaffRow;
import com.populace.staff.dto.BulkUploadError;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class BulkValidationService {

    private static final BigDecimal TWENTY_FOUR = new BigDecimal("24");

    private final StaffMemberRepository staffRepository;
    private final RoleRepository roleRepository;
    private final SiteRepository siteRepository;

    public BulkValidationService(StaffMemberRepository staffRepository,
                                  RoleRepository roleRepository,
                                  SiteRepository siteRepository) {
        this.staffRepository = staffRepository;
        this.roleRepository = roleRepository;
        this.siteRepository = siteRepository;
    }

    public record ValidationResult(
        List<BulkStaffRow.Validated> validatedRows,
        List<BulkUploadError> errors
    ) {
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }

    public ValidationResult validate(Long businessId, List<BulkStaffRow> rows) {
        ValidationContext context = buildContext(businessId, rows);
        return validateAllRows(rows, context);
    }

    private ValidationContext buildContext(Long businessId, List<BulkStaffRow> rows) {
        return new ValidationContext(
            loadRoleNameToIdMap(businessId),
            loadSiteNameToIdMap(businessId),
            loadExistingEmails(businessId),
            loadExistingEmployeeCodes(businessId),
            collectEmailOccurrences(rows),
            collectEmployeeCodeOccurrences(rows)
        );
    }

    private ValidationResult validateAllRows(List<BulkStaffRow> rows, ValidationContext context) {
        List<BulkUploadError> errors = new ArrayList<>();
        List<BulkStaffRow.Validated> validated = new ArrayList<>();

        for (BulkStaffRow row : rows) {
            List<BulkUploadError> rowErrors = findErrorsInRow(row, context);

            if (rowErrors.isEmpty()) {
                validated.add(convertRow(row, context));
            } else {
                errors.addAll(rowErrors);
            }
        }

        return new ValidationResult(validated, errors);
    }

    private List<BulkUploadError> findErrorsInRow(BulkStaffRow row, ValidationContext context) {
        List<BulkUploadError> errors = new ArrayList<>();
        int rowNum = row.rowNumber();

        ensureNameAndTypeArePresent(errors, rowNum, row);
        ensureEmailIsValid(errors, rowNum, row.email(), context);
        ensureEmployeeCodeIsUnique(errors, rowNum, row.employeeCode(), context);
        ensurePhoneFormatIsValid(errors, rowNum, row.phone());
        ensureRolesAndSitesExist(errors, rowNum, row, context);
        ensurePrimaryRoleIsAssigned(errors, rowNum, row, context);
        ensureCompensationIsComplete(errors, rowNum, row);
        ensureWorkHoursAreValid(errors, rowNum, row);
        validateMandatoryLeave(errors, rowNum, row);
        validateBreakOverrides(errors, rowNum, row);

        return errors;
    }

    // ========== Identity Validation ==========

    private void ensureNameAndTypeArePresent(List<BulkUploadError> errors, int rowNum, BulkStaffRow row) {
        if (isBlank(row.firstName())) {
            errors.add(error(rowNum, "first_name", "", "REQUIRED", "First name is required"));
        }
        if (isBlank(row.lastName())) {
            errors.add(error(rowNum, "last_name", "", "REQUIRED", "Last name is required"));
        }
        if (isBlank(row.employmentType())) {
            errors.add(error(rowNum, "employment_type", "", "REQUIRED", "Employment type is required"));
        } else if (!StaffValidationConstants.VALID_EMPLOYMENT_TYPES.contains(row.employmentType().toLowerCase())) {
            errors.add(error(rowNum, "employment_type", row.employmentType(), "INVALID",
                "Must be: permanent or contract"));
        }
    }

    private void ensureEmailIsValid(List<BulkUploadError> errors, int rowNum, String email, ValidationContext context) {
        if (isBlank(email)) return;

        String emailLower = email.toLowerCase();

        if (!EmailValidator.isValid(email)) {
            errors.add(error(rowNum, "email", email, "INVALID", "Invalid email format"));
            return;
        }

        if (context.existingEmails.contains(emailLower)) {
            errors.add(error(rowNum, "email", email, "EXISTS", "Email already exists"));
            return;
        }

        List<Integer> duplicates = context.emailOccurrences.get(emailLower);
        if (duplicates != null && duplicates.size() > 1) {
            errors.add(error(rowNum, "email", email, "DUPLICATE", "Duplicate in file (rows " + duplicates + ")"));
        }
    }

    private void ensureEmployeeCodeIsUnique(List<BulkUploadError> errors, int rowNum, String code, ValidationContext context) {
        if (isBlank(code)) return;

        String codeLower = code.toLowerCase();

        if (context.existingEmployeeCodes.contains(codeLower)) {
            errors.add(error(rowNum, "employee_code", code, "EXISTS", "Employee code already exists"));
            return;
        }

        List<Integer> duplicates = context.employeeCodeOccurrences.get(codeLower);
        if (duplicates != null && duplicates.size() > 1) {
            errors.add(error(rowNum, "employee_code", code, "DUPLICATE", "Duplicate in file (rows " + duplicates + ")"));
        }
    }

    // ========== Phone Validation ==========

    private void ensurePhoneFormatIsValid(List<BulkUploadError> errors, int rowNum, String phone) {
        if (isBlank(phone)) return;

        if (!StaffValidationConstants.PHONE_PATTERN.matcher(phone.trim()).matches()) {
            errors.add(error(rowNum, "phone", phone, "INVALID", "Invalid phone format"));
        }
    }

    // ========== Assignment Validation ==========

    private void ensureRolesAndSitesExist(List<BulkUploadError> errors, int rowNum, BulkStaffRow row, ValidationContext context) {
        for (String roleName : row.roles()) {
            String trimmed = roleName.trim();
            if (trimmed.isEmpty()) {
                errors.add(error(rowNum, "roles", roleName, "INVALID", "Empty role entry"));
            } else if (!context.roleNameToId.containsKey(trimmed.toLowerCase())) {
                errors.add(error(rowNum, "roles", trimmed, "NOT_FOUND", "Role not found: " + trimmed));
            }
        }

        for (String siteName : row.sites()) {
            String trimmed = siteName.trim();
            if (trimmed.isEmpty()) {
                errors.add(error(rowNum, "sites", siteName, "INVALID", "Empty site entry"));
            } else if (!context.siteNameToId.containsKey(trimmed.toLowerCase())) {
                errors.add(error(rowNum, "sites", trimmed, "NOT_FOUND", "Site not found: " + trimmed));
            }
        }

        for (String level : row.competenceLevels()) {
            String trimmed = level.trim();
            if (trimmed.isEmpty()) {
                errors.add(error(rowNum, "competence_levels", level, "INVALID", "Empty competence level"));
            } else if (!StaffValidationConstants.VALID_COMPETENCE_LEVELS.contains(trimmed.toUpperCase())) {
                errors.add(error(rowNum, "competence_levels", trimmed, "INVALID", "Must be: L1, L2, or L3"));
            }
        }
    }

    // ========== Primary Role Validation ==========

    private void ensurePrimaryRoleIsAssigned(List<BulkUploadError> errors, int rowNum, BulkStaffRow row, ValidationContext context) {
        if (isBlank(row.primaryRole())) return;

        String trimmed = row.primaryRole().trim();
        if (!row.roles().stream().anyMatch(r -> r.trim().equalsIgnoreCase(trimmed))) {
            errors.add(error(rowNum, "primary_role", trimmed, "INVALID",
                "Primary role must be one of the assigned roles"));
        }
    }

    // ========== Compensation Validation ==========

    private void ensureCompensationIsComplete(List<BulkUploadError> errors, int rowNum, BulkStaffRow row) {
        // If all compensation fields are blank, compensation is optional (same as UI behavior)
        if (isBlank(row.compensationType()) && isBlank(row.hourlyRate()) && isBlank(row.monthlySalary())) {
            return;
        }

        // If any compensation field is set, compensation_type is required
        if (isBlank(row.compensationType())) {
            errors.add(error(rowNum, "compensation_type", "", "REQUIRED",
                "compensation_type is required when hourly_rate or monthly_salary is provided"));
            return;
        }

        String type = row.compensationType().toLowerCase();

        if (!StaffValidationConstants.VALID_COMPENSATION_TYPES.contains(type)) {
            errors.add(error(rowNum, "compensation_type", row.compensationType(), "INVALID",
                "Must be: hourly or monthly"));
            return;
        }

        if (type.equals("hourly")) {
            if (isBlank(row.hourlyRate())) {
                errors.add(error(rowNum, "hourly_rate", "", "REQUIRED", "Required for hourly compensation"));
            } else {
                BigDecimal rate = toDecimalOrNull(row.hourlyRate());
                if (rate == null) {
                    errors.add(error(rowNum, "hourly_rate", row.hourlyRate(), "INVALID",
                        "hourly_rate: '" + row.hourlyRate() + "' is not a valid number"));
                } else if (rate.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add(error(rowNum, "hourly_rate", row.hourlyRate(), "INVALID", "Must be positive number"));
                }
            }
        } else {
            if (isBlank(row.monthlySalary())) {
                errors.add(error(rowNum, "monthly_salary", "", "REQUIRED", "Required for monthly compensation"));
            } else {
                BigDecimal salary = toDecimalOrNull(row.monthlySalary());
                if (salary == null) {
                    errors.add(error(rowNum, "monthly_salary", row.monthlySalary(), "INVALID",
                        "monthly_salary: '" + row.monthlySalary() + "' is not a valid number"));
                } else if (salary.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add(error(rowNum, "monthly_salary", row.monthlySalary(), "INVALID", "Must be positive number"));
                }
            }
        }
    }

    // ========== Work Hours Validation ==========

    private void ensureWorkHoursAreValid(List<BulkUploadError> errors, int rowNum, BulkStaffRow row) {
        // All work parameter fields are REQUIRED
        BigDecimal minPerDay = ensureNumericValueIsValid(errors, rowNum, "min_hours_per_day", row.minHoursPerDay());
        BigDecimal maxPerDay = ensureNumericValueIsValid(errors, rowNum, "max_hours_per_day", row.maxHoursPerDay());
        BigDecimal minPerWeek = ensureNumericValueIsValid(errors, rowNum, "min_hours_per_week", row.minHoursPerWeek());
        BigDecimal maxPerWeek = ensureNumericValueIsValid(errors, rowNum, "max_hours_per_week", row.maxHoursPerWeek());
        BigDecimal minPerMonth = ensureNumericValueIsValid(errors, rowNum, "min_hours_per_month", row.minHoursPerMonth());
        BigDecimal maxPerMonth = ensureNumericValueIsValid(errors, rowNum, "max_hours_per_month", row.maxHoursPerMonth());

        // Required field checks
        if (isBlank(row.minHoursPerDay())) {
            errors.add(error(rowNum, "min_hours_per_day", "", "REQUIRED", "minHoursPerDay is required"));
        }
        if (isBlank(row.maxHoursPerDay())) {
            errors.add(error(rowNum, "max_hours_per_day", "", "REQUIRED", "maxHoursPerDay is required"));
        }
        if (isBlank(row.minHoursPerWeek())) {
            errors.add(error(rowNum, "min_hours_per_week", "", "REQUIRED", "minHoursPerWeek is required"));
        }
        if (isBlank(row.maxHoursPerWeek())) {
            errors.add(error(rowNum, "max_hours_per_week", "", "REQUIRED", "maxHoursPerWeek is required"));
        }
        if (isBlank(row.minHoursPerMonth())) {
            errors.add(error(rowNum, "min_hours_per_month", "", "REQUIRED", "minHoursPerMonth is required"));
        }
        if (isBlank(row.maxHoursPerMonth())) {
            errors.add(error(rowNum, "max_hours_per_month", "", "REQUIRED", "maxHoursPerMonth is required"));
        }
        if (isBlank(row.minDaysOffPerWeek())) {
            errors.add(error(rowNum, "min_days_off_per_week", "", "REQUIRED", "minDaysOffPerWeek is required"));
        }
        if (isBlank(row.maxSitesPerDay())) {
            errors.add(error(rowNum, "max_sites_per_day", "", "REQUIRED", "maxSitesPerDay is required"));
        }

        // Must be strictly positive (aligned with StaffConstraintValidator)
        ensureValueIsPositive(errors, rowNum, "min_hours_per_day", minPerDay);
        ensureValueIsPositive(errors, rowNum, "max_hours_per_day", maxPerDay);
        ensureValueIsPositive(errors, rowNum, "max_hours_per_month", maxPerMonth);

        // Non-negative checks (zero is allowed for these)
        ensureValueIsNotNegative(errors, rowNum, "min_hours_per_week", minPerWeek);
        ensureValueIsNotNegative(errors, rowNum, "max_hours_per_week", maxPerWeek);
        ensureValueIsNotNegative(errors, rowNum, "min_hours_per_month", minPerMonth);

        // 24-hour upper bound for daily hours
        if (minPerDay != null && minPerDay.compareTo(TWENTY_FOUR) > 0) {
            errors.add(error(rowNum, "min_hours_per_day", row.minHoursPerDay(), "INVALID",
                "Cannot exceed 24 hours"));
        }
        if (maxPerDay != null && maxPerDay.compareTo(TWENTY_FOUR) > 0) {
            errors.add(error(rowNum, "max_hours_per_day", row.maxHoursPerDay(), "INVALID",
                "Cannot exceed 24 hours"));
        }

        // Range validations
        if (minPerDay != null && maxPerDay != null && minPerDay.compareTo(maxPerDay) > 0) {
            errors.add(error(rowNum, "max_hours_per_day", row.maxHoursPerDay(), "INVALID",
                "Max hours per day must be >= min"));
        }

        if (minPerWeek != null && maxPerWeek != null && minPerWeek.compareTo(maxPerWeek) > 0) {
            errors.add(error(rowNum, "max_hours_per_week", row.maxHoursPerWeek(), "INVALID",
                "Max hours per week must be >= min"));
        }

        if (minPerMonth != null && maxPerMonth != null && minPerMonth.compareTo(maxPerMonth) > 0) {
            errors.add(error(rowNum, "max_hours_per_month", row.maxHoursPerMonth(), "INVALID",
                "Max hours per month must be >= min"));
        }

        // Upper bound validations
        if (maxPerWeek != null && maxPerWeek.compareTo(new BigDecimal("168")) > 0) {
            errors.add(error(rowNum, "max_hours_per_week", row.maxHoursPerWeek(), "INVALID",
                "Cannot exceed 168 hours per week"));
        }
        if (maxPerMonth != null && maxPerMonth.compareTo(new BigDecimal("744")) > 0) {
            errors.add(error(rowNum, "max_hours_per_month", row.maxHoursPerMonth(), "INVALID",
                "Cannot exceed 744 hours per month"));
        }

        // Cross-tier consistency
        if (maxPerMonth != null && maxPerDay != null && maxPerMonth.compareTo(maxPerDay) < 0) {
            errors.add(error(rowNum, "max_hours_per_month", row.maxHoursPerMonth(), "INVALID",
                "Max hours per month cannot be less than max hours per day"));
        }
        if (maxPerWeek != null && maxPerDay != null && maxPerWeek.compareTo(maxPerDay) < 0) {
            errors.add(error(rowNum, "max_hours_per_week", row.maxHoursPerWeek(), "INVALID",
                "Max hours per week cannot be less than max hours per day"));
        }
        if (maxPerMonth != null && maxPerWeek != null && maxPerMonth.compareTo(maxPerWeek) < 0) {
            errors.add(error(rowNum, "max_hours_per_month", row.maxHoursPerMonth(), "INVALID",
                "Max hours per month cannot be less than max hours per week"));
        }

        // Integer field validations
        Integer daysOff = toIntegerOrNull(row.minDaysOffPerWeek());
        if (daysOff != null && (daysOff < 0 || daysOff > 7)) {
            errors.add(error(rowNum, "min_days_off_per_week", row.minDaysOffPerWeek(), "INVALID",
                "Must be between 0 and 7"));
        }
        if (!isBlank(row.minDaysOffPerWeek()) && daysOff == null) {
            errors.add(error(rowNum, "min_days_off_per_week", row.minDaysOffPerWeek(), "INVALID",
                "min_days_off_per_week: '" + row.minDaysOffPerWeek() + "' is not a valid number"));
        }

        Integer maxSites = toIntegerOrNull(row.maxSitesPerDay());
        if (maxSites != null && maxSites < 1) {
            errors.add(error(rowNum, "max_sites_per_day", row.maxSitesPerDay(), "INVALID",
                "Must be at least 1"));
        }
        if (maxSites != null && maxSites > 10) {
            errors.add(error(rowNum, "max_sites_per_day", row.maxSitesPerDay(), "INVALID",
                "Cannot exceed 10 sites per day"));
        }
        if (!isBlank(row.maxSitesPerDay()) && maxSites == null) {
            errors.add(error(rowNum, "max_sites_per_day", row.maxSitesPerDay(), "INVALID",
                "max_sites_per_day: '" + row.maxSitesPerDay() + "' is not a valid number"));
        }
    }

    /**
     * Parses a numeric string and reports an error if non-blank but unparseable.
     * Returns the parsed value or null.
     */
    private BigDecimal ensureNumericValueIsValid(List<BulkUploadError> errors, int rowNum, String field, String rawValue) {
        if (isBlank(rawValue)) return null;

        BigDecimal parsed = toDecimalOrNull(rawValue);
        if (parsed == null) {
            errors.add(error(rowNum, field, rawValue, "INVALID",
                field + ": '" + rawValue + "' is not a valid number"));
        }
        return parsed;
    }

    // ========== Mandatory Leave Validation ==========

    private void validateMandatoryLeave(List<BulkUploadError> errors, int rowNum, BulkStaffRow row) {
        Integer mustGoOnLeave = toIntegerOrNull(row.mustGoOnLeaveAfterDays());
        if (mustGoOnLeave != null && mustGoOnLeave < 1) {
            errors.add(error(rowNum, "must_go_on_leave_after_days", row.mustGoOnLeaveAfterDays(), "INVALID",
                "Must be at least 1"));
        }

        Integer accruesLeave = toIntegerOrNull(row.accruesOneDayLeaveAfterDays());
        if (accruesLeave != null && accruesLeave < 1) {
            errors.add(error(rowNum, "accrues_one_day_leave_after_days", row.accruesOneDayLeaveAfterDays(), "INVALID",
                "Must be at least 1"));
        }
    }

    // ========== Break Override Validation ==========

    private void validateBreakOverrides(List<BulkUploadError> errors, int rowNum, BulkStaffRow row) {
        Integer minBreak = toIntegerOrNull(row.minBreakMinutes());
        if (!isBlank(row.minBreakMinutes()) && minBreak == null) {
            errors.add(error(rowNum, "min_break_minutes", row.minBreakMinutes(), "INVALID",
                "min_break_minutes: '" + row.minBreakMinutes() + "' is not a valid number"));
        }
        if (minBreak != null && minBreak < 0) {
            errors.add(error(rowNum, "min_break_minutes", row.minBreakMinutes(), "INVALID", "Cannot be negative"));
        }

        Integer maxBreak = toIntegerOrNull(row.maxBreakMinutes());
        if (!isBlank(row.maxBreakMinutes()) && maxBreak == null) {
            errors.add(error(rowNum, "max_break_minutes", row.maxBreakMinutes(), "INVALID",
                "max_break_minutes: '" + row.maxBreakMinutes() + "' is not a valid number"));
        }
        if (maxBreak != null && maxBreak < 0) {
            errors.add(error(rowNum, "max_break_minutes", row.maxBreakMinutes(), "INVALID", "Cannot be negative"));
        }

        if (minBreak != null && maxBreak != null && minBreak > maxBreak) {
            errors.add(error(rowNum, "max_break_minutes", row.maxBreakMinutes(), "INVALID",
                "max_break_minutes must be >= min_break_minutes"));
        }

        Integer minWork = toIntegerOrNull(row.minWorkMinutesBeforeBreak());
        if (!isBlank(row.minWorkMinutesBeforeBreak()) && minWork == null) {
            errors.add(error(rowNum, "min_work_minutes_before_break", row.minWorkMinutesBeforeBreak(), "INVALID",
                "min_work_minutes_before_break: '" + row.minWorkMinutesBeforeBreak() + "' is not a valid number"));
        }
        if (minWork != null && minWork < 0) {
            errors.add(error(rowNum, "min_work_minutes_before_break", row.minWorkMinutesBeforeBreak(), "INVALID", "Cannot be negative"));
        }

        Integer maxContinuous = toIntegerOrNull(row.maxContinuousWorkMinutes());
        if (!isBlank(row.maxContinuousWorkMinutes()) && maxContinuous == null) {
            errors.add(error(rowNum, "max_continuous_work_minutes", row.maxContinuousWorkMinutes(), "INVALID",
                "max_continuous_work_minutes: '" + row.maxContinuousWorkMinutes() + "' is not a valid number"));
        }
        if (maxContinuous != null && maxContinuous < 0) {
            errors.add(error(rowNum, "max_continuous_work_minutes", row.maxContinuousWorkMinutes(), "INVALID", "Cannot be negative"));
        }
    }

    // ========== Row Conversion ==========

    private BulkStaffRow.Validated convertRow(BulkStaffRow row, ValidationContext context) {
        return new BulkStaffRow.Validated(
            row.rowNumber(),

            // Identity
            row.employeeCode(),
            row.firstName(),
            row.lastName(),
            row.email(),
            row.phone(),
            row.employmentType().toLowerCase(),

            // Assignments
            convertRolesToIds(row.roles(), context.roleNameToId),
            convertSitesToIds(row.sites(), context.siteNameToId),
            normalizeCompetenceLevels(row.competenceLevels()),
            resolvePrimaryRoleId(row.primaryRole(), context.roleNameToId),

            // Compensation
            isBlank(row.compensationType()) ? null : row.compensationType().toLowerCase(),
            toDecimalOrNull(row.hourlyRate()),
            toDecimalOrNull(row.monthlySalary()),

            // Work hours
            toDecimalOrNull(row.minHoursPerDay()),
            toDecimalOrNull(row.maxHoursPerDay()),
            toDecimalOrNull(row.minHoursPerMonth()),
            toDecimalOrNull(row.maxHoursPerMonth()),
            toIntegerOrNull(row.minDaysOffPerWeek()),
            toIntegerOrNull(row.maxSitesPerDay()),
            toDecimalOrNull(row.minHoursPerWeek()),
            toDecimalOrNull(row.maxHoursPerWeek()),

            // Mandatory leave
            toIntegerOrNull(row.mustGoOnLeaveAfterDays()),
            toIntegerOrNull(row.accruesOneDayLeaveAfterDays()),

            // Break overrides
            toIntegerOrNull(row.minBreakMinutes()),
            toIntegerOrNull(row.maxBreakMinutes()),
            toIntegerOrNull(row.minWorkMinutesBeforeBreak()),
            toIntegerOrNull(row.maxContinuousWorkMinutes())
        );
    }

    private List<Long> convertRolesToIds(List<String> names, Map<String, Long> nameToId) {
        return names.stream()
            .map(String::trim)
            .filter(n -> !n.isEmpty())
            .map(n -> nameToId.get(n.toLowerCase()))
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    private List<Long> convertSitesToIds(List<String> names, Map<String, Long> nameToId) {
        return names.stream()
            .map(String::trim)
            .filter(n -> !n.isEmpty())
            .map(n -> nameToId.get(n.toLowerCase()))
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    private Long resolvePrimaryRoleId(String primaryRole, Map<String, Long> roleNameToId) {
        if (isBlank(primaryRole)) return null;
        return roleNameToId.get(primaryRole.trim().toLowerCase());
    }

    private List<String> normalizeCompetenceLevels(List<String> levels) {
        return levels.stream()
            .map(String::trim)
            .filter(l -> !l.isEmpty())
            .map(String::toUpperCase)
            .distinct()
            .toList();
    }

    // ========== Data Loading ==========

    private Map<String, Long> loadRoleNameToIdMap(Long businessId) {
        Map<String, Long> map = new HashMap<>();
        for (Role role : roleRepository.findByBusiness_IdAndDeletedAtIsNull(businessId)) {
            map.put(role.getName().toLowerCase(), role.getId());
        }
        return map;
    }

    private Map<String, Long> loadSiteNameToIdMap(Long businessId) {
        Map<String, Long> map = new HashMap<>();
        for (Site site : siteRepository.findByBusiness_IdAndDeletedAtIsNull(businessId)) {
            map.put(site.getName().toLowerCase(), site.getId());
        }
        return map;
    }

    private Set<String> loadExistingEmails(Long businessId) {
        Set<String> emails = new HashSet<>();
        staffRepository.findByBusiness_IdAndDeletedAtIsNull(businessId).forEach(staff -> {
            if (staff.getEmail() != null) {
                emails.add(staff.getEmail().toLowerCase());
            }
        });
        return emails;
    }

    private Set<String> loadExistingEmployeeCodes(Long businessId) {
        Set<String> codes = new HashSet<>();
        staffRepository.findByBusiness_IdAndDeletedAtIsNull(businessId).forEach(staff -> {
            if (staff.getEmployeeCode() != null) {
                codes.add(staff.getEmployeeCode().toLowerCase());
            }
        });
        return codes;
    }

    private Map<String, List<Integer>> collectEmailOccurrences(List<BulkStaffRow> rows) {
        Map<String, List<Integer>> map = new HashMap<>();
        for (BulkStaffRow row : rows) {
            if (!isBlank(row.email())) {
                map.computeIfAbsent(row.email().toLowerCase(), k -> new ArrayList<>()).add(row.rowNumber());
            }
        }
        return map;
    }

    private Map<String, List<Integer>> collectEmployeeCodeOccurrences(List<BulkStaffRow> rows) {
        Map<String, List<Integer>> map = new HashMap<>();
        for (BulkStaffRow row : rows) {
            if (!isBlank(row.employeeCode())) {
                map.computeIfAbsent(row.employeeCode().toLowerCase(), k -> new ArrayList<>()).add(row.rowNumber());
            }
        }
        return map;
    }

    // ========== Utilities ==========

    private void ensureValueIsPositive(List<BulkUploadError> errors, int rowNum, String field, BigDecimal value) {
        if (value != null && value.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(error(rowNum, field, value.toString(), "INVALID", "Must be greater than 0"));
        }
    }

    private void ensureValueIsNotNegative(List<BulkUploadError> errors, int rowNum, String field, BigDecimal value) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            errors.add(error(rowNum, field, value.toString(), "INVALID", "Cannot be negative"));
        }
    }

    private BulkUploadError error(int row, String column, String value, String code, String message) {
        return new BulkUploadError(row, column, value, code, message);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private BigDecimal toDecimalOrNull(String value) {
        if (isBlank(value)) return null;
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer toIntegerOrNull(String value) {
        if (isBlank(value)) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ========== Context ==========

    private record ValidationContext(
        Map<String, Long> roleNameToId,
        Map<String, Long> siteNameToId,
        Set<String> existingEmails,
        Set<String> existingEmployeeCodes,
        Map<String, List<Integer>> emailOccurrences,
        Map<String, List<Integer>> employeeCodeOccurrences
    ) {}
}
