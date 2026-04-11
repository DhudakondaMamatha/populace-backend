package com.populace.repository;

import com.populace.domain.RoleCombo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleComboRepository extends JpaRepository<RoleCombo, Long> {

    List<RoleCombo> findByBusiness_IdAndActiveTrue(Long businessId);

    List<RoleCombo> findByBusiness_Id(Long businessId);

    Optional<RoleCombo> findByIdAndBusiness_Id(Long id, Long businessId);
}
