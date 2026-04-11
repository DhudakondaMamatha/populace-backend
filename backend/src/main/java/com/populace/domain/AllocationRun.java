package com.populace.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "allocation_runs")
public class AllocationRun {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "run_id", nullable = false, unique = true)
	private String runId;

	@Column(name = "business_id", nullable = false)
	private Long businessId;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "total_shifts")
	private int totalShifts;

	@Column(name = "shifts_filled")
	private int shiftsFilled;

	@Column(name = "shifts_partial")
	private int shiftsPartial;

	@Column(name = "shifts_unfilled")
	private int shiftsUnfilled;

	@Column(name = "total_allocations")
	private int totalAllocations;

	@Column(nullable = false)
	private String status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	public AllocationRun() {
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public String getRunId() { return runId; }
	public void setRunId(String runId) { this.runId = runId; }

	public Long getBusinessId() { return businessId; }
	public void setBusinessId(Long businessId) { this.businessId = businessId; }

	public LocalDate getStartDate() { return startDate; }
	public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

	public LocalDate getEndDate() { return endDate; }
	public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

	public int getTotalShifts() { return totalShifts; }
	public void setTotalShifts(int totalShifts) { this.totalShifts = totalShifts; }

	public int getShiftsFilled() { return shiftsFilled; }
	public void setShiftsFilled(int shiftsFilled) { this.shiftsFilled = shiftsFilled; }

	public int getShiftsPartial() { return shiftsPartial; }
	public void setShiftsPartial(int shiftsPartial) { this.shiftsPartial = shiftsPartial; }

	public int getShiftsUnfilled() { return shiftsUnfilled; }
	public void setShiftsUnfilled(int shiftsUnfilled) { this.shiftsUnfilled = shiftsUnfilled; }

	public int getTotalAllocations() { return totalAllocations; }
	public void setTotalAllocations(int totalAllocations) { this.totalAllocations = totalAllocations; }

	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }

	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

	public Instant getCompletedAt() { return completedAt; }
	public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
