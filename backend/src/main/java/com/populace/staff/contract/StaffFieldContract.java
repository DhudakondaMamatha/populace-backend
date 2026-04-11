package com.populace.staff.contract;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Single source of truth for all staff configuration fields.
 * Every layer (CSV upload, API validation, allocation engine, UI) derives from this contract.
 *
 * <p>Adding a new field requires:</p>
 * <ol>
 *   <li>Add an entry here with correct metadata</li>
 *   <li>Add the field to {@code BulkStaffRow} record</li>
 *   <li>Add the field to {@code UnifiedStaffCreateRequest} record</li>
 *   <li>The startup validator will fail-fast if any mismatch is detected</li>
 * </ol>
 */
public enum StaffFieldContract {

    // ===== IDENTITY =====
    EMPLOYEE_CODE("employee_code", "employeeCode", FieldCategory.IDENTITY,
            FieldDataType.STRING, false, false),
    FIRST_NAME("first_name", "firstName", FieldCategory.IDENTITY,
            FieldDataType.STRING, true, false),
    LAST_NAME("last_name", "lastName", FieldCategory.IDENTITY,
            FieldDataType.STRING, true, false),
    EMAIL("email", "email", FieldCategory.IDENTITY,
            FieldDataType.STRING, false, false),
    PHONE("phone", "phone", FieldCategory.IDENTITY,
            FieldDataType.STRING, false, false),
    EMPLOYMENT_TYPE("employment_type", "employmentType", FieldCategory.IDENTITY,
            FieldDataType.ENUM, true, true, "permanent", "contract"),

    // ===== ROLE_ASSIGNMENT =====
    ROLES("roles", "roleIds", FieldCategory.ROLE_ASSIGNMENT,
            FieldDataType.STRING, false, true),
    SITES("sites", "siteIds", FieldCategory.ROLE_ASSIGNMENT,
            FieldDataType.STRING, false, true),
    COMPETENCE_LEVELS("competence_levels", "competenceLevels", FieldCategory.ROLE_ASSIGNMENT,
            FieldDataType.STRING, false, true),
    PRIMARY_ROLE("primary_role", "primaryRole", FieldCategory.ROLE_ASSIGNMENT,
            FieldDataType.STRING, false, true),

    // ===== COMPENSATION =====
    COMPENSATION_TYPE("compensation_type", "compensationType", FieldCategory.COMPENSATION,
            FieldDataType.ENUM, false, false, "hourly", "monthly"),
    HOURLY_RATE("hourly_rate", "hourlyRate", FieldCategory.COMPENSATION,
            FieldDataType.DECIMAL, false, true),
    MONTHLY_SALARY("monthly_salary", "monthlySalary", FieldCategory.COMPENSATION,
            FieldDataType.DECIMAL, false, false),

    // ===== WORK_PARAMETERS =====
    MIN_HOURS_PER_DAY("min_hours_per_day", "minHoursPerDay", FieldCategory.WORK_PARAMETERS,
            FieldDataType.DECIMAL, false, true),
    MAX_HOURS_PER_DAY("max_hours_per_day", "maxHoursPerDay", FieldCategory.WORK_PARAMETERS,
            FieldDataType.DECIMAL, false, true),
    MIN_HOURS_PER_WEEK("min_hours_per_week", "minHoursPerWeek", FieldCategory.WORK_PARAMETERS,
            FieldDataType.DECIMAL, false, true),
    MAX_HOURS_PER_WEEK("max_hours_per_week", "maxHoursPerWeek", FieldCategory.WORK_PARAMETERS,
            FieldDataType.DECIMAL, false, true),
    MIN_HOURS_PER_MONTH("min_hours_per_month", "minHoursPerMonth", FieldCategory.WORK_PARAMETERS,
            FieldDataType.DECIMAL, false, true),
    MAX_HOURS_PER_MONTH("max_hours_per_month", "maxHoursPerMonth", FieldCategory.WORK_PARAMETERS,
            FieldDataType.DECIMAL, false, true),
    MIN_DAYS_OFF_PER_WEEK("min_days_off_per_week", "minDaysOffPerWeek", FieldCategory.WORK_PARAMETERS,
            FieldDataType.INTEGER, false, true),
    MAX_SITES_PER_DAY("max_sites_per_day", "maxSitesPerDay", FieldCategory.WORK_PARAMETERS,
            FieldDataType.INTEGER, false, true),

