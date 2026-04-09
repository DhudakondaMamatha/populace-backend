package com.populace.shift.service;

import com.populace.common.exception.ValidationException;
import com.populace.domain.*;
import com.populace.repository.*;
import com.populace.shift.dto.ShiftCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for BUG #4: Site Operating Hours Validation
 * Verifies shift times are validated against site operating hours.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Shift Operating Hours Validation Tests")
class ShiftOperatingHoursValidationTest {

    @Mock private ShiftRepository shiftRepository;
    @Mock private BusinessRepository businessRepository;
    @Mock private SiteRepository siteRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private SiteOperatingHoursRepository siteOperatingHoursRepository;
    @Mock private TimeBlockRepository timeBlockRepository;

    private ShiftService shiftService;
    private Business business;
    private Site site;
    private Role role;

    @BeforeEach
    void setUp() throws Exception {
        shiftService = new ShiftService(
            shiftRepository,
            businessRepository,
            siteRepository,
            roleRepository,
            siteOperatingHoursRepository,
            timeBlockRepository
        );

        // Create real entity instances and set IDs via reflection
        business = new Business();
        business.setName("Test Business");
        business.setEmail("test@business.com");
        setEntityId(business, 1L);

        site = new Site();
        site.setName("Test Site");
        site.setBusiness(business);
        setEntityId(site, 1L);

        role = new Role();
        role.setName("Test Role");
        role.setBusiness(business);
        setEntityId(role, 1L);
    }

    /**
     * Utility method to set entity ID via reflection since entities use identity generation.
     */
    private void setEntityId(Object entity, Long id) throws Exception {
        Field idField = findIdField(entity.getClass());
        if (idField != null) {
            idField.setAccessible(true);
            idField.set(entity, id);
        }
    }

