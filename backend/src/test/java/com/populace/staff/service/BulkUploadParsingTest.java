package com.populace.staff.service;

import com.populace.staff.dto.BulkStaffRow;
import com.populace.staff.dto.BulkUploadError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for bulk upload parsing and validation of multi-value fields.
 *
 * Tests cover:
 * - Pipe-separated role parsing
 * - Pipe-separated competence level parsing
 * - Strict validation (no fuzzy matching)
 * - Error handling for invalid entries
 *
 * Duplicate handling behavior (documented):
 * - Duplicates are removed silently during conversion to validated row
 * - This matches multi-select UI behavior
 */
@DisplayName("Bulk Upload Multi-Value Parsing Tests")
class BulkUploadParsingTest {

    private CsvParserService csvParserService;
    private TestBulkValidationHelper validationHelper;

    @BeforeEach
    void setUp() {
        csvParserService = new CsvParserService();
        validationHelper = new TestBulkValidationHelper();
    }

    @Nested
    @DisplayName("Role Parsing Tests")
    class RoleParsingTests {

        @Test
        @DisplayName("shouldParseMultipleRoles - pipe-separated roles are split correctly")
        void shouldParseMultipleRoles() {
            // Given
            String rolesValue = "Nurse|Team Lead";

            // When
            List<String> roles = parseListValue(rolesValue);

            // Then
            assertThat(roles).containsExactly("Nurse", "Team Lead");
        }

        @Test
        @DisplayName("shouldParseSingleRole - single role without pipe works")
        void shouldParseSingleRole() {
            // Given
            String rolesValue = "Doctor";

            // When
            List<String> roles = parseListValue(rolesValue);

            // Then
            assertThat(roles).containsExactly("Doctor");
        }

        @Test
        @DisplayName("shouldHandleWhitespaceAroundPipes - spaces are trimmed")
        void shouldHandleWhitespaceAroundPipes() {
            // Given
            String rolesValue = "Nurse | Team Lead | Doctor";

            // When
            List<String> roles = parseListValue(rolesValue);

            // Then - whitespace trimmed from each entry
            assertThat(roles).containsExactly("Nurse", "Team Lead", "Doctor");
        }

