package com.populace.repository;

import com.populace.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findByBusiness_IdAndDeletedAtIsNull(Long businessId);

    List<Role> findByBusiness_IdAndActiveAndDeletedAtIsNull(Long businessId, boolean active);

    Optional<Role> findByIdAndBusiness_IdAndDeletedAtIsNull(Long id, Long businessId);

    Optional<Role> findByBusiness_IdAndCode(Long businessId, String code);

    boolean existsByBusiness_IdAndDeletedAtIsNull(Long businessId);

    long countByBusiness_IdAndDeletedAtIsNull(Long businessId);

    boolean existsByIdAndBusiness_Id(Long id, Long businessId);
}