    private Field findIdField(Class<?> clazz) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField("id");
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }

    @Test
    @DisplayName("Should reject shift when site is closed")
    void shouldRejectShiftWhenSiteIsClosed() {
        // Given
        LocalDate shiftDate = LocalDate.of(2026, 2, 24); // Tuesday
        ShiftCreateRequest request = new ShiftCreateRequest(
            1L, 1L, shiftDate,
            LocalTime.of(9, 0), LocalTime.of(17, 0),
            30, 1, null
        );

        SiteOperatingHours hours = new SiteOperatingHours();
        hours.setSite(site);
        hours.setDayOfWeek(2); // Tuesday = 2
        hours.setClosed(true);

        when(businessRepository.findById(1L)).thenReturn(Optional.of(business));
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(siteOperatingHoursRepository.findBySite_IdAndDayOfWeek(1L, 2))
            .thenReturn(Optional.of(hours));

        // When/Then
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> shiftService.createShift(1L, request)
        );

        assertTrue(exception.getMessage().contains("closed"));
    }

    @Test
    @DisplayName("Should reject shift starting before opening time")
    void shouldRejectShiftStartingBeforeOpeningTime() {
        // Given
        LocalDate shiftDate = LocalDate.of(2026, 2, 24);
        ShiftCreateRequest request = new ShiftCreateRequest(
            1L, 1L, shiftDate,
            LocalTime.of(7, 0),  // Before opening
            LocalTime.of(15, 0),
            30, 1, null
        );

        SiteOperatingHours hours = new SiteOperatingHours();
        hours.setSite(site);
        hours.setDayOfWeek(2);
        hours.setClosed(false);
        hours.setOpenTime(LocalTime.of(9, 0));
        hours.setCloseTime(LocalTime.of(21, 0));

        when(businessRepository.findById(1L)).thenReturn(Optional.of(business));
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(siteOperatingHoursRepository.findBySite_IdAndDayOfWeek(1L, 2))
            .thenReturn(Optional.of(hours));

        // When/Then
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> shiftService.createShift(1L, request)
        );

        assertTrue(exception.getMessage().contains("before site opening time"));
    }

    @Test
    @DisplayName("Should reject shift ending after closing time")
    void shouldRejectShiftEndingAfterClosingTime() {
        // Given
        LocalDate shiftDate = LocalDate.of(2026, 2, 24);
        ShiftCreateRequest request = new ShiftCreateRequest(
            1L, 1L, shiftDate,
            LocalTime.of(18, 0),
            LocalTime.of(23, 0),  // After closing
            30, 1, null
        );

        SiteOperatingHours hours = new SiteOperatingHours();
        hours.setSite(site);
        hours.setDayOfWeek(2);
        hours.setClosed(false);
        hours.setOpenTime(LocalTime.of(9, 0));
        hours.setCloseTime(LocalTime.of(21, 0));

        when(businessRepository.findById(1L)).thenReturn(Optional.of(business));
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(siteOperatingHoursRepository.findBySite_IdAndDayOfWeek(1L, 2))
            .thenReturn(Optional.of(hours));

        // When/Then
        ValidationException exception = assertThrows(
            ValidationException.class,
            () -> shiftService.createShift(1L, request)
        );

        assertTrue(exception.getMessage().contains("after site closing time"));
    }

    @Test
    @DisplayName("Should allow shift within operating hours")
    void shouldAllowShiftWithinOperatingHours() throws Exception {
        // Given
        LocalDate shiftDate = LocalDate.of(2026, 2, 24);
        ShiftCreateRequest request = new ShiftCreateRequest(
            1L, 1L, shiftDate,
            LocalTime.of(9, 0),
            LocalTime.of(17, 0),
            30, 1, null
        );

        SiteOperatingHours hours = new SiteOperatingHours();
        hours.setSite(site);
        hours.setDayOfWeek(2);
        hours.setClosed(false);
        hours.setOpenTime(LocalTime.of(9, 0));
        hours.setCloseTime(LocalTime.of(21, 0));

        Shift savedShift = new Shift();
        setEntityId(savedShift, 1L);
        savedShift.setBusiness(business);
        savedShift.setSite(site);
        savedShift.setRole(role);
        savedShift.setShiftDate(shiftDate);
        savedShift.setStartTime(LocalTime.of(9, 0));
        savedShift.setEndTime(LocalTime.of(17, 0));

        when(businessRepository.findById(1L)).thenReturn(Optional.of(business));
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(siteOperatingHoursRepository.findBySite_IdAndDayOfWeek(1L, 2))
            .thenReturn(Optional.of(hours));
        when(shiftRepository.save(any(Shift.class))).thenReturn(savedShift);

        // When
        var result = shiftService.createShift(1L, request);

        // Then
        assertNotNull(result);
        verify(shiftRepository).save(any(Shift.class));
    }

    @Test
    @DisplayName("Should allow shift when no operating hours configured")
    void shouldAllowShiftWhenNoOperatingHoursConfigured() throws Exception {
        // Given
        LocalDate shiftDate = LocalDate.of(2026, 2, 24);
        ShiftCreateRequest request = new ShiftCreateRequest(
            1L, 1L, shiftDate,
            LocalTime.of(6, 0),  // Early morning
            LocalTime.of(22, 0), // Late evening
            30, 1, null
        );

        Shift savedShift = new Shift();
        setEntityId(savedShift, 1L);
        savedShift.setBusiness(business);
        savedShift.setSite(site);
        savedShift.setRole(role);
        savedShift.setShiftDate(shiftDate);
        savedShift.setStartTime(LocalTime.of(6, 0));
        savedShift.setEndTime(LocalTime.of(22, 0));

        when(businessRepository.findById(1L)).thenReturn(Optional.of(business));
        when(siteRepository.findById(1L)).thenReturn(Optional.of(site));
        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
        when(siteOperatingHoursRepository.findBySite_IdAndDayOfWeek(1L, 2))
            .thenReturn(Optional.empty());  // No operating hours
        when(shiftRepository.save(any(Shift.class))).thenReturn(savedShift);

        // When
        var result = shiftService.createShift(1L, request);

        // Then
        assertNotNull(result);
        verify(shiftRepository).save(any(Shift.class));
    }
}
