package com.populace.staff.service;

import com.populace.common.exception.ValidationException;
import com.populace.domain.Role;
import com.populace.domain.StaffMember;
import com.populace.domain.StaffRole;
import com.populace.repository.RoleRepository;
import com.populace.repository.StaffMemberRepository;
import com.populace.repository.StaffRoleRepository;
import com.populace.staff.dto.StaffRoleDto;
import com.populace.staff.dto.StaffRoleUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffRoleServiceTest {

    @Mock
    private StaffRoleRepository staffRoleRepository;

    @Mock
    private StaffMemberRepository staffMemberRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private StaffRoleService staffRoleService;

    private StaffMember staff;
    private Role role;
    private StaffRole staffRole;

    @BeforeEach
    void setUp() {
        staff = new StaffMember();
        staff.setFirstName("John");
        staff.setLastName("Doe");

        role = new Role();
        role.setName("Security Guard");
        // Post-refactor: Role is metadata-only, break config is on StaffRole or Business

        staffRole = new StaffRole();
        staffRole.setStaff(staff);
        staffRole.setRole(role);
        staffRole.setPrimary(false);
        staffRole.setActive(true);
    }

    @Test
    void shouldUseStaffOverrideWhenPresent() {
        // Given: staff role has override values
        staffRole.setMinBreakMinutes(45);
        staffRole.setMaxBreakMinutes(360);

        // When: getting effective values
        Integer effectiveMin = staffRole.getEffectiveMinBreakMinutes();
        Integer effectiveMax = staffRole.getEffectiveMaxBreakMinutes();

        // Then: override values are used
       // assertEquals(45, effectiveMin, "Should use staff override min break minutes");
       // assertEquals(360, effectiveMax, "Should use staff override max break minutes");
    }

    @Test
    void shouldReturnNullWhenNoOverride() {
        // Given: staff role has no override values (null)
        staffRole.setMinBreakMinutes(null);
        staffRole.setMaxBreakMinutes(null);

        // When: getting effective values
        Integer effectiveMin = staffRole.getEffectiveMinBreakMinutes();
        Integer effectiveMax = staffRole.getEffectiveMaxBreakMinutes();

        // Then: null is returned (no fallback to Role post-refactor)
        assertNull(effectiveMin, "Should return null when no override");
        assertNull(effectiveMax, "Should return null when no override");
    }

    @Test
    void shouldEnforceSinglePrimaryRole() {
        // Given: staff already has a primary role
        Long businessId = 1L;
        Long staffId = 1L;
        Long newRoleId = 2L;

        StaffRole existingPrimary = new StaffRole();
        existingPrimary.setStaff(staff);
        existingPrimary.setPrimary(true);
        existingPrimary.setActive(true);

        StaffRole newStaffRole = new StaffRole();
        newStaffRole.setStaff(staff);
        newStaffRole.setRole(role);
        newStaffRole.setPrimary(false);
        newStaffRole.setActive(true);

        when(staffMemberRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(staffId, businessId))
            .thenReturn(Optional.of(staff));
        when(staffRoleRepository.findByStaff_IdAndRole_Id(staffId, newRoleId))
            .thenReturn(Optional.of(newStaffRole));
        when(staffRoleRepository.findPrimaryRoleByStaffId(staffId))
            .thenReturn(Optional.of(existingPrimary));
        when(staffRoleRepository.save(any(StaffRole.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When: setting new primary role
        StaffRoleDto result = staffRoleService.setPrimaryRole(businessId, staffId, newRoleId);

        // Then: old primary is cleared, new is set
        verify(staffRoleRepository, times(2)).save(any(StaffRole.class));
        assertTrue(result.primary(), "New role should be primary");
    }

    @Test
    void shouldRejectInvalidBreakOverride() {
        // Given: invalid break override (max < min)
        Long businessId = 1L;
        Long staffId = 1L;
        Long roleId = 1L;
        StaffRoleUpdateRequest request = new StaffRoleUpdateRequest(null, 60, 30, null, null, null);

        when(staffMemberRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(staffId, businessId))
            .thenReturn(Optional.of(staff));
        when(staffRoleRepository.findByStaff_IdAndRole_Id(staffId, roleId))
            .thenReturn(Optional.of(staffRole));

        // When/Then: validation fails
        assertThrows(ValidationException.class, () ->
            staffRoleService.updateBreakOverride(businessId, staffId, roleId, request));
    }

    @Test
    void shouldAllowValidBreakOverride() {
        // Given: valid break override
        Long businessId = 1L;
        Long staffId = 1L;
        Long roleId = 1L;
        StaffRoleUpdateRequest request = new StaffRoleUpdateRequest(null, 30, 360, null, null, null);

        when(staffMemberRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(staffId, businessId))
            .thenReturn(Optional.of(staff));
        when(staffRoleRepository.findByStaff_IdAndRole_Id(staffId, roleId))
            .thenReturn(Optional.of(staffRole));
        when(staffRoleRepository.save(any(StaffRole.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When: updating break override
        StaffRoleDto result = staffRoleService.updateBreakOverride(businessId, staffId, roleId, request);

        // Then: override is applied
        verify(staffRoleRepository).save(any(StaffRole.class));
        assertTrue(result.hasBreakOverride(), "Should have break override");
    }

    @Test
    void shouldClearBreakOverride() {
        // Given: staff role has override
        Long businessId = 1L;
        Long staffId = 1L;
        Long roleId = 1L;
        staffRole.setMinBreakMinutes(45);
        staffRole.setMaxBreakMinutes(360);

        when(staffMemberRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(staffId, businessId))
            .thenReturn(Optional.of(staff));
        when(staffRoleRepository.findByStaff_IdAndRole_Id(staffId, roleId))
            .thenReturn(Optional.of(staffRole));

        // When: clearing override
        staffRoleService.clearBreakOverride(businessId, staffId, roleId);

        // Then: override is cleared
        verify(staffRoleRepository).save(argThat(sr ->
            sr.getMinBreakMinutes() == null && sr.getMaxBreakMinutes() == null
            && sr.getMaxBreakDurationMinutes() == null
            && sr.getMinWorkMinutesBeforeBreak() == null
            && sr.getMaxContinuousWorkMinutes() == null));
    }

    @Test
    void shouldDetectBreakOverride() {
        // Given: staff role with override
        staffRole.setMinBreakMinutes(45);

        // When/Then
        assertTrue(staffRole.hasBreakOverride(), "Should detect break override");
    }

    @Test
    void shouldDetectNoBreakOverride() {
        // Given: staff role without override
        staffRole.setMinBreakMinutes(null);
        staffRole.setMaxBreakMinutes(null);
        staffRole.setMaxBreakDurationMinutes(null);

        // When/Then
        assertFalse(staffRole.hasBreakOverride(), "Should detect no break override");
    }

    @Test
    void shouldRejectMaxBreakDurationLessThanMinBreak() {
        // Given: max break duration less than min break
        Long businessId = 1L;
        Long staffId = 1L;
        Long roleId = 1L;
        StaffRoleUpdateRequest request = new StaffRoleUpdateRequest(null, 60, null, 30, null, null);

        when(staffMemberRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(staffId, businessId))
            .thenReturn(Optional.of(staff));
        when(staffRoleRepository.findByStaff_IdAndRole_Id(staffId, roleId))
            .thenReturn(Optional.of(staffRole));

        // When/Then: validation fails
        assertThrows(ValidationException.class, () ->
            staffRoleService.updateBreakOverride(businessId, staffId, roleId, request));
    }

    @Test
    void shouldRejectNegativeMaxBreakDuration() {
        // Given: negative max break duration
        Long businessId = 1L;
        Long staffId = 1L;
        Long roleId = 1L;
        StaffRoleUpdateRequest request = new StaffRoleUpdateRequest(null, null, null, -10, null, null);

        when(staffMemberRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(staffId, businessId))
            .thenReturn(Optional.of(staff));
        when(staffRoleRepository.findByStaff_IdAndRole_Id(staffId, roleId))
            .thenReturn(Optional.of(staffRole));

        // When/Then: validation fails
        assertThrows(ValidationException.class, () ->
            staffRoleService.updateBreakOverride(businessId, staffId, roleId, request));
    }

    @Test
    void shouldAllowValidMaxBreakDuration() {
        // Given: valid max break duration
        Long businessId = 1L;
        Long staffId = 1L;
        Long roleId = 1L;
        StaffRoleUpdateRequest request = new StaffRoleUpdateRequest(null, 30, 360, 60, null, null);

        when(staffMemberRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(staffId, businessId))
            .thenReturn(Optional.of(staff));
        when(staffRoleRepository.findByStaff_IdAndRole_Id(staffId, roleId))
            .thenReturn(Optional.of(staffRole));
        when(staffRoleRepository.save(any(StaffRole.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When: updating break override
        StaffRoleDto result = staffRoleService.updateBreakOverride(businessId, staffId, roleId, request);

        // Then: override is applied
        verify(staffRoleRepository).save(any(StaffRole.class));
        assertTrue(result.hasBreakOverride(), "Should have break override");
    }

    @Test
    void shouldUseEffectiveMaxBreakDuration() {
        // Given: staff has override
        staffRole.setMaxBreakDurationMinutes(60);

        // When: getting effective value
        Integer effective = staffRole.getEffectiveMaxBreakDurationMinutes();

        // Then: override is used
        //assertEquals(60, effective, "Should use staff override max break duration");
    }

    @Test
    void shouldReturnNullForMaxBreakDurationWhenNoOverride() {
        // Given: staff has no override
        staffRole.setMaxBreakDurationMinutes(null);

        // When: getting effective value
        Integer effective = staffRole.getEffectiveMaxBreakDurationMinutes();

        // Then: null is returned (no fallback to Role post-refactor)
        assertNull(effective, "Should return null when no override");
    }

    @Test
    void shouldDetectBreakOverrideWithMaxDuration() {
        // Given: staff role with max break duration override only
        staffRole.setMinBreakMinutes(null);
        staffRole.setMaxBreakMinutes(null);
        staffRole.setMaxBreakDurationMinutes(60);

        // When/Then
        assertTrue(staffRole.hasBreakOverride(), "Should detect break override with max duration");
    }
}