    // ===== MANDATORY_LEAVE =====
    MUST_GO_ON_LEAVE_AFTER_DAYS("must_go_on_leave_after_days", "mustGoOnLeaveAfterDays", FieldCategory.MANDATORY_LEAVE,
            FieldDataType.INTEGER, false, true),
    ACCRUES_ONE_DAY_LEAVE_AFTER_DAYS("accrues_one_day_leave_after_days", "accruesOneDayLeaveAfterDays", FieldCategory.MANDATORY_LEAVE,
            FieldDataType.INTEGER, false, true),

    // ===== BREAK_OVERRIDES =====
    MIN_BREAK_MINUTES("min_break_minutes", "minBreakMinutes", FieldCategory.BREAK_OVERRIDES,
            FieldDataType.INTEGER, false, true),
    MAX_BREAK_MINUTES("max_break_minutes", "maxBreakMinutes", FieldCategory.BREAK_OVERRIDES,
            FieldDataType.INTEGER, false, true),
    MIN_WORK_MINUTES_BEFORE_BREAK("min_work_minutes_before_break", "minWorkMinutesBeforeBreak", FieldCategory.BREAK_OVERRIDES,
            FieldDataType.INTEGER, false, true),
    MAX_CONTINUOUS_WORK_MINUTES("max_continuous_work_minutes", "maxContinuousWorkMinutes", FieldCategory.BREAK_OVERRIDES,
            FieldDataType.INTEGER, false, true);

    // ===== Lookup caches =====
    private static final Map<String, StaffFieldContract> BY_CSV_HEADER;
    private static final List<String> ALL_CSV_HEADERS;
    private static final Set<String> REQUIRED_CSV_HEADERS;

    static {
        Map<String, StaffFieldContract> map = new LinkedHashMap<>();
        List<String> headers = new ArrayList<>();
        Set<String> required = new LinkedHashSet<>();

        for (StaffFieldContract field : values()) {
            map.put(field.csvHeader, field);
            headers.add(field.csvHeader);
            if (field.required) {
                required.add(field.csvHeader);
            }
        }

        BY_CSV_HEADER = Collections.unmodifiableMap(map);
        ALL_CSV_HEADERS = Collections.unmodifiableList(headers);
        REQUIRED_CSV_HEADERS = Collections.unmodifiableSet(required);
    }

    private final String csvHeader;
    private final String apiFieldName;
    private final FieldCategory category;
    private final FieldDataType dataType;
    private final boolean required;
    private final boolean usedInAllocation;
    private final String[] validValues;

    StaffFieldContract(String csvHeader, String apiFieldName, FieldCategory category,
                       FieldDataType dataType, boolean required, boolean usedInAllocation,
                       String... validValues) {
        this.csvHeader = csvHeader;
        this.apiFieldName = apiFieldName;
        this.category = category;
        this.dataType = dataType;
        this.required = required;
        this.usedInAllocation = usedInAllocation;
        this.validValues = validValues;
    }

    public String csvHeader() { return csvHeader; }
    public String apiFieldName() { return apiFieldName; }
    public FieldCategory category() { return category; }
    public FieldDataType dataType() { return dataType; }
    public boolean required() { return required; }
    public boolean usedInAllocation() { return usedInAllocation; }
    public String[] validValues() { return validValues; }

    // ===== Static helpers =====

    /** All CSV headers in declaration order. */
    public static List<String> allCsvHeaders() {
        return ALL_CSV_HEADERS;
    }

    /** CSV headers for fields marked as required. */
    public static Set<String> requiredCsvHeaders() {
        return REQUIRED_CSV_HEADERS;
    }

    /** Lookup a contract entry by its CSV header. */
    public static StaffFieldContract fieldByCsvHeader(String csvHeader) {
        return BY_CSV_HEADER.get(csvHeader);
    }

    /** All fields belonging to the given category. */
    public static List<StaffFieldContract> fieldsByCategory(FieldCategory category) {
        return Arrays.stream(values())
                .filter(f -> f.category == category)
                .toList();
    }

    /** All fields consumed by the allocation/scheduling engine. */
    public static List<StaffFieldContract> allocationFields() {
        return Arrays.stream(values())
                .filter(f -> f.usedInAllocation)
                .toList();
    }

    /** Total number of contract fields. */
    public static int fieldCount() {
        return values().length;
    }

    // ===== Inner enums =====

    public enum FieldCategory {
        IDENTITY,
        ROLE_ASSIGNMENT,
        COMPENSATION,
        WORK_PARAMETERS,
        MANDATORY_LEAVE,
        BREAK_OVERRIDES
    }

    public enum FieldDataType {
        STRING,
        DECIMAL,
        INTEGER,
        LONG,
        ENUM
    }
}
