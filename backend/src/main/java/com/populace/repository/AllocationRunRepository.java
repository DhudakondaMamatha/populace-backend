package com.populace.repository;

import com.populace.domain.AllocationRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AllocationRunRepository extends JpaRepository<AllocationRun, Long> {

	List<AllocationRun> findByBusinessIdOrderByCreatedAtDesc(Long businessId);

	Optional<AllocationRun> findByRunId(String runId);
}
