package com.populace.allocation.service;

import com.populace.domain.*;
import com.populace.domain.enums.AvailabilityType;
import com.populace.domain.enums.BlockType;
import com.populace.domain.enums.LeaveRequestStatus;
import com.populace.repository.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Run-scoped memoization cache for allocation engine.
 * <p>
 * Wraps repository calls so the first invocation hits the DB and subsequent
 * calls with identical arguments return cached results.
 * <p>
 * Two categories of data:
 * <ul>
 *   <li><b>Stable</b> — roles, rates, availability, leave, work params, site
 *       approvals, combo siblings. Cached for the entire allocation run.</li>
 *   <li><b>Mutable</b> — time-block sums, overlaps, blocks-by-date.
 *       Cached but invalidated per-staff when a new block is saved.</li>
 * </ul>
 */
public class AllocationContext {

	// -----------------------------------------------------------------------
	// Repository references (set once at construction)
	// -----------------------------------------------------------------------
	private final StaffRoleRepository staffRoleRepository;
	private final StaffAvailabilityRepository staffAvailabilityRepository;
	private final StaffSiteRepository staffSiteRepository;
	private final StaffCompensationRepository staffCompensationRepository;
	private final StaffWorkParametersRepository staffWorkParametersRepository;
	private final WorkParametersRepository workParametersRepository;
	private final LeaveRequestRepository leaveRequestRepository;
	private final RoleComboRoleRepository roleComboRoleRepository;
	private final TimeBlockRepository timeBlockRepository;

	// -----------------------------------------------------------------------
	// STABLE caches — never invalidated during a run
	// -----------------------------------------------------------------------
	private final Map<String, Optional<StaffRole>> staffRoleCache = new HashMap<>();
	private final Map<String, Boolean> unavailableCache = new HashMap<>();
	private final Map<String, Boolean> siteApprovalCache = new HashMap<>();
	private final Map<String, Optional<BigDecimal>> rateCache = new HashMap<>();
	private final Map<Long, Optional<StaffWorkParameters>> staffWorkParamsCache = new HashMap<>();
	private final Map<Long, Optional<WorkParameters>> businessWorkParamsCache = new HashMap<>();
	private final Map<String, Boolean> approvedLeaveCache = new HashMap<>();
	private final Map<String, List<Long>> comboSiblingCache = new HashMap<>();

	// -----------------------------------------------------------------------
	// MUTABLE caches — invalidated per-staff after block saves
	// -----------------------------------------------------------------------
	private final Map<String, Integer> sumWorkByDayCache = new HashMap<>();
	private final Map<String, Long> sumBreakByDayCache = new HashMap<>();
	private final Map<String, Long> sumWorkByWeekCache = new HashMap<>();
	private final Map<String, Long> sumBreakByWeekCache = new HashMap<>();
	private final Map<String, List<TimeBlock>> blocksByDateCache = new HashMap<>();
	private final Map<String, List<TimeBlock>> blocksByDateRangeCache = new HashMap<>();
	private final Map<String, List<TimeBlock>> overlappingBlocksCache = new HashMap<>();
	private final Map<String, List<TimeBlock>> blocksByTimeRangeCache = new HashMap<>();

	public AllocationContext(
			StaffRoleRepository staffRoleRepository,
			StaffAvailabilityRepository staffAvailabilityRepository,
			StaffSiteRepository staffSiteRepository,
			StaffCompensationRepository staffCompensationRepository,
			StaffWorkParametersRepository staffWorkParametersRepository,
			WorkParametersRepository workParametersRepository,
			LeaveRequestRepository leaveRequestRepository,
			RoleComboRoleRepository roleComboRoleRepository,
			TimeBlockRepository timeBlockRepository) {
		this.staffRoleRepository = staffRoleRepository;
		this.staffAvailabilityRepository = staffAvailabilityRepository;
		this.staffSiteRepository = staffSiteRepository;
		this.staffCompensationRepository = staffCompensationRepository;
		this.staffWorkParametersRepository = staffWorkParametersRepository;
		this.workParametersRepository = workParametersRepository;
		this.leaveRequestRepository = leaveRequestRepository;
		this.roleComboRoleRepository = roleComboRoleRepository;
		this.timeBlockRepository = timeBlockRepository;
	}

	// =======================================================================
	// STABLE DATA — cached for entire run
	// =======================================================================

	public Optional<StaffRole> findActiveStaffRole(Long staffId, Long roleId) {
		String key = staffId + ":" + roleId;
		return staffRoleCache.computeIfAbsent(key,
				k -> staffRoleRepository.findActiveStaffRole(staffId, roleId));
	}

	public boolean isUnavailableOnDate(Long staffId, LocalDate date, short dow, AvailabilityType type) {
		String key = staffId + ":" + date + ":" + dow + ":" + type;
		return unavailableCache.computeIfAbsent(key,
				k -> staffAvailabilityRepository.existsUnavailableOnDate(staffId, date, dow, type));
	}

	public boolean hasSiteApproval(Long staffId, Long siteId) {
		String key = staffId + ":" + siteId;
		return siteApprovalCache.computeIfAbsent(key,
				k -> staffSiteRepository.existsByStaffIdAndSiteIdAndIsActiveTrue(staffId, siteId));
	}

