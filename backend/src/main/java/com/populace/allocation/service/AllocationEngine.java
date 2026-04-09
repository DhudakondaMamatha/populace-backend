package com.populace.allocation.service;

import com.populace.allocation.dto.*;
import com.populace.domain.*;
import com.populace.domain.enums.AvailabilityType;
import com.populace.domain.enums.BlockType;
import com.populace.domain.enums.EmploymentStatus;
import com.populace.domain.enums.LeaveRequestStatus;
import com.populace.domain.enums.ShiftStatus;
import com.populace.domain.enums.SkillLevel;
import com.populace.repository.*;
import com.populace.shiftstatus.service.ShiftStatusService;

import org.hibernate.query.sqm.TemporalUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	public AllocationEngine(ShiftRepository shiftRepository, TimeBlockRepository timeBlockRepository,
			StaffMemberRepository staffMemberRepository, StaffRoleRepository staffRoleRepository,
			StaffSiteRepository staffSiteRepository, StaffAvailabilityRepository staffAvailabilityRepository,
			StaffCompensationRepository staffCompensationRepository,
			StaffWorkParametersRepository staffWorkParametersRepository,
			WorkParametersRepository workParametersRepository, AllocationRuleRepository allocationRuleRepository,
			BusinessRepository businessRepository, ShiftStatusService shiftStatusService, RoleRepository roleRepository,
			RoleComboRoleRepository roleComboRoleRepository, LeaveRequestRepository leaveRequestRepository) {
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
	}

	// -------------------------------------------------------------------------
	// Public API
	// -------------------------------------------------------------------------

	@Transactional
	public AllocationResultDto allocate(Long businessId, LocalDate startDate, LocalDate endDate,
			boolean clearAutoFirst) {
		String runId = UUID.randomUUID().toString();
		log.info("Starting allocation run {} for business {} [{} - {}]", runId, businessId, startDate, endDate);

		Business business = businessRepository.findById(businessId)
				.orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));

		// Load enabled rules
		Map<String, Boolean> enabledRules = loadEnabledRules(businessId);

		// Optionally clear auto-allocations first
		if (clearAutoFirst) {
			clearAutoAllocations(businessId, startDate, endDate);
		}

		List<Shift> shifts = shiftRepository.findUnfilledShifts(businessId, startDate, endDate);
		// List<Role> roles =
		// roleRepository.findByBusiness_IdAndDeletedAtIsNull(businessId);
		int blocksCreated = 0;
		int totalAllocations = 0;

		for (Shift shift : shifts) {
			if (shift.getStatus() == ShiftStatus.cancelled) {
				continue;
			}
			System.out.println(shift.getRole().getName());
			List<StaffMember> allStaff = new ArrayList<>(staffRoleRepository.findByBussinessIdAndRoldId(businessId,
					shift.getRole().getId(), EmploymentStatus.active));

			// Expand candidates via role combos (staff with sibling roles in the same
			// combo)
			if (isRuleEnabled(enabledRules, "role_combos")) {
				Set<Long> seenIds = allStaff.stream().map(StaffMember::getId).collect(Collectors.toSet());
				for (Long siblingRoleId : getComboSiblingRoleIds(shift.getRole().getId(), businessId)) {
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
				continue;
			}

			// Coverage cursor: fill shift start→end, handing off when a staff hits their
			// max hours
			Instant cursor = shiftStart;
			List<StaffMember> eligible = allStaff.stream()
					.filter(s -> collectHardViolations(s, shift, business, enabledRules).isEmpty())
					.collect(Collectors.toList());

			eligible = applySorting(eligible, shift, business, enabledRules);
			if(eligible.size()>shift.getStaffRequired()) {
				eligible = eligible.subList(0, shift.getStaffRequired());
			}
			if (eligible.isEmpty()) {
				break; // no eligible staff to cover remaining window
			}

			Set<Long> usedInShift = new HashSet<>(getAllocatedStaffIds(shift.getId(), shift.getRole().getId()));
			List<StaffMember> candidates = eligible.stream().filter(s -> !usedInShift.contains(s.getId()))
					.collect(Collectors.toList());

			// List<Long> staffIds = allStaff.stream().map(StaffMember::getId).toList();
			// Object[] totals =
			// staffWorkParametersRepository.sumHoursByRoleAndDate(staffIds,
			// shift.getRole().getId(), shift.getShiftDate());
			// BigDecimal minHours = ((BigDecimal) totals[0])/staffIds.size();
			// BigDecimal maxHours = ((BigDecimal) totals[1])/staffIds.size();

			// while (cursor.isBefore(shiftEnd)) {
			for (StaffMember staff : candidates) {
				// Candidates: same role, not already used in this shift

				Instant winStart = cursor;
				// Hard-filter against the remaining window

				// How much of the shift can this staff cover today?
				double minStaffHours = resolveMinHoursPerDay(staff.getId(), business);

				int alreadyWorkedMinutes = timeBlockRepository.sumWorkMinutesByStaffAndDate(staff.getId(),
						shift.getShiftDate());
				long remainingStaffMinutes = Math.max(0, (long) (minStaffHours * 60) - alreadyWorkedMinutes);
				long minStaffMinutes = Math.max(0, (long) (minStaffHours * 60));

				if (remainingStaffMinutes < minStaffMinutes) {
					Optional<StaffWorkParameters> swp = staffWorkParametersRepository
							.findCurrentByStaffId(staff.getId());
					if (swp.isPresent()) {
						winStart = shiftEnd.minus(swp.get().getMinHoursPerDay().longValue(), ChronoUnit.HOURS);
					}
					remainingStaffMinutes = swp.get().getMinHoursPerDay().longValue() * 60;

					// usedInShift.add(staff.getId());
					// continue;
				}

				Instant winEndCalc = winStart.plus(remainingStaffMinutes, ChronoUnit.MINUTES);
				final Instant winEnd = winEndCalc.isAfter(shiftEnd) ? shiftEnd : winEndCalc;

				createWorkBlockForStaff(staff, shift, winStart, winEnd, business, enabledRules, "AUTO");
				usedInShift.add(staff.getId());
				blocksCreated++;
				totalAllocations++;

				cursor = winEnd;
				if (blocksCreated == candidates.size()) {
					cursor = shiftEnd;
				}
			}
			// }

			shiftStatusService.updateShiftMetrics(shift.getId());
		}

		// Top-up pass: staff who haven't met minimum daily hours get allocated to other
		// approved sites
		topUpMinimumHours(businessId, startDate, endDate, business, enabledRules);

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

		log.info("Allocation run {} complete. Blocks created: {}, Total allocations: {}", runId, blocksCreated,
				totalAllocations);

		return new AllocationResultDto(runId, totalShifts, shiftsFilled, shiftsPartial, shiftsUnfilled, blocksCreated,
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
			List<String> violations = collectHardViolations(staff, shift, business, enabledRules);
			if (violations.isEmpty()) {
				double score = computeScore(staff, shift, business, enabledRules);
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

		Map<String, Boolean> enabledRules = loadEnabledRules(businessId);

		if (!req.overrideViolations()) {
			List<String> violations = collectHardViolations(staff, shift, business, enabledRules);
			if (!violations.isEmpty()) {
				return new ManualAllocationResponse(false, null, shiftId, req.staffId(), staff.getFullName(), null,
						null, false);
			}
		}

		// Create work block covering the full shift window
		ZoneId zone = resolveZone(business.getTimezone());
		Instant blockStart = resolveInstant(shift.getStartInstant(), shift.getShiftDate(), shift.getStartTime(), zone);
		Instant blockEnd = resolveInstant(shift.getEndInstant(), shift.getShiftDate(), shift.getEndTime(), zone);

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

		// Insert break if needed (Rule 8)
		Map<String, Boolean> rules = enabledRules;
		if (rules.getOrDefault("max_continuous_hours", true)) {
			tryInsertBreak(staff, shift, business, rules, "MANUAL");
		}

		// Update shift metrics
		shiftStatusService.updateShiftMetrics(shiftId);

		// Fetch hourly rate
		BigDecimal hourlyRate = staffCompensationRepository
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

	@Transactional
	public ReallocateShiftResponse reallocateShift(Long businessId, Long shiftId) {
		Shift shift = shiftRepository.findByIdAndBusiness_Id(shiftId, businessId)
				.orElseThrow(() -> new IllegalArgumentException("Shift not found: " + shiftId));

		Business business = businessRepository.findById(businessId)
				.orElseThrow(() -> new IllegalArgumentException("Business not found: " + businessId));

		List<TimeBlock> existingBlocks = timeBlockRepository.findByShift_Id(shiftId);

		// Separate manual vs auto blocks
		List<TimeBlock> autoBlocks = existingBlocks.stream().filter(
				tb -> !MANUAL_CREATED_BY.contains(tb.getCreatedBy() != null ? tb.getCreatedBy().toUpperCase() : ""))
				.collect(Collectors.toList());

		List<TimeBlock> manualBlocks = existingBlocks.stream().filter(
				tb -> MANUAL_CREATED_BY.contains(tb.getCreatedBy() != null ? tb.getCreatedBy().toUpperCase() : ""))
				.filter(tb -> tb.getBlockType() == BlockType.work).collect(Collectors.toList());

		int removedAuto = autoBlocks.size();
		timeBlockRepository.deleteAll(autoBlocks);

		// Reset staffAllocated to count of remaining manual work blocks
		int manualCount = manualBlocks.size();
		shift.setStaffAllocated(manualCount);
		shiftRepository.save(shift);

		// Re-run allocation for this single shift
		Map<String, Boolean> enabledRules = loadEnabledRules(businessId);
		List<StaffMember> allStaff = staffMemberRepository.findActiveStaff(businessId);
		Set<Long> manualStaffIds = manualBlocks.stream().map(TimeBlock::getStaffId).filter(Objects::nonNull)
				.collect(Collectors.toSet());

		List<StaffMember> candidates = allStaff.stream().filter(s -> !manualStaffIds.contains(s.getId()))
				.collect(Collectors.toList());

		List<StaffMember> eligible = applyHardFilters(candidates, shift, business, enabledRules);
		eligible = applySorting(eligible, shift, business, enabledRules);

		int slotsNeeded = shift.getRemainingSlots();
		ZoneId rzone = resolveZone(business.getTimezone());
		Instant rShiftStart = resolveInstant(shift.getStartInstant(), shift.getShiftDate(), shift.getStartTime(),
				rzone);
		Instant rShiftEnd = resolveInstant(shift.getEndInstant(), shift.getShiftDate(), shift.getEndTime(), rzone);

		// Cursor starts after any existing manual blocks that already occupy the window
		Instant rCursor = manualBlocks.stream().map(TimeBlock::getEndTime).filter(Objects::nonNull)
				.max(Comparator.naturalOrder()).orElse(rShiftStart);

		int newAutoAllocations = 0;
		for (StaffMember staff : eligible) {
//			if (newAutoAllocations >= slotsNeeded) break;
//			if (!rCursor.isBefore(rShiftEnd)) break;
//
//			double minHours  = resolveMinHoursPerDay(staff.getId(), business);
//			double maxHours  = resolveMaxHoursPerDay(staff.getId(), business);
//			long slotMinutes = minHours > 0 ? (long)(minHours * 60) : (long)(maxHours * 60);
//			if (slotMinutes <= 0) slotMinutes = Duration.between(rShiftStart, rShiftEnd).toMinutes();
//
//			Instant slotEnd = rCursor.plus(slotMinutes, ChronoUnit.MINUTES);
//			boolean isLast = (newAutoAllocations == slotsNeeded - 1) || slotEnd.isAfter(rShiftEnd);
//			if (isLast) slotEnd = rShiftEnd;

			int created = createWorkBlockForStaff(staff, shift, business, enabledRules, "AUTO");
//			if (created > 0) {
//				rCursor = slotEnd;
//				newAutoAllocations++;
//			}
		}

		shiftStatusService.updateShiftMetrics(shiftId);

		return new ReallocateShiftResponse(shiftId, removedAuto, manualCount, newAutoAllocations);
	}

	// -------------------------------------------------------------------------
	// Internal helpers
	// -------------------------------------------------------------------------

	private Map<String, Boolean> loadEnabledRules(Long businessId) {
		List<AllocationRule> rules = allocationRuleRepository.findByBusiness_IdOrderByPriorityAsc(businessId);
		Map<String, Boolean> map = new HashMap<>();
		for (AllocationRule rule : rules) {
			map.put(rule.getRuleKey(), rule.isEnabled());
		}
		map.put("hours_available", true);
		map.put("site_availability", true);
		map.put("role_capability", true);
		map.put("max_daily_hours", true);
		map.put("max_sites_per_day", true);
		map.put("site_continuity", true);
		map.put("target_hours", true);
		map.put("required_days_off", true);
		map.put("role_combos", true);
		return map;
	}

	private boolean isRuleEnabled(Map<String, Boolean> rules, String key) {
		return rules.getOrDefault(key, false); // default to disabled if not configured
	}

	private void clearAutoAllocations(Long businessId, LocalDate startDate, LocalDate endDate) {
		List<TimeBlock> allBlocks = timeBlockRepository.findAllBlocksInDateRange(businessId, startDate, endDate);
		List<TimeBlock> autoBlocks = allBlocks.stream().filter(
				tb -> !MANUAL_CREATED_BY.contains(tb.getCreatedBy() != null ? tb.getCreatedBy().toUpperCase() : ""))
				.collect(Collectors.toList());
		if (!autoBlocks.isEmpty()) {
			timeBlockRepository.deleteAll(autoBlocks);
			log.info("Cleared {} auto-allocations for business {} [{} - {}]", autoBlocks.size(), businessId, startDate,
					endDate);
		}
	}

	private Set<Long> getAllocatedStaffIds(Long shiftId, Long roleId) {
		return timeBlockRepository.findByStaff_IdAndRole_Id(shiftId, roleId).stream()
				.filter(tb -> tb.getBlockType() == BlockType.work).map(TimeBlock::getStaffId).filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	/**
	 * Applies all enabled hard filter rules. Returns the staff members that pass
	 * all filters.
	 */
	private List<StaffMember> applyHardFilters(List<StaffMember> candidates, Shift shift, Business business,
			Map<String, Boolean> rules) {
		List<StaffMember> result = new ArrayList<>();
		for (StaffMember staff : candidates) {
			List<String> violations = collectHardViolations(staff, shift, business, rules);
			if (violations.isEmpty()) {
				result.add(staff);
			}
		}
		return result;
	}

	/**
	 * Collects all hard constraint violations for a given staff member against a
	 * shift. Returns an empty list if the staff is fully eligible.
	 */
	private List<String> collectHardViolations(StaffMember staff, Shift shift, Business business,
			Map<String, Boolean> rules) {
		List<String> violations = new ArrayList<>();
		Long staffId = staff.getId();
		Long roleId = shift.getRole().getId();
		Long siteId = shift.getSite().getId();
		LocalDate shiftDate = shift.getShiftDate();

		// Rule 1: Check availability (not marked unavailable)
		if (isRuleEnabled(rules, "hours_available")) {
			short dow = (short) shiftDate.getDayOfWeek().getValue();
			boolean unavailable = staffAvailabilityRepository.existsUnavailableOnDate(staffId, shiftDate, dow,
					AvailabilityType.unavailable);
			if (unavailable) {
				violations.add("Staff is marked unavailable on " + shiftDate);
			}
		}

		// Rule 2: Staff must have an active StaffSite for this shift's site
		if (isRuleEnabled(rules, "site_availability")) {
			boolean hasSite = staffSiteRepository.existsByStaffIdAndSiteIdAndIsActiveTrue(staffId, siteId);
			if (!hasSite) {
				violations.add("Staff is not approved for site " + shift.getSite().getName());
			}
		}

		// Rule 3: Staff must have an active StaffRole for this shift's role
//		if (isRuleEnabled(rules, "role_capability")) {
//			Optional<StaffRole> staffRole = staffRoleRepository.findActiveStaffRole(staffId, roleId);
//			if (staffRole.isEmpty()) {
//				violations.add("Staff does not have active role " + shift.getRole().getName());
//			}
//		}

		// Hours fit check: shift duration must fall within staff's min/max hours per
		// day
		// from staff_work_parameters (covers total shift time constraint)
//		long shiftMinutes = computeShiftMinutes(shift);
//		double shiftHours = shiftMinutes / 60.0;
//		double minHoursPerDay = resolveMinHoursPerDay(staffId, business);
//		double maxHoursPerDay = resolveMaxHoursPerDay(staffId, business);
//		if (minHoursPerDay > 0 && shiftHours < minHoursPerDay) {
//			violations.add(String.format(
//				"Shift duration (%.1fh) is below this staff member's minimum daily hours (%.1fh)",
//				shiftHours, minHoursPerDay));
//		}
//		if (shiftHours > maxHoursPerDay) {
//			violations.add(String.format(
//				"Shift duration (%.1fh) exceeds this staff member's maximum daily hours (%.1fh)",
//				shiftHours, maxHoursPerDay));
//		}

		// Overlap check: reject if staff has any work block whose time window overlaps
		// this shift
		ZoneId shiftZone = resolveZone(business.getTimezone());
		Instant shiftStart = resolveInstant(shift.getStartInstant(), shiftDate, shift.getStartTime(), shiftZone);
		Instant shiftEnd = resolveInstant(shift.getEndInstant(), shiftDate, shift.getEndTime(), shiftZone);
		List<TimeBlock> overlapping = timeBlockRepository.findOverlappingBlocksForStaff(staffId, shiftStart, shiftEnd);
		boolean hasWorkOverlap = overlapping.stream().anyMatch(tb -> tb.getBlockType() == BlockType.work);
		if (hasWorkOverlap) {
			violations.add("Staff has a conflicting shift that overlaps this time window");
		}

		// One person per role per day: staff cannot be assigned the same role twice on
		// one date
		List<TimeBlock> todayBlocks = timeBlockRepository.findByStaffIdAndDate(staffId, shiftDate);
		boolean alreadyAssignedThisRoleToday = todayBlocks.stream().filter(tb -> tb.getBlockType() == BlockType.work)
				.anyMatch(tb -> roleId.equals(tb.getRoleId()));
		if (alreadyAssignedThisRoleToday) {
			violations.add("Staff is already assigned to role '" + shift.getRole().getName() + "' on " + shiftDate
					+ " — one person per role per day");
		}

		// Rule 9: Max daily hours check
//		if (isRuleEnabled(rules, "max_daily_hours")) {
//			long shiftMinutes = computeShiftMinutes(shift);
//			int existingMinutes = timeBlockRepository.sumWorkMinutesByStaffAndDate(staffId, shiftDate);
//			double maxHoursPerDay = resolveMaxHoursPerDay(staffId, business);
//			if (existingMinutes + shiftMinutes > maxHoursPerDay * 60) {
//				violations.add("Allocation would exceed max daily hours (" + maxHoursPerDay + "h)");
//			}
//			// Rule 6 filter: if staff has 0 remaining hours
//			if (existingMinutes >= maxHoursPerDay * 60) {
//				violations.add("Staff has no remaining hours for the day");
//			}
//		}

		// Rule 11: Max sites per day
		if (isRuleEnabled(rules, "max_sites_per_day")) {
			int maxSitesPerDay = resolveMaxSitesPerDay(staffId, business);
			long distinctSites = countDistinctSitesOnDate(staffId, shiftDate);
			// Check if this site is already one of them
			boolean alreadyAtThisSite = isStaffAtSiteOnDate(staffId, siteId, shiftDate);
			if (!alreadyAtThisSite && distinctSites >= maxSitesPerDay) {
				violations.add("Staff would exceed max sites per day (" + maxSitesPerDay + ")");
			}
		}

		// Rule 12: Site continuity — if staff worked at a different site yesterday and
		// hasn't met minDaysAtSiteBeforeMove
		if (isRuleEnabled(rules, "site_continuity")) {
			LocalDate yesterday = shiftDate.minusDays(1);
			ZoneId zone = resolveZone(business.getTimezone());
			Instant yStart = yesterday.atStartOfDay(zone).toInstant();
			Instant yEnd = yesterday.atTime(LocalTime.MAX).atZone(zone).toInstant();
			List<TimeBlock> yesterdayBlocks = timeBlockRepository.findOverlappingBlocksForStaff(staffId, yStart, yEnd);
			boolean workedDifferentSiteYesterday = yesterdayBlocks.stream()
					.filter(tb -> tb.getBlockType() == BlockType.work)
					.anyMatch(tb -> tb.getSiteId() != null && !tb.getSiteId().equals(siteId));
			if (workedDifferentSiteYesterday) {
				int minDays = business.getMinDaysAtSiteBeforeMove() != null ? business.getMinDaysAtSiteBeforeMove() : 1;
				// Check if staff already works at this site (has blocks at this site today or
				// recent)
				boolean alreadyAtThisSite = isStaffAtSiteOnDate(staffId, siteId, shiftDate);
				if (!alreadyAtThisSite && minDays > 1) {
					violations.add("Staff worked at a different site yesterday and must stay at that site for "
							+ minDays + " day(s)");
				}
			}
		}

		// Break sufficiency: if staff has already worked >= breakAfterHours today,
		// ensure they have the required break time before adding another shift
		checkSufficientBreakViolation(staffId, shift, business, violations);

		// Rule 15: Monthly target cap (target_hours)
		if (isRuleEnabled(rules, "target_hours")) {
			checkMonthlyCapViolation(staffId, shiftDate, business, violations);
		}

		// Rule 18: Required days off (required_days_off)
		if (isRuleEnabled(rules, "required_days_off")) {
			checkRequiredDaysOffViolation(staffId, shiftDate, business, violations);
		}

		// Leave check: block allocation if staff has approved leave covering the shift
		// date
		boolean onApprovedLeave = leaveRequestRepository.existsApprovedLeaveOnDate(staffId, shiftDate,
				LeaveRequestStatus.approved);
		if (onApprovedLeave) {
			violations.add("Staff has approved leave on " + shiftDate + " and cannot be allocated");
		}

		return violations;
	}

	/**
	 * Window-aware overload: same hard-filter checks but uses explicit
	 * winStart/winEnd for overlap detection (used during coverage-cursor
	 * allocation).
	 */
//	private List<String> collectHardViolations(StaffMember staff, Shift shift, Instant winStart, Instant winEnd,
//			Business business, Map<String, Boolean> rules) {
//		List<String> violations = new ArrayList<>();
//		Long staffId = staff.getId();
//		Long siteId = shift.getSite().getId();
//		LocalDate shiftDate = shift.getShiftDate();
//
//		// Rule 1: availability
//		if (isRuleEnabled(rules, "hours_available")) {
//			short dow = (short) shiftDate.getDayOfWeek().getValue();
//			boolean unavailable = staffAvailabilityRepository.existsUnavailableOnDate(staffId, shiftDate, dow,
//					AvailabilityType.unavailable);
//			if (unavailable) {
//				violations.add("Staff is marked unavailable on " + shiftDate);
//			}
//		}
//
//		// Rule 2: site approval
//		if (isRuleEnabled(rules, "site_availability")) {
//			boolean hasSite = staffSiteRepository.existsByStaffIdAndSiteIdAndIsActiveTrue(staffId, siteId);
//			if (!hasSite) {
//				violations.add("Staff is not approved for site " + shift.getSite().getName());
//			}
//		}
//
//		// Overlap check using the explicit window bounds
////		List<TimeBlock> overlapping = timeBlockRepository.findOverlappingBlocksForStaff(staffId, winStart, winEnd);
////		boolean hasWorkOverlap = overlapping.stream().anyMatch(tb -> tb.getBlockType() == BlockType.work);
////		if (hasWorkOverlap) {
////			violations.add("Staff has a conflicting shift that overlaps this time window");
////		}
//
//		// Leave check
//		boolean onApprovedLeave = leaveRequestRepository.existsApprovedLeaveOnDate(staffId, shiftDate,
//				LeaveRequestStatus.approved);
//		if (onApprovedLeave) {
//			violations.add("Staff has approved leave on " + shiftDate);
//		}
//
//		return violations;
//	}

	/**
	 * Top-up pass: for each staff member whose daily work hours are below their
	 * minimum, find open shifts at other approved sites and allocate them there.
	 */
	private void topUpMinimumHours(Long businessId, LocalDate startDate, LocalDate endDate, Business business,
			Map<String, Boolean> enabledRules) {

		List<StaffMember> allStaff = staffMemberRepository.findActiveStaff(businessId);

		for (StaffMember staff : allStaff) {
			double minHoursPerDay = resolveMinHoursPerDay(staff.getId(), business);
			if (minHoursPerDay <= 0)
				continue;

			// All approved sites for this staff member
			List<StaffSite> approvedSites = staffSiteRepository.findByStaffIdAndActiveWithSite(staff.getId(), true);
			if (approvedSites.size() <= 1)
				continue; // No alternative sites to try

			LocalDate date = startDate;
			while (!date.isAfter(endDate)) {
				final LocalDate currentDate = date;

				// Skip if staff is on approved leave this day
				if (leaveRequestRepository.existsApprovedLeaveOnDate(staff.getId(), currentDate,
						LeaveRequestStatus.approved)) {
					date = date.plusDays(1);
					continue;
				}

				int workedMinutes = timeBlockRepository.sumWorkMinutesByStaffAndDate(staff.getId(), currentDate);
				if (workedMinutes / 60.0 >= minHoursPerDay) {
					date = date.plusDays(1);
					continue; // Minimum already met
				}

				// Sites the staff is already scheduled at today
				List<TimeBlock> todayBlocks = timeBlockRepository.findByStaffIdAndDate(staff.getId(), currentDate);
				Set<Long> sitesWorkedToday = todayBlocks.stream().filter(tb -> tb.getBlockType() == BlockType.work)
						.map(TimeBlock::getSiteId).filter(Objects::nonNull).collect(Collectors.toSet());

				// Try each alternative approved site
				for (StaffSite ss : approvedSites) {
					Long altSiteId = ss.getSite().getId();
					if (sitesWorkedToday.contains(altSiteId))
						continue;

					List<Shift> altShifts = shiftRepository.findUnfilledShiftsBySiteAndDate(altSiteId, currentDate,
							List.of(ShiftStatus.open, ShiftStatus.partially_filled));

					for (Shift altShift : altShifts) {
						if (altShift.getStatus() == ShiftStatus.cancelled)
							continue;
						// Staff must not already be allocated to this shift
						Set<Long> alreadyOn = getAllocatedStaffIds(altShift.getId(), altShift.getRole().getId());
						if (alreadyOn.contains(staff.getId()))
							continue;

						List<String> violations = collectHardViolations(staff, altShift, business, enabledRules);
						if (!violations.isEmpty())
							continue;

						createWorkBlockForStaff(staff, altShift, business, enabledRules, "AUTO");
						shiftStatusService.updateShiftMetrics(altShift.getId());

						// Re-check if minimum is now met
						int updated = timeBlockRepository.sumWorkMinutesByStaffAndDate(staff.getId(), currentDate);
						if (updated / 60.0 >= minHoursPerDay)
							break;
					}

					// Break out of site loop too if minimum met
					int updated = timeBlockRepository.sumWorkMinutesByStaffAndDate(staff.getId(), currentDate);
					if (updated / 60.0 >= minHoursPerDay)
						break;
				}

				date = date.plusDays(1);
			}
		}
	}

	private void checkMonthlyCapViolation(Long staffId, LocalDate shiftDate, Business business,
			List<String> violations) {
		LocalDate monthStart = shiftDate.withDayOfMonth(1);
		LocalDate monthEnd = shiftDate.withDayOfMonth(shiftDate.lengthOfMonth());
		List<TimeBlock> monthBlocks = timeBlockRepository.findByStaffIdAndDateRange(staffId, monthStart, monthEnd);
		long existingMonthMinutes = monthBlocks.stream().filter(tb -> tb.getBlockType() == BlockType.work)
				.mapToLong(tb -> {
					if (tb.getDurationMinutes() != null) {
						return tb.getDurationMinutes();
					}
					if (tb.getStartTime() != null && tb.getEndTime() != null) {
						return Duration.between(tb.getStartTime(), tb.getEndTime()).toMinutes();
					}
					return 0L;
				}).sum();
		double existingMonthHours = existingMonthMinutes / 60.0;

		// Use staff-specific params or business cap
		Optional<StaffWorkParameters> swp = staffWorkParametersRepository.findCurrentByStaffId(staffId);
		double maxMonthlyHours;
		if (swp.isPresent() && swp.get().getMaxHoursPerMonth() != null) {
			maxMonthlyHours = swp.get().getMaxHoursPerMonth().doubleValue();
		} else {
			// Fallback to business default work parameters
			Optional<WorkParameters> wp = workParametersRepository.findByBusiness_IdAndIsDefaultTrue(business.getId());
			maxMonthlyHours = wp.filter(w -> w.getMaxHoursPerMonth() != null)
					.map(w -> w.getMaxHoursPerMonth().doubleValue()).orElse(Double.MAX_VALUE);
		}

		if (maxMonthlyHours < Double.MAX_VALUE) {
			BigDecimal toleranceOver = business.getToleranceOverPercentage() != null
					? business.getToleranceOverPercentage()
					: BigDecimal.ZERO;
			BigDecimal toleranceUnder = business.getToleranceUnderPercentage() != null
					? business.getToleranceUnderPercentage()
					: BigDecimal.ZERO;
			double cap = maxMonthlyHours * (1 + toleranceOver.doubleValue() / 100.0);
			double lowerBound = maxMonthlyHours * (1 - toleranceUnder.doubleValue() / 100.0);
			if (existingMonthHours >= cap) {
				violations.add("Staff monthly hours (" + String.format("%.1f", existingMonthHours) + "h) exceed cap ("
						+ String.format("%.1f", cap) + "h) [target window: " + String.format("%.1f", lowerBound)
						+ "h – " + String.format("%.1f", cap) + "h]");
			}
		}
	}

	private void checkSufficientBreakViolation(Long staffId, Shift shift, Business business, List<String> violations) {
		long breakAfterMinutes = resolveBreakAfterMinutes(business);
		int requiredBreakMinutes = resolveRequiredBreakMinutes(business);

		if (breakAfterMinutes <= 0 || requiredBreakMinutes <= 0) {
			return; // Rule not configured
		}

		LocalDate shiftDate = shift.getShiftDate();

		// Only check if the staff has already worked enough today to trigger the rule
		int existingWorkMinutes = timeBlockRepository.sumWorkMinutesByStaffAndDate(staffId, shiftDate);
		if (existingWorkMinutes < breakAfterMinutes) {
			return;
		}

		// They've crossed the threshold — verify existing break time is sufficient
		List<TimeBlock> todayBlocks = timeBlockRepository.findByStaffIdAndDate(staffId, shiftDate);
		int existingBreakMinutes = todayBlocks.stream().filter(tb -> tb.getBlockType() == BlockType.break_period)
				.mapToInt(tb -> tb.getDurationMinutes() != null ? tb.getDurationMinutes() : 0).sum();

		if (existingBreakMinutes < requiredBreakMinutes) {
			violations.add(String.format(
					"Staff has worked %.1fh today but only has %d min break (%.1fh+ work requires at least %d min break)",
					existingWorkMinutes / 60.0, existingBreakMinutes, breakAfterMinutes / 60.0, requiredBreakMinutes));
		}
	}

	private void checkRequiredDaysOffViolation(Long staffId, LocalDate shiftDate, Business business,
			List<String> violations) {
		LocalDate weekStart = shiftDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate weekEnd = weekStart.plusDays(6); // Sunday

		int minDaysOff = resolveMinDaysOffPerWeek(staffId, business);
		int maxWorkDays = 7 - minDaysOff;

		// Count distinct worked dates across the full week (excluding today, not yet
		// assigned)
		List<TimeBlock> weekBlocks = timeBlockRepository.findByStaffIdAndDateRange(staffId, weekStart, weekEnd);
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

	/**
	 * Sorts eligible staff by scoring rules.
	 */
	private List<StaffMember> applySorting(List<StaffMember> eligible, Shift shift, Business business,
			Map<String, Boolean> rules) {
		Long roleId = shift.getRole().getId();
		LocalDate shiftDate = shift.getShiftDate();
		Long staffId_unused = null; // for lambda

		List<StaffMember> sorted = new ArrayList<>(eligible);
		sorted.sort((a, b) -> {
			// Role combo: direct-role staff sorted before combo (via sibling-role) staff
			if (isRuleEnabled(rules, "role_combos")) {
				boolean aIsDirect = staffRoleRepository.findActiveStaffRole(a.getId(), roleId).isPresent();
				boolean bIsDirect = staffRoleRepository.findActiveStaffRole(b.getId(), roleId).isPresent();
				if (aIsDirect != bIsDirect)
					return aIsDirect ? -1 : 1;
			}

			// Rule 4: Competency DESC (L3 first)
			if (isRuleEnabled(rules, "role_competency")) {
				int skillCmp = compareSkillLevel(b.getId(), roleId) - compareSkillLevel(a.getId(), roleId);
				if (skillCmp != 0)
					return skillCmp;
			}

			// Rule 5: Cheapest ASC (lowest hourly rate first)
			if (isRuleEnabled(rules, "cheapest")) {
				BigDecimal rateA = staffCompensationRepository.findCurrentRate(a.getId(), roleId, shiftDate)
						.orElse(BigDecimal.ZERO);
				BigDecimal rateB = staffCompensationRepository.findCurrentRate(b.getId(), roleId, shiftDate)
						.orElse(BigDecimal.ZERO);
				int rateCmp = rateA.compareTo(rateB);
				if (rateCmp != 0)
					return rateCmp;
			}

			// Rule 6: Available hours DESC (most remaining hours first)
			if (isRuleEnabled(rules, "hours_available")) {
				double remA = computeRemainingHours(a.getId(), shiftDate, business);
				double remB = computeRemainingHours(b.getId(), shiftDate, business);
				int remCmp = Double.compare(remB, remA); // DESC
				if (remCmp != 0)
					return remCmp;
			}

			// Monthly target under-tolerance: prioritize staff furthest below their lower
			// bound
			if (isRuleEnabled(rules, "target_hours")) {
				LocalDate monthStart = shiftDate.withDayOfMonth(1);
				LocalDate monthEnd = shiftDate.withDayOfMonth(shiftDate.lengthOfMonth());
				double underPct = business.getToleranceUnderPercentage() != null
						? business.getToleranceUnderPercentage().doubleValue()
						: 0.0;
				double targetA = resolveMonthlyTarget(a.getId(), business);
				double targetB = resolveMonthlyTarget(b.getId(), business);
				double lowerA = targetA * (1 - underPct / 100.0);
				double lowerB = targetB * (1 - underPct / 100.0);
				long minutesA = timeBlockRepository.sumWorkMinutesByStaffAndMonth(a.getId(), monthStart, monthEnd);
				long minutesB = timeBlockRepository.sumWorkMinutesByStaffAndMonth(b.getId(), monthStart, monthEnd);
				double gapA = lowerA - (minutesA / 60.0); // positive = below minimum (needs hours)
				double gapB = lowerB - (minutesB / 60.0);
				int gapCmp = Double.compare(gapB, gapA); // larger gap = higher priority
				if (gapCmp != 0)
					return gapCmp;
			}

			// Alternation: prefer staff with fewer consecutive days on this same shift type
			int consA = countConsecutiveSameShiftDays(a.getId(), shift);
			int consB = countConsecutiveSameShiftDays(b.getId(), shift);
			if (consA != consB)
				return Integer.compare(consA, consB); // fewer consecutive = higher priority

			return Long.compare(a.getId(), b.getId()); // stable tiebreaker
		});

		return sorted;
	}

	private int compareSkillLevel(Long staffId, Long roleId) {
		return staffRoleRepository.findActiveStaffRole(staffId, roleId)
				.map(sr -> sr.getSkillLevel() != null ? sr.getSkillLevel().getNumericLevel() : 0).orElse(0);
	}

	private double computeRemainingHours(Long staffId, LocalDate date, Business business) {
		int existingMinutes = timeBlockRepository.sumWorkMinutesByStaffAndDate(staffId, date);
		double maxHoursPerDay = resolveMaxHoursPerDay(staffId, business);
		return maxHoursPerDay - (existingMinutes / 60.0);
	}

	private double computeScore(StaffMember staff, Shift shift, Business business, Map<String, Boolean> rules) {
		double score = 0.0;
		Long staffId = staff.getId();
		Long roleId = shift.getRole().getId();
		LocalDate shiftDate = shift.getShiftDate();

		if (isRuleEnabled(rules, "role_competency")) {
			score += compareSkillLevel(staffId, roleId) * 100.0;
		}

		if (isRuleEnabled(rules, "hours_available")) {
			double remHours = computeRemainingHours(staffId, shiftDate, business);
			score += remHours * 10.0;
		}

		if (isRuleEnabled(rules, "cheapest")) {
			BigDecimal rate = staffCompensationRepository.findCurrentRate(staffId, roleId, shiftDate)
					.orElse(BigDecimal.ZERO);
			// Lower rate = higher score: invert
			double rateVal = rate.doubleValue();
			score += (rateVal > 0) ? (100.0 / rateVal) : 10.0;
		}

		// Alternation penalty: reduce score for each consecutive day on the same shift
		int consecutiveDays = countConsecutiveSameShiftDays(staffId, shift);
		score -= consecutiveDays * 40.0;

		return score;
	}

	/**
	 * Counts how many consecutive days before the shift date the staff member has
	 * already worked the same shift type (same site + role). Used to enforce shift
	 * alternation — staff with higher consecutive counts are deprioritised.
	 */
	private int countConsecutiveSameShiftDays(Long staffId, Shift shift) {
		Long siteId = shift.getSite().getId();
		Long roleId = shift.getRole().getId();
		int count = 0;
		LocalDate checkDate = shift.getShiftDate().minusDays(1);
		for (int i = 0; i < 5; i++) {
			List<TimeBlock> blocks = timeBlockRepository.findByStaffIdAndDate(staffId, checkDate);
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
			String createdBy) {
		ZoneId zone = resolveZone(business.getTimezone());
		Instant blockStart = resolveInstant(shift.getStartInstant(), shift.getShiftDate(), shift.getStartTime(), zone);
		Instant blockEnd = resolveInstant(shift.getEndInstant(), shift.getShiftDate(), shift.getEndTime(), zone);
		return createWorkBlockForStaff(staff, shift, blockStart, blockEnd, business, rules, createdBy);
	}

	private int createWorkBlockForStaff(StaffMember staff, Shift shift, Instant blockStart, Instant blockEnd,
			Business business, Map<String, Boolean> rules, String createdBy) {
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
		int blocksCreated = 1;

		// Rule 8 + Rule 13: Insert break if shift duration > maxContinuousWorkMinutes
		// if (isRuleEnabled(rules, "max_continuous_hours")) {
		boolean breakInserted = tryInsertBreak(staff, shift, business, rules, createdBy);
		if (breakInserted) {
			blocksCreated++;
		}
		// }

		return blocksCreated;
	}

	/**
	 * Inserts a break_period TimeBlock if shift duration exceeds
	 * maxContinuousWorkMinutes. Returns true if a break was inserted.
	 */
	private boolean tryInsertBreak(StaffMember staff, Shift shift, Business business, Map<String, Boolean> rules,
			String createdBy) {
		long shiftMinutes = computeShiftMinutes(shift);
		int maxContinuous = business.getMaxContinuousWorkMinutes() != null ? business.getMaxContinuousWorkMinutes()
				: 300;
		long breakAfterMinutes = resolveBreakAfterMinutes(business);

		// Trigger on whichever threshold is lower (more restrictive)
		long triggerThreshold = breakAfterMinutes > 0 ? Math.min(maxContinuous, breakAfterMinutes) : maxContinuous;

		if (shiftMinutes <= triggerThreshold) {
			return false;
		}

		int breakDuration = resolveRequiredBreakMinutes(business);

		ZoneId zone = resolveZone(business.getTimezone());
		Instant shiftStart = resolveInstant(shift.getStartInstant(), shift.getShiftDate(), shift.getStartTime(), zone);
		Instant shiftEnd = resolveInstant(shift.getEndInstant(), shift.getShiftDate(), shift.getEndTime(), zone);

		// Place break at midpoint
		long midpointMinutes = shiftMinutes / 2;
		Instant breakStart = shiftStart.plus(midpointMinutes - (long) breakDuration / 2, ChronoUnit.MINUTES);
		Instant breakEnd = breakStart.plus(breakDuration, ChronoUnit.MINUTES);

		// Clamp to shift bounds
		if (breakStart.isBefore(shiftStart)) {
			breakStart = shiftStart;
			breakEnd = breakStart.plus(breakDuration, ChronoUnit.MINUTES);
		}
		if (breakEnd.isAfter(shiftEnd)) {
			breakEnd = shiftEnd;
			breakStart = breakEnd.minusSeconds(breakDuration * 60L);
		}

		// No two staff members may have breaks at the same time — stagger if overlap
		List<TimeBlock> concurrentBreaks = timeBlockRepository.findByRoleAndTimeRangeAndBlockType(
				shift.getRole().getId(), breakStart, breakEnd, BlockType.break_period);
		if (!concurrentBreaks.isEmpty()) {
			breakStart = breakStart.plus(shift.getBreakDurationMinutes(), ChronoUnit.MINUTES);
			breakEnd = breakEnd.plus(shift.getBreakDurationMinutes(), ChronoUnit.MINUTES);

			if (breakEnd.isAfter(shiftEnd)) {
				breakStart = shiftStart.plus(midpointMinutes - (long) breakDuration / 2, ChronoUnit.MINUTES).minus(15,
						ChronoUnit.MINUTES);
				breakEnd = breakStart.plus(breakDuration, ChronoUnit.MINUTES);
			}

			List<TimeBlock> stillConcurrent = timeBlockRepository.findByRoleAndTimeRangeAndBlockType(
					shift.getRole().getId(), breakStart, breakEnd, BlockType.break_period);
			if (!stillConcurrent.isEmpty()) {
				log.warn("Could not find non-concurrent break slot for staff {} on shift {}. Skipping break.",
						staff.getId(), shift.getId());
				return false;
			}
		}

		// Rule 13: Verify a break is needed (total continuous work minutes >
		// minWorkBeforeBreakMinutes)
		int minWorkBeforeBreak = business.getMinWorkBeforeBreakMinutes() != null
				? business.getMinWorkBeforeBreakMinutes()
				: 180;
		if (shiftMinutes < minWorkBeforeBreak) {
			return false; // No break needed yet
		}

		TimeBlock breakBlock = new TimeBlock();
		breakBlock.setShift(shift);
		breakBlock.setStaff(staff);
		breakBlock.setRole(shift.getRole());
		breakBlock.setSite(shift.getSite());
		breakBlock.setStartTime(breakStart);
		breakBlock.setEndTime(breakEnd);
		breakBlock.setBlockType(BlockType.break_period);
		breakBlock.setBreakSequenceNumber(1);
		breakBlock.setCreatedBy(createdBy);

		timeBlockRepository.save(breakBlock);
		return true;
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

	private double resolveMaxHoursPerDay(Long staffId, Business business) {
		Optional<StaffWorkParameters> swp = staffWorkParametersRepository.findCurrentByStaffId(staffId);
		if (swp.isPresent() && swp.get().getMaxHoursPerDay() != null) {
			return swp.get().getMaxHoursPerDay().doubleValue();
		}
		// Fall back to business cap
		double cap = business.getMaxDailyHoursCap() != null ? business.getMaxDailyHoursCap().doubleValue() : 12.0;

		// Also check work parameters defaults
		Optional<WorkParameters> wp = workParametersRepository.findByBusiness_IdAndIsDefaultTrue(business.getId());
		if (wp.isPresent() && wp.get().getMaxHoursPerDay() != null) {
			return wp.get().getMaxHoursPerDay().doubleValue();
		}
		return cap;
	}

	private double resolveMinHoursPerDay(Long staffId, Business business) {
		Optional<StaffWorkParameters> swp = staffWorkParametersRepository.findCurrentByStaffId(staffId);
		if (swp.isPresent() && swp.get().getMinHoursPerDay() != null) {
			return swp.get().getMinHoursPerDay().doubleValue();
		}
		Optional<WorkParameters> wp = workParametersRepository.findByBusiness_IdAndIsDefaultTrue(business.getId());
		if (wp.isPresent() && wp.get().getMinHoursPerDay() != null) {
			return wp.get().getMinHoursPerDay().doubleValue();
		}
		return 0.0; // No minimum configured — any shift length is acceptable
	}

	private int resolveMaxSitesPerDay(Long staffId, Business business) {
		Optional<StaffWorkParameters> swp = staffWorkParametersRepository.findCurrentByStaffId(staffId);
		if (swp.isPresent() && swp.get().getMaxSitesPerDay() != null) {
			return swp.get().getMaxSitesPerDay();
		}
		return 1; // Default: one site per day
	}

	private int resolveMinDaysOffPerWeek(Long staffId, Business business) {
		Optional<StaffWorkParameters> swp = staffWorkParametersRepository.findCurrentByStaffId(staffId);
		if (swp.isPresent() && swp.get().getMinDaysOffPerWeek() != null) {
			return swp.get().getMinDaysOffPerWeek();
		}
		Optional<WorkParameters> wp = workParametersRepository.findByBusiness_IdAndIsDefaultTrue(business.getId());
		if (wp.isPresent() && wp.get().getMinDaysOffPerWeek() != null) {
			return wp.get().getMinDaysOffPerWeek();
		}
		return 1; // Default: at least 1 day off per week
	}

	/**
	 * Returns the IDs of all roles that share an active combo with the given role,
	 * excluding the role itself. Empty list if combos are not configured.
	 */
	private List<Long> getComboSiblingRoleIds(Long roleId, Long businessId) {
		return roleComboRoleRepository.findComboSiblingRoleIds(roleId, businessId);
	}

	private double resolveMonthlyTarget(Long staffId, Business business) {
		Optional<StaffWorkParameters> swp = staffWorkParametersRepository.findCurrentByStaffId(staffId);
		if (swp.isPresent() && swp.get().getMaxHoursPerMonth() != null) {
			return swp.get().getMaxHoursPerMonth().doubleValue();
		}
		Optional<WorkParameters> wp = workParametersRepository.findByBusiness_IdAndIsDefaultTrue(business.getId());
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
	private long resolveBreakAfterMinutes(Business business) {
		Optional<WorkParameters> wp = workParametersRepository.findByBusiness_IdAndIsDefaultTrue(business.getId());
		if (wp.isPresent() && wp.get().getBreakAfterHours() != null) {
			return (long) (wp.get().getBreakAfterHours().doubleValue() * 60);
		}
		return business.getMaxContinuousWorkMinutes() != null ? business.getMaxContinuousWorkMinutes() : 300;
	}

	/**
	 * Returns the minimum required break duration in minutes. Reads from
	 * WorkParameters.minBreakDurationMinutes first, falls back to
	 * business.minBreakDurationMinutes.
	 */
	private int resolveRequiredBreakMinutes(Business business) {
		Optional<WorkParameters> wp = workParametersRepository.findByBusiness_IdAndIsDefaultTrue(business.getId());
		if (wp.isPresent() && wp.get().getMinBreakDurationMinutes() != null) {
			return wp.get().getMinBreakDurationMinutes();
		}
		return business.getMinBreakDurationMinutes() != null ? business.getMinBreakDurationMinutes() : 30;
	}

	private long countDistinctSitesOnDate(Long staffId, LocalDate date) {
		List<TimeBlock> blocks = timeBlockRepository.findByStaffIdAndDate(staffId, date);
		return blocks.stream().filter(tb -> tb.getBlockType() == BlockType.work).map(TimeBlock::getSiteId)
				.filter(Objects::nonNull).distinct().count();
	}

	private boolean isStaffAtSiteOnDate(Long staffId, Long siteId, LocalDate date) {
		List<TimeBlock> blocks = timeBlockRepository.findByStaffIdAndDate(staffId, date);
		return blocks.stream().filter(tb -> tb.getBlockType() == BlockType.work)
				.anyMatch(tb -> siteId.equals(tb.getSiteId()));
	}

	private ZoneId resolveZone(String timezone) {
		if (timezone == null || timezone.isBlank()) {
			return ZoneOffset.UTC;
		}
		try {
			return ZoneId.of(timezone);
		} catch (DateTimeException e) {
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
		return Instant.now();
	}
}