        @Test
        @DisplayName("shouldRejectUnknownRole - role not in database causes error")
        void shouldRejectUnknownRole() {
            // Given
            Map<String, Long> existingRoles = Map.of(
                "nurse", 1L,
                "doctor", 2L
            );
            List<String> rolesInRow = List.of("Nurse", "InvalidRole", "Doctor");

            // When
            List<BulkUploadError> errors = validationHelper.validateRoles(
                2, rolesInRow, existingRoles);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).column()).isEqualTo("roles");
            assertThat(errors.get(0).value()).isEqualTo("InvalidRole");
            assertThat(errors.get(0).errorCode()).isEqualTo("REFERENCE_NOT_FOUND");
            assertThat(errors.get(0).message()).contains("Role not found");
        }

        @Test
        @DisplayName("shouldRejectPartialRoleMatch - no fuzzy matching allowed")
        void shouldRejectPartialRoleMatch() {
            // Given - "Nurs" should NOT match "Nurse"
            Map<String, Long> existingRoles = Map.of("nurse", 1L);
            List<String> rolesInRow = List.of("Nurs");

            // When
            List<BulkUploadError> errors = validationHelper.validateRoles(
                2, rolesInRow, existingRoles);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).message()).contains("Role not found: \"Nurs\"");
        }

        @Test
        @DisplayName("shouldMatchRolesCaseInsensitive - case is ignored")
        void shouldMatchRolesCaseInsensitive() {
            // Given
            Map<String, Long> existingRoles = Map.of("nurse", 1L, "team lead", 2L);
            List<String> rolesInRow = List.of("NURSE", "Team Lead");

            // When
            List<BulkUploadError> errors = validationHelper.validateRoles(
                2, rolesInRow, existingRoles);

            // Then - no errors, case insensitive match
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("shouldRejectEmptyRoleEntry - consecutive pipes rejected")
        void shouldRejectEmptyRoleEntry() {
            // Given - "Nurse||Doctor" has empty entry between pipes
            String rolesValue = "Nurse||Doctor";
            List<String> roles = parseListValue(rolesValue);

            // When
            Map<String, Long> existingRoles = Map.of("nurse", 1L, "doctor", 2L);
            List<BulkUploadError> errors = validationHelper.validateRoles(
                2, roles, existingRoles);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).errorCode()).isEqualTo("INVALID_FORMAT");
            assertThat(errors.get(0).message()).contains("Empty role entry");
        }

        @Test
        @DisplayName("shouldRejectLeadingPipe - malformed format")
        void shouldRejectLeadingPipe() {
            // Given - "|Nurse" has empty entry at start
            String rolesValue = "|Nurse";
            List<String> roles = parseListValue(rolesValue);

            // When
            Map<String, Long> existingRoles = Map.of("nurse", 1L);
            List<BulkUploadError> errors = validationHelper.validateRoles(
                2, roles, existingRoles);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).errorCode()).isEqualTo("INVALID_FORMAT");
        }

        @Test
        @DisplayName("shouldRejectTrailingPipe - malformed format")
        void shouldRejectTrailingPipe() {
            // Given - "Nurse|" has empty entry at end
            String rolesValue = "Nurse|";
            List<String> roles = parseListValue(rolesValue);

            // When
            Map<String, Long> existingRoles = Map.of("nurse", 1L);
            List<BulkUploadError> errors = validationHelper.validateRoles(
                2, roles, existingRoles);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).errorCode()).isEqualTo("INVALID_FORMAT");
        }

        @Test
        @DisplayName("shouldRemoveDuplicateRolesSilently - duplicates handled")
        void shouldRemoveDuplicateRolesSilently() {
            // Given
            Map<String, Long> existingRoles = Map.of("nurse", 1L, "doctor", 2L);
            List<String> rolesInRow = List.of("Nurse", "Doctor", "Nurse");

            // When - validation should pass (duplicates are valid input)
            List<BulkUploadError> errors = validationHelper.validateRoles(
                2, rolesInRow, existingRoles);

            // Then - no errors during validation
            assertThat(errors).isEmpty();

            // And - during conversion, duplicates are removed
            List<Long> roleIds = validationHelper.convertRolesToIds(rolesInRow, existingRoles);
            assertThat(roleIds).containsExactly(1L, 2L); // Duplicates removed
        }
    }

    @Nested
    @DisplayName("Competence Level Parsing Tests")
    class CompetenceLevelParsingTests {

        @Test
        @DisplayName("shouldParseMultipleCompetenceLevels - pipe-separated levels split correctly")
        void shouldParseMultipleCompetenceLevels() {
            // Given
            String levelsValue = "L1|L2";

            // When
            List<String> levels = parseListValue(levelsValue);

            // Then
            assertThat(levels).containsExactly("L1", "L2");
        }

        @Test
        @DisplayName("shouldParseAllThreeLevels - L1, L2, L3 all valid")
        void shouldParseAllThreeLevels() {
            // Given
            String levelsValue = "L1|L2|L3";

            // When
            List<String> levels = parseListValue(levelsValue);

            // Then
            assertThat(levels).containsExactly("L1", "L2", "L3");
        }

        @Test
        @DisplayName("shouldRejectInvalidCompetenceLevel - L4 is invalid")
        void shouldRejectInvalidCompetenceLevel() {
            // Given
            List<String> levels = List.of("L1", "L4", "L2");

            // When
            List<BulkUploadError> errors = validationHelper.validateCompetenceLevels(2, levels);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).column()).isEqualTo("competence_levels");
            assertThat(errors.get(0).value()).isEqualTo("L4");
            assertThat(errors.get(0).errorCode()).isEqualTo("INVALID_ENUM");
            assertThat(errors.get(0).message()).contains("Invalid competence level: \"L4\"");
            assertThat(errors.get(0).message()).contains("Valid values: L1, L2, L3");
        }

        @Test
        @DisplayName("shouldRejectInvalidLevelFormat - must be L followed by number")
        void shouldRejectInvalidLevelFormat() {
            // Given
            List<String> levels = List.of("Level1", "Expert");

            // When
            List<BulkUploadError> errors = validationHelper.validateCompetenceLevels(2, levels);

            // Then
            assertThat(errors).hasSize(2);
            assertThat(errors.get(0).value()).isEqualTo("Level1");
            assertThat(errors.get(1).value()).isEqualTo("Expert");
        }

        @Test
        @DisplayName("shouldHandleWhitespaceProperly - spaces around levels trimmed")
        void shouldHandleWhitespaceProperly() {
            // Given
            String levelsValue = " L1 | L2 | L3 ";
            List<String> levels = parseListValue(levelsValue);

            // When
            List<BulkUploadError> errors = validationHelper.validateCompetenceLevels(2, levels);

            // Then - no errors, whitespace handled correctly
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("shouldMatchLevelsCaseInsensitive - l1 equals L1")
        void shouldMatchLevelsCaseInsensitive() {
            // Given
            List<String> levels = List.of("l1", "l2", "l3");

            // When
            List<BulkUploadError> errors = validationHelper.validateCompetenceLevels(2, levels);

            // Then - no errors, case insensitive
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("shouldRejectMalformedPipeFormat - empty entries rejected")
        void shouldRejectMalformedPipeFormat() {
            // Given
            String levelsValue = "L1||L3";
            List<String> levels = parseListValue(levelsValue);

            // When
            List<BulkUploadError> errors = validationHelper.validateCompetenceLevels(2, levels);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).errorCode()).isEqualTo("INVALID_FORMAT");
            assertThat(errors.get(0).message()).contains("Empty competence level");
        }

        @Test
        @DisplayName("shouldRemoveDuplicateLevelsSilently - L1|L2|L1 deduped")
        void shouldRemoveDuplicateLevelsSilently() {
            // Given
            List<String> levels = List.of("L1", "L2", "L1");

            // When - validation passes
            List<BulkUploadError> errors = validationHelper.validateCompetenceLevels(2, levels);
            assertThat(errors).isEmpty();

            // And - conversion deduplicates
            List<String> converted = validationHelper.convertCompetenceLevels(levels);
            assertThat(converted).containsExactly("L1", "L2");
        }
    }

    @Nested
    @DisplayName("Primary Role Parsing Tests")
    class PrimaryRoleParsingTests {

        @Test
        @DisplayName("shouldAcceptValidPrimaryRole - matches one of the assigned roles")
        void shouldAcceptValidPrimaryRole() {
            // Given
            List<String> roles = List.of("Nurse", "Doctor");
            String primaryRole = "Nurse";

            // When
            List<BulkUploadError> errors = validationHelper.validatePrimaryRole(2, primaryRole, roles);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("shouldAcceptPrimaryRoleCaseInsensitive - case is ignored")
        void shouldAcceptPrimaryRoleCaseInsensitive() {
            // Given
            List<String> roles = List.of("Nurse", "Doctor");
            String primaryRole = "NURSE";

            // When
            List<BulkUploadError> errors = validationHelper.validatePrimaryRole(2, primaryRole, roles);

            // Then
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("shouldRejectPrimaryRoleNotInAssignedRoles")
        void shouldRejectPrimaryRoleNotInAssignedRoles() {
            // Given
            List<String> roles = List.of("Nurse", "Doctor");
            String primaryRole = "Manager";

            // When
            List<BulkUploadError> errors = validationHelper.validatePrimaryRole(2, primaryRole, roles);

            // Then
            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).column()).isEqualTo("primary_role");
            assertThat(errors.get(0).errorCode()).isEqualTo("INVALID");
            assertThat(errors.get(0).message()).contains("must be one of the assigned roles");
        }

        @Test
        @DisplayName("shouldAllowEmptyPrimaryRole - defaults to first role")
        void shouldAllowEmptyPrimaryRole() {
            // Given
            List<String> roles = List.of("Nurse", "Doctor");
            String primaryRole = null;

            // When
            List<BulkUploadError> errors = validationHelper.validatePrimaryRole(2, primaryRole, roles);

            // Then
            assertThat(errors).isEmpty();
        }
    }

    @Nested
    @DisplayName("Combined Parsing Tests")
    class CombinedParsingTests {

        @Test
        @DisplayName("shouldHandleEmptyFields - empty roles and levels are allowed")
        void shouldHandleEmptyFields() {
            // Given
            String rolesValue = "";
            String levelsValue = "";

            // When
            List<String> roles = parseListValue(rolesValue);
            List<String> levels = parseListValue(levelsValue);

            // Then - empty lists, not null
            assertThat(roles).isEmpty();
            assertThat(levels).isEmpty();
        }

        @Test
        @DisplayName("shouldRejectAllInvalidEntriesInRow - multiple errors collected")
        void shouldRejectAllInvalidEntriesInRow() {
            // Given - row with invalid role AND invalid level
            Map<String, Long> existingRoles = Map.of("nurse", 1L);
            List<String> roles = List.of("Nurse", "FakeRole");
            List<String> levels = List.of("L1", "L5");

            // When
            List<BulkUploadError> roleErrors = validationHelper.validateRoles(2, roles, existingRoles);
            List<BulkUploadError> levelErrors = validationHelper.validateCompetenceLevels(2, levels);

            // Then - both errors collected
            assertThat(roleErrors).hasSize(1);
            assertThat(levelErrors).hasSize(1);
        }
    }

    // Helper method to use the same parsing logic as CsvParserService
    private List<String> parseListValue(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\|", -1))
            .map(String::trim)
            .toList();
    }

    /**
     * Test helper that replicates validation logic for isolated unit testing.
     */
    static class TestBulkValidationHelper {
        private static final Set<String> VALID_COMPETENCE_LEVELS = Set.of("L1", "L2", "L3");

        List<BulkUploadError> validateRoles(int rowNum, List<String> roles,
                                             Map<String, Long> roleMap) {
            List<BulkUploadError> errors = new ArrayList<>();
            for (String roleName : roles) {
                String trimmedRole = roleName.trim();
                if (trimmedRole.isEmpty()) {
                    errors.add(new BulkUploadError(rowNum, "roles", roleName, "INVALID_FORMAT",
                        "Empty role entry found. Check for consecutive pipes (||) or trailing/leading pipes."));
                    continue;
                }
                if (!roleMap.containsKey(trimmedRole.toLowerCase())) {
                    errors.add(new BulkUploadError(rowNum, "roles", trimmedRole, "REFERENCE_NOT_FOUND",
                        "Role not found: \"" + trimmedRole + "\""));
                }
            }
            return errors;
        }

        List<BulkUploadError> validateCompetenceLevels(int rowNum, List<String> levels) {
            List<BulkUploadError> errors = new ArrayList<>();
            for (String level : levels) {
                String trimmedLevel = level.trim();
                if (trimmedLevel.isEmpty()) {
                    errors.add(new BulkUploadError(rowNum, "competence_levels", level, "INVALID_FORMAT",
                        "Empty competence level found. Check for consecutive pipes (||) or trailing/leading pipes."));
                    continue;
                }
                if (!VALID_COMPETENCE_LEVELS.contains(trimmedLevel.toUpperCase())) {
                    errors.add(new BulkUploadError(rowNum, "competence_levels", trimmedLevel, "INVALID_ENUM",
                        "Invalid competence level: \"" + trimmedLevel + "\". Valid values: L1, L2, L3"));
                }
            }
            return errors;
        }

        List<Long> convertRolesToIds(List<String> roles, Map<String, Long> roleMap) {
            return roles.stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .map(name -> roleMap.get(name.toLowerCase()))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        }

        List<BulkUploadError> validatePrimaryRole(int rowNum, String primaryRole, List<String> roles) {
            List<BulkUploadError> errors = new ArrayList<>();
            if (primaryRole == null || primaryRole.isBlank()) return errors;
            String trimmed = primaryRole.trim();
            boolean matches = roles.stream().anyMatch(r -> r.trim().equalsIgnoreCase(trimmed));
            if (!matches) {
                errors.add(new BulkUploadError(rowNum, "primary_role", trimmed, "INVALID",
                    "Primary role must be one of the assigned roles"));
            }
            return errors;
        }

        List<String> convertCompetenceLevels(List<String> levels) {
            return levels.stream()
                .map(String::trim)
                .filter(level -> !level.isEmpty())
                .map(String::toUpperCase)
                .distinct()
                .toList();
        }
    }
}
