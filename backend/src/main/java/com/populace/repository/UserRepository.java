package com.populace.repository;

import com.populace.domain.User;
import com.populace.domain.enums.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByBusiness_IdAndEmail(Long businessId, String email);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    List<User> findByBusiness_IdAndDeletedAtIsNull(Long businessId);

    List<User> findByBusiness_IdAndUserTypeAndDeletedAtIsNull(Long businessId, UserType userType);

    boolean existsByBusiness_IdAndEmail(Long businessId, String email);

    boolean existsByEmail(String email);
}