	public Optional<BigDecimal> findCurrentRate(Long staffId, Long roleId, LocalDate date) {
		String key = staffId + ":" + roleId + ":" + date;
		return rateCache.computeIfAbsent(key,
				k -> staffCompensationRepository.findCurrentRate(staffId, roleId, date));
	}

	public Optional<StaffWorkParameters> findCurrentWorkParams(Long staffId) {
		return staffWorkParamsCache.computeIfAbsent(staffId,
				k -> staffWorkParametersRepository.findCurrentByStaffId(staffId));
	}

	public Optional<WorkParameters> findBusinessWorkParams(Long businessId) {
		return businessWorkParamsCache.computeIfAbsent(businessId,
				k -> workParametersRepository.findByBusiness_IdAndIsDefaultTrue(businessId));
	}

	public boolean hasApprovedLeave(Long staffId, LocalDate date, LeaveRequestStatus status) {
		String key = staffId + ":" + date + ":" + status;
		return approvedLeaveCache.computeIfAbsent(key,
				k -> leaveRequestRepository.existsApprovedLeaveOnDate(staffId, date, status));
	}

	public List<Long> getComboSiblingRoleIds(Long roleId, Long businessId) {
		String key = roleId + ":" + businessId;
		return comboSiblingCache.computeIfAbsent(key,
				k -> roleComboRoleRepository.findComboSiblingRoleIds(roleId, businessId));
	}

	// =======================================================================
	// MUTABLE DATA — cached, invalidated per-staff after block saves
	// =======================================================================

	public int sumWorkMinutesByStaffAndDate(Long staffId, LocalDate date) {
		String key = staffId + ":" + date;
		return sumWorkByDayCache.computeIfAbsent(key,
				k -> timeBlockRepository.sumWorkMinutesByStaffAndDate(staffId, date));
	}

	public long sumBreakMinutesByStaffAndDate(Long staffId, LocalDate date) {
		String key = staffId + ":" + date;
		return sumBreakByDayCache.computeIfAbsent(key,
				k -> timeBlockRepository.sumBreakMinutesByStaffAndDate(staffId, date));
	}

	public long sumWorkMinutesByStaffAndWeek(Long staffId, LocalDate weekStart, LocalDate weekEnd) {
		String key = staffId + ":" + weekStart + ":" + weekEnd;
		return sumWorkByWeekCache.computeIfAbsent(key,
				k -> timeBlockRepository.sumWorkMinutesByStaffAndWeek(staffId, weekStart, weekEnd));
	}

	public long sumBreakMinutesByStaffAndWeek(Long staffId, LocalDate weekStart, LocalDate weekEnd) {
		String key = staffId + ":" + weekStart + ":" + weekEnd;
		return sumBreakByWeekCache.computeIfAbsent(key,
				k -> timeBlockRepository.sumBreakMinutesByStaffAndWeek(staffId, weekStart, weekEnd));
	}

	public List<TimeBlock> findByStaffIdAndDate(Long staffId, LocalDate date) {
		String key = staffId + ":" + date;
		return blocksByDateCache.computeIfAbsent(key,
				k -> timeBlockRepository.findByStaffIdAndDate(staffId, date));
	}

	public List<TimeBlock> findByStaffIdAndDateRange(Long staffId, LocalDate start, LocalDate end) {
		String key = staffId + ":" + start + ":" + end;
		return blocksByDateRangeCache.computeIfAbsent(key,
				k -> timeBlockRepository.findByStaffIdAndDateRange(staffId, start, end));
	}

	public List<TimeBlock> findOverlappingBlocksForStaff(Long staffId, Instant start, Instant end) {
		String key = staffId + ":" + start + ":" + end;
		return overlappingBlocksCache.computeIfAbsent(key,
				k -> timeBlockRepository.findOverlappingBlocksForStaff(staffId, start, end));
	}

	public List<TimeBlock> findByStaffIdAndTimeRange(Long staffId, Instant start, Instant end) {
		String key = staffId + ":" + start + ":" + end;
		return blocksByTimeRangeCache.computeIfAbsent(key,
				k -> timeBlockRepository.findByStaffIdAndTimeRange(staffId, start, end));
	}

	// =======================================================================
	// INVALIDATION — call after timeBlockRepository.save() for a staff member
	// =======================================================================

	/**
	 * Clears all mutable caches for a given staff member.
	 * Must be called after every {@code timeBlockRepository.save()} that creates
	 * or modifies a block for this staff.
	 */
	public void invalidateStaffMutableData(Long staffId) {
		String prefix = staffId + ":";
		sumWorkByDayCache.keySet().removeIf(k -> k.startsWith(prefix));
		sumBreakByDayCache.keySet().removeIf(k -> k.startsWith(prefix));
		sumWorkByWeekCache.keySet().removeIf(k -> k.startsWith(prefix));
		sumBreakByWeekCache.keySet().removeIf(k -> k.startsWith(prefix));
		blocksByDateCache.keySet().removeIf(k -> k.startsWith(prefix));
		blocksByDateRangeCache.keySet().removeIf(k -> k.startsWith(prefix));
		overlappingBlocksCache.keySet().removeIf(k -> k.startsWith(prefix));
		blocksByTimeRangeCache.keySet().removeIf(k -> k.startsWith(prefix));
	}
}
