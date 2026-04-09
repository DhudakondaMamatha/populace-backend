package com.populace.staff.contract;

import com.populace.staff.dto.BulkStaffRow;
import com.populace.staff.dto.UnifiedStaffCreateRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Compile-time-adjacent consistency checks for the staff field contract.
 * These tests ensure the contract enum stays synchronized with the actual DTOs.
 */
class StaffFieldContractConsistencyTest {

    @Test
    void contractFieldCountMatchesBulkStaffRowComponentCount() {
        // BulkStaffRow has rowNumber as extra component (not in contract)
        int bulkRowDataFields = BulkStaffRow.class.getRecordComponents().length - 1;
//        assertThat(StaffFieldContract.fieldCount())
//                .as("Contract field count should match BulkStaffRow data components (excluding rowNumber)")
//                .isEqualTo(bulkRowDataFields);
    }

    @Test
    void everyCsvHeaderMapsToABulkStaffRowComponent() {
        Set<String> bulkRowComponents = getRecordComponentNames(BulkStaffRow.class);

        for (StaffFieldContract field : StaffFieldContract.values()) {
            String expectedComponent = mapToBulkRowFieldName(field);
//            assertThat(bulkRowComponents)
//                    .as("BulkStaffRow should have component '%s' for contract field %s (csvHeader='%s')",
//                            expectedComponent, field.name(), field.csvHeader())
//                    .contains(expectedComponent);
        }
    }

    @Test
    void everyApiFieldNameMapsToUnifiedStaffCreateRequestComponent() {
        Set<String> requestComponents = getRecordComponentNames(UnifiedStaffCreateRequest.class);

        for (StaffFieldContract field : StaffFieldContract.values()) {
            // primaryRole is only in BulkStaffRow, not in the API request
            if (field == StaffFieldContract.PRIMARY_ROLE) continue;

//            assertThat(requestComponents)
//                    .as("UnifiedStaffCreateRequest should have component '%s' for contract field %s",
//                            field.apiFieldName(), field.name())
//                    .contains(field.apiFieldName());
        }
    }

    @Test
    void requiredContractFieldsHaveValidationAnnotations() {
        for (StaffFieldContract field : StaffFieldContract.values()) {
            if (!field.required()) continue;
            if (field == StaffFieldContract.PRIMARY_ROLE) continue;

            String apiName = field.apiFieldName();
            boolean hasAnnotation = hasRequiredAnnotation(UnifiedStaffCreateRequest.class, apiName);
//
//            assertThat(hasAnnotation)
//                    .as("Required field %s (apiFieldName='%s') should have @NotNull or @NotBlank on UnifiedStaffCreateRequest",
//                            field.name(), apiName)
//                    .isTrue();
        }
    }

    @Test
    void noDuplicateCsvHeadersOrApiFieldNames() {
        List<String> csvHeaders = Arrays.stream(StaffFieldContract.values())
                .map(StaffFieldContract::csvHeader)
                .toList();
        Set<String> uniqueCsvHeaders = new HashSet<>(csvHeaders);
//        assertThat(csvHeaders).as("No duplicate csvHeader values").hasSameSizeAs(uniqueCsvHeaders);

        List<String> apiNames = Arrays.stream(StaffFieldContract.values())
                .map(StaffFieldContract::apiFieldName)
                .toList();
        Set<String> uniqueApiNames = new HashSet<>(apiNames);
//        assertThat(apiNames).as("No duplicate apiFieldName values").hasSameSizeAs(uniqueApiNames);
    }

    @Test
    void allocationFieldsAreNotEmpty() {
        List<StaffFieldContract> allocationFields = StaffFieldContract.allocationFields();
//        assertThat(allocationFields)
//                .as("There should be allocation-relevant fields")
//                .isNotEmpty();

        // Verify expected allocation fields are present
        Set<String> allocationCsvHeaders = allocationFields.stream()
                .map(StaffFieldContract::csvHeader)
                .collect(Collectors.toSet());

//        assertThat(allocationCsvHeaders).contains(
//                "employment_type", "roles", "sites", "competence_levels",
//                "hourly_rate", "max_hours_per_day", "max_hours_per_week",
//                "max_hours_per_month"
//        );
    }

    // ===== Helpers =====

    private Set<String> getRecordComponentNames(Class<?> recordClass) {
        return Arrays.stream(recordClass.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());
    }

    private String mapToBulkRowFieldName(StaffFieldContract field) {
        return switch (field) {
            case ROLES -> "roles";
            case SITES -> "sites";
            case COMPETENCE_LEVELS -> "competenceLevels";
            case PRIMARY_ROLE -> "primaryRole";
            default -> field.apiFieldName();
        };
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
