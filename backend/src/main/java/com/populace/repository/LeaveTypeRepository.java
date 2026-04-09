package com.populace.repository;

import com.populace.domain.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {

    List<LeaveType> findByBusiness_IdAndActiveTrue(Long businessId);

    Optional<LeaveType> findByIdAndBusiness_Id(Long id, Long businessId);
}
