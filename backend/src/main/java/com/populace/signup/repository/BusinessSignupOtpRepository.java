package com.populace.signup.repository;

import com.populace.signup.model.BusinessSignupOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface BusinessSignupOtpRepository extends JpaRepository<BusinessSignupOtp, Long> {

    Optional<BusinessSignupOtp> findByEmailAndVerifiedAtIsNull(String email);

    Optional<BusinessSignupOtp> findByVerificationTokenAndVerifiedAtIsNotNull(String token);

    Optional<BusinessSignupOtp> findByVerificationToken(String token);

    boolean existsByEmailAndVerifiedAtIsNull(String email);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM BusinessSignupOtp o WHERE o.expiresAt < :cutoff AND o.verifiedAt IS NULL")
    int deleteExpiredUnverified(Instant cutoff);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM BusinessSignupOtp o WHERE o.email = :email")
    void deleteByEmail(String email);
}
