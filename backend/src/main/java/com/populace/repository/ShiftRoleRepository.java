package com.populace.repository;

import com.populace.domain.ShiftRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftRoleRepository extends JpaRepository<ShiftRole, Long> {

    List<ShiftRole> findByShift_Id(Long shiftId);

    Optional<ShiftRole> findByShift_IdAndRole_Id(Long shiftId, Long roleId);

    boolean existsByShift_IdAndRole_Id(Long shiftId, Long roleId);
}
