package com.populace.staff.contract;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Shared validation constants for staff configuration.
 * Referenced by both {@code BulkValidationService} and {@code UnifiedStaffProvisioningService}
 * to prevent drift between CSV upload and API validation paths.
 */
public final class StaffValidationConstants {

    private StaffValidationConstants() {}

    public static final Set<String> VALID_EMPLOYMENT_TYPES = Set.of("permanent", "contract");
    public static final Set<String> VALID_COMPENSATION_TYPES = Set.of("hourly", "monthly");
    public static final Set<String> VALID_COMPETENCE_LEVELS = Set.of("L1", "L2", "L3");
    public static final Set<String> VALID_PROFICIENCY_LEVELS = Set.of("trainee", "competent", "expert");
    public static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[\\d\\s\\-()]{7,20}$");
}
