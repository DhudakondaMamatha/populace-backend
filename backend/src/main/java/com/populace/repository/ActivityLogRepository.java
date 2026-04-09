package com.populace.repository;

import com.populace.domain.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findByBusiness_IdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
        Long businessId, String entityType, Long entityId);

    List<ActivityLog> findByBusiness_IdAndActionOrderByCreatedAtDesc(
        Long businessId, String action);
}
