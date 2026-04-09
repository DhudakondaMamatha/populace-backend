package com.populace.staff.contract;

import com.populace.staff.dto.BulkStaffRow;
import com.populace.staff.dto.UnifiedStaffCreateRequest;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.RecordComponent;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Startup validator that ensures the {@link StaffFieldContract} stays in sync
 * with the actual DTOs used for CSV parsing and API validation.
 *
 * <p>If any mismatch is detected, the application fails to start (fail-fast).</p>
 */
@Component
public class StaffContractStartupValidator {

    private static final Logger log = LoggerFactory.getLogger(StaffContractStartupValidator.class);

    @PostConstruct
    public void validate() {
        List<String> errors = new ArrayList<>();

        validateBulkStaffRowAlignment(errors);
        validateUnifiedCreateRequestAlignment(errors);
        validateFieldCount(errors);

        if (!errors.isEmpty()) {
            String message = "StaffFieldContract validation failed:\n  - " + String.join("\n  - ", errors);
            log.error(message);
            throw new IllegalStateException(message);
        }

        long allocationCount = StaffFieldContract.allocationFields().size();
        log.info("StaffFieldContract: {} fields validated, {} used in allocation",
                StaffFieldContract.fieldCount(), allocationCount);
    }

    private void validateBulkStaffRowAlignment(List<String> errors) {
        Set<String> rowComponents = getRecordComponentNames(BulkStaffRow.class);

        for (StaffFieldContract field : StaffFieldContract.values()) {
            // CSV header must map to a component in BulkStaffRow via its apiFieldName
            String apiName = field.apiFieldName();
            // BulkStaffRow uses different field names for list-type fields
            String bulkRowFieldName = mapToBulkRowFieldName(field);

            if (!rowComponents.contains(bulkRowFieldName)) {
                errors.add("Contract field " + field.name() + " (csvHeader='" + field.csvHeader()
                        + "') has no matching component in BulkStaffRow. Expected: " + bulkRowFieldName);
            }
        }
    }

    private void validateUnifiedCreateRequestAlignment(List<String> errors) {
        Set<String> requestComponents = getRecordComponentNames(UnifiedStaffCreateRequest.class);

        for (StaffFieldContract field : StaffFieldContract.values()) {
            String apiName = field.apiFieldName();
            // Some contract fields map to different names in the request (e.g. roles -> roleIds)
            // The apiFieldName already captures this mapping
            if (!requestComponents.contains(apiName)) {
                // Skip fields that are not directly on UnifiedStaffCreateRequest
                // (e.g., primaryRole is only in BulkStaffRow, not in the API request)
                if (isSkippedForApiRequest(field)) {
                    continue;
                }
                errors.add("Contract field " + field.name() + " (apiFieldName='" + apiName
                        + "') has no matching component in UnifiedStaffCreateRequest");
            }
        }

        // Verify required fields have @NotNull or @NotBlank
        for (StaffFieldContract field : StaffFieldContract.values()) {
            if (!field.required()) continue;
            if (isSkippedForApiRequest(field)) continue;

            String apiName = field.apiFieldName();
            boolean hasRequiredAnnotation = hasRequiredAnnotation(UnifiedStaffCreateRequest.class, apiName);
            if (!hasRequiredAnnotation) {
                errors.add("Contract field " + field.name() + " is marked required but "
                        + "UnifiedStaffCreateRequest." + apiName + " lacks @NotNull/@NotBlank");
            }
        }
    }

    private void validateFieldCount(List<String> errors) {
        // BulkStaffRow has rowNumber as extra component (not a contract field)
        int bulkRowFieldCount = BulkStaffRow.class.getRecordComponents().length - 1;
        int contractFieldCount = StaffFieldContract.fieldCount();

        if (bulkRowFieldCount != contractFieldCount) {
            errors.add("Field count mismatch: StaffFieldContract has " + contractFieldCount
                    + " fields but BulkStaffRow has " + bulkRowFieldCount + " data components (excluding rowNumber)");
        }
    }

    /**
     * Maps a contract field to its BulkStaffRow component name.
     * BulkStaffRow uses the apiFieldName for most fields, but some differ
     * (e.g., "roleIds" in API vs "roles" in BulkStaffRow).
     */
    private String mapToBulkRowFieldName(StaffFieldContract field) {
        return switch (field) {
            case ROLES -> "roles";
            case SITES -> "sites";
            case COMPETENCE_LEVELS -> "competenceLevels";
            case PRIMARY_ROLE -> "primaryRole";
            default -> field.apiFieldName();
        };
    }

    /**
     * Fields that exist in BulkStaffRow but not in UnifiedStaffCreateRequest.
     * These are resolved during bulk processing (e.g., primaryRole is resolved to roleAssignments).
     */
    private boolean isSkippedForApiRequest(StaffFieldContract field) {
        return field == StaffFieldContract.PRIMARY_ROLE
            || field.category() == StaffFieldContract.FieldCategory.BREAK_OVERRIDES;
    }

    private Set<String> getRecordComponentNames(Class<?> recordClass) {
        return Arrays.stream(recordClass.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());
    }

    private boolean hasRequiredAnnotation(Class<?> recordClass, String componentName) {
        for (RecordComponent component : recordClass.getRecordComponents()) {
            if (component.getName().equals(componentName)) {
                // Check accessor method annotations (record annotations propagate there)
                boolean onAccessor = Arrays.stream(component.getAccessor().getAnnotations())
                        .anyMatch(a -> {
                            String name = a.annotationType().getSimpleName();
                            return "NotNull".equals(name) || "NotBlank".equals(name);
                        });
                if (onAccessor) return true;

                // Also check the backing field annotations
                try {
                    java.lang.reflect.Field field = recordClass.getDeclaredField(componentName);
                    return Arrays.stream(field.getAnnotations())
                            .anyMatch(a -> {
                                String name = a.annotationType().getSimpleName();
                                return "NotNull".equals(name) || "NotBlank".equals(name);
                            });
                } catch (NoSuchFieldException e) {
                    return false;
                }
            }
        }
        return false;
    }
}
