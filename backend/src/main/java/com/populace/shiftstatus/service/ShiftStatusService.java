package com.populace.shiftstatus.service;

import com.populace.common.exception.ResourceNotFoundException;
import com.populace.domain.Shift;
import com.populace.domain.ShiftRole;
import com.populace.domain.enums.ShiftStatus;
import com.populace.repository.ShiftRepository;
import com.populace.repository.ShiftRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ShiftStatusService {

    private final ShiftRepository shiftRepository;
    private final ShiftRoleRepository shiftRoleRepository;

    public ShiftStatusService(ShiftRepository shiftRepository,
                               ShiftRoleRepository shiftRoleRepository) {
        this.shiftRepository = shiftRepository;
        this.shiftRoleRepository = shiftRoleRepository;
    }

    @Transactional
    public Shift updateShiftMetrics(Long shiftId) {
        Shift shift = shiftRepository.findById(shiftId)
            .orElseThrow(() -> new ResourceNotFoundException("Shift", shiftId));

        calculateShiftMetrics(shift);
        determineShiftStatus(shift);

        return shiftRepository.save(shift);
    }

    @Transactional
    public Shift updateShiftMetrics(Long businessId, Long shiftId) {
        Shift shift = shiftRepository.findByIdAndBusiness_Id(shiftId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Shift", shiftId));

        calculateShiftMetrics(shift);
        determineShiftStatus(shift);

        return shiftRepository.save(shift);
    }

    public void calculateShiftMetrics(Shift shift) {
        List<ShiftRole> shiftRoles = shiftRoleRepository.findByShift_Id(shift.getId());

        int totalRequired = 0;
        int totalAllocated = 0;

        for (ShiftRole role : shiftRoles) {
            totalRequired += role.getStaffRequired();
            totalAllocated += role.getStaffAllocated();
        }

        shift.setStaffRequired(totalRequired);
        shift.setStaffAllocated(totalAllocated);

        BigDecimal fillRate = calculateFillRate(totalRequired, totalAllocated);
        shift.setFillRate(fillRate);
    }

    public void determineShiftStatus(Shift shift) {
        if (shift.getStatus() == ShiftStatus.cancelled) {
            return;
        }

        int required = shift.getStaffRequired();
        int allocated = shift.getStaffAllocated();

        if (required == 0) {
            shift.setStatus(ShiftStatus.open);
            return;
        }

        if (allocated == 0) {
            shift.setStatus(ShiftStatus.open);
        } else if (allocated < required) {
            shift.setStatus(ShiftStatus.partially_filled);
        } else {
            shift.setStatus(ShiftStatus.filled);
        }
    }

    private BigDecimal calculateFillRate(int required, int allocated) {
        if (required == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal rate = BigDecimal.valueOf(allocated)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(required), 2, RoundingMode.HALF_UP);

        return rate.min(BigDecimal.valueOf(100));
    }

    public Shift getShiftWithMetrics(Long businessId, Long shiftId) {
        Shift shift = shiftRepository.findByIdAndBusiness_Id(shiftId, businessId)
            .orElseThrow(() -> new ResourceNotFoundException("Shift", shiftId));

        calculateShiftMetrics(shift);
        determineShiftStatus(shift);

        return shift;
    }
}
