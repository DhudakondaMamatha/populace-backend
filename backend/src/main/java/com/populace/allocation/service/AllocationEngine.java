package com.populace.allocation.service;

import com.populace.allocation.dto.*;
import com.populace.domain.*;
import com.populace.domain.enums.AvailabilityType;
import com.populace.domain.enums.BlockType;
import com.populace.domain.enums.EmploymentStatus;
import com.populace.domain.enums.LeaveRequestStatus;
import com.populace.domain.enums.ProficiencyLevel;
import com.populace.domain.enums.ShiftStatus;
import com.populace.domain.enums.SkillLevel;
import com.populace.repository.*;
import com.populace.shiftstatus.service.ShiftStatusService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AllocationEngine {

	private static final Logger log = LoggerFactory.getLogger(AllocationEngine.class);

	private static final Set<String> MANUAL_CREATED_BY = Set.of("MANUAL", "MANUAL_ALLOCATION");

	private final ShiftRepository shiftRepository;
	private final TimeBlockRepository timeBlockRepository;
	private final StaffMemberRepository staffMemberRepository;
	private final StaffRoleRepository staffRoleRepository;
	private final StaffSiteRepository staffSiteRepository;
	private final StaffAvailabilityRepository staffAvailabilityRepository;
	private final StaffCompensationRepository staffCompensationRepository;
	private final StaffWorkParametersRepository staffWorkParametersRepository;
	private final WorkParametersRepository workParametersRepository;
	private final AllocationRuleRepository allocationRuleRepository;
	private final BusinessRepository businessRepository;
	private final ShiftStatusService shiftStatusService;
	private final RoleRepository roleRepository;
	private final RoleComboRoleRepository roleComboRoleRepository;
	private final LeaveRequestRepository leaveRequestRepository;
	private final AllocationRunRepository allocationRunRepository;
	private final JdbcTemplate jdbcTemplate;

	public AllocationEngine(ShiftRepository shiftRepository, TimeBlockRepository timeBlockRepository,
			StaffMemberRepository staffMemberRepository, StaffRoleRepository staffRoleRepository,
			StaffSiteRepository staffSiteRepository, StaffAvailabilityRepository staffAvailabilityRepository,
			StaffCompensationRepository staffCompensationRepository,
			StaffWorkParametersRepository staffWorkParametersRepository,
			WorkParametersRepository workParametersRepository, AllocationRuleRepository allocationRuleRepository,
			BusinessRepository businessRepository, ShiftStatusService shiftStatusService, RoleRepository roleRepository,
			RoleComboRoleRepository roleComboRoleRepository, LeaveRequestRepository leaveRequestRepository,
			AllocationRunRepository allocationRunRepository, JdbcTemplate jdbcTemplate) {
		this.shiftRepository = shiftRepository;
		this.timeBlockRepository = timeBlockRepository;
		this.staffMemberRepository = staffMemberRepository;
		this.staffRoleRepository = staffRoleRepository;
		this.staffSiteRepository = staffSiteRepository;
		this.staffAvailabilityRepository = staffAvailabilityRepository;
		this.staffCompensationRepository = staffCompensationRepository;
		this.staffWorkParametersRepository = staffWorkParametersRepository;
		this.workParametersRepository = workParametersRepository;
		this.allocationRuleRepository = allocationRuleRepository;
		this.businessRepository = businessRepository;
		this.shiftStatusService = shiftStatusService;
		this.roleRepository = roleRepository;
		this.roleComboRoleRepository = roleComboRoleRepository;
		this.leaveRequestRepository = leaveRequestRepository;
		this.allocationRunRepository = allocationRunRepository;
		this.jdbcTemplate = jdbcTemplate;
	}

	// -------------------------------------------------------------------------
	// Context factory
	// -------------------------------------------------------------------------

	private AllocationContext createContext() {
		return new AllocationContext(
				staffRoleRepository, staffAvailabilityRepository, staffSiteRepository,
				staffCompensationRepository, staffWorkParametersRepository, workParametersRepository,
				leaveRequestRepository, roleComboRoleRepository, timeBlockRepository);
	}

	// -------------------------------------------------------------------------
	// Public API
	// -------------------------------------------------------------------------

	@Transactional(timeout = 120)
	public AllocationResultDto allocate(Long businessId, LocalDate startDate, LocalDate endDate,
			boolean clearAutoFirst) {
		return allocate(businessId, startDate, endDate, clearAutoFirst, null);
	}

	@Transactional(timeout = 120)
	public AllocationResultDto allocate(Long businessId, LocalDate startDate, LocalDate endDate,
			boolean clearAutoFirst, List<Long> siteIds) {
		String runId = UUID.randomUUID().toString();
		log.info("Starting allocation run {} for business {} [{} - {}]", runId, businessId, startDate, endDate);

		boolean lockAcquired = acquireAllocationLock(businessId);
		if (!lockAcquired) {
			log.warn("Allocation already in progress for business {}", businessId);
			return new AllocationResultDto(runId, 0, 0, 0, 0, 0, 0,
					"rejected_concurrent", startDate.toString(), endDate.toString());
		}

		Business business = businessRepository.findById(businessId)
				.orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));

		AllocationContext ctx = createContext();

		Map<String, Boolean> enabledRules = loadEnabledRules(businessId);
		List<String> sortingPriority = loadSortingPriority(businessId);

		// Optionally clear auto-allocations first
		if (clearAutoFirst) {
			clearAutoAllocations(businessId, startDate, endDate);
		}

		List<Shift> shifts = shiftRepository.findUnfilledShifts(businessId, startDate, endDate);
		if (siteIds != null && !siteIds.isEmpty()) {
			shifts = shifts.stream()
					.filter(s -> siteIds.contains(s.getSite().getId()))
					.collect(Collectors.toList());
		}
		int totalAllocations = 0;

		for (Shift shift : shifts) {
			if (shift.getStatus() == ShiftStatus.cancelled) {
				continue;
			}
			List<StaffMember> allStaff = new ArrayList<>(staffRoleRepository.findByBussinessIdAndRoldId(businessId,
					shift.getRole().getId(), EmploymentStatus.active));

			// Expand candidates via role combos (staff with sibling roles in the same
			// combo)
			if (isRuleEnabled(enabledRules, "role_combos")) {
				Set<Long> seenIds = allStaff.stream().map(StaffMember::getId).collect(Collectors.toSet());
				for (Long siblingRoleId : ctx.getComboSiblingRoleIds(shift.getRole().getId(), businessId)) {
					for (StaffMember cs : staffRoleRepository.findByBussinessIdAndRoldId(businessId, siblingRoleId,
							EmploymentStatus.active)) {
						if (seenIds.add(cs.getId())) {
							allStaff.add(cs);
						}
					}
				}
			}

			if (shift.getRemainingSlots() <= 0) {
				continue;
			}

			ZoneId shiftZone = resolveZone(business.getTimezone());
			Instant shiftStart = resolveInstant(shift.getStartInstant(), shift.getShiftDate(), shift.getStartTime(),
					shiftZone);
			Instant shiftEnd = resolveInstant(shift.getEndInstant(), shift.getShiftDate(), shift.getEndTime(),
					shiftZone);

			if (shiftStart == null || shiftEnd == null || !shiftStart.isBefore(shiftEnd)) {
				log.warn("Skipping shift {} - invalid or missing time data", shift.getId());
				continue;
			}

			// Full-shift assignment: assign each eligible staff to the complete shift
			List<StaffMember> eligible = allStaff.stream()
					.filter(s -> collectHardViolations(s, shift, business, enabledRules, ctx).isEmpty())
					.collect(Collectors.toList());

			eligible = applySorting(eligible, shift, business, sortingPriority, ctx);

			Set<Long> usedInShift = new HashSet<>(getAllocatedStaffIds(shift.getId(), shift.getRole().getId()));
			List<StaffMember> candidates = eligible.stream().filter(s -> !usedInShift.contains(s.getId()))
					.collect(Collectors.toList());

			int slotsNeeded = shift.getRemainingSlots();
			int blocksCreated = 0;
			for (StaffMember staff : candidates) {
				if (blocksCreated >= slotsNeeded) break;
				createWorkBlockForStaff(staff, shift, shiftStart, shiftEnd, business, enabledRules, "AUTO", ctx);
				usedInShift.add(staff.getId());
				blocksCreated++;
				totalAllocations++;
			}

			// Partial allocation: fill remaining slots with partial-shift staff
			if (isRuleEnabled(enabledRules, "partial_allocation") && blocksCreated < slotsNeeded) {
				int remainingSlots = slotsNeeded - blocksCreated;
				int partialAllocations = allocatePartialForShift(
						shift, allStaff, shiftStart, shiftEnd, business,
						enabledRules, sortingPriority, usedInShift, remainingSlots, ctx);
				totalAllocations += partialAllocations;
			}

			shiftStatusService.updateShiftMetrics(shift.getId());
		}

		// Compute final counts
		List<Object[]> statusCounts = shiftRepository.countShiftsByStatusInRange(businessId, startDate, endDate);
		int totalShifts = 0;
		int shiftsFilled = 0;
		int shiftsPartial = 0;
		int shiftsUnfilled = 0;
		for (Object[] row : statusCounts) {
			ShiftStatus s = (ShiftStatus) row[0];
			long count = ((Number) row[1]).longValue();
			totalShifts += count;
			if (s == ShiftStatus.filled) {
				shiftsFilled += count;
			} else if (s == ShiftStatus.partially_filled) {
				shiftsPartial += count;
			} else if (s == ShiftStatus.open) {
				shiftsUnfilled += count;
			}
		}

		log.info("Allocation run {} complete. Total allocations: {}", runId, totalAllocations);

		saveAllocationRun(runId, businessId, startDate, endDate, totalShifts, shiftsFilled,
				shiftsPartial, shiftsUnfilled, totalAllocations, "completed");

		return new AllocationResultDto(runId, totalShifts, shiftsFilled, shiftsPartial, shiftsUnfilled, totalAllocations,
				totalAllocations, "completed", startDate.toString(), endDate.toString());
	}

	@Transactional(readOnly = true)
	public PreCheckDto preCheck(Long businessId, LocalDate startDate, LocalDate endDate) {
		List<Object[]> statusCounts = shiftRepository.countShiftsByStatusInRange(businessId, startDate, endDate);

		int totalShifts = 0;
		int openShifts = 0;
		int partialShifts = 0;
		int filledShifts = 0;

		for (Object[] row : statusCounts) {
			ShiftStatus s = (ShiftStatus) row[0];
			long count = ((Number) row[1]).longValue();
			totalShifts += count;
			if (s == ShiftStatus.open) {
				openShifts += (int) count;
			} else if (s == ShiftStatus.partially_filled) {
				partialShifts += (int) count;
			} else if (s == ShiftStatus.filled) {
				filledShifts += (int) count;
			}
		}

		long autoAllocationsToRemove = timeBlockRepository.countAutoAllocationsInRange(businessId, startDate, endDate);
		long manualAllocationsPreserved = timeBlockRepository.countManualAllocationsInRange(businessId, startDate,
				endDate);
		boolean hasFilledShifts = filledShifts > 0;

		String warningMessage = null;
		if (hasFilledShifts) {
			warningMessage = filledShifts + " fully filled shift(s) exist in this range. "
					+ "Running allocation will skip already-filled shifts but will clear auto-allocations from partial shifts.";
		}

		return new PreCheckDto(totalShifts, openShifts, partialShifts, filledShifts, autoAllocationsToRemove,
				manualAllocationsPreserved, hasFilledShifts, warningMessage);
	}

	@Transactional(readOnly = true)
	public EligibleStaffDto getEligibleStaff(Long businessId, Long shiftId) {
		Shift shift = shiftRepository.findByIdAndBusiness_Id(shiftId, businessId)
				.orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));

		Business business = businessRepository.findById(businessId)
				.orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));

		AllocationContext ctx = createContext();
		Map<String, Boolean> enabledRules = loadEnabledRules(businessId);

		// Already-allocated blocks for this shift
		List<TimeBlock> existingBlocks = timeBlockRepository.findByShift_Id(shiftId).stream()
				.filter(tb -> tb.getBlockType() == BlockType.work).collect(Collectors.toList());

		Set<Long> alreadyAllocatedStaffIds = existingBlocks.stream().map(TimeBlock::getStaffId).filter(Objects::nonNull)
				.collect(Collectors.toSet());

		List<EligibleStaffDto.AllocatedStaffDto> alreadyAllocated = existingBlocks.stream()
				.map(tb -> new EligibleStaffDto.AllocatedStaffDto(tb.getId(), tb.getStaffId(),
						tb.getStaff() != null ? tb.getStaff().getFullName() : "Unknown",
						MANUAL_CREATED_BY.contains(tb.getCreatedBy().toUpperCase()) ? "MANUAL" : "AUTO", "active"))
				.collect(Collectors.toList());

		List<StaffMember> allStaff = staffMemberRepository.findActiveStaff(businessId);
		List<StaffMember> unallocated = allStaff.stream().filter(s -> !alreadyAllocatedStaffIds.contains(s.getId()))
				.collect(Collectors.toList());

		List<EligibleStaffDto.EligiblePersonDto> eligibleList = new ArrayList<>();
		List<EligibleStaffDto.IneligiblePersonDto> ineligibleList = new ArrayList<>();

		for (StaffMember staff : unallocated) {
			List<String> violations = collectHardViolations(staff, shift, business, enabledRules, ctx);
			if (violations.isEmpty()) {
				double score = computeScore(staff, shift, business, enabledRules, ctx);
				eligibleList.add(new EligibleStaffDto.EligiblePersonDto(staff.getId(), staff.getFullName(), score, true,
						List.of()));
			} else {
				ineligibleList.add(new EligibleStaffDto.IneligiblePersonDto(staff.getId(), staff.getFullName(),
						violations.get(0), violations));
			}
		}

		// Sort eligible by score descending
		eligibleList.sort(Comparator.comparingDouble(EligibleStaffDto.EligiblePersonDto::score).reversed());

		EligibleStaffDto.ShiftDetailsDto shiftDetails = new EligibleStaffDto.ShiftDetailsDto(shift.getSite().getName(),
				shift.getRole().getName(), shift.getShiftDate().toString(), shift.getStartTime().toString(),
				shift.getEndTime().toString(), shift.getStaffRequired(), shift.getStaffAllocated(),
				shift.getRemainingSlots());

		return new EligibleStaffDto(shiftDetails, alreadyAllocated, eligibleList, ineligibleList);
	}

	@Transactional
	public ManualAllocationResponse createManualAllocation(Long businessId, Long shiftId, ManualAllocationRequest req) {
		Shift shift = shiftRepository.findByIdAndBusiness_Id(shiftId, businessId)
				.orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));

		Business business = businessRepository.findById(businessId)
				.orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));

		StaffMember staff = staffMemberRepository.findByIdAndBusiness_IdAndDeletedAtIsNull(req.staffId(), businessId)
				.orElseThrow(() -> new IllegalArgumentException("Staff not found: " + req.staffId()));

		// Check capacity before allocation
		if (shift.getRemainingSlots() <= 0 && !req.overrideViolations()) {
			log.warn("Manual allocation rejected for shift {} - shift is fully staffed", shiftId);
			return new ManualAllocationResponse(false, null, shiftId, req.staffId(), staff.getFullName(), null,
					null, false);
		}

		AllocationContext ctx = createContext();
		Map<String, Boolean> enabledRules = loadEnabledRules(businessId);

		if (!req.overrideViolations()) {
			List<String> violations = collectHardViolations(staff, shift, business, enabledRules, ctx);
			if (!violations.isEmpty()) {
				log.debug("Manual allocation rejected for staff {} on shift {} - violations: {}", req.staffId(), shiftId, violations);
				return new ManualAllocationResponse(false, null, shiftId, req.staffId(), staff.getFullName(), null,
						null, false);
			}
		}

		// Create work block covering the full shift window
		ZoneId zone = resolveZone(business.getTimezone());
		Instant blockStart = resolveInstant(shift.getStartInstant(), shift.getShiftDate(), shift.getStartTime(), zone);
		Instant blockEnd = resolveInstant(shift.getEndInstant(), shift.getShiftDate(), shift.getEndTime(), zone);

		if (blockStart == null || blockEnd == null) {
			log.warn("Manual allocation rejected for shift {} - missing start/end time", shiftId);
			return new ManualAllocationResponse(false, null, shiftId, req.staffId(), staff.getFullName(), null,
					null, false);
		}

		TimeBlock workBlock = new TimeBlock();
		workBlock.setShift(shift);
		workBlock.setStaff(staff);
		workBlock.setRole(shift.getRole());
		workBlock.setSite(shift.getSite());
		workBlock.setStartTime(blockStart);
		workBlock.setEndTime(blockEnd);
		workBlock.setBlockType(BlockType.work);
		workBlock.setCreatedBy("MANUAL");
		workBlock.setOverridePlaced(req.overrideViolations());
		if (req.overrideViolations() && req.overrideReason() != null) {
			workBlock.setOverrideReason(req.overrideReason());
		}

		TimeBlock saved = timeBlockRepository.save(workBlock);
		ctx.invalidateStaffMutableData(staff.getId());

		// Insert break if needed (Rule 8)
		Map<String, Boolean> rules = enabledRules;
		if (rules.getOrDefault("max_continuous_hours", true)) {
			tryInsertBreak(staff, shift, business, rules, "MANUAL", ctx);
		}

		// Update shift metrics
		shiftStatusService.updateShiftMetrics(shiftId);

		// Fetch hourly rate
		BigDecimal hourlyRate = ctx
				.findCurrentRate(staff.getId(), shift.getRole().getId(), shift.getShiftDate()).orElse(BigDecimal.ZERO);

		long blockMinutes = Duration.between(blockStart, blockEnd).toSeconds() / 60;
		BigDecimal hours = BigDecimal.valueOf(blockMinutes).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
		BigDecimal estimatedCost = hourlyRate.multiply(hours).setScale(2, RoundingMode.HALF_UP);

		return new ManualAllocationResponse(true, saved.getId(), shiftId, staff.getId(), staff.getFullName(),
				hourlyRate, estimatedCost, req.overrideViolations());
	}

	@Transactional
	public RemoveAllocationResponse removeAllocation(Long businessId, Long shiftId, Long blockId) {
		Shift shift = shiftRepository.findByIdAndBusiness_Id(shiftId, businessId)
				.orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));

		TimeBlock block = timeBlockRepository.findById(blockId)
				.orElseThrow(() -> new IllegalArgumentException("TimeBlock not found: " + blockId));

		// Only allow removing manual allocations
		String createdBy = block.getCreatedBy() != null ? block.getCreatedBy().toUpperCase() : "";
		if (!MANUAL_CREATED_BY.contains(createdBy)) {
			return new RemoveAllocationResponse(false, blockId,
					"Cannot remove auto-allocated block. Use reallocate to remove auto-allocations.");
		}

		// Verify it belongs to this shift
		if (!shiftId.equals(block.getShiftId())) {
			return new RemoveAllocationResponse(false, blockId, "Block does not belong to shift " + shiftId);
		}

		timeBlockRepository.delete(block);

		// Update shift metrics
		shiftStatusService.updateShiftMetrics(shiftId);

		return new RemoveAllocationResponse(true, blockId, "Allocation removed successfully.");
	}

	@Transactional(readOnly = true)
	public ShiftAllocationsDto getShiftAllocations(Long businessId, Long shiftId) {
		Shift shift = shiftRepository.findByIdAndBusiness_Id(shiftId, businessId)
				.orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));

		List<TimeBlock> blocks = timeBlockRepository.findByShift_Id(shiftId).stream()
				.filter(tb -> tb.getBlockType() == BlockType.work).collect(Collectors.toList());

		List<ShiftAllocationsDto.AllocationItemDto> items = blocks.stream().map(tb -> {
			String createdBy = tb.getCreatedBy() != null ? tb.getCreatedBy().toUpperCase() : "";
			String allocationType = MANUAL_CREATED_BY.contains(createdBy) ? "MANUAL" : "AUTO";
			String staffName = tb.getStaff() != null ? tb.getStaff().getFullName() : "Unknown";
			return new ShiftAllocationsDto.AllocationItemDto(tb.getId(), tb.getStaffId(), staffName, allocationType,
					"active");
		}).collect(Collectors.toList());

		return new ShiftAllocationsDto(shiftId, shift.getSite().getName(), shift.getRole().getName(),
				shift.getStaffRequired(), shift.getStaffAllocated(), items);
	}

	@Transactional(timeout = 60)
	public ReallocateShiftResponse reallocateShift(Long businessId, Long shiftId) {
		Shift shift = shiftRepository.findByIdAndBusiness_Id(shiftId, businessId)
				.orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));

		Business business = businessRepository.findById(businessId)
				.orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));

		AllocationContext ctx = createContext();

		List<TimeBlock> existingBlocks = timeBlockRepository.findByShift_Id(shiftId);

		List<TimeBlock> autoBlocks = existingBlocks.stream().filter(
				tb -> !MANUAL_CREATED_BY.contains(tb.getCreatedBy() != null ? tb.getCreatedBy().toUpperCase() : ""))
				.collect(Collectors.toList());

		List<TimeBlock> manualBlocks = existingBlocks.stream().filter(
				tb -> MANUAL_CREATED_BY.contains(tb.getCreatedBy() != null ? tb.getCreatedBy().toUpperCase() : ""))
				.filter(tb -> tb.getBlockType() == BlockType.work).collect(Collectors.toList());

		int removedAuto = autoBlocks.size();
		timeBlockRepository.deleteAll(autoBlocks);

		int manualCount = manualBlocks.size();
		shift.setStaffAllocated(manualCount);
		shiftRepository.save(shift);

		Map<String, Boolean> enabledRules = loadEnabledRules(businessId);
		List<String> sortingPriority = loadSortingPriority(businessId);

		// Load staff filtered by shift's role
		List<StaffMember> allStaff = new ArrayList<>(staffRoleRepository.findByBussinessIdAndRoldId(businessId,
				shift.getRole().getId(), EmploymentStatus.active));
		if (isRuleEnabled(enabledRules, "role_combos")) {
			Set<Long> seenIds = allStaff.stream().map(StaffMember::getId).collect(Collectors.toSet());
			for (Long siblingRoleId : ctx.getComboSiblingRoleIds(shift.getRole().getId(), businessId)) {
				for (StaffMember cs : staffRoleRepository.findByBussinessIdAndRoldId(
						businessId, siblingRoleId, EmploymentStatus.active)) {
					if (seenIds.add(cs.getId())) {
						allStaff.add(cs);
					}
				}
			}
		}
		Set<Long> manualStaffIds = manualBlocks.stream().map(TimeBlock::getStaffId).filter(Objects::nonNull)
				.collect(Collectors.toSet());

		List<StaffMember> candidates = allStaff.stream().filter(s -> !manualStaffIds.contains(s.getId()))
				.collect(Collectors.toList());

		List<StaffMember> eligible = applyHardFilters(candidates, shift, business, enabledRules, ctx);
		eligible = applySorting(eligible, shift, business, sortingPriority, ctx);

		int slotsNeeded = shift.getRemainingSlots();
		int newAutoAllocations = 0;

		for (StaffMember staff : eligible) {
			if (newAutoAllocations >= slotsNeeded) break;

			int created = createWorkBlockForStaff(staff, shift, business, enabledRules, "AUTO", ctx);
			if (created > 0) {
				newAutoAllocations++;
			}
		}

		shiftStatusService.updateShiftMetrics(shiftId);

		return new ReallocateShiftResponse(shiftId, removedAuto, manualCount, newAutoAllocations);
	}

	@Transactional(readOnly = true)
	public List<AllocationRun> getAllocationRuns(Long businessId) {
		return allocationRunRepository.findByBusinessIdOrderByCreatedAtDesc(businessId);
	}

	@Transactional(readOnly = true)
	public Optional<AllocationRun> getAllocationRun(String runId) {
		return allocationRunRepository.findByRunId(runId);
	}

	// -------------------------------------------------------------------------
	// Internal helpers
	// -------------------------------------------------------------------------

	private void saveAllocationRun(String runId, Long businessId, LocalDate startDate, LocalDate endDate,
			int totalShifts, int shiftsFilled, int shiftsPartial, int shiftsUnfilled,
			int totalAllocations, String status) {
		AllocationRun run = new AllocationRun();
		run.setRunId(runId);
		run.setBusinessId(businessId);
		run.setStartDate(startDate);
		run.setEndDate(endDate);
		run.setTotalShifts(totalShifts);
		run.setShiftsFilled(shiftsFilled);
		run.setShiftsPartial(shiftsPartial);
		run.setShiftsUnfilled(shiftsUnfilled);
		run.setTotalAllocations(totalAllocations);
		run.setStatus(status);
		run.setCreatedAt(Instant.now());
		run.setCompletedAt(Instant.now());
		allocationRunRepository.save(run);
	}

	private boolean acquireAllocationLock(Long businessId) {
		Boolean result = jdbcTemplate.queryForObject(
				"SELECT pg_try_advisory_xact_lock(?)", Boolean.class, businessId);
		return Boolean.TRUE.equals(result);
	}

	private Map<String, Boolean> loadEnabledRules(Long businessId) {
		List<AllocationRule> rules = allocationRuleRepository.findByBusiness_IdOrderByPriorityAsc(businessId);
		if (rules.isEmpty()) {
			log.warn("No allocation rules found for business {} — seeding defaults", businessId);
			rules = seedDefaultRules(businessId);
		}
		Map<String, Boolean> map = new HashMap<>();
		for (AllocationRule rule : rules) {
			map.put(rule.getRuleKey(), rule.isEnabled());
		}
		return map;
	}

	private List<AllocationRule> seedDefaultRules(Long businessId) {
		Business business = businessRepository.findById(businessId).orElse(null);
		if (business == null) return List.of();
		String[][] defaults = {
			{"cheapest",           "true"},   // sorting: enabled by default
			{"expertise",          "false"},  // sorting: user enables via UI
			{"hours_available",    "false"},  // sorting: user enables via UI
			{"partial_allocation", "true"},   // feature flag: always on
			{"role_combos",        "true"},   // feature flag: always on
		};
		for (int i = 0; i < defaults.length; i++) {
			AllocationRule rule = new AllocationRule();
			rule.setBusiness(business);
			rule.setRuleKey(defaults[i][0]);
			rule.setEnabled(Boolean.parseBoolean(defaults[i][1]));
			rule.setPriority(i + 1);
			allocationRuleRepository.save(rule);
		}
		return allocationRuleRepository.findByBusiness_IdOrderByPriorityAsc(businessId);
	}

	private boolean isRuleEnabled(Map<String, Boolean> rules, String key) {
		return rules.getOrDefault(key, false); // default to disabled if not configured
	}

	private void clearAutoAllocations(Long businessId, LocalDate startDate, LocalDate endDate) {
		int deleted = timeBlockRepository.deleteAutoAllocationsInRange(businessId, startDate, endDate);
		if (deleted > 0) {
			log.info("Cleared {} auto-allocations for business {} [{} - {}]", deleted, businessId, startDate, endDate);
			// Refresh shift metrics so statuses reflect the removal
			List<Shift> affectedShifts = shiftRepository.findShiftsByStatuses(businessId, startDate, endDate,
					List.of(ShiftStatus.filled, ShiftStatus.partially_filled));
			for (Shift shift : affectedShifts) {
				shiftStatusService.updateShiftMetrics(shift.getId());
			}
		}
	}

	private Set<Long> getAllocatedStaffIds(Long shiftId, Long roleId) {
		return timeBlockRepository.findByShift_IdAndRole_Id(shiftId, roleId).stream()
				.filter(tb -> tb.getBlockType() == BlockType.work).map(TimeBlock::getStaffId).filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	/**
	 * Applies all enabled hard filter rules. Returns the staff members that pass
	 * all filters.
	 */
	private List<StaffMember> applyHardFilters(List<StaffMember> candidates, Shift shift, Business business,
			Map<String, Boolean> rules, AllocationContext ctx) {
		List<StaffMember> result = new ArrayList<>();
		for (StaffMember staff : candidates) {
			List<String> violations = collectHardViolations(staff, shift, business, rules, ctx);
			if (violations.isEmpty()) {
				result.add(staff);
			}
		}
		return result;
	}

	/**
	 * Collects all hard constraint violations for a given staff member against a
	 * shift. All hard constraints are always active (no rule toggle gating).
	 * Returns an empty list if the staff is fully eligible.
	 */
	private List<String> collectHardViolations(StaffMember staff, Shift shift, Business business,
			Map<String, Boolean> rules, AllocationContext ctx) {
		List<String> violations = new ArrayList<>();
		Long staffId = staff.getId();
		Long roleId = shift.getRole().getId();
		Long siteId = shift.getSite().getId();
		LocalDate shiftDate = shift.getShiftDate();

		// Availability: staff must not be marked unavailable
		short dow = (short) shiftDate.getDayOfWeek().getValue();
		boolean unavailable = ctx.isUnavailableOnDate(staffId, shiftDate, dow,
				AvailabilityType.unavailable);
		if (unavailable) {
			violations.add("Staff is marked unavailable on " + shiftDate);
		}

		// Site approval: staff must have an active assignment to the shift's site
		boolean hasSite = ctx.hasSiteApproval(staffId, siteId);
		if (!hasSite) {
			violations.add("Staff is not approved for site " + shift.getSite().getName());
		}

		// Role capability: staff must have an active role assignment
		Optional<StaffRole> staffRole = ctx.findActiveStaffRole(staffId, roleId);
		if (staffRole.isEmpty() && isRuleEnabled(rules, "role_combos")) {
			for (Long siblingId : ctx.getComboSiblingRoleIds(roleId, business.getId())) {
				staffRole = ctx.findActiveStaffRole(staffId, siblingId);
				if (staffRole.isPresent()) break;
			}
		}
		if (staffRole.isEmpty()) {
			violations.add("Staff does not have active role " + shift.getRole().getName());
		}

		// Shift duration fit: net work hours (shift minus breaks) must be within staff's min/max daily hours
		long shiftMinutes = computeShiftMinutes(shift);
		long netShiftMinutes = computeNetShiftMinutes(shift, staffId, business, ctx);
		double netShiftHours = netShiftMinutes / 60.0;
		double minHoursPerDay = resolveMinHoursPerDay(staffId, business, ctx);
		double maxHoursPerDay = resolveMaxHoursPerDay(staffId, business, ctx);
		if (minHoursPerDay > 0 && netShiftHours < minHoursPerDay) {
			violations.add(String.format(
				"Net shift work (%.1fh) is below staff minimum daily hours (%.1fh)",
				netShiftHours, minHoursPerDay));
		}
		if (netShiftHours > maxHoursPerDay) {
			violations.add(String.format(
				"Net shift work (%.1fh) exceeds staff maximum daily hours (%.1fh)",
				netShiftHours, maxHoursPerDay));
		}

		// Overlap: reject if staff has conflicting work blocks
		ZoneId shiftZone = resolveZone(business.getTimezone());
		Instant shiftStart = resolveInstant(shift.getStartInstant(), shiftDate, shift.getStartTime(), shiftZone);
		Instant shiftEnd = resolveInstant(shift.getEndInstant(), shiftDate, shift.getEndTime(), shiftZone);
		if (shiftStart != null && shiftEnd != null) {
			List<TimeBlock> overlapping = ctx.findOverlappingBlocksForStaff(staffId, shiftStart, shiftEnd);
			boolean hasWorkOverlap = overlapping.stream().anyMatch(tb -> tb.getBlockType() == BlockType.work);
			if (hasWorkOverlap) {
				violations.add("Staff has a conflicting shift that overlaps this time window");
			}
		}

		// One role per day: staff cannot work the same role twice on one date
		List<TimeBlock> todayBlocks = ctx.findByStaffIdAndDate(staffId, shiftDate);
		boolean alreadyAssignedThisRoleToday = todayBlocks.stream().filter(tb -> tb.getBlockType() == BlockType.work)
				.anyMatch(tb -> roleId.equals(tb.getRoleId()));
		if (alreadyAssignedThisRoleToday) {
			violations.add("Staff is already assigned to role '" + shift.getRole().getName() + "' on " + shiftDate);
		}

		// Max daily hours: existing net + proposed net must not exceed max
		int existingDailyWorkMinutes = ctx.sumWorkMinutesByStaffAndDate(staffId, shiftDate);
		long existingDailyBreakMinutes = ctx.sumBreakMinutesByStaffAndDate(staffId, shiftDate);
		long existingNetDailyMinutes = existingDailyWorkMinutes - existingDailyBreakMinutes;
		if (existingNetDailyMinutes + netShiftMinutes > maxHoursPerDay * 60) {
			violations.add("Allocation would exceed max daily hours (" + maxHoursPerDay + "h)");
		}
		if (existingNetDailyMinutes >= maxHoursPerDay * 60) {
			violations.add("Staff has no remaining hours for the day");
		}

		// Max sites per day
		int maxSitesPerDay = resolveMaxSitesPerDay(staffId, business, ctx);
		long distinctSites = countDistinctSitesOnDate(staffId, shiftDate, ctx);
		boolean alreadyAtThisSite = isStaffAtSiteOnDate(staffId, siteId, shiftDate, ctx);
		if (!alreadyAtThisSite && distinctSites >= maxSitesPerDay) {
			violations.add("Staff would exceed max sites per day (" + maxSitesPerDay + ")");
		}

		// Site continuity: staff who worked a different site yesterday may be blocked
		LocalDate yesterday = shiftDate.minusDays(1);
		ZoneId zone = resolveZone(business.getTimezone());
		Instant yStart = yesterday.atStartOfDay(zone).toInstant();
		Instant yEnd = yesterday.atTime(LocalTime.MAX).atZone(zone).toInstant();
		List<TimeBlock> yesterdayBlocks = ctx.findOverlappingBlocksForStaff(staffId, yStart, yEnd);
		boolean workedDifferentSiteYesterday = yesterdayBlocks.stream()
				.filter(tb -> tb.getBlockType() == BlockType.work)
				.anyMatch(tb -> tb.getSiteId() != null && !tb.getSiteId().equals(siteId));
		if (workedDifferentSiteYesterday) {
			int minDays = business.getMinDaysAtSiteBeforeMove() != null ? business.getMinDaysAtSiteBeforeMove() : 1;
			boolean alreadyAtSiteToday = isStaffAtSiteOnDate(staffId, siteId, shiftDate, ctx);
			if (!alreadyAtSiteToday && minDays > 1) {
				violations.add("Staff worked at a different site yesterday and must stay for " + minDays + " day(s)");
			}
		}

		// Break sufficiency
		checkSufficientBreakViolation(staffId, shift, business, violations, ctx);

		// Monthly hours cap (includes proposed shift hours)
		checkMonthlyCapViolation(staffId, shiftDate, shift, business, violations, ctx);

		// Required days off
		checkRequiredDaysOffViolation(staffId, shiftDate, business, violations, ctx);

		// Approved leave
		boolean onApprovedLeave = ctx.hasApprovedLeave(staffId, shiftDate,
				LeaveRequestStatus.approved);
		if (onApprovedLeave) {
			violations.add("Staff has approved leave on " + shiftDate);
		}

		// Weekly hours cap
		checkWeeklyCapViolation(staffId, shiftDate, shift, business, violations, ctx);

		// Max consecutive working days
		checkConsecutiveDaysViolation(staffId, shiftDate, business, violations, ctx);

		// Minimum rest period between shifts
		checkRestPeriodViolation(staffId, shift, business, violations, ctx);

		// Competency threshold
		checkCompetencyViolation(staffRole, shift, violations);

		// Mandatory leave enforcement
		checkMandatoryLeaveViolation(staff, violations);

		return violations;
	}

	private void checkMonthlyCapViolation(Long staffId, LocalDate shiftDate, Shift shift, Business business,
			List<String> violations, AllocationContext ctx) {
		LocalDate monthStart = shiftDate.withDayOfMonth(1);
		LocalDate monthEnd = shiftDate.withDayOfMonth(shiftDate.lengthOfMonth());
		List<TimeBlock> monthBlocks = ctx.findByStaffIdAndDateRange(staffId, monthStart, monthEnd);
		long existingMonthWorkMinutes = monthBlocks.stream().filter(tb -> tb.getBlockType() == BlockType.work)
				.mapToLong(tb -> {
					if (tb.getDurationMinutes() != null) {
						return tb.getDurationMinutes();
					}
					if (tb.getStartTime() != null && tb.getEndTime() != null) {
						return Duration.between(tb.getStartTime(), tb.getEndTime()).toMinutes();
					}
					return 0L;
				}).sum();
		long existingMonthBreakMinutes = monthBlocks.stream().filter(tb -> tb.getBlockType() == BlockType.break_period)
				.mapToLong(tb -> {
					if (tb.getDurationMinutes() != null) {
						return tb.getDurationMinutes();
					}
					if (tb.getStartTime() != null && tb.getEndTime() != null) {
						return Duration.between(tb.getStartTime(), tb.getEndTime()).toMinutes();
					}
					return 0L;
				}).sum();
		double existingMonthHours = (existingMonthWorkMinutes - existingMonthBreakMinutes) / 60.0;
		double proposedHours = computeNetShiftMinutes(shift, staffId, business, ctx) / 60.0;

		Optional<StaffWorkParameters> swp = ctx.findCurrentWorkParams(staffId);
		double maxMonthlyHours;
		if (swp.isPresent() && swp.get().getMaxHoursPerMonth() != null) {
			maxMonthlyHours = swp.get().getMaxHoursPerMonth().doubleValue();
		} else {
			Optional<WorkParameters> wp = ctx.findBusinessWorkParams(business.getId());
			maxMonthlyHours = wp.filter(w -> w.getMaxHoursPerMonth() != null)
					.map(w -> w.getMaxHoursPerMonth().doubleValue()).orElse(Double.MAX_VALUE);
		}

		if (maxMonthlyHours < Double.MAX_VALUE) {
			BigDecimal toleranceOver = business.getToleranceOverPercentage() != null
					? business.getToleranceOverPercentage()
					: BigDecimal.ZERO;
			double cap = maxMonthlyHours * (1 + toleranceOver.doubleValue() / 100.0);
			if ((existingMonthHours + proposedHours) > cap) {
				violations.add("Staff monthly hours (" + String.format("%.1f", existingMonthHours)
						+ "h + " + String.format("%.1f", proposedHours) + "h proposed) would exceed cap ("
						+ String.format("%.1f", cap) + "h)");
			}
		}
	}

	private void checkSufficientBreakViolation(Long staffId, Shift shift, Business business, List<String> violations,
			AllocationContext ctx) {
		long breakAfterMinutes = resolveBreakAfterMinutes(business, ctx);
		int requiredBreakMinutes = resolveRequiredBreakMinutes(Optional.empty(), business, ctx);

		if (breakAfterMinutes <= 0 || requiredBreakMinutes <= 0) {
			return; // Rule not configured
		}

		LocalDate shiftDate = shift.getShiftDate();

		// Only check if the staff has already worked enough today to trigger the rule
		int existingWorkMinutes = ctx.sumWorkMinutesByStaffAndDate(staffId, shiftDate);
		if (existingWorkMinutes < breakAfterMinutes) {
			return;
		}

		// They've crossed the threshold — verify existing break time is sufficient
		List<TimeBlock> todayBlocks = ctx.findByStaffIdAndDate(staffId, shiftDate);
		int existingBreakMinutes = todayBlocks.stream().filter(tb -> tb.getBlockType() == BlockType.break_period)
				.mapToInt(tb -> tb.getDurationMinutes() != null ? tb.getDurationMinutes() : 0).sum();

		if (existingBreakMinutes < requiredBreakMinutes) {
			violations.add(String.format(
					"Staff has worked %.1fh today but only has %d min break (%.1fh+ work requires at least %d min break)",
					existingWorkMinutes / 60.0, existingBreakMinutes, breakAfterMinutes / 60.0, requiredBreakMinutes));
		}
	}

	private void checkRequiredDaysOffViolation(Long staffId, LocalDate shiftDate, Business business,
			List<String> violations, AllocationContext ctx) {
		LocalDate weekStart = shiftDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate weekEnd = weekStart.plusDays(6); // Sunday

		int minDaysOff = resolveMinDaysOffPerWeek(staffId, business, ctx);
		int maxWorkDays = 7 - minDaysOff;

		// Count distinct worked dates across the full week (excluding today, not yet
		// assigned)
		List<TimeBlock> weekBlocks = ctx.findByStaffIdAndDateRange(staffId, weekStart, weekEnd);
		ZoneId zone = resolveZone(business.getTimezone());
		Set<LocalDate> workedDates = weekBlocks.stream().filter(tb -> tb.getBlockType() == BlockType.work)
				.map(tb -> tb.getStartTime() != null ? tb.getStartTime().atZone(zone).toLocalDate() : null)
				.filter(Objects::nonNull).filter(d -> !d.equals(shiftDate)).collect(Collectors.toSet());

		// Assigning today would bring total worked days to workedDates.size() + 1
		if (workedDates.size() + 1 > maxWorkDays) {
			int daysOffRemaining = 7 - workedDates.size() - 1;
			violations.add("Assigning this shift would leave only " + daysOffRemaining
					+ " day(s) off this week (minimum required: " + minDaysOff + ")");
		}
	}

	private void checkWeeklyCapViolation(Long staffId, LocalDate shiftDate, Shift shift,
			Business business, List<String> violations, AllocationContext ctx) {
		double maxWeeklyHours = resolveMaxHoursPerWeek(staffId, business, ctx);
		if (maxWeeklyHours <= 0) {
			return;
		}

		LocalDate weekStart = shiftDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate weekEnd = weekStart.plusDays(6);
		long existingWeekWorkMinutes = ctx.sumWorkMinutesByStaffAndWeek(staffId, weekStart, weekEnd);
		long existingWeekBreakMinutes = ctx.sumBreakMinutesByStaffAndWeek(staffId, weekStart, weekEnd);
		double existingNetWeekHours = (existingWeekWorkMinutes - existingWeekBreakMinutes) / 60.0;
		double proposedNetHours = computeNetShiftMinutes(shift, staffId, business, ctx) / 60.0;

		if (existingNetWeekHours + proposedNetHours > maxWeeklyHours) {
			violations.add(String.format(
					"Weekly hours (%.1fh + %.1fh proposed) would exceed cap (%.1fh)",
					existingNetWeekHours, proposedNetHours, maxWeeklyHours));
		}
	}

	private void checkConsecutiveDaysViolation(Long staffId, LocalDate shiftDate, Business business,
			List<String> violations, AllocationContext ctx) {
		Integer maxConsecutive = business.getMaxConsecutiveDays();
		if (maxConsecutive == null || maxConsecutive <= 0) {
			return;
		}

		int consecutiveCount = 0;
		LocalDate checkDate = shiftDate.minusDays(1);
		for (int i = 0; i < maxConsecutive; i++) {
			int workedMinutes = ctx.sumWorkMinutesByStaffAndDate(staffId, checkDate);
			if (workedMinutes > 0) {
				consecutiveCount++;
				checkDate = checkDate.minusDays(1);
			} else {
				break;
			}
		}

		if (consecutiveCount >= maxConsecutive) {
			violations.add("Staff has worked " + consecutiveCount
					+ " consecutive days (max allowed: " + maxConsecutive + ")");
		}
	}

	private void checkRestPeriodViolation(Long staffId, Shift shift, Business business,
			List<String> violations, AllocationContext ctx) {
		int minRestHours = resolveMinRestBetweenShiftsHours(business, ctx);
		if (minRestHours <= 0) {
			return;
		}

		ZoneId zone = resolveZone(business.getTimezone());
		Instant shiftStart = resolveInstant(shift.getStartInstant(), shift.getShiftDate(),
				shift.getStartTime(), zone);
		if (shiftStart == null) {
			return;
		}

		Instant lastWorkEnd = findLastWorkEndBefore(staffId, shiftStart, ctx);
		if (lastWorkEnd == null) {
			return;
		}

		long restMinutes = Duration.between(lastWorkEnd, shiftStart).toMinutes();
		long requiredRestMinutes = minRestHours * 60L;
		if (restMinutes < requiredRestMinutes) {
			violations.add(String.format(
					"Rest period (%.1fh) is below minimum required (%dh)",
					restMinutes / 60.0, minRestHours));
		}
	}

	private void checkCompetencyViolation(Optional<StaffRole> staffRole, Shift shift,
			List<String> violations) {
		if (staffRole.isEmpty()) {
			return; // Already flagged by role capability check
		}
		ProficiencyLevel required = shift.getRequiredProficiencyLevel();
		if (required == null) {
			return;
		}
		SkillLevel staffSkill = staffRole.get().getSkillLevel();
		if (staffSkill == null) {
			violations.add("Staff has no skill level assigned for role " + shift.getRole().getName());
			return;
		}
		SkillLevel requiredSkill = SkillLevel.fromProficiencyLevel(required.name());
		if (requiredSkill != null && !staffSkill.meetsOrExceeds(requiredSkill)) {
			violations.add(String.format(
					"Staff skill level (%s) does not meet required level (%s)",
					staffSkill, required));
		}
	}

	private void checkMandatoryLeaveViolation(StaffMember staff, List<String> violations) {
		if (staff.mustTakeMandatoryLeave()) {
			violations.add("Staff must take mandatory leave (worked "
					+ staff.getConsecutiveWorkDays() + " consecutive days)");
		}
	}

	/**
	 * Loads enabled sorting rule keys ordered by priority from the database.
	 */
	private List<String> loadSortingPriority(Long businessId) {
		return allocationRuleRepository.findByBusiness_IdOrderByPriorityAsc(businessId)
				.stream()
				.filter(AllocationRule::isEnabled)
				.map(AllocationRule::getRuleKey)
				.collect(Collectors.toList());
	}

	/**
	 * Sorts eligible staff using UI-configured priority rules.
	 * Sorting order is determined entirely by the database rule priority.
	 */
	private List<StaffMember> applySorting(List<StaffMember> eligible, Shift shift, Business business,
			List<String> sortingPriority, AllocationContext ctx) {
		Long roleId = shift.getRole().getId();
		LocalDate shiftDate = shift.getShiftDate();

		List<StaffMember> sorted = new ArrayList<>(eligible);
		sorted.sort((a, b) -> {
			// Apply each enabled rule in UI priority order
			for (String ruleKey : sortingPriority) {
				int cmp = compareBySortingRule(ruleKey, a, b, roleId, shiftDate, business, ctx);
				if (cmp != 0) return cmp;
			}

			// Direct role before combo-qualified (lowest priority)
			boolean aIsDirect = ctx.findActiveStaffRole(a.getId(), roleId).isPresent();
			boolean bIsDirect = ctx.findActiveStaffRole(b.getId(), roleId).isPresent();
			if (aIsDirect != bIsDirect) return aIsDirect ? -1 : 1;

			// Deterministic tiebreaker
			return Long.compare(a.getId(), b.getId());
		});
		return sorted;
	}

	private int compareBySortingRule(String ruleKey, StaffMember a, StaffMember b,
			Long roleId, LocalDate shiftDate, Business business, AllocationContext ctx) {
		switch (ruleKey) {
			case "cheapest": {
				BigDecimal rateA = ctx.findCurrentRate(a.getId(), roleId, shiftDate)
						.orElse(BigDecimal.ZERO);
				BigDecimal rateB = ctx.findCurrentRate(b.getId(), roleId, shiftDate)
						.orElse(BigDecimal.ZERO);
				return rateA.compareTo(rateB); // ASC: cheaper first
			}
			case "expertise": {
				int skillA = compareSkillLevel(a.getId(), roleId, ctx);
				int skillB = compareSkillLevel(b.getId(), roleId, ctx);
				return Integer.compare(skillB, skillA); // DESC: higher skill first
			}
			case "hours_available": {
				double remA = computeRemainingHours(a.getId(), shiftDate, business, ctx);
				double remB = computeRemainingHours(b.getId(), shiftDate, business, ctx);
				return Double.compare(remB, remA); // DESC: more remaining first
			}
			default:
				return 0;
		}
	}

	private int compareSkillLevel(Long staffId, Long roleId, AllocationContext ctx) {
		return ctx.findActiveStaffRole(staffId, roleId)
				.map(sr -> sr.getSkillLevel() != null ? sr.getSkillLevel().getNumericLevel() : 0).orElse(0);
	}

	private double computeRemainingHours(Long staffId, LocalDate date, Business business, AllocationContext ctx) {
		int existingWorkMinutes = ctx.sumWorkMinutesByStaffAndDate(staffId, date);
		long existingBreakMinutes = ctx.sumBreakMinutesByStaffAndDate(staffId, date);
		double maxHoursPerDay = resolveMaxHoursPerDay(staffId, business, ctx);
		return maxHoursPerDay - ((existingWorkMinutes - existingBreakMinutes) / 60.0);
	}

	private double computeScore(StaffMember staff, Shift shift, Business business, Map<String, Boolean> rules,
			AllocationContext ctx) {
		double score = 0.0;
		Long staffId = staff.getId();
		Long roleId = shift.getRole().getId();
		LocalDate shiftDate = shift.getShiftDate();
		List<String> sortingPriority = loadSortingPriority(business.getId());

		double weight = 1000.0;
		for (String ruleKey : sortingPriority) {
			switch (ruleKey) {
				case "expertise":
					score += compareSkillLevel(staffId, roleId, ctx) * weight;
					break;
				case "hours_available":
					score += computeRemainingHours(staffId, shiftDate, business, ctx) * weight;
					break;
				case "cheapest":
					BigDecimal rate = ctx.findCurrentRate(staffId, roleId, shiftDate)
							.orElse(BigDecimal.ZERO);
					double rateVal = rate.doubleValue();
					score += (rateVal > 0 ? (1.0 / rateVal) : 0) * weight;
					break;
			}
			weight /= 10.0;
		}

		// Direct role bonus
		boolean isDirect = ctx.findActiveStaffRole(staffId, roleId).isPresent();
		if (isDirect) {
			score += 1.0;
		}

		return score;
	}

	/**
	 * Counts how many consecutive days before the shift date the staff member has
	 * already worked the same shift type (same site + role). Used to enforce shift
	 * alternation — staff with higher consecutive counts are deprioritised.
	 */
	private int countConsecutiveSameShiftDays(Long staffId, Shift shift, AllocationContext ctx) {
		Long siteId = shift.getSite().getId();
		Long roleId = shift.getRole().getId();
		int count = 0;
		LocalDate checkDate = shift.getShiftDate().minusDays(1);
		for (int i = 0; i < 5; i++) {
			List<TimeBlock> blocks = ctx.findByStaffIdAndDate(staffId, checkDate);
			boolean workedSameShift = blocks.stream().filter(tb -> tb.getBlockType() == BlockType.work)
					.anyMatch(tb -> siteId.equals(tb.getSiteId()) && roleId.equals(tb.getRoleId()));
			if (workedSameShift) {
				count++;
				checkDate = checkDate.minusDays(1);
			} else {
				break;
			}
		}
		return count;
	}

	/**
	 * Creates a work TimeBlock for a staff member on a shift.
	 *
	 * @param blockStart explicit start of this staff member's time window
	 * @param blockEnd   explicit end of this staff member's time window
	 * @return number of blocks created (1 work block, +1 if a break was also
	 *         inserted)
	 */
	private int createWorkBlockForStaff(StaffMember staff, Shift shift, Business business, Map<String, Boolean> rules,
			String createdBy, AllocationContext ctx) {
		ZoneId zone = resolveZone(business.getTimezone());
		Instant blockStart = resolveInstant(shift.getStartInstant(), shift.getShiftDate(), shift.getStartTime(), zone);
		Instant blockEnd = resolveInstant(shift.getEndInstant(), shift.getShiftDate(), shift.getEndTime(), zone);
		return createWorkBlockForStaff(staff, shift, blockStart, blockEnd, business, rules, createdBy, ctx);
	}

	private int createWorkBlockForStaff(StaffMember staff, Shift shift, Instant blockStart, Instant blockEnd,
			Business business, Map<String, Boolean> rules, String createdBy, AllocationContext ctx) {
		TimeBlock workBlock = new TimeBlock();
		workBlock.setShift(shift);
		workBlock.setStaff(staff);
		workBlock.setRole(shift.getRole());
		workBlock.setSite(shift.getSite());
		workBlock.setStartTime(blockStart);
		workBlock.setEndTime(blockEnd);
		workBlock.setBlockType(BlockType.work);
		workBlock.setCreatedBy(createdBy);

		timeBlockRepository.save(workBlock);
		ctx.invalidateStaffMutableData(staff.getId());
		int blocksCreated = 1;

		// Rule 8 + Rule 13: Insert break if shift duration > maxContinuousWorkMinutes
		// if (isRuleEnabled(rules, "max_continuous_hours")) {
		boolean breakInserted = tryInsertBreak(staff, shift, business, rules, createdBy, ctx);
		if (breakInserted) {
			blocksCreated++;
		}
		// }

		return blocksCreated;
	}

	/**
	 * Inserts break_period TimeBlocks if shift duration exceeds maxContinuousWorkMinutes.
	 * Supports multiple breaks per shift. Returns true if at least one break was inserted.
	 */
	private boolean tryInsertBreak(StaffMember staff, Shift shift, Business business, Map<String, Boolean> rules,
			String createdBy, AllocationContext ctx) {
		long shiftMinutes = computeShiftMinutes(shift);

		Optional<StaffRole> staffRole = ctx.findActiveStaffRole(
				staff.getId(), shift.getRole().getId());
		if (staffRole.isEmpty()) {
			for (Long siblingId : ctx.getComboSiblingRoleIds(shift.getRole().getId(), business.getId())) {
				staffRole = ctx.findActiveStaffRole(staff.getId(), siblingId);
				if (staffRole.isPresent()) break;
			}
		}
		int minWorkBeforeBreak = resolveMinWorkBeforeBreakMinutes(staffRole, business, ctx);
		if (shiftMinutes < minWorkBeforeBreak) {
			return false;
		}

		int maxContinuous = resolveMaxContinuousWorkMinutes(staffRole, business, ctx);
		if (shiftMinutes <= maxContinuous) {
			return false;
		}

		int breakDuration = resolveRequiredBreakMinutes(staffRole, business, ctx);
		int maxBreaks = resolveMaxBreaksPerShift(shift.getRole(), business);

		ZoneId zone = resolveZone(business.getTimezone());
		Instant shiftStart = resolveInstant(shift.getStartInstant(), shift.getShiftDate(), shift.getStartTime(), zone);
		Instant shiftEnd = resolveInstant(shift.getEndInstant(), shift.getShiftDate(), shift.getEndTime(), zone);
		if (shiftStart == null || shiftEnd == null) {
			return false;
		}

		boolean anyInserted = false;
		int segments = maxBreaks + 1;

		for (int breakIndex = 1; breakIndex <= maxBreaks; breakIndex++) {
			long segmentPoint = (shiftMinutes * breakIndex) / segments;
			Instant targetStart = shiftStart.plus(segmentPoint - (long) breakDuration / 2, ChronoUnit.MINUTES);
			Instant targetEnd = targetStart.plus(breakDuration, ChronoUnit.MINUTES);

			Instant placed = findNonConflictingBreakSlot(
					targetStart, targetEnd, breakDuration, shiftStart, shiftEnd, shift.getRole().getId());
			if (placed == null) {
				log.warn("Could not place break #{} for staff {} on shift {}", breakIndex, staff.getId(), shift.getId());
				continue;
			}

			TimeBlock breakBlock = new TimeBlock();
			breakBlock.setShift(shift);
			breakBlock.setStaff(staff);
			breakBlock.setRole(shift.getRole());
			breakBlock.setSite(shift.getSite());
			breakBlock.setStartTime(placed);
			breakBlock.setEndTime(placed.plus(breakDuration, ChronoUnit.MINUTES));
			breakBlock.setBlockType(BlockType.break_period);
			breakBlock.setBreakSequenceNumber(breakIndex);
			breakBlock.setCreatedBy(createdBy);

			timeBlockRepository.save(breakBlock);
			ctx.invalidateStaffMutableData(staff.getId());
			anyInserted = true;
		}

		return anyInserted;
	}

	private Instant findNonConflictingBreakSlot(Instant targetStart, Instant targetEnd,
			int breakDuration, Instant shiftStart, Instant shiftEnd, Long roleId) {
		// Try multiple positions: target, then forward and backward offsets
		int maxAttempts = 6;
		for (int attempt = 0; attempt < maxAttempts; attempt++) {
			long offsetMinutes = attempt * breakDuration;
			Instant candidateStart;
			if (attempt % 2 == 0) {
				candidateStart = targetStart.plus(offsetMinutes / 2, ChronoUnit.MINUTES);
			} else {
				candidateStart = targetStart.minus((offsetMinutes + 1) / 2, ChronoUnit.MINUTES);
			}
			Instant candidateEnd = candidateStart.plus(breakDuration, ChronoUnit.MINUTES);

			// Clamp to shift bounds
			if (candidateStart.isBefore(shiftStart)) {
				candidateStart = shiftStart;
				candidateEnd = candidateStart.plus(breakDuration, ChronoUnit.MINUTES);
			}
			if (candidateEnd.isAfter(shiftEnd)) {
				candidateEnd = shiftEnd;
				candidateStart = candidateEnd.minus(breakDuration, ChronoUnit.MINUTES);
			}
			if (candidateStart.isBefore(shiftStart)) {
				continue; // Break doesn't fit in shift
			}

			List<TimeBlock> conflicts = timeBlockRepository.findByRoleAndTimeRangeAndBlockType(
					roleId, candidateStart, candidateEnd, BlockType.break_period);
			if (conflicts.isEmpty()) {
				return candidateStart;
			}
		}
		return null;
	}

	private long computeShiftMinutes(Shift shift) {
		if (shift.getStartInstant() != null && shift.getEndInstant() != null) {
			return Duration.between(shift.getStartInstant(), shift.getEndInstant()).toMinutes();
		}
		if (shift.getStartTime() != null && shift.getEndTime() != null) {
			long minutes = Duration.between(shift.getStartTime(), shift.getEndTime()).toMinutes();
			if (minutes < 0) {
				minutes += 24 * 60; // overnight shift
			}
			return minutes;
		}
		return 0L;
	}

	/**
	 * Computes the total break minutes that would be inserted for a shift,
	 * mirroring the logic in tryInsertBreak without actually inserting.
	 */
	private long computeBreakMinutesForShift(Shift shift, Long staffId, Business business, AllocationContext ctx) {
		long shiftMinutes = computeShiftMinutes(shift);
		Optional<StaffRole> staffRole = ctx.findActiveStaffRole(staffId, shift.getRole().getId());
		if (staffRole.isEmpty()) {
			for (Long siblingId : ctx.getComboSiblingRoleIds(shift.getRole().getId(), business.getId())) {
				staffRole = ctx.findActiveStaffRole(staffId, siblingId);
				if (staffRole.isPresent()) break;
			}
		}
		int minWorkBeforeBreak = resolveMinWorkBeforeBreakMinutes(staffRole, business, ctx);
		if (shiftMinutes < minWorkBeforeBreak) return 0;
		int maxContinuous = resolveMaxContinuousWorkMinutes(staffRole, business, ctx);
		if (shiftMinutes <= maxContinuous) return 0;
		int breakDuration = resolveRequiredBreakMinutes(staffRole, business, ctx);
		int maxBreaks = resolveMaxBreaksPerShift(shift.getRole(), business);
		return (long) breakDuration * maxBreaks;
	}

	/**
	 * Returns shift duration minus break time — the actual work minutes
	 * that count toward daily/weekly/monthly caps.
	 */
	private long computeNetShiftMinutes(Shift shift, Long staffId, Business business, AllocationContext ctx) {
		return computeShiftMinutes(shift) - computeBreakMinutesForShift(shift, staffId, business, ctx);
	}

	private double resolveMaxHoursPerDay(Long staffId, Business business, AllocationContext ctx) {
		Optional<StaffWorkParameters> swp = ctx.findCurrentWorkParams(staffId);
		if (swp.isPresent() && swp.get().getMaxHoursPerDay() != null) {
			return swp.get().getMaxHoursPerDay().doubleValue();
		}
		// Fall back to business cap
		double cap = business.getMaxDailyHoursCap() != null ? business.getMaxDailyHoursCap().doubleValue() : 12.0;

		// Also check work parameters defaults
		Optional<WorkParameters> wp = ctx.findBusinessWorkParams(business.getId());
		if (wp.isPresent() && wp.get().getMaxHoursPerDay() != null) {
			return wp.get().getMaxHoursPerDay().doubleValue();
		}
		return cap;
	}

	private double resolveMinHoursPerDay(Long staffId, Business business, AllocationContext ctx) {
		Optional<StaffWorkParameters> swp = ctx.findCurrentWorkParams(staffId);
		if (swp.isPresent() && swp.get().getMinHoursPerDay() != null) {
			return swp.get().getMinHoursPerDay().doubleValue();
		}
		Optional<WorkParameters> wp = ctx.findBusinessWorkParams(business.getId());
		if (wp.isPresent() && wp.get().getMinHoursPerDay() != null) {
			return wp.get().getMinHoursPerDay().doubleValue();
		}
		return 0.0; // No minimum configured — any shift length is acceptable
	}

	private int resolveMaxSitesPerDay(Long staffId, Business business, AllocationContext ctx) {
		Optional<StaffWorkParameters> swp = ctx.findCurrentWorkParams(staffId);
		if (swp.isPresent() && swp.get().getMaxSitesPerDay() != null) {
			return swp.get().getMaxSitesPerDay();
		}
		return 1; // Default: one site per day
	}

	private int resolveMinDaysOffPerWeek(Long staffId, Business business, AllocationContext ctx) {
		Optional<StaffWorkParameters> swp = ctx.findCurrentWorkParams(staffId);
		if (swp.isPresent() && swp.get().getMinDaysOffPerWeek() != null) {
			return swp.get().getMinDaysOffPerWeek();
		}
		Optional<WorkParameters> wp = ctx.findBusinessWorkParams(business.getId());
		if (wp.isPresent() && wp.get().getMinDaysOffPerWeek() != null) {
			return wp.get().getMinDaysOffPerWeek();
		}
		return 1; // Default: at least 1 day off per week
	}

	private double resolveMaxHoursPerWeek(Long staffId, Business business, AllocationContext ctx) {
		Optional<StaffWorkParameters> swp = ctx.findCurrentWorkParams(staffId);
		if (swp.isPresent() && swp.get().getMaxHoursPerWeek() != null) {
			return swp.get().getMaxHoursPerWeek().doubleValue();
		}
		Optional<WorkParameters> wp = ctx.findBusinessWorkParams(business.getId());
		if (wp.isPresent() && wp.get().getMaxHoursPerWeek() != null) {
			return wp.get().getMaxHoursPerWeek().doubleValue();
		}
		if (business.getMaxWeeklyHoursCap() != null) {
			return business.getMaxWeeklyHoursCap().doubleValue();
		}
		return 0.0; // Not configured — no weekly cap
	}

	private int resolveMinRestBetweenShiftsHours(Business business, AllocationContext ctx) {
		Optional<WorkParameters> wp = ctx.findBusinessWorkParams(business.getId());
		if (wp.isPresent() && wp.get().getMinRestBetweenShiftsHours() != null) {
			return wp.get().getMinRestBetweenShiftsHours().intValue();
		}
		return business.getMinRestBetweenShiftsHours() != null
				? business.getMinRestBetweenShiftsHours() : 0;
	}

	private Instant findLastWorkEndBefore(Long staffId, Instant beforeInstant, AllocationContext ctx) {
		List<TimeBlock> recentBlocks = ctx.findByStaffIdAndTimeRange(
				staffId, beforeInstant.minus(48, ChronoUnit.HOURS), beforeInstant);
		return recentBlocks.stream()
				.filter(tb -> tb.getBlockType() == BlockType.work)
				.filter(tb -> tb.getEndTime() != null)
				.map(TimeBlock::getEndTime)
				.max(Instant::compareTo)
				.orElse(null);
	}

	private double resolveMonthlyTarget(Long staffId, Business business, AllocationContext ctx) {
		Optional<StaffWorkParameters> swp = ctx.findCurrentWorkParams(staffId);
		if (swp.isPresent() && swp.get().getMaxHoursPerMonth() != null) {
			return swp.get().getMaxHoursPerMonth().doubleValue();
		}
		Optional<WorkParameters> wp = ctx.findBusinessWorkParams(business.getId());
		if (wp.isPresent() && wp.get().getMaxHoursPerMonth() != null) {
			return wp.get().getMaxHoursPerMonth().doubleValue();
		}
		return 0.0;
	}

	/**
	 * Returns the work duration (in minutes) after which a break is required. Reads
	 * from WorkParameters.breakAfterHours first, falls back to
	 * business.maxContinuousWorkMinutes.
	 */
	private long resolveBreakAfterMinutes(Business business, AllocationContext ctx) {
		Optional<WorkParameters> wp = ctx.findBusinessWorkParams(business.getId());
		if (wp.isPresent() && wp.get().getBreakAfterHours() != null) {
			return (long) (wp.get().getBreakAfterHours().doubleValue() * 60);
		}
		return business.getMaxContinuousWorkMinutes() != null ? business.getMaxContinuousWorkMinutes() : 300;
	}

	private int resolveRequiredBreakMinutes(Optional<StaffRole> staffRole, Business business, AllocationContext ctx) {
		if (staffRole.isPresent() && staffRole.get().getMinBreakMinutes() != null) {
			return staffRole.get().getMinBreakMinutes();
		}
		Optional<WorkParameters> wp = ctx.findBusinessWorkParams(business.getId());
		if (wp.isPresent() && wp.get().getMinBreakDurationMinutes() != null) {
			return wp.get().getMinBreakDurationMinutes();
		}
		return business.getMinBreakDurationMinutes() != null ? business.getMinBreakDurationMinutes() : 30;
	}

	private int resolveMaxContinuousWorkMinutes(Optional<StaffRole> staffRole, Business business, AllocationContext ctx) {
		if (staffRole.isPresent() && staffRole.get().getMaxContinuousWorkMinutes() != null) {
			return staffRole.get().getMaxContinuousWorkMinutes();
		}
		return business.getMaxContinuousWorkMinutes() != null ? business.getMaxContinuousWorkMinutes() : 300;
	}

	private int resolveMinWorkBeforeBreakMinutes(Optional<StaffRole> staffRole, Business business, AllocationContext ctx) {
		if (staffRole.isPresent() && staffRole.get().getMinWorkMinutesBeforeBreak() != null) {
			return staffRole.get().getMinWorkMinutesBeforeBreak();
		}
		return business.getMinWorkBeforeBreakMinutes() != null ? business.getMinWorkBeforeBreakMinutes() : 180;
	}

	private int resolveMaxBreaksPerShift(Role role, Business business) {
		if (role.getMaxBreaksPerShift() != null) {
			return role.getMaxBreaksPerShift();
		}
		return business.getDefaultMaxBreaksPerShift() != null ? business.getDefaultMaxBreaksPerShift() : 2;
	}

	private long countDistinctSitesOnDate(Long staffId, LocalDate date, AllocationContext ctx) {
		List<TimeBlock> blocks = ctx.findByStaffIdAndDate(staffId, date);
		return blocks.stream().filter(tb -> tb.getBlockType() == BlockType.work).map(TimeBlock::getSiteId)
				.filter(Objects::nonNull).distinct().count();
	}

	private boolean isStaffAtSiteOnDate(Long staffId, Long siteId, LocalDate date, AllocationContext ctx) {
		List<TimeBlock> blocks = ctx.findByStaffIdAndDate(staffId, date);
		return blocks.stream().filter(tb -> tb.getBlockType() == BlockType.work)
				.anyMatch(tb -> siteId.equals(tb.getSiteId()));
	}

	private ZoneId resolveZone(String timezone) {
		if (timezone == null || timezone.isBlank()) {
			log.warn("No timezone configured, defaulting to UTC");
			return ZoneOffset.UTC;
		}
		try {
			return ZoneId.of(timezone);
		} catch (DateTimeException e) {
			log.warn("Invalid timezone '{}', defaulting to UTC", timezone);
			return ZoneOffset.UTC;
		}
	}

	private Instant resolveInstant(Instant stored, LocalDate date, LocalTime time, ZoneId zone) {
		if (stored != null) {
			return stored;
		}
		if (date != null && time != null) {
			return date.atTime(time).atZone(zone).toInstant();
		}
		return null;
	}

	// -------------------------------------------------------------------------
	// Partial Allocation
	// -------------------------------------------------------------------------

	/**
	 * Finds the start of the uncovered time window for a shift.
	 * Returns the latest work block end time, or shiftStart if no blocks exist.
	 */
	private Instant computeUncoveredStart(Shift shift, Instant shiftStart) {
		List<TimeBlock> blocks = timeBlockRepository.findByShift_Id(shift.getId());
		return blocks.stream()
				.filter(tb -> tb.getBlockType() == BlockType.work)
				.map(TimeBlock::getEndTime)
				.filter(Objects::nonNull)
				.max(Instant::compareTo)
				.orElse(shiftStart);
	}

	/**
	 * Computes how many minutes a staff member can work, given their remaining
	 * daily/weekly/monthly capacity and the uncovered gap size.
	 * Returns 0 if no capacity remains.
	 */
	private long computeAvailableMinutes(StaffMember staff, Shift shift, long uncoveredMinutes, Business business,
			AllocationContext ctx) {
		Long staffId = staff.getId();
		LocalDate shiftDate = shift.getShiftDate();

		// Daily remaining capacity
		int existingDailyWorkMinutes = ctx.sumWorkMinutesByStaffAndDate(staffId, shiftDate);
		long existingDailyBreakMinutes = ctx.sumBreakMinutesByStaffAndDate(staffId, shiftDate);
		long existingNetDailyMinutes = existingDailyWorkMinutes - existingDailyBreakMinutes;
		double maxDailyMinutes = resolveMaxHoursPerDay(staffId, business, ctx) * 60;
		long dailyRemaining = (long) (maxDailyMinutes - existingNetDailyMinutes);

		long available = Math.min(uncoveredMinutes, dailyRemaining);

		// Weekly remaining capacity
		double maxWeeklyHours = resolveMaxHoursPerWeek(staffId, business, ctx);
		if (maxWeeklyHours > 0) {
			LocalDate weekStart = shiftDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
			LocalDate weekEnd = weekStart.plusDays(6);
			long weekWork = ctx.sumWorkMinutesByStaffAndWeek(staffId, weekStart, weekEnd);
			long weekBreak = ctx.sumBreakMinutesByStaffAndWeek(staffId, weekStart, weekEnd);
			long weeklyRemaining = (long) (maxWeeklyHours * 60) - (weekWork - weekBreak);
			available = Math.min(available, weeklyRemaining);
		}

		// Monthly remaining capacity
		double maxMonthlyHours = resolveMonthlyTarget(staffId, business, ctx);
		if (maxMonthlyHours > 0) {
			LocalDate monthStart = shiftDate.withDayOfMonth(1);
			LocalDate monthEnd = shiftDate.withDayOfMonth(shiftDate.lengthOfMonth());
			List<TimeBlock> monthBlocks = ctx.findByStaffIdAndDateRange(staffId, monthStart, monthEnd);
			long monthWork = monthBlocks.stream()
					.filter(tb -> tb.getBlockType() == BlockType.work)
					.mapToLong(tb -> {
						if (tb.getDurationMinutes() != null) return tb.getDurationMinutes();
						if (tb.getStartTime() != null && tb.getEndTime() != null)
							return Duration.between(tb.getStartTime(), tb.getEndTime()).toMinutes();
						return 0L;
					}).sum();
			long monthBreak = monthBlocks.stream()
					.filter(tb -> tb.getBlockType() == BlockType.break_period)
					.mapToLong(tb -> {
						if (tb.getDurationMinutes() != null) return tb.getDurationMinutes();
						if (tb.getStartTime() != null && tb.getEndTime() != null)
							return Duration.between(tb.getStartTime(), tb.getEndTime()).toMinutes();
						return 0L;
					}).sum();
			BigDecimal toleranceOver = business.getToleranceOverPercentage() != null
					? business.getToleranceOverPercentage() : BigDecimal.ZERO;
			double cap = maxMonthlyHours * (1 + toleranceOver.doubleValue() / 100.0);
			long monthlyRemaining = (long) (cap * 60) - (monthWork - monthBreak);
			available = Math.min(available, monthlyRemaining);
		}

		return Math.max(available, 0);
	}

	/**
	 * Checks all non-duration hard constraints for partial allocation.
	 * Excludes shift duration fit, cumulative daily/weekly/monthly caps,
	 * and break sufficiency — those are handled by computeAvailableMinutes.
	 * Uses the uncovered time window for overlap and rest period checks.
	 */
	private List<String> collectNonDurationViolations(StaffMember staff, Shift shift, Business business,
			Map<String, Boolean> rules, Instant uncoveredStart, Instant uncoveredEnd, AllocationContext ctx) {
		List<String> violations = new ArrayList<>();
		Long staffId = staff.getId();
		Long roleId = shift.getRole().getId();
		Long siteId = shift.getSite().getId();
		LocalDate shiftDate = shift.getShiftDate();

		// Availability
		short dow = (short) shiftDate.getDayOfWeek().getValue();
		boolean unavailable = ctx.isUnavailableOnDate(staffId, shiftDate, dow,
				AvailabilityType.unavailable);
		if (unavailable) {
			violations.add("Staff is marked unavailable on " + shiftDate);
		}

		// Site approval
		boolean hasSite = ctx.hasSiteApproval(staffId, siteId);
		if (!hasSite) {
			violations.add("Staff is not approved for site " + shift.getSite().getName());
		}

		// Role capability
		Optional<StaffRole> staffRole = ctx.findActiveStaffRole(staffId, roleId);
		if (staffRole.isEmpty() && isRuleEnabled(rules, "role_combos")) {
			for (Long siblingId : ctx.getComboSiblingRoleIds(roleId, business.getId())) {
				staffRole = ctx.findActiveStaffRole(staffId, siblingId);
				if (staffRole.isPresent()) break;
			}
		}
		if (staffRole.isEmpty()) {
			violations.add("Staff does not have active role " + shift.getRole().getName());
		}

		// Overlap check using uncovered window
		List<TimeBlock> overlapping = ctx.findOverlappingBlocksForStaff(staffId,
				uncoveredStart, uncoveredEnd);
		boolean hasWorkOverlap = overlapping.stream().anyMatch(tb -> tb.getBlockType() == BlockType.work);
		if (hasWorkOverlap) {
			violations.add("Staff has a conflicting shift that overlaps this time window");
		}

		// One role per day
		List<TimeBlock> todayBlocks = ctx.findByStaffIdAndDate(staffId, shiftDate);
		boolean alreadyAssignedThisRoleToday = todayBlocks.stream()
				.filter(tb -> tb.getBlockType() == BlockType.work)
				.anyMatch(tb -> roleId.equals(tb.getRoleId()));
		if (alreadyAssignedThisRoleToday) {
			violations.add("Staff is already assigned to role '" + shift.getRole().getName() + "' on " + shiftDate);
		}

		// Max sites per day
		int maxSitesPerDay = resolveMaxSitesPerDay(staffId, business, ctx);
		long distinctSites = countDistinctSitesOnDate(staffId, shiftDate, ctx);
		boolean alreadyAtThisSite = isStaffAtSiteOnDate(staffId, siteId, shiftDate, ctx);
		if (!alreadyAtThisSite && distinctSites >= maxSitesPerDay) {
			violations.add("Staff would exceed max sites per day (" + maxSitesPerDay + ")");
		}

		// Site continuity
		LocalDate yesterday = shiftDate.minusDays(1);
		ZoneId zone = resolveZone(business.getTimezone());
		Instant yStart = yesterday.atStartOfDay(zone).toInstant();
		Instant yEnd = yesterday.atTime(LocalTime.MAX).atZone(zone).toInstant();
		List<TimeBlock> yesterdayBlocks = ctx.findOverlappingBlocksForStaff(staffId, yStart, yEnd);
		boolean workedDifferentSiteYesterday = yesterdayBlocks.stream()
				.filter(tb -> tb.getBlockType() == BlockType.work)
				.anyMatch(tb -> tb.getSiteId() != null && !tb.getSiteId().equals(siteId));
		if (workedDifferentSiteYesterday) {
			int minDays = business.getMinDaysAtSiteBeforeMove() != null ? business.getMinDaysAtSiteBeforeMove() : 1;
			boolean alreadyAtSiteToday = isStaffAtSiteOnDate(staffId, siteId, shiftDate, ctx);
			if (!alreadyAtSiteToday && minDays > 1) {
				violations.add("Staff worked at a different site yesterday and must stay for " + minDays + " day(s)");
			}
		}

		// Required days off
		checkRequiredDaysOffViolation(staffId, shiftDate, business, violations, ctx);

		// Approved leave
		boolean onApprovedLeave = ctx.hasApprovedLeave(staffId, shiftDate,
				LeaveRequestStatus.approved);
		if (onApprovedLeave) {
			violations.add("Staff has approved leave on " + shiftDate);
		}

		// Max consecutive working days
		checkConsecutiveDaysViolation(staffId, shiftDate, business, violations, ctx);

		// Rest period using uncoveredStart
		int minRestHours = resolveMinRestBetweenShiftsHours(business, ctx);
		if (minRestHours > 0) {
			Instant lastWorkEnd = findLastWorkEndBefore(staffId, uncoveredStart, ctx);
			if (lastWorkEnd != null) {
				long restMinutes = Duration.between(lastWorkEnd, uncoveredStart).toMinutes();
				long requiredRestMinutes = minRestHours * 60L;
				if (restMinutes < requiredRestMinutes) {
					violations.add(String.format("Rest period (%.1fh) is below minimum required (%dh)",
							restMinutes / 60.0, minRestHours));
				}
			}
		}

		// Competency
		checkCompetencyViolation(staffRole, shift, violations);

		// Mandatory leave
		checkMandatoryLeaveViolation(staff, violations);

		return violations;
	}

	/**
	 * Fills remaining unfilled slots on a shift using partial-duration staff assignments.
	 * Uses left-to-right fill: each partial worker covers as much of the uncovered
	 * time window as their capacity allows (minimum 60 minutes).
	 * No breaks are inserted for partial blocks.
	 *
	 * @return total number of partial allocations created
	 */
	private int allocatePartialForShift(Shift shift, List<StaffMember> allStaff,
			Instant shiftStart, Instant shiftEnd, Business business,
			Map<String, Boolean> enabledRules, List<String> sortingPriority,
			Set<Long> usedStaffIds, int remainingSlots, AllocationContext ctx) {

		int totalPartials = 0;

		for (int slot = 0; slot < remainingSlots; slot++) {
			Instant uncoveredStart = shiftStart;

			while (true) {
				long uncoveredMinutes = Duration.between(uncoveredStart, shiftEnd).toMinutes();
				if (uncoveredMinutes < 60) break;

				final Instant candidateStart = uncoveredStart;
				List<StaffMember> partialCandidates = allStaff.stream()
						.filter(s -> !usedStaffIds.contains(s.getId()))
						.filter(s -> collectNonDurationViolations(s, shift, business, enabledRules,
								candidateStart, shiftEnd, ctx).isEmpty())
						.collect(Collectors.toList());

				partialCandidates = applySorting(partialCandidates, shift, business, sortingPriority, ctx);

				StaffMember chosen = null;
				long chosenMinutes = 0;
				for (StaffMember candidate : partialCandidates) {
					long available = computeAvailableMinutes(candidate, shift, uncoveredMinutes, business, ctx);
					if (available >= 60) {
						chosen = candidate;
						chosenMinutes = available;
						break;
					}
				}

				if (chosen == null) break;

				Instant partialEnd = uncoveredStart.plus(chosenMinutes, ChronoUnit.MINUTES);
				if (partialEnd.isAfter(shiftEnd)) {
					partialEnd = shiftEnd;
				}

				// Create partial work block — no breaks for partial allocations
				TimeBlock workBlock = new TimeBlock();
				workBlock.setShift(shift);
				workBlock.setStaff(chosen);
				workBlock.setRole(shift.getRole());
				workBlock.setSite(shift.getSite());
				workBlock.setStartTime(uncoveredStart);
				workBlock.setEndTime(partialEnd);
				workBlock.setBlockType(BlockType.work);
				workBlock.setCreatedBy("PARTIAL_AUTO");
				timeBlockRepository.save(workBlock);
				ctx.invalidateStaffMutableData(chosen.getId());

				log.debug("Partial allocation: staff {} covers shift {} from {} to {}",
						chosen.getId(), shift.getId(), uncoveredStart, partialEnd);

				usedStaffIds.add(chosen.getId());
				totalPartials++;
				uncoveredStart = partialEnd;
			}
		}

		if (totalPartials > 0) {
			log.info("Partial allocation: {} partial blocks created for shift {}", totalPartials, shift.getId());
		}

		return totalPartials;
	}
}
