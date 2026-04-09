package com.populace.leave.service;

import com.populace.common.exception.ResourceNotFoundException;
import com.populace.common.exception.ValidationException;
import com.populace.domain.*;
import com.populace.domain.enums.LeaveRequestStatus;
import com.populace.leave.dto.LeaveRequestCreateDto;
import com.populace.leave.dto.LeaveRequestDto;
import com.populace.leave.dto.LeaveReviewDto;
import com.populace.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.populace.leave.dto.LeaveTypeDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class LeaveService {

    private static final Logger log = LoggerFactory.getLogger(LeaveService.class);

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final StaffMemberRepository staffRepository;
    private final UserRepository userRepository;
    private final StaffLeaveBalanceRepository balanceRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final BusinessRepository businessRepository;
    private final LeaveTypeInitializer leaveTypeInitializer;

    public LeaveService(LeaveRequestRepository leaveRequestRepository,
                        LeaveTypeRepository leaveTypeRepository,
                        StaffMemberRepository staffRepository,
                        UserRepository userRepository,
                        StaffLeaveBalanceRepository balanceRepository,
                        TimeBlockRepository timeBlockRepository,
                        BusinessRepository businessRepository,
                        LeaveTypeInitializer leaveTypeInitializer) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.staffRepository = staffRepository;
        this.userRepository = userRepository;
        this.balanceRepository = balanceRepository;
        this.timeBlockRepository = timeBlockRepository;
        this.businessRepository = businessRepository;
        this.leaveTypeInitializer = leaveTypeInitializer;
    }
    @Transactional(readOnly = true)
    public List<LeaveRequestDto> listRequests(Long businessId) {
        return leaveRequestRepository.findByBusinessId(businessId).stream()
            .map(this::toDto)
            .toList();
    }
    @Transactional(readOnly = true)
    public LeaveRequestDto getRequest(Long businessId, Long requestId) {
        LeaveRequest request = leaveRequestRepository.findByIdAndBusinessId(requestId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", requestId));
        return toDto(request);
    }

    @Transactional
    public LeaveRequestDto createRequest(Long businessId, LeaveRequestCreateDto dto) {
        StaffMember staff = staffRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(
                dto.staffId(), businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Staff", dto.staffId()));

        LeaveType leaveType = leaveTypeRepository.findByIdAndBusiness_Id(
                dto.leaveTypeId(), businessId)
            .orElseThrow(() -> new ResourceNotFoundException("LeaveType", dto.leaveTypeId()));

        validateNoOverlap(dto.staffId(), dto.startDate(), dto.endDate());

        LeaveRequest request = new LeaveRequest();
        request.setStaff(staff);
        request.setLeaveType(leaveType);
        request.setStartDate(dto.startDate());
        request.setEndDate(dto.endDate());
        request.setTotalDays(dto.totalDays() != null ? dto.totalDays() :
            calculateDays(dto.startDate(), dto.endDate()));
        request.setReason(dto.reason());
        request.setStatus(LeaveRequestStatus.pending);

        request = leaveRequestRepository.save(request);
        return toDto(request);
    }

    @Transactional
    public LeaveRequestDto approveRequest(Long businessId, Long requestId,
                                          Long reviewerId, LeaveReviewDto dto) {
        LeaveRequest request = leaveRequestRepository.findByIdAndBusinessId(requestId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", requestId));

        if (request.getStatus() != LeaveRequestStatus.pending) {
            throw new ValidationException("status", "Request is not pending");
        }

        User reviewer = userRepository.findById(reviewerId).orElse(null);
        request.setStatus(LeaveRequestStatus.approved);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(Instant.now());
        request.setReviewNotes(dto != null ? dto.notes() : null);

        request = leaveRequestRepository.save(request);

        // Auto-cancel overlapping time blocks when leave is approved
        int cancelled = cancelOverlappingTimeBlocks(
            request.getStaff().getId(),
            request.getStartDate(),
            request.getEndDate()
        );
        if (cancelled > 0) {
            log.info("Leave approval triggered deletion of {} time blocks for staff {}",
                cancelled, request.getStaff().getId());
        }

        return toDto(request);
    }

    /**
     * Delete all time blocks that overlap with the approved leave period.
     * TimeBlocks are deleted rather than marked cancelled since the V2 engine
     * creates new blocks on each allocation run.
     *
     * @return number of blocks deleted
     */
    private int cancelOverlappingTimeBlocks(Long staffId,
                                             java.time.LocalDate startDate,
                                             java.time.LocalDate endDate) {
        List<TimeBlock> overlapping = timeBlockRepository
            .findActiveBlocksInDateRange(staffId, startDate, endDate);

        int count = overlapping.size();
        if (count > 0) {
            timeBlockRepository.deleteAll(overlapping);

            // Update shift allocated counts
            for (TimeBlock block : overlapping) {
                Shift shift = block.getShift();
                if (shift != null && shift.getStaffAllocated() != null && shift.getStaffAllocated() > 0) {
                    shift.setStaffAllocated(shift.getStaffAllocated() - 1);
                }
            }
        }
        return count;
    }

    @Transactional
    public LeaveRequestDto rejectRequest(Long businessId, Long requestId,
                                         Long reviewerId, LeaveReviewDto dto) {
        LeaveRequest request = leaveRequestRepository.findByIdAndBusinessId(requestId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", requestId));

        if (request.getStatus() != LeaveRequestStatus.pending) {
            throw new ValidationException("status", "Request is not pending");
        }

        User reviewer = userRepository.findById(reviewerId).orElse(null);
        request.setStatus(LeaveRequestStatus.rejected);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(Instant.now());
        request.setReviewNotes(dto != null ? dto.notes() : null);

        request = leaveRequestRepository.save(request);
        return toDto(request);
    }

    public List<LeaveRequestDto> listRequestsByStaff(Long businessId, Long staffId,
                                                      LeaveRequestStatus status, Integer year) {
        // Verify staff belongs to business
        staffRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(staffId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));

        List<LeaveRequest> requests;
        if (status != null && year != null) {
            requests = leaveRequestRepository.findByStaffIdAndStatusAndYear(staffId, status, year);
        } else if (status != null) {
            requests = leaveRequestRepository.findByStaffIdAndStatus(staffId, status);
        } else if (year != null) {
            requests = leaveRequestRepository.findByStaffIdAndYear(staffId, year);
        } else {
            requests = leaveRequestRepository.findByStaffId(staffId);
        }

        return requests.stream().map(this::toDto).toList();
    }

    @Transactional
    public List<LeaveTypeDto> listLeaveTypes(Long businessId) {
        List<LeaveType> types = leaveTypeRepository.findByBusiness_IdAndActiveTrue(businessId);

        // Auto-initialize leave types for existing businesses that don't have any
        if (types.isEmpty()) {
            Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", businessId));
            leaveTypeInitializer.initializeDefaultLeaveTypes(business);
            types = leaveTypeRepository.findByBusiness_IdAndActiveTrue(businessId);
        }

        return types.stream()
            .map(this::toLeaveTypeDto)
            .toList();
    }

    @Transactional
    public LeaveRequestDto cancelRequest(Long businessId, Long requestId, Long userId) {
        LeaveRequest request = leaveRequestRepository.findByIdAndBusinessId(requestId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", requestId));

        if (request.getStatus() != LeaveRequestStatus.pending &&
            request.getStatus() != LeaveRequestStatus.approved) {
            throw new ValidationException("status", "Only pending or approved requests can be cancelled");
        }

        User reviewer = userRepository.findById(userId).orElse(null);
        request.setStatus(LeaveRequestStatus.cancelled);
        request.setReviewedBy(reviewer);
        request.setReviewedAt(Instant.now());

        request = leaveRequestRepository.save(request);
        log.info("Leave request {} cancelled by user {}", requestId, userId);

        return toDto(request);
    }

    private void validateNoOverlap(Long staffId, java.time.LocalDate start, java.time.LocalDate end) {
        List<LeaveRequest> overlaps = leaveRequestRepository.findApprovedOverlapping(
            staffId, start, end);
        if (!overlaps.isEmpty()) {
            throw new ValidationException("dates", "Overlaps existing approved leave");
        }
    }

    private BigDecimal calculateDays(java.time.LocalDate start, java.time.LocalDate end) {
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        return BigDecimal.valueOf(days);
    }

    private LeaveRequestDto toDto(LeaveRequest r) {
        return new LeaveRequestDto(
            r.getId(),
            r.getStaff().getId(),
            r.getStaff().getFirstName() + " " + r.getStaff().getLastName(),
            r.getLeaveType().getId(),
            r.getLeaveType().getName(),
            r.getStartDate(),
            r.getEndDate(),
            r.getTotalDays(),
            r.getReason(),
            r.getStatus().name(),
            r.getReviewedBy() != null ? r.getReviewedBy().getFullName() : null,
            r.getReviewedAt(),
            r.getReviewNotes(),
            r.getCreatedAt()
        );
    }

    private LeaveTypeDto toLeaveTypeDto(LeaveType lt) {
        return new LeaveTypeDto(
            lt.getId(),
            lt.getName(),
            lt.getCode(),
            lt.isPaid(),
            lt.isRequiresApproval(),
            lt.getMinNoticeDays(),
            lt.getColor()
        );
    }
}
