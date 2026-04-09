package com.populace.compensation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.populace.compensation.dto.CompensationCreateRequest;
import com.populace.compensation.dto.CompensationDto;
import com.populace.compensation.dto.CompensationUpdateRequest;
import com.populace.compensation.exception.CompensationValidationException;
import com.populace.compensation.service.StaffCompensationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CompensationController.class)
class CompensationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StaffCompensationService compensationService;

    @Nested
    @DisplayName("Create Compensation - POST /api/staff/{staffId}/compensation")
    class CreateCompensation {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should create hourly compensation successfully")
        void shouldCreateHourlyCompensationSuccessfully() throws Exception {
            // Given
            Long staffId = 1L;
            CompensationCreateRequest request = new CompensationCreateRequest(
                null,
                new BigDecimal("25.00"),
                LocalDate.now(),
                null,
                "hourly",
                null
            );

            CompensationDto response = new CompensationDto(
                1L,                          // id
                staffId,                     // staffId
                null,                        // roleId
                null,                        // roleName
                new BigDecimal("25.00"),     // hourlyRate
                LocalDate.now(),             // effectiveFrom
                null,                        // effectiveTo
                "hourly",                    // compensationType
                null,                        // monthlySalary
                true                         // isActive
            );

            when(compensationService.createCompensation(eq(staffId), any())).thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/staff/{staffId}/compensation", staffId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.compensationType").value("hourly"))
                .andExpect(jsonPath("$.hourlyRate").value(25.00))
                .andExpect(jsonPath("$.monthlySalary").doesNotExist());

            verify(compensationService).createCompensation(eq(staffId), any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should create monthly compensation successfully")
        void shouldCreateMonthlyCompensationSuccessfully() throws Exception {
            // Given
            Long staffId = 1L;
            CompensationCreateRequest request = new CompensationCreateRequest(
                null,
                new BigDecimal("28.85"),
                LocalDate.now(),
                null,
                "monthly",
                new BigDecimal("5000.00")
            );

            CompensationDto response = new CompensationDto(
                1L,                          // id
                staffId,                     // staffId
                null,                        // roleId
                null,                        // roleName
                new BigDecimal("28.85"),     // hourlyRate
                LocalDate.now(),             // effectiveFrom
                null,                        // effectiveTo
                "monthly",                   // compensationType
                new BigDecimal("5000.00"),   // monthlySalary
                true                         // isActive
            );

            when(compensationService.createCompensation(eq(staffId), any())).thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/staff/{staffId}/compensation", staffId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.compensationType").value("monthly"))
                .andExpect(jsonPath("$.monthlySalary").value(5000.00))
                .andExpect(jsonPath("$.hourlyRate").value(28.85));

            verify(compensationService).createCompensation(eq(staffId), any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return 400 for invalid hourly compensation (missing hourlyRate)")
        void shouldReturn400ForInvalidHourlyCompensation() throws Exception {
            // Given
            Long staffId = 1L;
            CompensationCreateRequest request = new CompensationCreateRequest(
                null, null,
                LocalDate.now(), null, "hourly", null
            );

            when(compensationService.createCompensation(eq(staffId), any()))
                .thenThrow(new CompensationValidationException("hourlyRate",
                    "Hourly rate is required for hourly compensation"));

            // When/Then
            mockMvc.perform(post("/api/staff/{staffId}/compensation", staffId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Error"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return 400 for invalid monthly compensation (missing monthlySalary)")
        void shouldReturn400ForInvalidMonthlyCompensation() throws Exception {
            // Given
            Long staffId = 1L;
            CompensationCreateRequest request = new CompensationCreateRequest(
                null,
                new BigDecimal("25.00"),
                LocalDate.now(), null, "monthly", null
            );

            when(compensationService.createCompensation(eq(staffId), any()))
                .thenThrow(new CompensationValidationException("monthlySalary",
                    "Monthly salary is required for monthly compensation"));

            // When/Then
            mockMvc.perform(post("/api/staff/{staffId}/compensation", staffId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Error"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should return 400 for overlapping compensation dates")
        void shouldReturn400ForOverlappingDates() throws Exception {
            // Given
            Long staffId = 1L;
            CompensationCreateRequest request = new CompensationCreateRequest(
                null,
                new BigDecimal("25.00"),
                LocalDate.of(2024, 6, 1),
                LocalDate.of(2025, 6, 1),
                "hourly",
                null
            );

            when(compensationService.createCompensation(eq(staffId), any()))
                .thenThrow(new CompensationValidationException("effectiveFrom",
                    "Date range overlaps with existing compensation record"));

            // When/Then
            mockMvc.perform(post("/api/staff/{staffId}/compensation", staffId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Error"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return 403 for non-admin user attempting to create")
        void shouldReturn403ForNonAdminCreate() throws Exception {
            // Given
            Long staffId = 1L;
            CompensationCreateRequest request = new CompensationCreateRequest(
                null,
                new BigDecimal("25.00"),
                LocalDate.now(), null, "hourly", null
            );

            // When/Then
            mockMvc.perform(post("/api/staff/{staffId}/compensation", staffId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

            verify(compensationService, never()).createCompensation(anyLong(), any());
        }

        @Test
        @DisplayName("should return 401 for unauthenticated user attempting to create")
        void shouldReturn401ForUnauthenticatedCreate() throws Exception {
            // Given
            Long staffId = 1L;
            CompensationCreateRequest request = new CompensationCreateRequest(
                null,
                new BigDecimal("25.00"),
                LocalDate.now(), null, "hourly", null
            );

            // When/Then
            mockMvc.perform(post("/api/staff/{staffId}/compensation", staffId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

            verify(compensationService, never()).createCompensation(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("Get Compensation - GET /api/staff/{staffId}/compensation")
    class GetCompensation {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should allow regular user to read compensation history")
        void shouldAllowUserToReadCompensationHistory() throws Exception {
            // Given
            Long staffId = 1L;
            CompensationDto comp1 = new CompensationDto(
                1L,                          // id
                staffId,                     // staffId
                null,                        // roleId
                null,                        // roleName
                new BigDecimal("25.00"),     // hourlyRate
                LocalDate.now().minusMonths(6), // effectiveFrom
                LocalDate.now().minusDays(1),   // effectiveTo
                "hourly",                    // compensationType
                null,                        // monthlySalary
                false                        // isActive
            );
            CompensationDto comp2 = new CompensationDto(
                2L,                          // id
                staffId,                     // staffId
                null,                        // roleId
                null,                        // roleName
                new BigDecimal("28.85"),     // hourlyRate
                LocalDate.now(),             // effectiveFrom
                null,                        // effectiveTo
                "monthly",                   // compensationType
                new BigDecimal("5000.00"),   // monthlySalary
                true                         // isActive
            );

            when(compensationService.getCompensationHistory(staffId))
                .thenReturn(List.of(comp1, comp2));

            // When/Then
            mockMvc.perform(get("/api/staff/{staffId}/compensation", staffId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].compensationType").value("hourly"))
                .andExpect(jsonPath("$[1].compensationType").value("monthly"));

            verify(compensationService).getCompensationHistory(staffId);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should allow admin to read compensation history")
        void shouldAllowAdminToReadCompensationHistory() throws Exception {
            // Given
            Long staffId = 1L;
            when(compensationService.getCompensationHistory(staffId))
                .thenReturn(List.of());

            // When/Then
            mockMvc.perform(get("/api/staff/{staffId}/compensation", staffId))
                .andExpect(status().isOk());

            verify(compensationService).getCompensationHistory(staffId);
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return current compensation for regular user")
        void shouldReturnCurrentCompensationForUser() throws Exception {
            // Given
            Long staffId = 1L;
            CompensationDto current = new CompensationDto(
                1L,                          // id
                staffId,                     // staffId
                null,                        // roleId
                null,                        // roleName
                new BigDecimal("25.00"),     // hourlyRate
                LocalDate.now(),             // effectiveFrom
                null,                        // effectiveTo
                "hourly",                    // compensationType
                null,                        // monthlySalary
                true                         // isActive
            );

            when(compensationService.getCurrentCompensation(staffId))
                .thenReturn(Optional.of(current));

            // When/Then
            mockMvc.perform(get("/api/staff/{staffId}/compensation/current", staffId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.compensationType").value("hourly"));

            verify(compensationService).getCurrentCompensation(staffId);
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return 204 when no current compensation exists")
        void shouldReturn204WhenNoCurrentCompensation() throws Exception {
            // Given
            Long staffId = 1L;
            when(compensationService.getCurrentCompensation(staffId))
                .thenReturn(Optional.empty());

            // When/Then
            mockMvc.perform(get("/api/staff/{staffId}/compensation/current", staffId))
                .andExpect(status().isNoContent());

            verify(compensationService).getCurrentCompensation(staffId);
        }
    }

    @Nested
    @DisplayName("Update Compensation - PUT /api/compensation/{id}")
    class UpdateCompensation {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should update compensation successfully")
        void shouldUpdateCompensationSuccessfully() throws Exception {
            // Given
            Long id = 1L;
            CompensationUpdateRequest request = new CompensationUpdateRequest(
                new BigDecimal("30.00"),
                null, null, null, null
            );

            CompensationDto updated = new CompensationDto(
                id,                          // id
                1L,                          // staffId
                null,                        // roleId
                null,                        // roleName
                new BigDecimal("30.00"),     // hourlyRate
                LocalDate.now(),             // effectiveFrom
                null,                        // effectiveTo
                "hourly",                    // compensationType
                null,                        // monthlySalary
                true                         // isActive
            );

            when(compensationService.updateCompensation(eq(id), any())).thenReturn(updated);

            // When/Then
            mockMvc.perform(put("/api/compensation/{id}", id)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.hourlyRate").value(30.00));

            verify(compensationService).updateCompensation(eq(id), any());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return 403 for non-admin user attempting to update")
        void shouldReturn403ForNonAdminUpdate() throws Exception {
            // Given
            Long id = 1L;
            CompensationUpdateRequest request = new CompensationUpdateRequest(
                new BigDecimal("30.00"),
                null, null, null, null
            );

            // When/Then
            mockMvc.perform(put("/api/compensation/{id}", id)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

            verify(compensationService, never()).updateCompensation(anyLong(), any());
        }
    }

    @Nested
    @DisplayName("End Compensation - PATCH /api/compensation/{id}")
    class EndCompensation {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("should end compensation successfully")
        void shouldEndCompensationSuccessfully() throws Exception {
            // Given
            Long id = 1L;
            LocalDate endDate = LocalDate.now();
            String requestBody = "{\"endDate\": \"" + endDate + "\"}";

            doNothing().when(compensationService).endCompensation(eq(id), any());

            // When/Then
            mockMvc.perform(patch("/api/compensation/{id}", id)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isNoContent());

            verify(compensationService).endCompensation(eq(id), eq(endDate));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("should return 403 for non-admin user attempting to end compensation")
        void shouldReturn403ForNonAdminEnd() throws Exception {
            // Given
            Long id = 1L;
            String requestBody = "{\"endDate\": \"" + LocalDate.now() + "\"}";

            // When/Then
            mockMvc.perform(patch("/api/compensation/{id}", id)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isForbidden());

            verify(compensationService, never()).endCompensation(anyLong(), any());
        }
    }
}
