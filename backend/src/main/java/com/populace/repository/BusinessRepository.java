package com.populace.repository;

import com.populace.domain.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusinessRepository extends JpaRepository<Business, Long> {

    Optional<Business> findByEmail(String email);

    Optional<Business> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByEmail(String email);

    Optional<Business> findByBusinessCode(String businessCode);

    boolean existsByBusinessCode(String businessCode);
}
