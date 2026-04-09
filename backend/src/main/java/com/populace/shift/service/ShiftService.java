package com.populace.shift.service;

import com.populace.common.exception.ResourceNotFoundException;
import com.populace.common.exception.ValidationException;
import com.populace.domain.*;
import com.populace.domain.enums.ShiftStatus;
import com.populace.repository.*;
import com.populace.shift.dto.BulkShiftCreateRequest;
import com.populace.shift.dto.BulkShiftCreateResponse;
import com.populace.shift.dto.BulkShiftUpdateRequest;
import com.populace.shift.dto.BulkShiftUpdateResponse;
import com.populace.shift.dto.ShiftCreateRequest;
import com.populace.shift.dto.ShiftDto;
import com.populace.shift.specification.ShiftSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShiftService {

    private static final Logger log = LoggerFactory.getLogger(ShiftService.class);

    private final ShiftRepository shiftRepository;
    private final BusinessRepository businessRepository;
    private final SiteRepository siteRepository;
    private final RoleRepository roleRepository;
    private final SiteOperatingHoursRepository siteOperatingHoursRepository;
    private final TimeBlockRepository timeBlockRepository;

    public ShiftService(ShiftRepository shiftRepository,
                        BusinessRepository businessRepository,
                        SiteRepository siteRepository,
                        RoleRepository roleRepository,
                        SiteOperatingHoursRepository siteOperatingHoursRepository,
                        TimeBlockRepository timeBlockRepository) {
        this.shiftRepository = shiftRepository;
        this.businessRepository = businessRepository;
        this.siteRepository = siteRepository;
        this.roleRepository = roleRepository;
        this.siteOperatingHoursRepository = siteOperatingHoursRepository;
        this.timeBlockRepository = timeBlockRepository;
    }

    @Transactional(readOnly = true)
    public List<ShiftDto> listShifts(Long businessId, LocalDate startDate, LocalDate endDate,
                                      List<Long> siteIds, List<Long> roleIds,
                                      List<ShiftStatus> statuses, Boolean excludeCancelled) {
        LocalDate effectiveStart = startDate != null ? startDate : LocalDate.now();
        LocalDate effectiveEnd = endDate != null ? endDate : effectiveStart.plusDays(7);

        var spec = ShiftSpecification.withFilters(
            businessId, effectiveStart, effectiveEnd,
            siteIds, roleIds, statuses, excludeCancelled
        );

        return shiftRepository.findAll(spec)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ShiftDto> listUnfilledShifts(Long businessId, LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now();
        if (endDate == null) endDate = startDate.plusDays(7);

        return shiftRepository.findUnfilledShifts(businessId, startDate, endDate)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public ShiftDto getShiftById(Long businessId, Long shiftId) {
        Shift shift = shiftRepository.findByIdAndBusiness_Id(shiftId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Shift", shiftId));
        return toDto(shift);
    }

    @Transactional
    public ShiftDto createShift(Long businessId, ShiftCreateRequest request) {
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Business", businessId));
        Site site = siteRepository.findById(request.siteId())
            .orElseThrow(() -> new ResourceNotFoundException("Site", request.siteId()));
        Role role = roleRepository.findById(request.roleId())
            .orElseThrow(() -> new ResourceNotFoundException("Role", request.roleId()));

        validateShiftAgainstOperatingHours(site, request.shiftDate(), request.startTime(), request.endTime());

        Shift shift = new Shift();
        shift.setBusiness(business);
        shift.setSite(site);
        shift.setRole(role);
        shift.setShiftDate(request.shiftDate());
        shift.setStartTime(request.startTime());
        shift.setEndTime(request.endTime());
        shift.setBreakDurationMinutes(request.breakDurationMinutes() != null
            ? request.breakDurationMinutes() : 0);
        shift.setStaffRequired(request.staffRequired() != null
            ? request.staffRequired() : 1);
        shift.setStaffAllocated(0);
        shift.setStatus(ShiftStatus.open);
        shift.setNotes(request.notes());

        shift = shiftRepository.save(shift);
        return toDto(shift);
    }

    @Transactional
    public ShiftDto updateShift(Long businessId, Long shiftId, ShiftCreateRequest request) {
        Shift shift = shiftRepository.findByIdAndBusiness_Id(shiftId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Shift", shiftId));

        if (request.siteId() != null) {
            Site site = siteRepository.findById(request.siteId())
                .orElseThrow(() -> new ResourceNotFoundException("Site", request.siteId()));
            shift.setSite(site);
        }
        if (request.roleId() != null) {
            Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role", request.roleId()));
            shift.setRole(role);
        }
        if (request.shiftDate() != null) shift.setShiftDate(request.shiftDate());
        if (request.startTime() != null) shift.setStartTime(request.startTime());
        if (request.endTime() != null) shift.setEndTime(request.endTime());
        if (request.breakDurationMinutes() != null) {
            shift.setBreakDurationMinutes(request.breakDurationMinutes());
        }
        if (request.staffRequired() != null) shift.setStaffRequired(request.staffRequired());
        if (request.notes() != null) shift.setNotes(request.notes());

        shift = shiftRepository.save(shift);
        return toDto(shift);
    }

    @Transactional
    public BulkShiftCreateResponse createBulkShifts(Long businessId, BulkShiftCreateRequest request) {
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Business", businessId));
        Site site = siteRepository.findById(request.siteId())
            .orElseThrow(() -> new ResourceNotFoundException("Site", request.siteId()));
        Role role = roleRepository.findById(request.roleId())
            .orElseThrow(() -> new ResourceNotFoundException("Role", request.roleId()));

        List<ShiftDto> createdShifts = new ArrayList<>();
        List<String> skippedDates = new ArrayList<>();

        LocalDate currentDate = request.startDate();
        LocalDate endDate = request.endDate();

        while (!currentDate.isAfter(endDate)) {
            if (shiftExistsForDate(businessId, request.siteId(), request.roleId(),
                    currentDate, request.startTime(), request.endTime())) {
                skippedDates.add(currentDate.toString());
            } else {
                Shift shift = createShiftForDate(business, site, role, request, currentDate);
                createdShifts.add(toDto(shift));
            }
            currentDate = currentDate.plusDays(1);
        }

        return BulkShiftCreateResponse.of(createdShifts, skippedDates);
    }

    private boolean shiftExistsForDate(Long businessId, Long siteId, Long roleId,
                                        LocalDate date, java.time.LocalTime startTime,
                                        java.time.LocalTime endTime) {
        return shiftRepository.findByBusiness_IdAndShiftDateBetween(businessId, date, date)
            .stream()
            .anyMatch(s -> s.getSite().getId().equals(siteId)
                && s.getRole().getId().equals(roleId)
                && s.getStartTime().equals(startTime)
                && s.getEndTime().equals(endTime)
                && s.getStatus() != ShiftStatus.cancelled);
    }

    private Shift createShiftForDate(Business business, Site site, Role role,
                                      BulkShiftCreateRequest request, LocalDate date) {
        validateShiftAgainstOperatingHours(site, date, request.startTime(), request.endTime());

        Shift shift = new Shift();
        shift.setBusiness(business);
        shift.setSite(site);
        shift.setRole(role);
        shift.setShiftDate(date);
        shift.setStartTime(request.startTime());
        shift.setEndTime(request.endTime());
        shift.setBreakDurationMinutes(request.breakDurationMinutes() != null
            ? request.breakDurationMinutes() : 0);
        shift.setStaffRequired(request.staffRequired() != null
            ? request.staffRequired() : 1);
        shift.setStaffAllocated(0);
        shift.setStatus(ShiftStatus.open);
        shift.setNotes(request.notes());
        return shiftRepository.save(shift);
    }

    @Transactional
    public BulkShiftUpdateResponse bulkUpdateShifts(Long businessId, BulkShiftUpdateRequest request) {
        List<Shift> shifts = shiftRepository
            .findByBusiness_IdAndShiftDateBetween(businessId, request.startDate(), request.endDate())
            .stream()
            .filter(s -> s.getStatus() != ShiftStatus.cancelled)
            .collect(Collectors.toList());

        if (request.roleIds() != null && !request.roleIds().isEmpty()) {
            shifts = shifts.stream()
                .filter(s -> request.roleIds().contains(s.getRole().getId()))
                .collect(Collectors.toList());
        }

        for (Shift shift : shifts) {
            if (request.breakDurationMinutes() != null) {
                shift.setBreakDurationMinutes(request.breakDurationMinutes());
            }
            if (request.staffRequired() != null) {
                shift.setStaffRequired(request.staffRequired());
            }
        }
        shiftRepository.saveAll(shifts);

        return new BulkShiftUpdateResponse(shifts.size());
    }

    @Transactional
    public void cancelShift(Long businessId, Long shiftId) {
        Shift shift = shiftRepository.findByIdAndBusiness_Id(shiftId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Shift", shiftId));

        // Delete all time blocks associated with this shift
        List<TimeBlock> timeBlocks = timeBlockRepository.findByShift_Id(shiftId);
        if (!timeBlocks.isEmpty()) {
            timeBlockRepository.deleteAll(timeBlocks);
        }

        // Reset allocation count and set status to cancelled
        shift.setStaffAllocated(0);
        shift.setStatus(ShiftStatus.cancelled);
        shiftRepository.save(shift);
    }

    private ShiftDto toDto(Shift shift) {
        return new ShiftDto(
            shift.getId(),
            shift.getSite().getId(),
            shift.getSite().getName(),
            shift.getRole().getId(),
            shift.getRole().getName(),
            shift.getShiftDate(),
            shift.getStartTime(),
            shift.getEndTime(),
            shift.getBreakDurationMinutes(),
            shift.getTotalHours(),
            shift.getStaffRequired(),
            shift.getStaffAllocated(),
            shift.getStatus().name(),
            shift.getNotes(),
            shift.getFillRate()
        );
    }

    /**
     * Validates shift times are valid (not zero-duration).
     * @throws ValidationException if start time equals end time
     */
    private void validateShiftDuration(LocalTime startTime, LocalTime endTime) {
        if (startTime.equals(endTime)) {
            throw new ValidationException("shift",
                "Start time and end time cannot be the same. Shift must have a duration.");
        }
    }

    /**
     * Validates that a shift falls within the site's operating hours.
     * Skips validation if no operating hours are configured for the site.
     */
    private void validateShiftAgainstOperatingHours(Site site, LocalDate shiftDate,
                                                     LocalTime startTime, LocalTime endTime) {
        // First validate shift has valid duration
        validateShiftDuration(startTime, endTime);

        int dayOfWeek = convertToDatabaseDayOfWeek(shiftDate);

        Optional<SiteOperatingHours> hoursOpt = siteOperatingHoursRepository
            .findBySite_IdAndDayOfWeek(site.getId(), dayOfWeek);

        if (hoursOpt.isEmpty()) {
            return; // No operating hours configured, allow shift
        }

        SiteOperatingHours hours = hoursOpt.get();

        if (hours.isClosed()) {
            log.warn("Shift validation failed: site '{}' is closed on {} (dayOfWeek={})",
                site.getName(), shiftDate, dayOfWeek);
            throw new ValidationException("shift",
                "Cannot create shift: site '" + site.getName() + "' is closed on this day");
        }

        if (hours.getOpenTime() != null && startTime.isBefore(hours.getOpenTime())) {
            log.warn("Shift validation failed: start time {} is before site '{}' opening time {} on {}",
                startTime, site.getName(), hours.getOpenTime(), shiftDate);
            throw new ValidationException("shift",
                "Shift start time (" + startTime + ") is before site opening time (" + hours.getOpenTime() + ")");
        }

        if (hours.getCloseTime() != null && endTime.isAfter(hours.getCloseTime())) {
            log.warn("Shift validation failed: end time {} is after site '{}' closing time {} on {}",
                endTime, site.getName(), hours.getCloseTime(), shiftDate);
            throw new ValidationException("shift",
                "Shift end time (" + endTime + ") is after site closing time (" + hours.getCloseTime() + ")");
        }
    }

    /**
     * Converts Java DayOfWeek (Monday=1 to Sunday=7) to database format (Sunday=0 to Saturday=6).
     */
    private int convertToDatabaseDayOfWeek(LocalDate date) {
        return date.getDayOfWeek().getValue() % 7;
    }
}
